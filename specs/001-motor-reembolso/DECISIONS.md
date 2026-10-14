# Log de Decisões e Mudanças de Spec

> Uma entrada **toda vez** que a spec mudar. Este arquivo é a prova de que a spec
> foi tratada como artefato vivo e não como cerimônia de abertura.
>
> Spec que não muda em dois dias é spec que ninguém consultou. Mudança não é
> demérito — mudança não registrada é.

Ordem cronológica inversa: a mais recente primeiro.

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
