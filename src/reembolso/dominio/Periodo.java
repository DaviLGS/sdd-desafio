package reembolso.dominio;

import java.time.LocalDate;

/** Bloco {@code periodo} da entrada. {@code inicio} e {@code fim} são inclusivos (RN-010). */
public record Periodo(String competencia, LocalDate inicio, LocalDate fim) {
}
