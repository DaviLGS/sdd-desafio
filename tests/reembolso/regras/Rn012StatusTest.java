package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reembolso.dominio.Motivo;
import reembolso.dominio.Status;

/** T-011: RN-012 — status da despesa a partir dos motivos. */
class Rn012StatusTest {

    @Test
    @DisplayName("RN-012: sem motivos → aprovada")
    void sem_motivos_aprovada() {
        assertEquals(Status.APROVADA, StatusDespesa.consolidar(List.of()));
    }

    @DisplayName("RN-012: um único motivo de revisão → em_revisao")
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Motivo.class, names = "CATEGORIA_FORA_DA_POLITICA", mode = EnumSource.Mode.EXCLUDE)
    void um_motivo_de_revisao(Motivo motivo) {
        assertEquals(Status.EM_REVISAO, StatusDespesa.consolidar(List.of(motivo)));
    }

    @Test
    @DisplayName("RN-012: vários motivos de revisão (fora do período, sem NF e duplicata) → em_revisao")
    void varios_motivos_de_revisao() {
        assertEquals(Status.EM_REVISAO,
                StatusDespesa.consolidar(List.of(Motivo.FORA_DO_PERIODO, Motivo.NF_AUSENTE, Motivo.DUPLICATA)));
    }

    @Test
    @DisplayName("RN-012: só CATEGORIA_FORA_DA_POLITICA → recusada")
    void so_categoria_fora_recusada() {
        assertEquals(Status.RECUSADA, StatusDespesa.consolidar(List.of(Motivo.CATEGORIA_FORA_DA_POLITICA)));
    }

    @Test
    @DisplayName("RN-012: coworking de 15/04 — CATEGORIA_FORA_DA_POLITICA + FORA_DO_PERIODO → recusada (aceite da spec)")
    void recusa_prevalece_sobre_revisao() {
        assertEquals(Status.RECUSADA,
                StatusDespesa.consolidar(List.of(Motivo.CATEGORIA_FORA_DA_POLITICA, Motivo.FORA_DO_PERIODO)));
    }

    @Test
    @DisplayName("RN-012: a ordem dos motivos não muda o status")
    void ordem_nao_importa() {
        assertEquals(Status.RECUSADA,
                StatusDespesa.consolidar(List.of(Motivo.FORA_DO_PERIODO, Motivo.CATEGORIA_FORA_DA_POLITICA)));
    }
}
