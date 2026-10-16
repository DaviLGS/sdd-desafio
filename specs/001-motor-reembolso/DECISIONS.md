# Log de Decisões e Mudanças de Spec

> Uma entrada **toda vez** que a spec mudar. Este arquivo é a prova de que a spec
> foi tratada como artefato vivo e não como cerimônia de abertura.
>
> Spec que não muda em dois dias é spec que ninguém consultou. Mudança não é
> demérito — mudança não registrada é.

Ordem cronológica inversa: a mais recente primeiro.

---

## D-006 — Envelope do Dia 2: Política de Reembolso v4 · `2026-10-16`

**Gatilho:** envelope lacrado do Dia 2 (`exemplos/envelope/00-ENVELOPE-LACRADO.md`), com
`politica-v4.json`, `cambio.json` e dois conjuntos de despesas. Vigência imediata, retroativa à
competência atual. Ponto de partida: commit `5f7712c`.

**O que o envelope pede:**
- **A.** Limites por centro de custo, lidos de um arquivo externo (`politica-v4.json`); categoria
  nova `representacao` no `CC-COMERCIAL`; `CC-ENG-PLATAFORMA` não reembolsa hospedagem; CC sem
  entrada usa a política padrão.
- **B.** Campo `moeda` (ISO 4217, ausente = BRL); conversão pela taxa da data da despesa
  (`cambio.json`); limites sempre em BRL.
- **C.** (opcional) Fila de aprovação acima de R$ 500 — **não feito** (ver abaixo).

**O que mudou na spec (1.5 → 2.0):**
- Seção 3 (fora de escopo): saem "só R$" e "sem regras por centro de custo"; entram "sem valores
  de política embutidos" e "sem o item C".
- Seção 4: três arquivos de entrada obrigatórios (despesas, política, câmbio); campo `moeda`;
  saída com `valor` em BRL, campos de câmbio para despesa estrangeira e `resumo.tabela_aplicada`;
  motivo novo `CAMBIO_INDISPONIVEL`.
- RN-002: arredondamento uma vez, sobre o valor já convertido.
- RN-003: elegível = categoria na tabela aplicada com limite > 0.
- RN-004 / RN-005: limites vêm da tabela; a regra (por dia ou por noite) vem da `periodicidade`.
- RN-006: piso vem do arquivo; comparado em BRL.
- RN-007 / RN-008: percentual vem do arquivo; vale para toda categoria por dia; hospedagem
  recusada também gera viagem.
- RN-009: `moeda` entra no critério de duplicata.
- RN-011: estorno estrangeiro é convertido antes de abater.
- **RN-013 (nova):** tabela de limites por centro de custo.
- **RN-014 (nova):** conversão de moeda.
- **Ambiguidades novas AMB-021 a AMB-037** (decisões E1–E19 do responsável, todas na opção
  recomendada): tabela incompleta, categoria nova fora do CC, limite zero, viagem com hospedagem
  recusada, nome do CC, periodicidade, piso e percentual do arquivo, arquivos ausentes, vigência,
  moeda sem cotação, dia sem cotação ("taxa da data da despesa"), data fora do arquivo, ordem
  conversão/arredondamento, NF em moeda estrangeira, moeda na duplicata, estorno estrangeiro, saída.
- Seção 7: 16 casos de borda novos; os antigos passam a declarar a tabela padrão.
- Seção 8: passo 0 (tabela) e conversão no passo 1.
- Seção 9: reescrita para os três arquivos de entrada, calculada à mão antes do código.
- Seção 10: item C e quatro decisões provisórias do Claude a confirmar.

**Por quê:** comunicado do RH (política v4). As decisões das ambiguidades novas estão
justificadas na seção 6 da spec.

**O que isso invalidou:**
- **Resultado do exemplo original:** a colaboradora do `despesas-exemplo.json` é do
  `CC-ENG-PLATAFORMA`. Com a v4, alimentação passa de 60 para 75, e a hospedagem (d-010, d-013)
  deixa de ser reembolsável. Total: **R$ 680,53 → R$ 216,53**; quantidade 6/7/1 → **5/6/3**.
- **Política embutida no código:** `Politica.V3` (T-002) deixa de existir como fonte; a política
  passa a ser lida do arquivo.
- **Lista fixa de categorias** "alimentação e transporte" em RN-004/RN-007/RN-008 (T-013, T-014):
  passa a ser "categorias de periodicidade `dia`".
- **Interface da linha de comando** (T-020): passa a receber os arquivos de política e câmbio.
- **Testes que dependem de valores fixos da v3 ou do resultado antigo:** `PoliticaTest`,
  `MotorReembolsoExemploTest` (680,53), `EscritorSaidaTest` (exemplo da seção 4 ganha
  `tabela_aplicada`), `MainCliTest`, e os testes de regra que usam `Politica.V3`.

**Tasks afetadas:** refeitas — T-002, T-005, T-013, T-014, T-015, T-017, T-019, T-020, T-021,
T-022; novas — T-023 em diante (Fase 6 do `tasks.md`).

**Item C (opcional) não feito:** o reembolso desta spec é por grupo, não por item; especificar
"valor reembolsável do item" exigiria antes desfazer a AMB-002. O envelope diz que deixar a spec
inconsistente para chegar no C custa pontos.

**Custo:** <preencher ao final: arquivos tocados à mão, tempo, `git diff 5f7712c HEAD --stat`>

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
