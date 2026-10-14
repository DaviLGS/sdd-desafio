package reembolso.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma despesa da entrada.
 *
 * <p>{@code categoriaOriginal} é o texto como veio na entrada; {@code categoria} é a forma
 * normalizada (RN-001). {@code valor} é o valor após o arredondamento (RN-002).
 *
 * <p>Ao sair da leitura da entrada, a despesa ainda está crua: {@code categoria} é igual a
 * {@code categoriaOriginal} e {@code valor} tem os dígitos exatos do JSON. A normalização e o
 * arredondamento são o passo 1 da seção 8 da spec, aplicados pelo motor.
 */
public record Despesa(
        String id,
        LocalDate data,
        String categoriaOriginal,
        String categoria,
        String descricao,
        String fornecedor,
        BigDecimal valor,
        boolean temNotaFiscal) {
}
