package reembolso.regras;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;

/**
 * RN-005: o número de noites de uma hospedagem é lido da descrição — um número inteiro seguido de
 * "diária(s)" ou "noite(s)", sem diferenciar maiúsculas, minúsculas ou acentos.
 */
public final class NoitesHospedagem {

    /** Aplicado sobre a descrição já normalizada (sem acentos, minúsculas). */
    private static final Pattern NOITES = Pattern.compile("(\\d+)\\s*(?:diarias?|noites?)\\b");

    private NoitesHospedagem() {
    }

    /**
     * Devolve o número de noites, ou vazio se não for identificável: ausente, zero, escrito por
     * extenso, ou com mais de uma indicação na mesma descrição.
     */
    public static OptionalInt identificar(String descricao) {
        Matcher matcher = NOITES.matcher(Normalizacao.texto(descricao));
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        String numero = matcher.group(1);
        if (matcher.find()) {
            return OptionalInt.empty();
        }
        int noites;
        try {
            noites = Integer.parseInt(numero);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
        return noites > 0 ? OptionalInt.of(noites) : OptionalInt.empty();
    }

    /** Só se aplica a hospedagem; nas demais categorias não gera motivo. */
    public static Optional<Motivo> verificar(Despesa despesa) {
        if (!Politica.HOSPEDAGEM.equals(despesa.categoria())) {
            return Optional.empty();
        }
        if (identificar(despesa.descricao()).isEmpty()) {
            return Optional.of(Motivo.NOITES_NAO_IDENTIFICADAS);
        }
        return Optional.empty();
    }
}
