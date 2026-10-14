package reembolso.regras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import reembolso.dominio.Despesa;
import reembolso.dominio.Entrada;
import reembolso.dominio.Grupo;
import reembolso.dominio.Motivo;
import reembolso.dominio.Resultado;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/** Orquestra as regras na ordem da seção 8 da spec. Sem I/O. */
public final class MotorReembolso {

    private final Politica politica;

    public MotorReembolso(Politica politica) {
        this.politica = politica;
    }

    public Resultado calcular(Entrada entrada) {
        // Passo 1: arredondamento (RN-002) e normalização de categoria (RN-001).
        List<Despesa> despesas = entrada.despesas().stream().map(MotorReembolso::normalizar).toList();

        // Passos 2 e 3: verificações por despesa e status (RN-003, RN-010, RN-009, RN-006, RN-005, RN-012).
        Set<String> duplicadas = Duplicatas.identificar(despesas);
        List<ResultadoDespesa> resultados = new ArrayList<>();
        for (Despesa despesa : despesas) {
            List<Motivo> motivos = new ArrayList<>();
            CategoriaElegivel.verificar(despesa, politica).ifPresent(motivos::add);
            PeriodoCompetencia.verificar(despesa, entrada.periodo()).ifPresent(motivos::add);
            if (duplicadas.contains(despesa.id())) {
                motivos.add(Motivo.DUPLICATA);
            }
            NotaFiscal.verificar(despesa, politica).ifPresent(motivos::add);
            NoitesHospedagem.verificar(despesa).ifPresent(motivos::add);
            resultados.add(new ResultadoDespesa(despesa, StatusDespesa.consolidar(motivos), motivos, ""));
        }

        // Passo 4: dias de viagem (RN-007), inclusive de hospedagens em revisão.
        Set<LocalDate> diasViagem = DiasViagem.calcular(despesas);

        // Passo 5: estornos (RN-011).
        resultados = Estornos.aplicar(resultados);

        // Passo 6: grupos e limites (RN-004, RN-005, RN-008).
        List<Grupo> grupos = new ArrayList<>();
        grupos.addAll(LimiteDiario.agrupar(resultados, diasViagem, politica));
        grupos.addAll(LimiteHospedagem.agrupar(resultados, politica));
        grupos.sort(Comparator.comparing(Grupo::data).thenComparing(Grupo::categoria));

        // Passo 7: resumo.
        BigDecimal total = grupos.stream().map(Grupo::valorReembolsado)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        Map<Status, Integer> quantidade = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            quantidade.put(status, 0);
        }
        resultados.forEach(r -> quantidade.merge(r.status(), 1, Integer::sum));

        return new Resultado(entrada.colaborador(), entrada.periodo(), resultados, grupos, total, quantidade);
    }

    private static Despesa normalizar(Despesa despesa) {
        return new Despesa(
                despesa.id(),
                despesa.data(),
                despesa.categoriaOriginal(),
                Normalizacao.categoria(despesa.categoriaOriginal()),
                despesa.descricao(),
                despesa.fornecedor(),
                Arredondamento.arredondar(despesa.valor()),
                despesa.temNotaFiscal());
    }
}
