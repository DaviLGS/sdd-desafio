package reembolso.regras;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.Despesa;
import reembolso.dominio.Grupo;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/**
 * RN-005: cada hospedagem aprovada forma o seu próprio grupo, com limite de R$ 250 × noites e sem
 * ampliação. Hospedagens que cobrem o mesmo dia são limitadas isoladamente.
 */
public final class LimiteHospedagem {

    private LimiteHospedagem() {
    }

    /** Só hospedagens com status {@link Status#APROVADA} formam grupo; saem ordenadas por data. */
    public static List<Grupo> agrupar(List<ResultadoDespesa> resultados, Politica politica) {
        List<Grupo> grupos = new ArrayList<>();
        for (ResultadoDespesa resultado : resultados) {
            Despesa despesa = resultado.despesa();
            if (resultado.status() != Status.APROVADA || !Politica.HOSPEDAGEM.equals(despesa.categoria())) {
                continue;
            }
            OptionalInt noites = NoitesHospedagem.identificar(despesa.descricao());
            if (noites.isEmpty()) {
                // Não acontece no fluxo do motor: sem noites, a despesa já está em revisão (RN-005).
                throw new IllegalStateException("hospedagem aprovada sem noites identificáveis: " + despesa.id());
            }
            BigDecimal valor = despesa.valor().setScale(2);
            BigDecimal limite = politica.limiteHospedagem(noites.getAsInt());
            grupos.add(new Grupo(
                    despesa.data(),
                    Politica.HOSPEDAGEM,
                    List.of(despesa.id()),
                    noites.getAsInt(),
                    valor,
                    limite,
                    Ampliacao.NENHUM,
                    valor.min(limite)));
        }
        grupos.sort(Comparator.comparing(Grupo::data));
        return grupos;
    }
}
