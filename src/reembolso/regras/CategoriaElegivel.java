package reembolso.regras;

import java.util.Optional;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;

/** RN-003: só as categorias da política são reembolsáveis. */
public final class CategoriaElegivel {

    private CategoriaElegivel() {
    }

    /** Espera a despesa já normalizada (RN-001). */
    public static Optional<Motivo> verificar(Despesa despesa, Politica politica) {
        if (politica.categoriasElegiveis().contains(despesa.categoria())) {
            return Optional.empty();
        }
        return Optional.of(Motivo.CATEGORIA_FORA_DA_POLITICA);
    }
}
