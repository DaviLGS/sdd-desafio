package reembolso.regras;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.LimiteCategoria;
import reembolso.dominio.Periodicidade;

/**
 * A tabela de limites aplicada a um colaborador, já resolvida (RN-013) e com as categorias
 * normalizadas (RN-001). Os valores vêm do arquivo de política (plano, seção 4).
 *
 * @param nomeTabela          centro de custo da tabela usada, ou {@code "padrao"}
 * @param categorias          categoria normalizada → limite e periodicidade
 * @param pisoNotaFiscal      valor a partir do qual a NF é obrigatória, inclusive (RN-006)
 * @param percentualAcrescimo percentual de ampliação dos limites por dia (RN-007, RN-008)
 */
public record Politica(
        String nomeTabela,
        Map<String, LimiteCategoria> categorias,
        BigDecimal pisoNotaFiscal,
        BigDecimal percentualAcrescimo) {

    public static final String ALIMENTACAO = "alimentacao";
    public static final String TRANSPORTE_URBANO = "transporte_urbano";
    public static final String HOSPEDAGEM = "hospedagem";

    /**
     * Política v3, montada no formato da v4. Usada só pela CLI até a T-031, que passa a ler a
     * política do arquivo; será removida nessa task.
     */
    @Deprecated
    public static final Politica V3 = new Politica(
            "padrao",
            Map.of(
                    ALIMENTACAO, new LimiteCategoria(new BigDecimal("60.00"), Periodicidade.DIA),
                    TRANSPORTE_URBANO, new LimiteCategoria(new BigDecimal("80.00"), Periodicidade.DIA),
                    HOSPEDAGEM, new LimiteCategoria(new BigDecimal("250.00"), Periodicidade.DIARIA)),
            new BigDecimal("100.00"),
            new BigDecimal("50"));

    private static final BigDecimal CEM = new BigDecimal("100");

    public Politica {
        categorias = Map.copyOf(categorias);
    }

    /** RN-003: a categoria está na tabela com limite maior que zero (AMB-023). */
    public boolean elegivel(String categoria) {
        LimiteCategoria limite = categorias.get(categoria);
        return limite != null && limite.limite().signum() > 0;
    }

    /** Periodicidade da categoria na tabela, se ela existir. */
    public Optional<Periodicidade> periodicidade(String categoria) {
        return Optional.ofNullable(categorias.get(categoria)).map(LimiteCategoria::periodicidade);
    }

    /** Multiplicador do limite ampliado: 1 + percentual / 100. */
    public BigDecimal fatorAmpliacao() {
        return BigDecimal.ONE.add(percentualAcrescimo.divide(CEM));
    }

    /**
     * Limite diário de uma categoria de periodicidade {@code dia}, base ou ampliado (RN-004,
     * RN-007, RN-008).
     *
     * @throws IllegalArgumentException se a categoria não tem periodicidade {@code dia}
     */
    public BigDecimal limiteDiario(String categoria, Ampliacao ampliacao) {
        BigDecimal base = limite(categoria, Periodicidade.DIA);
        BigDecimal limite = ampliacao == Ampliacao.NENHUM ? base : base.multiply(fatorAmpliacao());
        return limite.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Limite de uma categoria de periodicidade {@code diaria}: limite × noites, nunca ampliado
     * (RN-005).
     *
     * @throws IllegalArgumentException se a categoria não tem periodicidade {@code diaria}
     */
    public BigDecimal limitePorNoite(String categoria, int noites) {
        return limite(categoria, Periodicidade.DIARIA).multiply(BigDecimal.valueOf(noites))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal limite(String categoria, Periodicidade esperada) {
        LimiteCategoria limite = categorias.get(categoria);
        if (limite == null || limite.periodicidade() != esperada) {
            throw new IllegalArgumentException(
                    "categoria sem limite de periodicidade " + esperada.codigo() + ": " + categoria);
        }
        return limite.limite();
    }
}
