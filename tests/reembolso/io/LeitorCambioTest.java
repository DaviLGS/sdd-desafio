package reembolso.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reembolso.dominio.TabelaCambio;

/** T-026: leitura e validação do arquivo de câmbio (spec, seção 4; AMB-028). */
class LeitorCambioTest {

    private static final Path CAMBIO = Path.of("exemplos", "envelope", "cambio.json");

    private final LeitorCambio leitor = new LeitorCambio();

    @Test
    @DisplayName("Seção 4: lê cambio.json — 12 dias úteis de 13/07 a 28/07, USD e EUR")
    void le_cambio() throws Exception {
        TabelaCambio cambio = leitor.lerArquivo(CAMBIO);

        assertEquals(12, cambio.taxas().size());
        assertEquals(new BigDecimal("5.93"), cambio.taxas().get(LocalDate.of(2026, 7, 14)).get("EUR"));
        assertEquals(new BigDecimal("5.50"), cambio.taxas().get(LocalDate.of(2026, 7, 20)).get("USD"));
    }

    @Test
    @DisplayName("Seção 4: dias sem cotação no arquivo (fim de semana) simplesmente não aparecem")
    void fim_de_semana_ausente() throws Exception {
        TabelaCambio cambio = leitor.lerArquivo(CAMBIO);

        assertTrue(!cambio.taxas().containsKey(LocalDate.of(2026, 7, 18)));
        assertTrue(!cambio.taxas().containsKey(LocalDate.of(2026, 7, 19)));
    }

    @DisplayName("AMB-028: arquivo de câmbio malformado é rejeitado")
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "sem taxas              | { \"moeda_base\": \"BRL\" }",
        "data inválida          | { \"taxas\": { \"2026-02-30\": { \"USD\": 5.4 } } }",
        "taxa como texto        | { \"taxas\": { \"2026-07-13\": { \"USD\": \"5,42\" } } }",
        "taxa zero              | { \"taxas\": { \"2026-07-13\": { \"USD\": 0 } } }",
        "dia não é objeto       | { \"taxas\": { \"2026-07-13\": 5.42 } }",
    })
    void cambio_malformado_e_rejeitado(String caso, String json) {
        assertThrows(EntradaInvalidaException.class, () -> leitor.lerTexto(json));
    }

    @Test
    @DisplayName("AMB-028: arquivo de câmbio inexistente é rejeitado")
    void arquivo_inexistente() {
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> leitor.lerArquivo(Path.of("nao-existe.json")));
        assertTrue(erro.getMessage().contains("de câmbio"), erro.getMessage());
    }
}
