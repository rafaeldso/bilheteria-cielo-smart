# Spec: Venda de ingressos com pagamento Cielo Smart

| Campo | Valor |
|---|---|
| **ID** | 001 |
| **Status** | Implementada |
| **Versão** | 1.3.0 |
| **Módulo** | `feature/ticketing` (provisório; o `plan.md` confirma a divisão de módulos) |
| **Autor** | Rafael Oliveira (com Claude Code) |
| **Links** | [case_android.md](../../../case_android.md) · [research.md](research.md) · Design ([ui-spec.md](ui-spec.md) v0.4.4 aprovada · [prancha](design/mockups.html)) |

> Esta spec descreve **o quê** e **por quê**. Detalhes de implementação ficam no `plan.md`.
> Fonte dos requisitos: o case (RF-1…RF-5, RNFs), a documentação oficial da Cielo Smart resumida no
> [research.md](research.md) e as respostas do humano às perguntas da seção 10.

**Status da compra** (vocabulário usado em toda a spec):

| Status | Significado | É resultado de venda? |
|---|---|---|
| Em andamento | Compra registrada, pagamento aberto na Cielo, sem retorno ainda | Não (transitório) |
| Aprovada | A Cielo autorizou o pagamento | Sim |
| Negada | A Cielo devolveu código 3 "erro no pagamento": o pagamento em si não passou | Sim |
| Cancelada | A Cielo devolveu código 1: alguém cancelou no terminal | Sim |
| Falhou | Problema técnico (códigos 2/4, código desconhecido, app Cielo ausente, pagamento não encontrado na consulta): o pagamento não chegou a um resultado | Não; permite nova tentativa |

Por que "negada" = código 3 (Q8, opção A): a documentação da Cielo não tem código próprio para "recusado pelo
banco" e o emulador só simula Sucesso/Cancelado/Erro. O código 3 é o único que fala do pagamento em si; os
códigos 2 e 4 apontam problema do lado do app (configuração, credencial). Chamá-los de "negada" levaria o
operador a pedir outro cartão quando nenhum cartão funcionaria. Esta explicação deve constar também em KDoc no
código que faz o mapeamento do retorno da Cielo.

## 1. Visão Geral
**Problema:** vender ingressos de eventos locais num terminal Cielo Smart com um fluxo de compra simples,
sem cobrar duas vezes e sem perder o resultado do pagamento.

**Objetivo:** fluxo ponta a ponta: ver eventos → escolher quantidade → pagar via Cielo Smart (Deep Link) →
registrar o resultado (aprovada, negada ou cancelada) → exibir comprovante/resumo; e consultar as compras
registradas no histórico.

**Métricas de sucesso** (do case: funcionalidade, erros, duplicidade, testes):
- Os 5 fluxos do case funcionam no emulador Cielo (Journey do fluxo principal verde no `verify-on-device`).
- Nenhuma cobrança duplicada nos cenários de reenvio cobertos por AC (toque repetido, rotação, morte do processo).
- Todo AC com teste automatizado; `verify.sh full` verde.

## 2. Histórias de Usuário
Persona: **operador da bilheteria** usando o terminal Cielo Smart (Q1).

| ID | História | Prioridade (MoSCoW) | Origem |
|---|---|---|---|
| US-01 | Como operador, quero ver os eventos disponíveis para compra, para escolher o que vender. | Deve ter | RF-1 |
| US-02 | Como operador, quero escolher a quantidade de ingressos e ver o total, para cobrar o valor certo. | Deve ter | RF-2 |
| US-03 | Como operador, quero iniciar e concluir o pagamento na Cielo Smart, para receber pela venda. | Deve ter | RF-3 |
| US-04 | Como operador, quero que o resultado da compra (aprovada, negada ou cancelada) fique registrado, para ter controle das vendas. | Deve ter | RF-4 |
| US-05 | Como operador, quero ver o comprovante/resumo da compra ao final da operação, para conferir e informar o comprador. | Deve ter | RF-5 |
| US-06 | Como operador, quero que um toque repetido ou uma interrupção não gere segunda cobrança, para não lesar o comprador. | Deve ter | RNF duplicidade |
| US-07 | Como operador, quero mensagens claras quando a integração ou o pagamento falhar, para saber o que fazer. | Deve ter | RNF erros |
| US-08 | Como operador, quero consultar o histórico das compras registradas e reabrir o comprovante, para conferir vendas anteriores. | Deveria ter | Q3 |
| US-09 | Como operador, quero ir direto aos eventos de um mês, para não rolar a lista inteira quando o comprador pede uma data distante. | Deveria ter | Q13 |

