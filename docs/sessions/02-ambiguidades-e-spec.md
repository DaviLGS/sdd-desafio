# Sessão 02 — Levantamento de ambiguidades e primeira versão da spec

**Data:** 2026-10-14 · **Ferramenta:** Claude Code (app desktop)

> **Formato deste export:** o comando `/export` não funciona neste ambiente
> (ver sessão 01). Seguindo a alternativa do `FAQ.md`, o Claude reproduziu as
> mensagens desta parte da conversa. As mensagens do usuário estão **literais**.
> As respostas do Claude estão **condensadas**: tabelas longas foram resumidas e
> chamadas de ferramenta aparecem entre colchetes. Informações pessoais foram omitidas.
> Esta sessão continua a mesma janela de contexto da sessão 01.

---

## Usuário

siga para o proximo passo

## Claude

Fez a varredura das 14 despesas de `exemplos/despesas-exemplo.json` contra a política e listou **15 ambiguidades candidatas**, cada uma com as leituras possíveis e **sem decidir nenhuma**:

- **Unidade de aplicação:** A1 limite por dia × por despesa (d-001+d-002, d-003+d-004); A2 de qual item sai o corte; A3 diárias por lançamento de hospedagem (d-010 "2 diarias", d-013 "3 noites"); A4 NF por despesa × soma do dia.
- **Fronteira:** A5 R$ 100,00 exige NF? (d-003 × d-004); A6 o que é "reembolsar parcialmente"; A7 o que acontece sem NF; A8 NF pelo valor original × limitado; A9 arredondamento (d-011 = 33,333); A10 período inclusivo (d-014 = 31/07).
- **Dado ausente:** A11 "em viagem" sem campo; A12 competência sem data de lançamento (d-008); A13 duplicatas (d-006 = d-007); A14 estorno negativo (d-009); A15 categoria em maiúsculas (d-014).
- **Não são ambiguidade, mas precisam de uma frase na spec:** d-005 coworking (regra 9 é clara), d-012 sábado/plantão, ordem de aplicação das regras.

Recomendou que o usuário revisasse a lista com olhar crítico e registrasse no relatório (Delegação) que o levantamento inicial foi feito pelo Claude.

---

## Usuário

,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

## Claude

Apontou que a mensagem parecia enviada sem querer e pediu as decisões no formato `A1: (a) — porque ...`.

---

## Usuário

1 - Alimentação: a soma é limitada a 60 reais. Caso ultrapasse, deve reembolsar apenas 60 reais. Se for menos, reembolse o que foi gasto.

2 - Transporte: Mesma regra da alimentação. OBS: não acumula caso gaste menos

3 - Hospedagem: Mesma lógica das regras dos pontos acima

4 - Despesas acima do limite são reembolsadas parcialmente, ou seja, reembolsa apenas o limite diário estipulado. EX: gastei 100 reais com alimentação hoje. Vou ser reembolsado somente 60 reais.

5 - Nota fiscal é necessária apenas em valores = a 100 reais ou acima. 99 reais pra baixo, não precisa. A nota fiscal é necessária de acordo com o valor do gasto, desconsiderando o que a empresa vai reembolsar, ou seja, se o gasto com hotel foi de 100 reais, deve apresentar nota fiscal mesmo que a empresa tenha um limite estipulado em 250 reais

OBS: Os arredondamentos em valores com 3 ou mais casas decimais sempre considera o ultimo numero, se for um numero de 1 a 4, arredonda pra baixo e se for de 5 a 9, arredonda pra cima. EX: 3,445 = 3,45 e 3,444 = 3,44

6 - Viagem: só é considerado viagem quando há hospedagem. Caso não precise de hospedagem, não tem o aumento de 50% no limite

7 - Duplicatas: Caso haja algum pedido de reembolso com os mesmos dados e valores, deve solicitar comprovante da despesa independente do valor gasto pelo solicitante do reembolso. Caso não tenha, deve revisar com o solicitante.

Caso falte alguma ambiguidade para eu ditar a regra, enumere-as e me explique melhor

## Claude

Resumiu como entendeu cada decisão (A1 soma do dia; limite não acumula; A6 paga o limite; A5 ≥ 100 exige NF; A8 valor original; A9 arredondamento; A11 viagem = hospedagem; A13 duplicatas → comprovante/revisão) e listou o que faltava, Q1 a Q15:

