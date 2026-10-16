package reembolso.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reembolso.dominio.Despesa;
import reembolso.dominio.Entrada;

/** T-005: leitura e validação da entrada (spec, seção 4; plano, DT-001). */
class LeitorEntradaTest {

    private static final Path EXEMPLO = Path.of("exemplos", "despesas-exemplo.json");

    private final LeitorEntrada leitor = new LeitorEntrada();

    @Test
    @DisplayName("Seção 4: lê o arquivo de exemplo com colaborador, período e 14 despesas")
    void le_arquivo_de_exemplo() throws Exception {
        Entrada entrada = leitor.lerArquivo(EXEMPLO);

        assertEquals("c-0417", entrada.colaborador().id());
        assertEquals("CC-ENG-PLATAFORMA", entrada.colaborador().centroCusto());
        assertEquals("2026-07", entrada.periodo().competencia());
        assertEquals(LocalDate.of(2026, 7, 1), entrada.periodo().inicio());
        assertEquals(LocalDate.of(2026, 7, 31), entrada.periodo().fim());
        assertEquals(14, entrada.despesas().size());
    }

    @Test
    @DisplayName("DT-001: d-011 chega com o valor exato 33,333, sem passar por double")
    void d011_chega_com_valor_exato() throws Exception {
        Despesa d011 = leitor.lerArquivo(EXEMPLO).despesas().get(10);

        assertEquals("d-011", d011.id());
        assertEquals(new BigDecimal("33.333"), d011.valor());
    }

    @Test
    @DisplayName("DT-001: zeros à direita são preservados (72.50 não vira 72.5)")
    void zeros_a_direita_preservados() throws Exception {
        Despesa d001 = leitor.lerArquivo(EXEMPLO).despesas().get(0);

        assertEquals(new BigDecimal("72.50"), d001.valor());
    }

    @Test
    @DisplayName("Seção 4: despesa sai crua — categoria e valor como vieram (normalização é do motor)")
    void despesa_sai_crua() throws Exception {
        Despesa d014 = leitor.lerArquivo(EXEMPLO).despesas().get(13);

        assertEquals("ALIMENTACAO", d014.categoriaOriginal());
        assertEquals("ALIMENTACAO", d014.categoria());
        assertTrue(d014.temNotaFiscal());
    }

    @Test
    @DisplayName("Seção 4: valor negativo (estorno d-009) e despesa sem NF são lidos")
    void le_estorno_e_sem_nf() throws Exception {
        Despesa d009 = leitor.lerArquivo(EXEMPLO).despesas().get(8);

        assertEquals(new BigDecimal("-45.00"), d009.valor());
        assertFalse(d009.temNotaFiscal());
    }

