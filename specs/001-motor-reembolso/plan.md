# Plano Técnico — Motor de Cálculo de Reembolso

**Versão:** 2.0 · **Baseado na spec:** 2.1

> Aqui mora o COMO. Este arquivo pode e deve falar de linguagem, biblioteca e
> arquitetura. O que ele **não** pode é introduzir regra de negócio nova — se
> apareceu uma, ela pertence à `spec.md`.

---

## 1. Stack

| Escolha | O quê | Por quê | O que descartei e por quê |
|---|---|---|---|
| Linguagem | Java, compilado com `--release 17` | É a linguagem que o responsável domina. O alvo 17 roda em qualquer JDK ≥ 17, então o corretor não precisa ter o JDK 25 local. | Python: segunda opção do responsável, descartada por menor familiaridade. Alvo Java 21+: ganharia pouco (pattern matching em `switch`) e restringiria quem pode rodar. |
| Build | Maven 3.9 | Já instalado; padrão de mercado; gera um JAR executável único. | Gradle: não instalado; mais uma ferramenta para aprender em 2 dias. |
| Testes | JUnit 5 (Jupiter) | Padrão em Java; `@DisplayName` e `@ParameterizedTest` deixam o nome do teste citar a RN. | JUnit 4: sem testes parametrizados decentes nem `@DisplayName`. |
| Parsing/validação | Jackson Databind | Leitura e escrita de JSON madura; lê números direto como `BigDecimal`, sem passar por `double`. | `org.json` / Gson: o Gson converte para `double` por padrão, o que é perigoso para dinheiro. Parser manual: risco de bug sem ganho. |
| Aritmética monetária | `java.math.BigDecimal`, escala 2 | Representação decimal exata; o arredondamento da RN-002 é implementado sobre ela. | `double`: 33,333 vira 33,33299999…, o que quebra a RN-002 e as comparações de fronteira da RN-006 (100,00 vs 100,01). `long` em centavos: exigiria arredondar *antes* de converter, e a entrada tem 3+ casas. |

## 2. Arquitetura

```
despesas JSON -> LeitorEntrada  -> Entrada         -+
politica JSON -> LeitorPolitica -> TabelaPoliticas  +-> MotorReembolso -> Resultado -> EscritorSaida -> saida JSON
cambio JSON   -> LeitorCambio   -> TabelaCambio    -+   (regras puras)
                 (I/O + validacao)  (dominio)
                        ^                                                          ^
                        +----------------------- Main (CLI) -----------------------+
```

| Bloco | Responsabilidade |
|---|---|
| `cli.Main` | Interpreta `calcular --input X --politica P --cambio C --output Y`, chama leitores → motor → escritor e define o código de saída do processo. |
| `io.LeitorEntrada` | Lê o JSON de despesas (inclusive `moeda`), valida o formato (seção 4 da spec) e constrói o domínio. Entrada inválida gera `EntradaInvalidaException`. |
| `io.LeitorPolitica` | Lê e valida o arquivo de política → `TabelaPoliticas` (padrão + tabelas por CC). Arquivo inválido gera `EntradaInvalidaException`. |
| `io.LeitorCambio` | Lê e valida o arquivo de câmbio → `TabelaCambio`. Arquivo inválido gera `EntradaInvalidaException`. |
| `io.EscritorSaida` | Serializa o `Resultado` no schema da spec (seção 4), com todos os valores com 2 casas. |
| `dominio.*` | Records imutáveis: `Entrada`, `Despesa`, `Resultado`, `ResultadoDespesa`, `Grupo`, `TabelaPoliticas`, `LimiteCategoria`, `TabelaCambio`, enums `Status`, `Motivo`, `Ampliacao`, `Periodicidade`. Só dados, sem regra. |
| `regras.*` | Uma classe por regra ou grupo de regras, todas funções puras (sem I/O). |
| `regras.MotorReembolso` | Orquestra as regras **na ordem da seção 8 da spec**. |

**Fronteiras:** só `cli` e `io` tocam disco e JSON. Os leitores **não** aplicam regra de negócio: a normalização das categorias da política (RN-001), a escolha da tabela (RN-013) e a busca da cotação (RN-014) ficam em `regras`. Todo o pacote `regras` recebe e devolve objetos de domínio. Assim, cada RN pode ser testada sem arquivo, e uma mudança de requisito no Dia 2 tende a ficar contida em `regras` e `Politica`.