## 3. Escopo
**Dentro do escopo:**
- Lista de eventos a partir de um JSON local empacotado no app ([roadmap](../../roadmap.md#decisões)).
- Seleção de quantidade, pagamento via Deep Link `lio://payment`, registro local do resultado.
- Tela de resultado ao fim da compra, que é o próprio comprovante/resumo (Q3).
- Histórico de compras registradas, com acesso ao comprovante de cada uma (Q3).
- Prevenção de cobrança duplicada e recuperação de pagamento sem retorno.
- Tratamento explícito de erros de integração e de pagamento.

**Fora do escopo:**
- QR Code do ingresso (opcional no case): spec própria `002`, só após esta.
- Backend próprio (o case não o avalia) e fonte remota de eventos.
- Estorno/cancelamento pós-venda (`lio://payment-reversal`) (Q2).
- Impressão de comprovante na impressora do terminal (`lio://print`): não pedido no case.
- Login, cadastro do comprador, escolha do meio de pagamento no app (Q7), multi-EC.
- Reabrir o app depois de uma venda já concluída leva à lista de eventos, não ao comprovante: o comprovante da última venda fica no histórico (decisão da T25, 2026-10-18).

## 4. Regras de Negócio (EARS)
| ID | Regra | História | Prioridade |
|---|---|---|---|
| BR-01 | QUANDO a tela de eventos for aberta, o sistema deve exibir os eventos disponíveis para compra com nome, data/hora, local, preço unitário e ingressos disponíveis. | US-01 | Deve ter |
| BR-02 | O sistema deve considerar disponível para compra o evento com data/hora futura e ingressos disponíveis > 0. | US-01 | Deve ter |
| BR-03 | SE não houver evento disponível, ENTÃO o sistema deve exibir o estado vazio. | US-01 | Deve ter |
| BR-04 | SE a leitura dos eventos falhar, ENTÃO o sistema deve exibir erro com a opção de tentar novamente. | US-01, US-07 | Deve ter |
| BR-05 | QUANDO um evento for selecionado, o sistema deve iniciar a quantidade em 1 e exibir o total. | US-02 | Deve ter |
| BR-06 | O sistema deve calcular o total como preço unitário × quantidade, em centavos, sem arredondamento. | US-02 | Deve ter |
| BR-07 | O sistema deve limitar a quantidade a no mínimo 1 e no máximo 10 por compra, nunca acima dos ingressos disponíveis do evento. | US-02 | Deve ter |
| BR-08 | QUANDO o operador confirmar a compra, o sistema deve registrar a compra com um identificador único e status "em andamento" **antes** de abrir o pagamento, e enviar esse identificador como `reference` ao `lio://payment`. | US-03, US-06 | Deve ter |
| BR-09 | QUANDO a compra for registrada, o sistema deve abrir o pagamento na Cielo Smart com o valor total e os itens da compra, sem fixar o meio de pagamento (o terminal pergunta). | US-03 | Deve ter |
| BR-10 | ENQUANTO houver compra "em andamento", o sistema deve ignorar novas confirmações de pagamento e manter a ação de pagar indisponível. | US-06 | Deve ter |
| BR-11 | QUANDO a Cielo retornar pagamento autorizado, o sistema deve registrar a compra como **Aprovada**, com os dados da transação classificados na seção 8. | US-04 | Deve ter |
| BR-12 | QUANDO a Cielo retornar "cancelado pelo usuário" (código 1), o sistema deve registrar a compra como **Cancelada**. | US-04 | Deve ter |
| BR-13 | QUANDO a Cielo retornar código 3 ("erro no pagamento"), o sistema deve registrar a compra como **Negada**. | US-04 | Deve ter |
| BR-14 | SE o pagamento não puder ser iniciado (app Cielo ausente) ou a Cielo retornar código 2 (erro genérico), código 4 (autenticação) ou código desconhecido, ENTÃO o sistema deve exibir mensagem específica do erro, registrar a tentativa como **Falhou** e permitir nova tentativa. | US-07 | Deve ter |
| BR-25 | SE a resposta da Cielo não puder ser lida, ENTÃO o sistema não deve liberar nova cobrança antes de consultar a Cielo pelo `reference`, seguindo a BR-15 (a cobrança pode ter sido aprovada). | US-06, US-07 | Deve ter |
| BR-15 | SE o app voltar a abrir com compra "em andamento" sem resultado (ou ocorrer a BR-25), ENTÃO o sistema deve consultar a Cielo pelo `reference` antes de permitir nova cobrança; se encontrar, registrar o resultado encontrado e exibir o comprovante; se não encontrar, registrar a tentativa como **Falhou** e voltar à compra, com o mesmo evento e quantidade, informando que o pagamento anterior não foi encontrado e permitindo nova tentativa. | US-06 | Deve ter |
| BR-16 | QUANDO a compra receber resultado, o sistema deve exibir o comprovante/resumo com nº da compra, evento, data do evento, local, quantidade, valor unitário, total, data/hora da compra e status; se Aprovada, também bandeira, código de autorização e NSU. O número do cartão nunca é exibido. | US-05 | Deve ter |
| BR-17 | O sistema deve calcular os ingressos disponíveis de um evento como o total do catálogo menos a soma das quantidades de compras **Aprovadas** desse evento (contagem por aparelho). | US-01, US-04 | Deve ter |
| BR-18 | QUANDO o operador abrir o histórico, o sistema deve listar as compras Aprovadas, Negadas e Canceladas, da mais recente para a mais antiga, com evento, data/hora, quantidade, total e status. Tentativas "Em andamento" e "Falhou" não aparecem (não são vendas). | US-08 | Deveria ter |
| BR-19 | QUANDO o operador selecionar uma compra no histórico, o sistema deve exibir o comprovante/resumo dessa compra (BR-16). | US-08 | Deveria ter |
| BR-20 | SE não houver compras registradas, ENTÃO o sistema deve exibir o histórico vazio. | US-08 | Deveria ter |
| BR-21 | SE a leitura do histórico falhar, ENTÃO o sistema deve exibir erro com a opção de tentar novamente. | US-08, US-07 | Deveria ter |
| BR-22 | O sistema deve exibir os eventos disponíveis em ordem cronológica, agrupados por mês, com um cabeçalho por mês ("novembro de 2026"). | US-01, US-09 | Deveria ter |
| BR-23 | O sistema deve oferecer um filtro de mês com a opção "Todos" (padrão ao abrir) e somente os meses que têm eventos disponíveis; QUANDO o operador selecionar um mês, o sistema deve exibir só os eventos disponíveis desse mês. | US-09 | Deveria ter |
| BR-24 | SE o mês selecionado deixar de ter eventos disponíveis (ex.: último ingresso vendido), ENTÃO o sistema deve voltar o filtro para "Todos". | US-09 | Deveria ter |
| BR-26 | SE a consulta da BR-15 não puder ser concluída (falha ao ler ou gravar a compra, ou falha ao consultar a Cielo), ENTÃO o sistema deve exibir erro com a opção de tentar novamente, e a nova tentativa deve repetir a consulta, sem abrir nova cobrança. | US-06, US-07 | Deve ter |
| BR-27 | SE o evento da compra não puder ser carregado (catálogo ilegível, evento inexistente ou sem ingressos) e não houver compra em andamento, ENTÃO o sistema deve informar que o evento está indisponível e oferecer a volta para a lista de eventos. | US-02, US-07 | Deve ter |
| BR-28 | SE o comprovante não puder ser carregado (falha de leitura, compra inexistente ou sem resultado de venda), ENTÃO o sistema deve exibir erro com a opção de tentar novamente. | US-05, US-07 | Deve ter |
| BR-29 | QUANDO a Cielo retornar pagamento autorizado cujo `reference` seja o de uma compra registrada como **Falhou** por "pagamento não encontrado" (BR-15) e cujo valor pago seja igual ao total dessa compra, ENTÃO o sistema deve registrar essa compra como **Aprovada**, com os dados da transação, sem alterar a compra em andamento, se houver outra. Pagamento autorizado com valor diferente, ou para compra que falhou por outro motivo, continua descartado. | US-06, US-07 | Deve ter |

## 5. Critérios de Aceite (Gherkin)
Valores ilustrativos (preço, nomes) são dados de teste, não regra.

### AC-01 — Lista de eventos disponíveis (BR-01, BR-02)
```gherkin
Dado que o catálogo tem um evento com data futura e outro com data passada
Quando o operador abre o app
Então vê o evento futuro com nome, data/hora, local, preço unitário e ingressos disponíveis
E não vê o evento com data passada
Dado um evento futuro com 0 ingressos disponíveis
Quando o operador abre o app
Então não vê esse evento
```

### AC-02 — Catálogo sem eventos disponíveis (BR-03)
```gherkin
Dado que nenhum evento do catálogo está disponível
Quando o operador abre o app
Então vê o estado vazio com a mensagem de que não há eventos à venda
```

### AC-03 — Falha ao ler eventos e nova tentativa (BR-04)
```gherkin
Dado que a leitura do catálogo falha
Quando o operador abre o app
Então vê o estado de erro com a ação "Tentar novamente"
Quando toca em "Tentar novamente" e a leitura funciona
Então vê a lista de eventos
```

### AC-04 — Seleção de evento inicia quantidade 1 (BR-05, BR-06)
```gherkin
Dado o evento "Show A" com preço unitário de R$ 50,00
Quando o operador seleciona o evento
Então a quantidade é 1 e o total é R$ 50,00
```

### AC-05 — Alterar quantidade recalcula o total (BR-06)
```gherkin
Dado o evento "Show A" com preço unitário de R$ 50,00 e quantidade 1
Quando o operador aumenta a quantidade para 3
Então o total é R$ 150,00
```

### AC-06 — Limites de quantidade (BR-07)
```gherkin
Dado a quantidade 1
Quando o operador tenta diminuir
Então a quantidade continua 1 e a ação de diminuir fica indisponível
Dado a quantidade 10 num evento com 50 ingressos disponíveis
Quando o operador tenta aumentar
Então a quantidade continua 10 e a ação de aumentar fica indisponível
Dado a quantidade 3 num evento com 3 ingressos disponíveis
Quando o operador tenta aumentar
Então a quantidade continua 3 e a ação de aumentar fica indisponível
```

### AC-07 — Compra registrada antes do pagamento (BR-08, BR-09)
```gherkin
Dado o evento selecionado com quantidade 2
Quando o operador confirma a compra
Então a compra é registrada com identificador único e status "em andamento"
E o pagamento é aberto na Cielo com o valor total, os itens e esse identificador como reference
E sem meio de pagamento fixado
```

### AC-08 — Toque repetido não gera segunda cobrança (BR-10)
```gherkin
Dado uma compra "em andamento"
Quando o operador toca em "Pagar" novamente (ou a tela é recriada)
Então nenhum novo pagamento é aberto na Cielo
E a ação de pagar continua indisponível
```

### AC-09 — Pagamento aprovado (BR-11)
```gherkin
Dado uma compra "em andamento"
Quando a Cielo retorna pagamento autorizado
Então a compra é registrada como Aprovada com os dados da transação
E o comprovante é exibido
```

### AC-10 — Pagamento cancelado no terminal (BR-12)
```gherkin
Dado uma compra "em andamento"
Quando a Cielo retorna código 1 "cancelado pelo usuário"
Então a compra é registrada como Cancelada
E o resumo informa que o pagamento foi cancelado
```

### AC-11 — Pagamento negado (BR-13)
```gherkin
Dado uma compra "em andamento"
Quando a Cielo retorna código 3 "erro no pagamento"
Então a compra é registrada como Negada
E o resumo informa que o pagamento foi negado
```

### AC-12 — App Cielo ausente (BR-14)
```gherkin
Dado que o app de pagamento da Cielo não está instalado
Quando o operador confirma a compra
Então vê a mensagem de que o pagamento Cielo não está disponível no aparelho
E nenhuma cobrança é feita
```

### AC-13 — Erro de integração ou autenticação (BR-14)
```gherkin
Dado uma compra "em andamento"
Quando a Cielo retorna erro genérico (código 2), de autenticação (código 4) ou um código desconhecido
Então a tentativa é registrada como Falhou
E o operador vê uma mensagem específica para o erro
E pode tentar pagar novamente sem duplicar a cobrança
```

### AC-14 — Resposta ilegível da Cielo (BR-25)
```gherkin
Dado uma compra "em andamento"
Quando o retorno da Cielo não pode ser decodificado
Então a compra não é registrada como Aprovada nem liberada para nova cobrança
E o sistema consulta a Cielo pelo reference, como na recuperação (AC-15)
```

### AC-15 — Recuperação após interrupção (BR-15)
```gherkin
Dado uma compra "em andamento" sem resultado registrado
Quando o app é aberto novamente
Então o sistema consulta a Cielo pelo reference antes de permitir nova cobrança
E, se encontrar o pedido, registra o resultado encontrado e exibe o comprovante
E, se não encontrar, registra a tentativa como Falhou
E volta à compra com o mesmo evento e quantidade, informando que o pagamento anterior não foi encontrado
E permite tentar pagar de novo
```

### AC-16 — Comprovante/resumo (BR-16)
```gherkin
Dado uma compra Aprovada
Quando o comprovante é exibido
Então mostra nº da compra, evento, data do evento, local, quantidade, valor unitário, total, data/hora, status, bandeira, código de autorização e NSU
E não mostra o número do cartão
Dado uma compra Negada ou Cancelada
Quando o resumo é exibido
Então mostra os mesmos campos, exceto bandeira, código de autorização e NSU
```

### AC-17 — Disponibilidade após compra (BR-17)
```gherkin
Dado o evento "Show A" com 50 ingressos no catálogo e nenhuma compra
Quando uma compra de 1 ingresso é aprovada
Então a lista mostra 49 ingressos disponíveis para "Show A"
Quando uma compra de 2 ingressos é negada, cancelada ou falha
Então a lista continua mostrando 49 ingressos disponíveis
```

### AC-18 — Histórico de compras (BR-18)
```gherkin
Dado uma compra Aprovada às 10:00, uma Cancelada às 11:00 e uma tentativa Falhou às 12:00
Quando o operador abre o histórico
Então vê a compra das 11:00 antes da compra das 10:00
E não vê a tentativa das 12:00
E cada item mostra evento, data/hora, quantidade, total e status
```

### AC-19 — Reabrir comprovante pelo histórico (BR-19)
```gherkin
Dado uma compra Aprovada no histórico
Quando o operador seleciona a compra
Então vê o comprovante dessa compra
```

### AC-20 — Histórico vazio (BR-20)
```gherkin
Dado que nenhuma compra foi registrada
Quando o operador abre o histórico
Então vê a mensagem de que ainda não há compras
```

### AC-21 — Falha ao ler o histórico (BR-21)
```gherkin
Dado que a leitura do histórico falha
Quando o operador abre o histórico
Então vê o estado de erro com a ação "Tentar novamente"
```

### AC-22 — Eventos agrupados por mês (BR-22)
```gherkin
Dado eventos disponíveis em novembro e dezembro de 2026
Quando o operador abre o app
Então vê os eventos de novembro sob o cabeçalho "novembro de 2026", antes dos de dezembro sob "dezembro de 2026"
E dentro de cada mês os eventos estão em ordem de data/hora
```

### AC-23 — Filtrar por mês (BR-23)
```gherkin
Dado eventos disponíveis em novembro e dezembro de 2026 e nenhum em janeiro de 2027
Quando o operador abre o app
Então o filtro mostra "Todos" selecionado e as opções de novembro e dezembro, sem janeiro
Quando seleciona dezembro
Então vê só os eventos de dezembro
Quando seleciona "Todos"
Então vê os eventos de todos os meses
```

### AC-24 — Mês esgotado volta para "Todos" (BR-24)
```gherkin
Dado o filtro em dezembro, com um único evento disponível nesse mês e 1 ingresso restante
Quando esse ingresso é vendido e o operador volta para a lista
Então o filtro está em "Todos" e dezembro não aparece mais como opção
```

### AC-25 — Consulta que falha não libera cobrança (BR-26)
```gherkin
Dado uma compra "em andamento" sem resultado ao reabrir o app
Quando a tela da compra é exibida e a consulta à Cielo ou a leitura da compra falhar
Então é exibido erro com "Tentar novamente"
E tentar novamente repete a consulta sem abrir novo pagamento
```
> Nota (2026-10-18, T25): o erro aparece na tela da compra. O app leva o operador a ela ao reabrir com compra em
> andamento legível; se a compra aberta não puder ser lida, o app abre na lista de eventos (sem o `eventId` não há
> para onde ir) e o erro aparece quando o operador abre a compra. A lista não mostra esse erro. Em nenhum caso há
> nova cobrança.

### AC-26 — Evento indisponível na compra (BR-27)
```gherkin
Dado que não há compra em andamento
Quando a tela de compra é aberta para um evento que não pode ser carregado
Então é informado que o evento está indisponível, com a opção de voltar para os eventos
E a ação de pagar não é oferecida
```
> Nota (2026-10-18): se, além do evento, a leitura da compra em andamento também falhar, a tela fica em "indisponível"
> (sem pagar); a nova tentativa é reabrir a compra, que relê a compra em andamento antes de liberar pagamento (BR-10).

### AC-27 — Comprovante que não carrega (BR-28)
```gherkin
Dado uma compra cujo registro não pode ser lido
Quando o comprovante é aberto
Então é exibido erro com "Tentar novamente"
E tentar novamente repete a leitura
```

### AC-28 — Pagamento aprovado depois de "não encontrado" (BR-29)
```gherkin
Dado uma compra registrada como Falhou por "pagamento anterior não encontrado"
Quando a Cielo retorna pagamento autorizado com o reference e o valor dessa compra
Então a compra passa a Aprovada, com os dados da transação
E aparece no histórico e conta na disponibilidade do evento (BR-17)
E uma compra em andamento, se houver, não é alterada
```
> Nota (2026-10-18, T25): o emulador mostrou o caso: o app foi reaberto com o terminal ainda aberto, a consulta não achou o
> pedido (Falhou) e o terminal concluiu o pagamento depois. O app não avisa o operador (ver Q14).

## 6. Estados de UI e Eventos
**Tela de eventos** (`EventListUiState`):

| Estado | Quando ocorre | O que a UI exibe | AC |
|---|---|---|---|
| `Loading` | Leitura do catálogo em curso | Indicador de carregamento | AC-01 |
| `Content` | Há eventos disponíveis | Filtro de mês, lista agrupada por mês e acesso ao histórico (carrega os meses disponíveis e o mês selecionado) | AC-01, AC-22, AC-23, AC-24 |
| `Empty` | Nenhum evento disponível | Mensagem de vazio | AC-02 |
| `Error` | Falha na leitura | Mensagem + "Tentar novamente" | AC-03 |

**Tela de compra** (`PurchaseUiState`):

| Estado | Quando ocorre | O que a UI exibe | AC |
|---|---|---|---|
| `Editing` | Evento selecionado, sem compra em andamento | Evento, quantidade (+/−), total, "Pagar" | AC-04, AC-05, AC-06 |
| `AwaitingPayment` | Compra "em andamento" | Aguardando pagamento no terminal; "Pagar" indisponível | AC-07, AC-08 |
| `Recovering` | Reabertura com compra sem resultado, ou resposta ilegível da Cielo | Confirmando o pagamento com a Cielo antes de permitir nova cobrança | AC-14, AC-15 |
| `Error` | App ausente, erro genérico/autenticação/código desconhecido, pagamento anterior não encontrado, ou consulta que falhou | Mensagem específica + "Tentar novamente" (na consulta que falhou, repete a consulta) | AC-12, AC-13, AC-15, AC-25 |
| `Unavailable` | Evento não pode ser carregado e não há compra em andamento | Aviso de evento indisponível + "Voltar para eventos" | AC-26 |

**Tela de resultado = comprovante/resumo** (`ReceiptUiState`), ao fim da compra ou aberta pelo histórico:

| Estado | Quando ocorre | O que a UI exibe | AC |
|---|---|---|---|
| `Approved` | Compra aprovada | Sucesso + comprovante | AC-09, AC-16, AC-19 |
| `Declined` | Compra negada | Resumo com aviso de pagamento negado | AC-11, AC-16 |
| `Cancelled` | Cancelada no terminal | Resumo com aviso de cancelamento | AC-10, AC-16 |
| `Error` | Registro não pode ser lido, não existe ou não tem resultado de venda | Mensagem + "Tentar novamente" | AC-27 |

**Tela de histórico** (`PurchaseHistoryUiState`):

| Estado | Quando ocorre | O que a UI exibe | AC |
|---|---|---|---|
| `Loading` | Leitura em curso | Indicador de carregamento | AC-18 |
| `Content` | Há compras registradas | Lista da mais recente para a mais antiga | AC-18, AC-19 |
| `Empty` | Nenhuma compra | Mensagem de histórico vazio | AC-20 |
| `Error` | Falha na leitura | Mensagem + "Tentar novamente" | AC-21 |

**Eventos do usuário e do sistema:**

| Evento | Origem | Efeito esperado |
|---|---|---|
| `OnRetry` | Eventos / compra / comprovante / histórico | Repete a leitura, a consulta à Cielo ou a tentativa de pagamento |
| `OnEventSelected(id)` | Lista de eventos | Abre a compra com quantidade 1 |
| `OnMonthSelected(month)` | Filtro de mês | Mostra só os eventos do mês; `month` vazio = "Todos" |
| `OnIncrement` / `OnDecrement` | Tela de compra | Altera a quantidade dentro dos limites |
| `OnPay` | Tela de compra | Registra a compra e abre o pagamento (ignorado se em andamento) |
| `OnPaymentResult(response)` | Retorno da Cielo (`order://response`) | Registra o resultado e abre o comprovante |
| `OnOpenHistory` | Lista de eventos | Abre o histórico |
| `OnPurchaseSelected(id)` | Histórico | Abre o comprovante da compra |
| `OnNewSale` | Comprovante | Volta para a lista de eventos |

## 7. Dependências e Contexto
- **Módulos afetados:** novos módulos da feature; `app` (manifest, navegação, activity de retorno); `core:designsystem`.
- **APIs/endpoints:** Deep Link Cielo Smart `lio://payment` e consulta `lio://orders` / `lio://order` ([research.md](research.md) §3, §6).
- **Fontes de dados:** catálogo de eventos em JSON local (assets); compras persistidas localmente.
- **Feature flags:** nenhuma.
- **Dependências externas:**
  - App UriApp da Cielo (`com.ads.lio.uriappclient`) no terminal ou `lio-emulator.apk` 1.61.8 em Android 7/10.
  - Credenciais `clientID` e `accessToken`: primeiro testar valores fictícios no emulador; se forem recusados, o humano
    cadastra o app no Portal de Desenvolvedores Cielo e os valores entram por propriedade local não versionada (Q12).
- **Restrições da Cielo:** proibido SDK de fabricante (camada de abstração obrigatória); retorno exige
  `<queries>`, intent-filter `order://response`, `cs_integration_type=uri` e serviço em primeiro plano durante o pagamento.

## 8. Dados e Privacidade
| Dado | Classificação | Observações |
|---|---|---|
| Dados do evento (nome, data, local, preço) | Público | Catálogo local |
| Identificador da compra (`reference`), quantidade, total, status, data/hora | Interno | Persistido localmente |
| `orderId`, código de autorização (`authCode`), NSU (`cieloCode`), bandeira | Interno (dado de transação) | Persistir só o necessário ao comprovante e à recuperação; nunca em log |
| Número do cartão (mesmo mascarado) | Dado de cartão (PCI) | Não persistido nem exibido (Q10) |
| PAN completo, CVV, trilha | Dado de cartão (PCI) | O app nunca recebe nem trata (a Cielo captura) |
| `clientID`, `accessToken` | Segredo | Fora do repositório (Art. V.4); nunca em log |
| E-mail/CPF do comprador | PII (LGPD) | Não coletados |

## 9. Requisitos Não Funcionais
- **Performance:** sem meta no case; lista e totais sem bloqueio da main thread.
- **Acessibilidade:** constitution Art. VI.2 (content descriptions, toque ≥ 48dp, fonte até 200%); telas do terminal de 480×854 a 1440×720.
- **Offline / conectividade:** catálogo e histórico funcionam offline; o pagamento depende só do app Cielo no aparelho.
- **Analytics:** nenhum (ADR-0009, S1). Logs via `Logger`, sem PAN, PII ou tokens.
- **Segurança:** activity de retorno aceita só `order://response`; resposta tratada como entrada não confiável (decodificação e parse defensivos).
- **Robustez:** a compra "em andamento" sobrevive a rotação e morte do processo (persistida antes do deep link).

## 10. Dúvidas em Aberto
| # | Pergunta | Resposta / recomendação | Status |
|---|---|---|---|
| Q1 | Quem usa o app? | Operador da bilheteria | Respondida |
| Q2 | Estorno pós-venda entra? | Não; "cancelada" = código 1 no terminal | Respondida |
| Q3 | Tela de histórico? | **Sim**, e a tela de resultado ao fim da compra é o próprio comprovante/resumo | Respondida |
| Q4 | Campos do evento | id, nome, data/hora, local, preço unitário (+ disponíveis, se Q11 = sim) | Respondida |
| Q5 | Quando um evento está disponível? | Data/hora futura (+ disponíveis > 0, se Q11 = sim) | Respondida |
| Q6 | Máximo por compra | 10, nunca acima do disponível | Respondida |
| Q7 | Meio de pagamento | Não fixar; o terminal pergunta | Respondida |
| Q8 | O que é "negada"? E erro técnico vira qual status? | **Opção A**: código 3 → Negada; códigos 2/4 e falhas técnicas → Falhou. Documentar o significado e o motivo em KDoc no mapeamento | Respondida |
| Q9 | Pedido não encontrado na recuperação | Falhou + libera nova tentativa | Respondida |
| Q10 | Campos do comprovante | Ver BR-16 | Respondida |
| Q11 | Decrementar disponibilidade após aprovação? | **Sim**: catálogo − aprovadas, por aparelho (limitação vai para os trade-offs do README) | Respondida |
| Q12 | Credenciais Cielo | Fictícias no emulador; se recusadas, cadastro no Portal Cielo | Respondida |
| Q13 | Como achar eventos de um mês sem rolar a lista toda? | Filtro por mês (chips) + lista agrupada com cabeçalho por mês (A+B). O catálogo JSON passa a cobrir 3–4 meses | Respondida |
| Q14 | O operador deve ser avisado quando uma compra Falhou for aprovada depois (BR-29)? | Por ora **não**: a venda aparece no histórico como Aprovada. O risco é cobrar de novo o comprador se o operador já tentou pagar outra vez; avaliar aviso e estorno em spec futura | Respondida (2026-10-18): aceito sem aviso e sem prazo máximo; reavaliar aviso, prazo e estorno em spec futura |

## 11. Definition of Ready
- [x] Problema, objetivo e métricas definidos
- [x] Histórias seguem INVEST e têm prioridade
- [x] Toda BR está em EARS e tem pelo menos um AC
- [x] Todos os estados de UI e eventos estão mapeados
- [x] Dados classificados (PII / cartão)
- [x] Nenhum `[PRECISA ESCLARECIMENTO]` aberto
- [x] Design disponível (quando houver UI): `ui-spec.md` v0.4.4 aprovada em 2026-10-18

## 12. Changelog
| Versão | Data | Autor | Mudança |
|---|---|---|---|
| 0.1.0 | 2026-10-17 | Rafael Oliveira (com Claude Code) | Versão inicial a partir do case e do research.md |
| 0.2.0 | 2026-10-17 | Rafael Oliveira (com Claude Code) | Respostas Q1–Q7, Q9, Q10, Q12; histórico de compras (US-08, BR-18…BR-21, AC-18…AC-21); tela de resultado = comprovante |
| 1.0.0 | 2026-10-17 | Rafael Oliveira | Aprovada. Q8 opção A (código 3 = Negada; status Falhou para falhas técnicas, fora do histórico); Q11 sim (BR-17: catálogo − aprovadas) |
| 1.1.0 | 2026-10-17 | Rafael Oliveira (com Claude Code) | Navegação por mês (Q13): US-09, BR-22…BR-24, AC-22…AC-24, `OnMonthSelected`. Pendentes P1/P3 da UI Spec |
| 1.1.0 | 2026-10-17 | Rafael Oliveira | Aprovada com P1 e P3 da UI Spec: BR-25 (resposta ilegível consulta a Cielo antes de nova cobrança; AC-14 reescrito); BR-15 define o destino quando o pagamento não é encontrado (AC-15) |
| 1.2.0 | 2026-10-18 | Rafael Oliveira (com Claude Code) | Estados presos achados na revisão do lote D: BR-26/AC-25 (consulta que falha → erro com retry da consulta), BR-27/AC-26 (`PurchaseUiState.Unavailable`), BR-28/AC-27 (`ReceiptUiState.Error`). Decisão humana na revisão |
| 1.2.0 | 2026-10-18 | Rafael Oliveira | Aprovada (BR-26..28, AC-25..27) |
| 1.3.0 | 2026-10-18 | Rafael Oliveira (com Claude Code) | Achados da T25 no emulador, recomendações aprovadas pelo humano: BR-29/AC-28 (pagamento aprovado depois de "não encontrado" vira Aprovada); AC-25 ajustado (o erro aparece na tela da compra); fora do escopo: reabrir o app após venda concluída; Q14 aberta |
| 1.3.0 | 2026-10-18 | Rafael Oliveira | Aprovada. Q14 respondida: aprovação tardia sem aviso ao operador e sem prazo máximo, risco de cobrança dupla aceito (aviso, prazo e estorno ficam para spec futura); nota do AC-25 corrigida (compra aberta ilegível abre na lista de eventos, sem cobrança) |
| 1.3.0 | 2026-10-18 | Rafael Oliveira (com Claude Code) | Implementada. T01–T30 concluídas, `verify.sh full` verde, análise de consistência (T27) sem divergência de comportamento. Pendências aceitas: J7 e J9 sem execução no AVD; TalkBack, AVD API 30+ e ordenação do `orders` em terminal real adiados |
