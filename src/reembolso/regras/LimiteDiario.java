package reembolso.regras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.Despesa;
import reembolso.dominio.Grupo;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/**
 * RN-004: despesas aprovadas de alimentação e de transporte urbano formam um grupo por dia e
 * categoria; o reembolso do grupo é o menor valor entre a soma e o limite do dia.
 */
public final class LimiteDiario {

    private static final Set<String> CATEGORIAS = Set.of(Politica.ALIMENTACAO, Politica.TRANSPORTE_URBANO);

    private LimiteDiario() {
    }

    private record ChaveGrupo(LocalDate data, String categoria) {
    }

    /**
     * Só despesas com status {@link Status#APROVADA} entram nos grupos (RN-012): as demais não
     * consomem limite. Os grupos saem ordenados por data e categoria.
     */
    public static List<Grupo> agrupar(List<ResultadoDespesa> resultados, Set<LocalDate> diasViagem, Politica politica) {
        Map<ChaveGrupo, List<Despesa>> porDiaECategoria = new LinkedHashMap<>();
        for (ResultadoDespesa resultado : resultados) {
            Despesa despesa = resultado.despesa();
            if (resultado.status() != Status.APROVADA || !CATEGORIAS.contains(despesa.categoria())) {
                continue;
            }
            porDiaECategoria
                    .computeIfAbsent(new ChaveGrupo(despesa.data(), despesa.categoria()), chave -> new ArrayList<>())
                    .add(despesa);
        }

        List<Grupo> grupos = new ArrayList<>();
        porDiaECategoria.forEach((chave, despesas) -> grupos.add(grupo(chave, despesas, diasViagem, politica)));
        grupos.sort(Comparator.comparing(Grupo::data).thenComparing(Grupo::categoria));
        return grupos;
    }

    private static Grupo grupo(ChaveGrupo chave, List<Despesa> despesas, Set<LocalDate> diasViagem, Politica politica) {
        BigDecimal total = despesas.stream().map(Despesa::valor).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
        Ampliacao ampliacao = AmpliacaoLimite.determinar(chave.data(), chave.categoria(), despesas, diasViagem);
        BigDecimal limite = politica.limiteDiario(chave.categoria(), ampliacao);
        return new Grupo(
                chave.data(),
                chave.categoria(),
                despesas.stream().map(Despesa::id).toList(),
                null,
                total,
                limite,
                ampliacao,
                total.min(limite));
    }
}
