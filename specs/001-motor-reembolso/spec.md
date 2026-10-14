# Spec — Motor de Cálculo de Reembolso

**Versão:** 1.3 · **Status:** rascunho · **Última alteração:** 2026-10-14

> **Regra de ouro deste arquivo:** ele descreve o QUÊ e o PORQUÊ. Nenhuma linha
> aqui pode citar linguagem, biblioteca, classe, função ou estrutura de pasta.
> Se apareceu solução, o lugar dela é o `plan.md`.
>
> **Teste de aceitação da própria spec:** uma pessoa que nunca viu o projeto
> consegue, lendo só este arquivo, verificar se o sistema está correto?

---

## 1. Problema

Hoje o financeiro confere manualmente, item por item, cada despesa dos
colaboradores contra a política de reembolso do RH. O processo é lento, e a
política é ambígua, então pessoas diferentes chegam a valores diferentes para a
mesma despesa.

## 2. Objetivo

Dado o conjunto de despesas de um colaborador em um período de competência, o
sistema calcula de forma determinística quanto é reembolsável e justifica cada
decisão, aplicando a política conforme interpretada nesta spec.

## 3. Fora de escopo

- Não efetua pagamento nem se integra a sistemas de folha ou financeiro.
- Não resolve pendências: despesas "em revisão" são apenas sinalizadas, e a revisão é humana.
- Não lê nem valida o conteúdo de notas fiscais. Confia no campo que informa se há NF.
- Não trata moedas além do real (R$).
- Não processa mais de um colaborador ou mais de um período por execução.
- Não guarda histórico entre execuções. Duplicatas só são detectadas dentro do mesmo arquivo de entrada.
- Não interpreta o texto da descrição, exceto para dois fins: o número de noites de hospedagem (RN-005) e a palavra "plantão" (RN-008).
- Não aplica regras por cargo, centro de custo ou cliente. Esses campos são apenas repassados para a saída.

## 4. Entrada e saída

### Entrada

Conforme `exemplos/despesas-exemplo.json`.

| Campo | Tipo | Significado | Obrigatório |
|---|---|---|---|
| `colaborador.id` | texto | Identificador do colaborador | sim |
| `colaborador.nome` | texto | Nome do colaborador | sim |
| `colaborador.centro_custo` | texto | Centro de custo (apenas repassado) | sim |
| `periodo.competencia` | texto `AAAA-MM` | Mês de competência | sim |
| `periodo.inicio` | data `AAAA-MM-DD` | Primeiro dia do período (inclusivo) | sim |
| `periodo.fim` | data `AAAA-MM-DD` | Último dia do período (inclusivo) | sim |
| `despesas[].id` | texto | Identificador único da despesa | sim |
| `despesas[].data` | data `AAAA-MM-DD` | Dia em que a despesa ocorreu | sim |
| `despesas[].categoria` | texto | Categoria; comparada sem diferenciar maiúsculas, minúsculas e acentos (RN-001) | sim |
| `despesas[].descricao` | texto | Descrição livre | sim |
| `despesas[].fornecedor` | texto | Fornecedor | sim |
| `despesas[].valor` | número | Valor em R$. Pode ter mais de 2 casas decimais (RN-002) e pode ser negativo, no caso de estorno (RN-011) | sim |
| `despesas[].tem_nota_fiscal` | booleano | Se há nota fiscal | sim |

Uma entrada que não respeita este formato (campo obrigatório ausente, tipo
errado, data inválida) não é processada: o sistema informa o erro e não gera
saída.

### Saída

