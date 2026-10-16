package reembolso;

import java.math.BigDecimal;
import java.util.Map;
import reembolso.dominio.LimiteCategoria;
import reembolso.dominio.Periodicidade;
import reembolso.dominio.TabelaCambio;
import reembolso.dominio.TabelaPoliticas;
import reembolso.regras.Politica;

/**
 * Políticas montadas para os testes de regra, com os valores de {@code exemplos/envelope/politica-v4.json}.
 * Os testes de regra não leem arquivo (plano, seção 6).
 */
public final class Politicas {

    private Politicas() {
    }

    private static LimiteCategoria dia(String limite) {
        return new LimiteCategoria(new BigDecimal(limite), Periodicidade.DIA);
    }

    private static LimiteCategoria diaria(String limite) {
        return new LimiteCategoria(new BigDecimal(limite), Periodicidade.DIARIA);
    }

    private static final BigDecimal PISO_NF = new BigDecimal("100.00");
    private static final BigDecimal ACRESCIMO = new BigDecimal("50");

    /** Tabela {@code padrao}: alimentação 60/dia, transporte 80/dia, hospedagem 250/noite. */
    public static final Politica PADRAO = new Politica("padrao",
            Map.of("alimentacao", dia("60.00"), "transporte_urbano", dia("80.00"), "hospedagem", diaria("250.00")),
            PISO_NF, ACRESCIMO);

    /** Tabela do {@code CC-COMERCIAL}: alimentação 90, transporte 150, hospedagem 400/noite, representação 300. */
    public static final Politica COMERCIAL = new Politica("CC-COMERCIAL",
            Map.of("alimentacao", dia("90.00"), "transporte_urbano", dia("150.00"), "hospedagem", diaria("400.00"),
                    "representacao", dia("300.00")),
            PISO_NF, ACRESCIMO);

    /** Arquivo de política só com a tabela padrão: qualquer centro de custo cai nela. */
    public static final TabelaPoliticas SO_PADRAO = new TabelaPoliticas(Map.of(), PADRAO.categorias(), PISO_NF, ACRESCIMO);

    /** Arquivo de câmbio vazio: só despesas em BRL. */
    public static final TabelaCambio SEM_CAMBIO = new TabelaCambio(Map.of());

    /** Tabela do {@code CC-ENG-PLATAFORMA}: alimentação 75, transporte 80, hospedagem 0 (não reembolsável). */
    public static final Politica ENG_PLATAFORMA = new Politica("CC-ENG-PLATAFORMA",
            Map.of("alimentacao", dia("75.00"), "transporte_urbano", dia("80.00"), "hospedagem", diaria("0.00")),
            PISO_NF, ACRESCIMO);
}
