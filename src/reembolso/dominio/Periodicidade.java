package reembolso.dominio;

/** Periodicidade de um limite no arquivo de política (spec, seção 4). */
public enum Periodicidade {
    /** Limite por dia (RN-004): {@code "dia"} no arquivo. */
    DIA("dia"),
    /** Limite por noite (RN-005): {@code "diaria"} no arquivo. */
    DIARIA("diaria");

    private final String codigo;

    Periodicidade(String codigo) {
        this.codigo = codigo;
    }

    /** Valor como aparece no arquivo de política. */
    public String codigo() {
        return codigo;
    }
}
