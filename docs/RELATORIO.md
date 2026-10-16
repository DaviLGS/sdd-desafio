# Relatório — Desafio SDD

**Aluno:** `<nome>` · **Repositório:** https://github.com/DaviLGS/sdd-desafio · **Data:** `<data da entrega>`

> **Status deste arquivo: RASCUNHO.** As evidências (hashes, arquivos, sessões, números) foram
> levantadas pelo Claude a partir do repositório. Os trechos marcados com ✍️ **[VOCÊ]** pedem a
> **minha** experiência — só eu posso escrevê-los. Antes de entregar: preencher todos os ✍️,
> reler cada afirmação contra o repositório e apagar esta nota.

---

## Notas sobre as evidências

- **Sessões:** o `/export` não funcionou no meu ambiente (Claude Code no app desktop). Segui a
  alternativa do `FAQ.md`: ao fim de cada bloco de trabalho, pedi ao Claude que reproduzisse a
  conversa. Os arquivos em [`docs/sessions/`](sessions) têm **minhas mensagens literais** e as
  respostas do Claude **condensadas** (tabelas resumidas, chamadas de ferramenta entre colchetes).
  São reconstruções, não cópias byte a byte — o cabeçalho de cada arquivo diz isso.
- **Cronologia** (`git log --date=iso`): estrutura inicial em 09/10; sessão de trabalho em 14/10,
  das 16h06 às 19h14 — spec às 18h15, plano às 18h25, tasks às 18h28, primeiro código (T-001)
  às 18h32. Envelope em 16/10, das 18h38 às 19h12 — spec 2.0 às 18h42, plano às 18h57, tasks às
  18h58, primeiro código (T-023) às 19h01. A ordem spec → plan → tasks → código está nos timestamps,
  nos dois dias.
- **Números** (até `fff1e97`): 69 commits na `main`; 33 tasks (22 do sistema base + 11 do envelope);
  344 testes, todos passando; spec da v1.0 à v2.1; 7 mudanças de spec registradas (D-001 a D-007);
  10 sessões exportadas.

---

## Delegação

*O que eu fiz, o que o Claude fez, e por que dividi assim.*

**A divisão:**

| Atividade | Quem | Evidência |
|---|---|---|
| Identificar ambiguidades | **Claude** levantou 15 candidatas (A1–A15) varrendo o JSON de exemplo contra a política; depois apontou mais lacunas (e uma contradição nas minhas respostas) em perguntas de esclarecimento | [sessão 02](sessions/02-ambiguidades-e-spec.md) |
| Decidir as ambiguidades | **Eu.** Ditei as regras de 1 a 7 e respondi Q1–Q15 e as rodadas de múltipla escolha | [sessão 02](sessions/02-ambiguidades-e-spec.md), respostas literais |
| Escrever a spec | **Claude** redigiu a partir das minhas respostas; eu revisei e corrigi uma decisão provisória (D-001) | `f66e073`, `7ad4776` |
| Desenhar a arquitetura | **Claude** propôs; eu escolhi a linguagem (Java) e aprovei o plano | `5638237`, [sessão 03](sessions/03-plano-e-tasks.md) |
| Implementar | **Claude**, uma task por commit (T-001 a T-022) | `ce35fd0` … `5e5bc96` |
| Escrever testes | **Claude**, junto com cada task | `tests/` |
| Decidir lacunas achadas durante a implementação | **Eu** (D-002 a D-005), sempre perguntado pelo Claude | [DECISIONS.md](../specs/001-motor-reembolso/DECISIONS.md) |
| Absorver o envelope | **Eu** decidi as 19 ambiguidades novas (E1–E19) e confirmei 4 decisões do Claude (D-007); **Claude** redigiu spec 2.0, plano e tasks e implementou T-023 a T-033 | [sessão 10](sessions/10-envelope-politica-v4.md), `149f189` … `738f1e6` |

✍️ **[VOCÊ] Por que dividi assim:** *(ex.: por que deleguei o levantamento das ambiguidades em
vez de fazer sozinho primeiro, como o Claude chegou a sugerir na sessão 01)*

