package reembolso.io;

import static reembolso.io.Json.objeto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import reembolso.dominio.TabelaCambio;

/**
 * Lê e valida o arquivo de câmbio (spec, seção 4; plano, DT-008).
 *
 * <p>{@code moeda_base}, {@code fonte} e {@code observacao} são informativos e ignorados.
 */
public final class LeitorCambio {

    public TabelaCambio lerArquivo(Path arquivo) throws EntradaInvalidaException {
        return lerTexto(Json.lerArquivo(arquivo, "de câmbio"));
    }

    public TabelaCambio lerTexto(String json) throws EntradaInvalidaException {
        JsonNode raiz = Json.raiz(json, "o câmbio");
        JsonNode taxas = objeto(raiz, "taxas", "taxas");

        Map<LocalDate, Map<String, BigDecimal>> porData = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = taxas.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> dia = it.next();
            String caminho = "taxas." + dia.getKey();
            LocalDate data = Json.comoData(dia.getKey(), caminho);
            JsonNode moedas = Json.comoObjeto(dia.getValue(), caminho);

            Map<String, BigDecimal> porMoeda = new LinkedHashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> m = moedas.fields(); m.hasNext(); ) {
                Map.Entry<String, JsonNode> moeda = m.next();
                String caminhoMoeda = caminho + "." + moeda.getKey();
                BigDecimal taxa = Json.comoNumero(moeda.getValue(), caminhoMoeda);
                if (taxa.signum() <= 0) {
                    throw new EntradaInvalidaException(caminhoMoeda + ": a taxa deve ser maior que zero: "
                            + taxa.toPlainString());
                }
                porMoeda.put(moeda.getKey(), taxa);
            }
            porData.put(data, porMoeda);
        }
        return new TabelaCambio(porData);
    }
}
