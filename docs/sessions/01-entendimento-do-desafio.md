# Sessão 01 — Entendimento do desafio e preparação

**Data:** 2026-10-14 · **Ferramenta:** Claude Code (app desktop)

> **Formato deste export:** o comando `/export` não funcionou neste ambiente.
> Seguindo a alternativa do `FAQ.md`, pedi ao Claude que reproduzisse todas as
> mensagens da janela de contexto como um único bloco. Informações pessoais
> identificáveis (nome do diretório pessoal, e-mail) foram ocultadas como `<oculto>`.
> As chamadas de ferramenta estão resumidas entre colchetes.

---

## Usuário

leia a pasta sdd-desafio anexada e me explique passo a passo o que preciso fazer para completar o desafio

## Claude

[Ferramentas: listou os arquivos do repositório e leu `README.md`, `DESAFIO.md`, `RUBRICA.md`, `FAQ.md`, `docs/RELATORIO.md`, `exemplos/despesas-exemplo.json` e os quatro arquivos em `specs/001-motor-reembolso/`.]

Li todos os arquivos. O ponto principal primeiro: **o desafio não avalia principalmente o código.** O produto funcionando vale só 10 de 100 pontos. Os outros 90 dependem de você mostrar que trabalhou com Spec Driven Development (SDD) de verdade: especificação, rastreabilidade, resposta à mudança do Dia 2 e relatório.

### Onde você está agora

O passo de preparação já está feito. O commit `chore: estrutura inicial a partir do template` copiou `CLAUDE.md`, `specs/` e `docs/` para a raiz. Esses arquivos ainda são só esqueletos, com campos `<...>` para preencher.

### Passo a passo

**Passo 0 — Antes de tudo: teste o `/export`.** Troque duas mensagens no Claude Code, rode `/export` e confirme que o arquivo foi gerado. Sem `docs/sessions/`, o critério do relatório (20 pts) vale zero. Depois, exporte ao final de cada sessão: `docs/sessions/01-spec-inicial.md`, `02-...` e assim por diante. Uma única sessão exportada no fim limita a nota a 12. Confirme também que o seu fork é público.

**Passo 1 — Estude a entrada (Dia 1, manhã).** Leia `exemplos/despesas-exemplo.json` item por item e compare cada despesa com as 9 regras da política em `DESAFIO.md`. Cada uma das 14 despesas está ali para expor alguma ambiguidade. A rubrica diz que existem no mínimo 8 ambiguidades, de três tipos:
- Unidade de aplicação: a que conjunto a regra se aplica (por dia, por despesa, por diária…).
- Fronteira: comparações, arredondamento, limites inclusivos ou exclusivos.
- Dado ausente: a política pressupõe uma informação que o JSON não traz.

**Passo 2 — Escreva a `spec.md` (sem código ainda).** Preencha `specs/001-motor-reembolso/spec.md`:
- Problema, objetivo e fora de escopo. O "fora de escopo" vale ponto.
- Entrada e saída. O schema de saída é você quem define. Inclua um exemplo concreto.
- Regras de negócio `RN-001`, `RN-002`…, cada uma com um aceite numérico concreto.
- Ambiguidades `AMB-001`… (seção 6, que vale a maior parte dos 25 pontos). Para cada uma: texto original do RH, leituras possíveis, sua decisão e justificativa em uma linha.
- Casos de borda, ordem de aplicação das regras e critérios de aceite.

Proibido na spec: nome de linguagem, biblioteca, classe ou pasta. O teste é: "se eu trocasse de linguagem amanhã, isso mudaria?". Se mudaria, o lugar é o `plan.md`. Não existe interpretação certa. O que perde ponto é uma ambiguidade resolvida em silêncio dentro do código. Commit: `docs(spec): ...`

**Passo 3 — Escreva o `plan.md`.** Stack (e o que você descartou); arquitetura com o núcleo de regras puro separado do I/O; aritmética monetária sem float (decimal ou centavos inteiros); onde os limites ficam, decisões técnicas e estratégia de testes, com nomes de teste que remetem às `RN`. Commit: `docs(plan): ...`

**Passo 4 — Escreva o `tasks.md`.** Quebre o trabalho em `T-001`, `T-002`… Cada task tem: o que faz, quais `RN`/`AMB` atende e um aceite do tipo "o teste X passa". Cada task deve ser pequena o bastante para virar um commit. Commit: `docs(tasks): ...`. Feche spec, plan e tasks até o meio-dia, mesmo imperfeitos.

**Passo 5 — Complete o `CLAUDE.md`.** Linguagem, comandos para rodar, testar e fazer lint, representação monetária e fora de escopo. A ausência do `CLAUDE.md` custa -3.

