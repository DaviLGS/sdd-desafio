# CLAUDE.md

> Este arquivo é lido pelo Claude Code no início de toda sessão. É onde moram as
> convenções que você não quer repetir em todo prompt.
> Substitua os `<...>` e apague o que não usar. Mantenha curto — CLAUDE.md longo
> é CLAUDE.md ignorado.

## O projeto

Motor de cálculo de reembolso de despesas corporativas. CLI que lê um JSON de
despesas e emite um JSON com o valor reembolsável e a justificativa de cada item.

## Fonte da verdade

`specs/001-motor-reembolso/spec.md` define **o que** o sistema faz.
`specs/001-motor-reembolso/plan.md` define **como**.
`specs/001-motor-reembolso/tasks.md` define **em que ordem**.

Quando o código e a spec discordarem, a spec está certa e o código é o bug —
a menos que a spec esteja errada, e nesse caso corrigimos a spec primeiro e
registramos em `DECISIONS.md`.

**Antes de implementar qualquer coisa, leia a task correspondente em `tasks.md`.**
Se o que eu pedi não está coberto por nenhuma task, me avise em vez de implementar.

## Regras de trabalho

- Toda regra de negócio vive na spec, não no chat e não em comentário de código.
- Se eu te explicar uma regra que não está na spec, **pare e me diga isso** antes
  de escrever código. Isso é um bug de spec.
- Todo commit referencia uma task: `feat(T-003): <descrição>`.
  Mudanças de documentação: `docs(spec):`, `docs(plan):`, `docs(tasks):`.
- Nenhuma regra de negócio entra sem teste.

## Stack e comandos

- Linguagem: Java, alvo `--release 17` (JDK 17+), build com Maven 3.9. Detalhes e motivos em `plan.md`.
- Compilar/empacotar: `mvn package` → gera `target/motor-reembolso.jar`
- Rodar: `java -jar target/motor-reembolso.jar calcular --input exemplos/despesas-exemplo.json --output resultado.json`
- Testes: `mvn test`
- Lint/format: não há linter configurado. Siga o estilo do código ao redor.

## Convenções de código

- Pastas: código em `src/`, testes em `tests/` (não o layout padrão do Maven; ver `plan.md` DT-003).
- Pacotes: `cli` (linha de comando), `io` (leitura/escrita de JSON), `dominio` (records e enums),
  `regras` (regras de negócio puras, **sem I/O**). Só `cli` e `io` tocam disco ou JSON.
- Valores da política (limites, fator de ampliação, piso da NF, categorias) vivem **apenas** em
  `regras.Politica`. Nunca escreva 60, 80, 250, 100 ou 1.5 soltos no código.
- Valores monetários: **sempre `BigDecimal`**, escala 2 após a RN-002. Nunca `double`/`float`,
  nem como passo intermediário. O JSON é lido com `USE_BIG_DECIMAL_FOR_FLOATS` (DT-001).
- Erros: entrada inválida → `EntradaInvalidaException` → código de saída 1, sem arquivo de saída.
  Uso incorreto da CLI → código 2. Falha ao gravar a saída → código 3 (DT-005).
  Mensagens de console sem acentos (o console do Windows os corrompe).
- Testes: uma classe por RN, `RnNNN<Nome>Test`, com `@DisplayName("RN-NNN / AMB-NNN: <caso>")`.
  O nome do teste precisa permitir chegar à regra da spec sem adivinhar.
- Ao concluir uma task: marque `[x]` em `tasks.md` e preencha o campo **Commit** no commit seguinte.

## Sessões

- O `/export` não funciona neste ambiente. Ao fim de cada sessão, o usuário pede "exporta a sessão":
  gere `docs/sessions/NN-descricao-curta.md` seguindo o formato de `docs/sessions/01-*.md`
  (mensagens do usuário literais, respostas condensadas, sem dados pessoais) e commite com
  `docs(sessions): exporta sessão NN — <descrição>`.

## Fora de escopo

Lista oficial na seção 3 da `spec.md`. Em resumo: não paga, não resolve revisões (só sinaliza),
não lê o conteúdo de notas fiscais, só R$, um colaborador e um período por execução, sem histórico
entre execuções, sem regras por cargo ou centro de custo. Não invente feature fora disso. Pergunte.
