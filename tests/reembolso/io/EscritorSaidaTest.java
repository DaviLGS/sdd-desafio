package reembolso.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reembolso.Politicas;
import reembolso.dominio.Resultado;
import reembolso.regras.MotorReembolso;

/** T-019: escrita da saída JSON no schema da seção 4 da spec. */
class EscritorSaidaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);

    /** Entrada do exemplo da seção 4 da spec: duas refeições num dia útil, sem viagem. */
    private static final String ENTRADA_DA_SPEC = """
            {
              "colaborador": { "id": "c-1", "nome": "Fulana", "centro_custo": "CC-X" },
              "periodo": { "competencia": "2026-07", "inicio": "2026-07-01", "fim": "2026-07-31" },
              "despesas": [
                { "id": "d-1", "data": "2026-07-03", "categoria": "alimentacao", "descricao": "Almoco",
                  "fornecedor": "Restaurante", "valor": 72.50, "tem_nota_fiscal": true },
                { "id": "d-2", "data": "2026-07-03", "categoria": "alimentacao", "descricao": "Jantar",
                  "fornecedor": "Cantina", "valor": 38.00, "tem_nota_fiscal": true }
              ]
            }
            """;

    /** Saída copiada do exemplo da seção 4 da spec. */
    private static final String SAIDA_DA_SPEC = """
            {
              "colaborador": { "id": "c-1", "nome": "Fulana", "centro_custo": "CC-X" },
              "periodo": { "competencia": "2026-07", "inicio": "2026-07-01", "fim": "2026-07-31" },
              "despesas": [
                { "id": "d-1", "data": "2026-07-03", "categoria": "alimentacao", "valor": 72.50,
                  "status": "aprovada", "motivos": [], "justificativa": "Aprovada; limite aplicado no grupo 2026-07-03/alimentacao." },
                { "id": "d-2", "data": "2026-07-03", "categoria": "alimentacao", "valor": 38.00,
                  "status": "aprovada", "motivos": [], "justificativa": "Aprovada; limite aplicado no grupo 2026-07-03/alimentacao." }
              ],
              "grupos": [
                { "data": "2026-07-03", "categoria": "alimentacao", "despesas": ["d-1", "d-2"],
                  "valor_total": 110.50, "limite": 60.00, "limite_ampliado": "nenhum",
                  "valor_reembolsado": 60.00 }
              ],
              "resumo": { "total_reembolsado": 60.00,
                          "quantidade": { "aprovada": 2, "em_revisao": 0, "recusada": 0 } }
            }
            """;

    private final EscritorSaida escritor = new EscritorSaida();

    private static Resultado calcular(String json) throws Exception {
        return new MotorReembolso(Politicas.SO_PADRAO, Politicas.SEM_CAMBIO).calcular(new LeitorEntrada().lerTexto(json));
    }

    private static Resultado calcularExemplo() throws Exception {
        var entrada = new LeitorEntrada().lerArquivo(Path.of("exemplos", "despesas-exemplo.json"));
        return new MotorReembolso(Politicas.SO_PADRAO, Politicas.SEM_CAMBIO).calcular(entrada);
    }

    @Test
    @DisplayName("Seção 4: o exemplo de saída da spec é reproduzido campo a campo")
    void reproduz_exemplo_da_spec() throws Exception {
        String saida = escritor.paraTexto(calcular(ENTRADA_DA_SPEC));

        assertEquals(MAPPER.readTree(SAIDA_DA_SPEC), MAPPER.readTree(saida));
    }

    @Test
    @DisplayName("Seção 4 / seção 9: valores monetários com exatamente 2 casas (60 → 60.00)")
    void valores_com_duas_casas() throws Exception {
        String saida = escritor.paraTexto(calcularExemplo());

        assertTrue(saida.contains("\"total_reembolsado\" : 680.53"), saida);
        assertTrue(saida.contains("\"limite\" : 60.00"), "limite inteiro deve sair com 2 casas");
        assertTrue(saida.contains("\"valor\" : 33.33"), "d-011 sai arredondado");
        assertTrue(saida.contains("\"valor\" : -45.00"), "estorno sai com sinal e 2 casas");
        assertFalse(saida.matches("(?s).*\"(valor|valor_total|limite|valor_reembolsado|total_reembolsado)\" : -?\\d+(\\.\\d)?[,\\s].*"),
                "nenhum valor monetário com 0 ou 1 casa decimal");
    }

    @Test
    @DisplayName("Seção 4: status em minúsculas e motivos com os códigos da spec")
    void status_e_motivos_com_codigos_da_spec() throws Exception {
        JsonNode raiz = MAPPER.readTree(escritor.paraTexto(calcularExemplo()));

        JsonNode d005 = raiz.get("despesas").get(4);
        assertEquals("d-005", d005.get("id").asText());
        assertEquals("recusada", d005.get("status").asText());
        assertEquals("CATEGORIA_FORA_DA_POLITICA", d005.get("motivos").get(0).asText());

        JsonNode d009 = raiz.get("despesas").get(8);
        assertEquals("em_revisao", d009.get("status").asText());
        assertEquals("ESTORNO_SEM_DESPESA", d009.get("motivos").get(0).asText());
    }

    @Test
    @DisplayName("Seção 4: categoria normalizada na saída (d-014 ALIMENTACAO → alimentacao)")
    void categoria_normalizada_na_saida() throws Exception {
        JsonNode d014 = MAPPER.readTree(escritor.paraTexto(calcularExemplo())).get("despesas").get(13);

        assertEquals("d-014", d014.get("id").asText());
        assertEquals("alimentacao", d014.get("categoria").asText());
    }

    @Test
    @DisplayName("Seção 4: noites só aparece no grupo de hospedagem; limite_ampliado com os códigos da spec")
    void noites_so_em_hospedagem() throws Exception {
        JsonNode grupos = MAPPER.readTree(escritor.paraTexto(calcularExemplo())).get("grupos");

        for (JsonNode grupo : grupos) {
            boolean hospedagem = grupo.get("categoria").asText().equals("hospedagem");
            assertEquals(hospedagem, grupo.has("noites"), grupo.toString());
        }
        assertEquals(2, grupos.get(1).get("noites").asInt());
        assertEquals("viagem", grupos.get(2).get("limite_ampliado").asText());
        assertEquals("fim_de_semana_ou_plantao", grupos.get(3).get("limite_ampliado").asText());
    }

    @Test
    @DisplayName("Seção 4: resumo traz total e quantidade por status (680.53; 6/7/1)")
    void resumo() throws Exception {
        JsonNode resumo = MAPPER.readTree(escritor.paraTexto(calcularExemplo())).get("resumo");

        assertEquals("680.53", resumo.get("total_reembolsado").decimalValue().toPlainString());
        assertEquals(6, resumo.get("quantidade").get("aprovada").asInt());
        assertEquals(7, resumo.get("quantidade").get("em_revisao").asInt());
        assertEquals(1, resumo.get("quantidade").get("recusada").asInt());
    }

    @Test
    @DisplayName("Seção 4: escreve o arquivo em UTF-8 (acentos da justificativa preservados)")
    void escreve_arquivo_utf8(@TempDir Path pasta) throws Exception {
        Path arquivo = pasta.resolve("resultado.json");

        escritor.escrever(calcularExemplo(), arquivo);

        String conteudo = Files.readString(arquivo);
        assertTrue(conteudo.contains("Em revisão"), "acento preservado");
        assertEquals(14, MAPPER.readTree(conteudo).get("despesas").size());
    }
}
