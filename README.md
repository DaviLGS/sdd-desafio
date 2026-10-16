# Motor de Cálculo de Reembolso

CLI que lê as despesas de um colaborador num período, a **tabela de limites** do RH e as
**cotações de câmbio**, e gera um JSON dizendo, para cada despesa, se ela foi **aprovada**, está
**em revisão** ou foi **recusada**, por quê, e quanto é reembolsável no total.

Projeto feito para o [Desafio SDD](DESAFIO.md) (Spec Driven Development). A política de reembolso
do RH é ambígua; todas as interpretações estão decididas e justificadas na
[spec](specs/001-motor-reembolso/spec.md). A versão atual implementa a **Política v4** (envelope do
Dia 2, em [`exemplos/envelope/`](exemplos/envelope)): limites por centro de custo, lidos de um
arquivo, e despesas em moeda estrangeira.

---

## Pré-requisitos

| Ferramenta | Versão | Conferir com |
|---|---|---|
| JDK | 17 ou superior (testado com 25) | `java -version` |
| Maven | 3.9 ou superior (testado com 3.9.12) | `mvn -v` |

Na primeira compilação o Maven baixa as dependências (Jackson e JUnit 5), então é preciso internet.

## Compilar e rodar

```bash
mvn package
```

Isso compila, roda os testes e gera `target/motor-reembolso.jar`. O comando recebe **quatro
arquivos, todos obrigatórios**, em qualquer ordem:

| Opção | Arquivo |
|---|---|
| `--input` | despesas do colaborador |
| `--politica` | tabela de limites por centro de custo (ex.: `exemplos/envelope/politica-v4.json`) |
| `--cambio` | cotações por data (ex.: `exemplos/envelope/cambio.json`) |
| `--output` | onde gravar o resultado |

Os três exemplos do projeto:

```bash
java -jar target/motor-reembolso.jar calcular --input exemplos/despesas-exemplo.json --politica exemplos/envelope/politica-v4.json --cambio exemplos/envelope/cambio.json --output resultado.json
```

```bash
java -jar target/motor-reembolso.jar calcular --input exemplos/envelope/despesas-envelope.json --politica exemplos/envelope/politica-v4.json --cambio exemplos/envelope/cambio.json --output resultado.json
```

```bash
java -jar target/motor-reembolso.jar calcular --input exemplos/envelope/despesas-envelope-cc-desconhecido.json --politica exemplos/envelope/politica-v4.json --cambio exemplos/envelope/cambio.json --output resultado.json
```

Saída esperada no console, em ordem:

```
reembolso calculado: R$ 216.53 (tabela CC-ENG-PLATAFORMA) - saida gravada em resultado.json
reembolso calculado: R$ 1993.26 (tabela CC-COMERCIAL) - saida gravada em resultado.json
reembolso calculado: R$ 373.76 (tabela padrao) - saida gravada em resultado.json
```

Os comandos são os mesmos no Bash, no PowerShell e no Prompt de Comando.

### Códigos de saída

| Código | Significado |
|---|---|
| `0` | Sucesso; o arquivo de saída foi gravado |
| `1` | Arquivo de despesas, política ou câmbio inválido ou inexistente; nenhum arquivo é gravado |
| `2` | Uso incorreto (comando ou opção faltando/desconhecida) |
| `3` | Falha ao gravar a saída (ex.: pasta inexistente) |

## Testar

```bash
mvn test
```

São 344 testes. Os nomes remetem às regras da spec. Por exemplo:
- `Rn006NotaFiscalTest` testa a RN-006 e `Rn014ConversaoMoedaTest` testa a RN-014;
- `CasosDeBordaTest` tem um teste por linha da seção 7 da spec, e falha se uma linha nova da spec ficar sem teste;
- `MotorReembolsoExemploTest` confere os três arquivos de exemplo contra as tabelas da seção 9.

## Entrada e saída

- **Entrada:** despesas no formato de [`exemplos/despesas-exemplo.json`](exemplos/despesas-exemplo.json), com o campo opcional `moeda` (ausente = BRL); política e câmbio no formato dos arquivos de [`exemplos/envelope/`](exemplos/envelope).
- **Saída:** definida na [seção 4 da spec](specs/001-motor-reembolso/spec.md#4-entrada-e-saída). Em resumo:
  - **`despesas`:** uma linha por despesa, com `valor` em BRL, `status`, `motivos` e `justificativa`. Despesas em moeda estrangeira trazem também `moeda`, `valor_original`, `taxa_cambio` e `data_cotacao`.
  - **`grupos`:** onde os limites são aplicados, com a soma do que foi gasto, o limite, a ampliação e o valor reembolsado. Há um grupo por dia e categoria para limites diários (alimentação, transporte, representação) e um por hospedagem.
  - **`resumo`:** a `tabela_aplicada` (centro de custo ou `padrao`), o `total_reembolsado` e a quantidade de despesas por status.

| Arquivo | Tabela aplicada | Total | Aprovadas / em revisão / recusadas |
|---|---|---|---|
| `despesas-exemplo.json` | `CC-ENG-PLATAFORMA` | R$ 216,53 | 5 / 6 / 3 |
| `envelope/despesas-envelope.json` | `CC-COMERCIAL` | R$ 1.993,26 | 7 / 2 / 1 |
| `envelope/despesas-envelope-cc-desconhecido.json` | `padrao` | R$ 373,76 | 3 / 0 / 1 |

## Documentação

| Arquivo | O que tem |
|---|---|
| [`spec.md`](specs/001-motor-reembolso/spec.md) | **O quê e por quê:** regras (RN-001…RN-014), as 37 ambiguidades e a decisão de cada uma, casos de borda, critérios de aceite |
| [`plan.md`](specs/001-motor-reembolso/plan.md) | **Como:** stack, arquitetura, decisões técnicas (DT-001…DT-010), estratégia de testes |
| [`tasks.md`](specs/001-motor-reembolso/tasks.md) | **Em que ordem:** T-001…T-033, cada uma com requisitos, aceite e commit; matriz de cobertura |
| [`DECISIONS.md`](specs/001-motor-reembolso/DECISIONS.md) | Log de cada mudança de spec (D-001…D-007): o que mudou, por quê, o que quebrou |
| [`exemplos/envelope/`](exemplos/envelope) | O envelope do Dia 2 (Política v4) e os arquivos que vieram com ele |
| [`docs/sessions/`](docs/sessions) | Exports das conversas com o Claude Code |
| [`docs/RELATORIO.md`](docs/RELATORIO.md) | Relatório final do desafio |
| [`CLAUDE.md`](CLAUDE.md) | Convenções do projeto para o agente |

## Estrutura

```
src/reembolso/
├── cli/       Main: linha de comando e códigos de saída
├── io/        leitura/validação de despesas, política e câmbio, e escrita da saída (JSON)
├── dominio/   records e enums (Despesa, Resultado, Grupo, TabelaPoliticas, TabelaCambio, Motivo…)
└── regras/    regras de negócio puras, sem I/O — uma classe por regra + MotorReembolso
tests/reembolso/   testes, espelhando os pacotes acima
```

Nenhum valor da política (limites, percentual, piso da NF) está no código: todos vêm do arquivo
passado em `--politica`.

## Sobre o desafio

O enunciado original, a rubrica e o FAQ continuam em [`DESAFIO.md`](DESAFIO.md),
[`RUBRICA.md`](RUBRICA.md) e [`FAQ.md`](FAQ.md).
