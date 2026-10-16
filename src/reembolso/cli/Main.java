package reembolso.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Map;
import reembolso.dominio.Resultado;
import reembolso.dominio.TabelaCambio;
import reembolso.dominio.TabelaPoliticas;
import reembolso.io.EntradaInvalidaException;
import reembolso.io.EscritorSaida;
import reembolso.io.LeitorEntrada;
import reembolso.regras.MotorReembolso;
import reembolso.regras.Politica;

/**
 * CLI: {@code calcular --input <entrada.json> --output <saida.json>}.
 *
 * <p>Códigos de saída (plano, DT-005): 0 sucesso; 1 entrada inválida; 2 uso incorreto;
 * 3 falha ao gravar a saída.
 */
public final class Main {

    static final int SUCESSO = 0;
    static final int ENTRADA_INVALIDA = 1;
    static final int USO_INCORRETO = 2;
    static final int FALHA_AO_GRAVAR = 3;

    private static final String USO =
            "uso: java -jar motor-reembolso.jar calcular --input <entrada.json> --output <saida.json>";

    private Main() {
    }

    /**
     * Imprime no console sem acentos. O console do Windows usa códigos de página variados e
     * corrompe acentos; o arquivo de saída continua em UTF-8 com acentos.
     */
    private static void console(PrintStream destino, String mensagem) {
        String semAcentos = Normalizer.normalize(mensagem, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        destino.println(semAcentos);
    }

    public static void main(String[] args) {
        System.exit(executar(args, System.out, System.err));
    }

    static int executar(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || !args[0].equals("calcular")) {
            console(err, args.length == 0 ? "nenhum comando informado" : "comando desconhecido: " + args[0]);
            console(err, USO);
            return USO_INCORRETO;
        }

        Path entrada = null;
        Path saida = null;
        for (int i = 1; i < args.length; i++) {
            String opcao = args[i];
            if (!opcao.equals("--input") && !opcao.equals("--output")) {
                console(err, "opção desconhecida: " + opcao);
                console(err, USO);
                return USO_INCORRETO;
            }
            if (i + 1 >= args.length) {
                console(err, "falta o valor de " + opcao);
                console(err, USO);
                return USO_INCORRETO;
            }
            Path valor = Path.of(args[++i]);
            if (opcao.equals("--input")) {
                entrada = valor;
            } else {
                saida = valor;
            }
        }
        if (entrada == null || saida == null) {
            console(err, entrada == null ? "falta --input" : "falta --output");
            console(err, USO);
            return USO_INCORRETO;
        }

        Resultado resultado;
        try {
            // Até a T-031: só a tabela padrão (valores da v3) e nenhum câmbio.
            TabelaPoliticas v3 = new TabelaPoliticas(Map.of(), Politica.V3.categorias(),
                    Politica.V3.pisoNotaFiscal(), Politica.V3.percentualAcrescimo());
            resultado = new MotorReembolso(v3, new TabelaCambio(Map.of()))
                    .calcular(new LeitorEntrada().lerArquivo(entrada));
        } catch (EntradaInvalidaException e) {
            console(err, "entrada inválida: " + e.getMessage());
            return ENTRADA_INVALIDA;
        }

        try {
            new EscritorSaida().escrever(resultado, saida);
        } catch (IOException e) {
            console(err, "não foi possível gravar a saída em " + saida + ": " + e.getMessage());
            return FALHA_AO_GRAVAR;
        }

        console(out, "reembolso calculado: R$ " + resultado.totalReembolsado().toPlainString()
                + " - saida gravada em " + saida);
        return SUCESSO;
    }
}
