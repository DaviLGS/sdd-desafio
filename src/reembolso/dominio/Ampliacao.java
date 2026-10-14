package reembolso.dominio;

/** Por que o limite de um grupo foi (ou não) ampliado (RN-007, RN-008). */
public enum Ampliacao {
    NENHUM("nenhum"),
    VIAGEM("viagem"),
    FIM_DE_SEMANA_OU_PLANTAO("fim_de_semana_ou_plantao");

    private final String codigo;

    Ampliacao(String codigo) {
        this.codigo = codigo;
    }

    /** Valor que aparece no campo {@code limite_ampliado} da saída. */
    public String codigo() {
        return codigo;
    }
}
