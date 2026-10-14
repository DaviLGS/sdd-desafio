package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static reembolso.Despesas.despesa;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reembolso.dominio.Motivo;
import reembolso.dominio.Status;

/** T-006: RN-003 — categorias elegíveis. */
class Rn003CategoriaElegivelTest {

    @Test
    @DisplayName("RN-003: coworking (d-005) → CATEGORIA_FORA_DA_POLITICA")
    void coworking_fora_da_politica() {
        var d005 = despesa("d-005", "2026-07-07", "coworking", "89.00", true);

        assertEquals(Optional.of(Motivo.CATEGORIA_FORA_DA_POLITICA), CategoriaElegivel.verificar(d005, Politica.V3));
    }

    @Test
    @DisplayName("RN-003 / RN-012: categoria fora da política leva a status recusada")
    void categoria_fora_leva_a_recusada() {
        assertEquals(Status.RECUSADA, Motivo.CATEGORIA_FORA_DA_POLITICA.statusResultante());
    }

    @DisplayName("RN-003: as 3 categorias elegíveis passam")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"alimentacao", "transporte_urbano", "hospedagem"})
    void categorias_elegiveis_passam(String categoria) {
        var despesa = despesa("d-1", "2026-07-03", categoria, "10.00", true);

        assertEquals(Optional.empty(), CategoriaElegivel.verificar(despesa, Politica.V3));
    }

    @DisplayName("RN-003: categorias parecidas mas fora da política são recusadas")
    @ParameterizedTest(name = "categoria \"{0}\"")
    @ValueSource(strings = {"transporte", "transporte_interurbano", "alimentacao_extra", ""})
    void categorias_parecidas_sao_recusadas(String categoria) {
        var despesa = despesa("d-1", "2026-07-03", categoria, "10.00", true);

        assertEquals(Optional.of(Motivo.CATEGORIA_FORA_DA_POLITICA), CategoriaElegivel.verificar(despesa, Politica.V3));
    }
}