✍️ **[VOCÊ] Onde deleguei e me arrependi:**

✍️ **[VOCÊ] Onde não deleguei e deveria ter delegado:**

**Usei subagentes / skills / MCP / hooks?** Não configurei subagentes, skills próprias, MCP nem
hooks. Recursos do Claude Code que usei:
- **Worktree:** todo o trabalho foi feito numa branch isolada
  (`claude/sdd-desafio-explicacao-873b51`) e levado para a `main` por fast-forward ao final
  (`bc3d841..c7b408f`).
- **Perguntas de múltipla escolha:** o Claude apresentou as lacunas em formato de opções, com uma
  recomendada. Foi assim que decidi D-002 a D-005 sem precisar redigir as regras do zero.
- **`CLAUDE.md`:** as convenções (política só em `Politica.java`, dinheiro só em `BigDecimal`,
  testes nomeados por RN, regra de parar quando uma regra não está na spec) foram seguidas em
  todas as tasks.

✍️ **[VOCÊ] Valeu a pena?**

---

## Descrição

*Como um requisito ambíguo virou requisito verificável.*

**Requisito escolhido:** estornos (valores negativos). A política do RH não fala deles; o
`despesas-exemplo.json` tem um (d-009, −R$ 45,00, "Estorno de corrida cancelada").

**Versão 1 (spec 1.0, commit `f66e073`):**
> ```
> RN-011 — Estorno (valor negativo)
> Regra: uma despesa com valor negativo é um estorno. Ele abate do grupo da mesma categoria
> no mesmo dia (RN-004), entrando na soma do grupo antes da aplicação do limite. Se no mesmo
> dia e categoria não houver despesas aprovadas cuja soma seja maior ou igual ao valor
> absoluto do estorno, o estorno fica em_revisao com motivo ESTORNO_SEM_DESPESA e não entra
> em nenhum grupo.
> ```

**Versão final (spec 1.5, commit `d335001`):**
> ```
> RN-011 — Estorno (valor negativo)
> Regra: uma despesa com valor negativo é um estorno. Em alimentação e transporte urbano, ele
> abate do grupo da mesma categoria no mesmo dia (RN-004), entrando na soma do grupo antes da
> aplicação do limite. Estorno de hospedagem vai sempre para revisão (ESTORNO_SEM_DESPESA),
> pois hospedagem não é agrupada por dia. Os estornos de um mesmo dia e categoria são
> avaliados em conjunto: se a soma das despesas aprovadas (não negativas) desse dia e
> categoria for menor que o valor absoluto da soma dos estornos, todos os estornos daquele
> dia e categoria ficam em_revisao com motivo ESTORNO_SEM_DESPESA e não entram em nenhum grupo.
>
> Estorno que já tem outro motivo: só um estorno aprovado pelas demais verificações pode ser
> abatido. Um estorno que já está em revisão ou recusado por outro motivo também recebe
> ESTORNO_SEM_DESPESA, porque não será abatido de nenhum grupo; a saída lista os dois
> motivos (RN-012).
> ```

**O que estava ambíguo, camada por camada:**

| Versão | Pergunta que a versão anterior não respondia | Como apareceu | Commit |
|---|---|---|---|
| 1.0 | A política nem menciona estorno: ignorar, abater do dia, do mês, ou rejeitar? | Varredura do JSON (d-009); eu decidi "abate do dia" (Q12) e "dia negativo → revisão" | `f66e073` |
| 1.1 | E estorno de alimentação ou de hospedagem? A decisão tinha sido dada só para transporte | O Claude listou como decisão provisória; eu confirmei | `7ad4776` (D-001) |
| 1.4 | Dois estornos no mesmo dia que, **juntos**, excedem o dia (50 − 45 − 45)? Cada um sozinho "cabia" | O Claude percebeu ao preparar a T-016 e perguntou **antes** do código | `a3b4255` (D-004) |
| 1.5 | Estorno que **já** está em revisão por outro motivo recebe também `ESTORNO_SEM_DESPESA`? | **Um teste falhou**: a bateria de casos de borda (T-021) mostrou que o código divergia da spec | `d335001` (D-005) |

