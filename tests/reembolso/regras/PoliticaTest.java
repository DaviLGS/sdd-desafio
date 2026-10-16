package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.Politicas;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.LimiteCategoria;
import reembolso.dominio.Periodicidade;

/** T-002 / T-023: a {@link Politica} resolvida para uma tabela (RN-003 a RN-008, AMB-023, AMB-026, AMB-027). */
class PoliticaTest {

    @Test
    @DisplayName("RN-003: categoria da tabela com limite > 0 é elegível")
    void categoria_com_limite_e_elegivel() {
        assertTrue(Politicas.PADRAO.elegivel("alimentacao"));
        assertTrue(Politicas.COMERCIAL.elegivel("representacao"));
    }

    @Test
    @DisplayName("RN-003 / AMB-023: limite zero (hospedagem no CC-ENG-PLATAFORMA) → não elegível")
    void limite_zero_nao_e_elegivel() {
        assertFalse(Politicas.ENG_PLATAFORMA.elegivel("hospedagem"));
    }

    @Test
    @DisplayName("RN-003 / AMB-022: categoria ausente da tabela (representação no padrão) → não elegível")
    void categoria_ausente_nao_e_elegivel() {
        assertFalse(Politicas.PADRAO.elegivel("representacao"));
        assertFalse(Politicas.PADRAO.elegivel("coworking"));
    }

    @Test
    @DisplayName("RN-004: limite diário base vem da tabela (padrão 60/80; CC-COMERCIAL 90/150/300)")
    void limite_diario_base() {
        assertEquals(new BigDecimal("60.00"), Politicas.PADRAO.limiteDiario("alimentacao", Ampliacao.NENHUM));
        assertEquals(new BigDecimal("80.00"), Politicas.PADRAO.limiteDiario("transporte_urbano", Ampliacao.NENHUM));
        assertEquals(new BigDecimal("90.00"), Politicas.COMERCIAL.limiteDiario("alimentacao", Ampliacao.NENHUM));
        assertEquals(new BigDecimal("300.00"), Politicas.COMERCIAL.limiteDiario("representacao", Ampliacao.NENHUM));
    }

    @Test
    @DisplayName("RN-007 / RN-008 / AMB-027: ampliado = base × (1 + percentual/100) — 50% → 60 vira 90")
    void limite_ampliado_pelo_percentual_do_arquivo() {
        assertEquals(new BigDecimal("1.5"), Politicas.PADRAO.fatorAmpliacao());
        assertEquals(new BigDecimal("90.00"), Politicas.PADRAO.limiteDiario("alimentacao", Ampliacao.VIAGEM));
        assertEquals(new BigDecimal("112.50"),
                Politicas.ENG_PLATAFORMA.limiteDiario("alimentacao", Ampliacao.FIM_DE_SEMANA_OU_PLANTAO));
    }

    @Test
    @DisplayName("RN-004 / AMB-026: representação (periodicidade dia) também é ampliada — 300 vira 450")
    void representacao_ampliada() {
        assertEquals(new BigDecimal("450.00"),
                Politicas.COMERCIAL.limiteDiario("representacao", Ampliacao.FIM_DE_SEMANA_OU_PLANTAO));
    }

    @Test
    @DisplayName("AMB-027: outro percentual no arquivo muda a ampliação (20% → 60 vira 72)")
    void outro_percentual() {
        Politica vinte = new Politica("x",
                Map.of("alimentacao", new LimiteCategoria(new BigDecimal("60.00"), Periodicidade.DIA)),
                new BigDecimal("100.00"), new BigDecimal("20"));

        assertEquals(new BigDecimal("72.00"), vinte.limiteDiario("alimentacao", Ampliacao.VIAGEM));
    }

    @Test
    @DisplayName("RN-005: limite por noite = limite × noites (padrão 250 × 2 = 500; CC-COMERCIAL 400 × 3 = 1.200)")
    void limite_por_noite() {
        assertEquals(new BigDecimal("500.00"), Politicas.PADRAO.limitePorNoite("hospedagem", 2));
        assertEquals(new BigDecimal("1200.00"), Politicas.COMERCIAL.limitePorNoite("hospedagem", 3));
    }

    @Test
    @DisplayName("AMB-026: a periodicidade vem da tabela; limite diário de categoria por noite (e vice-versa) é erro")
    void periodicidade_errada_e_erro() {
        assertEquals(Optional.of(Periodicidade.DIARIA), Politicas.PADRAO.periodicidade("hospedagem"));
        assertEquals(Optional.of(Periodicidade.DIA), Politicas.COMERCIAL.periodicidade("representacao"));
        assertEquals(Optional.empty(), Politicas.PADRAO.periodicidade("coworking"));
        assertThrows(IllegalArgumentException.class, () -> Politicas.PADRAO.limiteDiario("hospedagem", Ampliacao.NENHUM));
        assertThrows(IllegalArgumentException.class, () -> Politicas.PADRAO.limitePorNoite("alimentacao", 1));
    }

    @Test
    @DisplayName("RN-006 / AMB-027: piso da nota fiscal vem da tabela (100,00)")
    void piso_nota_fiscal() {
        assertEquals(new BigDecimal("100.00"), Politicas.PADRAO.pisoNotaFiscal());
    }
}
