package reembolso.regras;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/** Texto legível da decisão sobre cada despesa (spec, seção 4, campo {@code justificativa}). */
public final class Justificativa {

    private Justificativa() {
    }

    public static String para(ResultadoDespesa resultado, Politica politica) {
        Despesa despesa = resultado.despesa();
        if (resultado.status() == Status.APROVADA) {
            return "Aprovada; limite aplicado no grupo " + despesa.data() + "/" + despesa.categoria() + ".";
        }
        String prefixo = resultado.status() == Status.RECUSADA ? "Recusada" : "Em revisão";
        return prefixo + ": " + textos(resultado.motivos(), politica);
    }

    private static String textos(List<Motivo> motivos, Politica politica) {
        return motivos.stream().map(m -> texto(m, politica)).collect(Collectors.joining(" "));
    }

    static String texto(Motivo motivo, Politica politica) {
        return switch (motivo) {
            case CATEGORIA_FORA_DA_POLITICA -> "categoria fora da política de reembolso (RN-003).";
            case FORA_DO_PERIODO -> "data fora do período de competência (RN-010).";
            case DUPLICATA -> "despesa com os mesmos dados de outra; todas as cópias aguardam revisão (RN-009).";
            case NF_AUSENTE -> "valor a partir de " + reais(politica.pisoNotaFiscal())
                    + " sem nota fiscal (RN-006).";
            case NOITES_NAO_IDENTIFICADAS -> "número de noites não identificado na descrição (RN-005).";
            case ESTORNO_SEM_DESPESA -> "estorno sem despesa suficiente no mesmo dia e categoria para abater (RN-011).";
            case CAMBIO_INDISPONIVEL -> "sem cotação da moeda na data da despesa ou antes dela no arquivo de câmbio (RN-014).";
        };
    }

    private static String reais(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        return "R$ " + formato.format(valor);
    }
}
