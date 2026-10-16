package reembolso.io;

import static reembolso.io.Json.numero;
import static reembolso.io.Json.objeto;
import static reembolso.io.Json.texto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import reembolso.dominio.LimiteCategoria;
import reembolso.dominio.Periodicidade;
import reembolso.dominio.TabelaPoliticas;

/**
 * Lê e valida o arquivo de política (spec, seção 4; plano, DT-007).
 *
 * <p>Só valida a forma; nomes de centro de custo e de categoria ficam como estão no arquivo.
 * {@code versao}, {@code vigencia}, {@code moeda_base} e {@code observacao} são ignorados (AMB-029).
 */
public final class LeitorPolitica {

    public TabelaPoliticas lerArquivo(Path arquivo) throws EntradaInvalidaException {
        return lerTexto(Json.lerArquivo(arquivo, "de política"));
    }

    public TabelaPoliticas lerTexto(String json) throws EntradaInvalidaException {
        JsonNode raiz = Json.raiz(json, "a política");

        Map<String, LimiteCategoria> padrao = tabela(objeto(raiz, "padrao", "padrao"), "padrao");

        Map<String, Map<String, LimiteCategoria>> centrosCusto = new LinkedHashMap<>();
        JsonNode centros = raiz.get("centros_custo");
        if (centros != null && !centros.isNull()) {
            Json.comoObjeto(centros, "centros_custo");
            for (Iterator<Map.Entry<String, JsonNode>> it = centros.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> centro = it.next();
                String caminho = "centros_custo." + centro.getKey();
                centrosCusto.put(centro.getKey(), tabela(Json.comoObjeto(centro.getValue(), caminho), caminho));
            }
        }

        BigDecimal piso = naoNegativo(numero(raiz, "nota_fiscal_obrigatoria_acima_de",
                "nota_fiscal_obrigatoria_acima_de"), "nota_fiscal_obrigatoria_acima_de");
        BigDecimal percentual = naoNegativo(numero(raiz, "acrescimo_em_viagem_percentual",
                "acrescimo_em_viagem_percentual"), "acrescimo_em_viagem_percentual");

        return new TabelaPoliticas(centrosCusto, padrao, piso, percentual);
    }

    private static Map<String, LimiteCategoria> tabela(JsonNode no, String caminho) throws EntradaInvalidaException {
        Map<String, LimiteCategoria> categorias = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = no.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> categoria = it.next();
            String caminhoCategoria = caminho + "." + categoria.getKey();
            JsonNode limite = Json.comoObjeto(categoria.getValue(), caminhoCategoria);
            categorias.put(categoria.getKey(), new LimiteCategoria(
                    naoNegativo(numero(limite, "limite", caminhoCategoria + ".limite"), caminhoCategoria + ".limite"),
                    periodicidade(texto(limite, "periodicidade", caminhoCategoria + ".periodicidade"),
                            caminhoCategoria + ".periodicidade")));
        }
        return categorias;
    }

    private static Periodicidade periodicidade(String valor, String caminho) throws EntradaInvalidaException {
        for (Periodicidade periodicidade : Periodicidade.values()) {
            if (periodicidade.codigo().equals(valor)) {
                return periodicidade;
            }
        }
        throw new EntradaInvalidaException(caminho + ": esperado \"dia\" ou \"diaria\": " + valor);
    }

    private static BigDecimal naoNegativo(BigDecimal valor, String caminho) throws EntradaInvalidaException {
        if (valor.signum() < 0) {
            throw new EntradaInvalidaException(caminho + ": não pode ser negativo: " + valor.toPlainString());
        }
        return valor;
    }
}
