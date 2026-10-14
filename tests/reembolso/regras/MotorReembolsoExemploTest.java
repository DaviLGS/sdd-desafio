package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
import reembolso.io.LeitorEntrada;

/** T-017: o motor completo contra a tabela da seção 9 da spec, com {@code exemplos/despesas-exemplo.json}. */
class MotorReembolsoExemploTest {

    private static Entrada entrada;
    private static Resultado resultado;

    @BeforeAll
    static void calcular() throws Exception {
        entrada = new LeitorEntrada().lerArquivo(Path.of("exemplos", "despesas-exemplo.json"));
        resultado = new MotorReembolso(Politica.V3).calcular(entrada);
    }

    private static ResultadoDespesa despesa(String id) {
        return resultado.despesas().stream().filter(r -> r.despesa().id().equals(id)).findFirst().orElseThrow();
    }

    @DisplayName("Seção 9: status e motivos de cada despesa")
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
        "d-010 | APROVADA   | ",
        "d-011 | APROVADA   | ",
        "d-012 | APROVADA   | ",
        "d-013 | EM_REVISAO | NF_AUSENTE",
        "d-014 | APROVADA   | ",
    })
    void status_e_motivos(String id, Status status, String motivo) {
        ResultadoDespesa r = despesa(id);
        List<Motivo> esperados = motivo == null ? List.of() : List.of(Motivo.valueOf(motivo));

        assertEquals(status, r.status());
        assertEquals(esperados, r.motivos());
    }

    @Test
    @DisplayName("Seção 9: os 5 grupos, em ordem, com total, limite, ampliação e reembolso")
    void cinco_grupos() {
        List<Grupo> grupos = resultado.grupos();
        assertEquals(5, grupos.size());

        assertGrupo(grupos.get(0), "2026-07-03", "alimentacao", List.of("d-001", "d-002"), null,
                "110.50", "60.00", Ampliacao.NENHUM, "60.00");
        assertGrupo(grupos.get(1), "2026-07-14", "hospedagem", List.of("d-010"), 2,
                "480.00", "500.00", Ampliacao.NENHUM, "480.00");
        assertGrupo(grupos.get(2), "2026-07-15", "alimentacao", List.of("d-011"), null,
                "33.33", "90.00", Ampliacao.VIAGEM, "33.33");
        assertGrupo(grupos.get(3), "2026-07-18", "alimentacao", List.of("d-012"), null,
                "47.20", "90.00", Ampliacao.FIM_DE_SEMANA_OU_PLANTAO, "47.20");
        assertGrupo(grupos.get(4), "2026-07-31", "alimentacao", List.of("d-014"), null,
                "61.00", "60.00", Ampliacao.NENHUM, "60.00");
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

    @Test
    @DisplayName("Seção 9: total reembolsado = R$ 680,53")
    void total_reembolsado() {
        assertEquals(new BigDecimal("680.53"), resultado.totalReembolsado());
    }

    @Test
    @DisplayName("Seção 9: quantidade — 6 aprovadas, 7 em revisão, 1 recusada")
    void quantidade_por_status() {
        assertEquals(Map.of(Status.APROVADA, 6, Status.EM_REVISAO, 7, Status.RECUSADA, 1), resultado.quantidade());
    }

    @Test
    @DisplayName("Seção 9: toda despesa da entrada aparece na saída, na mesma ordem")
    void todas_as_despesas_na_mesma_ordem() {
        assertEquals(
                entrada.despesas().stream().map(d -> d.id()).toList(),
                resultado.despesas().stream().map(r -> r.despesa().id()).toList());
    }

    @Test
    @DisplayName("RN-001 / RN-002: na saída, d-014 tem categoria normalizada e d-011 tem valor arredondado")
    void normalizacao_e_arredondamento_aplicados() {
        assertEquals("alimentacao", despesa("d-014").despesa().categoria());
        assertEquals("ALIMENTACAO", despesa("d-014").despesa().categoriaOriginal());
        assertEquals(new BigDecimal("33.33"), despesa("d-011").despesa().valor());
    }

    @Test
    @DisplayName("Seção 9: duas execuções com a mesma entrada produzem exatamente o mesmo resultado")
    void deterministico() {
        Resultado outra = new MotorReembolso(Politica.V3).calcular(entrada);

        assertEquals(resultado, outra);
    }
}
