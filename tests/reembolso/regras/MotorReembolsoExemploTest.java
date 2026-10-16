package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.Entrada;
import reembolso.dominio.Grupo;
import reembolso.dominio.Motivo;
import reembolso.dominio.Resultado;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;
import reembolso.dominio.TabelaCambio;
import reembolso.dominio.TabelaPoliticas;
import reembolso.io.LeitorCambio;
import reembolso.io.LeitorEntrada;
import reembolso.io.LeitorPolitica;

/**
 * T-017 / T-029: o motor completo contra a seção 9 da spec 2.1, com {@code politica-v4.json} e
 * {@code cambio.json}. Cada tabela da seção 9 é um {@code @Nested}.
 */
class MotorReembolsoExemploTest {

    private static TabelaPoliticas tabelas;
    private static TabelaCambio cambio;

    @BeforeAll
    static void lerPoliticaECambio() throws Exception {
        tabelas = new LeitorPolitica().lerArquivo(Path.of("exemplos", "envelope", "politica-v4.json"));
        cambio = new LeitorCambio().lerArquivo(Path.of("exemplos", "envelope", "cambio.json"));
    }

    private static Resultado calcular(Path arquivo) throws Exception {
        Entrada entrada = new LeitorEntrada().lerArquivo(arquivo);
        return new MotorReembolso(tabelas, cambio).calcular(entrada);
    }

    private static ResultadoDespesa despesa(Resultado resultado, String id) {
        return resultado.despesas().stream().filter(r -> r.despesa().id().equals(id)).findFirst().orElseThrow();
    }

    private static void assertStatus(Resultado resultado, String id, Status status, String motivos) {
        ResultadoDespesa r = despesa(resultado, id);
        List<Motivo> esperados = motivos == null ? List.of()
                : Arrays.stream(motivos.split("\\+")).map(String::trim).map(Motivo::valueOf).toList();
        assertEquals(status, r.status(), id);
        assertEquals(esperados, r.motivos(), id);
    }

    private static void assertGrupo(Grupo g, String data, String categoria, List<String> ids, Integer noites,
            String total, String limite, Ampliacao ampliacao, String reembolso) {
        String contexto = data + "/" + categoria;
        assertEquals(LocalDate.parse(data), g.data(), contexto);
        assertEquals(categoria, g.categoria(), contexto);
        assertEquals(ids, g.idsDespesas(), contexto);
        assertEquals(noites, g.noites(), contexto);
        assertEquals(new BigDecimal(total), g.valorTotal(), contexto);
        assertEquals(new BigDecimal(limite), g.limite(), contexto);
        assertEquals(ampliacao, g.ampliacao(), contexto);
        assertEquals(new BigDecimal(reembolso), g.valorReembolsado(), contexto);
    }

    @Nested
    @DisplayName("Seção 9.1: despesas-exemplo.json — CC-ENG-PLATAFORMA")
    class ExemploOriginal {

        private static Resultado resultado;

        @BeforeAll
        static void calcularExemplo() throws Exception {
            resultado = calcular(Path.of("exemplos", "despesas-exemplo.json"));
        }

        @DisplayName("Seção 9.1: status e motivos de cada despesa")
        @ParameterizedTest(name = "{0} → {1} {2}")
        @CsvSource(delimiter = '|', value = {
            "d-001 | APROVADA   | ",
            "d-002 | APROVADA   | ",
            "d-003 | EM_REVISAO | NF_AUSENTE",
            "d-004 | EM_REVISAO | NF_AUSENTE",
            "d-005 | RECUSADA   | CATEGORIA_FORA_DA_POLITICA",
            "d-006 | EM_REVISAO | DUPLICATA",
            "d-007 | EM_REVISAO | DUPLICATA",
            "d-008 | EM_REVISAO | FORA_DO_PERIODO",
            "d-009 | EM_REVISAO | ESTORNO_SEM_DESPESA",
            "d-010 | RECUSADA   | CATEGORIA_FORA_DA_POLITICA",
            "d-011 | APROVADA   | ",
            "d-012 | APROVADA   | ",
            "d-013 | RECUSADA   | CATEGORIA_FORA_DA_POLITICA + NF_AUSENTE",
            "d-014 | APROVADA   | ",
        })
        void status_e_motivos(String id, Status status, String motivos) {
            assertStatus(resultado, id, status, motivos);
        }

