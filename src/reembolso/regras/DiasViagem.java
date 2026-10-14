package reembolso.regras;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import reembolso.dominio.Despesa;

/**
 * RN-007: um dia é de viagem quando está coberto por uma noite de hospedagem.
 *
 * <p>Hospedagem com data D e N noites cobre D, D+1, …, D+N−1; o dia da saída (D+N) não conta.
 */
public final class DiasViagem {

    private DiasViagem() {
    }

    /**
     * Considera toda hospedagem com número de noites identificável (RN-005), mesmo que ela própria
     * esteja em revisão por outro motivo (ex.: falta de NF). Espera as despesas já normalizadas.
     */
    public static Set<LocalDate> calcular(List<Despesa> despesas) {
        Set<LocalDate> dias = new HashSet<>();
        for (Despesa despesa : despesas) {
            if (!Politica.HOSPEDAGEM.equals(despesa.categoria())) {
                continue;
            }
            OptionalInt noites = NoitesHospedagem.identificar(despesa.descricao());
            if (noites.isEmpty()) {
                continue;
            }
            for (int i = 0; i < noites.getAsInt(); i++) {
                dias.add(despesa.data().plusDays(i));
            }
        }
        return dias;
    }
}
