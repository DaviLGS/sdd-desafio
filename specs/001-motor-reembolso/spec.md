# Spec — Motor de Cálculo de Reembolso

**Versão:** 2.0 · **Status:** rascunho · **Última alteração:** 2026-10-16

> **Regra de ouro deste arquivo:** ele descreve o QUÊ e o PORQUÊ. Nenhuma linha
> aqui pode citar linguagem, biblioteca, classe, função ou estrutura de pasta.
> Se apareceu solução, o lugar dela é o `plan.md`.
>
> **Teste de aceitação da própria spec:** uma pessoa que nunca viu o projeto
> consegue, lendo só este arquivo, verificar se o sistema está correto?
>
> **Versão 2.0:** incorpora a Política de Reembolso **v4** (envelope do Dia 2,
> `exemplos/envelope/`): limites por centro de custo lidos de um arquivo externo
> e despesas em moeda estrangeira. Ver `DECISIONS.md`, D-006.

---

## 1. Problema

Hoje o financeiro confere manualmente, item por item, cada despesa dos
colaboradores contra a política de reembolso do RH. O processo é lento, e a
política é ambígua, então pessoas diferentes chegam a valores diferentes para a
mesma despesa. Desde a v4, os limites variam por centro de custo e mudam sem
aviso, e há despesas em moeda estrangeira.

## 2. Objetivo

Dado o conjunto de despesas de um colaborador em um período de competência, a
tabela de limites vigente e as cotações de câmbio, o sistema calcula de forma
determinística quanto é reembolsável e justifica cada decisão, aplicando a
política conforme interpretada nesta spec.

## 3. Fora de escopo

- Não efetua pagamento nem se integra a sistemas de folha ou financeiro.
- Não resolve pendências: despesas "em revisão" são apenas sinalizadas, e a revisão é humana.
- Não lê nem valida o conteúdo de notas fiscais. Confia no campo que informa se há NF.
- Não busca cotações nem limites em nenhum lugar além dos arquivos recebidos. Não há valores de política embutidos no sistema.
- Não implementa a fila de aprovação manual acima de R$ 500 (item C da v4, opcional). Ver seção 10.
- Não processa mais de um colaborador ou mais de um período por execução.
- Não guarda histórico entre execuções. Duplicatas só são detectadas dentro do mesmo arquivo de entrada.
- Não interpreta o texto da descrição, exceto para dois fins: o número de noites de hospedagem (RN-005) e a palavra "plantão" (RN-008).
- Não aplica regras por cargo ou cliente. O centro de custo serve **apenas** para escolher a tabela de limites (RN-013).

## 4. Entrada e saída

### Entrada

O sistema recebe **três arquivos**, todos obrigatórios:

1. **Despesas** — conforme `exemplos/despesas-exemplo.json` e `exemplos/envelope/despesas-envelope.json`.
2. **Política** — conforme `exemplos/envelope/politica-v4.json`.
3. **Câmbio** — conforme `exemplos/envelope/cambio.json`.

Se qualquer um dos três estiver ausente, ilegível ou fora do formato, nada é
processado: o sistema informa o erro e não gera saída (AMB-028).

#### Arquivo de despesas

| Campo | Tipo | Significado | Obrigatório |
|---|---|---|---|
| `colaborador.id` | texto | Identificador do colaborador | sim |
| `colaborador.nome` | texto | Nome do colaborador | sim |
| `colaborador.centro_custo` | texto | Centro de custo; escolhe a tabela de limites (RN-013) | sim |
| `periodo.competencia` | texto `AAAA-MM` | Mês de competência | sim |
| `periodo.inicio` | data `AAAA-MM-DD` | Primeiro dia do período (inclusivo) | sim |
| `periodo.fim` | data `AAAA-MM-DD` | Último dia do período (inclusivo) | sim |
| `despesas[].id` | texto | Identificador único da despesa | sim |
| `despesas[].data` | data `AAAA-MM-DD` | Dia em que a despesa ocorreu | sim |
| `despesas[].categoria` | texto | Categoria; comparada sem diferenciar maiúsculas, minúsculas e acentos (RN-001) | sim |
| `despesas[].descricao` | texto | Descrição livre | sim |
| `despesas[].fornecedor` | texto | Fornecedor | sim |
| `despesas[].valor` | número | Valor na moeda da despesa. Pode ter mais de 2 casas decimais (RN-002) e pode ser negativo, no caso de estorno (RN-011) | sim |
| `despesas[].moeda` | texto (código ISO 4217) | Moeda do valor. Ausente = `BRL` (RN-014) | não |
| `despesas[].tem_nota_fiscal` | booleano | Se há nota fiscal | sim |

#### Arquivo de política

| Campo | Tipo | Significado |
|---|---|---|
| `padrao` | tabela | Tabela usada pelos centros de custo sem entrada própria (RN-013) |
| `centros_custo.<nome>` | tabela | Tabela de um centro de custo |
| `<tabela>.<categoria>.limite` | número | Limite em BRL. `0` = categoria não reembolsável (AMB-023) |
| `<tabela>.<categoria>.periodicidade` | `dia` \| `diaria` | `dia`: limite por dia (RN-004); `diaria`: limite por noite (RN-005) |
| `nota_fiscal_obrigatoria_acima_de` | número | Piso da nota fiscal, em BRL (RN-006) |
| `acrescimo_em_viagem_percentual` | número | Percentual de ampliação dos limites (RN-007, RN-008) |
| `versao`, `vigencia`, `moeda_base`, `observacao` | — | Informativos; não alteram o cálculo (AMB-029) |

Os nomes de categoria da política seguem a mesma normalização da RN-001.
Periodicidade diferente de `dia` ou `diaria` torna o arquivo inválido.

#### Arquivo de câmbio

| Campo | Tipo | Significado |
|---|---|---|
| `taxas.<AAAA-MM-DD>.<MOEDA>` | número | Quantos BRL vale 1 unidade da moeda naquela data |
| `moeda_base`, `fonte`, `observacao` | — | Informativos |

### Saída

| Campo | Tipo | Significado |
|---|---|---|
| `colaborador` | objeto | Cópia do bloco de entrada |
| `periodo` | objeto | Cópia do bloco de entrada |
| `despesas[]` | lista | Uma linha por despesa de entrada, na mesma ordem |
| `despesas[].id` | texto | ID da despesa |
| `despesas[].data` | data | Data da despesa |
| `despesas[].categoria` | texto | Categoria normalizada, em minúsculas e sem acentos |
| `despesas[].valor` | número (2 casas) \| `null` | Valor **em BRL**, após conversão (RN-014) e arredondamento (RN-002). `null` quando não há cotação (`CAMBIO_INDISPONIVEL`) |
| `despesas[].moeda` | texto | Somente em despesa em moeda estrangeira: a moeda original |
| `despesas[].valor_original` | número | Somente em despesa em moeda estrangeira: o valor como veio na entrada |
| `despesas[].taxa_cambio` | número \| `null` | Somente em despesa em moeda estrangeira: a cotação usada; `null` sem cotação |
| `despesas[].data_cotacao` | data \| `null` | Somente em despesa em moeda estrangeira: a data da cotação usada (pode ser anterior à da despesa, AMB-031) |
| `despesas[].status` | `aprovada` \| `em_revisao` \| `recusada` | Situação da despesa (RN-012) |
| `despesas[].motivos` | lista de códigos | Motivos de revisão ou recusa; vazia se aprovada (tabela abaixo) |
| `despesas[].justificativa` | texto | Explicação legível da decisão |
| `grupos[]` | lista | Agrupamentos onde o limite é aplicado (RN-004 e RN-005) |
| `grupos[].data` | data | Dia do grupo |
| `grupos[].categoria` | texto | Categoria do grupo |
| `grupos[].despesas` | lista de IDs | Despesas **aprovadas** que compõem o grupo |
| `grupos[].noites` | inteiro | Somente em categoria de periodicidade `diaria`: número de noites (RN-005) |
| `grupos[].valor_total` | número (2 casas) | Soma dos valores (em BRL) das despesas do grupo |
| `grupos[].limite` | número (2 casas) | Limite aplicado ao grupo |
| `grupos[].limite_ampliado` | `nenhum` \| `viagem` \| `fim_de_semana_ou_plantao` | Por que o limite foi (ou não) ampliado (RN-007, RN-008) |
| `grupos[].valor_reembolsado` | número (2 casas) | Menor valor entre `valor_total` e `limite` |
| `resumo.tabela_aplicada` | texto | Nome do centro de custo cuja tabela foi usada, ou `padrao` (RN-013) |
| `resumo.total_reembolsado` | número (2 casas) | Soma de `valor_reembolsado` de todos os grupos |
| `resumo.quantidade` | objeto | Quantidade de despesas por status |

