# Sessão 04 — Fase 1 (Fundação): T-001 a T-005

**Data:** 2026-10-14 · **Ferramenta:** Claude Code (app desktop)

> **Formato deste export:** o comando `/export` não funciona neste ambiente
> (ver sessão 01). Seguindo a alternativa do `FAQ.md`, o Claude reproduziu as
> mensagens desta parte da conversa. As mensagens do usuário estão **literais**.
> As respostas do Claude estão **condensadas**: chamadas de ferramenta aparecem
> entre colchetes. Informações pessoais foram omitidas.
> Esta sessão continua a mesma janela de contexto das sessões 01 a 03.

---

## Usuário

pode começar pela T-001

## Claude

[Criou `pom.xml` e `tests/reembolso/ProjetoSmokeTest.java`; rodou `mvn package`: 2 testes passando, `target/motor-reembolso.jar` gerado.]

O projeto Maven ficou com alvo Java 17, `src/` e `tests/` na raiz (DT-003), JUnit 5, Jackson e JAR único (DT-004). O teste de fumaça confere o runtime ≥ 17 e que o Jackson lê `33.333` como `BigDecimal` exato (DT-001).

**Erro do Claude, corrigido antes do commit:** o primeiro rascunho do `pom.xml` incluía uma pasta de recursos de teste (`tests-resources`) que **não estava no plano**. O próprio Claude notou ao revisar e removeu antes do commit.

[Commit `ce35fd0 feat(T-001): projeto Maven com Java 17, src/ e tests/ na raiz, JUnit 5, Jackson e JAR executável`.]

---

## Usuário

pode seguir para a T-002

## Claude

**Erro do Claude:** tentou criar todos os arquivos de uma vez com um único script de shell, e o script falhou ao ser interpretado (`unexpected EOF while looking for matching '`). Nenhum arquivo foi criado; refez pela ferramenta de escrita, um arquivo por vez.

[Criou os records e enums em `src/reembolso/dominio/` (`Entrada`, `Colaborador`, `Periodo`, `Despesa`, `Resultado`, `ResultadoDespesa`, `Grupo`, `Status`, `Motivo`, `Ampliacao`), `src/reembolso/regras/Politica.java` com `Politica.V3` (60/80/250, fator 1,5, piso da NF 100, 3 categorias) e `PoliticaTest` (6 testes). Total de 8 testes passando.]

Ponto levantado para o usuário: o enum `Motivo` já carrega o status que provoca, ou seja, parte da RN-012 ficou embutida no modelo; será testada na T-011.

[Commit `3014e50 feat(T-002): modelo de domínio (records e enums) e Politica.V3 com os valores da política`.]

---

## Usuário

pode seguir para a T-003

## Claude

[Criou `Arredondamento.java` (laço que reduz uma casa por vez com `HALF_UP`, conforme DT-002) e `Rn002ArredondamentoTest` (16 casos).]

**Erro do Claude, detectado antes de rodar:** no teste, escreveu o caso esperado `3,44449 → 3,44`. Pela RN-002 (cadeia), o correto é 3,44449 → 3,4445 → 3,445 → 3,45. O comentário que o próprio Claude escreveu ao lado já mostrava a cadeia chegando a 3,45. Corrigido para 3,45 antes da execução. Exemplo de teste escrito pelo agente testando a coisa errada.

Observação: um arredondamento direto (`setScale(2)`) daria 3,44 para 3,4449, então o teste distingue a regra da spec do comportamento padrão do Java. Ponto levantado: 99,995 → 100,00, ou seja, depois de arredondado o valor passa a exigir NF; a spec já cobre o caso (a RN-006 compara o valor após a RN-002).

[Total de 24 testes passando. Commit `9b1c73f feat(T-003): arredondamento em cadeia para 2 casas (RN-002)`.]

---

## Usuário

pode seguir para a T-004

## Claude

[Criou `Normalizacao.java` (`texto()`: remove acentos e passa para minúsculas, reaproveitável nas RN-005 e RN-008 conforme DT-006; `categoria()`: RN-001) e `Rn001NormalizacaoCategoriaTest` (8 casos, incluindo `ALIMENTACAO` do d-014 e `Alimentação` da D-001).]

Ponto levantado: espaços extras (`" alimentacao "`) não são removidos, porque a RN-001 não fala deles. Se o usuário quiser, a mudança entra antes pela spec (D-002).

[Total de 32 testes passando. Commit `2aefedd feat(T-004): normalização de categoria sem maiúsculas e acentos (RN-001)`.]

---

## Usuário

pode seguir para a T-005

## Claude

[Criou `io/LeitorEntrada.java`, `io/EntradaInvalidaException.java` e `LeitorEntradaTest` (15 testes); ajustou o comentário de `Despesa`.]

O leitor valida o formato da seção 4 (campo ausente ou nulo, tipo errado, data inválida, JSON quebrado, arquivo inexistente), com mensagem apontando o campo (`despesas[0].valor: deve ser número`). Desativou `STRIP_TRAILING_BIGDECIMAL_ZEROES` do Jackson, que transformaria 100.00 em 1E+2. A despesa sai crua (o aceite da T-005 exige 33,333 exato); normalização e arredondamento ficam no passo 1 do motor (seção 8 da spec).

Pontos levantados para decisão do usuário (a spec não cobre): IDs repetidos não são validados; período com `inicio` depois do `fim` não é validado.

[Total de 47 testes passando. Commit `e93ca00 feat(T-005): leitura e validação da entrada JSON com valores decimais exatos`. Fase 1 concluída.]

---

## Usuário

pode exportar a sessão 04 e seguir para a T-006

## Claude

[Gerou este arquivo e fez o commit; em seguida, a T-006.]
