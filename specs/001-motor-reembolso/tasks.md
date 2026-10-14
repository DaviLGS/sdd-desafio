# Tasks — Motor de Cálculo de Reembolso

**Baseado na spec:** 1.1 · **Baseado no plano:** 1.0

> Cada task é pequena o bastante para virar **um commit**. Se você não consegue
> descrever o critério de aceite como "o teste X passa", a task está grande demais.
>
> Marque `[x]` conforme conclui — ao longo do caminho, não tudo no fim. O histórico
> de quando cada task foi marcada é lido na correção.

**Formato do commit:** `feat(T-003): <descrição>` · `test(T-003): <descrição>`

**Ordem:** as tasks seguem a ordem de aplicação da seção 8 da spec. Cada task de
regra entrega o código **e** o teste no mesmo commit (ou em `feat` + `test` em sequência).

---

## Fase 1 — Fundação

- [x] **T-001** — Criar o projeto Maven: alvo Java 17, `src/` e `tests/` na raiz, JUnit 5, Jackson e geração do JAR executável.
  - **Atende:** plano DT-003, DT-004
  - **Aceite:** `mvn test` executa e passa o `ProjetoSmokeTest`; `mvn package` gera `target/motor-reembolso.jar`.
  - **Commit:** `ce35fd0`

- [x] **T-002** — Modelo de domínio (records e enums da seção 3 do plano) e `Politica.V3` com os valores da política.
  - **Atende:** spec seção 4; plano seções 3 e 4
  - **Aceite:** `PoliticaTest` passa (limites 60/80/250, fator 1,5, piso da NF 100,00, 3 categorias elegíveis).
  - **Commit:** `3014e50`

- [x] **T-003** — Arredondamento em cadeia para 2 casas.
  - **Atende:** RN-002, AMB-010
  - **Aceite:** `Rn002ArredondamentoTest` passa, cobrindo 33,333 → 33,33; 3,444 → 3,44; 3,445 → 3,45; 3,4449 → 3,45; −3,445 → −3,45; valor com 2 casas inalterado.
  - **Commit:** `9b1c73f`

- [x] **T-004** — Normalização de categoria (maiúsculas, minúsculas e acentos).
  - **Atende:** RN-001, AMB-020
  - **Aceite:** `Rn001NormalizacaoCategoriaTest` passa: `ALIMENTACAO`, `Alimentação` e `alimentacao` → `alimentacao`.
  - **Commit:**

- [ ] **T-005** — Leitura e validação da entrada JSON, com valores lidos como decimais exatos.
  - **Atende:** spec seção 4 (entrada e entrada malformada); plano DT-001
  - **Aceite:** `LeitorEntradaTest` passa:
    - lê `exemplos/despesas-exemplo.json` com 14 despesas;
    - o d-011 chega com valor exato 33,333;
    - campo obrigatório ausente, data inválida ou valor não numérico → `EntradaInvalidaException`.
  - **Commit:**

## Fase 2 — Regras de negócio: verificações por despesa

- [ ] **T-006** — Categorias elegíveis: fora da política → `recusada`.
  - **Atende:** RN-003
  - **Aceite:** `Rn003CategoriaElegivelTest` passa: `coworking` → `CATEGORIA_FORA_DA_POLITICA`; as 3 categorias elegíveis passam.
  - **Commit:**

- [ ] **T-007** — Período de competência inclusivo; fora dele → revisão.
  - **Atende:** RN-010, AMB-017, AMB-018
  - **Aceite:** `Rn010PeriodoCompetenciaTest` passa: `inicio` e `fim` dentro; dia anterior ao `inicio` e posterior ao `fim` → `FORA_DO_PERIODO`; 15/04 num período de julho → `FORA_DO_PERIODO`.
  - **Commit:**

- [ ] **T-008** — Detecção de duplicatas (todos os campos iguais, exceto o ID); todas as cópias vão para revisão.
  - **Atende:** RN-009, AMB-016
  - **Aceite:** `Rn009DuplicataTest` passa:
    - duas despesas idênticas → as duas com `DUPLICATA`;
    - três cópias → as três;
    - diferença só na descrição → não é duplicata;
    - `ALIMENTACAO` × `alimentacao` com o resto igual → é duplicata.
  - **Commit:**

- [ ] **T-009** — Nota fiscal obrigatória a partir de R$ 100,00, por despesa e pelo valor gasto.
  - **Atende:** RN-006, AMB-006, AMB-007, AMB-008, AMB-009
  - **Aceite:** `Rn006NotaFiscalTest` passa:
    - 100,00 sem NF → `NF_AUSENTE`;
    - 100,01 sem NF → `NF_AUSENTE`;
    - 99,99 sem NF → ok;
    - 690,00 de hospedagem sem NF → `NF_AUSENTE`;
    - 60,00 + 50,00 no mesmo dia sem NF → ok;
    - estorno negativo sem NF → ok.
  - **Commit:**

