# Sessão 05 — Fase 2 (Verificações por despesa): T-006 a T-011

**Data:** 2026-10-14 · **Ferramenta:** Claude Code (app desktop)

> **Formato deste export:** o comando `/export` não funciona neste ambiente
> (ver sessão 01). Seguindo a alternativa do `FAQ.md`, o Claude reproduziu as
> mensagens desta parte da conversa. As mensagens do usuário estão **literais**.
> As respostas do Claude estão **condensadas**: chamadas de ferramenta aparecem
> entre colchetes. Informações pessoais foram omitidas.
> Esta sessão continua a mesma janela de contexto das sessões 01 a 04.

---

## Usuário

pode exportar a sessão 04 e seguir para a T-006

## Claude

[Gerou `docs/sessions/04-fundacao-t001-a-t005.md`; commit `f1d9953`.]

[T-006: criou `CategoriaElegivel.java`, `Rn003CategoriaElegivelTest` (9 casos) e o utilitário de teste `tests/reembolso/Despesas.java`.]

**Erro do Claude:** a primeira execução teve 1 erro. O caso de categoria vazia (`""`) gerava um nome de teste em branco, que o JUnit rejeita (`displayName must not be null or blank`). A regra estava certa; o problema era a montagem do teste. Corrigido o nome do caso parametrizado.

[56 testes passando. Commit `54b5976 feat(T-006): categorias elegíveis — fora da política é recusada (RN-003)`.]

---

## Usuário

pode seguir para a T-007

## Claude

[Criou `PeriodoCompetencia.java` e `Rn010PeriodoCompetenciaTest` (8 casos: 01/07, 15/07 e 31/07 dentro; 30/06, 01/08, 15/04 (d-008) e julho/2025 fora; fora do período → em_revisao).]

[64 testes passando na primeira execução. Commit `20ac56b feat(T-007): período de competência inclusivo; fora dele vai para revisão (RN-010)`.]

---

## Usuário

pode seguir para a T-008

## Claude

[Criou `Duplicatas.java` (agrupa por data, categoria normalizada, descrição, fornecedor, valor e NF; marca todas as cópias) e `Rn009DuplicataTest` (8 casos, incluindo diferença em cada campo, um por vez, e `ALIMENTACAO` × `alimentacao`).]

Pontos levantados para o usuário: a descrição é comparada exatamente como foi digitada ("Almoco" ≠ "almoço"), porque só a categoria tem normalização na spec; mudar isso exigiria D-002. O valor é comparado numericamente (54.9 = 54.90).

[72 testes passando. Commit `4e36467 feat(T-008): detecção de duplicatas — todas as cópias vão para revisão (RN-009)`.]

---

## Usuário

pode seguir para a T-009

## Claude

[Criou `NotaFiscal.java` e `Rn006NotaFiscalTest` (13 casos): 100,00 e 100,01 sem NF → revisão; 99,99 → dispensa; hospedagem 690 e 100 sem NF → revisão (AMB-008); táxis de 60 + 50 no mesmo dia → dispensa (AMB-007); estorno → dispensa; 99,995 arredondado → 100,00 → revisão.]

[85 testes passando. Commit `eceef83 feat(T-009): nota fiscal obrigatória a partir de R$ 100,00 por despesa (RN-006)`.]

---

## Usuário

pode seguir para a T-010

## Claude

[Criou `NoitesHospedagem.java` e `Rn005NoitesHospedagemTest` (16 casos). 101 testes passando.]

**Erro do Claude, regra de negócio decidida no código:** ao implementar, o Claude decidiu sozinho dois comportamentos que a RN-005 não cobria: aceitar "2diarias" (sem espaço) e mandar para revisão descrições com mais de uma indicação ("2 noites + 1 diaria"). Percebeu antes do commit, **segurou o código** e perguntou ao usuário:

| Pergunta | Resposta do usuário |
|---|---|
| "Hotel 2diarias" (sem espaço) deve ser aceito? | Aceitar (Recomendado) |
| Descrição com mais de uma indicação de noites? | Vai para revisão (Recomendado) |

[Atualizou a spec para 1.2 (RN-005, AMB-004, seção 7), criou a D-002 em `DECISIONS.md` (registrando que o código veio antes da spec) e ajustou o aceite da T-010. Commits na ordem: `e006f18 docs(spec): v1.2 — noites sem espaço e indicações múltiplas na RN-005 (D-002)` e depois `872f566 feat(T-010): leitura do número de noites na descrição da hospedagem (RN-005)`.]

---

## Usuário

pode seguir para a T-011

## Claude

[Criou `StatusDespesa.java` e `Rn012StatusTest` (10 casos: sem motivos → aprovada; cada motivo de revisão → em_revisao; recusa prevalece; o aceite da spec (coworking de 15/04 → recusada); a ordem não importa).]

**Erro do Claude:** o nome de um teste citava "d-013 fora do período e sem NF", mas o d-013 (22/07) está dentro do período. Corrigido antes do commit.

Padrão observado e relatado ao usuário: os erros do Claude até aqui se concentram em **dados e textos de teste** (3,44449; nome em branco; referência ao d-013), não na lógica das regras.

[111 testes passando. Commit `f60c3da feat(T-011): consolidação do status a partir dos motivos (RN-012)`. Fase 2 concluída.]

---

## Usuário

pode exportar a sessão 05 e seguir para a T-012

## Claude

[Gerou este arquivo e fez o commit; em seguida, a T-012.]
