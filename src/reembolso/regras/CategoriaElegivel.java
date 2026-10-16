package reembolso.regras;

import java.util.Optional;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;

/** RN-003: só as categorias da tabela aplicada, com limite maior que zero, são reembolsáveis. */
public final class CategoriaElegivel {

    private CategoriaElegivel() {
    }

    /** Espera a despesa já normalizada (RN-001). */
    public static Optional<Motivo> verificar(Despesa despesa, Politica politica) {
        if (politica.elegivel(despesa.categoria())) {
            return Optional.empty();
        }
        return Optional.of(Motivo.CATEGORIA_FORA_DA_POLITICA);
    }
}
