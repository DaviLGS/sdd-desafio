package reembolso.regras;

import java.util.Optional;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;
import reembolso.dominio.Periodo;

/** RN-010: a data da despesa precisa estar no período, com início e fim inclusivos. */
public final class PeriodoCompetencia {

    private PeriodoCompetencia() {
    }

    public static Optional<Motivo> verificar(Despesa despesa, Periodo periodo) {
        boolean antesDoInicio = despesa.data().isBefore(periodo.inicio());
        boolean depoisDoFim = despesa.data().isAfter(periodo.fim());
        if (antesDoInicio || depoisDoFim) {
            return Optional.of(Motivo.FORA_DO_PERIODO);
        }
        return Optional.empty();
    }
}
