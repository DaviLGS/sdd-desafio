package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static reembolso.Despesas.despesa;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reembolso.Politicas;
import reembolso.dominio.Motivo;
import reembolso.dominio.Status;

/** T-009: RN-006 / AMB-006 a AMB-009 — nota fiscal obrigatória a partir de R$ 100,00. */
class Rn006NotaFiscalTest {

    private static Optional<Motivo> verificar(String categoria, String valor, boolean temNotaFiscal) {
        return NotaFiscal.verificar(despesa("d-1", "2026-07-06", categoria, valor, temNotaFiscal), Politicas.PADRAO);
    }

    @Test
    @DisplayName("RN-006 / AMB-006: R$ 100,00 exatos sem NF (d-003) → NF_AUSENTE")
    void cem_exatos_sem_nf() {
        assertEquals(Optional.of(Motivo.NF_AUSENTE), verificar("transporte_urbano", "100.00", false));
    }

    @Test
    @DisplayName("RN-006: R$ 100,01 sem NF (d-004) → NF_AUSENTE")
    void cem_e_um_centavo_sem_nf() {
        assertEquals(Optional.of(Motivo.NF_AUSENTE), verificar("transporte_urbano", "100.01", false));
    }

    @Test
    @DisplayName("RN-006 / AMB-006: R$ 99,99 sem NF → não exige NF")
    void noventa_e_nove_e_noventa_e_nove_sem_nf() {
        assertEquals(Optional.empty(), verificar("alimentacao", "99.99", false));
    }

    @Test
    @DisplayName("RN-006 / AMB-008: hospedagem de R$ 690,00 sem NF (d-013) → NF_AUSENTE, mesmo que o reembolso possível seja menor")
    void hospedagem_690_sem_nf() {
        assertEquals(Optional.of(Motivo.NF_AUSENTE), verificar("hospedagem", "690.00", false));
    }

    @Test
    @DisplayName("RN-006 / AMB-008: compara o valor gasto — R$ 100,00 de hospedagem sem NF exige NF mesmo abaixo do limite de 250")
    void compara_valor_gasto_nao_o_limite() {
        assertEquals(Optional.of(Motivo.NF_AUSENTE), verificar("hospedagem", "100.00", false));
    }

    @Test
    @DisplayName("RN-006 / AMB-007: dois táxis de 60 + 50 no mesmo dia sem NF → nenhum exige NF (comparação por despesa)")
    void dois_taxis_no_mesmo_dia_sem_nf() {
        var taxi1 = despesa("d-1", "2026-07-06", "transporte_urbano", "60.00", false);
        var taxi2 = despesa("d-2", "2026-07-06", "transporte_urbano", "50.00", false);

        assertEquals(Optional.empty(), NotaFiscal.verificar(taxi1, Politicas.PADRAO));
        assertEquals(Optional.empty(), NotaFiscal.verificar(taxi2, Politicas.PADRAO));
    }

    @Test
    @DisplayName("RN-006 / RN-011: estorno negativo sem NF (d-009) → não exige NF")
    void estorno_sem_nf() {
        assertEquals(Optional.empty(), verificar("transporte_urbano", "-45.00", false));
    }

    @DisplayName("RN-006: com NF, qualquer valor passa")
    @ParameterizedTest(name = "R$ {0} com NF")
    @CsvSource({"100.00", "100.01", "480.00", "99.99"})
    void com_nf_passa(String valor) {
        assertEquals(Optional.empty(), verificar("alimentacao", valor, true));
    }

    @Test
    @DisplayName("RN-006 / RN-002: 99,995 arredondado vira 100,00 e passa a exigir NF")
    void valor_arredondado_cruza_a_fronteira() {
        BigDecimal arredondado = Arredondamento.arredondar(new BigDecimal("99.995"));

        assertEquals(Optional.of(Motivo.NF_AUSENTE), verificar("alimentacao", arredondado.toPlainString(), false));
    }

    @Test
    @DisplayName("RN-006 / AMB-009: NF ausente leva a em_revisao, não a recusada")
    void nf_ausente_leva_a_revisao() {
        assertEquals(Status.EM_REVISAO, Motivo.NF_AUSENTE.statusResultante());
    }


    private static reembolso.dominio.Despesa estrangeira(String valorBrl, String valorOriginal, String moeda, boolean nf) {
        return new reembolso.dominio.Despesa("e-1", java.time.LocalDate.parse("2026-07-20"), "transporte_urbano",
                "transporte_urbano", "Corridas", "F", valorBrl == null ? null : new BigDecimal(valorBrl), nf, moeda,
                new BigDecimal(valorOriginal), null, null);
    }

    @Test
    @DisplayName("RN-006 / AMB-034: USD 40,00 convertido em R$ 220,00, sem NF (e-005) → NF_AUSENTE (compara em BRL)")
    void nf_comparada_em_brl() {
        assertEquals(Optional.of(Motivo.NF_AUSENTE), NotaFiscal.verificar(estrangeira("220.00", "40.00", "USD", false), Politicas.PADRAO));
    }

    @Test
    @DisplayName("RN-006 / RN-014: sem cotação (valor em BRL nulo) a NF não é avaliada")
    void sem_cotacao_nao_avalia_nf() {
        assertEquals(Optional.empty(), NotaFiscal.verificar(estrangeira(null, "500.00", "GBP", false), Politicas.PADRAO));
    }
}
