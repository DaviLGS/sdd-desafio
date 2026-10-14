package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static reembolso.Despesas.despesa;

import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import reembolso.dominio.Motivo;
import reembolso.dominio.Status;

/** T-010: RN-005 / AMB-004 — número de noites lido da descrição da hospedagem. */
class Rn005NoitesHospedagemTest {

    @DisplayName("RN-005 / AMB-004: descrições com número de noites identificável")
    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(delimiter = '|', value = {
        "Hotel Rio - 2 diarias  | 2",   // d-010
        "Airbnb 3 noites        | 3",   // d-013
        "1 diária               | 1",
        "Pousada 1 noite        | 1",
        "HOTEL 4 DIÁRIAS        | 4",
        "Hotel 2diarias         | 2",   // sem espaço entre número e palavra
        "Hostel 10 noites       | 10",
    })
    void noites_identificaveis(String descricao, int esperado) {
        assertEquals(OptionalInt.of(esperado), NoitesHospedagem.identificar(descricao));
    }

    @DisplayName("RN-005: descrições sem número de noites identificável")
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
        "Hotel centro",              // sem número
        "0 noites",                  // zero
        "duas noites",               // por extenso
        "Hotel quarto 302",          // número sem a palavra noite/diária
        "2 noites + 1 diaria",       // mais de uma indicação: ambígua
    })
    void noites_nao_identificaveis(String descricao) {
        assertEquals(OptionalInt.empty(), NoitesHospedagem.identificar(descricao));
    }

    @Test
    @DisplayName("RN-005: hospedagem sem noites identificáveis → NOITES_NAO_IDENTIFICADAS")
    void hospedagem_sem_noites_gera_motivo() {
        var despesa = despesa("d-1", "2026-07-14", "hospedagem", "300.00", true, "Hotel centro");

        assertEquals(Optional.of(Motivo.NOITES_NAO_IDENTIFICADAS), NoitesHospedagem.verificar(despesa));
    }

    @Test
    @DisplayName("RN-005: hospedagem com noites identificáveis (d-010) → sem motivo")
    void hospedagem_com_noites_sem_motivo() {
        var d010 = despesa("d-010", "2026-07-14", "hospedagem", "480.00", true, "Hotel Rio - 2 diarias");

        assertEquals(Optional.empty(), NoitesHospedagem.verificar(d010));
    }

    @Test
    @DisplayName("RN-005: outras categorias não são verificadas, mesmo sem número de noites")
    void outras_categorias_nao_sao_verificadas() {
        var almoco = despesa("d-1", "2026-07-03", "alimentacao", "50.00", true, "Almoco");

        assertEquals(Optional.empty(), NoitesHospedagem.verificar(almoco));
    }

    @Test
    @DisplayName("RN-005: noites não identificadas leva a em_revisao")
    void noites_nao_identificadas_leva_a_revisao() {
        assertEquals(Status.EM_REVISAO, Motivo.NOITES_NAO_IDENTIFICADAS.statusResultante());
    }
}
