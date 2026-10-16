package reembolso.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import reembolso.dominio.Grupo;
import reembolso.dominio.Motivo;
import reembolso.dominio.Resultado;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/** Serializa o {@link Resultado} no schema de saída da seção 4 da spec. */
public final class EscritorSaida {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

    public void escrever(Resultado resultado, Path arquivo) throws IOException {
        Files.writeString(arquivo, paraTexto(resultado) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public String paraTexto(Resultado resultado) {
        try {
            return mapper.writeValueAsString(paraJson(resultado));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("falha ao serializar o resultado", e);
        }
    }

    ObjectNode paraJson(Resultado resultado) {
        ObjectNode raiz = mapper.createObjectNode();

        ObjectNode colaborador = raiz.putObject("colaborador");
        colaborador.put("id", resultado.colaborador().id());
        colaborador.put("nome", resultado.colaborador().nome());
        colaborador.put("centro_custo", resultado.colaborador().centroCusto());

        ObjectNode periodo = raiz.putObject("periodo");
        periodo.put("competencia", resultado.periodo().competencia());
        periodo.put("inicio", resultado.periodo().inicio().toString());
        periodo.put("fim", resultado.periodo().fim().toString());

        ArrayNode despesas = raiz.putArray("despesas");
        for (ResultadoDespesa r : resultado.despesas()) {
            ObjectNode no = despesas.addObject();
            no.put("id", r.despesa().id());
            no.put("data", r.despesa().data().toString());
            no.put("categoria", r.despesa().categoria());
            no.put("valor", dinheiro(r.despesa().valor()));
            if (r.despesa().emMoedaEstrangeira()) {
                // AMB-037: campos de câmbio só em despesa estrangeira; repetem a entrada e o câmbio usados.
                no.put("moeda", r.despesa().moeda());
                no.put("valor_original", r.despesa().valorOriginal());
                no.put("taxa_cambio", r.despesa().taxaCambio());
                no.put("data_cotacao", r.despesa().dataCotacao() == null ? null : r.despesa().dataCotacao().toString());
            }
            no.put("status", r.status().codigo());
            ArrayNode motivos = no.putArray("motivos");
            for (Motivo motivo : r.motivos()) {
                motivos.add(motivo.name());
            }
            no.put("justificativa", r.justificativa());
        }

        ArrayNode grupos = raiz.putArray("grupos");
        for (Grupo g : resultado.grupos()) {
            ObjectNode no = grupos.addObject();
            no.put("data", g.data().toString());
            no.put("categoria", g.categoria());
            ArrayNode ids = no.putArray("despesas");
            g.idsDespesas().forEach(ids::add);
            if (g.noites() != null) {
                no.put("noites", g.noites());
            }
            no.put("valor_total", dinheiro(g.valorTotal()));
            no.put("limite", dinheiro(g.limite()));
            no.put("limite_ampliado", g.ampliacao().codigo());
            no.put("valor_reembolsado", dinheiro(g.valorReembolsado()));
        }

        ObjectNode resumo = raiz.putObject("resumo");
        resumo.put("tabela_aplicada", resultado.tabelaAplicada());
        resumo.put("total_reembolsado", dinheiro(resultado.totalReembolsado()));
        ObjectNode quantidade = resumo.putObject("quantidade");
        for (Status status : Status.values()) {
            quantidade.put(status.codigo(), resultado.quantidade().getOrDefault(status, 0));
        }
        return raiz;
    }

    /** Todo valor monetário da saída tem exatamente 2 casas (spec, seção 9); sem cotação, {@code null}. */
    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.UNNECESSARY);
    }
}
