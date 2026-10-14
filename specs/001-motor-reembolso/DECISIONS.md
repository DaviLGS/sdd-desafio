# Log de Decisões e Mudanças de Spec

> Uma entrada **toda vez** que a spec mudar. Este arquivo é a prova de que a spec
> foi tratada como artefato vivo e não como cerimônia de abertura.
>
> Spec que não muda em dois dias é spec que ninguém consultou. Mudança não é
> demérito — mudança não registrada é.

Ordem cronológica inversa: a mais recente primeiro.

---

## D-005 — Estorno que já tem outro motivo também recebe ESTORNO_SEM_DESPESA · `2026-10-14`

**Gatilho:** T-021 (bateria dos casos de borda da seção 7). O caso "Estorno de hospedagem"
(hospedagem −100,00, `ESTORNO_SEM_DESPESA`) **falhou**: com a descrição "Estorno hotel" (sem número
de noites), o código já mandava o estorno para revisão por `NOITES_NAO_IDENTIFICADAS` no passo 2,
e o passo 5 só avaliava estornos ainda aprovados — o motivo `ESTORNO_SEM_DESPESA` nunca era
acrescentado. A spec dizia "estorno de hospedagem vai **sempre** para revisão
(`ESTORNO_SEM_DESPESA`)" e "a saída lista **todos** os motivos" (RN-012). Spec e código divergiam;
a decisão foi levada ao responsável.

**O que mudou na spec (1.4 → 1.5):**
- RN-011: só estorno aprovado pode ser abatido; estorno já em revisão ou recusado por outro
  motivo também recebe `ESTORNO_SEM_DESPESA`. Exemplo novo no aceite.
- Seção 8, passo 5: esclarecido.
- Seção 7: caso novo "Estorno já em revisão por outro motivo".

**Por quê:** a RN-012 manda listar todos os motivos; o revisor humano precisa ver de uma vez tudo
o que impede o reembolso. O status final não muda (continua `em_revisao` ou `recusada`).

**O que isso invalidou:** o comportamento de `Estornos.aplicar` (T-016), que ignorava estornos já
em revisão. Nenhum teste existente quebra: o resultado do exemplo (seção 9) não muda — d-009 é o
único estorno do exemplo e já estava aprovado ao chegar no passo 5.

**Tasks afetadas:** T-016 (código corrigido; teste novo em `Rn011EstornoTest`) e T-021.

**Custo:** 2 arquivos de código + 2 de teste + 3 de documentação.

---

## D-004 — Vários estornos no mesmo dia e categoria · `2026-10-14`

**Gatilho:** preparação da T-016. A RN-011 comparava **um** estorno com a soma das despesas do
dia, mas não dizia o que fazer com dois estornos que, juntos, excedem as despesas (ex.: táxi de
50 + −45 + −45: cada um cabe, juntos deixariam o dia negativo). Levado ao responsável antes do código.

**O que mudou na spec (1.3 → 1.4):**
- RN-011: os estornos do mesmo dia e categoria são avaliados pela **soma**; se a soma excede as
  despesas aprovadas, **todos** os estornos daquele dia e categoria vão para revisão.
- RN-011: dois exemplos novos no aceite. AMB-019: decisão complementada. Seção 7: caso novo.

**Por quê:** simples e conservador — nunca produz grupo negativo, e não depende da ordem das
despesas no arquivo (a alternativa "um por vez, em ordem" dependeria).

**O que isso invalidou:** nada (nenhum código de estorno existia ainda).

**Tasks afetadas:** T-016 (aceite ampliado).

**Custo:** 3 arquivos de documentação; poucos minutos.

---

## D-003 — Motivo da ampliação quando viagem e fim de semana coincidem · `2026-10-14`

**Gatilho:** preparação da T-013. A RN-008 já dizia que as ampliações não acumulam, mas o campo
de saída `limite_ampliado` aceita um único motivo, e a spec não dizia qual mostrar quando os dois
se aplicam. Desta vez a lacuna foi levada ao responsável **antes** de escrever o código.