Despesas em BRL (com `moeda` ausente ou `BRL`) **não** trazem `moeda`,
`valor_original`, `taxa_cambio` nem `data_cotacao`: a saída delas é a mesma da
versão 1.x (AMB-037).

**Códigos de motivo:**

| Código | Status resultante | Regra |
|---|---|---|
| `CATEGORIA_FORA_DA_POLITICA` | recusada | RN-003 |
| `FORA_DO_PERIODO` | em_revisao | RN-010 |
| `DUPLICATA` | em_revisao | RN-009 |
| `NF_AUSENTE` | em_revisao | RN-006 |
| `NOITES_NAO_IDENTIFICADAS` | em_revisao | RN-005 |
| `ESTORNO_SEM_DESPESA` | em_revisao | RN-011 |
| `CAMBIO_INDISPONIVEL` | em_revisao | RN-014 |

**Exemplo** (entrada com duas refeições no mesmo dia útil, sem viagem, centro de
custo `CC-X` sem entrada na tabela, logo política padrão):

```json
{
  "colaborador": { "id": "c-1", "nome": "Fulana", "centro_custo": "CC-X" },
  "periodo": { "competencia": "2026-07", "inicio": "2026-07-01", "fim": "2026-07-31" },
  "despesas": [
    { "id": "d-1", "data": "2026-07-03", "categoria": "alimentacao", "valor": 72.50,
      "status": "aprovada", "motivos": [], "justificativa": "Aprovada; limite aplicado no grupo 2026-07-03/alimentacao." },
    { "id": "d-2", "data": "2026-07-03", "categoria": "alimentacao", "valor": 38.00,
      "status": "aprovada", "motivos": [], "justificativa": "Aprovada; limite aplicado no grupo 2026-07-03/alimentacao." }
  ],
  "grupos": [
    { "data": "2026-07-03", "categoria": "alimentacao", "despesas": ["d-1", "d-2"],
      "valor_total": 110.50, "limite": 60.00, "limite_ampliado": "nenhum",
      "valor_reembolsado": 60.00 }
  ],
  "resumo": { "tabela_aplicada": "padrao", "total_reembolsado": 60.00,
              "quantidade": { "aprovada": 2, "em_revisao": 0, "recusada": 0 } }
}
```

**Despesa em moeda estrangeira** (trecho de `despesas[]`):

```json
{ "id": "e-004", "data": "2026-07-18", "categoria": "alimentacao", "valor": 178.80,
  "moeda": "EUR", "valor_original": 30.00, "taxa_cambio": 5.96, "data_cotacao": "2026-07-17",
  "status": "aprovada", "motivos": [], "justificativa": "Aprovada; limite aplicado no grupo 2026-07-18/alimentacao." }
```

## 5. Regras de negócio

### RN-001 — Normalização de categoria
**Regra:** a categoria é comparada sem diferenciar maiúsculas, minúsculas e acentos. `ALIMENTACAO`, `Alimentação` e `alimentacao` são a mesma categoria. A mesma normalização vale para os nomes de categoria do arquivo de política.
**Origem:** dados de entrada (AMB-020)
**Aceite:** despesas com categoria `ALIMENTACAO` ou `alimentação` são tratadas como alimentação e aparecem na saída como `alimentacao`.

### RN-002 — Arredondamento
**Regra:** todo valor com mais de 2 casas decimais é arredondado para 2 casas **antes de qualquer outra regra de valor**. O arredondamento é feito em cadeia, do último dígito para a esquerda, até restarem 2 casas: um dígito de 0 a 4 é descartado; de 5 a 9, soma 1 à casa anterior. Valores negativos são arredondados pelo valor absoluto, mantendo o sinal. Em despesa em moeda estrangeira, o arredondamento é aplicado **uma única vez, sobre o valor já convertido em BRL**, e não sobre o valor original (AMB-033).
**Origem:** dados de entrada (AMB-010)
**Aceite:**
- 33,333 → 33,33 · 3,444 → 3,44 · 3,445 → 3,45 · 3,4449 → 3,445 → 3,45 · −3,445 → −3,45.
- USD 10,005 com cotação 5,42 → 54,22710 → **54,23** (e não 10,01 × 5,42 = 54,25).

### RN-003 — Categorias elegíveis
**Regra:** uma categoria é reembolsável quando aparece na tabela aplicada ao colaborador (RN-013) com limite **maior que zero**. Categoria ausente da tabela, ou com limite zero, resulta em status `recusada`, motivo `CATEGORIA_FORA_DA_POLITICA`, e reembolso de R$ 0,00.
**Origem:** política do RH, item 9; política v4, item A (AMB-021, AMB-022, AMB-023)
**Aceite:**
- `coworking` de R$ 89,00 → `recusada` (não está em nenhuma tabela).
- `hospedagem` no `CC-ENG-PLATAFORMA` (limite 0) → `recusada`.
- `hospedagem` no `CC-ADM` (ausente da tabela do CC) → `recusada`.
- `representacao` no `CC-COMERCIAL` → elegível; num centro de custo sem entrada (tabela padrão) → `recusada`.

### RN-004 — Limite por dia
**Regra:** para cada categoria de periodicidade `dia` na tabela aplicada (ex.: alimentação, transporte urbano, representação), as despesas aprovadas da categoria num mesmo dia formam um grupo. O reembolso do grupo é o menor valor entre a soma do grupo (em BRL) e o limite do dia. O excedente não é reembolsado. O limite não usado em um dia **não acumula** para outros dias. Despesas de valor zero são aprovadas normalmente e não alteram o reembolso.
- Limite base: o `limite` da categoria na tabela aplicada.
- Limite ampliado (RN-007 ou RN-008): limite base × (1 + `acrescimo_em_viagem_percentual` / 100).

**Origem:** política do RH, itens 1, 2 e 4; política v4, item A (AMB-001, AMB-002, AMB-003, AMB-005, AMB-026)
**Aceite** (tabela padrão: alimentação 60, transporte 80; acréscimo 50%):
- Alimentação de R$ 72,50 + R$ 38,00 numa sexta-feira sem viagem → grupo com `valor_total` 110,50 e `valor_reembolsado` 60,00.
- Alimentação de R$ 40,00 num dia → reembolsa 40,00, e o dia seguinte continua com limite de 60,00.
- `CC-COMERCIAL`, representação de R$ 340,00 num dia útil → limite 300,00 → reembolsa 300,00.

