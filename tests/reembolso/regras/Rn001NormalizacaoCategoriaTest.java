package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** T-004: RN-001 / AMB-020 — categoria sem diferenciar maiúsculas, minúsculas e acentos. */
class Rn001NormalizacaoCategoriaTest {

    @DisplayName("RN-001 / AMB-020: variações de grafia viram a categoria normalizada")
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "alimentacao,        alimentacao",
        "ALIMENTACAO,        alimentacao",   // d-014 do exemplo
        "Alimentação,        alimentacao",   // decisão D-001
        "ALIMENTAÇÃO,        alimentacao",
        "transporte_urbano,  transporte_urbano",
        "Transporte_Urbano,  transporte_urbano",
        "HOSPEDAGEM,         hospedagem",
    })
    void variacoes_de_grafia(String entrada, String esperado) {
        assertEquals(esperado, Normalizacao.categoria(entrada));
    }

    @Test
    @DisplayName("RN-001: categoria fora da política só é normalizada, não é convertida em outra")
    void categoria_fora_da_politica_continua_fora() {
        assertEquals("coworking", Normalizacao.categoria("CoWorking"));
    }
}
