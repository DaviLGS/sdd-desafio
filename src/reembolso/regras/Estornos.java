package reembolso.regras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/**
 * RN-011: estornos (valor negativo) abatem do grupo da mesma categoria e dia, ou vão para revisão.
 *
 * <p>Estornos aprovados continuam aprovados e entram na soma do grupo diário (RN-004). Os que não
 * podem ser abatidos recebem {@link Motivo#ESTORNO_SEM_DESPESA} — inclusive os que já estavam em
 * revisão ou recusados por outro motivo (D-005).
 */
public final class Estornos {

    private Estornos() {
    }

    private record ChaveDia(LocalDate data, String categoria) {
    }

    /** Devolve os resultados na mesma ordem, com os estornos não abatíveis movidos para revisão. */
    public static List<ResultadoDespesa> aplicar(List<ResultadoDespesa> resultados) {
        Map<ChaveDia, BigDecimal> despesasPorDia = new HashMap<>();
        Map<ChaveDia, BigDecimal> estornosPorDia = new HashMap<>();
        for (ResultadoDespesa resultado : resultados) {
            if (resultado.status() != Status.APROVADA) {
                continue;
            }
            Despesa despesa = resultado.despesa();
            ChaveDia chave = new ChaveDia(despesa.data(), despesa.categoria());
            if (estorno(despesa)) {
                estornosPorDia.merge(chave, despesa.valor().negate(), BigDecimal::add);
            } else {
                despesasPorDia.merge(chave, despesa.valor(), BigDecimal::add);
            }
        }

        // D-004: os estornos de um dia e categoria são avaliados em conjunto.
        Set<ChaveDia> diasSemSaldo = new HashSet<>();
        estornosPorDia.forEach((chave, totalEstornado) -> {
            BigDecimal totalDespesas = despesasPorDia.getOrDefault(chave, BigDecimal.ZERO);
            boolean hospedagem = Politica.HOSPEDAGEM.equals(chave.categoria());
            if (hospedagem || totalDespesas.compareTo(totalEstornado) < 0) {
                diasSemSaldo.add(chave);
            }
        });

        List<ResultadoDespesa> atualizados = new ArrayList<>(resultados.size());
        for (ResultadoDespesa resultado : resultados) {
            Despesa despesa = resultado.despesa();
            boolean abatido = resultado.status() == Status.APROVADA
                    && !diasSemSaldo.contains(new ChaveDia(despesa.data(), despesa.categoria()));
            // D-005: estorno que já tem outro motivo também recebe ESTORNO_SEM_DESPESA.
            atualizados.add(estorno(despesa) && !abatido ? semDespesa(resultado) : resultado);
        }
        return atualizados;
    }

    private static boolean estorno(Despesa despesa) {
        return despesa.valor().signum() < 0;
    }

    private static ResultadoDespesa semDespesa(ResultadoDespesa resultado) {
        List<Motivo> motivos = new ArrayList<>(resultado.motivos());
        motivos.add(Motivo.ESTORNO_SEM_DESPESA);
        return new ResultadoDespesa(resultado.despesa(), StatusDespesa.consolidar(motivos), motivos,
                resultado.justificativa());
    }
}
