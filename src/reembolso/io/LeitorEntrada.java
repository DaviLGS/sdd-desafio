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
import java.util.ArrayList;
import java.util.List;
import reembolso.dominio.Colaborador;
import reembolso.dominio.Despesa;
import reembolso.dominio.Entrada;
import reembolso.dominio.Periodo;

/**
 * Lê e valida o JSON de entrada (spec, seção 4).
 *
 * <p>Os números são lidos como {@link BigDecimal} com os dígitos exatos do arquivo (plano, DT-001).
 * A despesa sai crua: normalização (RN-001) e arredondamento (RN-002) ficam com o motor.
 */
public final class LeitorEntrada {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);

    public Entrada lerArquivo(Path arquivo) throws EntradaInvalidaException {
        String conteudo;
        try {
            conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EntradaInvalidaException("não foi possível ler o arquivo de entrada: " + arquivo, e);
        }
        return lerTexto(conteudo);
    }

    public Entrada lerTexto(String json) throws EntradaInvalidaException {
        JsonNode raiz;
        try {
            raiz = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new EntradaInvalidaException("JSON malformado: " + e.getOriginalMessage(), e);
        }
        if (raiz == null || !raiz.isObject()) {
            throw new EntradaInvalidaException("a entrada deve ser um objeto JSON");
        }

        JsonNode colaborador = objeto(raiz, "colaborador", "colaborador");
        JsonNode periodo = objeto(raiz, "periodo", "periodo");

        return new Entrada(
                new Colaborador(
                        texto(colaborador, "id", "colaborador.id"),
                        texto(colaborador, "nome", "colaborador.nome"),
                        texto(colaborador, "centro_custo", "colaborador.centro_custo")),
                new Periodo(
                        texto(periodo, "competencia", "periodo.competencia"),
                        data(periodo, "inicio", "periodo.inicio"),
                        data(periodo, "fim", "periodo.fim")),
                despesas(raiz));
    }

    private List<Despesa> despesas(JsonNode raiz) throws EntradaInvalidaException {
        JsonNode lista = raiz.get("despesas");
        if (lista == null || lista.isNull()) {
            throw new EntradaInvalidaException("campo obrigatório ausente: despesas");
        }
        if (!lista.isArray()) {
            throw new EntradaInvalidaException("despesas: deve ser uma lista");
        }

        List<Despesa> despesas = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            String caminho = "despesas[" + i + "]";
            JsonNode no = lista.get(i);
            if (!no.isObject()) {
                throw new EntradaInvalidaException(caminho + ": deve ser um objeto");
            }
            String categoria = texto(no, "categoria", caminho + ".categoria");
            despesas.add(new Despesa(
                    texto(no, "id", caminho + ".id"),
                    data(no, "data", caminho + ".data"),
                    categoria,
                    categoria,
                    texto(no, "descricao", caminho + ".descricao"),
                    texto(no, "fornecedor", caminho + ".fornecedor"),
                    numero(no, "valor", caminho + ".valor"),
                    booleano(no, "tem_nota_fiscal", caminho + ".tem_nota_fiscal")));
        }
        return despesas;
    }

    private static JsonNode obrigatorio(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        JsonNode no = pai.get(campo);
        if (no == null || no.isNull()) {
            throw new EntradaInvalidaException("campo obrigatório ausente: " + caminho);
        }
        return no;
    }

    private static JsonNode objeto(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        JsonNode no = obrigatorio(pai, campo, caminho);
        if (!no.isObject()) {
            throw new EntradaInvalidaException(caminho + ": deve ser um objeto");
        }
        return no;
    }

    private static String texto(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        JsonNode no = obrigatorio(pai, campo, caminho);
        if (!no.isTextual()) {
            throw new EntradaInvalidaException(caminho + ": deve ser texto");
        }
        return no.textValue();
    }

    private static LocalDate data(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        String valor = texto(pai, campo, caminho);
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new EntradaInvalidaException(caminho + ": data inválida, esperado AAAA-MM-DD: " + valor, e);
        }
    }

    private static BigDecimal numero(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        JsonNode no = obrigatorio(pai, campo, caminho);
        if (!no.isNumber()) {
            throw new EntradaInvalidaException(caminho + ": deve ser número");
        }
        return no.decimalValue();
    }

    private static boolean booleano(JsonNode pai, String campo, String caminho) throws EntradaInvalidaException {
        JsonNode no = obrigatorio(pai, campo, caminho);
        if (!no.isBoolean()) {
            throw new EntradaInvalidaException(caminho + ": deve ser true ou false");
        }
        return no.booleanValue();
    }
}
