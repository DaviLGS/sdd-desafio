# Tasks — Motor de Cálculo de Reembolso

**Baseado na spec:** 2.1 · **Baseado no plano:** 2.0

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
  - **Commit:** `2aefedd`

- [x] **T-005** — Leitura e validação da entrada JSON, com valores lidos como decimais exatos.
  - **Atende:** spec seção 4 (entrada e entrada malformada); plano DT-001
  - **Aceite:** `LeitorEntradaTest` passa:
    - lê `exemplos/despesas-exemplo.json` com 14 despesas;
    - o d-011 chega com valor exato 33,333;
    - campo obrigatório ausente, data inválida ou valor não numérico → `EntradaInvalidaException`.
  - **Commit:** `e93ca00`

## Fase 2 — Regras de negócio: verificações por despesa

- [x] **T-006** — Categorias elegíveis: fora da política → `recusada`.
  - **Atende:** RN-003
  - **Aceite:** `Rn003CategoriaElegivelTest` passa: `coworking` → `CATEGORIA_FORA_DA_POLITICA`; as 3 categorias elegíveis passam.
  - **Commit:** `54b5976`

- [x] **T-007** — Período de competência inclusivo; fora dele → revisão.
  - **Atende:** RN-010, AMB-017, AMB-018
  - **Aceite:** `Rn010PeriodoCompetenciaTest` passa: `inicio` e `fim` dentro; dia anterior ao `inicio` e posterior ao `fim` → `FORA_DO_PERIODO`; 15/04 num período de julho → `FORA_DO_PERIODO`.
  - **Commit:** `20ac56b`

- [x] **T-008** — Detecção de duplicatas (todos os campos iguais, exceto o ID); todas as cópias vão para revisão.
  - **Atende:** RN-009, AMB-016
  - **Aceite:** `Rn009DuplicataTest` passa:
    - duas despesas idênticas → as duas com `DUPLICATA`;
    - três cópias → as três;
    - diferença só na descrição → não é duplicata;
    - `ALIMENTACAO` × `alimentacao` com o resto igual → é duplicata.
  - **Commit:** `4e36467`

- [x] **T-009** — Nota fiscal obrigatória a partir de R$ 100,00, por despesa e pelo valor gasto.
  - **Atende:** RN-006, AMB-006, AMB-007, AMB-008, AMB-009
  - **Aceite:** `Rn006NotaFiscalTest` passa:
    - 100,00 sem NF → `NF_AUSENTE`;
    - 100,01 sem NF → `NF_AUSENTE`;
    - 99,99 sem NF → ok;
    - 690,00 de hospedagem sem NF → `NF_AUSENTE`;
    - 60,00 + 50,00 no mesmo dia sem NF → ok;
    - estorno negativo sem NF → ok.
  - **Commit:** `eceef83`

- [x] **T-010** — Leitura do número de noites na descrição da hospedagem.
  - **Atende:** RN-005, AMB-004
  - **Aceite:** `Rn005NoitesHospedagemTest` passa:
    - "Hotel Rio - 2 diarias" → 2;
    - "Airbnb 3 noites" → 3;
    - "1 diária" → 1;
    - "Hotel 2diarias" (sem espaço) → 2 (D-002);
    - "Hotel centro", "0 noites", "duas noites" e "2 noites + 1 diaria" → `NOITES_NAO_IDENTIFICADAS` (D-002).
  - **Commit:** `872f566`

- [x] **T-011** — Consolidação do status a partir dos motivos.
  - **Atende:** RN-012
  - **Aceite:** `Rn012StatusTest` passa:
    - sem motivos → `aprovada`;
    - só motivos de revisão → `em_revisao`;
    - `CATEGORIA_FORA_DA_POLITICA` + `FORA_DO_PERIODO` → `recusada`, com os dois motivos listados.
  - **Commit:** `f60c3da`

## Fase 3 — Regras de negócio: limites

- [x] **T-012** — Dias de viagem a partir das hospedagens (noites; o dia da saída não conta; hospedagem em revisão conta).
  - **Atende:** RN-007, AMB-011, AMB-012, AMB-013
  - **Aceite:** `Rn007ViagemTest` passa:
    - hospedagem de 14/07 com 2 noites → 14 e 15 são de viagem, 16 não;
    - hospedagem sem NF com 3 noites ainda gera dias de viagem;
    - hospedagem sem noites identificáveis não gera.
  - **Commit:** `20faa4f`

