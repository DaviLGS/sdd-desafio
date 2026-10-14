# Sessão 03 — Plano técnico, tasks e CLAUDE.md

**Data:** 2026-10-14 · **Ferramenta:** Claude Code (app desktop)

> **Formato deste export:** o comando `/export` não funciona neste ambiente
> (ver sessão 01). Seguindo a alternativa do `FAQ.md`, o Claude reproduziu as
> mensagens desta parte da conversa. As mensagens do usuário estão **literais**.
> As respostas do Claude estão **condensadas**: chamadas de ferramenta aparecem
> entre colchetes. Informações pessoais foram omitidas.
> Esta sessão continua a mesma janela de contexto das sessões 01 e 02.

---

## Usuário

a linguagem que tenho mais facilidade é java. Caso não possa utiliza-la, podemos usar python

## Claude

[Ferramenta: verificou o ambiente: JDK 25, Maven 3.9.12, Python 3.14; Gradle não instalado.]

Confirmou que Java funciona e escreveu `specs/001-motor-reembolso/plan.md`:

- **Stack:** Java com alvo `--release 17` (roda em qualquer JDK ≥ 17), Maven, JUnit 5, Jackson, `BigDecimal` para dinheiro. Python e Gradle foram registrados como alternativas descartadas.
- **Arquitetura:** `cli` → `io` (leitura e validação) → `regras` (puras, sem I/O) → `io` (escrita). Os valores da política ficam num único record, `Politica.V3`.
- **Decisões técnicas:**
  - DT-001: JSON lido direto como `BigDecimal`. Se passar por `double`, 33,333 vira 33,33299…
  - DT-002: arredondamento em cadeia feito em laço com `HALF_UP`, porque `setScale(2)` direto daria 3,44 para 3,4449.
  - DT-003: `src/` e `tests/` na raiz.
  - DT-004: JAR único.
  - DT-005: códigos de saída 0/1/2.
  - DT-006: normalização de acentos.
- **Testes:** uma classe por RN (`Rn006NotaFiscalTest`), `@DisplayName` citando RN e AMB, teste de integração com o exemplo dando R$ 680,53.

[Commit `5638237 docs(plan): plano técnico v1.0 — Java 17 + Maven, BigDecimal, regras puras separadas de I/O`.]

Pediu revisão do plano e perguntou se podia seguir com o `tasks.md`.

---

## Usuário

pode seguir com o tasks.md

## Claude

[Escreveu `specs/001-motor-reembolso/tasks.md`; commit `ebac0a3 docs(tasks): T-001 a T-022 com requisitos atendidos, critérios de aceite e matriz de cobertura`.]

22 tasks em 5 fases, mais uma fase vazia para o envelope:

- **Fundação (T-001 a T-005):** projeto Maven, domínio e `Politica`, arredondamento, normalização de categoria, leitura do JSON.
- **Verificações por despesa (T-006 a T-011):** categoria, período, duplicata, NF, noites, status.
- **Limites (T-012 a T-016):** viagem, fim de semana/plantão, limite diário, hospedagem, estorno.
- **Saída e CLI (T-017 a T-020):** motor com o exemplo dando 680,53, justificativa, JSON de saída, CLI.
- **Casos de borda e README (T-021 e T-022).**

Cada task cita as RN e AMB que atende e tem como aceite um teste nomeado. A matriz de cobertura no fim liga cada RN e AMB a uma task e a um teste.

Sugeriu, como próximos passos, preencher o `CLAUDE.md` e exportar a sessão 03 antes de começar a codificar.

---

## Usuário

pode preencher o CLAUDE.md e exportar a sessão 03

## Claude

[Preencheu `CLAUDE.md`:
- stack e comandos (`mvn package`, `mvn test`, `java -jar target/motor-reembolso.jar calcular ...`);
- convenções: pacotes `cli`, `io`, `dominio` e `regras`; política só em `Politica`; dinheiro sempre em `BigDecimal`; erros e códigos de saída; nomes de teste por RN; marcar `[x]` nas tasks;
- instrução de export de sessão, já que o `/export` não funciona;
- resumo do fora de escopo.

Commit `338fe1e docs(claude): preenche stack, comandos, convenções e fora de escopo no CLAUDE.md`. Gerou este arquivo e fez o commit.]
