package reembolso.regras;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.Despesa;

/**
 * Decide se o limite de um grupo é ampliado e por quê (RN-007, RN-008).
 *
 * <p>Hospedagem nunca é ampliada. As ampliações não acumulam; quando viagem e fim de
 * semana/plantão coincidem, o motivo informado é viagem (D-003).
 */
public final class AmpliacaoLimite {

    private static final String PLANTAO = "plantao";

    private AmpliacaoLimite() {
    }

    /**
     * @param data              dia do grupo
     * @param categoria         categoria normalizada do grupo
     * @param despesasDoGrupo   despesas do grupo; basta uma com "plantão" para ampliar o grupo
     * @param diasViagem        dias de viagem calculados pela RN-007
     */
    public static Ampliacao determinar(
            LocalDate data, String categoria, List<Despesa> despesasDoGrupo, Set<LocalDate> diasViagem) {
        if (Politica.HOSPEDAGEM.equals(categoria)) {
            return Ampliacao.NENHUM;
        }
        if (diasViagem.contains(data)) {
            return Ampliacao.VIAGEM;
        }
        if (fimDeSemana(data) || algumaEmPlantao(despesasDoGrupo)) {
            return Ampliacao.FIM_DE_SEMANA_OU_PLANTAO;
        }
        return Ampliacao.NENHUM;
    }

    private static boolean fimDeSemana(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    private static boolean algumaEmPlantao(List<Despesa> despesas) {
        return despesas.stream().anyMatch(d -> Normalizacao.texto(d.descricao()).contains(PLANTAO));
    }
}