### RN-005 — Limite por noite (hospedagem)
**Regra:** cada despesa aprovada de categoria com periodicidade `diaria` (hospedagem) forma o seu próprio grupo. O número de noites é lido da descrição: um número inteiro seguido de "diária(s)" ou "noite(s)", com ou sem espaço entre eles, sem diferenciar maiúsculas, minúsculas ou acentos. O limite do grupo é o `limite` da tabela × noites, e o reembolso é o menor valor entre o valor da despesa (em BRL) e esse limite. Hospedagem **nunca** tem limite ampliado. Duas hospedagens que cobrem o mesmo dia são limitadas cada uma isoladamente, sem verificação de sobreposição. Se o número de noites não puder ser identificado (ausente, zero, escrito por extenso, ou com mais de uma indicação na mesma descrição), a despesa fica `em_revisao` com motivo `NOITES_NAO_IDENTIFICADAS`.
**Origem:** política do RH, itens 3 e 4 (AMB-004, AMB-013)
**Aceite** (tabela padrão: hospedagem 250/noite):
- R$ 480,00 com "2 diarias" → limite 500,00 → reembolsa 480,00.
- R$ 750,00 com "3 noites" → limite 750,00 → reembolsa 750,00.
- R$ 900,00 com "3 noites" → reembolsa 750,00.
- R$ 300,00 com "Hotel centro" → `em_revisao`, `NOITES_NAO_IDENTIFICADAS`.
- "Hotel 2diarias" (sem espaço) → 2 noites.
- "Hotel 2 noites + 1 diaria" (duas indicações) → `em_revisao`, `NOITES_NAO_IDENTIFICADAS`.
- `CC-COMERCIAL` (400/noite), R$ 1.200,00 com "3 noites" → limite 1.200,00 → reembolsa 1.200,00.

### RN-006 — Nota fiscal obrigatória
**Regra:** toda despesa com valor em BRL **maior ou igual** ao piso `nota_fiscal_obrigatoria_acima_de` da política exige nota fiscal. A comparação usa o valor de **cada despesa**, já convertido (RN-014) e arredondado (RN-002), e não o valor reembolsável nem a soma do dia. Sem nota fiscal, a despesa fica `em_revisao` com motivo `NF_AUSENTE`.
**Origem:** política do RH, item 5 (AMB-006, AMB-007, AMB-008, AMB-009, AMB-027, AMB-034)
**Aceite** (piso 100,00):
- R$ 100,00 sem NF → `em_revisao`.
- R$ 99,99 sem NF → não exige NF.
- Hospedagem de R$ 690,00 sem NF → `em_revisao`, mesmo que o reembolso possível fosse menor.
- Dois táxis de R$ 60,00 e R$ 50,00 no mesmo dia, sem NF → nenhum dos dois exige NF.
- USD 40,00 a 5,50 = R$ 220,00, sem NF → `em_revisao`, `NF_AUSENTE`.

### RN-007 — Colaborador em viagem
**Regra:** um dia é "de viagem" quando está coberto por uma noite de hospedagem. Uma hospedagem com data D e N noites cobre os dias D, D+1, …, D+N−1; o dia da saída (D+N) **não** é de viagem. Uma hospedagem conta para a viagem sempre que o seu número de noites é identificável (RN-005), mesmo que ela própria esteja `em_revisao` ou `recusada` por outro motivo (ex.: falta de NF, ou hospedagem não reembolsável no centro de custo). Em dias de viagem, os limites das categorias de periodicidade `dia` são ampliados pelo `acrescimo_em_viagem_percentual` da política (RN-004). A hospedagem não é ampliada.
**Origem:** política do RH, item 6 (AMB-011, AMB-012, AMB-013, AMB-024)
**Aceite** (tabela padrão, acréscimo 50%):
- Hospedagem em 14/07 com 2 noites → os dias 14/07 e 15/07 são de viagem e 16/07 não é.
- Alimentação de R$ 85,00 em 15/07 → reembolsa 85,00 (limite de 90,00).
- Alimentação de R$ 85,00 em 16/07 → reembolsa 60,00.
- `CC-ENG-PLATAFORMA`: hospedagem recusada em 14/07 com 2 noites → 15/07 continua dia de viagem (limite de alimentação 75 × 1,5 = 112,50).

### RN-008 — Fim de semana ou plantão
**Regra:** os limites das categorias de periodicidade `dia` também são ampliados, pelo mesmo percentual da RN-007, nas despesas cuja data cai em sábado ou domingo, ou cuja descrição contém a palavra "plantão" (sem diferenciar maiúsculas, minúsculas ou acentos). O aumento **não acumula** com o da RN-007: o limite ampliado é aplicado no máximo uma vez. Hospedagem não é ampliada.
**Origem:** decisão do responsável pelo projeto; **não consta na política do RH** (AMB-014, AMB-015, AMB-026)
**Aceite** (tabela padrão, acréscimo 50%):
- Alimentação de R$ 85,00 num sábado → reembolsa 85,00.
- Alimentação de R$ 85,00 numa quarta-feira com descrição "Almoco - plantao" → reembolsa 85,00.
- Alimentação de R$ 100,00 num sábado que também é dia de viagem → reembolsa 90,00 (não 120,00).
- `CC-COMERCIAL`, representação de R$ 400,00 num sábado → limite 450,00 → reembolsa 400,00.

**Grupo com despesas de origens diferentes:** se ao menos uma despesa do grupo tem "plantão" na descrição, o grupo inteiro usa o limite ampliado.

**Motivo informado na saída:** quando o dia é ao mesmo tempo de viagem e de fim de semana/plantão, o campo `limite_ampliado` informa `viagem`. A viagem vem da política do RH (item 6), e esta regra é adicional.
- Alimentação num sábado que também é dia de viagem → `limite_ampliado` = `viagem`, limite 90,00.

### RN-009 — Duplicatas
**Regra:** duas ou mais despesas com todos os campos iguais, exceto o `id` (data, categoria após a RN-001, descrição, fornecedor, valor e moeda como informados, e nota fiscal), são duplicatas. `moeda` ausente equivale a `BRL`. **Todas** as cópias ficam `em_revisao` com motivo `DUPLICATA` e não são reembolsadas até que a revisão as resolva.
**Origem:** política do RH, item 8 (AMB-016, AMB-035)
**Aceite:**
- Duas despesas idênticas de R$ 54,90 com IDs diferentes → as duas ficam `em_revisao` e nenhum grupo é formado com elas.
- 22,00 EUR e 22,00 USD com os demais campos iguais → **não** são duplicatas.
- 22,00 sem `moeda` e 22,00 `BRL` com os demais campos iguais → são duplicatas.

### RN-010 — Período de competência
**Regra:** a despesa pertence ao período quando a sua `data` está entre `periodo.inicio` e `periodo.fim`, **incluindo os dois extremos**. Como não há data de lançamento na entrada, a data da despesa é o critério. Uma despesa fora do período fica `em_revisao` com motivo `FORA_DO_PERIODO`.
**Origem:** política do RH, item 7 (AMB-017, AMB-018)
**Aceite:**
- Período de 01/07 a 31/07: despesa de 31/07 → dentro do período.
- Despesa de 15/04 → `em_revisao`, `FORA_DO_PERIODO`.

### RN-011 — Estorno (valor negativo)
**Regra:** uma despesa com valor negativo é um estorno. Em estorno em moeda estrangeira, o valor é convertido pela RN-014 antes de qualquer comparação (AMB-036). Em categorias de periodicidade `dia`, ele abate do grupo da mesma categoria no mesmo dia (RN-004), entrando na soma do grupo antes da aplicação do limite. Estorno de hospedagem vai sempre para revisão (`ESTORNO_SEM_DESPESA`), pois hospedagem não é agrupada por dia. Os estornos de um mesmo dia e categoria são avaliados **em conjunto**: se a soma das despesas aprovadas (não negativas) desse dia e categoria for menor que o valor absoluto da **soma dos estornos**, **todos** os estornos daquele dia e categoria ficam `em_revisao` com motivo `ESTORNO_SEM_DESPESA` e não entram em nenhum grupo.
**Origem:** dados de entrada; a política não trata estornos (AMB-019, AMB-036)
**Aceite** (tabela padrão):
- Transporte de R$ 100,00 com NF + estorno de −R$ 45,00 no mesmo dia → `valor_total` 55,00, reembolsa 55,00.
- Estorno de −R$ 45,00 sozinho no dia → `em_revisao`, `ESTORNO_SEM_DESPESA`.
- Táxi de R$ 50,00 + estornos de −R$ 45,00 e −R$ 45,00 no mesmo dia → os dois estornos ficam `em_revisao`, `ESTORNO_SEM_DESPESA`; o grupo tem só o táxi (reembolsa 50,00).
- Táxi de R$ 100,00 (NF) + estornos de −R$ 30,00 e −R$ 20,00 → `valor_total` 50,00, reembolsa 50,00.
- Estorno de transporte de −R$ 45,00 com data fora do período → `em_revisao`, motivos `FORA_DO_PERIODO` e `ESTORNO_SEM_DESPESA`.
- Transporte de USD 50,00 (NF) + estorno de −USD 10,00 em 20/07 (cotação 5,50) → 275,00 − 55,00 = `valor_total` 220,00 → reembolsa 80,00.

