package reembolso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-001: garante que o projeto compila, os testes rodam e as dependências estão no classpath. */
class ProjetoSmokeTest {

    @Test
    @DisplayName("T-001: runtime Java é 17 ou superior")
    void runtime_java_17_ou_superior() {
        assertTrue(Runtime.version().feature() >= 17);
    }

    @Test
    @DisplayName("T-001 / DT-001: Jackson lê número decimal como BigDecimal sem perder dígitos")
    void jackson_le_decimal_exato() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        JsonNode valor = mapper.readTree("{\"valor\": 33.333}").get("valor");

        assertEquals(new BigDecimal("33.333"), valor.decimalValue());
    }
}
