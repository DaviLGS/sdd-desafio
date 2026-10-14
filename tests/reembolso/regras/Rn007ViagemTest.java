package reembolso.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reembolso.Despesas.despesa;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-012: RN-007 / AMB-011 / AMB-012 / AMB-013 — dias de viagem a partir das hospedagens. */
class Rn007ViagemTest {

    private static LocalDate dia(String data) {
        return LocalDate.parse(data);
    }

    @Test
    @DisplayName("RN-007 / AMB-012: hospedagem de 14/07 com 2 noites (d-010) → 14 e 15 são de viagem, 16 não")
    void hospedagem_duas_noites() {
        var d010 = despesa("d-010", "2026-07-14", "hospedagem", "480.00", true, "Hotel Rio - 2 diarias");

        Set<LocalDate> dias = DiasViagem.calcular(List.of(d010));

        assertEquals(Set.of(dia("2026-07-14"), dia("2026-07-15")), dias);
        assertFalse(dias.contains(dia("2026-07-16")), "o dia da saída não é de viagem");
    }

    @Test
    @DisplayName("RN-007: hospedagem sem NF (d-013, 3 noites) ainda gera dias de viagem")
    void hospedagem_em_revisao_gera_viagem() {
        var d013 = despesa("d-013", "2026-07-22", "hospedagem", "690.00", false, "Airbnb 3 noites");

        assertEquals(Set.of(dia("2026-07-22"), dia("2026-07-23"), dia("2026-07-24")),
                DiasViagem.calcular(List.of(d013)));
    }

    @Test
    @DisplayName("RN-007 / RN-005: hospedagem sem noites identificáveis não gera dias de viagem")
    void hospedagem_sem_noites_nao_gera_viagem() {
        var hotel = despesa("d-1", "2026-07-14", "hospedagem", "300.00", true, "Hotel centro");

        assertTrue(DiasViagem.calcular(List.of(hotel)).isEmpty());
    }

    @Test
    @DisplayName("RN-007 / AMB-011: sem hospedagem não há viagem, mesmo com outras despesas")
    void sem_hospedagem_sem_viagem() {
        var almoco = despesa("d-1", "2026-07-14", "alimentacao", "50.00", true, "Almoco 2 noites depois");
        var taxi = despesa("d-2", "2026-07-14", "transporte_urbano", "30.00", true, "Corrida");

        assertTrue(DiasViagem.calcular(List.of(almoco, taxi)).isEmpty());
    }

    @Test
    @DisplayName("RN-007: hospedagem de 1 noite cobre só o próprio dia")
    void hospedagem_uma_noite() {
        var hotel = despesa("d-1", "2026-07-14", "hospedagem", "200.00", true, "Pousada 1 noite");

        assertEquals(Set.of(dia("2026-07-14")), DiasViagem.calcular(List.of(hotel)));
    }

    @Test
    @DisplayName("RN-007: noites que atravessam a virada do mês são contadas normalmente")
    void noites_atravessam_o_mes() {
        var hotel = despesa("d-1", "2026-07-30", "hospedagem", "750.00", true, "Hotel 3 noites");

        assertEquals(Set.of(dia("2026-07-30"), dia("2026-07-31"), dia("2026-08-01")),
                DiasViagem.calcular(List.of(hotel)));
    }

    @Test
    @DisplayName("RN-007: hospedagens sobrepostas somam os dias cobertos, sem duplicar")
    void hospedagens_sobrepostas() {
        var hotelA = despesa("d-1", "2026-07-14", "hospedagem", "480.00", true, "Hotel A 2 noites");
        var hotelB = despesa("d-2", "2026-07-15", "hospedagem", "480.00", true, "Hotel B 2 noites");

        assertEquals(Set.of(dia("2026-07-14"), dia("2026-07-15"), dia("2026-07-16")),
                DiasViagem.calcular(List.of(hotelA, hotelB)));
    }
}