**Como percebi:** cada camada apareceu de um jeito diferente — os dados de exemplo (1.0), a
revisão das decisões provisórias (1.1), a preparação de uma task (1.4) e um teste de ponta a
ponta (1.5). A 1.5 é a mais instrutiva: a regra **parecia** completa até que um caso real
combinou duas regras (RN-005 sem noites + RN-011 estorno).

✍️ **[VOCÊ]** *(acrescente o que você sentiu/percebeu — ex.: se teria pensado nos dois estornos sozinho)*

---

## Discernimento

*Onde o Claude errou e alguém pegou.*

> **Nota honesta sobre quem detectou:** na maior parte dos casos abaixo, **foi o próprio Claude**
> que encontrou o erro (revisando antes do commit, rodando testes ou o JAR) e me reportou. O Caso 1
> é o único em que **eu** discordei de algo que ele propôs. ✍️ **[VOCÊ]**: antes de entregar, revise
> o código/testes por conta própria e, se achar algo, acrescente aqui — é a evidência mais forte.

### Caso 1 — decisão provisória rejeitada por mim (categoria com acento)

**O que ele propôs:** ao redigir a spec 1.0, o Claude tomou sete decisões que eu não tinha
dado e as listou como "provisórias" na seção 10. A nº 4 dizia que `alimentação` (com acento)
**não** seria reconhecida e cairia em `CATEGORIA_FORA_DA_POLITICA`.

**Por que estava errado:** contradizia a justificativa que ele mesmo escreveu na AMB-020
("diferença de digitação não muda a natureza do gasto"): recusar por causa de um acento é tratar
digitação como categoria.

**Como eu detectei:** lendo a lista de decisões provisórias que ele apresentou para confirmação.

**O que eu fiz:** aprovei 1 a 7 **exceto** a 4: "aceite alimentação com acento". Virou a spec 1.1
e a D-001.

**Onde está a evidência:** [sessão 02](sessions/02-ambiguidades-e-spec.md), última mensagem do
usuário; commit `7ad4776`; [DECISIONS.md](../specs/001-motor-reembolso/DECISIONS.md), D-001.

### Caso 2 — regra de negócio decidida dentro do código (D-002)

**O que ele propôs:** na T-010 (ler o número de noites da hospedagem), o Claude implementou
duas regras que a spec não tinha: aceitar "2diarias" sem espaço e mandar para revisão
"2 noites + 1 diaria".

**Por que estava errado:** viola a regra 2 do desafio e o `CLAUDE.md` — regra de negócio
decidida fora da spec. Ele mesmo havia escrito essa regra no `CLAUDE.md`.

**Como foi detectado:** pelo próprio Claude, revisando antes do commit; ele segurou o código e
me perguntou.

**O que eu fiz:** decidi as duas regras; spec 1.2 + D-002 commitadas **antes** do código.

**Onde está a evidência:** [sessão 05](sessions/05-verificacoes-t006-a-t011.md), bloco da T-010;
ordem dos commits `e006f18` (spec) → `872f566` (código); o texto da D-002 registra que o código
veio primeiro.

### Caso 3 — bug de interação entre regras, achado por teste de ponta a ponta (D-005)

**O que ele propôs:** na T-016, `Estornos.aplicar` só avaliava estornos ainda aprovados.

**Por que estava errado:** um estorno de hospedagem sem noites na descrição ia para revisão só
por `NOITES_NAO_IDENTIFICADAS`; a spec dizia que estorno de hospedagem recebe **sempre**
`ESTORNO_SEM_DESPESA` e que a saída lista **todos** os motivos. Os 11 testes unitários da T-016
passavam — testavam a regra isolada, com estornos já aprovados.

**Como foi detectado:** a bateria de casos de borda (T-021), que roda cada linha da seção 7 da
spec pelo motor completo, falhou no caso "Estorno de hospedagem".