**Passo 6 — Implemente seguindo as tasks (Dia 1, tarde).** Uma task por vez, com teste junto, e commits do tipo `feat(T-003): ...`. Marque `[x]` no `tasks.md` ao longo do caminho. A CLI precisa rodar assim: `<comando> calcular --input despesas.json --output resultado.json`. Regra de ouro: se você precisar explicar uma regra ao Claude no chat que não está na spec, pare, atualize a spec, registre em `DECISIONS.md` e só depois volte ao código. Cada regra que só existe no chat custa -5. Chegue ao Dia 2 com o sistema base funcionando e testado.

**Passo 7 — Dia 2, 10h: o "envelope".** Uma mudança de requisito chega pelo canal da turma e vale 20 pontos. Ordem obrigatória: `spec.md` → `DECISIONS.md` → novas tasks → código → testes. Anote quantos arquivos você tocou e quanto tempo levou.

**Passo 8 — README, relatório e entrega (Dia 2, tarde).** README do seu projeto (como rodar e testar; se não roda, -3). `docs/RELATORIO.md` organizado pelos 4 Ds, com evidências; a seção de Discernimento exige pelo menos um erro concreto do Claude que você pegou, com link para a sessão. Exporte a última sessão e envie o link do fork até Dia 2, 18h.

| Critério | Pts |
|---|---|
| Qualidade da spec | 25 |
| Rastreabilidade spec → tasks → commits → testes | 25 |
| Mudança do Dia 2 | 20 |
| Relatório | 20 |
| Produto funciona | 10 |

Armadilhas: commits "wip"/"ajustes"; um único commit gigante (-15); spec que só copia a política do RH; ambiguidade decidida só no código; esquecer o `/export`.

---

## Usuário

vamos começar com os dois primeiros passos. Me passa uma lista do que tenho que fazer

## Claude

Esta é a lista para os passos 0 e 1. Nenhum dos dois envolve código.

### Passo 0 — Preparar o terreno

- [ ] Confirmar que o fork é público.
- [ ] Criar a pasta das sessões: `docs/sessions/`.
- [ ] Testar o `/export` agora.
- [ ] Se o `/export` falhar: usar a alternativa do `FAQ.md`.
- [ ] Nomenclatura: `docs/sessions/01-descricao-curta.md`, `02-...`.
- [ ] Ao final de cada sessão: `/export` e commit.
- [ ] Abrir um arquivo de anotações pessoais para o relatório e anotar toda vez que o Claude errar algo (necessário para a seção de Discernimento).

### Passo 1 — Estudar a entrada e caçar ambiguidades

- [ ] Reler as 9 regras da política e os três tipos de ambiguidade da rubrica.
- [ ] Olhar os blocos `colaborador` e `periodo`: quais campos a política pressupõe mas não aparecem?
- [ ] Para cada uma das 14 despesas: qual regra se aplica; o que há de estranho; dá para dizer com certeza quanto reembolsar; qual o tipo da ambiguidade.
- [ ] Olhar também os pares de despesas (mesma data, mesmo fornecedor, mesmo valor).
- [ ] Perguntar, para cada regra, se a ordem de aplicação muda o resultado.
- [ ] Montar a lista de ambiguidades (texto da política, despesas que a expõem, leituras possíveis), com pelo menos 8.
- [ ] Anotar os casos que serão declarados fora de escopo.
- [ ] Ainda não decidir nada — as decisões vêm na `spec.md`.

---

## Usuário

cria a pasta docs/sessions pra mim

## Claude

[Ferramenta: criou `docs/sessions/` com um arquivo `.gitkeep`.]

Criei a pasta `docs/sessions/` com um arquivo vazio `.gitkeep` dentro, porque o Git não registra pastas vazias. Ainda não fiz commit. Estamos trabalhando num worktree, na branch `claude/sdd-desafio-explicacao-873b51`; no fim, isso precisa ser integrado à `main` do fork.

---

## Usuário

pode fazer o commit e em seguida vamos para o segundo passo

## Claude

[Ferramenta: `git commit` → `f79ae27 docs(sessions): cria pasta para exports das sessões`.]

Commit feito. Para o Passo 1, montei uma tabela com as 14 despesas do JSON (ID, data, categoria, valor, NF, descrição) e duas colunas para o usuário preencher: "Regra(s) da política" e "O que é estranho? / Leituras possíveis". Instruções: para cada linha, perguntar se dá para dizer com certeza quanto reembolsar; comparar linhas entre si; classificar o tipo; anotar as leituras sem decidir. Próximo passo: o usuário traz o que encontrou e o Claude revisa o que ficou de fora, o que está duplicado e o que não é ambiguidade.

---

## Usuário

antes de começar com o passo 1, o /export não esta funcionando. Faça uma export no padrão que o desafio pede e salve ele como foi pedido

## Claude

[Ferramenta: gerou este arquivo, `docs/sessions/01-entendimento-do-desafio.md`, seguindo a alternativa do `FAQ.md`.]
