package reembolso;

import java.math.BigDecimal;
import java.time.LocalDate;
import reembolso.dominio.Despesa;

/** Fábrica de despesas já normalizadas (passo 1 da seção 8 aplicado) para os testes de regra. */
public final class Despesas {

    private Despesas() {
    }

    public static Despesa despesa(String id, String data, String categoria, String valor, boolean temNotaFiscal) {
        return despesa(id, data, categoria, valor, temNotaFiscal, "Descricao " + id);
    }

    public static Despesa despesa(
            String id, String data, String categoria, String valor, boolean temNotaFiscal, String descricao) {
        return new Despesa(
                id,
                LocalDate.parse(data),
                categoria,
                categoria,
                descricao,
                "Fornecedor",
                new BigDecimal(valor),
                temNotaFiscal);
    }
}