**O que eu fiz:** decidi "listar todos os motivos"; spec 1.5 + D-005 → correção `fix(T-016)`.

**Onde está a evidência:** [sessão 08](sessions/08-casos-de-borda-e-readme-t021-t022.md);
commits `d335001` → `4a774a8` → `8493181`.

### Padrão que eu notei

Os erros do Claude se concentraram em **dados e textos de teste**, não na lógica das regras:

| Onde | Erro | Evidência |
|---|---|---|
| T-003 | Caso esperado `3,44449 → 3,44`; pela regra em cadeia é 3,45 | [sessão 04](sessions/04-fundacao-t001-a-t005.md) |
| T-011 | Nome de teste dizia "d-013 fora do período"; d-013 (22/07) está dentro | [sessão 05](sessions/05-verificacoes-t006-a-t011.md) |
| T-014 | Método `um_centavo_de_real_acima` para diferença de R$ 1,00; IDs d-006/d-007 sugerindo cenário inexistente; variável `semNf` para despesa **com** NF (erro introduzido na própria correção) | [sessão 06](sessions/06-limites-t012-a-t016.md) |
| T-018, T-020 | Escape de barra invertida errado em scripts de edição — gerou `"\p{M}"`, que não compila | [sessão 07](sessions/07-motor-saida-cli-t017-a-t020.md) |
| T-020 | Mensagens de console com `→` e acentos corrompidos no Windows — **os testes passavam**; só a execução real do JAR revelou | [sessão 07](sessions/07-motor-saida-cli-t017-a-t020.md) |

Sinal que passou a me deixar em alerta: ✍️ **[VOCÊ]**

---

## Diligência

*O que eu verifiquei antes de aceitar.*

**O que o processo verificou a cada task (feito pelo Claude, visível nas sessões):**
- `mvn test` antes de todo commit; nenhum commit de código com teste falhando.
- Revisão dos nomes e dados de teste antes do commit (a partir da T-011, depois de notar o padrão acima).
- A tabela da seção 9 da spec foi **calculada à mão antes do código**; o motor bateu na primeira
  execução (R$ 680,53; 6/7/1) — `96e3a1a`.
- O JSON de saída da seção 4 da spec foi copiado para um teste e comparado campo a campo — `04f2c39`.
- Execução real do JAR (T-020) e clone limpo seguindo só o README (T-022).
- Um teste que **lê a spec** e falha se alguma linha da seção 7 ficar sem teste — `8493181`.

✍️ **[VOCÊ] Meu procedimento de verificação:** *(o que **você** de fato fazia — não o que o
Claude fazia. Seja honesto: na sessão, a maioria das minhas mensagens foi "pode seguir para a
T-0NN". Os commits das tasks T-001 a T-022 têm intervalos de 1 a 4 minutos — `git log`
mostra isso.)*

✍️ **[VOCÊ] Li o diff inteiro em que porcentagem das entregas?**

✍️ **[VOCÊ] O que aceitei sem verificar direito, e o que me custou:** *(candidatos: os textos
das justificativas e a redação das AMB foram escritos pelo Claude; a interpretação de
"plantão" como "contém a palavra" foi dele)*

**Testes: quem escreveu, e como sei que testam a coisa certa?** Todos os testes foram escritos
pelo Claude — o mesmo agente que escreveu o código. O que reduz esse risco:
- Os **valores esperados vêm da spec**, não do código: a seção 9 foi calculada antes do código existir.
- O teste de arredondamento falharia com a implementação ingênua (`setScale(2)` dá 3,44 para 3,4449).
- A bateria da T-021 achou um bug que os testes unitários não pegavam (Caso 3).

O que **não** reduz: ✍️ **[VOCÊ]** *(ex.: eu não escrevi nenhum caso de teste próprio)*

---

## O envelope

*A mudança de requisito do Dia 2: Política de Reembolso v4* ([envelope](../exemplos/envelope/00-ENVELOPE-LACRADO.md),
[sessão 10](sessions/10-envelope-politica-v4.md), [D-006](../specs/001-motor-reembolso/DECISIONS.md)).

