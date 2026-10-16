package reembolso.regras;

import java.util.Optional;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;

/**
 * RN-006: despesa com valor maior ou igual ao piso da política exige nota fiscal.
 *
 * <p>Compara o valor de cada despesa em BRL, já convertido (RN-014) e arredondado (RN-002) — não a
 * soma do dia nem o valor reembolsável (AMB-034). Sem cotação ({@code valor} nulo), não é avaliada.
 */
public final class NotaFiscal {

    private NotaFiscal() {
    }

    public static Optional<Motivo> verificar(Despesa despesa, Politica politica) {
        if (despesa.valor() == null) {
            return Optional.empty();
        }
        boolean exigeNotaFiscal = despesa.valor().compareTo(politica.pisoNotaFiscal()) >= 0;
        if (exigeNotaFiscal && !despesa.temNotaFiscal()) {
            return Optional.of(Motivo.NF_AUSENTE);
        }
        return Optional.empty();
    }
}
