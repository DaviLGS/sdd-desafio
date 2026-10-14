package reembolso.dominio;

/** Situação de uma despesa na saída (RN-012). */
public enum Status {
    APROVADA("aprovada"),
    EM_REVISAO("em_revisao"),
    RECUSADA("recusada");

    private final String codigo;

    Status(String codigo) {
        this.codigo = codigo;
    }

    /** Valor que aparece no campo {@code status} da saída. */
    public String codigo() {
        return codigo;
    }
}