**O que chegou:** **A.** limites por centro de custo, lidos de um arquivo externo, com categoria nova
(`representacao`) e um CC sem hospedagem; **B.** despesas em moeda estrangeira, convertidas pela taxa da
data; **C.** (opcional) fila de aprovação acima de R$ 500 — **não feito**, por decisão minha: o
reembolso da spec é por grupo, não por item, e especificar o C exigiria desfazer a AMB-002 (spec 2.1,
seção 10).

**Hash antes do envelope:** `5f7712c` (último commit antes do envelope; a absorção vai de `909641d` a `738f1e6`).

**Quantos arquivos toquei na mão:** **5** — os arquivos do envelope, que baixei e coloquei em
`exemplos/envelope/`. Nenhum arquivo de spec, plano, código ou teste foi editado por mim: as 19 decisões
(E1–E19) e a confirmação da D-007 eu dei no chat, e o Claude as escreveu na spec.
✍️ **[VOCÊ]** *confirme — se você editou algo à mão, corrija este número.*

**Quanto tempo levou:**
- Do commit dos arquivos do envelope (`909641d`, 18h38 de 16/10) ao último commit da absorção
  (`738f1e6`, 19h12): **34 minutos**.
- Antes disso houve a análise de impacto e as minhas 19 decisões, que não aparecem nos commits.
  ✍️ **[VOCÊ]** *horário em que o envelope chegou / em que você começou: `<hh:mm>` → tempo total `<...>`.*

**Diff de absorção** (`git diff 5f7712c ed7197a --stat`, até o último commit de código/docs; o `738f1e6` só acrescenta este custo à D-006): **54 arquivos, +2.874/−483 linhas**, em 16 commits.

| Área | Arquivos | Linhas |
|---|---|---|
| `src/` | 24 (15 alterados, 9 novos) | +644 / −172 |
| `tests/` | 19 (14 alterados, 5 novos) | +1.283 / −178 |
| `specs/` (spec, plano, tasks, DECISIONS) | 4 | +600 / −101 |
| `exemplos/envelope/` | 5 | +285 |
| README e `CLAUDE.md` | 2 | +62 / −32 |

Suíte de testes: **237 → 344**, todos passando.

**Quanto veio de reexecutar tasks a partir da spec vs. edição manual:**
- **Tasks reexecutadas a partir da spec atualizada:** 11 tasks novas (T-023 a T-033), uma por commit,
  cada uma citando as RN/AMB que atende. Todo o código e todos os testes (43 arquivos) vieram delas.
- **Edição manual minha:** 5 arquivos (os do envelope), mais as 19 decisões e 4 confirmações dadas no chat.
- **Tasks antigas afetadas** (D-006): T-002, T-005, T-013, T-014, T-015, T-017, T-019, T-020, T-021 e
  T-022 — refeitas *dentro* das tasks novas, sem reabrir as antigas.

**Absorveu de graça:**
- **15 dos 30 arquivos de `src/` não mudaram**: `Arredondamento`, `Normalizacao`, `PeriodoCompetencia`,
  `StatusDespesa`, `DiasViagem`, `NoitesHospedagem`, `AmpliacaoLimite` e 8 records/enums do domínio.
- O motor **já recebia a `Politica` como parâmetro**. O plano v1 (seção 4) tinha descartado o arquivo
  externo com a ressalva *"se o Dia 2 pedir políticas variáveis em tempo de execução, isso é revisto"* —
  foi exatamente o que aconteceu, e as regras não mudaram de assinatura: só mudou de onde vêm os números.
- As regras eram funções puras sem I/O, então política e câmbio entraram como dados, com teste sem arquivo
  (`Rn013TabelaCentroCustoTest`, `Rn014ConversaoMoedaTest`).
- A seção 9 calculada à mão: recalculá-la para os três arquivos **antes** do código deu o alvo exato, e o
  motor bateu na primeira execução (216,53 / 1.993,26 / 373,76; commit `362c2cd`).

