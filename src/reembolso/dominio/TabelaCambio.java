package reembolso.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * O arquivo de câmbio: data → moeda → quantos BRL vale 1 unidade da moeda (spec, seção 4).
 *
 * <p>As datas ficam em ordem crescente. A busca da cotação (na data, senão a última anterior)
 * fica nas regras (RN-014).
 */
public record TabelaCambio(Map<LocalDate, Map<String, BigDecimal>> taxas) {

    public TabelaCambio {
        TreeMap<LocalDate, Map<String, BigDecimal>> ordenadas = new TreeMap<>();
        taxas.forEach((data, porMoeda) -> ordenadas.put(data, Map.copyOf(porMoeda)));
        taxas = Collections.unmodifiableNavigableMap(ordenadas);
    }
}
