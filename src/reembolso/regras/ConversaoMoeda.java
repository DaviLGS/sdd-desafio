package reembolso.regras;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;
import reembolso.dominio.TabelaCambio;

/** RN-014: conversão de moeda pela cotação da data da despesa (plano, DT-008). */
public final class ConversaoMoeda {

    private ConversaoMoeda() {
    }

    /** Cotação usada numa conversão: a data de onde ela veio e a taxa. */
    public record Cotacao(LocalDate data, BigDecimal taxa) {
    }

    /**
     * Cotação da moeda na data; sem cotação nessa data, a última anterior (AMB-031). Vazio se não
     * existe nenhuma cotação da moeda na data ou antes dela (AMB-030, AMB-032). O código da moeda é
     * comparado exatamente.
     */
    public static Optional<Cotacao> cotacao(String moeda, LocalDate data, TabelaCambio cambio) {
        NavigableMap<LocalDate, Map<String, BigDecimal>> ateAData = new TreeMap<>(cambio.taxas()).headMap(data, true);
        for (Map.Entry<LocalDate, Map<String, BigDecimal>> dia : ateAData.descendingMap().entrySet()) {
            BigDecimal taxa = dia.getValue().get(moeda);
            if (taxa != null) {
                return Optional.of(new Cotacao(dia.getKey(), taxa));
            }
        }
        return Optional.empty();
    }

    /**
     * Devolve a despesa com {@code valor} em BRL, arredondado uma única vez pela RN-002 (AMB-033).
     * Em BRL não há conversão. Sem cotação, {@code valor} fica {@code null}.
     */
    public static Despesa converter(Despesa despesa, TabelaCambio cambio) {
        if (!despesa.emMoedaEstrangeira()) {
            return comValor(despesa, Arredondamento.arredondar(despesa.valorOriginal()), null, null);
        }
        Optional<Cotacao> cotacao = cotacao(despesa.moeda(), despesa.data(), cambio);
        if (cotacao.isEmpty()) {
            return comValor(despesa, null, null, null);
        }
        BigDecimal convertido = despesa.valorOriginal().multiply(cotacao.get().taxa());
        return comValor(despesa, Arredondamento.arredondar(convertido), cotacao.get().taxa(), cotacao.get().data());
    }

    /** {@link Motivo#CAMBIO_INDISPONIVEL} quando a conversão não encontrou cotação. */
    public static Optional<Motivo> verificar(Despesa despesa) {
        return despesa.valor() == null ? Optional.of(Motivo.CAMBIO_INDISPONIVEL) : Optional.empty();
    }

    private static Despesa comValor(Despesa d, BigDecimal valor, BigDecimal taxa, LocalDate dataCotacao) {
        return new Despesa(d.id(), d.data(), d.categoriaOriginal(), d.categoria(), d.descricao(), d.fornecedor(),
                valor, d.temNotaFiscal(), d.moeda(), d.valorOriginal(), taxa, dataCotacao);
    }
}
