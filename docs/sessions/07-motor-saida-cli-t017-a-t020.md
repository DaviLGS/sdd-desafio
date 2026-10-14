# Sessão 07 — Fase 4 (Motor, saída e CLI): T-017 a T-020

**Data:** 2026-10-14 · **Ferramenta:** Claude Code (app desktop)

> **Formato deste export:** o comando `/export` não funciona neste ambiente
> (ver sessão 01). Seguindo a alternativa do `FAQ.md`, o Claude reproduziu as
> mensagens desta parte da conversa. As mensagens do usuário estão **literais**.
> As respostas do Claude estão **condensadas**: chamadas de ferramenta aparecem
> entre colchetes. Informações pessoais foram omitidas.
> Esta sessão continua a mesma janela de contexto das sessões 01 a 06.

---

## Usuário

pode exportar a sessão 06 e seguir para a T-017

## Claude

[Gerou `docs/sessions/06-limites-t012-a-t016.md`; commit `8b8f267`.]

[T-017: criou `MotorReembolso.java`, que executa os passos 1 a 7 da seção 8 da spec com comentários citando passo e RN, e `MotorReembolsoExemploTest` (20 testes).]

**Resultado:** o arquivo de exemplo deu **na primeira execução** exatamente a tabela da seção 9 da spec, calculada à mão antes de existir código: R$ 680,53; 6 aprovadas, 7 em revisão, 1 recusada; status e motivos das 14 despesas e os 5 grupos campo a campo. Evidência de que a spec era precisa o bastante para que cálculo manual e código chegassem ao mesmo resultado.

[184 testes passando. Commit `96e3a1a feat(T-017): MotorReembolso orquestra as regras na ordem da seção 8; exemplo dá R$ 680,53`.]

---

## Usuário

pode seguir para a T-018

## Claude

[Criou `Justificativa.java` (aprovada: texto idêntico ao exemplo da seção 4 da spec; revisão ou recusa: prefixo + texto de cada motivo citando a RN; o piso da NF vem da `Politica`), ligou-a ao motor e criou `JustificativaTest` (6 testes).]

Ponto levantado: os textos dos motivos foram redigidos pelo Claude (a spec só define o texto da aprovada). São apresentação, não regra de negócio, mas o usuário deve revisar a redação.

**Erros do Claude:** escreveu uma asserção com dupla negação (`assertFalse(!...)`), trocada por `assertTrue`. A primeira tentativa de correção via script falhou no escape das barras invertidas; o script parou antes de alterar o arquivo e a correção foi refeita com a ferramenta de edição.

[190 testes passando. Commit `047a6fc feat(T-018): justificativa textual de cada despesa, citando grupo ou motivos com a RN`.]

---

## Usuário

siga para a t-019

## Claude

[Conferiu que a árvore de trabalho estava limpa (os avisos de "arquivo alterado" eram conversão de fim de linha). Criou `io/EscritorSaida.java` e `EscritorSaidaTest` (7 testes).]

O JSON de saída do exemplo da seção 4 da spec foi copiado para o teste e comparado com o gerado: idêntico campo a campo, inclusive a justificativa. Valores sempre com 2 casas (`60.00`, `-45.00`), status em minúsculas, motivos com os códigos da spec, `noites` só em hospedagem, acentos preservados no arquivo.

[197 testes passando. Commit `04f2c39 feat(T-019): escrita da saída JSON no schema da seção 4, valores com 2 casas`.]

---

## Usuário

pode seguir para a T-020

## Claude

[Criou `cli/Main.java` e `MainCliTest`; rodou `mvn package` e o JAR real.]

**Problemas que só a execução real do JAR revelou (os testes passavam):**
1. A seta `→` da mensagem de sucesso saiu como `?` no console do Windows. Trocada por texto ASCII.
2. Ao testar entrada inválida no JAR real, os acentos saíram corrompidos (`entrada inv�lida: campo obrigat�rio ausente`). Solução: todas as mensagens de **console** saem sem acentos; o arquivo JSON continua em UTF-8 com acentos. Teste novo garante mensagens só em ASCII.

**Erro do Claude:** ao aplicar a correção via script, repetiu o erro de escape da T-018 (gerou `"\p{M}"`, que não compila) e deixou um caractere `�` num comentário. Corrigido com a ferramenta de edição.

Decisão técnica nova: código de saída **3** para falha ao gravar a saída (sem ele, a falha teria de usar o `1`, que significa "entrada inválida"). Registrada no `plan.md` v1.1 (DT-005) antes do commit do código; `CLAUDE.md` atualizado.

[Execução real: exemplo → código 0, "R$ 680.53"; entrada inválida → código 1, sem arquivo de saída; sem argumentos → código 2. 209 testes passando. Commits `95267a4 docs(plan): v1.1 — código de saída 3 para falha ao gravar a saída (DT-005)` e `164cd5a feat(T-020): CLI calcular --input/--output com códigos de saída 0/1/2/3`.]

Lição registrada para Diligência: "os testes passam" não basta; rodar o produto como o usuário vai rodar revelou dois defeitos.

---

## Usuário

pode exportar a sessão 07 e seguir para a T-021

## Claude

[Gerou este arquivo e fez o commit; em seguida, a T-021.]