## 3. Modelo de dados

```java
record Entrada(Colaborador colaborador, Periodo periodo, List<Despesa> despesas)
record Periodo(String competencia, LocalDate inicio, LocalDate fim)
record Despesa(String id, LocalDate data, String categoriaOriginal, String categoria,   // categoria normalizada (RN-001)
               String descricao, String fornecedor, BigDecimal valor,                  // em BRL, convertido e arredondado; null sem cotação
               boolean temNotaFiscal,
               String moeda, BigDecimal valorOriginal,                                 // como vieram na entrada (moeda ausente = "BRL")
               BigDecimal taxaCambio, LocalDate dataCotacao)                           // null em BRL ou sem cotação
// + construtor de 8 argumentos (despesa em BRL), mantido para os testes da v1

record TabelaPoliticas(Map<String, Map<String, LimiteCategoria>> centrosCusto,
                       Map<String, LimiteCategoria> padrao,
                       BigDecimal pisoNotaFiscal, BigDecimal percentualAcrescimo)   // nomes crus, como no arquivo
record LimiteCategoria(BigDecimal limite, Periodicidade periodicidade)
enum Periodicidade { DIA, DIARIA }
record TabelaCambio(Map<LocalDate, Map<String, BigDecimal>> taxas)

enum Status  { APROVADA, EM_REVISAO, RECUSADA }       // saída: aprovada | em_revisao | recusada
enum Motivo  { CATEGORIA_FORA_DA_POLITICA, FORA_DO_PERIODO, DUPLICATA,
               NF_AUSENTE, NOITES_NAO_IDENTIFICADAS, ESTORNO_SEM_DESPESA,
               CAMBIO_INDISPONIVEL }                                         // cada um sabe se recusa ou revisa
enum Ampliacao { NENHUM, VIAGEM, FIM_DE_SEMANA_OU_PLANTAO }

record ResultadoDespesa(Despesa despesa, Status status, List<Motivo> motivos, String justificativa)
record Grupo(LocalDate data, String categoria, List<String> idsDespesas, Integer noites,  // noites só em hospedagem
             BigDecimal valorTotal, BigDecimal limite, Ampliacao ampliacao, BigDecimal valorReembolsado)
record Resultado(Colaborador colaborador, Periodo periodo, String tabelaAplicada, List<ResultadoDespesa> despesas,
                 List<Grupo> grupos, BigDecimal totalReembolsado, Map<Status, Integer> quantidade)
```

A **justificativa** é montada a partir dos motivos e do grupo, com textos fixos por motivo. Assim ela fica determinística e testável.

## 4. Como a política é representada

**Versão 2.0 (envelope do Dia 2):** a política **não** está mais no código. Ela vem do arquivo
informado em `--politica` (ex.: `exemplos/envelope/politica-v4.json`).

- `io.LeitorPolitica` lê o arquivo para `TabelaPoliticas`, com os nomes como estão no arquivo.
- `regras.SelecaoTabela` aplica a RN-013 (centro de custo → tabela, ou `padrao`) e a RN-001 nos
  nomes de categoria, e devolve uma `regras.Politica` já resolvida para o colaborador:
  `Politica(nomeTabela, Map<categoria, LimiteCategoria>, pisoNotaFiscal, percentualAcrescimo)`.
- As regras continuam recebendo uma `Politica`, como na v1: a mudança de origem dos números não
  muda a assinatura delas. `Politica` responde `elegivel(categoria)` (limite > 0, RN-003),
  `periodicidade(categoria)`, `limiteDiario(categoria, ampliacao)` e
  `limitePorNoite(categoria, noites)`.
- **Removido:** a constante `Politica.V3`. Os testes de regra usam uma `Politica` montada no
  próprio teste (`tests/reembolso/Politicas.java`, com os valores da tabela `padrao`).

**Histórico (v1.x):** a política vivia na constante `Politica.V3`, e o arquivo externo tinha sido
descartado com a ressalva "se o Dia 2 pedir políticas variáveis em tempo de execução, isso é
revisto". Foi o que aconteceu. Como o motor já recebia a `Politica` como parâmetro, a troca ficou
contida em `Politica`, nos leitores e na CLI.

## 5. Decisões técnicas

