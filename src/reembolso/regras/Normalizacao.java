package reembolso.regras;

import java.text.Normalizer;
import java.util.Locale;

/** Normalização de texto usada nas comparações da spec (plano, DT-006). */
public final class Normalizacao {

    private Normalizacao() {
    }

    /** Remove acentos e converte para minúsculas: "Alimentação" → "alimentacao". */
    public static String texto(String valor) {
        String semAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT);
    }

    /** RN-001: categoria comparada sem diferenciar maiúsculas, minúsculas e acentos. */
    public static String categoria(String categoria) {
        return texto(categoria);
    }
}
