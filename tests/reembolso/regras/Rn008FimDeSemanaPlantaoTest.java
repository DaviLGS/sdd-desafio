package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static reembolso.Despesas.despesa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.Despesa;

/** T-013: RN-008 / AMB-014 / AMB-015 — ampliação por fim de semana ou plantão, sem acumular. */
class Rn008FimDeSemanaPlantaoTest {

    private static final Set<LocalDate> SEM_VIAGEM = Set.of();

    private static Ampliacao ampliacao(Despesa despesa, Set<LocalDate> diasViagem) {
        return AmpliacaoLimite.determinar(despesa.data(), despesa.categoria(), List.of(despesa), diasViagem);
    }

    @Test
    @DisplayName("RN-008: sábado (d-012, 18/07) → FIM_DE_SEMANA_OU_PLANTAO, limite de alimentação 90,00")
    void sabado_amplia() {
        var d012 = despesa("d-012", "2026-07-18", "alimentacao", "47.20", true, "Almoco de sabado - plantao");

        Ampliacao ampliacao = ampliacao(d012, SEM_VIAGEM);

        assertEquals(Ampliacao.FIM_DE_SEMANA_OU_PLANTAO, ampliacao);
        assertEquals(new BigDecimal("90.00"), Politica.V3.limiteDiario("alimentacao", ampliacao));
    }

    @Test
    @DisplayName("RN-008: domingo → FIM_DE_SEMANA_OU_PLANTAO")
    void domingo_amplia() {
        var almoco = despesa("d-1", "2026-07-19", "alimentacao", "50.00", true, "Almoco");

        assertEquals(Ampliacao.FIM_DE_SEMANA_OU_PLANTAO, ampliacao(almoco, SEM_VIAGEM));
    }

    @Test
    @DisplayName("RN-008: quarta-feira com \"Almoco - plantao\" na descrição → ampliado")
    void plantao_em_dia_util_amplia() {
        var almoco = despesa("d-1", "2026-07-22", "alimentacao", "85.00", true, "Almoco - plantao");

        assertEquals(Ampliacao.FIM_DE_SEMANA_OU_PLANTAO, ampliacao(almoco, SEM_VIAGEM));
    }

    @Test
    @DisplayName("RN-008: \"PLANTÃO\" (maiúsculas e acento) → ampliado")
    void plantao_maiusculo_com_acento_amplia() {
        var almoco = despesa("d-1", "2026-07-22", "alimentacao", "85.00", true, "JANTAR PLANTÃO");

        assertEquals(Ampliacao.FIM_DE_SEMANA_OU_PLANTAO, ampliacao(almoco, SEM_VIAGEM));
    }

    @Test
    @DisplayName("RN-008: grupo com uma despesa de plantão e outra comum → o grupo inteiro é ampliado")
    void uma_despesa_de_plantao_amplia_o_grupo() {
        var comum = despesa("d-1", "2026-07-22", "alimentacao", "40.00", true, "Almoco");
        var plantao = despesa("d-2", "2026-07-22", "alimentacao", "40.00", true, "Jantar plantao");

        assertEquals(Ampliacao.FIM_DE_SEMANA_OU_PLANTAO,
                AmpliacaoLimite.determinar(LocalDate.parse("2026-07-22"), "alimentacao", List.of(comum, plantao), SEM_VIAGEM));
    }

    @Test
    @DisplayName("RN-008 / AMB-015 / D-003: sábado em viagem → VIAGEM, limite 90,00 (não 120,00)")
    void sabado_em_viagem_nao_acumula() {
        var almoco = despesa("d-1", "2026-07-18", "alimentacao", "100.00", true, "Almoco");

        Ampliacao ampliacao = ampliacao(almoco, Set.of(LocalDate.parse("2026-07-18")));

        assertEquals(Ampliacao.VIAGEM, ampliacao);
        assertEquals(new BigDecimal("90.00"), Politica.V3.limiteDiario("alimentacao", ampliacao));
    }

    @Test
    @DisplayName("RN-007: dia útil em viagem (d-011, 15/07) → VIAGEM, limite de alimentação 90,00")
    void dia_util_em_viagem() {
        var d011 = despesa("d-011", "2026-07-15", "alimentacao", "33.33", true, "Cafe da manha hotel");

        Ampliacao ampliacao = ampliacao(d011, Set.of(LocalDate.parse("2026-07-14"), LocalDate.parse("2026-07-15")));

        assertEquals(Ampliacao.VIAGEM, ampliacao);
        assertEquals(new BigDecimal("90.00"), Politica.V3.limiteDiario("alimentacao", ampliacao));
    }

    @Test
    @DisplayName("RN-004: dia útil sem viagem nem plantão → NENHUM, limite de alimentação 60,00")
    void dia_util_sem_ampliacao() {
        var almoco = despesa("d-1", "2026-07-03", "alimentacao", "72.50", true, "Almoco com cliente");

        Ampliacao ampliacao = ampliacao(almoco, SEM_VIAGEM);

        assertEquals(Ampliacao.NENHUM, ampliacao);
        assertEquals(new BigDecimal("60.00"), Politica.V3.limiteDiario("alimentacao", ampliacao));
    }

    @Test
    @DisplayName("RN-008: transporte urbano num sábado → limite 120,00")
    void transporte_sabado() {
        var taxi = despesa("d-1", "2026-07-18", "transporte_urbano", "100.00", true, "Corrida");

        Ampliacao ampliacao = ampliacao(taxi, SEM_VIAGEM);

        assertEquals(new BigDecimal("120.00"), Politica.V3.limiteDiario("transporte_urbano", ampliacao));
    }

    @Test
    @DisplayName("RN-004: transporte urbano em dia útil sem ampliação → limite 80,00")
    void transporte_dia_util() {
        assertEquals(new BigDecimal("80.00"), Politica.V3.limiteDiario("transporte_urbano", Ampliacao.NENHUM));
    }

    @Test
    @DisplayName("RN-008 / AMB-013: hospedagem num sábado em viagem não é ampliada")
    void hospedagem_nunca_ampliada() {
        var hotel = despesa("d-1", "2026-07-18", "hospedagem", "480.00", true, "Hotel 2 noites plantao");

        assertEquals(Ampliacao.NENHUM, ampliacao(hotel, Set.of(LocalDate.parse("2026-07-18"))));
    }

    @Test
    @DisplayName("RN-005: hospedagem não tem limite diário (é por noite)")
    void hospedagem_sem_limite_diario() {
        assertThrows(IllegalArgumentException.class, () -> Politica.V3.limiteDiario("hospedagem", Ampliacao.NENHUM));
    }
}
