package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.LimiteCategoria;
import reembolso.dominio.Periodicidade;
import reembolso.dominio.TabelaPoliticas;

/** T-025: RN-013 / AMB-021 / AMB-022 / AMB-025 — tabela de limites por centro de custo. */
class Rn013TabelaCentroCustoTest {

    private static LimiteCategoria dia(String limite) {
        return new LimiteCategoria(new BigDecimal(limite), Periodicidade.DIA);
    }

    private static LimiteCategoria diaria(String limite) {
        return new LimiteCategoria(new BigDecimal(limite), Periodicidade.DIARIA);
    }

    /** Mesmo conteúdo de exemplos/envelope/politica-v4.json, sem ler arquivo. */
    private static final TabelaPoliticas V4 = new TabelaPoliticas(
            Map.of(
                    "CC-ENG-PLATAFORMA", Map.of("alimentacao", dia("75.00"), "transporte_urbano", dia("80.00"),
                            "hospedagem", diaria("0.00")),
                    "CC-COMERCIAL", Map.of("alimentacao", dia("90.00"), "transporte_urbano", dia("150.00"),
                            "hospedagem", diaria("400.00"), "representacao", dia("300.00")),
                    "CC-ADM", Map.of("alimentacao", dia("45.00"), "transporte_urbano", dia("60.00"))),
            Map.of("alimentacao", dia("60.00"), "transporte_urbano", dia("80.00"), "hospedagem", diaria("250.00")),
            new BigDecimal("100.00"),
            new BigDecimal("50"));

    @Test
    @DisplayName("RN-013: CC-COMERCIAL usa a própria tabela (alimentação 90, representação 300)")
    void centro_com_entrada_usa_a_propria_tabela() {
        Politica politica = SelecaoTabela.politicaPara("CC-COMERCIAL", V4);

        assertEquals("CC-COMERCIAL", politica.nomeTabela());
        assertEquals(new BigDecimal("90.00"), politica.limiteDiario("alimentacao", Ampliacao.NENHUM));
        assertTrue(politica.elegivel("representacao"));
    }

    @Test
    @DisplayName("RN-013: CC-SUPORTE-N2 (sem entrada) usa a tabela padrão")
    void centro_sem_entrada_usa_padrao() {
        Politica politica = SelecaoTabela.politicaPara("CC-SUPORTE-N2", V4);

        assertEquals("padrao", politica.nomeTabela());
        assertEquals(new BigDecimal("60.00"), politica.limiteDiario("alimentacao", Ampliacao.NENHUM));
    }

    @Test
    @DisplayName("RN-013 / AMB-021: CC-ADM tem entrada sem hospedagem → hospedagem não elegível (não completa com o padrão)")
    void tabela_incompleta_nao_completa_com_padrao() {
        Politica politica = SelecaoTabela.politicaPara("CC-ADM", V4);

        assertEquals("CC-ADM", politica.nomeTabela());
        assertFalse(politica.elegivel("hospedagem"));
        assertEquals(new BigDecimal("45.00"), politica.limiteDiario("alimentacao", Ampliacao.NENHUM));
    }

    @Test
    @DisplayName("RN-003 / AMB-022: representação só existe no CC-COMERCIAL; na tabela padrão não é elegível")
    void representacao_so_no_comercial() {
        assertFalse(SelecaoTabela.politicaPara("CC-SUPORTE-N2", V4).elegivel("representacao"));
        assertFalse(SelecaoTabela.politicaPara("CC-ENG-PLATAFORMA", V4).elegivel("representacao"));
    }

    @Test
    @DisplayName("RN-013 / AMB-025: espaços nas pontas são ignorados (\" CC-COMERCIAL \" → CC-COMERCIAL)")
    void espacos_nas_pontas_ignorados() {
        assertEquals("CC-COMERCIAL", SelecaoTabela.politicaPara(" CC-COMERCIAL ", V4).nomeTabela());
    }

    @Test
    @DisplayName("RN-013 / AMB-025: outra grafia não é o mesmo CC (cc-comercial → padrão)")
    void outra_grafia_usa_padrao() {
        assertEquals("padrao", SelecaoTabela.politicaPara("cc-comercial", V4).nomeTabela());
    }

    @Test
    @DisplayName("RN-001: nomes de categoria da política são normalizados (\"Alimentação\" no arquivo = alimentacao)")
    void categorias_da_politica_normalizadas() {
        TabelaPoliticas comAcento = new TabelaPoliticas(Map.of(),
                Map.of("Alimentação", dia("60.00")), new BigDecimal("100.00"), new BigDecimal("50"));

        assertTrue(SelecaoTabela.politicaPara("CC-X", comAcento).elegivel("alimentacao"));
    }

    @Test
    @DisplayName("RN-013 / AMB-027: piso da NF e percentual de acréscimo vêm do arquivo, para qualquer tabela")
    void piso_e_percentual_do_arquivo() {
        Politica politica = SelecaoTabela.politicaPara("CC-COMERCIAL", V4);

        assertEquals(new BigDecimal("100.00"), politica.pisoNotaFiscal());
        assertEquals(new BigDecimal("50"), politica.percentualAcrescimo());
    }
}
