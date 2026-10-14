package reembolso.regras;

import java.util.List;
import reembolso.dominio.Motivo;
import reembolso.dominio.Status;

/** RN-012: o status de uma despesa a partir dos seus motivos. */
public final class StatusDespesa {

    private StatusDespesa() {
    }

    /**
     * Sem motivos → aprovada. Algum motivo de recusa → recusada. Senão, havendo motivo → em_revisao.
     */
    public static Status consolidar(List<Motivo> motivos) {
        if (motivos.isEmpty()) {
            return Status.APROVADA;
        }
        boolean algumRecusa = motivos.stream().anyMatch(m -> m.statusResultante() == Status.RECUSADA);
        return algumRecusa ? Status.RECUSADA : Status.EM_REVISAO;
    }
}