- [ ] **T-010** — Leitura do número de noites na descrição da hospedagem.
  - **Atende:** RN-005, AMB-004
  - **Aceite:** `Rn005NoitesHospedagemTest` passa:
    - "Hotel Rio - 2 diarias" → 2;
    - "Airbnb 3 noites" → 3;
    - "1 diária" → 1;
    - "Hotel centro", "0 noites" e "duas noites" → `NOITES_NAO_IDENTIFICADAS`.
  - **Commit:**

- [ ] **T-011** — Consolidação do status a partir dos motivos.
  - **Atende:** RN-012
  - **Aceite:** `Rn012StatusTest` passa:
    - sem motivos → `aprovada`;
    - só motivos de revisão → `em_revisao`;
    - `CATEGORIA_FORA_DA_POLITICA` + `FORA_DO_PERIODO` → `recusada`, com os dois motivos listados.
  - **Commit:**

## Fase 3 — Regras de negócio: limites

- [ ] **T-012** — Dias de viagem a partir das hospedagens (noites; o dia da saída não conta; hospedagem em revisão conta).
  - **Atende:** RN-007, AMB-011, AMB-012, AMB-013
  - **Aceite:** `Rn007ViagemTest` passa:
    - hospedagem de 14/07 com 2 noites → 14 e 15 são de viagem, 16 não;
    - hospedagem sem NF com 3 noites ainda gera dias de viagem;
    - hospedagem sem noites identificáveis não gera.
  - **Commit:**

- [ ] **T-013** — Ampliação por fim de semana ou plantão, sem acumular com a viagem.
  - **Atende:** RN-008, AMB-014, AMB-015
  - **Aceite:** `Rn008FimDeSemanaPlantaoTest` passa:
    - sábado → `FIM_DE_SEMANA_OU_PLANTAO`;
    - quarta com "Almoco - plantao" → ampliado;
    - "PLANTÃO" → ampliado;
    - sábado em viagem → limite 90,00 (não 120,00);
    - dia útil sem viagem → `NENHUM`.
  - **Commit:**

- [ ] **T-014** — Grupos diários de alimentação e transporte com o limite do dia.
  - **Atende:** RN-004, AMB-001, AMB-002, AMB-003, AMB-005
  - **Aceite:** `Rn004LimiteDiarioTest` passa:
    - 72,50 + 38,00 → total 110,50, reembolso 60,00;
    - 40,00 → 40,00;
    - transporte 100,00 → 80,00;
    - valor zero aprovado sem efeito;
    - despesas em revisão não entram no grupo;
    - sobra de um dia não passa para o seguinte.
  - **Commit:**

- [ ] **T-015** — Grupo de hospedagem com limite de R$ 250 × noites, sem ampliação.
  - **Atende:** RN-005, AMB-004, AMB-005, AMB-013
  - **Aceite:** `Rn005LimiteHospedagemTest` passa:
    - 480,00 / 2 noites → 480,00;
    - 750,00 / 3 → 750,00;
    - 900,00 / 3 → 750,00;
    - hospedagem em dia de viagem/sábado continua com 250/noite;
    - hospedagens sobrepostas são limitadas isoladamente.
  - **Commit:**

- [ ] **T-016** — Estornos: abatem do grupo do dia (alimentação/transporte) ou vão para revisão.
  - **Atende:** RN-011, AMB-019
  - **Aceite:** `Rn011EstornoTest` passa:
    - transporte 100,00 + −45,00 → total 55,00, reembolso 55,00;
    - −45,00 sozinho → `ESTORNO_SEM_DESPESA`;
    - estorno maior que a soma do dia → `ESTORNO_SEM_DESPESA`;
    - estorno de hospedagem → `ESTORNO_SEM_DESPESA`.
  - **Commit:**

## Fase 4 — Saída e CLI

- [ ] **T-017** — `MotorReembolso`: orquestração na ordem da seção 8 da spec e resumo.
  - **Atende:** spec seções 8 e 9; RN-001 a RN-012 integradas
  - **Aceite:** `MotorReembolsoExemploTest` passa:
    - com `exemplos/despesas-exemplo.json`, o status e os motivos de cada despesa batem com a tabela da seção 9;
    - os 5 grupos batem;
    - `total_reembolsado` = 680,53;
    - quantidade 6/7/1;
    - duas execuções produzem resultado idêntico.
  - **Commit:**