**Estorno que já tem outro motivo:** só um estorno **aprovado** pelas demais verificações pode ser abatido. Um estorno que já está em revisão ou recusado por outro motivo também recebe `ESTORNO_SEM_DESPESA`, porque não será abatido de nenhum grupo; a saída lista os dois motivos (RN-012).

### RN-012 — Status da despesa
**Regra:**
- `recusada`: a despesa nunca será reembolsada (RN-003).
- `em_revisao`: há pendência a resolver por um humano. Enquanto isso, o reembolso é R$ 0,00 e a despesa **não** entra em nenhum grupo, ou seja, não consome limite.
- `aprovada`: a despesa entra no seu grupo.

Uma despesa pode ter vários motivos e a saída lista todos. Se um dos motivos leva a `recusada`, o status é `recusada`; senão, havendo qualquer motivo, é `em_revisao`.
**Origem:** organização das demais regras
**Aceite:** uma despesa `coworking` de 15/04 → status `recusada`, motivos `CATEGORIA_FORA_DA_POLITICA` e `FORA_DO_PERIODO`.

### RN-013 — Tabela de limites por centro de custo
**Regra:** a tabela de limites do colaborador é a do seu `centro_custo` no arquivo de política, comparando o nome **exatamente**, ignorando apenas espaços no início e no fim (AMB-025). Se o centro de custo não tem entrada, aplica-se a tabela `padrao`. Quando o centro de custo **tem** entrada, vale **somente** a tabela dele: uma categoria que falta nela **não** é buscada na tabela padrão (AMB-021). O piso da nota fiscal e o percentual de ampliação vêm do arquivo de política e valem para todos os centros de custo (AMB-027). A `vigencia` do arquivo não é verificada (AMB-029).
**Origem:** política v4, item A
**Aceite:**
- `CC-COMERCIAL` → tabela do `CC-COMERCIAL` (alimentação 90, transporte 150, hospedagem 400, representação 300); `resumo.tabela_aplicada` = `CC-COMERCIAL`.
- `CC-SUPORTE-N2` (sem entrada) → tabela `padrao` (60, 80, 250); `resumo.tabela_aplicada` = `padrao`.
- `CC-ADM`, hospedagem → `recusada` (o `CC-ADM` tem entrada, e ela não tem hospedagem).
- `cc-comercial` (minúsculas) → **não** é o `CC-COMERCIAL`; usa a tabela `padrao`.

### RN-014 — Conversão de moeda
**Regra:** despesa sem `moeda`, ou com `moeda` = `BRL`, não é convertida. Nas demais, o valor em BRL é o valor da entrada × a cotação da moeda na **data da despesa**, segundo o arquivo de câmbio. Se não há cotação nessa data (ex.: fim de semana ou feriado), usa-se a **última cotação anterior** disponível no arquivo para aquela moeda (AMB-031). O resultado é arredondado pela RN-002. Se não existe nenhuma cotação da moeda na data ou antes dela — moeda ausente do arquivo (AMB-030) ou data anterior à primeira cotação (AMB-032) —, a despesa fica `em_revisao` com motivo `CAMBIO_INDISPONIVEL`. Nesse caso o valor em BRL é desconhecido, e as regras que dependem dele (RN-006 e o abatimento da RN-011) não são avaliadas para essa despesa. O código da moeda é comparado exatamente como informado.
**Origem:** política v4, item B (AMB-030 a AMB-034, AMB-036)
**Aceite** (`exemplos/envelope/cambio.json`):
- EUR 22,00 em 14/07 (cotação 5,93) → R$ 130,46.
- EUR 30,00 em 18/07, sábado, sem cotação → cotação de 17/07 (5,96) → R$ 178,80; `data_cotacao` = 2026-07-17.
- GBP 55,00 → `em_revisao`, `CAMBIO_INDISPONIVEL`; `valor` = `null`.
- USD em 10/07 (antes da primeira cotação, 13/07) → `em_revisao`, `CAMBIO_INDISPONIVEL`.
- USD em 30/07 (depois da última cotação, 28/07) → cotação de 28/07.
- Despesa sem `moeda` → tratada como BRL, sem campos de câmbio na saída.

---

## 6. Ambiguidades identificadas e decisões

### Unidade de aplicação

#### AMB-001 — Limite por dia ou por despesa
**Texto original do RH:** "Alimentação tem limite de R$ 60 por dia." / "Transporte urbano tem limite de R$ 80 por dia."
**O que não está claro:** se o limite vale para a soma do dia ou para cada despesa isoladamente (ex.: dois almoços no mesmo dia).
**Decisão:** o limite vale para a soma das despesas aprovadas da categoria no dia.
**Justificativa:** o texto diz "por dia"; limitar por despesa permitiria contornar o teto dividindo o gasto.
**Regra afetada:** RN-004

#### AMB-002 — Como mostrar o corte quando o dia ultrapassa o limite
**Texto original do RH:** "Despesas acima do limite são reembolsadas parcialmente."
**O que não está claro:** com várias despesas no dia, de qual delas sai o corte.
**Decisão:** o corte não é atribuído a uma despesa. A saída lista cada despesa com o seu valor, e o grupo do dia mostra a soma e o valor reembolsado.
**Justificativa:** o limite é do dia, não de um item; atribuir o corte a um item específico seria arbitrário.
**Regra afetada:** RN-004, seção 4

#### AMB-003 — Sobra de limite entre dias
**Texto original do RH:** "R$ 60 por dia."
**O que não está claro:** se o limite não usado num dia pode ser usado em outro.
**Decisão:** não acumula.
**Justificativa:** "por dia" define um teto diário independente.
**Regra afetada:** RN-004

#### AMB-004 — Diárias em um único lançamento de hospedagem
**Texto original do RH:** "Hospedagem tem limite de R$ 250 por diária."
**O que não está claro:** a entrada não informa o número de diárias; um lançamento pode cobrir várias noites (d-010: "2 diarias"; d-013: "3 noites").
**Decisão:** o número de noites é lido da descrição, e o limite é o limite por noite × noites. Sem número identificável, ou com mais de uma indicação, a despesa vai para revisão.
**Justificativa:** limitar um lançamento de várias noites a uma diária puniria quem lança a estadia de uma vez; adivinhar o número de noites seria pior que revisar.
**Regra afetada:** RN-005

#### AMB-007 — Nota fiscal por despesa ou pela soma do dia
**Texto original do RH:** "Nota fiscal é obrigatória acima de R$ 100."
**O que não está claro:** se o valor comparado é o de cada despesa ou a soma do dia.
**Decisão:** é o valor de cada despesa.
**Justificativa:** a nota fiscal é um documento de cada compra, não do dia.
**Regra afetada:** RN-006

#### AMB-021 — Centro de custo com tabela incompleta (v4)
**Texto original do RH:** "Alguns centros de custo não têm entrada na tabela. Nesse caso, aplica-se a política padrão."
**O que não está claro:** o `CC-ADM` **tem** entrada, mas sem hospedagem. A política padrão vale só para o CC sem entrada, ou também para a categoria que falta numa entrada existente?
**Decisão:** só para o CC sem entrada. Se o CC tem entrada, vale apenas a tabela dele, e a categoria ausente é fora da política.
**Justificativa:** o texto condiciona a política padrão a "não ter entrada"; completar uma tabela existente com o padrão daria ao CC um benefício que o financeiro não listou.
**Regra afetada:** RN-003, RN-013

