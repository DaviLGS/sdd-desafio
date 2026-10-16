package reembolso.dominio;

/** Motivos de revisão ou recusa, com o status que cada um provoca (spec, seção 4). */
public enum Motivo {
    CATEGORIA_FORA_DA_POLITICA(Status.RECUSADA),
    FORA_DO_PERIODO(Status.EM_REVISAO),
    DUPLICATA(Status.EM_REVISAO),
    NF_AUSENTE(Status.EM_REVISAO),
    NOITES_NAO_IDENTIFICADAS(Status.EM_REVISAO),
    ESTORNO_SEM_DESPESA(Status.EM_REVISAO),
    CAMBIO_INDISPONIVEL(Status.EM_REVISAO);

    private final Status statusResultante;

    Motivo(Status statusResultante) {
        this.statusResultante = statusResultante;
    }

    public Status statusResultante() {
        return statusResultante;
    }
}