- [x] **T-013** — Ampliação por fim de semana ou plantão, sem acumular com a viagem.
  - **Atende:** RN-008, AMB-014, AMB-015
  - **Aceite:** `Rn008FimDeSemanaPlantaoTest` passa:
    - sábado → `FIM_DE_SEMANA_OU_PLANTAO`;
    - quarta com "Almoco - plantao" → ampliado;
    - "PLANTÃO" → ampliado;
    - sábado em viagem → limite 90,00 (não 120,00) e ampliação `VIAGEM` (D-003);
    - dia útil sem viagem → `NENHUM`.
  - **Commit:** `7124b69`

- [x] **T-014** — Grupos diários de alimentação e transporte com o limite do dia.
  - **Atende:** RN-004, AMB-001, AMB-002, AMB-003, AMB-005
  - **Aceite:** `Rn004LimiteDiarioTest` passa:
    - 72,50 + 38,00 → total 110,50, reembolso 60,00;
    - 40,00 → 40,00;
    - transporte 100,00 → 80,00;
    - valor zero aprovado sem efeito;
    - despesas em revisão não entram no grupo;
    - sobra de um dia não passa para o seguinte.
  - **Commit:** `1ec0bb2`

- [x] **T-015** — Grupo de hospedagem com limite de R$ 250 × noites, sem ampliação.
  - **Atende:** RN-005, AMB-004, AMB-005, AMB-013
  - **Aceite:** `Rn005LimiteHospedagemTest` passa:
    - 480,00 / 2 noites → 480,00;
    - 750,00 / 3 → 750,00;
    - 900,00 / 3 → 750,00;
    - hospedagem em dia de viagem/sábado continua com 250/noite;
    - hospedagens sobrepostas são limitadas isoladamente.
  - **Commit:** `dd069b5`

- [x] **T-016** — Estornos: abatem do grupo do dia (alimentação/transporte) ou vão para revisão.
  - **Atende:** RN-011, AMB-019
  - **Aceite:** `Rn011EstornoTest` passa:
    - transporte 100,00 + −45,00 → total 55,00, reembolso 55,00;
    - −45,00 sozinho → `ESTORNO_SEM_DESPESA`;
    - estorno maior que a soma do dia → `ESTORNO_SEM_DESPESA`;
    - 50,00 + −45,00 + −45,00 → os dois estornos com `ESTORNO_SEM_DESPESA` (D-004);
    - 100,00 + −30,00 + −20,00 → total 50,00 (D-004);
    - estorno de hospedagem → `ESTORNO_SEM_DESPESA`;
    - estorno já em revisão por outro motivo também recebe `ESTORNO_SEM_DESPESA` (D-005, corrigido na T-021).
  - **Commit:** `6ba2e66`

## Fase 4 — Saída e CLI

- [x] **T-017** — `MotorReembolso`: orquestração na ordem da seção 8 da spec e resumo.
  - **Atende:** spec seções 8 e 9; RN-001 a RN-012 integradas
  - **Aceite:** `MotorReembolsoExemploTest` passa:
    - com `exemplos/despesas-exemplo.json`, o status e os motivos de cada despesa batem com a tabela da seção 9;
    - os 5 grupos batem;
    - `total_reembolsado` = 680,53;
    - quantidade 6/7/1;
    - duas execuções produzem resultado idêntico.
  - **Commit:** `96e3a1a`

- [x] **T-018** — Justificativa textual de cada despesa.
  - **Atende:** spec seção 4 (`justificativa`)
  - **Aceite:** `JustificativaTest` passa: cada motivo gera seu texto fixo; despesa aprovada cita o grupo (`2026-07-03/alimentacao`); vários motivos aparecem todos.
  - **Commit:** `047a6fc`

- [x] **T-019** — Escrita da saída JSON no schema da spec.
  - **Atende:** spec seção 4 (saída), AMB-002
  - **Aceite:** `EscritorSaidaTest` passa:
    - o exemplo da seção 4 da spec é reproduzido campo a campo;
    - todos os valores com exatamente 2 casas (60 → `60.00`);
    - status e motivos em minúsculas/códigos da spec;
    - `noites` só em hospedagem.
  - **Commit:** `04f2c39`

- [x] **T-020** — CLI `calcular --input X --output Y` com códigos de saída.
  - **Atende:** spec seção 4 (entrada malformada); plano DT-005
  - **Aceite:** `MainCliTest` passa:
    - o exemplo gera um arquivo com `total_reembolsado` 680.53 e código 0;
    - entrada inválida → código 1 e nenhum arquivo criado;
    - comando ou argumento ausente → código 2;
    - pasta de saída inexistente → código 3 (plano DT-005 v1.1);
    - mensagens de console sem acentos.
  - **Commit:** `164cd5a`

## Fase 5 — Casos de borda e documentação

