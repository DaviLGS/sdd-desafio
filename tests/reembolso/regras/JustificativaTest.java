package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reembolso.Despesas.despesa;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.dominio.Motivo;
import reembolso.dominio.Resultado;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;
import reembolso.io.LeitorEntrada;

/** T-018: justificativa textual de cada despesa (spec, seção 4). */
class JustificativaTest {

    private static String justificativa(Status status, List<Motivo> motivos) {
        var d = despesa("d-1", "2026-07-03", "alimentacao", "72.50", true);
        return Justificativa.para(new ResultadoDespesa(d, status, motivos, ""), Politica.V3);
    }

    @Test
    @DisplayName("Seção 4: despesa aprovada cita o grupo (texto igual ao exemplo da spec)")
    void aprovada_cita_o_grupo() {
        assertEquals("Aprovada; limite aplicado no grupo 2026-07-03/alimentacao.",
                justificativa(Status.APROVADA, List.of()));
    }

    @Test
    @DisplayName("Seção 4 / RN-006: NF ausente cita o piso da política formatado em reais")
    void nf_ausente() {
        assertEquals("Em revisão: valor a partir de R$ 100,00 sem nota fiscal (RN-006).",
                justificativa(Status.EM_REVISAO, List.of(Motivo.NF_AUSENTE)));
    }

    @Test
    @DisplayName("Seção 4 / RN-003: recusada por categoria")
    void recusada_por_categoria() {
        assertEquals("Recusada: categoria fora da política de reembolso (RN-003).",
                justificativa(Status.RECUSADA, List.of(Motivo.CATEGORIA_FORA_DA_POLITICA)));
    }

    @Test
    @DisplayName("Seção 4 / RN-012: vários motivos aparecem todos, na ordem")
    void varios_motivos_aparecem_todos() {
        assertEquals("Recusada: categoria fora da política de reembolso (RN-003)."
                        + " data fora do período de competência (RN-010).",
                justificativa(Status.RECUSADA, List.of(Motivo.CATEGORIA_FORA_DA_POLITICA, Motivo.FORA_DO_PERIODO)));
    }

    @Test
    @DisplayName("Seção 4: cada motivo tem um texto fixo, distinto e que cita a sua RN")
    void cada_motivo_tem_texto_proprio() {
        Set<String> textos = new HashSet<>();
        for (Motivo motivo : EnumSet.allOf(Motivo.class)) {
            String texto = Justificativa.texto(motivo, Politica.V3);
            assertFalse(texto.isBlank(), motivo.name());
            assertTrue(texto.matches(".*\\(RN-\\d{3}\\)\\.$"), "deve terminar citando a RN: " + motivo);
            textos.add(texto);
        }
        assertEquals(Motivo.values().length, textos.size());
    }

    @Test
    @DisplayName("Seção 4: o motor preenche a justificativa de toda despesa do exemplo")
    void motor_preenche_justificativas() throws Exception {
        var entrada = new LeitorEntrada().lerArquivo(Path.of("exemplos", "despesas-exemplo.json"));
        Resultado resultado = new MotorReembolso(Politica.V3).calcular(entrada);

        for (ResultadoDespesa r : resultado.despesas()) {
            assertFalse(r.justificativa().isBlank(), r.despesa().id());
        }
        assertEquals("Aprovada; limite aplicado no grupo 2026-07-03/alimentacao.",
                resultado.despesas().get(0).justificativa());
        assertEquals("Em revisão: despesa com os mesmos dados de outra; todas as cópias aguardam revisão (RN-009).",
                resultado.despesas().get(5).justificativa());
        assertEquals("Aprovada; limite aplicado no grupo 2026-07-31/alimentacao.",
                resultado.despesas().get(13).justificativa());
    }
}