### DT-001 — Números do JSON lidos direto como `BigDecimal`
**Contexto:** a RN-002 exige arredondar a partir do **último dígito digitado** (33,333; 3,4449). Se o número passar por `double`, o último dígito se perde.
**Decisão:** Jackson configurado com `USE_BIG_DECIMAL_FOR_FLOATS`. A RN-002 é aplicada sobre o `BigDecimal` com a escala original.
**Alternativa descartada:** ler como `double` e converter depois. Isso produz dígitos espúrios (33,332999…).
**Consequência:** fronteiras exatas (100,00 × 100,01) funcionam. Toda aritmética passa a exigir `BigDecimal`.

### DT-002 — Arredondamento em cadeia implementado como laço
**Contexto:** o arredondamento em cadeia da RN-002 difere do `HALF_UP` padrão em casos como 3,4449.
**Decisão:** enquanto a escala for maior que 2, reduzir uma casa com `RoundingMode.HALF_UP`. O `HALF_UP` do `BigDecimal` opera sobre o valor absoluto, o que já cumpre "arredonda o absoluto e mantém o sinal".
**Alternativa descartada:** `setScale(2, HALF_UP)` direto, que dá 3,44 para 3,4449 e viola a spec.
**Consequência:** é uma função pequena, com teste parametrizado cobrindo todos os exemplos da RN-002.

### DT-003 — Estrutura de pastas `src/` e `tests/` na raiz
**Contexto:** o desafio pede `src/` e `tests/` na raiz; o padrão Maven é `src/main/java` e `src/test/java`.
**Decisão:** configurar no `pom.xml` `<sourceDirectory>src</sourceDirectory>` e `<testSourceDirectory>tests</testSourceDirectory>`.
**Alternativa descartada:** o layout padrão do Maven, que divergiria da estrutura pedida na entrega.
**Consequência:** IDEs que assumem o layout padrão podem precisar reimportar o projeto. O Maven funciona normalmente.

### DT-004 — JAR executável único
**Contexto:** o README precisa permitir rodar sem intervenção.
**Decisão:** `maven-shade-plugin` gera `target/motor-reembolso.jar` com as dependências. O uso fica `java -jar target/motor-reembolso.jar calcular --input X --output Y`.
**Alternativa descartada:** `mvn exec:java`, que é mais lento e torna a passagem de argumentos menos clara.
**Consequência:** um `mvn package` produz o artefato final.

### DT-005 — Códigos de saída da CLI
**Decisão:** `0` = sucesso; `1` = entrada inválida (RN de formato, seção 4), com mensagem em stderr e sem arquivo de saída; `2` = uso incorreto da CLI (comando ou argumentos); `3` = falha ao gravar o arquivo de saída (ex.: pasta inexistente).
**Consequência:** é possível testar automaticamente que uma entrada inválida não gera saída.
**Revisão (v1.1, T-020):** o código `3` foi acrescentado na implementação da CLI. Sem ele, uma falha de gravação teria de reutilizar o `1`, que significa "entrada inválida" e enganaria quem lê o código de saída. A mensagem de sucesso no console usa só ASCII, porque o console do Windows nem sempre está em UTF-8.

### DT-007 — Política externa: leitor cru + seleção nas regras
**Contexto:** a v4 exige ler a política de fora do código (RN-013).
**Decisão:** `io.LeitorPolitica` só valida a forma do arquivo (objeto `padrao`, `limite` numérico ≥ 0, `periodicidade` `dia`/`diaria`, piso e percentual numéricos) e devolve `TabelaPoliticas` com os nomes crus. A escolha da tabela e a normalização ficam em `regras.SelecaoTabela`.
**Alternativa descartada:** o leitor já devolver a `Politica` do colaborador. Misturaria I/O com a RN-013, que precisa de teste próprio sem arquivo.
**Consequência:** `MotorReembolso` passa a receber `TabelaPoliticas` e `TabelaCambio`, e faz o passo 0 da seção 8.

### DT-008 — Câmbio: dados no domínio, busca nas regras
**Decisão:** `io.LeitorCambio` devolve `TabelaCambio` (data → moeda → taxa). A busca "na data, senão a última anterior" (RN-014, AMB-031/032) fica em `regras.ConversaoMoeda`, que também converte e arredonda (AMB-033).
**Alternativa descartada:** consultar uma API de câmbio. Fora de escopo (seção 3 da spec) e não determinístico.
**Consequência:** a conversão é testada sem arquivo.

