package reembolso.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Agrupamento onde o limite é aplicado (RN-004, RN-005).
 *
 * <p>{@code noites} só é preenchido em hospedagem; nas demais categorias é {@code null}.
 */
public record Grupo(
        LocalDate data,
        String categoria,
        List<String> idsDespesas,
        Integer noites,
        BigDecimal valorTotal,
        BigDecimal limite,
        Ampliacao ampliacao,
        BigDecimal valorReembolsado) {

    public Grupo {
        idsDespesas = List.copyOf(idsDespesas);
    }
}