| Campo | Tipo | Significado |
|---|---|---|
| `colaborador` | objeto | Cópia do bloco de entrada |
| `periodo` | objeto | Cópia do bloco de entrada |
| `despesas[]` | lista | Uma linha por despesa de entrada, na mesma ordem |
| `despesas[].id` | texto | ID da despesa |
| `despesas[].data` | data | Data da despesa |
| `despesas[].categoria` | texto | Categoria normalizada, em minúsculas e sem acentos |
| `despesas[].valor` | número (2 casas) | Valor após o arredondamento da RN-002 |
| `despesas[].status` | `aprovada` \| `em_revisao` \| `recusada` | Situação da despesa (RN-012) |
| `despesas[].motivos` | lista de códigos | Motivos de revisão ou recusa; vazia se aprovada (tabela abaixo) |
| `despesas[].justificativa` | texto | Explicação legível da decisão |
| `grupos[]` | lista | Agrupamentos onde o limite é aplicado (RN-004 e RN-005) |
| `grupos[].data` | data | Dia do grupo |
| `grupos[].categoria` | texto | Categoria do grupo |
| `grupos[].despesas` | lista de IDs | Despesas **aprovadas** que compõem o grupo |
| `grupos[].noites` | inteiro | Somente em hospedagem: número de noites (RN-005) |
| `grupos[].valor_total` | número (2 casas) | Soma dos valores das despesas do grupo |
| `grupos[].limite` | número (2 casas) | Limite aplicado ao grupo |
| `grupos[].limite_ampliado` | `nenhum` \| `viagem` \| `fim_de_semana_ou_plantao` | Por que o limite foi (ou não) ampliado (RN-007, RN-008) |
| `grupos[].valor_reembolsado` | número (2 casas) | Menor valor entre `valor_total` e `limite` |
| `resumo.total_reembolsado` | número (2 casas) | Soma de `valor_reembolsado` de todos os grupos |
| `resumo.quantidade` | objeto | Quantidade de despesas por status |

**Códigos de motivo:**

| Código | Status resultante | Regra |
|---|---|---|
| `CATEGORIA_FORA_DA_POLITICA` | recusada | RN-003 |
| `FORA_DO_PERIODO` | em_revisao | RN-010 |
| `DUPLICATA` | em_revisao | RN-009 |
| `NF_AUSENTE` | em_revisao | RN-006 |
| `NOITES_NAO_IDENTIFICADAS` | em_revisao | RN-005 |
| `ESTORNO_SEM_DESPESA` | em_revisao | RN-011 |

