package reembolso.regras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import reembolso.dominio.Despesa;

/** RN-009: despesas com todos os campos iguais, exceto o ID, são duplicatas. */
public final class Duplicatas {

    private Duplicatas() {
    }

    /** Campos comparados pela RN-009. O valor é comparado numericamente (54.9 = 54.90). */
    private record Chave(
            LocalDate data,
            String categoria,
            String descricao,
            String fornecedor,
            BigDecimal valor,
            boolean temNotaFiscal) {

        static Chave de(Despesa despesa) {
            return new Chave(
                    despesa.data(),
                    despesa.categoria(),
                    despesa.descricao(),
                    despesa.fornecedor(),
                    despesa.valor().stripTrailingZeros(),
                    despesa.temNotaFiscal());
        }
    }

    /**
     * Devolve os IDs de <b>todas</b> as cópias de cada grupo de duplicatas.
     *
     * <p>Espera as despesas já normalizadas (RN-001) e arredondadas (RN-002).
     */
    public static Set<String> identificar(List<Despesa> despesas) {
        Map<Chave, List<String>> porChave = new LinkedHashMap<>();
        for (Despesa despesa : despesas) {
            porChave.computeIfAbsent(Chave.de(despesa), chave -> new ArrayList<>()).add(despesa.id());
        }

        Set<String> duplicadas = new HashSet<>();
        for (List<String> ids : porChave.values()) {
            if (ids.size() > 1) {
                duplicadas.addAll(ids);
            }
        }
        return duplicadas;
    }
}