#### AMB-022 — Categoria nova numa tabela que não a tem (v4)
**Texto original do RH:** "`CC-COMERCIAL` tem uma categoria nova, `representacao`."
**O que não está claro:** um colaborador de outro CC (ou da tabela padrão, como o f-003) com despesa de `representacao`.
**Decisão:** categoria fora da política daquele CC: `recusada`.
**Justificativa:** a categoria foi criada para o `CC-COMERCIAL`; estendê-la a outros CCs seria inventar política.
**Regra afetada:** RN-003

#### AMB-026 — Quais categorias têm limite por dia e ampliação (v4)
**Texto original do RH:** "`representacao` [...] limite de R$ 300 por dia."
**O que não está claro:** a categoria nova segue as regras de limite diário (RN-004) e de ampliação em viagem e fim de semana (RN-007, RN-008), que antes valiam só para alimentação e transporte?
**Decisão:** sim. Toda categoria de periodicidade `dia` segue a RN-004 e é ampliada; toda de periodicidade `diaria` segue a RN-005 e não é ampliada.
**Justificativa:** a regra passa a depender da periodicidade declarada pelo financeiro, e não de uma lista fixa de categorias, o que absorve categorias novas sem mudar a spec.
**Regra afetada:** RN-004, RN-005, RN-007, RN-008

### Fronteira

#### AMB-005 — O que é "reembolsar parcialmente"
**Texto original do RH:** "Despesas acima do limite são reembolsadas parcialmente."
**O que não está claro:** se o sistema paga até o limite e corta o excedente, ou recusa o item inteiro.
**Decisão:** paga até o limite e o excedente não é reembolsado.
**Justificativa:** "parcialmente" exclui recusar tudo; pagar o teto é a leitura mais direta.
**Regra afetada:** RN-004, RN-005

#### AMB-006 — R$ 100,00 exatos exigem nota fiscal?
**Texto original do RH:** "Nota fiscal é obrigatória acima de R$ 100."
**O que não está claro:** se "acima" inclui o próprio R$ 100,00 (d-003 = 100,00; d-004 = 100,01).
**Decisão:** inclui. Valores ≥ piso exigem NF; valores abaixo do piso não exigem.
**Justificativa:** um valor redondo de R$ 100 já é relevante o bastante para exigir comprovante.
**Regra afetada:** RN-006

#### AMB-008 — Nota fiscal pelo valor gasto ou pelo valor reembolsável
**Texto original do RH:** "Nota fiscal é obrigatória acima de R$ 100."
**O que não está claro:** se o valor comparado é o gasto original ou o valor depois do limite (ex.: hospedagem de R$ 690 limitada).
**Decisão:** é o valor gasto, após o arredondamento.
**Justificativa:** a NF comprova o gasto realizado, não o que a empresa decide pagar.
**Regra afetada:** RN-006

#### AMB-009 — O que acontece quando falta a nota fiscal obrigatória
**Texto original do RH:** "Nota fiscal é obrigatória acima de R$ 100."
**O que não está claro:** se a despesa sem NF é recusada inteira, paga até R$ 100 ou fica pendente.
**Decisão:** fica em revisão, sem reembolso até ser resolvida.
**Justificativa:** a NF pode existir e não ter sido anexada; recusar em definitivo seria injusto, e pagar sem documento violaria a política.
**Regra afetada:** RN-006, RN-012

#### AMB-010 — Arredondamento de valores com mais de 2 casas
**Texto original do RH:** — (a política não fala de arredondamento; d-011 = 33,333)
**O que não está claro:** quando arredondar (na entrada ou no total) e como (meio para cima, bancário, truncar).
**Decisão:** na entrada, antes de qualquer regra de valor, arredondando em cadeia a partir do último dígito (0–4 para baixo, 5–9 para cima).
**Justificativa:** arredondar na entrada faz todas as regras operarem sobre os mesmos valores, em centavos, que aparecem na saída.
**Regra afetada:** RN-002

#### AMB-018 — O último dia do período entra?
**Texto original do RH:** "Despesas devem ser lançadas dentro do período de competência."
**O que não está claro:** se os extremos do período (d-014 = 31/07, igual ao `fim`) estão incluídos.
**Decisão:** os dois extremos estão incluídos.
**Justificativa:** o período de competência é o mês inteiro, e o último dia faz parte dele.
**Regra afetada:** RN-010

#### AMB-023 — Limite zero: "não reembolsa de forma alguma" (v4)
**Texto original do RH:** "`CC-ENG-PLATAFORMA` não reembolsa `hospedagem` de forma alguma." (no arquivo: limite 0)
**O que não está claro:** a despesa é recusada, ou aprovada com reembolso R$ 0,00?
**Decisão:** recusada, com `CATEGORIA_FORA_DA_POLITICA`. Limite zero equivale a categoria ausente da tabela.
**Justificativa:** "de forma alguma" é recusa; aprovar com R$ 0 sinalizaria ao colaborador algo que nunca será pago.
**Regra afetada:** RN-003

#### AMB-025 — Comparação do nome do centro de custo (v4)
**Texto original do RH:** — (a v4 não diz como o nome do CC da despesa é casado com o da tabela)
**O que não está claro:** `cc-comercial` ou ` CC-COMERCIAL ` são o mesmo CC?
**Decisão:** comparação exata, ignorando apenas espaços no início e no fim.
**Justificativa:** o nome do CC é um código mantido pelo financeiro, não texto livre; tolerar variações poderia aplicar a tabela errada.
**Regra afetada:** RN-013

#### AMB-027 — Piso da NF e percentual de ampliação na v4
**Texto original do RH:** arquivo: `nota_fiscal_obrigatoria_acima_de: 100`, `acrescimo_em_viagem_percentual: 50`
**O que não está claro:** se esses valores substituem os da v3, se "acima de" muda a fronteira da AMB-006 e se o percentual "em viagem" vale também para fim de semana/plantão (RN-008).
**Decisão:** os dois vêm do arquivo; a fronteira continua **≥** (AMB-006); o mesmo percentual vale para a RN-008.
**Justificativa:** o envelope manda ler a política de fora do código; manter a fronteira e o percentual únicos evita duas regras diferentes para o mesmo conceito.
**Regra afetada:** RN-006, RN-007, RN-008

#### AMB-033 — Ordem entre conversão e arredondamento (v4)
**Texto original do RH:** "Uma despesa em EUR é convertida antes de ser comparada ao limite."
**O que não está claro:** arredondar o valor original, converter e arredondar de novo, ou converter o valor exato e arredondar uma vez.
**Decisão:** converte o valor exato da entrada e aplica a RN-002 uma única vez, sobre o valor em BRL.
**Justificativa:** um único arredondamento evita erro acumulado (USD 10,005 × 5,42 dá 54,23, e não 54,25).
**Regra afetada:** RN-002, RN-014

#### AMB-034 — Nota fiscal em despesa estrangeira (v4)
**Texto original do RH:** "Os limites da política são sempre em BRL."
**O que não está claro:** o piso de R$ 100 da nota fiscal é comparado na moeda original ou em BRL (e-005: USD 40 ≈ R$ 220, sem NF)?
**Decisão:** em BRL, após a conversão.
**Justificativa:** o piso é um valor em reais, como os limites; comparar USD 40 com "100" ignoraria a moeda.
**Regra afetada:** RN-006

### Dado ausente

#### AMB-011 — O que caracteriza "em viagem"
**Texto original do RH:** "Colaborador em viagem tem limites ampliados em 50%."
**O que não está claro:** a entrada não tem campo de viagem.
**Decisão:** há viagem quando há hospedagem.
**Justificativa:** a hospedagem é o único dado da entrada que comprova pernoite fora de casa.
**Regra afetada:** RN-007

#### AMB-012 — Quais dias são de viagem
**Texto original do RH:** "Colaborador em viagem..."
**O que não está claro:** se a viagem cobre só a data da hospedagem, todas as noites ou também o dia da saída.
**Decisão:** cobre os dias das noites hospedadas; o dia da saída não conta.
**Justificativa:** a regra acompanha as noites efetivamente hospedadas; uma refeição no dia seguinte à última noite não caracteriza diária.
**Regra afetada:** RN-007