        @Test
        @DisplayName("Seção 9.1: 4 grupos — alimentação 75/dia; hospedagem recusada ainda gera viagem em 15/07 (112,50)")
        void grupos() {
            List<Grupo> g = resultado.grupos();
            assertEquals(4, g.size());
            assertGrupo(g.get(0), "2026-07-03", "alimentacao", List.of("d-001", "d-002"), null,
                    "110.50", "75.00", Ampliacao.NENHUM, "75.00");
            assertGrupo(g.get(1), "2026-07-15", "alimentacao", List.of("d-011"), null,
                    "33.33", "112.50", Ampliacao.VIAGEM, "33.33");
            assertGrupo(g.get(2), "2026-07-18", "alimentacao", List.of("d-012"), null,
                    "47.20", "112.50", Ampliacao.FIM_DE_SEMANA_OU_PLANTAO, "47.20");
            assertGrupo(g.get(3), "2026-07-31", "alimentacao", List.of("d-014"), null,
                    "61.00", "75.00", Ampliacao.NENHUM, "61.00");
        }

        @Test
        @DisplayName("Seção 9.1: tabela CC-ENG-PLATAFORMA; total R$ 216,53; 5 aprovadas, 6 em revisão, 3 recusadas")
        void resumo() {
            assertEquals("CC-ENG-PLATAFORMA", resultado.tabelaAplicada());
            assertEquals(new BigDecimal("216.53"), resultado.totalReembolsado());
            assertEquals(Map.of(Status.APROVADA, 5, Status.EM_REVISAO, 6, Status.RECUSADA, 3), resultado.quantidade());
        }

        @Test
        @DisplayName("RN-001 / RN-002: d-014 com categoria normalizada e d-011 arredondado; despesas na ordem da entrada")
        void normalizacao_arredondamento_e_ordem() {
            assertEquals("alimentacao", despesa(resultado, "d-014").despesa().categoria());
            assertEquals(new BigDecimal("33.33"), despesa(resultado, "d-011").despesa().valor());
            assertEquals("d-001", resultado.despesas().get(0).despesa().id());
            assertEquals("d-014", resultado.despesas().get(13).despesa().id());
        }
    }

    @Nested
    @DisplayName("Seção 9.2: despesas-envelope.json — CC-COMERCIAL")
    class Envelope {

        private static Resultado resultado;

        @BeforeAll
        static void calcularEnvelope() throws Exception {
            resultado = calcular(Path.of("exemplos", "envelope", "despesas-envelope.json"));
        }

        @DisplayName("Seção 9.2: status e motivos de cada despesa")
        @ParameterizedTest(name = "{0} → {1} {2}")
        @CsvSource(delimiter = '|', value = {
            "e-001 | APROVADA   | ",
            "e-002 | APROVADA   | ",
            "e-003 | APROVADA   | ",
            "e-004 | APROVADA   | ",
            "e-005 | EM_REVISAO | NF_AUSENTE",
            "e-006 | EM_REVISAO | CAMBIO_INDISPONIVEL",
            "e-007 | APROVADA   | ",
            "e-008 | APROVADA   | ",
            "e-009 | RECUSADA   | CATEGORIA_FORA_DA_POLITICA",
            "e-010 | APROVADA   | ",
        })
        void status_e_motivos(String id, Status status, String motivos) {
            assertStatus(resultado, id, status, motivos);
        }

        @DisplayName("Seção 9.2 / RN-014: conversões (valor em BRL, taxa e data da cotação)")
        @ParameterizedTest(name = "{0}: {1} → {2} (taxa {3} de {4})")
        @CsvSource({
            "e-002, 22.00, 130.46, 5.93, 2026-07-14",
            "e-003, 14.50, 85.26,  5.88, 2026-07-15",
            "e-004, 30.00, 178.80, 5.96, 2026-07-17",
            "e-005, 40.00, 220.00, 5.50, 2026-07-20",
        })
        void conversoes(String id, String original, String brl, String taxa, String dataCotacao) {
            var d = despesa(resultado, id).despesa();
            assertEquals(new BigDecimal(original), d.valorOriginal());
            assertEquals(new BigDecimal(brl), d.valor());
            assertEquals(new BigDecimal(taxa), d.taxaCambio());
            assertEquals(LocalDate.parse(dataCotacao), d.dataCotacao());
        }

        @Test
        @DisplayName("Seção 9.2 / RN-014: e-006 (GBP) sem cotação — valor nulo")
        void gbp_sem_valor() {
            assertNull(despesa(resultado, "e-006").despesa().valor());
        }

