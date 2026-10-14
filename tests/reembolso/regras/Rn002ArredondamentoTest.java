package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** T-003: RN-002 / AMB-010 — arredondamento em cadeia para 2 casas. */
class Rn002ArredondamentoTest {

    @DisplayName("RN-002 / AMB-010: exemplos da spec")
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "33.333,  33.33",   // d-011 do exemplo
        "3.444,   3.44",
        "3.445,   3.45",
        "3.4449,  3.45",    // em cadeia: 3,4449 → 3,445 → 3,45
        "-3.445,  -3.45",   // negativo: arredonda o absoluto e mantém o sinal
    })
    void exemplos_da_spec(String entrada, String esperado) {
        assertEquals(new BigDecimal(esperado), Arredondamento.arredondar(new BigDecimal(entrada)));
    }

    @DisplayName("RN-002: valor com até 2 casas fica inalterado, sempre com escala 2")
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "72.50,   72.50",
        "100.01,  100.01",
        "100,     100.00",
        "61.0,    61.00",
        "-45.00,  -45.00",
        "0,       0.00",
    })
    void valor_com_ate_duas_casas_inalterado(String entrada, String esperado) {
        assertEquals(new BigDecimal(esperado), Arredondamento.arredondar(new BigDecimal(entrada)));
    }

    @DisplayName("RN-002: casos adicionais de cadeia (dígitos 4 e 5 nas fronteiras)")
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "99.994,   99.99",
        "99.995,   100.00",   // a cadeia pode cruzar a fronteira da NF (RN-006 compara o valor arredondado)
        "3.44449,  3.45",     // 3,44449 → 3,4445 → 3,445 → 3,45 (o 9 final propaga pela cadeia)
        "1.2349,   1.24",
        "1.2344,   1.23",
    })
    void cadeia_casos_adicionais(String entrada, String esperado) {
        assertEquals(new BigDecimal(esperado), Arredondamento.arredondar(new BigDecimal(entrada)));
    }
}
