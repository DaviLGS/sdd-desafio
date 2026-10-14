# Plano Técnico — Motor de Cálculo de Reembolso

**Versão:** 1.0 · **Baseado na spec:** 1.1

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
entrada JSON ──► LeitorEntrada ──► Entrada (domínio) ──► MotorReembolso ──► Resultado (domínio) ──► EscritorSaida ──► saída JSON
                 (I/O + validação)                       (regras puras)                           (I/O)
                         ▲                                                                           ▲
                         └──────────────────────────────── Main (CLI) ───────────────────────────────┘
```

| Bloco | Responsabilidade |
|---|---|
| `cli.Main` | Interpreta `calcular --input X --output Y`, chama leitor → motor → escritor e define o código de saída do processo. |
| `io.LeitorEntrada` | Lê o JSON, valida o formato (seção 4 da spec) e constrói o domínio. Entrada inválida gera `EntradaInvalidaException`. |
| `io.EscritorSaida` | Serializa o `Resultado` no schema da spec (seção 4), com todos os valores com 2 casas. |
| `dominio.*` | Records imutáveis: `Entrada`, `Despesa`, `Resultado`, `ResultadoDespesa`, `Grupo`, enums `Status`, `Motivo`, `Ampliacao`. |
| `regras.*` | Uma classe por regra ou grupo de regras, todas funções puras (sem I/O). |
| `regras.MotorReembolso` | Orquestra as regras **na ordem da seção 8 da spec**. |

**Fronteiras:** só `cli` e `io` tocam disco e JSON. Todo o pacote `regras` recebe e devolve objetos de domínio. Assim, cada RN pode ser testada sem arquivo, e uma mudança de requisito no Dia 2 tende a ficar contida em `regras` e `Politica`.

## 3. Modelo de dados

```java
record Entrada(Colaborador colaborador, Periodo periodo, List<Despesa> despesas)
record Periodo(String competencia, LocalDate inicio, LocalDate fim)
record Despesa(String id, LocalDate data, String categoriaOriginal, String categoria,   // categoria normalizada (RN-001)
               String descricao, String fornecedor, BigDecimal valor,                  // valor já arredondado (RN-002)
               boolean temNotaFiscal)

enum Status  { APROVADA, EM_REVISAO, RECUSADA }       // saída: aprovada | em_revisao | recusada
enum Motivo  { CATEGORIA_FORA_DA_POLITICA, FORA_DO_PERIODO, DUPLICATA,
               NF_AUSENTE, NOITES_NAO_IDENTIFICADAS, ESTORNO_SEM_DESPESA }  // cada um sabe se recusa ou revisa
enum Ampliacao { NENHUM, VIAGEM, FIM_DE_SEMANA_OU_PLANTAO }

record ResultadoDespesa(Despesa despesa, Status status, List<Motivo> motivos, String justificativa)
record Grupo(LocalDate data, String categoria, List<String> idsDespesas, Integer noites,  // noites só em hospedagem
             BigDecimal valorTotal, BigDecimal limite, Ampliacao ampliacao, BigDecimal valorReembolsado)
record Resultado(Colaborador colaborador, Periodo periodo, List<ResultadoDespesa> despesas,
                 List<Grupo> grupos, BigDecimal totalReembolsado, Map<Status, Integer> quantidade)
```

A **justificativa** é montada a partir dos motivos e do grupo, com textos fixos por motivo. Assim ela fica determinística e testável.

## 4. Como a política é representada

Os valores da política (limites base, fator de ampliação de 50%, piso da NF de R$ 100,00, categorias elegíveis) vivem **num único lugar**: o record `regras.Politica`, com uma constante `Politica.V3`. O motor recebe a `Politica` como parâmetro.

- **Mudança de valor** (ex.: limite de alimentação passa a R$ 70): altera uma linha.
- **Testes** podem criar uma `Politica` diferente sem tocar no código das regras.
- **Descartado:** arquivo de configuração externo (JSON/YAML). Exigiria validação, caminho de arquivo e documentação a mais, e a CLI fixa da spec não prevê esse parâmetro. Se o Dia 2 pedir políticas variáveis em tempo de execução, isso é revisto.

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
**Decisão:** `0` = sucesso; `1` = entrada inválida (RN de formato, seção 4), com mensagem em stderr e sem arquivo de saída; `2` = uso incorreto da CLI (comando ou argumentos).
**Consequência:** é possível testar automaticamente que uma entrada inválida não gera saída.

### DT-006 — Normalização de texto
**Decisão:** categoria e descrição são comparadas após `Normalizer.Form.NFD`, remoção de marcas diacríticas e conversão para minúsculas. É o que a RN-001 (categoria) e a RN-008 ("plantão") exigem. O mesmo normalizador é usado para ler noites na RN-005 ("diária"/"noite").
**Consequência:** "plantão", "Plantao" e "PLANTÃO" são equivalentes.

## 6. Estratégia de testes

- **Nível:**
  - **Unitário** (~80%): uma classe de teste por RN, chamando as regras diretamente com objetos de domínio.
  - **Integração** (~15%): `MotorReembolso` com a entrada de exemplo completa, conferindo a tabela da seção 9 da spec (R$ 680,53).
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
| Mudança do Dia 2 exigir uma regra que não cabe em `Politica` (ex.: limite por colaborador) | Média | O motor recebe a `Politica` como parâmetro; a mudança entra pela spec e pela `DECISIONS.md`, e só então no código. |
| Corretor sem JDK 17+ ou sem Maven | Baixa | README com pré-requisitos explícitos e versões testadas. |
| Ordem de aplicação das regras implementada diferente da seção 8 | Média | `MotorReembolso` segue a seção 8 passo a passo, com comentário citando cada passo. O teste de integração com o exemplo pega desvios. |
