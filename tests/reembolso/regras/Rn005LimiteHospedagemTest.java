package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reembolso.Despesas.despesa;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reembolso.Politicas;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.Despesa;
import reembolso.dominio.Grupo;
import reembolso.dominio.Motivo;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;

/** T-015: RN-005 / AMB-004 / AMB-005 / AMB-013 — limite de hospedagem de R$ 250 × noites. */
class Rn005LimiteHospedagemTest {

    private static ResultadoDespesa aprovada(Despesa despesa) {
        return new ResultadoDespesa(despesa, Status.APROVADA, List.of(), "");
    }

    private static Grupo unicoGrupo(Despesa despesa) {
        List<Grupo> grupos = LimiteHospedagem.agrupar(List.of(aprovada(despesa)), Politicas.PADRAO);
        assertEquals(1, grupos.size());
        return grupos.get(0);
    }

    @Test
    @DisplayName("RN-005 / AMB-004: d-010 R$ 480,00 com \"2 diarias\" → limite 500,00, reembolsa 480,00")
    void d010_duas_diarias() {
        var d010 = despesa("d-010", "2026-07-14", "hospedagem", "480.00", true, "Hotel Rio - 2 diarias");

        Grupo grupo = unicoGrupo(d010);

        assertEquals("hospedagem", grupo.categoria());
        assertEquals(List.of("d-010"), grupo.idsDespesas());
        assertEquals(2, grupo.noites());
        assertEquals(new BigDecimal("480.00"), grupo.valorTotal());
        assertEquals(new BigDecimal("500.00"), grupo.limite());
        assertEquals(Ampliacao.NENHUM, grupo.ampliacao());
        assertEquals(new BigDecimal("480.00"), grupo.valorReembolsado());
    }

    @DisplayName("RN-005 / AMB-005: limite de 250 × noites, excedente não reembolsado")
    @ParameterizedTest(name = "R$ {0} com \"{1}\" → limite {2}, reembolsa {3}")
    @CsvSource(delimiter = '|', value = {
        "750.00 | Hotel 3 noites | 750.00 | 750.00",
        "900.00 | Hotel 3 noites | 750.00 | 750.00",
        "250.00 | Hotel 1 noite  | 250.00 | 250.00",
        "250.01 | Hotel 1 noite  | 250.00 | 250.00",
        "100.00 | Hotel 1 noite  | 250.00 | 100.00",
    })
    void limite_por_noite(String valor, String descricao, String limite, String reembolso) {
        Grupo grupo = unicoGrupo(despesa("d-1", "2026-07-14", "hospedagem", valor, true, descricao));

        assertEquals(new BigDecimal(limite), grupo.limite());
        assertEquals(new BigDecimal(reembolso), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-005 / AMB-013: hospedagem num sábado (e com \"plantao\" na descrição) continua com 250/noite")
    void hospedagem_nunca_ampliada() {
        var hotel = despesa("d-1", "2026-07-18", "hospedagem", "400.00", true, "Hotel 1 noite plantao");

        Grupo grupo = unicoGrupo(hotel);

        assertEquals(Ampliacao.NENHUM, grupo.ampliacao());
        assertEquals(new BigDecimal("250.00"), grupo.limite());
        assertEquals(new BigDecimal("250.00"), grupo.valorReembolsado());
    }

    @Test
    @DisplayName("RN-005: hospedagens sobrepostas são limitadas isoladamente, cada uma em seu grupo")
    void hospedagens_sobrepostas_isoladas() {
        var hotelA = despesa("d-1", "2026-07-14", "hospedagem", "600.00", true, "Hotel A 2 noites");
        var hotelB = despesa("d-2", "2026-07-15", "hospedagem", "600.00", true, "Hotel B 2 noites");

        List<Grupo> grupos = LimiteHospedagem.agrupar(List.of(aprovada(hotelA), aprovada(hotelB)), Politicas.PADRAO);

        assertEquals(2, grupos.size());
        assertEquals(new BigDecimal("500.00"), grupos.get(0).valorReembolsado());
        assertEquals(new BigDecimal("500.00"), grupos.get(1).valorReembolsado());
    }

    @Test
    @DisplayName("RN-005 / RN-012: hospedagem em revisão (d-013 sem NF) não forma grupo")
    void hospedagem_em_revisao_sem_grupo() {
        var d013 = despesa("d-013", "2026-07-22", "hospedagem", "690.00", false, "Airbnb 3 noites");
        var emRevisao = new ResultadoDespesa(d013, Status.EM_REVISAO, List.of(Motivo.NF_AUSENTE), "");

        assertTrue(LimiteHospedagem.agrupar(List.of(emRevisao), Politicas.PADRAO).isEmpty());
    }

    @Test
    @DisplayName("RN-005: alimentação e transporte não entram nos grupos de hospedagem")
    void outras_categorias_fora() {
        var almoco = despesa("d-1", "2026-07-14", "alimentacao", "50.00", true, "Almoco 2 noites");

        assertTrue(LimiteHospedagem.agrupar(List.of(aprovada(almoco)), Politicas.PADRAO).isEmpty());
    }
}
