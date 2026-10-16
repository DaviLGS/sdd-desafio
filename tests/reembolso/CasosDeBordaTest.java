package reembolso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reembolso.dominio.Ampliacao;
import reembolso.dominio.Colaborador;
import reembolso.dominio.Despesa;
import reembolso.dominio.Entrada;
import reembolso.dominio.Grupo;
import reembolso.dominio.Motivo;
import reembolso.dominio.Periodo;
import reembolso.dominio.Resultado;
import reembolso.dominio.ResultadoDespesa;
import reembolso.dominio.Status;
import reembolso.dominio.TabelaCambio;
import reembolso.dominio.TabelaPoliticas;
import reembolso.io.LeitorCambio;
import reembolso.io.LeitorPolitica;
import reembolso.regras.MotorReembolso;

/**
 * T-021 / T-032: uma verificação por linha da tabela de casos de borda (spec, seção 7), pelo motor
 * completo, com {@code exemplos/envelope/politica-v4.json} e {@code cambio.json}.
 *
 * <p>O {@code @DisplayName} de cada teste é idêntico ao texto da coluna "Caso" da spec, e
 * {@link #toda_linha_da_secao_7_tem_teste()} garante que nenhuma linha fique sem teste. Salvo
 * indicação, o colaborador é de um centro de custo sem entrada (tabela padrão), como diz a seção 7.
 */
class CasosDeBordaTest {

    private static final Periodo JULHO = new Periodo("2026-07", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
    private static final String DIA_UTIL = "2026-07-06";   // segunda-feira
    private static final String SABADO = "2026-07-18";

    private static TabelaPoliticas politica;
    private static TabelaCambio cambio;

    @BeforeAll
    static void lerPoliticaECambio() throws Exception {
        politica = new LeitorPolitica().lerArquivo(Path.of("exemplos", "envelope", "politica-v4.json"));
        cambio = new LeitorCambio().lerArquivo(Path.of("exemplos", "envelope", "cambio.json"));
    }

    /** Despesa crua em BRL, como sai do leitor: o motor converte, normaliza e arredonda (passo 1 da seção 8). */
    private static Despesa d(String id, String data, String categoria, String valor, boolean nf, String descricao) {
        return new Despesa(id, LocalDate.parse(data), categoria, categoria, descricao, "Fornecedor",
                new BigDecimal(valor), nf);
    }

    /** Despesa crua em moeda estrangeira. */
    private static Despesa dm(String id, String data, String categoria, String valor, String moeda, boolean nf,
            String descricao) {
        return new Despesa(id, LocalDate.parse(data), categoria, categoria, descricao, "Fornecedor",
                new BigDecimal(valor), nf, moeda, new BigDecimal(valor), null, null);
    }

    /** Colaborador de um centro de custo sem entrada na tabela: política padrão. */
    private static Resultado calcular(Despesa... despesas) {
        return calcularNoCentro("CC-SEM-ENTRADA", despesas);
    }

    private static Resultado calcularNoCentro(String centroCusto, Despesa... despesas) {
        Entrada entrada = new Entrada(new Colaborador("c-1", "Fulana", centroCusto), JULHO, List.of(despesas));
        return new MotorReembolso(politica, cambio).calcular(entrada);
    }

    private static ResultadoDespesa despesa(Resultado r, String id) {
        return r.despesas().stream().filter(x -> x.despesa().id().equals(id)).findFirst().orElseThrow();
    }

    private static Grupo grupo(Resultado r, String data, String categoria) {
        return r.grupos().stream()
                .filter(g -> g.data().equals(LocalDate.parse(data)) && g.categoria().equals(categoria))
                .findFirst().orElseThrow(() -> new AssertionError("sem grupo " + data + "/" + categoria));
    }

    private static void assertStatus(Resultado r, String id, Status status, Motivo... motivos) {
        ResultadoDespesa x = despesa(r, id);
        assertEquals(status, x.status(), id);
        assertEquals(List.of(motivos), x.motivos(), id);
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("Duas refeições no mesmo dia acima do limite")
    void duas_refeicoes_acima_do_limite() {
        Resultado r = calcular(
                d("d-1", "2026-07-03", "alimentacao", "72.50", true, "Almoco"),
                d("d-2", "2026-07-03", "alimentacao", "38.00", true, "Jantar"));

        Grupo g = grupo(r, "2026-07-03", "alimentacao");
        assertEquals(v("110.50"), g.valorTotal());
        assertEquals(v("60.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Exatamente R$ 100,00 sem NF")
    void exatamente_cem_sem_nf() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "transporte_urbano", "100.00", false, "Corrida"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.NF_AUSENTE);
    }

    @Test
    @DisplayName("Um centavo acima de 100 sem NF")
    void um_centavo_acima_sem_nf() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "transporte_urbano", "100.01", false, "Corrida"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.NF_AUSENTE);
    }

