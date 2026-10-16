package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reembolso.Despesas.despesa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.Politicas;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.Despesa;
import reembolso.dominio.Grupo;
import reembolso.dominio.Motivo;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/** T-014: RN-004 / AMB-001 / AMB-002 / AMB-003 / AMB-005 — limite diário de alimentação e transporte. */
class Rn004LimiteDiarioTest {

    private static final Set<LocalDate> SEM_VIAGEM = Set.of();

    private static ResultadoDespesa aprovada(Despesa despesa) {
        return new ResultadoDespesa(despesa, Status.APROVADA, List.of(), "");
    }

    private static ResultadoDespesa emRevisao(Despesa despesa, Motivo motivo) {
        return new ResultadoDespesa(despesa, Status.EM_REVISAO, List.of(motivo), "");
    }

    private static List<Grupo> agrupar(ResultadoDespesa... resultados) {
        return LimiteDiario.agrupar(List.of(resultados), SEM_VIAGEM, Politicas.PADRAO);
    }

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("RN-004 / AMB-001 / AMB-002: d-001 72,50 + d-002 38,00 (03/07) → total 110,50, reembolso 60,00")
    void duas_refeicoes_acima_do_limite() {
        var d001 = despesa("d-001", "2026-07-03", "alimentacao", "72.50", true);
        var d002 = despesa("d-002", "2026-07-03", "alimentacao", "38.00", true);

        List<Grupo> grupos = agrupar(aprovada(d001), aprovada(d002));

        assertEquals(1, grupos.size());
        Grupo grupo = grupos.get(0);
        assertEquals(LocalDate.parse("2026-07-03"), grupo.data());
        assertEquals("alimentacao", grupo.categoria());
        assertEquals(List.of("d-001", "d-002"), grupo.idsDespesas());
        assertEquals(valor("110.50"), grupo.valorTotal());
        assertEquals(valor("60.00"), grupo.limite());
        assertEquals(Ampliacao.NENHUM, grupo.ampliacao());
        assertEquals(valor("60.00"), grupo.valorReembolsado());
        assertNull(grupo.noites());
    }