- [x] **T-021** — Bateria dos casos de borda da seção 7 da spec, rodando pelo motor completo.
  - **Atende:** spec seção 7 (todas as linhas)
  - **Aceite:** `CasosDeBordaTest` passa, com um teste por linha da tabela da seção 7 e o `@DisplayName` igual ao texto da coluna "Caso"; um teste lê a spec e falha se alguma linha da seção 7 ficar sem teste.
  - **Commit:** `8493181`

- [x] **T-022** — README do projeto: pré-requisitos, como compilar, rodar e testar, e o que foi construído.
  - **Atende:** entrega (README)
  - **Aceite:** seguindo só o README, num clone limpo, `mvn package` e o comando de exemplo geram a saída com total 680.53.
  - **Commit:** `5e5bc96`

---

## Fase 6 — Envelope do Dia 2: Política v4 (D-006, D-007)

Ponto de partida: `5f7712c`. Spec 2.1, plano 2.0. Ao fim de cada task, os testes passam — os
testes da v1 que dependiam de `Politica.V3` ou do total 680,53 são ajustados na task que os
quebra, e o ajuste é citado no commit.

**Exceção justificada:** o teste de rastreabilidade `CasosDeBordaTest.toda_linha_da_secao_7_tem_teste`
fica **vermelho** desde a spec 2.0 (`149f189`) até a T-032. É ele apontando as 16 linhas novas da
seção 7 que ainda não têm teste — exatamente o papel dele. Não foi desligado.

- [x] **T-023** — `Politica` configurável: `LimiteCategoria` e `Periodicidade` no domínio; `Politica` resolvida por tabela (`elegivel`, `periodicidade`, `limiteDiario`, `limitePorNoite`, percentual e piso); fixture `tests/reembolso/Politicas.java` (padrão, CC-COMERCIAL, CC-ENG-PLATAFORMA); `LimiteDiario`, `LimiteHospedagem` e `Estornos` agrupam pela `periodicidade`. `Politica.V3` continua só para a CLI, montada no formato novo e marcada `@Deprecated`, e sai na T-031 (a CLI só lê a política do arquivo a partir dela).
  - **Atende:** RN-003, RN-004, RN-005, RN-011 (periodicidade), AMB-023, AMB-026, AMB-027
  - **Aceite:** `PoliticaTest` (reescrito) passa: limite 0 → não elegível; categoria ausente → não elegível; ampliado = base × 1,5; `representacao` de periodicidade `dia` forma grupo diário (`Rn004LimiteDiarioTest`); todos os testes da v1 passam usando a fixture.
  - **Commit:** `45b7391`

- [x] **T-024** — Leitura e validação do arquivo de política (`io.LeitorPolitica` → `TabelaPoliticas`).
  - **Atende:** seção 4 (arquivo de política), AMB-028, AMB-029
  - **Aceite:** `LeitorPoliticaTest` passa: lê `politica-v4.json` (padrão + 3 CCs, piso 100, percentual 50); `vigencia` ignorada; rejeita arquivo ausente, sem `padrao`, `limite` negativo ou não numérico, `periodicidade` desconhecida.
  - **Commit:** `c09c8c8`

- [x] **T-025** — Seleção da tabela por centro de custo (`regras.SelecaoTabela`).
  - **Atende:** RN-013, RN-001 (categorias da política), AMB-021, AMB-022, AMB-025
  - **Aceite:** `Rn013TabelaCentroCustoTest` passa: `CC-COMERCIAL` → tabela própria; `CC-SUPORTE-N2` → `padrao`; `CC-ADM` sem hospedagem → hospedagem não elegível; `" CC-COMERCIAL "` → `CC-COMERCIAL`; `cc-comercial` → `padrao`; `representacao` só no `CC-COMERCIAL`.
  - **Commit:**

- [ ] **T-026** — Leitura do câmbio (`io.LeitorCambio` → `TabelaCambio`) e campo `moeda` na entrada.
  - **Atende:** seção 4 (arquivo de câmbio, campo `moeda`), AMB-028
  - **Aceite:** `LeitorCambioTest` passa (lê `cambio.json`; rejeita taxa não numérica e data inválida); `LeitorEntradaTest` passa com `moeda` presente, ausente (= `BRL`) e não textual (rejeitada).
  - **Commit:**

- [ ] **T-027** — Conversão de moeda (`regras.ConversaoMoeda`).
  - **Atende:** RN-014, RN-002, AMB-030, AMB-031, AMB-032, AMB-033, AMB-036
  - **Aceite:** `Rn014ConversaoMoedaTest` passa: EUR 22 em 14/07 → 130,46; sábado 18/07 → cotação de 17/07; GBP → sem cotação; 10/07 → sem cotação; 30/07 → cotação de 28/07; BRL e sem moeda → sem conversão; USD 10,005 × 5,42 → 54,23; estorno −USD 10 → −55,00.
  - **Commit:**

