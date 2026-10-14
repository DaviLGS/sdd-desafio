package reembolso.dominio;

import java.util.List;

/** O arquivo de entrada inteiro (spec, seção 4). */
public record Entrada(Colaborador colaborador, Periodo periodo, List<Despesa> despesas) {

    public Entrada {
        despesas = List.copyOf(despesas);
    }
}
