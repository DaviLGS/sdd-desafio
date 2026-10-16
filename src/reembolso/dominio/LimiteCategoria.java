package reembolso.dominio;

import java.math.BigDecimal;

/** Limite de uma categoria numa tabela da política: valor em BRL e periodicidade (spec, seção 4). */
public record LimiteCategoria(BigDecimal limite, Periodicidade periodicidade) {
}