#### AMB-013 — Quais categorias têm o limite ampliado
**Texto original do RH:** "...tem limites ampliados em 50%."
**O que não está claro:** se a hospedagem também é ampliada (o que, como toda hospedagem implica viagem, a tornaria sempre 50% maior).
**Decisão:** hospedagem nunca é ampliada. Na v4, são ampliadas as categorias de periodicidade `dia` (AMB-026).
**Justificativa:** ampliar a hospedagem tornaria o limite por noite da política letra morta.
**Regra afetada:** RN-005, RN-007

#### AMB-014 — Fim de semana e plantão
**Texto original do RH:** — (a política não fala de dia da semana; d-012 = "Almoco de sabado - plantao")
**O que não está claro:** se trabalho em fim de semana ou plantão muda o limite.
**Decisão:** o limite é ampliado (exceto hospedagem) em sábados, domingos e despesas com "plantão" na descrição.
**Justificativa:** decisão do responsável pelo projeto: trabalho fora do expediente normal recebe o mesmo tratamento de viagem. **Esta regra não existe na política do RH e deve ser validada com o RH.**
**Regra afetada:** RN-008

#### AMB-015 — Viagem + fim de semana acumulam?
**Texto original do RH:** —
**O que não está claro:** se um dia de viagem que cai num sábado tem a ampliação uma ou duas vezes.
**Decisão:** não acumula; a ampliação é aplicada no máximo uma vez. Quando os dois se aplicam, a saída informa `viagem` como motivo da ampliação.
**Justificativa:** os dois aumentos compensam a mesma coisa (gasto fora da rotina), e somá-los dobraria o limite.
**Regra afetada:** RN-008

#### AMB-016 — Duplicatas: o que são e como tratar
**Texto original do RH:** "Duplicatas devem ser tratadas."
**O que não está claro:** qual critério define uma duplicata e o que fazer com ela (d-006 e d-007 são idênticas, com IDs diferentes).
**Decisão:** são duplicatas as despesas com todos os campos iguais exceto o ID. Todas as cópias vão para revisão, sem reembolso até a resolução.
**Justificativa:** o sistema não tem como saber qual cópia é legítima (ou se ambas são), e pagar uma delas poderia pagar a errada.
**Regra afetada:** RN-009

#### AMB-017 — Competência sem data de lançamento
**Texto original do RH:** "Despesas devem ser lançadas dentro do período de competência."
**O que não está claro:** a entrada não tem data de lançamento, só a data da despesa (d-008 = abril, "lançado com atraso").
**Decisão:** usa a data da despesa; fora do período, a despesa vai para revisão.
**Justificativa:** um lançamento atrasado pode ser legítimo, mas precisa de validação humana em vez de recusa automática.
**Regra afetada:** RN-010

#### AMB-019 — Estornos (valores negativos)
**Texto original do RH:** — (a política não trata estornos; d-009 = −45,00)
**O que não está claro:** se o estorno é ignorado, abatido do dia, abatido do período ou rejeitado.
**Decisão:** abate do grupo da mesma categoria e dia; sem despesa a abater, vai para revisão. Vários estornos no mesmo dia e categoria são avaliados pela soma: se juntos excedem as despesas, todos vão para revisão.
**Justificativa:** o estorno anula um gasto daquele dia; sem o gasto correspondente, não há como saber a qual despesa ele se refere.
**Regra afetada:** RN-011

#### AMB-020 — Grafia da categoria
**Texto original do RH:** "Categorias fora da política não são reembolsáveis."
**O que não está claro:** se `ALIMENTACAO` (d-014) é uma categoria diferente de `alimentacao`.
**Decisão:** é a mesma categoria; a comparação ignora maiúsculas, minúsculas e acentos.
**Justificativa:** diferença de digitação não muda a natureza do gasto.
**Regra afetada:** RN-001

#### AMB-024 — Hospedagem não reembolsável gera viagem? (v4)
**Texto original do RH:** "`CC-ENG-PLATAFORMA` não reembolsa `hospedagem` de forma alguma."
**O que não está claro:** se a hospedagem recusada ainda caracteriza viagem para ampliar alimentação e transporte (no exemplo original, d-010 cobre o dia de d-011).
**Decisão:** sim; a hospedagem com noites identificáveis gera dias de viagem mesmo recusada.
**Justificativa:** a viagem aconteceu; não reembolsar o hotel não muda o fato de o colaborador estar fora, como já decidido para hospedagem em revisão (D-001).
**Regra afetada:** RN-007

#### AMB-028 — Arquivos de política ou câmbio ausentes ou inválidos (v4)
**Texto original do RH:** "O motor precisa ler a política de fora, não de dentro do código."
**O que não está claro:** o que fazer se o arquivo não existir ou estiver malformado.
**Decisão:** erro, sem saída, como uma entrada inválida. Nunca usar valores embutidos.
**Justificativa:** cair para valores internos violaria a própria exigência da v4 e produziria reembolsos com a política errada sem ninguém perceber.
**Regra afetada:** seção 4

#### AMB-029 — Vigência da política (v4)
**Texto original do RH:** "Vigência imediata, retroativa à competência atual." (arquivo: `vigencia: 2026-07-01`)
**O que não está claro:** se uma competência anterior à vigência deve ser rejeitada.
**Decisão:** a vigência é informativa e não é verificada.
**Justificativa:** o próprio comunicado manda aplicar a v4 retroativamente; a escolha do arquivo correto é de quem executa.
**Regra afetada:** RN-013

#### AMB-030 — Moeda sem cotação no arquivo (v4)
**Texto original do RH:** "As taxas estão em `cambio.json`." (e-006 é GBP; o arquivo só tem USD e EUR)
**O que não está claro:** recusar, revisar ou rejeitar a entrada inteira.
**Decisão:** `em_revisao`, motivo `CAMBIO_INDISPONIVEL`.
**Justificativa:** a despesa pode ser legítima; falta um dado para calcular, o que pede revisão humana, e não recusa nem a perda das demais despesas.
**Regra afetada:** RN-014

#### AMB-031 — "Taxa da data da despesa" em dia sem cotação (v4)
**Texto original do RH:** "A conversão usa a taxa da data da despesa." (arquivo: "Cotações publicadas apenas em dias úteis bancários"; e-004 é de sábado, 18/07)
**O que não está claro:** usar a cotação anterior, a seguinte, ou mandar para revisão.
**Decisão:** a última cotação anterior disponível para aquela moeda.
**Justificativa:** é a prática do PTAX: em dia sem fechamento, vale o último fechamento publicado.
**Regra afetada:** RN-014

#### AMB-032 — Data sem cotação anterior no arquivo (v4)
**Texto original do RH:** —
**O que não está claro:** despesa antes da primeira data do arquivo (13/07) ou depois da última (28/07).
**Decisão:** antes da primeira → `CAMBIO_INDISPONIVEL`; depois da última → vale a última cotação anterior (AMB-031).
**Justificativa:** "última cotação anterior" só existe se houver alguma antes; depois da última data, a regra da AMB-031 já se aplica.
**Regra afetada:** RN-014

#### AMB-035 — Moeda no critério de duplicata (v4)
**Texto original do RH:** "Duplicatas devem ser tratadas."
**O que não está claro:** se 22,00 EUR e 22,00 USD com os demais campos iguais são duplicatas.
**Decisão:** não são; a moeda entra no critério, e ausência de moeda equivale a BRL.
**Justificativa:** "todos os campos iguais, exceto o ID" já inclui a moeda; os valores reais são diferentes.
**Regra afetada:** RN-009

#### AMB-036 — Estorno em moeda estrangeira (v4)
**Texto original do RH:** —
**O que não está claro:** se o estorno estrangeiro abate em BRL ou na moeda original.
**Decisão:** é convertido pela mesma regra e abate em BRL.
**Justificativa:** os grupos e limites são em BRL; comparar moedas diferentes não faria sentido.
**Regra afetada:** RN-011, RN-014

