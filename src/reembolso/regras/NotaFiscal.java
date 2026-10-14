package reembolso.regras;

import java.util.Optional;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;

/**
 * RN-006: despesa com valor maior ou igual ao piso da política exige nota fiscal.
 *
 * <p>Compara o valor de cada despesa após o arredondamento (RN-002) — não a soma do dia nem o
 * valor reembolsável.
 */
public final class NotaFiscal {

    private NotaFiscal() {
    }

    public static Optional<Motivo> verificar(Despesa despesa, Politica politica) {
        boolean exigeNotaFiscal = despesa.valor().compareTo(politica.pisoNotaFiscal()) >= 0;
        if (exigeNotaFiscal && !despesa.temNotaFiscal()) {
            return Optional.of(Motivo.NF_AUSENTE);
        }
        return Optional.empty();
    }
}