- [ ] **T-018** — Justificativa textual de cada despesa.
  - **Atende:** spec seção 4 (`justificativa`)
  - **Aceite:** `JustificativaTest` passa: cada motivo gera seu texto fixo; despesa aprovada cita o grupo (`2026-07-03/alimentacao`); vários motivos aparecem todos.
  - **Commit:**

- [ ] **T-019** — Escrita da saída JSON no schema da spec.
  - **Atende:** spec seção 4 (saída), AMB-002
  - **Aceite:** `EscritorSaidaTest` passa:
    - o exemplo da seção 4 da spec é reproduzido campo a campo;
    - todos os valores com exatamente 2 casas (60 → `60.00`);
    - status e motivos em minúsculas/códigos da spec;
    - `noites` só em hospedagem.
  - **Commit:**

- [ ] **T-020** — CLI `calcular --input X --output Y` com códigos de saída.
  - **Atende:** spec seção 4 (entrada malformada); plano DT-005
  - **Aceite:** `MainCliTest` passa:
    - o exemplo gera um arquivo com `total_reembolsado` 680.53 e código 0;
    - entrada inválida → código 1 e nenhum arquivo criado;
    - comando ou argumento ausente → código 2.
  - **Commit:**

## Fase 5 — Casos de borda e documentação

- [ ] **T-021** — Bateria dos casos de borda da seção 7 da spec, rodando pelo motor completo.
  - **Atende:** spec seção 7 (todas as linhas)
  - **Aceite:** `CasosDeBordaTest` passa, com um caso parametrizado por linha da tabela da seção 7 e o `@DisplayName` igual ao texto da coluna "Caso".
  - **Commit:**

- [ ] **T-022** — README do projeto: pré-requisitos, como compilar, rodar e testar, e o que foi construído.
  - **Atende:** entrega (README)
  - **Aceite:** seguindo só o README, num clone limpo, `mvn package` e o comando de exemplo geram a saída com total 680.53.
  - **Commit:**

---

## Fase 6 — Envelope (criar no Dia 2)

<Novas tasks a partir da mudança de requisito. Numeração continua de onde parou —
não reinicie e não renumere as antigas: a numeração é o eixo da rastreabilidade.>

---

## Cobertura

Preencha ao fechar cada fase. É a sua própria checagem de rastreabilidade — e é
exatamente a matriz que a correção vai montar.

| Regra da spec | Task | Teste |
|---|---|---|
| RN-001 | T-004 | `Rn001NormalizacaoCategoriaTest` |
| RN-002 | T-003 | `Rn002ArredondamentoTest` |
| RN-003 | T-006 | `Rn003CategoriaElegivelTest` |
| RN-004 | T-014 | `Rn004LimiteDiarioTest` |
| RN-005 | T-010, T-015 | `Rn005NoitesHospedagemTest`, `Rn005LimiteHospedagemTest` |
| RN-006 | T-009 | `Rn006NotaFiscalTest` |
| RN-007 | T-012 | `Rn007ViagemTest` |
| RN-008 | T-013 | `Rn008FimDeSemanaPlantaoTest` |
| RN-009 | T-008 | `Rn009DuplicataTest` |
| RN-010 | T-007 | `Rn010PeriodoCompetenciaTest` |
| RN-011 | T-016 | `Rn011EstornoTest` |
| RN-012 | T-011 | `Rn012StatusTest` |
| AMB-001, 002, 003, 005 | T-014 | `Rn004LimiteDiarioTest` |
| AMB-004 | T-010, T-015 | `Rn005NoitesHospedagemTest`, `Rn005LimiteHospedagemTest` |
| AMB-006, 007, 008, 009 | T-009 | `Rn006NotaFiscalTest` |
| AMB-010 | T-003 | `Rn002ArredondamentoTest` |
| AMB-011, 012 | T-012 | `Rn007ViagemTest` |
| AMB-013 | T-012, T-015 | `Rn007ViagemTest`, `Rn005LimiteHospedagemTest` |
| AMB-014, 015 | T-013 | `Rn008FimDeSemanaPlantaoTest` |
| AMB-016 | T-008 | `Rn009DuplicataTest` |
| AMB-017, 018 | T-007 | `Rn010PeriodoCompetenciaTest` |
| AMB-019 | T-016 | `Rn011EstornoTest` |
| AMB-020 | T-004 | `Rn001NormalizacaoCategoriaTest` |
| Seção 4 (entrada/saída) | T-005, T-018, T-019, T-020 | `LeitorEntradaTest`, `JustificativaTest`, `EscritorSaidaTest`, `MainCliTest` |
| Seção 7 (casos de borda) | T-021 | `CasosDeBordaTest` |
| Seções 8 e 9 (ordem e aceite) | T-017 | `MotorReembolsoExemploTest` |