    @Test
    @DisplayName("RN-004: R$ 40,00 de alimentação → reembolsa 40,00")
    void abaixo_do_limite_reembolsa_tudo() {
        Grupo grupo = agrupar(aprovada(despesa("d-1", "2026-07-03", "alimentacao", "40.00", true))).get(0);

        assertEquals(valor("40.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-004 / AMB-005: d-014 R$ 61,00 → reembolsa 60,00 (paga o limite, corta o excedente)")
    void um_real_acima_do_limite() {
        Grupo grupo = agrupar(aprovada(despesa("d-014", "2026-07-31", "alimentacao", "61.00", true))).get(0);

        assertEquals(valor("61.00"), grupo.valorTotal());
        assertEquals(valor("60.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-004: exatamente no limite (60,00) → reembolsa 60,00")
    void exatamente_no_limite() {
        Grupo grupo = agrupar(aprovada(despesa("d-1", "2026-07-03", "alimentacao", "60.00", true))).get(0);

        assertEquals(valor("60.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-004: transporte urbano de R$ 100,00 (com NF) num dia útil → reembolsa 80,00")
    void transporte_acima_do_limite() {
        Grupo grupo = agrupar(aprovada(despesa("d-1", "2026-07-06", "transporte_urbano", "100.00", true))).get(0);

        assertEquals(valor("80.00"), grupo.limite());
        assertEquals(valor("80.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-004: alimentação e transporte no mesmo dia formam grupos separados, cada um com seu limite")
    void categorias_separadas_no_mesmo_dia() {
        var almoco = despesa("d-1", "2026-07-03", "alimentacao", "70.00", true);
        var taxi = despesa("d-2", "2026-07-03", "transporte_urbano", "70.00", true);

        List<Grupo> grupos = agrupar(aprovada(almoco), aprovada(taxi));

        assertEquals(2, grupos.size());
        assertEquals(valor("60.00"), grupos.get(0).valorReembolsado());   // alimentacao
        assertEquals(valor("70.00"), grupos.get(1).valorReembolsado());   // transporte_urbano
    }

    @Test
    @DisplayName("RN-004 / AMB-003: sobra de um dia não passa para o seguinte")
    void sobra_nao_acumula() {
        var dia1 = despesa("d-1", "2026-07-06", "alimentacao", "40.00", true);
        var dia2 = despesa("d-2", "2026-07-07", "alimentacao", "80.00", true);

        List<Grupo> grupos = agrupar(aprovada(dia1), aprovada(dia2));

        assertEquals(valor("40.00"), grupos.get(0).valorReembolsado());
        assertEquals(valor("60.00"), grupos.get(1).limite());
        assertEquals(valor("60.00"), grupos.get(1).valorReembolsado());
    }

    @Test
    @DisplayName("RN-004 / RN-012: despesas em revisão não entram no grupo nem consomem limite")
    void em_revisao_fora_do_grupo() {
        var almoco = despesa("d-1", "2026-07-09", "alimentacao", "54.90", true);
        var outra = despesa("d-2", "2026-07-09", "alimentacao", "54.90", true);

        List<Grupo> grupos = agrupar(aprovada(almoco), emRevisao(outra, Motivo.FORA_DO_PERIODO));

        assertEquals(1, grupos.size());
        assertEquals(List.of("d-1"), grupos.get(0).idsDespesas());
        assertEquals(valor("54.90"), grupos.get(0).valorReembolsado());
    }

    @Test
    @DisplayName("RN-004: dia só com despesas em revisão não forma grupo (d-003 e d-004 sem NF)")
    void dia_so_com_revisao_sem_grupo() {
        var d003 = despesa("d-003", "2026-07-06", "transporte_urbano", "100.00", false);
        var d004 = despesa("d-004", "2026-07-06", "transporte_urbano", "100.01", false);

        assertTrue(agrupar(emRevisao(d003, Motivo.NF_AUSENTE), emRevisao(d004, Motivo.NF_AUSENTE)).isEmpty());
    }

    @Test
    @DisplayName("RN-004: valor zero é aprovado e não altera o reembolso")
    void valor_zero_sem_efeito() {
        var zero = despesa("d-1", "2026-07-03", "alimentacao", "0.00", true);
        var almoco = despesa("d-2", "2026-07-03", "alimentacao", "40.00", true);

        Grupo grupo = agrupar(aprovada(zero), aprovada(almoco)).get(0);

        assertEquals(List.of("d-1", "d-2"), grupo.idsDespesas());
        assertEquals(valor("40.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-004: hospedagem não entra nos grupos diários (tem limite por noite, RN-005)")
    void hospedagem_fora_dos_grupos_diarios() {
        var hotel = despesa("d-010", "2026-07-14", "hospedagem", "480.00", true, "Hotel Rio - 2 diarias");

        assertTrue(agrupar(aprovada(hotel)).isEmpty());
    }

    @Test
    @DisplayName("RN-004 / RN-007: dia de viagem usa o limite ampliado (d-011 em 15/07 → limite 90,00)")
    void dia_de_viagem_usa_limite_ampliado() {
        var d011 = despesa("d-011", "2026-07-15", "alimentacao", "85.00", true);

        Grupo grupo = LimiteDiario.agrupar(List.of(aprovada(d011)), Set.of(LocalDate.parse("2026-07-15")), Politicas.PADRAO)
                .get(0);

        assertEquals(Ampliacao.VIAGEM, grupo.ampliacao());
        assertEquals(valor("90.00"), grupo.limite());
        assertEquals(valor("85.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-004: grupos saem ordenados por data e categoria, independentemente da ordem de entrada")
    void grupos_ordenados() {
        var depois = despesa("d-1", "2026-07-20", "alimentacao", "10.00", true);
        var antes = despesa("d-2", "2026-07-05", "transporte_urbano", "10.00", true);
        var antesAlimentacao = despesa("d-3", "2026-07-05", "alimentacao", "10.00", true);

        List<Grupo> grupos = agrupar(aprovada(depois), aprovada(antes), aprovada(antesAlimentacao));

        assertEquals(List.of("d-3"), grupos.get(0).idsDespesas());
        assertEquals(List.of("d-2"), grupos.get(1).idsDespesas());
        assertEquals(List.of("d-1"), grupos.get(2).idsDespesas());
    }


    @Test
    @DisplayName("RN-004 / AMB-026: representação no CC-COMERCIAL (periodicidade dia) forma grupo diário — 340 → 300")
    void representacao_forma_grupo_diario() {
        var jantar = despesa("e-001", "2026-07-13", "representacao", "340.00", true);

        Grupo grupo = LimiteDiario.agrupar(List.of(aprovada(jantar)), SEM_VIAGEM, Politicas.COMERCIAL).get(0);

        assertEquals("representacao", grupo.categoria());
        assertEquals(valor("300.00"), grupo.limite());
        assertEquals(valor("300.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-004: o limite diário vem da tabela aplicada (CC-COMERCIAL: alimentação 95 → 90)")
    void limite_da_tabela_aplicada() {
        var almoco = despesa("d-1", "2026-07-06", "alimentacao", "95.00", true);

        Grupo grupo = LimiteDiario.agrupar(List.of(aprovada(almoco)), SEM_VIAGEM, Politicas.COMERCIAL).get(0);

        assertEquals(valor("90.00"), grupo.valorReembolsado());
    }
}