    @DisplayName("Seção 4: entrada malformada é rejeitada")
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "campo obrigatório ausente (valor)       | {\"id\":\"d-1\",\"data\":\"2026-07-03\",\"categoria\":\"alimentacao\",\"descricao\":\"x\",\"fornecedor\":\"y\",\"tem_nota_fiscal\":true}",
        "data inválida                           | {\"id\":\"d-1\",\"data\":\"2026-02-30\",\"categoria\":\"alimentacao\",\"descricao\":\"x\",\"fornecedor\":\"y\",\"valor\":10.00,\"tem_nota_fiscal\":true}",
        "data em outro formato                   | {\"id\":\"d-1\",\"data\":\"03/07/2026\",\"categoria\":\"alimentacao\",\"descricao\":\"x\",\"fornecedor\":\"y\",\"valor\":10.00,\"tem_nota_fiscal\":true}",
        "valor não numérico                      | {\"id\":\"d-1\",\"data\":\"2026-07-03\",\"categoria\":\"alimentacao\",\"descricao\":\"x\",\"fornecedor\":\"y\",\"valor\":\"10.00\",\"tem_nota_fiscal\":true}",
        "nota fiscal não booleana                | {\"id\":\"d-1\",\"data\":\"2026-07-03\",\"categoria\":\"alimentacao\",\"descricao\":\"x\",\"fornecedor\":\"y\",\"valor\":10.00,\"tem_nota_fiscal\":\"sim\"}",
        "categoria nula                          | {\"id\":\"d-1\",\"data\":\"2026-07-03\",\"categoria\":null,\"descricao\":\"x\",\"fornecedor\":\"y\",\"valor\":10.00,\"tem_nota_fiscal\":true}",
    })
    void despesa_malformada_e_rejeitada(String caso, String despesa) {
        String json = "{\"colaborador\":{\"id\":\"c-1\",\"nome\":\"F\",\"centro_custo\":\"CC\"},"
                + "\"periodo\":{\"competencia\":\"2026-07\",\"inicio\":\"2026-07-01\",\"fim\":\"2026-07-31\"},"
                + "\"despesas\":[" + despesa + "]}";

        assertThrows(EntradaInvalidaException.class, () -> leitor.lerTexto(json));
    }

    @Test
    @DisplayName("Seção 4: bloco periodo ausente é rejeitado")
    void periodo_ausente_e_rejeitado() {
        String json = "{\"colaborador\":{\"id\":\"c-1\",\"nome\":\"F\",\"centro_custo\":\"CC\"},\"despesas\":[]}";

        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class, () -> leitor.lerTexto(json));
        assertTrue(erro.getMessage().contains("periodo"));
    }

    @Test
    @DisplayName("Seção 4: JSON sintaticamente inválido é rejeitado")
    void json_sintaticamente_invalido() {
        assertThrows(EntradaInvalidaException.class, () -> leitor.lerTexto("{ isto não é json"));
    }

    @Test
    @DisplayName("Seção 4: arquivo inexistente é rejeitado")
    void arquivo_inexistente() {
        assertThrows(EntradaInvalidaException.class, () -> leitor.lerArquivo(Path.of("nao-existe.json")));
    }

    @Test
    @DisplayName("Seção 4: mensagem de erro aponta o campo e a posição da despesa")
    void mensagem_aponta_o_campo() {
        String json = "{\"colaborador\":{\"id\":\"c-1\",\"nome\":\"F\",\"centro_custo\":\"CC\"},"
                + "\"periodo\":{\"competencia\":\"2026-07\",\"inicio\":\"2026-07-01\",\"fim\":\"2026-07-31\"},"
                + "\"despesas\":[{\"id\":\"d-1\",\"data\":\"2026-07-03\",\"categoria\":\"alimentacao\","
                + "\"descricao\":\"x\",\"fornecedor\":\"y\",\"valor\":\"abc\",\"tem_nota_fiscal\":true}]}";

        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class, () -> leitor.lerTexto(json));
        assertEquals("despesas[0].valor: deve ser número", erro.getMessage());
    }

    private static final String CABECALHO = "{\"colaborador\":{\"id\":\"c-1\",\"nome\":\"F\",\"centro_custo\":\"CC\"},"
            + "\"periodo\":{\"competencia\":\"2026-07\",\"inicio\":\"2026-07-01\",\"fim\":\"2026-07-31\"},";

    private static String comDespesa(String camposExtras) {
        return CABECALHO + "\"despesas\":[{\"id\":\"e-1\",\"data\":\"2026-07-14\",\"categoria\":\"alimentacao\","
                + "\"descricao\":\"x\",\"fornecedor\":\"y\",\"valor\":22.00,\"tem_nota_fiscal\":true" + camposExtras + "}]}";
    }

    @Test
    @DisplayName("Seção 4 / RN-014: moeda presente é lida como veio (EUR), com o valor original exato")
    void moeda_presente() throws Exception {
        Despesa despesa = leitor.lerTexto(comDespesa(",\"moeda\":\"EUR\"")).despesas().get(0);

        assertEquals("EUR", despesa.moeda());
        assertEquals(new BigDecimal("22.00"), despesa.valorOriginal());
        assertTrue(despesa.emMoedaEstrangeira());
    }

    @Test
    @DisplayName("Seção 4 / RN-014: moeda ausente = BRL")
    void moeda_ausente_e_brl() throws Exception {
        Despesa despesa = leitor.lerTexto(comDespesa("")).despesas().get(0);

        assertEquals("BRL", despesa.moeda());
        assertFalse(despesa.emMoedaEstrangeira());
    }

    @Test
    @DisplayName("Seção 4: moeda que não é texto é rejeitada")
    void moeda_nao_textual() {
        assertThrows(EntradaInvalidaException.class, () -> leitor.lerTexto(comDespesa(",\"moeda\":978")));
    }

    @Test
    @DisplayName("Seção 4: o arquivo do envelope é lido, com as moedas de cada despesa (e-010 sem moeda = BRL)")
    void le_despesas_do_envelope() throws Exception {
        var despesas = leitor.lerArquivo(Path.of("exemplos", "envelope", "despesas-envelope.json")).despesas();

        assertEquals(10, despesas.size());
        assertEquals("EUR", despesas.get(1).moeda());
        assertEquals("GBP", despesas.get(5).moeda());
        assertEquals("BRL", despesas.get(9).moeda());
    }
}
