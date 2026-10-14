package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static reembolso.Despesas.despesa;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reembolso.dominio.Motivo;
import reembolso.dominio.Periodo;
import reembolso.dominio.Status;

/** T-007: RN-010 / AMB-017 / AMB-018 — período de competência inclusivo. */
class Rn010PeriodoCompetenciaTest {

    private static final Periodo JULHO = new Periodo("2026-07", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

    private static Optional<Motivo> verificar(String data) {
        return PeriodoCompetencia.verificar(despesa("d-1", data, "alimentacao", "10.00", true), JULHO);
    }

    @Test
    @DisplayName("RN-010 / AMB-018: primeiro dia do período (inicio) está dentro")
    void inicio_dentro() {
        assertEquals(Optional.empty(), verificar("2026-07-01"));
    }

    @Test
    @DisplayName("RN-010 / AMB-018: último dia do período (fim, d-014 = 31/07) está dentro")
    void fim_dentro() {
        assertEquals(Optional.empty(), verificar("2026-07-31"));
    }

    @Test
    @DisplayName("RN-010: dia no meio do período está dentro")
    void meio_dentro() {
        assertEquals(Optional.empty(), verificar("2026-07-15"));
    }

    @DisplayName("RN-010: datas fora do período → FORA_DO_PERIODO")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "2026-06-30",   // dia anterior ao inicio
        "2026-08-01",   // dia posterior ao fim
        "2026-04-15",   // d-008: despesa de abril lançada com atraso (AMB-017)
        "2025-07-15",   // mesmo mês, outro ano
    })
    void fora_do_periodo(String data) {
        assertEquals(Optional.of(Motivo.FORA_DO_PERIODO), verificar(data));
    }

    @Test
    @DisplayName("RN-010 / AMB-017: fora do período leva a em_revisao, não a recusada")
    void fora_do_periodo_leva_a_revisao() {
        assertEquals(Status.EM_REVISAO, Motivo.FORA_DO_PERIODO.statusResultante());
    }
}