**Resistiu:**
- **A lista fixa "alimentação e transporte"**, espalhada em `LimiteDiario`, `Politica` e `Estornos`. Com
  uma categoria nova (`representacao`), virou "categorias de periodicidade `dia`" (T-023, AMB-026).
- **A `Despesa`**, que só conhecia um valor em R$: ganhou `moeda`, `valorOriginal`, `taxaCambio` e
  `dataCotacao`, e `valor` passou a poder ser `null` (T-026, DT-010). Isso obrigou a ajustar
  `NotaFiscal`, `Duplicatas` e `Estornos` (T-028).
- **A constante `Politica.V3`**, usada pela CLI: não deu para removê-la na T-023 como planejado; a remoção
  foi adiada para a T-031 (registrado no `tasks.md`).
- **O teste de rastreabilidade da seção 7** ficou vermelho de `149f189` até `f5fa575`, apontando os 16
  casos de borda novos sem teste. Foi uma quebra intencional e registrada, não desligada.

**Ordem em que fiz** (os timestamps estão no `git log`):

| Ordem | Commit | O quê |
|---|---|---|
| 1 | `909641d` | Arquivos do envelope |
| 2 | `149f189` | Spec 2.0 + D-006 (inclusive a seção 9 recalculada à mão) |
| 3 | `55b592b` | Spec 2.1 + D-007 (confirmação das decisões provisórias) |
| 4 | `ae2c86c` | Plano 2.0 (DT-007 a DT-010) |
| 5 | `9229702` | Tasks: Fase 6, T-023 a T-033 |
| 6 | `45b7391` … `ed7197a` | Código, uma task por commit |
| 7 | `738f1e6` | Custo registrado na D-006 |

Spec → DECISIONS → tasks → código, sem exceção.

**Se eu tivesse escrito a spec original sabendo desta mudança:**
✍️ **[VOCÊ]** *revise e reescreva com as suas palavras; os pontos abaixo são o que a absorção mostrou:*
- Teria escrito as regras de limite em função de **"limite por dia" e "limite por noite"**, e não de
  "alimentação e transporte" — foi a principal resistência.
- Teria deixado **os números da política fora da spec e do código** desde a v1 (ex.: "o limite de cada
  categoria vem da tabela vigente"), com a v3 como um exemplo de tabela.
- Teria separado **valor informado** de **valor considerado** desde o início: a v1 já arredondava na
  entrada (RN-002), e a conversão de moeda é o mesmo tipo de transformação.

**O que a spec me poupou, em concreto:**
- **O alvo antes do código:** a seção 9 recalculada à mão para os três arquivos virou o teste de
  integração da T-029, e o motor acertou de primeira.
- **O mesmo processo de ambiguidades:** o envelope avisava que "traz várias"; com o formato
  AMB/decisão/justificativa já pronto, as 19 novas (AMB-021 a AMB-037) foram levantadas e decididas antes
  do código — inclusive as duas que o envelope apontava ("aplica-se a política padrão" → AMB-021;
  "taxa da data da despesa" → AMB-031).
- **A rede de segurança da rastreabilidade:** o teste que lê a seção 7 da spec acusou sozinho os 16 casos
  novos ainda sem teste.
- **O mapa do que quebrar:** a D-006 listou antes do código o que seria invalidado (total 680,53,
  `Politica.V3`, lista fixa de categorias, CLI e os testes dependentes), e foi exatamente isso que as
  tasks ajustaram.

---

## Fechamento

✍️ **[VOCÊ]** — estas respostas são inteiramente suas.

**Para qual tamanho de projeto isto valeu a pena?**

**Para qual não valeria?**

**O que eu faria diferente:** *(sugestão de ponto a considerar: o Claude recomendou na sessão 01
que eu fizesse a varredura de ambiguidades sozinho primeiro; eu pedi para ele seguir)*

**A coisa mais desconfortável que aprendi sobre como eu trabalho com IA:**