**Exemplo** (entrada com duas refeições no mesmo dia útil, sem viagem):

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
  "resumo": { "total_reembolsado": 60.00,
              "quantidade": { "aprovada": 2, "em_revisao": 0, "recusada": 0 } }
}
```

## 5. Regras de negócio

### RN-001 — Normalização de categoria
**Regra:** a categoria é comparada sem diferenciar maiúsculas, minúsculas e acentos. `ALIMENTACAO`, `Alimentação` e `alimentacao` são a mesma categoria.
**Origem:** dados de entrada (AMB-020)
**Aceite:** despesas com categoria `ALIMENTACAO` ou `alimentação` são tratadas como alimentação e aparecem na saída como `alimentacao`.

### RN-002 — Arredondamento
**Regra:** todo valor de entrada com mais de 2 casas decimais é arredondado para 2 casas **antes de qualquer outra regra**. O arredondamento é feito em cadeia, do último dígito para a esquerda, até restarem 2 casas: um dígito de 0 a 4 é descartado; de 5 a 9, soma 1 à casa anterior.
Valores negativos são arredondados pelo valor absoluto, mantendo o sinal.
**Origem:** dados de entrada (AMB-010)
**Aceite:** 33,333 → 33,33 · 3,444 → 3,44 · 3,445 → 3,45 · 3,4449 → 3,445 → 3,45 · −3,445 → −3,45.

### RN-003 — Categorias elegíveis
**Regra:** apenas `alimentacao`, `transporte_urbano` e `hospedagem` são reembolsáveis. Qualquer outra categoria resulta em status `recusada`, motivo `CATEGORIA_FORA_DA_POLITICA`, e reembolso de R$ 0,00.
**Origem:** política do RH, item 9
**Aceite:** uma despesa `coworking` de R$ 89,00 tem status `recusada` e não entra em nenhum grupo.

### RN-004 — Limite diário de alimentação e transporte
**Regra:** as despesas aprovadas de alimentação de um mesmo dia formam um grupo, e as de transporte urbano também. O reembolso do grupo é o menor valor entre a soma do grupo e o limite do dia. O excedente não é reembolsado. O limite não usado em um dia **não acumula** para outros dias. Despesas de valor zero são aprovadas normalmente e não alteram o reembolso.
- Limite base: alimentação R$ 60,00/dia; transporte urbano R$ 80,00/dia.
- Limite ampliado (RN-007 ou RN-008): alimentação R$ 90,00/dia; transporte urbano R$ 120,00/dia.

**Origem:** política do RH, itens 1, 2 e 4 (AMB-001, AMB-002, AMB-003, AMB-005)
**Aceite:**
- Alimentação de R$ 72,50 + R$ 38,00 numa sexta-feira sem viagem → grupo com `valor_total` 110,50 e `valor_reembolsado` 60,00.
- Alimentação de R$ 40,00 num dia → reembolsa 40,00, e o dia seguinte continua com limite de 60,00.

### RN-005 — Limite de hospedagem por noite
**Regra:** cada despesa de hospedagem aprovada forma o seu próprio grupo. O número de noites é lido da descrição: um número inteiro seguido de "diária(s)" ou "noite(s)", com ou sem espaço entre eles, sem diferenciar maiúsculas, minúsculas ou acentos. O limite do grupo é R$ 250,00 × noites, e o reembolso é o menor valor entre o valor da despesa e esse limite. Hospedagem **nunca** tem limite ampliado. Duas hospedagens que cobrem o mesmo dia são limitadas cada uma isoladamente, sem verificação de sobreposição. Se o número de noites não puder ser identificado (ausente, zero, escrito por extenso, ou com mais de uma indicação na mesma descrição), a despesa fica `em_revisao` com motivo `NOITES_NAO_IDENTIFICADAS`.
**Origem:** política do RH, itens 3 e 4 (AMB-004, AMB-013)
**Aceite:**
- R$ 480,00 com "2 diarias" → limite 500,00 → reembolsa 480,00.
- R$ 750,00 com "3 noites" → limite 750,00 → reembolsa 750,00.
- R$ 900,00 com "3 noites" → reembolsa 750,00.
- R$ 300,00 com "Hotel centro" → `em_revisao`, `NOITES_NAO_IDENTIFICADAS`.
- "Hotel 2diarias" (sem espaço) → 2 noites.
- "Hotel 2 noites + 1 diaria" (duas indicações) → `em_revisao`, `NOITES_NAO_IDENTIFICADAS`.

### RN-006 — Nota fiscal obrigatória
**Regra:** toda despesa com valor **maior ou igual a R$ 100,00** exige nota fiscal. A comparação usa o valor de **cada despesa** após a RN-002, e não o valor reembolsável nem a soma do dia. Sem nota fiscal, a despesa fica `em_revisao` com motivo `NF_AUSENTE`.
**Origem:** política do RH, item 5 (AMB-006, AMB-007, AMB-008, AMB-009)
**Aceite:**
- R$ 100,00 sem NF → `em_revisao`.
- R$ 99,99 sem NF → não exige NF.
- Hospedagem de R$ 690,00 sem NF → `em_revisao`, mesmo que o reembolso possível fosse menor.
- Dois táxis de R$ 60,00 e R$ 50,00 no mesmo dia, sem NF → nenhum dos dois exige NF.

### RN-007 — Colaborador em viagem
**Regra:** um dia é "de viagem" quando está coberto por uma noite de hospedagem. Uma hospedagem com data D e N noites cobre os dias D, D+1, …, D+N−1; o dia da saída (D+N) **não** é de viagem. Uma hospedagem conta para a viagem sempre que o seu número de noites é identificável (RN-005), mesmo que ela própria esteja `em_revisao` por outro motivo (ex.: falta de NF). Em dias de viagem, os limites de alimentação e transporte urbano são ampliados em 50% (RN-004). A hospedagem não é ampliada.
**Origem:** política do RH, item 6 (AMB-011, AMB-012, AMB-013)
**Aceite:**
- Hospedagem em 14/07 com 2 noites → os dias 14/07 e 15/07 são de viagem e 16/07 não é.
- Alimentação de R$ 85,00 em 15/07 → reembolsa 85,00 (limite de 90,00).
- Alimentação de R$ 85,00 em 16/07 → reembolsa 60,00.

### RN-008 — Fim de semana ou plantão
**Regra:** os limites de alimentação e transporte urbano também são ampliados em 50% nas despesas cuja data cai em sábado ou domingo, ou cuja descrição contém a palavra "plantão" (sem diferenciar maiúsculas, minúsculas ou acentos). O aumento **não acumula** com o da RN-007: o limite ampliado é no máximo +50%. Hospedagem não é ampliada.
**Origem:** decisão do responsável pelo projeto; **não consta na política do RH** (AMB-014, AMB-015)
**Aceite:**
- Alimentação de R$ 85,00 num sábado → reembolsa 85,00.
- Alimentação de R$ 85,00 numa quarta-feira com descrição "Almoco - plantao" → reembolsa 85,00.
- Alimentação de R$ 100,00 num sábado que também é dia de viagem → reembolsa 90,00 (não 120,00).

**Grupo com despesas de origens diferentes:** se ao menos uma despesa do grupo tem "plantão" na descrição, o grupo inteiro usa o limite ampliado.

**Motivo informado na saída:** quando o dia é ao mesmo tempo de viagem e de fim de semana/plantão, o campo `limite_ampliado` informa `viagem`. A viagem vem da política do RH (item 6), e esta regra é adicional.
- Alimentação num sábado que também é dia de viagem → `limite_ampliado` = `viagem`, limite 90,00.

### RN-009 — Duplicatas
**Regra:** duas ou mais despesas com todos os campos iguais, exceto o `id` (data, categoria após a RN-001, descrição, fornecedor, valor após a RN-002 e nota fiscal), são duplicatas. **Todas** as cópias ficam `em_revisao` com motivo `DUPLICATA` e não são reembolsadas até que a revisão as resolva.
**Origem:** política do RH, item 8 (AMB-016)
**Aceite:** duas despesas idênticas de R$ 54,90 com IDs diferentes → as duas ficam `em_revisao` e nenhum grupo é formado com elas.

### RN-010 — Período de competência
**Regra:** a despesa pertence ao período quando a sua `data` está entre `periodo.inicio` e `periodo.fim`, **incluindo os dois extremos**. Como não há data de lançamento na entrada, a data da despesa é o critério. Uma despesa fora do período fica `em_revisao` com motivo `FORA_DO_PERIODO`.
**Origem:** política do RH, item 7 (AMB-017, AMB-018)
**Aceite:**
- Período de 01/07 a 31/07: despesa de 31/07 → dentro do período.
- Despesa de 15/04 → `em_revisao`, `FORA_DO_PERIODO`.

### RN-011 — Estorno (valor negativo)
**Regra:** uma despesa com valor negativo é um estorno. Em alimentação e transporte urbano, ele abate do grupo da mesma categoria no mesmo dia (RN-004), entrando na soma do grupo antes da aplicação do limite. Estorno de hospedagem vai sempre para revisão (`ESTORNO_SEM_DESPESA`), pois hospedagem não é agrupada por dia. Se no mesmo dia e categoria não houver despesas aprovadas cuja soma seja maior ou igual ao valor absoluto do estorno, o estorno fica `em_revisao` com motivo `ESTORNO_SEM_DESPESA` e não entra em nenhum grupo.
**Origem:** dados de entrada; a política não trata estornos (AMB-019)
**Aceite:**
- Transporte de R$ 100,00 com NF + estorno de −R$ 45,00 no mesmo dia → `valor_total` 55,00, reembolsa 55,00.
- Estorno de −R$ 45,00 sozinho no dia → `em_revisao`, `ESTORNO_SEM_DESPESA`.

### RN-012 — Status da despesa
**Regra:**
- `recusada`: a despesa nunca será reembolsada (RN-003).
- `em_revisao`: há pendência a resolver por um humano. Enquanto isso, o reembolso é R$ 0,00 e a despesa **não** entra em nenhum grupo, ou seja, não consome limite.
- `aprovada`: a despesa entra no seu grupo.

Uma despesa pode ter vários motivos e a saída lista todos. Se um dos motivos leva a `recusada`, o status é `recusada`; senão, havendo qualquer motivo, é `em_revisao`.
**Origem:** organização das demais regras
**Aceite:** uma despesa `coworking` de 15/04 → status `recusada`, motivos `CATEGORIA_FORA_DA_POLITICA` e `FORA_DO_PERIODO`.

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
**Decisão:** o número de noites é lido da descrição, e o limite é R$ 250 × noites. Sem número identificável, ou com mais de uma indicação, a despesa vai para revisão.
**Justificativa:** limitar um lançamento de várias noites a R$ 250 puniria quem lança a estadia de uma vez; adivinhar o número de noites seria pior que revisar.
**Regra afetada:** RN-005

#### AMB-007 — Nota fiscal por despesa ou pela soma do dia
**Texto original do RH:** "Nota fiscal é obrigatória acima de R$ 100."
**O que não está claro:** se o valor comparado é o de cada despesa ou a soma do dia.
**Decisão:** é o valor de cada despesa.
**Justificativa:** a nota fiscal é um documento de cada compra, não do dia.
**Regra afetada:** RN-006

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
**Decisão:** inclui. Valores ≥ R$ 100,00 exigem NF; valores abaixo de R$ 100,00 não exigem.
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
**Decisão:** na entrada, antes de qualquer regra, arredondando em cadeia a partir do último dígito (0–4 para baixo, 5–9 para cima).
**Justificativa:** arredondar na entrada faz todas as regras operarem sobre os mesmos valores, em centavos, que aparecem na saída.
**Regra afetada:** RN-002

#### AMB-018 — O último dia do período entra?
**Texto original do RH:** "Despesas devem ser lançadas dentro do período de competência."
**O que não está claro:** se os extremos do período (d-014 = 31/07, igual ao `fim`) estão incluídos.
**Decisão:** os dois extremos estão incluídos.
**Justificativa:** o período de competência é o mês inteiro, e o último dia faz parte dele.
**Regra afetada:** RN-010

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
**O que não está claro:** se a hospedagem também é ampliada (o que, como toda hospedagem implica viagem, a tornaria sempre R$ 375).
**Decisão:** só alimentação e transporte urbano; hospedagem continua em R$ 250 por noite.
**Justificativa:** ampliar a hospedagem tornaria o limite de R$ 250 da política letra morta.
**Regra afetada:** RN-005, RN-007

#### AMB-014 — Fim de semana e plantão
**Texto original do RH:** — (a política não fala de dia da semana; d-012 = "Almoco de sabado - plantao")
**O que não está claro:** se trabalho em fim de semana ou plantão muda o limite.
**Decisão:** o limite é ampliado em 50% (exceto hospedagem) em sábados, domingos e despesas com "plantão" na descrição.
**Justificativa:** decisão do responsável pelo projeto: trabalho fora do expediente normal recebe o mesmo tratamento de viagem. **Esta regra não existe na política do RH e deve ser validada com o RH.**
**Regra afetada:** RN-008

#### AMB-015 — Viagem + fim de semana acumulam?
**Texto original do RH:** —
**O que não está claro:** se um dia de viagem que cai num sábado tem +50% ou +100%.
**Decisão:** não acumula; o máximo é +50%. Quando os dois se aplicam, a saída informa `viagem` como motivo da ampliação.
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
**Decisão:** abate do grupo da mesma categoria e dia; sem despesa a abater, vai para revisão.
**Justificativa:** o estorno anula um gasto daquele dia; sem o gasto correspondente, não há como saber a qual despesa ele se refere.
**Regra afetada:** RN-011

#### AMB-020 — Grafia da categoria
**Texto original do RH:** "Categorias fora da política não são reembolsáveis."
**O que não está claro:** se `ALIMENTACAO` (d-014) é uma categoria diferente de `alimentacao`.
**Decisão:** é a mesma categoria; a comparação ignora maiúsculas, minúsculas e acentos.
**Justificativa:** diferença de digitação não muda a natureza do gasto.
**Regra afetada:** RN-001

---

## 7. Casos de borda

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
| Hospedagem com várias noites | 480,00, "2 diarias", com NF | Limite 500,00, reembolso 480,00 | RN-005 |
| Hospedagem sem número de noites | 300,00, "Hotel centro", com NF | `em_revisao`, `NOITES_NAO_IDENTIFICADAS` | RN-005 |
| Noites sem espaço | "Hotel 2diarias" | 2 noites | RN-005 |
| Mais de uma indicação de noites | "Hotel 2 noites + 1 diaria", com NF | `em_revisao`, `NOITES_NAO_IDENTIFICADAS` | RN-005 |
| Valor com 3 casas | 33,333 | Tratado como 33,33 | RN-002 |
| Categoria em maiúsculas | `ALIMENTACAO` | Tratada como `alimentacao` | RN-001 |
| Categoria com acento | `alimentação` | Tratada como `alimentacao` | RN-001 |
| Estorno de hospedagem | hospedagem −100,00 | `em_revisao`, `ESTORNO_SEM_DESPESA` | RN-011 |
| Dia de viagem | alimentação 85,00 em noite de hospedagem | Limite 90,00, reembolso 85,00 | RN-007 |
| Dia de saída da hospedagem | alimentação 85,00 no dia seguinte à última noite | Limite 60,00, reembolso 60,00 | RN-007 |
| Sábado | alimentação 85,00 | Limite 90,00, reembolso 85,00 | RN-008 |
| Sábado em viagem | alimentação 100,00 | Limite 90,00 (não acumula), reembolso 90,00, `limite_ampliado` = `viagem` | RN-008 |

## 8. Ordem de aplicação das regras

1. **Arredondamento** de todos os valores (RN-002) e **normalização** das categorias (RN-001).
2. **Verificações por despesa**, que podem gerar motivos de revisão ou recusa. Todas são avaliadas e todos os motivos são listados:
   categoria (RN-003) · período (RN-010) · duplicata (RN-009) · nota fiscal (RN-006) · noites de hospedagem (RN-005).
3. **Status** de cada despesa (RN-012). Despesas `em_revisao` e `recusadas` saem do cálculo e **não consomem limite**.
4. **Dias de viagem** (RN-007), a partir das hospedagens com número de noites identificável, mesmo as que estão em revisão.
5. **Estornos** (RN-011): abatidos dos grupos, ou enviados para revisão.
6. **Grupos e limites** (RN-004, RN-005, RN-008): soma das despesas aprovadas, limite (base ou ampliado), reembolso = menor valor entre os dois.
7. **Resumo**: soma dos reembolsos dos grupos.

## 9. Critérios de aceite

Para `exemplos/despesas-exemplo.json` (julho de 2026), o sistema produz:

| Despesa | Status | Motivo(s) |
|---|---|---|
| d-001, d-002 | aprovada | — |
| d-003, d-004 | em_revisao | `NF_AUSENTE` |
| d-005 | recusada | `CATEGORIA_FORA_DA_POLITICA` |
| d-006, d-007 | em_revisao | `DUPLICATA` |
| d-008 | em_revisao | `FORA_DO_PERIODO` |
| d-009 | em_revisao | `ESTORNO_SEM_DESPESA` |
| d-010 | aprovada | — |
| d-011 | aprovada | — |
| d-012 | aprovada | — |
| d-013 | em_revisao | `NF_AUSENTE` |
| d-014 | aprovada | — |

| Grupo | Total | Limite | Ampliação | Reembolso |
|---|---|---|---|---|
| 03/07 alimentação (d-001, d-002) | 110,50 | 60,00 | nenhum | 60,00 |
| 14/07 hospedagem (d-010, 2 noites) | 480,00 | 500,00 | nenhum | 480,00 |
| 15/07 alimentação (d-011) | 33,33 | 90,00 | viagem | 33,33 |
| 18/07 alimentação (d-012, sábado) | 47,20 | 90,00 | fim_de_semana_ou_plantao | 47,20 |
| 31/07 alimentação (d-014) | 61,00 | 60,00 | nenhum | 60,00 |

- [ ] `resumo.total_reembolsado` = **680,53**; quantidade: 6 aprovadas, 7 em revisão, 1 recusada.
- [ ] Toda despesa da entrada aparece na saída, na mesma ordem, com status, motivos e justificativa.
- [ ] Todos os valores monetários da saída têm exatamente 2 casas decimais.
- [ ] Cada caso da seção 7 produz o comportamento descrito.
- [ ] Executar duas vezes a mesma entrada produz exatamente a mesma saída.
- [ ] Entrada malformada é rejeitada com mensagem de erro e sem gerar saída.

## 10. O que fica em aberto

1. **RN-008 (fim de semana/plantão)** não vem da política do RH. Deve ser validada com o RH antes do uso em produção.

As decisões provisórias da versão 1.0 foram confirmadas pelo responsável e incorporadas às regras na versão 1.1 (ver `DECISIONS.md`, D-001).
