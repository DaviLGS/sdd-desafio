package reembolso.dominio;

/** Bloco {@code colaborador} da entrada; apenas repassado para a saída (spec, seção 4). */
public record Colaborador(String id, String nome, String centroCusto) {
}