    @Test
    @DisplayName("R$ 99,99 sem NF")
    void noventa_e_nove_sem_nf() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "alimentacao", "99.99", false, "Jantar"));

        assertStatus(r, "d-1", Status.APROVADA);
        assertEquals(v("60.00"), grupo(r, DIA_UTIL, "alimentacao").valorReembolsado());
    }

    @Test
    @DisplayName("Categoria fora da política")
    void categoria_fora_da_politica() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "coworking", "89.00", true, "Diaria coworking"));

        assertStatus(r, "d-1", Status.RECUSADA, Motivo.CATEGORIA_FORA_DA_POLITICA);
        assertTrue(r.grupos().isEmpty());
    }

    @Test
    @DisplayName("Duplicata exata")
    void duplicata_exata() {
        Resultado r = calcular(
                d("d-1", DIA_UTIL, "alimentacao", "54.90", true, "Almoco"),
                d("d-2", DIA_UTIL, "alimentacao", "54.90", true, "Almoco"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.DUPLICATA);
        assertStatus(r, "d-2", Status.EM_REVISAO, Motivo.DUPLICATA);
        assertTrue(r.grupos().isEmpty());
    }

    @Test
    @DisplayName("Fora do período")
    void fora_do_periodo() {
        Resultado r = calcular(d("d-1", "2026-04-15", "alimentacao", "41.00", true, "Almoco de abril"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.FORA_DO_PERIODO);
    }

    @Test
    @DisplayName("Último dia do período")
    void ultimo_dia_do_periodo() {
        Resultado r = calcular(d("d-1", "2026-07-31", "alimentacao", "40.00", true, "Jantar"));

        assertStatus(r, "d-1", Status.APROVADA);
    }

    @Test
    @DisplayName("Estorno sem despesa no dia")
    void estorno_sem_despesa() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "transporte_urbano", "-45.00", false, "Estorno corrida"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.ESTORNO_SEM_DESPESA);
    }

    @Test
    @DisplayName("Estorno com despesa no dia")
    void estorno_com_despesa() {
        Resultado r = calcular(
                d("d-1", DIA_UTIL, "transporte_urbano", "100.00", true, "Corrida"),
                d("d-2", DIA_UTIL, "transporte_urbano", "-45.00", false, "Estorno corrida"));

        Grupo g = grupo(r, DIA_UTIL, "transporte_urbano");
        assertEquals(v("55.00"), g.valorTotal());
        assertEquals(v("55.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Dois estornos que juntos excedem o dia")
    void dois_estornos_excedem() {
        Resultado r = calcular(
                d("d-1", DIA_UTIL, "transporte_urbano", "50.00", true, "Corrida"),
                d("d-2", DIA_UTIL, "transporte_urbano", "-45.00", false, "Estorno corrida A"),
                d("d-3", DIA_UTIL, "transporte_urbano", "-45.00", false, "Estorno corrida B"));

        assertStatus(r, "d-2", Status.EM_REVISAO, Motivo.ESTORNO_SEM_DESPESA);
        assertStatus(r, "d-3", Status.EM_REVISAO, Motivo.ESTORNO_SEM_DESPESA);
        assertEquals(v("50.00"), grupo(r, DIA_UTIL, "transporte_urbano").valorReembolsado());
    }

    @Test
    @DisplayName("Hospedagem com várias noites")
    void hospedagem_varias_noites() {
        Resultado r = calcular(d("d-1", "2026-07-14", "hospedagem", "480.00", true, "Hotel Rio - 2 diarias"));

        Grupo g = grupo(r, "2026-07-14", "hospedagem");
        assertEquals(v("500.00"), g.limite());
        assertEquals(v("480.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Hospedagem sem número de noites")
    void hospedagem_sem_noites() {
        Resultado r = calcular(d("d-1", "2026-07-14", "hospedagem", "300.00", true, "Hotel centro"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.NOITES_NAO_IDENTIFICADAS);
    }

    @Test
    @DisplayName("Noites sem espaço")
    void noites_sem_espaco() {
        Resultado r = calcular(d("d-1", "2026-07-14", "hospedagem", "400.00", true, "Hotel 2diarias"));

        Grupo g = grupo(r, "2026-07-14", "hospedagem");
        assertEquals(2, g.noites());
        assertEquals(v("500.00"), g.limite());
    }

    @Test
    @DisplayName("Mais de uma indicação de noites")
    void mais_de_uma_indicacao() {
        Resultado r = calcular(d("d-1", "2026-07-14", "hospedagem", "480.00", true, "Hotel 2 noites + 1 diaria"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.NOITES_NAO_IDENTIFICADAS);
    }

    @Test
    @DisplayName("Valor com 3 casas")
    void valor_com_tres_casas() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "alimentacao", "33.333", true, "Cafe"));

        assertEquals(v("33.33"), despesa(r, "d-1").despesa().valor());
        assertEquals(v("33.33"), grupo(r, DIA_UTIL, "alimentacao").valorReembolsado());
    }

    @Test
    @DisplayName("Categoria em maiúsculas")
    void categoria_maiusculas() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "ALIMENTACAO", "40.00", true, "Jantar"));

        assertStatus(r, "d-1", Status.APROVADA);
        assertEquals("alimentacao", despesa(r, "d-1").despesa().categoria());
    }

    @Test
    @DisplayName("Categoria com acento")
    void categoria_com_acento() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "alimentação", "40.00", true, "Jantar"));

        assertStatus(r, "d-1", Status.APROVADA);
        assertEquals("alimentacao", despesa(r, "d-1").despesa().categoria());
    }

    @Test
    @DisplayName("Estorno de hospedagem")
    void estorno_de_hospedagem() {
        Resultado r = calcular(d("d-1", "2026-07-14", "hospedagem", "-100.00", false, "Estorno hotel"));

        assertEquals(Status.EM_REVISAO, despesa(r, "d-1").status());
        assertTrue(despesa(r, "d-1").motivos().contains(Motivo.ESTORNO_SEM_DESPESA),
                "motivos: " + despesa(r, "d-1").motivos());
    }

    @Test
    @DisplayName("Estorno já em revisão por outro motivo")
    void estorno_ja_em_revisao() {
        Resultado r = calcular(d("d-1", "2026-04-15", "transporte_urbano", "-45.00", false, "Estorno corrida"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.FORA_DO_PERIODO, Motivo.ESTORNO_SEM_DESPESA);
    }

    @Test
    @DisplayName("Dia de viagem")
    void dia_de_viagem() {
        Resultado r = calcular(
                d("d-1", "2026-07-14", "hospedagem", "480.00", true, "Hotel 2 noites"),
                d("d-2", "2026-07-15", "alimentacao", "85.00", true, "Jantar"));

        Grupo g = grupo(r, "2026-07-15", "alimentacao");
        assertEquals(v("90.00"), g.limite());
        assertEquals(v("85.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Dia de saída da hospedagem")
    void dia_de_saida() {
        Resultado r = calcular(
                d("d-1", "2026-07-14", "hospedagem", "480.00", true, "Hotel 2 noites"),
                d("d-2", "2026-07-16", "alimentacao", "85.00", true, "Jantar"));

        Grupo g = grupo(r, "2026-07-16", "alimentacao");
        assertEquals(v("60.00"), g.limite());
        assertEquals(v("60.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Sábado")
    void sabado() {
        Resultado r = calcular(d("d-1", SABADO, "alimentacao", "85.00", true, "Almoco"));

        Grupo g = grupo(r, SABADO, "alimentacao");
        assertEquals(v("90.00"), g.limite());
        assertEquals(v("85.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Sábado em viagem")
    void sabado_em_viagem() {
        Resultado r = calcular(
                d("d-1", "2026-07-17", "hospedagem", "480.00", true, "Hotel 2 noites"),
                d("d-2", SABADO, "alimentacao", "100.00", true, "Almoco"));

        Grupo g = grupo(r, SABADO, "alimentacao");
        assertEquals(v("90.00"), g.limite());
        assertEquals(v("90.00"), g.valorReembolsado());
        assertEquals(Ampliacao.VIAGEM, g.ampliacao());
    }

    // ---- Casos da v4 (spec 2.0, D-006) ----

    @Test
    @DisplayName("Centro de custo com tabela própria")
    void centro_com_tabela_propria() {
        Resultado r = calcularNoCentro("CC-COMERCIAL", d("d-1", DIA_UTIL, "alimentacao", "95.00", true, "Almoco"));

        Grupo g = grupo(r, DIA_UTIL, "alimentacao");
        assertEquals(v("90.00"), g.limite());
        assertEquals(v("90.00"), g.valorReembolsado());
        assertEquals("CC-COMERCIAL", r.tabelaAplicada());
    }

    @Test
    @DisplayName("Categoria ausente da tabela do CC")
    void categoria_ausente_da_tabela_do_cc() {
        Resultado r = calcularNoCentro("CC-ADM", d("d-1", "2026-07-14", "hospedagem", "200.00", true, "Pousada 1 diaria"));

        assertStatus(r, "d-1", Status.RECUSADA, Motivo.CATEGORIA_FORA_DA_POLITICA);
    }

    @Test
    @DisplayName("Categoria com limite zero")
    void categoria_com_limite_zero() {
        Resultado r = calcularNoCentro("CC-ENG-PLATAFORMA",
                d("d-1", "2026-07-14", "hospedagem", "480.00", true, "Hotel Rio - 2 diarias"));

        assertStatus(r, "d-1", Status.RECUSADA, Motivo.CATEGORIA_FORA_DA_POLITICA);
    }

    @Test
    @DisplayName("Hospedagem recusada ainda gera viagem")
    void hospedagem_recusada_gera_viagem() {
        Resultado r = calcularNoCentro("CC-ENG-PLATAFORMA",
                d("d-1", "2026-07-14", "hospedagem", "480.00", true, "Hotel Rio - 2 diarias"),
                d("d-2", "2026-07-15", "alimentacao", "100.00", true, "Jantar"));

        assertStatus(r, "d-1", Status.RECUSADA, Motivo.CATEGORIA_FORA_DA_POLITICA);
        Grupo g = grupo(r, "2026-07-15", "alimentacao");
        assertEquals(v("112.50"), g.limite());
        assertEquals(v("100.00"), g.valorReembolsado());
        assertEquals(Ampliacao.VIAGEM, g.ampliacao());
    }

    @Test
    @DisplayName("Representação no CC-COMERCIAL")
    void representacao_no_comercial() {
        Resultado r = calcularNoCentro("CC-COMERCIAL",
                d("d-1", "2026-07-13", "representacao", "340.00", true, "Jantar com prospect"));

        Grupo g = grupo(r, "2026-07-13", "representacao");
        assertEquals(v("300.00"), g.limite());
        assertEquals(v("300.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Representação fora do CC-COMERCIAL")
    void representacao_fora_do_comercial() {
        Resultado r = calcular(d("d-1", "2026-07-17", "representacao", "190.00", true, "Jantar com fornecedor"));

        assertStatus(r, "d-1", Status.RECUSADA, Motivo.CATEGORIA_FORA_DA_POLITICA);
    }

    @Test
    @DisplayName("Representação no sábado")
    void representacao_no_sabado() {
        Resultado r = calcularNoCentro("CC-COMERCIAL", d("d-1", SABADO, "representacao", "400.00", true, "Jantar"));

        Grupo g = grupo(r, SABADO, "representacao");
        assertEquals(v("450.00"), g.limite());
        assertEquals(v("400.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Nome do CC com outra grafia")
    void nome_do_cc_com_outra_grafia() {
        Resultado r = calcularNoCentro("cc-comercial", d("d-1", DIA_UTIL, "alimentacao", "95.00", true, "Almoco"));

        assertEquals("padrao", r.tabelaAplicada());
        assertEquals(v("60.00"), grupo(r, DIA_UTIL, "alimentacao").limite());
    }

    @Test
    @DisplayName("Despesa sem moeda")
    void despesa_sem_moeda() {
        Resultado r = calcular(d("d-1", DIA_UTIL, "alimentacao", "40.00", true, "Almoco"));

        Despesa despesa = despesa(r, "d-1").despesa();
        assertEquals("BRL", despesa.moeda());
        assertFalse(despesa.emMoedaEstrangeira());
        assertNull(despesa.taxaCambio());
        assertEquals(v("40.00"), despesa.valor());
    }

    @Test
    @DisplayName("Moeda sem cotação no arquivo")
    void moeda_sem_cotacao() {
        Resultado r = calcular(dm("d-1", "2026-07-21", "alimentacao", "55.00", "GBP", true, "Almoco - Londres"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.CAMBIO_INDISPONIVEL);
        assertNull(despesa(r, "d-1").despesa().valor());
    }

    @Test
    @DisplayName("Despesa em dia sem cotação")
    void despesa_em_dia_sem_cotacao() {
        Resultado r = calcular(dm("d-1", SABADO, "alimentacao", "30.00", "EUR", true, "Jantar de sabado"));

        Despesa despesa = despesa(r, "d-1").despesa();
        assertEquals(v("178.80"), despesa.valor());
        assertEquals(v("5.96"), despesa.taxaCambio());
        assertEquals(LocalDate.of(2026, 7, 17), despesa.dataCotacao());
    }

    @Test
    @DisplayName("Data anterior à primeira cotação")
    void data_anterior_a_primeira_cotacao() {
        Resultado r = calcular(dm("d-1", "2026-07-10", "alimentacao", "20.00", "USD", true, "Almoco"));

        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.CAMBIO_INDISPONIVEL);
    }

    @Test
    @DisplayName("Conversão antes do arredondamento")
    void conversao_antes_do_arredondamento() {
        Resultado r = calcular(dm("d-1", "2026-07-13", "alimentacao", "10.005", "USD", true, "Cafe"));

        assertEquals(v("54.23"), despesa(r, "d-1").despesa().valor());
    }

    @Test
    @DisplayName("Nota fiscal em moeda estrangeira")
    void nota_fiscal_em_moeda_estrangeira() {
        Resultado r = calcular(dm("d-1", "2026-07-20", "transporte_urbano", "40.00", "USD", false, "Corridas do dia"));

        assertEquals(v("220.00"), despesa(r, "d-1").despesa().valor());
        assertStatus(r, "d-1", Status.EM_REVISAO, Motivo.NF_AUSENTE);
    }

    @Test
    @DisplayName("Estorno em moeda estrangeira")
    void estorno_em_moeda_estrangeira() {
        Resultado r = calcular(
                dm("d-1", "2026-07-20", "transporte_urbano", "50.00", "USD", true, "Corrida"),
                dm("d-2", "2026-07-20", "transporte_urbano", "-10.00", "USD", false, "Estorno corrida"));

        Grupo g = grupo(r, "2026-07-20", "transporte_urbano");
        assertEquals(v("220.00"), g.valorTotal());
        assertEquals(v("80.00"), g.valorReembolsado());
    }

    @Test
    @DisplayName("Mesmo valor em moedas diferentes")
    void mesmo_valor_em_moedas_diferentes() {
        Resultado r = calcular(
                dm("d-1", "2026-07-14", "alimentacao", "22.00", "EUR", true, "Almoco"),
                dm("d-2", "2026-07-14", "alimentacao", "22.00", "USD", true, "Almoco"));

        assertFalse(despesa(r, "d-1").motivos().contains(Motivo.DUPLICATA));
        assertFalse(despesa(r, "d-2").motivos().contains(Motivo.DUPLICATA));
    }

    @Test
    @DisplayName("Rastreabilidade: toda linha da seção 7 da spec tem um teste nesta classe")
    void toda_linha_da_secao_7_tem_teste() throws Exception {
        List<String> linhas = Files.readAllLines(Path.of("specs", "001-motor-reembolso", "spec.md"),
                StandardCharsets.UTF_8);
        Set<String> casosDaSpec = new TreeSet<>();
        boolean naSecao = false;
        List<String> tabela = new ArrayList<>();
        for (String linha : linhas) {
            if (linha.startsWith("## 7.")) {
                naSecao = true;
            } else if (linha.startsWith("## ") && naSecao) {
                break;
            } else if (naSecao && linha.startsWith("|")) {
                tabela.add(linha);
            }
        }
        for (String linha : tabela.subList(2, tabela.size())) {   // pula cabeçalho e separador
            casosDaSpec.add(linha.split("\\|")[1].trim());
        }

        Set<String> casosTestados = Arrays.stream(CasosDeBordaTest.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Test.class) && m.isAnnotationPresent(DisplayName.class))
                .map(Method::getName)
                .filter(nome -> !nome.equals("toda_linha_da_secao_7_tem_teste"))
                .map(nome -> metodo(nome).getAnnotation(DisplayName.class).value())
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(casosDaSpec, casosTestados);
    }

    private static Method metodo(String nome) {
        try {
            return CasosDeBordaTest.class.getDeclaredMethod(nome);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
