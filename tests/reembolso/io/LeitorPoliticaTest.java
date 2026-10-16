package reembolso.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reembolso.dominio.LimiteCategoria;
import reembolso.dominio.Periodicidade;
import reembolso.dominio.TabelaPoliticas;

/** T-024: leitura e validação do arquivo de política (spec, seção 4; AMB-028, AMB-029). */
class LeitorPoliticaTest {

    private static final Path POLITICA_V4 = Path.of("exemplos", "envelope", "politica-v4.json");

    private final LeitorPolitica leitor = new LeitorPolitica();

    private static LimiteCategoria limite(String valor, Periodicidade periodicidade) {
        return new LimiteCategoria(new BigDecimal(valor), periodicidade);
    }

    @Test
    @DisplayName("Seção 4: lê politica-v4.json — tabela padrão, 3 centros de custo, piso 100 e acréscimo 50")
    void le_politica_v4() throws Exception {
        TabelaPoliticas tabela = leitor.lerArquivo(POLITICA_V4);

        assertEquals(limite("60.00", Periodicidade.DIA), tabela.padrao().get("alimentacao"));
        assertEquals(limite("80.00", Periodicidade.DIA), tabela.padrao().get("transporte_urbano"));
        assertEquals(limite("250.00", Periodicidade.DIARIA), tabela.padrao().get("hospedagem"));
        assertEquals(Set.of("CC-ENG-PLATAFORMA", "CC-COMERCIAL", "CC-ADM"), tabela.centrosCusto().keySet());
        assertEquals(new BigDecimal("100.00"), tabela.pisoNotaFiscal());
        assertEquals(new BigDecimal("50"), tabela.percentualAcrescimo());
    }

    @Test
    @DisplayName("Seção 4: tabelas dos centros de custo como no arquivo (representação no CC-COMERCIAL; hospedagem 0 no CC-ENG-PLATAFORMA; CC-ADM sem hospedagem)")
    void tabelas_dos_centros_de_custo() throws Exception {
        TabelaPoliticas tabela = leitor.lerArquivo(POLITICA_V4);

        assertEquals(limite("300.00", Periodicidade.DIA), tabela.centrosCusto().get("CC-COMERCIAL").get("representacao"));
        assertEquals(limite("0.00", Periodicidade.DIARIA), tabela.centrosCusto().get("CC-ENG-PLATAFORMA").get("hospedagem"));
        assertEquals(Set.of("alimentacao", "transporte_urbano"), tabela.centrosCusto().get("CC-ADM").keySet());
    }

    @Test
    @DisplayName("AMB-029: vigencia, versao e campos informativos não são exigidos")
    void campos_informativos_opcionais() throws Exception {
        TabelaPoliticas tabela = leitor.lerTexto("""
                { "padrao": { "alimentacao": { "limite": 60.00, "periodicidade": "dia" } },
                  "nota_fiscal_obrigatoria_acima_de": 100.00, "acrescimo_em_viagem_percentual": 50 }
                """);

        assertTrue(tabela.centrosCusto().isEmpty());
        assertEquals(1, tabela.padrao().size());
    }

    @DisplayName("AMB-028: arquivo de política malformado é rejeitado")
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "sem padrao                     | { \"centros_custo\": {}, \"nota_fiscal_obrigatoria_acima_de\": 100, \"acrescimo_em_viagem_percentual\": 50 }",
        "limite negativo                | { \"padrao\": { \"alimentacao\": { \"limite\": -1, \"periodicidade\": \"dia\" } }, \"nota_fiscal_obrigatoria_acima_de\": 100, \"acrescimo_em_viagem_percentual\": 50 }",
        "limite como texto              | { \"padrao\": { \"alimentacao\": { \"limite\": \"60\", \"periodicidade\": \"dia\" } }, \"nota_fiscal_obrigatoria_acima_de\": 100, \"acrescimo_em_viagem_percentual\": 50 }",
        "periodicidade desconhecida     | { \"padrao\": { \"alimentacao\": { \"limite\": 60, \"periodicidade\": \"semana\" } }, \"nota_fiscal_obrigatoria_acima_de\": 100, \"acrescimo_em_viagem_percentual\": 50 }",
        "sem piso da nota fiscal        | { \"padrao\": { \"alimentacao\": { \"limite\": 60, \"periodicidade\": \"dia\" } }, \"acrescimo_em_viagem_percentual\": 50 }",
        "sem percentual de acréscimo    | { \"padrao\": { \"alimentacao\": { \"limite\": 60, \"periodicidade\": \"dia\" } }, \"nota_fiscal_obrigatoria_acima_de\": 100 }",
        "centro de custo não é objeto   | { \"padrao\": {}, \"centros_custo\": { \"CC-X\": 10 }, \"nota_fiscal_obrigatoria_acima_de\": 100, \"acrescimo_em_viagem_percentual\": 50 }",
        "não é objeto JSON              | [1, 2, 3]",
    })
    void politica_malformada_e_rejeitada(String caso, String json) {
        assertThrows(EntradaInvalidaException.class, () -> leitor.lerTexto(json));
    }

    @Test
    @DisplayName("AMB-028: arquivo de política inexistente é rejeitado")
    void arquivo_inexistente() {
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> leitor.lerArquivo(Path.of("nao-existe.json")));
        assertTrue(erro.getMessage().contains("de política"), erro.getMessage());
    }

    @Test
    @DisplayName("Seção 4: mensagem de erro aponta o campo da política")
    void mensagem_aponta_o_campo() {
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class, () -> leitor.lerTexto("""
                { "padrao": { "hospedagem": { "limite": 250, "periodicidade": "mes" } },
                  "nota_fiscal_obrigatoria_acima_de": 100, "acrescimo_em_viagem_percentual": 50 }
                """));
        assertEquals("padrao.hospedagem.periodicidade: esperado \"dia\" ou \"diaria\": mes", erro.getMessage());
    }
}
