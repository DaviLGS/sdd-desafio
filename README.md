# Motor de Cálculo de Reembolso

CLI que lê um JSON com as despesas de um colaborador num período e gera um JSON dizendo, para
cada despesa, se ela foi **aprovada**, está **em revisão** ou foi **recusada**, por quê, e quanto
é reembolsável no total.

Projeto feito para o [Desafio SDD](DESAFIO.md) (Spec Driven Development). A política de reembolso
do RH é ambígua; todas as interpretações estão decididas e justificadas na
[spec](specs/001-motor-reembolso/spec.md).

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

Isso compila, roda os testes e gera `target/motor-reembolso.jar`. Depois:

```bash
java -jar target/motor-reembolso.jar calcular --input exemplos/despesas-exemplo.json --output resultado.json
```

Saída esperada no console:

```
reembolso calculado: R$ 680.53 - saida gravada em resultado.json
```

O comando é o mesmo no Bash, no PowerShell e no Prompt de Comando.

### Códigos de saída

| Código | Significado |
|---|---|
| `0` | Sucesso; o arquivo de saída foi gravado |
| `1` | Entrada inválida (JSON malformado, campo ausente, data inválida…); nenhum arquivo é gravado |
| `2` | Uso incorreto (comando ou opção faltando/desconhecida) |
| `3` | Falha ao gravar a saída (ex.: pasta inexistente) |

## Testar

```bash
mvn test
```

São 237 testes. Os nomes remetem às regras da spec. Por exemplo:
- `Rn006NotaFiscalTest` testa a RN-006;
- `CasosDeBordaTest` tem um teste por linha da seção 7 da spec, e falha se uma linha nova da spec ficar sem teste;
- `MotorReembolsoExemploTest` confere o arquivo de exemplo contra a tabela da seção 9.

## Entrada e saída

- **Entrada:** o formato de [`exemplos/despesas-exemplo.json`](exemplos/despesas-exemplo.json).
- **Saída:** definida na [seção 4 da spec](specs/001-motor-reembolso/spec.md#4-entrada-e-saída). Em resumo:
  - **`despesas`:** uma linha por despesa da entrada, com `status`, `motivos` e `justificativa`.
  - **`grupos`:** onde os limites são aplicados, com a soma do que foi gasto, o limite, a ampliação e o valor reembolsado. Há um grupo por dia e categoria para alimentação e transporte, e um por hospedagem.
  - **`resumo`:** o `total_reembolsado` e a quantidade de despesas por status.

Para o arquivo de exemplo: **R$ 680,53** reembolsados; 6 despesas aprovadas, 7 em revisão, 1 recusada.

## Documentação

| Arquivo | O que tem |
|---|---|
| [`spec.md`](specs/001-motor-reembolso/spec.md) | **O quê e por quê:** regras (RN-001…RN-012), as 20 ambiguidades da política e a decisão de cada uma, casos de borda, critérios de aceite |
| [`plan.md`](specs/001-motor-reembolso/plan.md) | **Como:** stack, arquitetura, decisões técnicas (DT-001…DT-006), estratégia de testes |
| [`tasks.md`](specs/001-motor-reembolso/tasks.md) | **Em que ordem:** T-001…T-022, cada uma com requisitos, aceite e commit; matriz de cobertura |
| [`DECISIONS.md`](specs/001-motor-reembolso/DECISIONS.md) | Log de cada mudança de spec: o que mudou, por quê, o que quebrou |
| [`docs/sessions/`](docs/sessions) | Exports das conversas com o Claude Code |
| [`docs/RELATORIO.md`](docs/RELATORIO.md) | Relatório final do desafio |
| [`CLAUDE.md`](CLAUDE.md) | Convenções do projeto para o agente |

## Estrutura

```
src/reembolso/
├── cli/       Main: linha de comando e códigos de saída
├── io/        leitura/validação da entrada e escrita da saída (JSON)
├── dominio/   records e enums (Despesa, Resultado, Grupo, Status, Motivo…)
└── regras/    regras de negócio puras, sem I/O — uma classe por regra + MotorReembolso
tests/reembolso/   testes, espelhando os pacotes acima
```

Os valores da política (60, 80, 250, +50%, piso da NF) existem só em
[`Politica.java`](src/reembolso/regras/Politica.java).

## Sobre o desafio

O enunciado original, a rubrica e o FAQ continuam em [`DESAFIO.md`](DESAFIO.md),
[`RUBRICA.md`](RUBRICA.md) e [`FAQ.md`](FAQ.md).