### DT-009 — CLI com `--politica` e `--cambio` obrigatórios
**Decisão:** `calcular --input X --politica P --cambio C --output Y`, opções em qualquer ordem, todas obrigatórias (AMB-028). Arquivo de política ou câmbio ausente ou inválido → código 1, sem saída.
**Alternativa descartada:** caminhos padrão implícitos. Esconderiam qual política foi usada.
**Consequência:** o comando do README muda; o resultado depende explicitamente dos três arquivos.

### DT-010 — `Despesa` carrega o valor original e o convertido
**Decisão:** `Despesa` ganha `moeda`, `valorOriginal`, `taxaCambio` e `dataCotacao`; `valor` passa a ser o valor em BRL (ou `null` sem cotação). Um construtor de 8 argumentos (despesa em BRL) é mantido para não reescrever os testes da v1.
**Consequência:** regras que usam o valor (`NotaFiscal`) tratam `null`; `Duplicatas` passa a comparar `valorOriginal` + `moeda` (RN-009); `Estornos` identifica o estorno pelo sinal de `valorOriginal`.

### DT-006 — Normalização de texto
**Decisão:** categoria e descrição são comparadas após `Normalizer.Form.NFD`, remoção de marcas diacríticas e conversão para minúsculas. É o que a RN-001 (categoria) e a RN-008 ("plantão") exigem. O mesmo normalizador é usado para ler noites na RN-005 ("diária"/"noite").
**Consequência:** "plantão", "Plantao" e "PLANTÃO" são equivalentes.

## 6. Estratégia de testes

- **Nível:**
  - **Unitário** (~80%): uma classe de teste por RN, chamando as regras diretamente com objetos de domínio.
  - **Integração** (~15%): `MotorReembolso` com os três arquivos de entrada da seção 9 da spec (R$ 216,53; R$ 1.993,26; R$ 373,76), usando `politica-v4.json` e `cambio.json`.
  - **Ponta a ponta** (~5%): `Main` lendo e escrevendo arquivos temporários, incluindo entrada inválida → código 1 e nenhum arquivo gerado.
- **Cada `RN-NNN` da spec tem teste?** Cada classe de teste se chama `RnNNN<Nome>Test` (ex.: `Rn006NotaFiscalTest`). A tabela de cobertura no fim do `tasks.md` mapeia RN → task → classe de teste, e uma RN sem linha nessa tabela é uma lacuna visível.
- **Casos de borda da seção 7 da spec:** cada linha da tabela vira ao menos um caso de teste, citado no `@DisplayName`.
- **Nomenclatura:**
  - classe: `Rn006NotaFiscalTest`;
  - método: `valor_100_exatos_sem_nf_vai_para_revisao`;
  - `@DisplayName("RN-006 / AMB-006: R$ 100,00 sem NF → em_revisao")`.
- **Comando:** `mvn test`.

## 7. Riscos

| Risco | Probabilidade | O que faço se acontecer |
|---|---|---|
| Algum valor monetário passar por `double` em um ponto esquecido | Média | Testes de fronteira (100,00 × 100,01; 33,333; 3,4449) quebram e acusam. Revisar o diff procurando `double`/`float`. |
| Leitura de noites na descrição falhar em formatos não previstos ("2 diárias", "2diarias", "duas noites") | Média | A spec já manda para revisão quando não identifica. Ampliar os testes com variações. |
| Mudança do Dia 2 exigir uma regra que não cabe em `Politica` (ex.: limite por colaborador) | Média | **Aconteceu** (v4, D-006): limites por centro de custo e câmbio. Absorvido trocando a origem da `Politica` (DT-007) e acrescentando a conversão no passo 1 (DT-008). |
| Arquivo de política ou câmbio com formato diferente do exemplo do envelope | Média | Os leitores rejeitam com mensagem apontando o campo (código 1); nunca caem para valores embutidos (AMB-028). |
| Corretor sem JDK 17+ ou sem Maven | Baixa | README com pré-requisitos explícitos e versões testadas. |
| Ordem de aplicação das regras implementada diferente da seção 8 | Média | `MotorReembolso` segue a seção 8 passo a passo, com comentário citando cada passo. O teste de integração com o exemplo pega desvios. |
