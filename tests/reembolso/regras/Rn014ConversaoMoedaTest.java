package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;
import reembolso.dominio.Status;
import reembolso.dominio.TabelaCambio;

/** T-027: RN-014 / RN-002 / AMB-030 a AMB-033, AMB-036 — conversão de moeda. */
class Rn014ConversaoMoedaTest {

    private static Map<String, BigDecimal> usdEur(String usd, String eur) {
        return Map.of("USD", new BigDecimal(usd), "EUR", new BigDecimal(eur));
    }

    /** Trecho de exemplos/envelope/cambio.json, sem ler arquivo. */
    private static final TabelaCambio CAMBIO = new TabelaCambio(Map.of(
            LocalDate.of(2026, 7, 13), usdEur("5.42", "5.91"),
            LocalDate.of(2026, 7, 14), usdEur("5.44", "5.93"),
            LocalDate.of(2026, 7, 17), usdEur("5.47", "5.96"),
            LocalDate.of(2026, 7, 20), usdEur("5.50", "6.01"),
            LocalDate.of(2026, 7, 28), usdEur("5.51", "6.02")));

    private static Despesa despesa(String data, String valor, String moeda) {
        return new Despesa("e-1", LocalDate.parse(data), "alimentacao", "alimentacao", "Almoco", "F",
                new BigDecimal(valor), true, moeda, new BigDecimal(valor), null, null);
    }

    private static Despesa converter(String data, String valor, String moeda) {
        return ConversaoMoeda.converter(despesa(data, valor, moeda), CAMBIO);
    }

    @Test
    @DisplayName("RN-014: EUR 22,00 em 14/07 (cotação 5,93) → R$ 130,46 (e-002)")
    void eur_na_data() {
        Despesa d = converter("2026-07-14", "22.00", "EUR");

        assertEquals(new BigDecimal("130.46"), d.valor());
        assertEquals(new BigDecimal("5.93"), d.taxaCambio());
        assertEquals(LocalDate.of(2026, 7, 14), d.dataCotacao());
        assertEquals(new BigDecimal("22.00"), d.valorOriginal());
    }

    @Test
    @DisplayName("RN-014 / AMB-031: sábado 18/07 sem cotação → usa a de 17/07 (5,96); EUR 30 → R$ 178,80 (e-004)")
    void dia_sem_cotacao_usa_a_anterior() {
        Despesa d = converter("2026-07-18", "30.00", "EUR");

        assertEquals(new BigDecimal("178.80"), d.valor());
        assertEquals(LocalDate.of(2026, 7, 17), d.dataCotacao());
    }

    @Test
    @DisplayName("RN-014 / AMB-030: GBP (ausente do arquivo) → sem cotação, valor null, CAMBIO_INDISPONIVEL (e-006)")
    void moeda_sem_cotacao() {
        Despesa d = converter("2026-07-21", "55.00", "GBP");

        assertNull(d.valor());
        assertNull(d.taxaCambio());
        assertEquals(Optional.of(Motivo.CAMBIO_INDISPONIVEL), ConversaoMoeda.verificar(d));
    }

    @Test
    @DisplayName("RN-014 / AMB-032: data anterior à primeira cotação (10/07) → CAMBIO_INDISPONIVEL")
    void antes_da_primeira_cotacao() {
        assertEquals(Optional.of(Motivo.CAMBIO_INDISPONIVEL), ConversaoMoeda.verificar(converter("2026-07-10", "20.00", "USD")));
    }

    @Test
    @DisplayName("RN-014 / AMB-032: depois da última cotação (30/07) → usa a de 28/07")
    void depois_da_ultima_cotacao() {
        Despesa d = converter("2026-07-30", "10.00", "USD");

        assertEquals(LocalDate.of(2026, 7, 28), d.dataCotacao());
        assertEquals(new BigDecimal("55.10"), d.valor());
    }

    @Test
    @DisplayName("RN-014: BRL não é convertido — valor arredondado, sem taxa nem data de cotação")
    void brl_nao_e_convertido() {
        Despesa d = converter("2026-07-14", "33.333", "BRL");

        assertEquals(new BigDecimal("33.33"), d.valor());
        assertNull(d.taxaCambio());
        assertNull(d.dataCotacao());
        assertEquals(Optional.empty(), ConversaoMoeda.verificar(d));
    }

    @Test
    @DisplayName("RN-014: código de moeda comparado exatamente (\"usd\" minúsculo → CAMBIO_INDISPONIVEL)")
    void codigo_exato() {
        assertNull(converter("2026-07-14", "10.00", "usd").valor());
    }

    @Test
    @DisplayName("RN-002 / AMB-033: converte o valor exato e arredonda uma vez — USD 10,005 × 5,42 = 54,23 (não 54,25)")
    void converte_antes_de_arredondar() {
        assertEquals(new BigDecimal("54.23"), converter("2026-07-13", "10.005", "USD").valor());
    }

    @Test
    @DisplayName("RN-014 / AMB-036: estorno em moeda estrangeira é convertido — −USD 10 em 20/07 → −R$ 55,00")
    void estorno_estrangeiro_convertido() {
        assertEquals(new BigDecimal("-55.00"), converter("2026-07-20", "-10.00", "USD").valor());
    }

    @Test
    @DisplayName("RN-014 / RN-012: sem cotação leva a em_revisao")
    void cambio_indisponivel_leva_a_revisao() {
        assertEquals(Status.EM_REVISAO, Motivo.CAMBIO_INDISPONIVEL.statusResultante());
    }
}
