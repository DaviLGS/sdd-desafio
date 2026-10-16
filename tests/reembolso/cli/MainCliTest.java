package reembolso.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * T-020 / T-031: CLI {@code calcular --input X --politica P --cambio C --output Y} e códigos de saída
 * (plano, DT-005 e DT-009).
 */
class MainCliTest {

    private static final String EXEMPLO = Path.of("exemplos", "despesas-exemplo.json").toString();
    private static final String POLITICA = Path.of("exemplos", "envelope", "politica-v4.json").toString();
    private static final String CAMBIO = Path.of("exemplos", "envelope", "cambio.json").toString();

    @TempDir
    Path pasta;

    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void preparar() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
    }

    private int executar(String... args) {
        return Main.executar(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    /** Os quatro argumentos válidos, com a entrada e a saída indicadas. */
    private int calcular(String entrada, Path saida) {
        return executar("calcular", "--input", entrada, "--politica", POLITICA, "--cambio", CAMBIO,
                "--output", saida.toString());
    }

    private String erro() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private static String total(Path saida) throws Exception {
        var raiz = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(Files.readString(saida));
        return raiz.get("resumo").get("total_reembolsado").decimalValue().toPlainString();
    }

    @DisplayName("Seção 9 / DT-009: os três arquivos da seção 9 → código 0 e o total esperado")
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "exemplos/despesas-exemplo.json,                          216.53",
        "exemplos/envelope/despesas-envelope.json,                1993.26",
        "exemplos/envelope/despesas-envelope-cc-desconhecido.json, 373.76",
    })
    void tres_exemplos(String entrada, String totalEsperado) throws Exception {
        Path saida = pasta.resolve("resultado.json");

        assertEquals(0, calcular(entrada, saida), erro());
        assertEquals(totalEsperado, total(saida));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains(totalEsperado));
    }

    @Test
    @DisplayName("DT-009: as opções podem vir em qualquer ordem")
    void opcoes_em_qualquer_ordem() throws Exception {
        Path saida = pasta.resolve("resultado.json");

        int codigo = executar("calcular", "--cambio", CAMBIO, "--output", saida.toString(),
                "--input", EXEMPLO, "--politica", POLITICA);

        assertEquals(0, codigo, erro());
        assertEquals("216.53", total(saida));
    }

    @Test
    @DisplayName("Seção 4 / DT-005: entrada malformada → código 1 e nenhum arquivo de saída criado")
    void entrada_invalida_nao_gera_saida() throws Exception {
        Path entrada = pasta.resolve("invalida.json");
        Files.writeString(entrada, "{ \"colaborador\": {} }");
        Path saida = pasta.resolve("resultado.json");

        assertEquals(1, calcular(entrada.toString(), saida));
        assertFalse(Files.exists(saida));
        assertTrue(erro().contains("entrada invalida: campo obrigatorio ausente"), erro());
    }

    @Test
    @DisplayName("AMB-028 / DT-009: política malformada → código 1, nenhum arquivo de saída")
    void politica_invalida() throws Exception {
        Path politica = pasta.resolve("politica.json");
        Files.writeString(politica, "{ \"nota_fiscal_obrigatoria_acima_de\": 100 }");
        Path saida = pasta.resolve("resultado.json");

        int codigo = executar("calcular", "--input", EXEMPLO, "--politica", politica.toString(),
                "--cambio", CAMBIO, "--output", saida.toString());

        assertEquals(1, codigo);
        assertFalse(Files.exists(saida));
        assertTrue(erro().contains("politica invalida: campo obrigatorio ausente: padrao"), erro());
    }

    @Test
    @DisplayName("AMB-028 / DT-009: arquivo de câmbio inexistente → código 1, nenhum arquivo de saída")
    void cambio_inexistente() {
        Path saida = pasta.resolve("resultado.json");

        int codigo = executar("calcular", "--input", EXEMPLO, "--politica", POLITICA,
                "--cambio", pasta.resolve("nao-existe.json").toString(), "--output", saida.toString());

        assertEquals(1, codigo);
        assertFalse(Files.exists(saida));
        assertTrue(erro().contains("cambio invalido"), erro());
    }

    @Test
    @DisplayName("DT-005: mensagens de console saem sem acentos (console do Windows)")
    void console_sem_acentos() throws Exception {
        Path entrada = pasta.resolve("invalida.json");
        Files.writeString(entrada, "{ \"colaborador\": {} }");

        calcular(entrada.toString(), pasta.resolve("r.json"));

        assertTrue(erro().chars().allMatch(c -> c < 128), erro());
    }

    @Test
    @DisplayName("Seção 4 / DT-005: arquivo de entrada inexistente → código 1 e nenhum arquivo de saída")
    void entrada_inexistente() {
        Path saida = pasta.resolve("resultado.json");

        assertEquals(1, calcular(pasta.resolve("nao-existe.json").toString(), saida));
        assertFalse(Files.exists(saida));
    }

    @Test
    @DisplayName("DT-005: sem comando → código 2 com mensagem de uso citando as quatro opções")
    void sem_comando() {
        assertEquals(2, executar());
        assertTrue(erro().contains("uso:") && erro().contains("--politica") && erro().contains("--cambio"), erro());
    }

    @Test
    @DisplayName("DT-005: comando desconhecido → código 2")
    void comando_desconhecido() {
        assertEquals(2, executar("processar", "--input", EXEMPLO, "--output", "x.json"));
    }

    @DisplayName("DT-009: falta uma das opções obrigatórias → código 2")
    @ParameterizedTest(name = "sem {0}")
    @CsvSource({"--input", "--politica", "--cambio", "--output"})
    void falta_opcao(String ausente) {
        String[] todas = {"--input", EXEMPLO, "--politica", POLITICA, "--cambio", CAMBIO,
                "--output", pasta.resolve("r.json").toString()};
        java.util.List<String> args = new java.util.ArrayList<>(java.util.List.of("calcular"));
        for (int i = 0; i < todas.length; i += 2) {
            if (!todas[i].equals(ausente)) {
                args.add(todas[i]);
                args.add(todas[i + 1]);
            }
        }

        assertEquals(2, executar(args.toArray(String[]::new)));
        assertTrue(erro().contains("falta " + ausente), erro());
    }

    @Test
    @DisplayName("DT-005: opção sem valor → código 2")
    void opcao_sem_valor() {
        assertEquals(2, executar("calcular", "--output", pasta.resolve("r.json").toString(), "--input"));
    }

    @Test
    @DisplayName("DT-005: opção desconhecida → código 2")
    void opcao_desconhecida() {
        assertEquals(2, executar("calcular", "--input", EXEMPLO, "--saida", "x.json"));
    }

    @Test
    @DisplayName("DT-005: pasta de saída inexistente → código 3")
    void falha_ao_gravar() {
        Path saida = pasta.resolve("nao-existe").resolve("resultado.json");

        assertEquals(3, calcular(EXEMPLO, saida));
        assertFalse(Files.exists(saida));
    }
}