- Q1 de qual despesa sai o corte do limite diário · Q2 quantas diárias numa hospedagem · Q3 o que acontece sem NF · Q4 NF por despesa ou soma do dia · Q5 valores entre 99 e 100 · Q6 arredondamento com 4+ casas (3,4449) · Q7 quando arredondar · Q8 dias e categorias da viagem · Q9 detalhes das duplicatas · Q10 despesa fora do período · Q11 último dia do período · Q12 estorno · Q13 categoria em maiúsculas · Q14 ordem de aplicação · Q15 coworking e sábado/plantão.

Lembrou que, pelo `CLAUDE.md`, toda regra explicada no chat precisa ir para a spec.

---

## Usuário

Q1 - não entendi

Q2 - Considere o valor total e divida pelo numero de noites colocadas. EX: valor total de 750 reais e 3 noites. Valor da diaria é 250, ou seja, a empresa  reembolsa tudo. Caso o valor da noite fique acima do valor que a empresa reembolsa, a diferença o solicitante que paga. Lembrando que hospedagem tem aumento de 50% do limite

Q3 - b

Q4 - a. No caso do transporte, a empresa reembolsará apenas o valor estipulado da categoria. O excedente não será reembolsado

Q5 - sim

Q6 - b

Q7 - a

Q8 - é considerado o numero de noites ficadas. Caso so tome um cafe no outro dia, não é contado como diaria.
Vamos manter todas as categorias exceto a hospedagem com o aumento de 50%

Q9 - caso haja duplicata, vai tudo pra revisao e não tera reembolso ate ser resolvido

Q10 - c

Q11 - a

Q12 - c

Q13 - a

Q14 - a

Q15 - coworking: categoria fora da politica, sem reembolso. plantão ou fim de semana deve aplicar a regra dos 50% acima do limite (menos pra hospedagem)

Após o termino das regras, organize-as e salve-as no spec.md

## Claude

Antes de escrever a spec, identificou pontos ainda abertos, inclusive uma **contradição** (Q2 dizia que hospedagem tem +50%; Q8 e Q15 diziam que não), e perguntou em duas rodadas de múltipla escolha:

| Pergunta | Resposta do usuário |
|---|---|
| Q1 reexplicada: em 03/07, almoço 72,50 + jantar 38,00 = 110,50 e a empresa paga 60. Como mostrar no resultado? | "deve aparecer todos os gastos com seus respectivos valores e sua soma. E deve reembolsar apenas 60 reias dos 110,50 gastos" |
| Contradição: hospedagem tem ou não +50%? | Hospedagem sem aumento |
| Viagem + fim de semana acumulam? | Não acumula — máx. +50% |
| Estorno num dia sem outra despesa (11/07 só tem −45) | Vai para revisão |
| Quais dias contam como viagem (hotel 14/07, 2 noites)? | Só as noites (14 e 15) |
| Como identificar plantão/fim de semana? | Fim de semana + "plantão" na descrição |
| Hospedagem sem número de noites na descrição | Vai para revisão |
| Quais campos definem duplicata? | Todos exceto o ID |

[Ferramentas: verificou os dias da semana de julho/2026; escreveu `specs/001-motor-reembolso/spec.md`; commit `f66e073 docs(spec): primeira versão da spec com 20 ambiguidades resolvidas`.]

Resumo entregue: 20 ambiguidades (AMB-001 a AMB-020), 12 regras (RN-001 a RN-012), schema de saída com despesas, grupos por dia/categoria e resumo, ordem de aplicação, e resultado esperado do exemplo de **R$ 680,53** (6 aprovadas, 7 em revisão, 1 recusada). Destacou que a RN-008 (fim de semana/plantão) **não existe na política do RH**, e listou 7 decisões provisórias que o próprio Claude tomou ao redigir e que precisavam de confirmação:

1. hospedagem em revisão conta como viagem; 2. estorno de alimentação segue a regra do transporte, estorno de hospedagem vai para revisão; 3. negativo arredonda pelo valor absoluto; 4. `alimentação` com acento não é reconhecida; 5. hospedagens sobrepostas limitadas isoladamente; 6. valor zero aprovado; 7. grupo com uma despesa de "plantão" usa o limite ampliado inteiro.

---

## Usuário

1 a 7 ok, mas aceite alimentação com acento
faça o export da conversa para a pasta sessions
apos isso faça o commit e me de os proximos passos

## Claude

[Ferramentas: atualizou a spec para a versão 1.1 (RN-001 ignora acentos; decisões confirmadas incorporadas às regras RN-002, RN-004, RN-005, RN-007 e RN-011; novos casos de borda; seção 10 reduzida à validação da RN-008 com o RH); criou a entrada D-001 em `DECISIONS.md`; gerou este arquivo; fez os commits.]