        @Test
        @DisplayName("Seção 9.2: 7 grupos — representação 300, alimentação 90 (135 em sábado/viagem), hospedagem 400/noite")
        void grupos() {
            List<Grupo> g = resultado.grupos();
            assertEquals(7, g.size());
            assertGrupo(g.get(0), "2026-07-13", "representacao", List.of("e-001"), null,
                    "340.00", "300.00", Ampliacao.NENHUM, "300.00");
            assertGrupo(g.get(1), "2026-07-14", "alimentacao", List.of("e-002"), null,
                    "130.46", "90.00", Ampliacao.NENHUM, "90.00");
            assertGrupo(g.get(2), "2026-07-15", "alimentacao", List.of("e-003"), null,
                    "85.26", "90.00", Ampliacao.NENHUM, "85.26");
            assertGrupo(g.get(3), "2026-07-18", "alimentacao", List.of("e-004"), null,
                    "178.80", "135.00", Ampliacao.FIM_DE_SEMANA_OU_PLANTAO, "135.00");
            assertGrupo(g.get(4), "2026-07-22", "hospedagem", List.of("e-007"), 3,
                    "1200.00", "1200.00", Ampliacao.NENHUM, "1200.00");
            assertGrupo(g.get(5), "2026-07-23", "alimentacao", List.of("e-008"), null,
                    "95.00", "135.00", Ampliacao.VIAGEM, "95.00");
            assertGrupo(g.get(6), "2026-07-27", "alimentacao", List.of("e-010"), null,
                    "88.00", "90.00", Ampliacao.NENHUM, "88.00");
        }

        @Test
        @DisplayName("Seção 9.2: tabela CC-COMERCIAL; total R$ 1.993,26; 7 aprovadas, 2 em revisão, 1 recusada")
        void resumo() {
            assertEquals("CC-COMERCIAL", resultado.tabelaAplicada());
            assertEquals(new BigDecimal("1993.26"), resultado.totalReembolsado());
            assertEquals(Map.of(Status.APROVADA, 7, Status.EM_REVISAO, 2, Status.RECUSADA, 1), resultado.quantidade());
        }
    }

    @Nested
    @DisplayName("Seção 9.3: despesas-envelope-cc-desconhecido.json — CC-SUPORTE-N2 (tabela padrão)")
    class CentroDeCustoDesconhecido {

        private static Resultado resultado;

        @BeforeAll
        static void calcularDesconhecido() throws Exception {
            resultado = calcular(Path.of("exemplos", "envelope", "despesas-envelope-cc-desconhecido.json"));
        }

        @DisplayName("Seção 9.3: status e motivos de cada despesa")
        @ParameterizedTest(name = "{0} → {1} {2}")
        @CsvSource(delimiter = '|', value = {
            "f-001 | APROVADA | ",
            "f-002 | APROVADA | ",
            "f-003 | RECUSADA | CATEGORIA_FORA_DA_POLITICA",
            "f-004 | APROVADA | ",
        })
        void status_e_motivos(String id, Status status, String motivos) {
            assertStatus(resultado, id, status, motivos);
        }

        @Test
        @DisplayName("Seção 9.3: 3 grupos — alimentação 60, hospedagem 250/noite, transporte USD 12 × 5,48 = 65,76")
        void grupos() {
            List<Grupo> g = resultado.grupos();
            assertEquals(3, g.size());
            assertGrupo(g.get(0), "2026-07-16", "alimentacao", List.of("f-001"), null,
                    "58.00", "60.00", Ampliacao.NENHUM, "58.00");
            assertGrupo(g.get(1), "2026-07-17", "hospedagem", List.of("f-002"), 1,
                    "310.00", "250.00", Ampliacao.NENHUM, "250.00");
            assertGrupo(g.get(2), "2026-07-21", "transporte_urbano", List.of("f-004"), null,
                    "65.76", "80.00", Ampliacao.NENHUM, "65.76");
        }

        @Test
        @DisplayName("Seção 9.3: tabela padrao; total R$ 373,76; 3 aprovadas, 0 em revisão, 1 recusada")
        void resumo() {
            assertEquals("padrao", resultado.tabelaAplicada());
            assertEquals(new BigDecimal("373.76"), resultado.totalReembolsado());
            assertEquals(Map.of(Status.APROVADA, 3, Status.EM_REVISAO, 0, Status.RECUSADA, 1), resultado.quantidade());
        }
    }

    @Test
    @DisplayName("Seção 9.4: duas execuções com a mesma entrada produzem exatamente o mesmo resultado")
    void deterministico() throws Exception {
        Path arquivo = Path.of("exemplos", "envelope", "despesas-envelope.json");

        assertEquals(calcular(arquivo), calcular(arquivo));
    }
}
