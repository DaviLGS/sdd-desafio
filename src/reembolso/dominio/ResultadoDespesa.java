package reembolso.dominio;

import java.util.List;

/** Decisão sobre uma despesa: status, motivos e justificativa (spec, seção 4). */
public record ResultadoDespesa(Despesa despesa, Status status, List<Motivo> motivos, String justificativa) {

    public ResultadoDespesa {
        motivos = List.copyOf(motivos);
    }
}
