package reembolso.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Leitura e validação de JSON comuns aos leitores de despesas, política e câmbio.
 *
 * <p>Números são lidos como {@link BigDecimal} com os dígitos exatos do arquivo (plano, DT-001).
 * Toda falha vira {@link EntradaInvalidaException} com o caminho do campo na mensagem.
 */
final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);

    private Json() {
    }

    /** Lê o arquivo como texto UTF-8; {@code descricao} entra na mensagem de erro ("de entrada", "de política"…). */
    static String lerArquivo(Path arquivo, String descricao) throws EntradaInvalidaException {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EntradaInvalidaException("não foi possível ler o arquivo " + descricao + ": " + arquivo, e);
        }
    }

    /** Interpreta o texto e exige um objeto JSON na raiz; {@code nome} entra na mensagem de erro. */
    static JsonNode raiz(String json, String nome) throws EntradaInvalidaException {
        JsonNode raiz;
        try {
            raiz = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("JSON malformado: " + e.getOriginalMessage(), e);
        }
        if (raiz == null || !raiz.isObject()) {
            throw new EntradaInvalidaException(nome + " deve ser um objeto JSON");
        }
        return raiz;
    }

    static JsonNode obrigatorio(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        JsonNode no = pai.get(campo);
        if (no == null || no.isNull()) {
            throw new EntradaInvalidaException("campo obrigatório ausente: " + caminho);
        }
        return no;
    }

    static JsonNode objeto(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        return comoObjeto(obrigatorio(pai, campo, caminho), caminho);
    }

    static JsonNode comoObjeto(JsonNode no, String caminho) throws EntradaInvalidaException {
        if (!no.isObject()) {
            throw new EntradaInvalidaException(caminho + ": deve ser um objeto");
        }
        return no;
    }

    static String texto(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        JsonNode no = obrigatorio(pai, campo, caminho);
        if (!no.isTextual()) {
            throw new EntradaInvalidaException(caminho + ": deve ser texto");
        }
        return no.textValue();
    }

    static LocalDate data(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        return comoData(texto(pai, campo, caminho), caminho);
    }

    static LocalDate comoData(String valor, String caminho) throws EntradaInvalidaException {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new EntradaInvalidaException(caminho + ": data inválida, esperado AAAA-MM-DD: " + valor, e);
        }
    }

    static BigDecimal numero(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        return comoNumero(obrigatorio(pai, campo, caminho), caminho);
    }

    static BigDecimal comoNumero(JsonNode no, String caminho) throws EntradaInvalidaException {
        if (!no.isNumber()) {
            throw new EntradaInvalidaException(caminho + ": deve ser número");
        }
        return no.decimalValue();
    }

    static boolean booleano(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        JsonNode no = obrigatorio(pai, campo, caminho);
        if (!no.isBoolean()) {
            throw new EntradaInvalidaException(caminho + ": deve ser true ou false");
        }
        return no.booleanValue();
    }
}
