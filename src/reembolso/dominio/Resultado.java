package reembolso.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** O resultado completo do cálculo (spec, seção 4). */
public record Resultado(
        Colaborador colaborador,
        Periodo periodo,
        String tabelaAplicada,
        List<ResultadoDespesa> despesas,
        List<Grupo> grupos,
        BigDecimal totalReembolsado,
        Map<Status, Integer> quantidade) {

    public Resultado {
        despesas = List.copyOf(despesas);
        grupos = List.copyOf(grupos);
        quantidade = Map.copyOf(quantidade);
    }
}
