# Sessão 06 — Fase 3 (Limites): T-012 a T-016

**Data:** 2026-10-14 · **Ferramenta:** Claude Code (app desktop)

> **Formato deste export:** o comando `/export` não funciona neste ambiente
> (ver sessão 01). Seguindo a alternativa do `FAQ.md`, o Claude reproduziu as
> mensagens desta parte da conversa. As mensagens do usuário estão **literais**.
> As respostas do Claude estão **condensadas**: chamadas de ferramenta aparecem
> entre colchetes. Informações pessoais foram omitidas.
> Esta sessão continua a mesma janela de contexto das sessões 01 a 05.

---

## Usuário

pode exportar a sessão 05 e seguir para a T-012

## Claude

[Gerou `docs/sessions/05-verificacoes-t006-a-t011.md`; commit `448ad94`.]

[T-012: criou `DiasViagem.java` e `Rn007ViagemTest` (7 casos: d-010 cobre 14 e 15/07, e o 16 não conta; d-013 sem NF ainda gera viagem; sem noites não gera; sem hospedagem não há viagem; 1 noite; virada do mês; hotéis sobrepostos).]

Ponto levantado para o usuário (sem ação): um estorno de hospedagem com noites na descrição também gera dias de viagem, pela letra da spec ("toda hospedagem com noites identificáveis").

[118 testes passando. Commit `20faa4f feat(T-012): dias de viagem a partir das noites de hospedagem (RN-007)`.]

---

## Usuário

pode seguir para a T-013

## Claude

Antes de escrever o código, encontrou uma lacuna: quando o dia é de viagem **e** de fim de semana, o campo `limite_ampliado` só aceita um motivo, e a spec não dizia qual. Perguntou **antes** de implementar:

| Pergunta | Resposta do usuário |
|---|---|
| Qual motivo aparece em `limite_ampliado` quando viagem e fim de semana coincidem? | viagem (Recomendado) |

[Spec 1.3, D-003 e aceite da T-013; commit `eee62cd docs(spec): v1.3 — viagem prevalece no rótulo de ampliação (D-003)`.]

[Criou `AmpliacaoLimite.java`, o método `Politica.limiteDiario(...)` e `Rn008FimDeSemanaPlantaoTest` (12 casos).]

Ponto levantado: a busca por "plantão" é por **conter** "plantao" após a normalização. "plantões" (vira "plantoes") não amplia; uma palavra que contenha "plantao" amplia.

[130 testes passando. Commit `7124b69 feat(T-013): ampliação por fim de semana ou plantão, sem acumular com viagem (RN-008)`.]

---

## Usuário

pode seguir para a T-014

## Claude

[Criou `LimiteDiario.java` e `Rn004LimiteDiarioTest` (13 casos).]

**Erros do Claude, pegos na revisão de nomes e dados de teste antes do commit:**
1. Método chamado `um_centavo_de_real_acima` para uma diferença de R$ 1,00 (d-014 = 61,00).
2. Teste usava `d-006` como aprovada e `d-007` como duplicata, mas no exemplo **as duas** são duplicatas: cenário enganoso.
3. Ao corrigir o item 2, criou uma variável `semNf` para uma despesa **com** NF e motivo `FORA_DO_PERIODO`. Corrigido para `outra`.

[143 testes passando. Commit `1ec0bb2 feat(T-014): grupos diários de alimentação e transporte com limite do dia (RN-004)`.]

---

## Usuário

pode seguir para a T-015

## Claude

[Criou `LimiteHospedagem.java`, o método `Politica.limiteHospedagem(noites)` e `Rn005LimiteHospedagemTest` (10 casos: d-010 480/2 noites → 480; 900/3 → 750; 250,01/1 → 250; sábado com "plantao" → 250/noite; sobrepostas isoladas; d-013 em revisão sem grupo).]

Revisão de nomes e dados de teste: nenhum erro encontrado. Adicionada uma proteção: hospedagem aprovada sem noites gera erro, em vez de inventar um limite.

[153 testes passando. Commit `dd069b5 feat(T-015): grupo de hospedagem com limite de R$ 250 × noites, sem ampliação (RN-005)`.]

---

## Usuário

pode seguir para a T-016

## Claude

Antes de escrever o código, encontrou outra lacuna: dois estornos no mesmo dia que, juntos, excedem as despesas (táxi 50 + −45 + −45). Perguntou **antes** de implementar:

| Pergunta | Resposta do usuário |
|---|---|
| Dois estornos que juntos excedem o dia? | Soma dos estornos (Recomendado): se juntos excedem, todos vão para revisão |

[Spec 1.4, D-004 e aceite da T-016; commit `a3b4255 docs(spec): v1.4 — estornos do mesmo dia avaliados pela soma (D-004)`.]

[Criou `Estornos.java` e `Rn011EstornoTest` (11 casos).]

Evolução do processo registrada: na D-002 o código veio antes da spec; na D-003 e na D-004 a lacuna foi perguntada antes do código.

[164 testes passando. Commit `6ba2e66 feat(T-016): estornos abatem do grupo do dia ou vão para revisão (RN-011)`. Fase 3 concluída.]

---

## Usuário

pode exportar a sessão 06 e seguir para a T-017

## Claude

[Gerou este arquivo e fez o commit; em seguida, a T-017.]
