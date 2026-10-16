package reembolso.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma despesa da entrada.
 *
 * <p>{@code categoriaOriginal} é o texto como veio na entrada; {@code categoria} é a forma
 * normalizada (RN-001). {@code valor} é o valor em BRL, convertido (RN-014) e arredondado (RN-002);
 * é {@code null} quando não há cotação para a moeda. {@code moeda} e {@code valorOriginal} são os
 * da entrada; {@code taxaCambio} e {@code dataCotacao} só existem em despesa convertida.
 *
 * <p>Ao sair da leitura da entrada, a despesa ainda está crua: {@code categoria} é igual a
 * {@code categoriaOriginal} e {@code valor} é igual a {@code valorOriginal}, com os dígitos exatos
 * do JSON. Conversão, normalização e arredondamento são o passo 1 da seção 8 da spec, aplicados
 * pelo motor.
 */
public record Despesa(
        String id,
        LocalDate data,
        String categoriaOriginal,
        String categoria,
        String descricao,
        String fornecedor,
        BigDecimal valor,
        boolean temNotaFiscal,
        String moeda,
        BigDecimal valorOriginal,
        BigDecimal taxaCambio,
        LocalDate dataCotacao) {

    /** Moeda assumida quando a entrada não traz {@code moeda} (RN-014). */
    public static final String BRL = "BRL";

    /** Despesa em BRL, sem conversão (plano, DT-010). */
    public Despesa(String id, LocalDate data, String categoriaOriginal, String categoria, String descricao,
            String fornecedor, BigDecimal valor, boolean temNotaFiscal) {
        this(id, data, categoriaOriginal, categoria, descricao, fornecedor, valor, temNotaFiscal, BRL, valor,
                null, null);
    }

    /** Se a despesa foi lançada em moeda diferente de BRL. */
    public boolean emMoedaEstrangeira() {
        return !BRL.equals(moeda);
    }
}