#### AMB-037 — Saída de despesa estrangeira (v4)
**Texto original do RH:** —
**O que não está claro:** como mostrar o valor original, a cotação e o valor em BRL.
**Decisão:** `valor` passa a ser sempre em BRL; despesas estrangeiras ganham `moeda`, `valor_original`, `taxa_cambio` e `data_cotacao`. Despesas em BRL ficam como na versão 1.x.
**Justificativa:** quem confere precisa refazer a conta (valor × taxa) e ver qual data de cotação foi usada; a saída das despesas em BRL não muda para quem já a consome.
**Regra afetada:** seção 4

---

## 7. Casos de borda

Salvo indicação na coluna "Entrada", os casos usam um centro de custo **sem
entrada** na tabela (política padrão: alimentação 60/dia, transporte 80/dia,
hospedagem 250/noite; acréscimo 50%; piso da NF 100) e o câmbio de
`exemplos/envelope/cambio.json`.

| Caso | Entrada | Comportamento esperado | Regra |
|---|---|---|---|
| Duas refeições no mesmo dia acima do limite | 72,50 + 38,00 em dia útil | Grupo: total 110,50, reembolso 60,00 | RN-004 |
| Exatamente R$ 100,00 sem NF | transporte 100,00, sem NF | `em_revisao`, `NF_AUSENTE` | RN-006 |
| Um centavo acima de 100 sem NF | transporte 100,01, sem NF | `em_revisao`, `NF_AUSENTE` | RN-006 |
| R$ 99,99 sem NF | alimentação 99,99, sem NF | Não exige NF; segue o limite | RN-006 |
| Categoria fora da política | coworking 89,00 | `recusada`, `CATEGORIA_FORA_DA_POLITICA` | RN-003 |
| Duplicata exata | dois almoços idênticos de 54,90 | Ambos `em_revisao`, `DUPLICATA` | RN-009 |
| Fora do período | data 15/04, período em julho | `em_revisao`, `FORA_DO_PERIODO` | RN-010 |
| Último dia do período | data = `periodo.fim` | Dentro do período | RN-010 |
| Estorno sem despesa no dia | transporte −45,00 sozinho | `em_revisao`, `ESTORNO_SEM_DESPESA` | RN-011 |
| Estorno com despesa no dia | transporte 100,00 (NF) + −45,00 | Total 55,00, reembolso 55,00 | RN-011 |
| Dois estornos que juntos excedem o dia | transporte 50,00 + −45,00 + −45,00 | Os dois estornos `em_revisao`; reembolso 50,00 | RN-011 |
| Hospedagem com várias noites | 480,00, "2 diarias", com NF | Limite 500,00, reembolso 480,00 | RN-005 |
| Hospedagem sem número de noites | 300,00, "Hotel centro", com NF | `em_revisao`, `NOITES_NAO_IDENTIFICADAS` | RN-005 |
| Noites sem espaço | "Hotel 2diarias" | 2 noites | RN-005 |
| Mais de uma indicação de noites | "Hotel 2 noites + 1 diaria", com NF | `em_revisao`, `NOITES_NAO_IDENTIFICADAS` | RN-005 |
| Valor com 3 casas | 33,333 | Tratado como 33,33 | RN-002 |
| Categoria em maiúsculas | `ALIMENTACAO` | Tratada como `alimentacao` | RN-001 |
| Categoria com acento | `alimentação` | Tratada como `alimentacao` | RN-001 |
| Estorno de hospedagem | hospedagem −100,00 | `em_revisao`, `ESTORNO_SEM_DESPESA` | RN-011 |
| Estorno já em revisão por outro motivo | transporte −45,00 com data 15/04, período em julho | `em_revisao`, `FORA_DO_PERIODO` e `ESTORNO_SEM_DESPESA` | RN-011, RN-012 |
| Dia de viagem | alimentação 85,00 em noite de hospedagem | Limite 90,00, reembolso 85,00 | RN-007 |
| Dia de saída da hospedagem | alimentação 85,00 no dia seguinte à última noite | Limite 60,00, reembolso 60,00 | RN-007 |
| Sábado | alimentação 85,00 | Limite 90,00, reembolso 85,00 | RN-008 |
| Sábado em viagem | alimentação 100,00 | Limite 90,00 (não acumula), reembolso 90,00, `limite_ampliado` = `viagem` | RN-008 |
| Centro de custo com tabela própria | `CC-COMERCIAL`, alimentação 95,00 em dia útil | Limite 90,00, reembolso 90,00; `tabela_aplicada` = `CC-COMERCIAL` | RN-013 |
| Categoria ausente da tabela do CC | `CC-ADM`, hospedagem 200,00, "1 diaria" | `recusada`, `CATEGORIA_FORA_DA_POLITICA` | RN-003, RN-013 |
| Categoria com limite zero | `CC-ENG-PLATAFORMA`, hospedagem 480,00, "2 diarias" | `recusada`, `CATEGORIA_FORA_DA_POLITICA` | RN-003 |
| Hospedagem recusada ainda gera viagem | `CC-ENG-PLATAFORMA`, hospedagem 14/07 "2 diarias" + alimentação 100,00 (NF) em 15/07 | Hospedagem `recusada`; alimentação: limite 112,50, reembolso 100,00 | RN-007 |
| Representação no CC-COMERCIAL | `CC-COMERCIAL`, representação 340,00 em dia útil | Limite 300,00, reembolso 300,00 | RN-004 |
| Representação fora do CC-COMERCIAL | representação 190,00 | `recusada`, `CATEGORIA_FORA_DA_POLITICA` | RN-003 |
| Representação no sábado | `CC-COMERCIAL`, representação 400,00 num sábado | Limite 450,00, reembolso 400,00 | RN-008 |
| Nome do CC com outra grafia | centro de custo `cc-comercial`, alimentação 95,00 | Tabela `padrao`: limite 60,00 | RN-013 |
| Despesa sem moeda | alimentação 40,00 sem o campo `moeda` | Tratada como BRL; sem campos de câmbio na saída | RN-014 |
| Moeda sem cotação no arquivo | GBP 55,00 | `em_revisao`, `CAMBIO_INDISPONIVEL`; `valor` = `null` | RN-014 |
| Despesa em dia sem cotação | EUR 30,00 em 18/07 (sábado) | Cotação de 17/07 (5,96) → 178,80 | RN-014 |
| Data anterior à primeira cotação | USD 20,00 em 10/07 | `em_revisao`, `CAMBIO_INDISPONIVEL` | RN-014 |
| Conversão antes do arredondamento | USD 10,005 em 13/07 (cotação 5,42) | 54,23 (não 54,25) | RN-002, RN-014 |
| Nota fiscal em moeda estrangeira | USD 40,00 em 20/07 (5,50) = 220,00, sem NF | `em_revisao`, `NF_AUSENTE` | RN-006 |
| Estorno em moeda estrangeira | transporte USD 50,00 (NF) + −USD 10,00 em 20/07 | Total 220,00, reembolso 80,00 | RN-011, RN-014 |
| Mesmo valor em moedas diferentes | EUR 22,00 e USD 22,00, demais campos iguais | Não são duplicatas | RN-009 |

## 8. Ordem de aplicação das regras

0. **Tabela de limites** do colaborador (RN-013).
1. **Conversão** para BRL (RN-014), **arredondamento** de todos os valores (RN-002) e **normalização** das categorias (RN-001).
2. **Verificações por despesa**, que podem gerar motivos de revisão ou recusa. Todas são avaliadas e todos os motivos são listados:
   câmbio (RN-014) · categoria (RN-003) · período (RN-010) · duplicata (RN-009) · nota fiscal (RN-006, só com valor em BRL conhecido) · noites de hospedagem (RN-005).
