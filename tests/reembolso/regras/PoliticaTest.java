package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-002: os valores de {@link Politica#V3} correspondem à spec 1.1. */
class PoliticaTest {

    private final Politica politica = Politica.V3;

    @Test
    @DisplayName("RN-004: limite diário base de alimentação é R$ 60,00")
    void limite_alimentacao() {
        assertEquals(new BigDecimal("60.00"), politica.limiteAlimentacaoDia());
    }

    @Test
    @DisplayName("RN-004: limite diário base de transporte urbano é R$ 80,00")
    void limite_transporte() {
        assertEquals(new BigDecimal("80.00"), politica.limiteTransporteDia());
    }

    @Test
    @DisplayName("RN-005: limite de hospedagem é R$ 250,00 por noite")
    void limite_hospedagem() {
        assertEquals(new BigDecimal("250.00"), politica.limiteHospedagemNoite());
    }

    @Test
    @DisplayName("RN-007 / RN-008: fator de ampliação é 1,5 (+50%)")
    void fator_ampliacao() {
        assertEquals(new BigDecimal("1.5"), politica.fatorAmpliacao());
    }

    @Test
    @DisplayName("RN-006: piso da nota fiscal é R$ 100,00")
    void piso_nota_fiscal() {
        assertEquals(new BigDecimal("100.00"), politica.pisoNotaFiscal());
    }

    @Test
    @DisplayName("RN-003: categorias elegíveis são alimentação, transporte urbano e hospedagem")
    void categorias_elegiveis() {
        assertEquals(Set.of("alimentacao", "transporte_urbano", "hospedagem"), politica.categoriasElegiveis());
    }
}