**O que mudou na spec (1.2 → 1.3):**
- RN-008: quando o dia é de viagem e também de fim de semana/plantão, `limite_ampliado` = `viagem`.
- AMB-015: decisão complementada com o motivo informado.
- Seção 7: o caso "Sábado em viagem" passou a informar o `limite_ampliado` esperado.

**Por quê:** a viagem vem da política do RH (item 6); fim de semana/plantão é regra adicional
(RN-008). Mostrar a regra da política facilita a auditoria.

**O que isso invalidou:** nada. Só define um rótulo de saída; os valores não mudam.

**Tasks afetadas:** T-013 (aceite inclui o rótulo).

**Custo:** 3 arquivos de documentação; poucos minutos.

---

## D-002 — Leitura de noites: sem espaço e indicações múltiplas · `2026-10-14`

**Gatilho:** implementação da T-010. Ao escrever a leitura do número de noites, o Claude
encontrou dois casos que a RN-005 não decidia e os implementou antes de registrar na spec.
O código foi segurado (sem commit) e as decisões levadas ao responsável antes de seguir.

**O que mudou na spec (1.1 → 1.2):**
- RN-005: "um número inteiro seguido de diária(s) ou noite(s)" passou a valer **com ou sem
  espaço** entre o número e a palavra ("2diarias" = 2 noites).
- RN-005 / AMB-004: descrição com **mais de uma indicação** de noites ("2 noites + 1 diaria")
  passou a ser não identificável → `em_revisao`, `NOITES_NAO_IDENTIFICADAS`.
- Seção 7: dois casos de borda novos.

**Por quê:** "2diarias" é erro de digitação comum e continua inequívoco. Com duas indicações,
qualquer escolha automática (primeira, soma) seria um palpite; a spec já manda para revisão o
que não pode ser identificado.

**O que isso invalidou:** nada. Nenhum teste existente mudou; o resultado do exemplo (seção 9)
não muda.

**Tasks afetadas:** T-010 (o aceite passa a incluir os dois casos; já coberto em
`Rn005NoitesHospedagemTest`).

**Custo:** 2 arquivos de spec; poucos minutos.

---

## D-001 — Confirmação das decisões provisórias e categoria com acento · `2026-10-14`

**Gatilho:** revisão da spec 1.0 pelo responsável. A seção 10 listava decisões
provisórias redigidas pelo Claude que ainda não tinham sido confirmadas.

**O que mudou na spec (1.0 → 1.1):**
- **RN-001 / AMB-020:** a categoria passou a ser comparada ignorando também acentos.
  De "`alimentação` cai em `CATEGORIA_FORA_DA_POLITICA`" para "`alimentação` = `alimentacao`".
  Esta foi a única decisão provisória **alterada** pelo responsável.
- Decisões provisórias **confirmadas** e movidas da seção 10 para as regras:
  - RN-002: valor negativo é arredondado pelo valor absoluto, mantendo o sinal.
  - RN-004: despesa de valor zero é aprovada normalmente.
  - RN-005: hospedagens sobrepostas são limitadas isoladamente.
  - RN-007: hospedagem em revisão conta como viagem, se o número de noites for identificável.
  - RN-011: o estorno também abate em alimentação; estorno de hospedagem vai para revisão.
- Casos de borda adicionados: categoria com acento e estorno de hospedagem.
- Seção 10: resta em aberto apenas a validação da RN-008 com o RH.

**Por quê:** decisões provisórias não confirmadas são regra de negócio que não foi
decidida pelo responsável. Aceitar categoria com acento segue a justificativa já
registrada na AMB-020: diferença de digitação não muda a natureza do gasto.

**O que isso invalidou:** nada implementado (ainda não há código nem testes).
O resultado esperado do exemplo (seção 9, R$ 680,53) não muda.

**Tasks afetadas:** nenhuma (o `tasks.md` ainda não foi escrito).

**Custo:** 2 arquivos (`spec.md`, `DECISIONS.md`), poucos minutos.
