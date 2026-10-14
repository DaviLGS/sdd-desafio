package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reembolso.Despesas.despesa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.dominio.Despesa;
import reembolso.dominio.Motivo;
import reembolso.dominio.Status;

/** T-008: RN-009 / AMB-016 — duplicatas. */
class Rn009DuplicataTest {

    private static Despesa almoco(String id) {
        return despesa(id, "2026-07-09", "alimentacao", "54.90", true, "Almoco");
    }

    private static Despesa comCampos(
            String id, String data, String categoria, String descricao, String fornecedor, String valor, boolean nf) {
        return new Despesa(id, LocalDate.parse(data), categoria, categoria, descricao, fornecedor,
                new BigDecimal(valor), nf);
    }

    @Test
    @DisplayName("RN-009 / AMB-016: d-006 e d-007 idênticas → as duas são duplicatas")
    void duas_identicas_ambas_duplicatas() {
        assertEquals(Set.of("d-006", "d-007"), Duplicatas.identificar(List.of(almoco("d-006"), almoco("d-007"))));
    }

    @Test
    @DisplayName("RN-009: três cópias → as três são duplicatas")
    void tres_copias_todas_duplicatas() {
        var despesas = List.of(almoco("d-1"), almoco("d-2"), almoco("d-3"));

        assertEquals(Set.of("d-1", "d-2", "d-3"), Duplicatas.identificar(despesas));
    }

    @Test
    @DisplayName("RN-009: despesa sem cópia não é duplicata, mesmo ao lado de um par duplicado")
    void despesa_unica_nao_e_duplicata() {
        var unica = despesa("d-9", "2026-07-03", "alimentacao", "72.50", true, "Almoco com cliente");

        Set<String> duplicadas = Duplicatas.identificar(List.of(almoco("d-1"), unica, almoco("d-2")));

        assertEquals(Set.of("d-1", "d-2"), duplicadas);
    }

    @Test
    @DisplayName("RN-009: diferença só na descrição → não é duplicata")
    void diferenca_na_descricao_nao_e_duplicata() {
        var a = despesa("d-1", "2026-07-09", "alimentacao", "54.90", true, "Almoco");
        var b = despesa("d-2", "2026-07-09", "alimentacao", "54.90", true, "Almoco com equipe");

        assertTrue(Duplicatas.identificar(List.of(a, b)).isEmpty());
    }

    @Test
    @DisplayName("RN-009: diferença em qualquer outro campo (data, fornecedor, valor, NF, categoria) → não é duplicata")
    void diferenca_em_cada_campo_nao_e_duplicata() {
        var base = comCampos("d-0", "2026-07-09", "alimentacao", "Almoco", "Bistro", "54.90", true);

        var outraData = comCampos("d-1", "2026-07-10", "alimentacao", "Almoco", "Bistro", "54.90", true);
        var outroFornecedor = comCampos("d-2", "2026-07-09", "alimentacao", "Almoco", "Padaria", "54.90", true);
        var outroValor = comCampos("d-3", "2026-07-09", "alimentacao", "Almoco", "Bistro", "54.91", true);
        var outraNf = comCampos("d-4", "2026-07-09", "alimentacao", "Almoco", "Bistro", "54.90", false);
        var outraCategoria = comCampos("d-5", "2026-07-09", "transporte_urbano", "Almoco", "Bistro", "54.90", true);

        for (Despesa outra : List.of(outraData, outroFornecedor, outroValor, outraNf, outraCategoria)) {
            assertTrue(Duplicatas.identificar(List.of(base, outra)).isEmpty(), "não deveria ser duplicata: " + outra.id());
        }
    }

    @Test
    @DisplayName("RN-009 / RN-001: ALIMENTACAO × alimentacao com o resto igual → é duplicata (compara após normalizar)")
    void categoria_normalizada_e_duplicata() {
        var maiuscula = new Despesa("d-1", LocalDate.parse("2026-07-09"), "ALIMENTACAO", "alimentacao",
                "Almoco", "Bistro", new BigDecimal("54.90"), true);
        var minuscula = new Despesa("d-2", LocalDate.parse("2026-07-09"), "alimentacao", "alimentacao",
                "Almoco", "Bistro", new BigDecimal("54.90"), true);

        assertEquals(Set.of("d-1", "d-2"), Duplicatas.identificar(List.of(maiuscula, minuscula)));
    }

    @Test
    @DisplayName("RN-009: nenhuma despesa → nenhuma duplicata")
    void lista_vazia() {
        assertTrue(Duplicatas.identificar(List.of()).isEmpty());
    }

    @Test
    @DisplayName("RN-009 / AMB-016: duplicata leva a em_revisao")
    void duplicata_leva_a_revisao() {
        assertEquals(Status.EM_REVISAO, Motivo.DUPLICATA.statusResultante());
    }
}
