package reembolso.dominio;

import java.math.BigDecimal;
import java.util.Map;

/**
 * O arquivo de política inteiro, com os nomes como estão no arquivo (spec, seção 4).
 *
 * <p>A escolha da tabela de um colaborador (RN-013) e a normalização das categorias (RN-001)
 * ficam nas regras; aqui são só os dados.
 *
 * @param centrosCusto        nome do centro de custo → categoria → limite
 * @param padrao              tabela dos centros de custo sem entrada
 * @param pisoNotaFiscal      {@code nota_fiscal_obrigatoria_acima_de}
 * @param percentualAcrescimo {@code acrescimo_em_viagem_percentual}
 */
public record TabelaPoliticas(
        Map<String, Map<String, LimiteCategoria>> centrosCusto,
        Map<String, LimiteCategoria> padrao,
        BigDecimal pisoNotaFiscal,
        BigDecimal percentualAcrescimo) {

    public TabelaPoliticas {
        centrosCusto = Map.copyOf(centrosCusto);
        padrao = Map.copyOf(padrao);
    }
}
