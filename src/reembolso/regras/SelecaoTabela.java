package reembolso.regras;

import java.util.LinkedHashMap;
import java.util.Map;
import reembolso.dominio.LimiteCategoria;
import reembolso.dominio.TabelaPoliticas;

/** RN-013: escolhe a tabela de limites do colaborador pelo centro de custo. */
public final class SelecaoTabela {

    /** Nome informado em {@code resumo.tabela_aplicada} quando o CC não tem entrada. */
    public static final String PADRAO = "padrao";

    private SelecaoTabela() {
    }

    /**
     * Tabela do centro de custo, comparando o nome exatamente e ignorando só espaços nas pontas
     * (AMB-025). Sem entrada → tabela padrão. Com entrada, vale só a tabela dele, sem completar
     * com o padrão (AMB-021). Os nomes de categoria são normalizados pela RN-001.
     */
    public static Politica politicaPara(String centroCusto, TabelaPoliticas tabelas) {
        String procurado = centroCusto.strip();
        for (Map.Entry<String, Map<String, LimiteCategoria>> centro : tabelas.centrosCusto().entrySet()) {
            if (centro.getKey().strip().equals(procurado)) {
                return politica(centro.getKey().strip(), centro.getValue(), tabelas);
            }
        }
        return politica(PADRAO, tabelas.padrao(), tabelas);
    }

    private static Politica politica(String nome, Map<String, LimiteCategoria> categorias, TabelaPoliticas tabelas) {
        Map<String, LimiteCategoria> normalizadas = new LinkedHashMap<>();
        categorias.forEach((categoria, limite) -> normalizadas.put(Normalizacao.categoria(categoria), limite));
        return new Politica(nome, normalizadas, tabelas.pisoNotaFiscal(), tabelas.percentualAcrescimo());
    }
}
