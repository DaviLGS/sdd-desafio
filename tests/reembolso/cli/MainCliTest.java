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

/** T-020: CLI {@code calcular --input X --output Y} e códigos de saída (plano, DT-005). */
class MainCliTest {

    private static final String EXEMPLO = Path.of("exemplos", "despesas-exemplo.json").toString();

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

    private String erro() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("DT-005: exemplo → código 0 e arquivo com total_reembolsado 680.53")
    void exemplo_gera_saida() throws Exception {
        Path saida = pasta.resolve("resultado.json");

        int codigo = executar("calcular", "--input", EXEMPLO, "--output", saida.toString());

        assertEquals(0, codigo, erro());
        assertTrue(Files.exists(saida));
        var raiz = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(Files.readString(saida));
        assertEquals("680.53", raiz.get("resumo").get("total_reembolsado").decimalValue().toPlainString());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("680.53"));
    }

    @Test
    @DisplayName("DT-005: as opções podem vir em qualquer ordem")
    void opcoes_em_qualquer_ordem() {
        Path saida = pasta.resolve("resultado.json");

        assertEquals(0, executar("calcular", "--output", saida.toString(), "--input", EXEMPLO), erro());
        assertTrue(Files.exists(saida));
    }

    @Test
    @DisplayName("Seção 4 / DT-005: entrada malformada → código 1 e nenhum arquivo de saída criado")
    void entrada_invalida_nao_gera_saida() throws Exception {
        Path entrada = pasta.resolve("invalida.json");
        Files.writeString(entrada, "{ \"colaborador\": {} }");
        Path saida = pasta.resolve("resultado.json");

        int codigo = executar("calcular", "--input", entrada.toString(), "--output", saida.toString());

        assertEquals(1, codigo);
        assertFalse(Files.exists(saida));
        assertTrue(erro().contains("entrada invalida: campo obrigatorio ausente"), erro());
    }

    @Test
    @DisplayName("DT-005: mensagens de console saem sem acentos (console do Windows)")
    void console_sem_acentos() throws Exception {
        Path entrada = pasta.resolve("invalida.json");
        Files.writeString(entrada, "{ \"colaborador\": {} }");

        executar("calcular", "--input", entrada.toString(), "--output", pasta.resolve("r.json").toString());

        assertTrue(erro().chars().allMatch(c -> c < 128), erro());
    }

    @Test
    @DisplayName("Seção 4 / DT-005: arquivo de entrada inexistente → código 1 e nenhum arquivo de saída")
    void entrada_inexistente() {
        Path saida = pasta.resolve("resultado.json");

        int codigo = executar("calcular", "--input", pasta.resolve("nao-existe.json").toString(),
                "--output", saida.toString());

        assertEquals(1, codigo);
        assertFalse(Files.exists(saida));
    }

    @Test
    @DisplayName("DT-005: sem comando → código 2 com mensagem de uso")
    void sem_comando() {
        assertEquals(2, executar());
        assertTrue(erro().contains("uso:"), erro());
    }

    @Test
    @DisplayName("DT-005: comando desconhecido → código 2")
    void comando_desconhecido() {
        assertEquals(2, executar("processar", "--input", EXEMPLO, "--output", "x.json"));
    }

    @Test
    @DisplayName("DT-005: falta --output → código 2")
    void falta_output() {
        assertEquals(2, executar("calcular", "--input", EXEMPLO));
        assertTrue(erro().contains("falta --output"), erro());
    }

    @Test
    @DisplayName("DT-005: falta --input → código 2")
    void falta_input() {
        assertEquals(2, executar("calcular", "--output", pasta.resolve("r.json").toString()));
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

        assertEquals(3, executar("calcular", "--input", EXEMPLO, "--output", saida.toString()));
        assertFalse(Files.exists(saida));
    }
}