3. **Status** de cada despesa (RN-012). Despesas `em_revisao` e `recusadas` saem do cálculo e **não consomem limite**.
4. **Dias de viagem** (RN-007), a partir das hospedagens com número de noites identificável, mesmo as que estão em revisão ou recusadas.
5. **Estornos** (RN-011): abatidos dos grupos, ou enviados para revisão. Estornos que já têm outro motivo também recebem `ESTORNO_SEM_DESPESA`.
6. **Grupos e limites** (RN-004, RN-005, RN-008): soma das despesas aprovadas, limite (base ou ampliado), reembolso = menor valor entre os dois.
7. **Resumo**: soma dos reembolsos dos grupos.

## 9. Critérios de aceite

Todos os casos abaixo usam `exemplos/envelope/politica-v4.json` e `exemplos/envelope/cambio.json`.

### 9.1 `exemplos/despesas-exemplo.json` — `CC-ENG-PLATAFORMA`

Tabela do CC: alimentação 75/dia, transporte 80/dia, hospedagem **0** (não reembolsável).

| Despesa | Status | Motivo(s) |
|---|---|---|
| d-001, d-002 | aprovada | — |
| d-003, d-004 | em_revisao | `NF_AUSENTE` |
| d-005 | recusada | `CATEGORIA_FORA_DA_POLITICA` |
| d-006, d-007 | em_revisao | `DUPLICATA` |
| d-008 | em_revisao | `FORA_DO_PERIODO` |
| d-009 | em_revisao | `ESTORNO_SEM_DESPESA` |
| d-010 | recusada | `CATEGORIA_FORA_DA_POLITICA` |
| d-011 | aprovada | — |
| d-012 | aprovada | — |
| d-013 | recusada | `CATEGORIA_FORA_DA_POLITICA`, `NF_AUSENTE` |
| d-014 | aprovada | — |

| Grupo | Total | Limite | Ampliação | Reembolso |
|---|---|---|---|---|
| 03/07 alimentação (d-001, d-002) | 110,50 | 75,00 | nenhum | 75,00 |
| 15/07 alimentação (d-011) | 33,33 | 112,50 | viagem | 33,33 |
| 18/07 alimentação (d-012, sábado) | 47,20 | 112,50 | fim_de_semana_ou_plantao | 47,20 |
| 31/07 alimentação (d-014) | 61,00 | 75,00 | nenhum | 61,00 |

- [ ] `tabela_aplicada` = `CC-ENG-PLATAFORMA`; `total_reembolsado` = **216,53**; quantidade: 5 aprovadas, 6 em revisão, 3 recusadas.

### 9.2 `exemplos/envelope/despesas-envelope.json` — `CC-COMERCIAL`

Tabela do CC: alimentação 90/dia, transporte 150/dia, hospedagem 400/noite, representação 300/dia. Dias de viagem: 22, 23 e 24/07 (e-007).

| Despesa | Conversão | Status | Motivo(s) |
|---|---|---|---|
| e-001 | — | aprovada | — |
| e-002 | EUR 22,00 × 5,93 (14/07) = 130,46 | aprovada | — |
| e-003 | EUR 14,50 × 5,88 (15/07) = 85,26 | aprovada | — |
| e-004 | EUR 30,00 × 5,96 (17/07; 18/07 é sábado) = 178,80 | aprovada | — |
| e-005 | USD 40,00 × 5,50 (20/07) = 220,00 | em_revisao | `NF_AUSENTE` |
| e-006 | GBP — sem cotação | em_revisao | `CAMBIO_INDISPONIVEL` |
| e-007 | — | aprovada | — |
| e-008 | — | aprovada | — |
| e-009 | — | recusada | `CATEGORIA_FORA_DA_POLITICA` |
| e-010 | — (sem `moeda` = BRL) | aprovada | — |

| Grupo | Total | Limite | Ampliação | Reembolso |
|---|---|---|---|---|
| 13/07 representação (e-001) | 340,00 | 300,00 | nenhum | 300,00 |
| 14/07 alimentação (e-002) | 130,46 | 90,00 | nenhum | 90,00 |
| 15/07 alimentação (e-003) | 85,26 | 90,00 | nenhum | 85,26 |
| 18/07 alimentação (e-004, sábado) | 178,80 | 135,00 | fim_de_semana_ou_plantao | 135,00 |
| 22/07 hospedagem (e-007, 3 noites) | 1.200,00 | 1.200,00 | nenhum | 1.200,00 |
| 23/07 alimentação (e-008) | 95,00 | 135,00 | viagem | 95,00 |
| 27/07 alimentação (e-010) | 88,00 | 90,00 | nenhum | 88,00 |

- [ ] `tabela_aplicada` = `CC-COMERCIAL`; `total_reembolsado` = **1.993,26**; quantidade: 7 aprovadas, 2 em revisão, 1 recusada.

### 9.3 `exemplos/envelope/despesas-envelope-cc-desconhecido.json` — `CC-SUPORTE-N2`

Sem entrada na tabela → tabela `padrao` (alimentação 60, transporte 80, hospedagem 250; sem representação). Dia de viagem: 17/07 (f-002).

| Despesa | Conversão | Status | Motivo(s) |
|---|---|---|---|
| f-001 | — | aprovada | — |
| f-002 | — | aprovada | — |
| f-003 | — | recusada | `CATEGORIA_FORA_DA_POLITICA` |
| f-004 | USD 12,00 × 5,48 (21/07) = 65,76 | aprovada | — |

| Grupo | Total | Limite | Ampliação | Reembolso |
|---|---|---|---|---|
| 16/07 alimentação (f-001) | 58,00 | 60,00 | nenhum | 58,00 |
| 17/07 hospedagem (f-002, 1 noite) | 310,00 | 250,00 | nenhum | 250,00 |
| 21/07 transporte urbano (f-004) | 65,76 | 80,00 | nenhum | 65,76 |

- [ ] `tabela_aplicada` = `padrao`; `total_reembolsado` = **373,76**; quantidade: 3 aprovadas, 0 em revisão, 1 recusada.

### 9.4 Critérios gerais

- [ ] Toda despesa da entrada aparece na saída, na mesma ordem, com status, motivos e justificativa.
- [ ] Todos os valores monetários da saída têm exatamente 2 casas decimais (exceto `valor` = `null` com `CAMBIO_INDISPONIVEL`, e `valor_original`/`taxa_cambio`, que repetem os arquivos de entrada).
- [ ] Cada caso da seção 7 produz o comportamento descrito.
- [ ] Executar duas vezes a mesma entrada produz exatamente a mesma saída.
- [ ] Arquivo de despesas, política ou câmbio malformado ou ausente é rejeitado com mensagem de erro e sem gerar saída.

## 10. O que fica em aberto

1. **RN-008 (fim de semana/plantão)** não vem da política do RH. Deve ser validada com o RH antes do uso em produção.
2. **Item C da v4 (fila de aprovação acima de R$ 500)** não foi especificado nem implementado, por decisão do responsável: é opcional, e há uma ambiguidade estrutural — o reembolso desta spec é calculado por **grupo**, não por item, então "valor reembolsável do item" não existe hoje. Especificá-lo exigiria decidir antes como repartir o reembolso de um grupo entre os seus itens (o que a AMB-002 decidiu não fazer).
3. **Decisões provisórias tomadas ao redigir a versão 2.0, ainda não confirmadas pelo responsável:**
   - Despesa com `CAMBIO_INDISPONIVEL` sai com `valor` = `null`, e as regras que dependem do valor em BRL (nota fiscal, abatimento de estorno) não são avaliadas para ela (RN-014).
   - O campo de saída `data_cotacao` e o campo `resumo.tabela_aplicada` (seção 4), acrescentados para que se possa auditar a cotação usada (AMB-031) e a tabela escolhida (RN-013).
   - O código da moeda é comparado exatamente como informado (`usd` em minúsculas não é encontrado e cai em `CAMBIO_INDISPONIVEL`).
   - A categoria é reembolsável por ter limite > 0 na tabela; o tipo de regra (diária ou por noite) vem da `periodicidade`, e não do nome da categoria (AMB-026).

As decisões provisórias da versão 1.0 foram confirmadas pelo responsável e incorporadas às regras na versão 1.1 (ver `DECISIONS.md`, D-001).
