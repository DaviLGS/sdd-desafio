package reembolso.regras;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** RN-002: arredondamento em cadeia para 2 casas decimais (plano, DT-002). */
public final class Arredondamento {

    private static final int CASAS = 2;

    private Arredondamento() {
    }

    /**
     * Arredonda {@code valor} para 2 casas, uma casa por vez, do último dígito para a esquerda.
     *
     * <p>Cada passo descarta o último dígito: de 0 a 4 é descartado, de 5 a 9 soma 1 à casa
     * anterior. Por isso 3,4449 → 3,445 → 3,45 (e não 3,44, como daria um arredondamento direto).
     * {@link RoundingMode#HALF_UP} opera sobre o valor absoluto, então negativos mantêm o sinal.
     */
    public static BigDecimal arredondar(BigDecimal valor) {
        BigDecimal resultado = valor;
        while (resultado.scale() > CASAS) {
            resultado = resultado.setScale(resultado.scale() - 1, RoundingMode.HALF_UP);
        }
        return resultado.setScale(CASAS, RoundingMode.UNNECESSARY);
    }
}
