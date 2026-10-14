package reembolso.regras;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import reembolso.dominio.Ampliacao;

/**
 * Valores da política de reembolso. Único lugar do código onde eles existem (plano, seção 4).
 *
 * @param limiteAlimentacaoDia  limite diário base de alimentação (RN-004)
 * @param limiteTransporteDia   limite diário base de transporte urbano (RN-004)
 * @param limiteHospedagemNoite limite por noite de hospedagem (RN-005)
 * @param fatorAmpliacao        multiplicador do limite ampliado (RN-007, RN-008)
 * @param pisoNotaFiscal        valor a partir do qual a NF é obrigatória, inclusive (RN-006)
 * @param categoriasElegiveis   categorias reembolsáveis, já normalizadas (RN-003)
 */
public record Politica(
        BigDecimal limiteAlimentacaoDia,
        BigDecimal limiteTransporteDia,
        BigDecimal limiteHospedagemNoite,
        BigDecimal fatorAmpliacao,
        BigDecimal pisoNotaFiscal,
        Set<String> categoriasElegiveis) {

    public static final String ALIMENTACAO = "alimentacao";
    public static final String TRANSPORTE_URBANO = "transporte_urbano";
    public static final String HOSPEDAGEM = "hospedagem";

    /** Política de Reembolso de Despesas — v3, conforme interpretada na spec 1.1. */
    public static final Politica V3 = new Politica(
            new BigDecimal("60.00"),
            new BigDecimal("80.00"),
            new BigDecimal("250.00"),
            new BigDecimal("1.5"),
            new BigDecimal("100.00"),
            Set.of(ALIMENTACAO, TRANSPORTE_URBANO, HOSPEDAGEM));

    public Politica {
        categoriasElegiveis = Set.copyOf(categoriasElegiveis);
    }

    /**
     * Limite diário de alimentação ou transporte urbano, base ou ampliado (RN-004, RN-007, RN-008).
     *
     * @throws IllegalArgumentException para hospedagem (limite por noite, RN-005) ou outra categoria
     */
    public BigDecimal limiteDiario(String categoria, Ampliacao ampliacao) {
        BigDecimal base = switch (categoria) {
            case ALIMENTACAO -> limiteAlimentacaoDia;
            case TRANSPORTE_URBANO -> limiteTransporteDia;
            default -> throw new IllegalArgumentException("categoria sem limite diário: " + categoria);
        };
        BigDecimal limite = ampliacao == Ampliacao.NENHUM ? base : base.multiply(fatorAmpliacao);
        return limite.setScale(2, RoundingMode.HALF_UP);
    }

    /** Limite de uma hospedagem: limite por noite × noites, nunca ampliado (RN-005). */
    public BigDecimal limiteHospedagem(int noites) {
        return limiteHospedagemNoite.multiply(BigDecimal.valueOf(noites)).setScale(2, RoundingMode.HALF_UP);
    }
}
