package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static reembolso.Despesas.despesa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.dominio.Despesa;
import reembolso.dominio.Grupo;
import reembolso.dominio.Motivo;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/** T-016: RN-011 / AMB-019 / D-004 — estornos abatem do grupo do dia ou vão para revisão. */
class Rn011EstornoTest {

    private static ResultadoDespesa aprovada(Despesa despesa) {
        return new ResultadoDespesa(despesa, Status.APROVADA, List.of(), "");
    }

    private static ResultadoDespesa resultadoDe(List<ResultadoDespesa> resultados, String id) {
        return resultados.stream().filter(r -> r.despesa().id().equals(id)).findFirst().orElseThrow();
    }

    private static List<Grupo> grupos(List<ResultadoDespesa> resultados) {
        return LimiteDiario.agrupar(resultados, Set.of(), Politica.V3);
    }

    @Test
    @DisplayName("RN-011: transporte 100,00 (NF) + estorno −45,00 no mesmo dia → total 55,00, reembolsa 55,00")
    void estorno_abate_do_dia() {
        var taxi = despesa("d-1", "2026-07-06", "transporte_urbano", "100.00", true);
        var estorno = despesa("d-2", "2026-07-06", "transporte_urbano", "-45.00", false);

        List<ResultadoDespesa> resultados = Estornos.aplicar(List.of(aprovada(taxi), aprovada(estorno)));

        assertEquals(Status.APROVADA, resultadoDe(resultados, "d-2").status());
        Grupo grupo = grupos(resultados).get(0);
        assertEquals(List.of("d-1", "d-2"), grupo.idsDespesas());
        assertEquals(new BigDecimal("55.00"), grupo.valorTotal());
        assertEquals(new BigDecimal("55.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-011 / AMB-019: d-009 −45,00 sozinho no dia (11/07) → ESTORNO_SEM_DESPESA, sem grupo")
    void estorno_sozinho_vai_para_revisao() {
        var d009 = despesa("d-009", "2026-07-11", "transporte_urbano", "-45.00", false);

        List<ResultadoDespesa> resultados = Estornos.aplicar(List.of(aprovada(d009)));

        ResultadoDespesa resultado = resultados.get(0);
        assertEquals(Status.EM_REVISAO, resultado.status());
        assertEquals(List.of(Motivo.ESTORNO_SEM_DESPESA), resultado.motivos());
        assertEquals(List.of(), grupos(resultados));
    }

    @Test
    @DisplayName("RN-011: estorno maior que a soma do dia → ESTORNO_SEM_DESPESA; o grupo fica só com a despesa")
    void estorno_maior_que_o_dia() {
        var taxi = despesa("d-1", "2026-07-06", "transporte_urbano", "30.00", true);
        var estorno = despesa("d-2", "2026-07-06", "transporte_urbano", "-45.00", false);

        List<ResultadoDespesa> resultados = Estornos.aplicar(List.of(aprovada(taxi), aprovada(estorno)));

        assertEquals(Status.EM_REVISAO, resultadoDe(resultados, "d-2").status());
        Grupo grupo = grupos(resultados).get(0);
        assertEquals(List.of("d-1"), grupo.idsDespesas());
        assertEquals(new BigDecimal("30.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-011: estorno igual à soma do dia é abatido → total 0,00")
    void estorno_igual_ao_dia() {
        var taxi = despesa("d-1", "2026-07-06", "transporte_urbano", "45.00", true);
        var estorno = despesa("d-2", "2026-07-06", "transporte_urbano", "-45.00", false);

        List<ResultadoDespesa> resultados = Estornos.aplicar(List.of(aprovada(taxi), aprovada(estorno)));

        assertEquals(Status.APROVADA, resultadoDe(resultados, "d-2").status());
        assertEquals(new BigDecimal("0.00"), grupos(resultados).get(0).valorReembolsado());
    }

    @Test
    @DisplayName("RN-011 / D-004: 50,00 + −45,00 + −45,00 → os dois estornos em revisão; grupo reembolsa 50,00")
    void dois_estornos_que_juntos_excedem() {
        var taxi = despesa("d-1", "2026-07-06", "transporte_urbano", "50.00", true);
        var estorno1 = despesa("d-2", "2026-07-06", "transporte_urbano", "-45.00", false);
        var estorno2 = despesa("d-3", "2026-07-06", "transporte_urbano", "-45.00", false, "Outro estorno");

        List<ResultadoDespesa> resultados =
                Estornos.aplicar(List.of(aprovada(taxi), aprovada(estorno1), aprovada(estorno2)));

        assertEquals(Status.EM_REVISAO, resultadoDe(resultados, "d-2").status());
        assertEquals(Status.EM_REVISAO, resultadoDe(resultados, "d-3").status());
        assertEquals(new BigDecimal("50.00"), grupos(resultados).get(0).valorReembolsado());
    }

    @Test
    @DisplayName("RN-011 / D-004: 100,00 + −30,00 + −20,00 → total 50,00, reembolsa 50,00")
    void dois_estornos_que_cabem() {
        var taxi = despesa("d-1", "2026-07-06", "transporte_urbano", "100.00", true);
        var estorno1 = despesa("d-2", "2026-07-06", "transporte_urbano", "-30.00", false);
        var estorno2 = despesa("d-3", "2026-07-06", "transporte_urbano", "-20.00", false);

        List<ResultadoDespesa> resultados =
                Estornos.aplicar(List.of(aprovada(taxi), aprovada(estorno1), aprovada(estorno2)));

        Grupo grupo = grupos(resultados).get(0);
        assertEquals(new BigDecimal("50.00"), grupo.valorTotal());
        assertEquals(new BigDecimal("50.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-011: estorno abate antes do limite — alimentação 100,00 + −30,00 → total 70,00, reembolsa 60,00")
    void estorno_entra_antes_do_limite() {
        var jantar = despesa("d-1", "2026-07-03", "alimentacao", "100.00", true);
        var estorno = despesa("d-2", "2026-07-03", "alimentacao", "-30.00", false);

        Grupo grupo = grupos(Estornos.aplicar(List.of(aprovada(jantar), aprovada(estorno)))).get(0);

        assertEquals(new BigDecimal("70.00"), grupo.valorTotal());
        assertEquals(new BigDecimal("60.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-011: estorno não abate de outra categoria nem de outro dia")
    void estorno_so_abate_mesma_categoria_e_dia() {
        var almoco = despesa("d-1", "2026-07-06", "alimentacao", "50.00", true);
        var taxiOutroDia = despesa("d-2", "2026-07-07", "transporte_urbano", "50.00", true);
        var estorno = despesa("d-3", "2026-07-06", "transporte_urbano", "-20.00", false);

        List<ResultadoDespesa> resultados =
                Estornos.aplicar(List.of(aprovada(almoco), aprovada(taxiOutroDia), aprovada(estorno)));

        assertEquals(Status.EM_REVISAO, resultadoDe(resultados, "d-3").status());
    }

    @Test
    @DisplayName("RN-011: estorno de hospedagem → sempre ESTORNO_SEM_DESPESA")
    void estorno_de_hospedagem() {
        var hotel = despesa("d-1", "2026-07-14", "hospedagem", "480.00", true, "Hotel 2 noites");
        var estorno = despesa("d-2", "2026-07-14", "hospedagem", "-100.00", false, "Estorno hotel 2 noites");

        List<ResultadoDespesa> resultados = Estornos.aplicar(List.of(aprovada(hotel), aprovada(estorno)));

        assertEquals(Status.APROVADA, resultadoDe(resultados, "d-1").status());
        assertEquals(Status.EM_REVISAO, resultadoDe(resultados, "d-2").status());
        assertEquals(List.of(Motivo.ESTORNO_SEM_DESPESA), resultadoDe(resultados, "d-2").motivos());
    }

    @Test
    @DisplayName("RN-011 / RN-012: despesa em revisão não serve de saldo para o estorno")
    void despesa_em_revisao_nao_e_saldo() {
        var taxiSemNf = despesa("d-1", "2026-07-06", "transporte_urbano", "100.00", false);
        var estorno = despesa("d-2", "2026-07-06", "transporte_urbano", "-45.00", false);
        var taxiEmRevisao = new ResultadoDespesa(taxiSemNf, Status.EM_REVISAO, List.of(Motivo.NF_AUSENTE), "");

        List<ResultadoDespesa> resultados = Estornos.aplicar(List.of(taxiEmRevisao, aprovada(estorno)));

        assertEquals(Status.EM_REVISAO, resultadoDe(resultados, "d-2").status());
    }

    @Test
    @DisplayName("RN-011 / RN-012 / D-005: estorno já em revisão (fora do período) também recebe ESTORNO_SEM_DESPESA")
    void estorno_ja_em_revisao_recebe_o_motivo() {
        var estorno = despesa("d-1", "2026-04-15", "transporte_urbano", "-45.00", false);
        var emRevisao = new ResultadoDespesa(estorno, Status.EM_REVISAO, List.of(Motivo.FORA_DO_PERIODO), "");

        ResultadoDespesa resultado = Estornos.aplicar(List.of(emRevisao)).get(0);

        assertEquals(Status.EM_REVISAO, resultado.status());
        assertEquals(List.of(Motivo.FORA_DO_PERIODO, Motivo.ESTORNO_SEM_DESPESA), resultado.motivos());
    }

    @Test
    @DisplayName("RN-011 / D-005: estorno recusado (categoria fora) recebe o motivo e continua recusado")
    void estorno_recusado_continua_recusado() {
        var estorno = despesa("d-1", "2026-07-07", "coworking", "-20.00", false);
        var recusado = new ResultadoDespesa(estorno, Status.RECUSADA, List.of(Motivo.CATEGORIA_FORA_DA_POLITICA), "");

        ResultadoDespesa resultado = Estornos.aplicar(List.of(recusado)).get(0);

        assertEquals(Status.RECUSADA, resultado.status());
        assertEquals(List.of(Motivo.CATEGORIA_FORA_DA_POLITICA, Motivo.ESTORNO_SEM_DESPESA), resultado.motivos());
    }

    @Test
    @DisplayName("RN-011: despesa positiva em revisão não recebe ESTORNO_SEM_DESPESA")
    void despesa_positiva_em_revisao_inalterada() {
        var taxi = despesa("d-1", "2026-07-06", "transporte_urbano", "100.00", false);
        var emRevisao = new ResultadoDespesa(taxi, Status.EM_REVISAO, List.of(Motivo.NF_AUSENTE), "");

        assertEquals(emRevisao, Estornos.aplicar(List.of(emRevisao)).get(0));
    }

    @Test
    @DisplayName("RN-011: a ordem dos resultados é preservada")
    void ordem_preservada() {
        var estorno = despesa("d-1", "2026-07-11", "transporte_urbano", "-45.00", false);
        var almoco = despesa("d-2", "2026-07-03", "alimentacao", "40.00", true);

        List<ResultadoDespesa> resultados = Estornos.aplicar(List.of(aprovada(estorno), aprovada(almoco)));

        assertEquals(List.of("d-1", "d-2"), resultados.stream().map(r -> r.despesa().id()).toList());
    }
}