- [ ] **T-028** — Regras existentes com valor convertido: NF em BRL e ignorada sem cotação; `moeda` na duplicata; estorno pelo sinal do valor original.
  - **Atende:** RN-006, RN-009, RN-011, AMB-034, AMB-035
  - **Aceite:** `Rn006NotaFiscalTest` (USD 40 = 220 sem NF → `NF_AUSENTE`; `valor` nulo → sem motivo), `Rn009DuplicataTest` (EUR 22 × USD 22 → não duplicata; sem moeda × BRL → duplicata) e `Rn011EstornoTest` passam.
  - **Commit:**

- [ ] **T-029** — Motor v4: passo 0 (tabela), conversão no passo 1, motivo `CAMBIO_INDISPONIVEL` no passo 2, `tabelaAplicada` no resultado; justificativa do novo motivo.
  - **Atende:** seções 8 e 9 da spec, RN-013, RN-014, RN-012
  - **Aceite:** `MotorReembolsoExemploTest` reescrito para as três entradas da seção 9: 216,53 (5/6/3), 1.993,26 (7/2/1) e 373,76 (3/0/1), com status, motivos e grupos de cada tabela; `JustificativaTest` cobre `CAMBIO_INDISPONIVEL`.
  - **Commit:**

- [ ] **T-030** — Saída v4: `valor` em BRL ou `null`; `moeda`, `valor_original`, `taxa_cambio`, `data_cotacao` só em despesa estrangeira; `resumo.tabela_aplicada`.
  - **Atende:** seção 4 (saída), AMB-037
  - **Aceite:** `EscritorSaidaTest` passa: exemplo da seção 4 com `tabela_aplicada`; o trecho de e-004 da seção 4 reproduzido; e-006 com `valor` nulo; despesas em BRL sem campos de câmbio.
  - **Commit:**

- [ ] **T-031** — CLI com `--politica` e `--cambio` obrigatórios; remoção de `Politica.V3`.
  - **Atende:** plano DT-009, AMB-028
  - **Aceite:** `MainCliTest` passa: os três arquivos da seção 9 geram os totais esperados; falta `--politica` ou `--cambio` → código 2; política ou câmbio inválido/inexistente → código 1, sem saída.
  - **Commit:**

- [ ] **T-032** — Casos de borda da v4 (16 linhas novas da seção 7).
  - **Atende:** seção 7 da spec
  - **Aceite:** `CasosDeBordaTest` passa com um teste por linha da seção 7, incluindo as 16 novas; o teste de rastreabilidade contra a spec passa.
  - **Commit:**

- [ ] **T-033** — README e `CLAUDE.md` atualizados para a v4 (comando com `--politica` e `--cambio`, resultados dos três exemplos).
  - **Atende:** entrega (README)
  - **Aceite:** seguindo só o README, num clone limpo, os três comandos geram 216,53, 1.993,26 e 373,76.
  - **Commit:**

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
| RN-013 | T-025 | `Rn013TabelaCentroCustoTest` |
| RN-014 | T-027 | `Rn014ConversaoMoedaTest` |
| RN-003/004/005 (v4: tabela e periodicidade) | T-023 | `PoliticaTest`, `Rn004LimiteDiarioTest` |
| RN-006, RN-009, RN-011 (v4: valor convertido) | T-028 | `Rn006NotaFiscalTest`, `Rn009DuplicataTest`, `Rn011EstornoTest` |
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
| AMB-021, 022, 025 | T-025 | `Rn013TabelaCentroCustoTest` |
| AMB-023, 026, 027 | T-023 | `PoliticaTest` |
| AMB-024 | T-029, T-032 | `MotorReembolsoExemploTest`, `CasosDeBordaTest` |
| AMB-028, 029 | T-024, T-026, T-031 | `LeitorPoliticaTest`, `LeitorCambioTest`, `MainCliTest` |
| AMB-030 a 033, 036 | T-027 | `Rn014ConversaoMoedaTest` |
| AMB-034, 035 | T-028 | `Rn006NotaFiscalTest`, `Rn009DuplicataTest` |
| AMB-037 | T-030 | `EscritorSaidaTest` |
| Seção 4 (entrada/saída) | T-005, T-018, T-019, T-020 | `LeitorEntradaTest`, `JustificativaTest`, `EscritorSaidaTest`, `MainCliTest` |
| Seção 7 (casos de borda) | T-021, T-032 | `CasosDeBordaTest` |
| Seções 8 e 9 (ordem e aceite) | T-017, T-029 | `MotorReembolsoExemploTest` |
