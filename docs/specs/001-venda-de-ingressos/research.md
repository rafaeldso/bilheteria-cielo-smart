<!--
  Copyright 2026 Rafael Oliveira

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Research: integração de pagamento Cielo Smart / LIO (spike)

Base: `https://docs.cielo.com.br/cielo-smart/docs/` (abreviada `…/docs/`). Tudo abaixo vem das páginas lidas;
o que não consta está marcado `[NÃO DOCUMENTADO]`. Observação metodológica: o leitor de páginas devolveu
resumos (não texto literal) de várias páginas; trechos de código e JSON completos de resposta não puderam ser
copiados e devem ser conferidos na fonte antes do plano (ver seção 10).

## 1. Resumo executivo e recomendação

- A Cielo Smart oferece dois modelos: Deep Link (app Android chama `lio://…` e recebe o retorno por scheme/host próprios)
  e Integração Remota (PDV chama APIs do Order Manager) (`…/modelos-de-integracao`). A página de modelos não traz prós/contras.
- A doc do emulador recomenda Deep Link; o SDK da Integração Local (`com.cielo.lio:order-manager:1.7.1`) consta como
  descontinuado, só com correções (README do sample Local).
- A política proíbe SDK do fabricante; tudo passa por `lio://` (UriApp) (`…/politica-de-integracao-camada-de-abstracao-obrigatoria`).
- O pagamento é um JSON em Base64 enviado em `lio://payment?request=<base64>&urlCallback=order://response`; o resultado volta
  por intent em `order://response?response=<base64>&responsecode=0`.
- Anti-duplicidade: existem `reference` (opcional) e consulta de pedido por `orderId` ou por `amount+authCode+cieloCode`;
  não há chave de idempotência documentada.
- Emulador: `lio-emulator.apk` 1.61.8, só Android 7 ou 10; simula Success/Cancelled/Error.

**Recomendação: Deep Link.** Prós (doc): recomendado pelo guia do emulador; sem SDK complexo nem dependência de API, menos
conflito de bibliotecas e melhor compatibilidade entre dispositivos e versões (`…/deep-link-exemplo-de-codigo`); SDK Local descontinuado
e distribuído por `.m2` local (copiar diretórios `com` e `cielo`). Contras: contrato de retorno por scheme/host no manifest, retorno
dependente de o processo do app estar vivo (FAQ pede serviço em primeiro plano), `responsecode` não discrimina erro (seção 4), e
o JSON de resposta precisa de parsing próprio. Integração Local: tipada (`createDraftOrder`, `placeOrder`, `checkoutOrder` com
`PaymentListener`: `onStart/onPayment/onCancel/onError`), mas descontinuada e exige repositório local.

## 2. Política de camada de abstração obrigatória

Fonte: `…/politica-de-integracao-camada-de-abstracao-obrigatoria`.
- Vale para Cielo Smart e LIO.
- Proibido acessar o hardware via SDK/bibliotecas do fabricante, para qualquer função: pagamento, cancelamento, consulta de pedido,
  impressão, leitura/escrita Mifare.
- Obrigatório: pagamentos, pedidos e impressão via deep link `lio://` (componente UriApp). Ações: `payment`, `payment-reversal`,
  `order`, `orders`, `print`, `products`, `enabledproducts`, `establishments`, `terminalinfo`.
- Mifare/cashless: app `cielo.lio.cashless` via Intent (fora do escopo do case).
- Motivo declarado: fabricantes mudam hardware; chamar SDK direto ignora validações, controle de sessão, formato de callback e tratamento
  de erro, e não tem suporte da Cielo.
- Implicação para o projeto: nenhuma chamada direta ao hardware; necessidade não coberta deve ser solicitada à Cielo. O design deve
  isolar `lio://` atrás de uma interface de domínio (gateway de pagamento), coerente com a Clean Architecture do projeto.

## 3. Deep Link: pagamento

Fontes: `…/pagamento`, `…/deep-link-exemplo-de-codigo`, `…/configurando-o-android-manifest`, `…/autenticacao-e-credenciais`,
`…/servico-em-primeiro-plano`, `…/integracao-via-deep-link`, README do sample Local (seção "Integração Via Deeplink").

**Chamada.** Monta-se um JSON, converte-se para Base64 e abre-se (intent `ACTION_VIEW`, flag `FLAG_ACTIVITY_NEW_TASK`, com try/catch
para app ausente ou link inválido):
`lio://payment?request=$base64&urlCallback=order://response` (string do sample). `urlCallback` é o "contrato de resposta" (scheme
`order`, host `response`), que deve coincidir com o manifest. Detalhe de encoding do Base64 (flags, URL-safe) `[NÃO DOCUMENTADO]`.

**JSON de pagamento** (exemplo da doc; `Obrigatório` conforme tabela da doc):

| Campo | Tipo | Obrig. | Descrição |
|---|---|---|---|
| `accessToken` | string | Sim | Token de acesso da Cielo |
| `clientID` | string | Sim | Identificador do cliente na Cielo |
| `items` | array | Sim | Itens: `name`, `quantity`, `sku`, `unitOfMeasure`, `unitPrice` (exemplo) |
| `value` | string | Sim | Total em centavos (ex.: `"1000"` = R$ 10,00) |
| `reference` | string | Não | Referência do pedido, "controle interno" |
| `merchantCode` | string | Não | Multi-EC: `00` + EC (10 dígitos) + loja (4), 16 dígitos |
| `email` | string | Não | E-mail do cliente |
| `installments` | inteiro | Não | Nº de parcelas; 0 = à vista |
| `paymentCode` | string | Não | Meio de pagamento (seção abaixo) |

Inconsistências na própria doc: o exemplo usa `"value": "10"` e `"unitPrice": 10` (contradiz "centavos"); a tabela de `items`
cita `unit_price`/`unit_of_measure`/`id` enquanto o exemplo usa `unitPrice`/`unitOfMeasure`/`sku`. Tipo e unidade de `unitPrice`
`[NÃO DOCUMENTADO]` com clareza; validar no emulador.

**`paymentCode`** (`…/valores-aceitos-no-campo-paymentcode`): `CREDITO_AVISTA`, `CREDITO_PARCELADO_LOJA`, `CREDITO_PARCELADO_ADM`,
`CREDITO_PARCELADO_BNCO`, `DEBITO_AVISTA`, `PIX`, `PRE_AUTORIZACAO`, `FROTAS`, vouchers (`VOUCHER_ALIMENTACAO`, `VOUCHER_REFEICAO` etc.),
cartão da loja e crediário (lista completa na fonte). Se omitido, o emulador pede produto primário/secundário
(`…/realizando-pagamentos`); comportamento no terminal real `[NÃO DOCUMENTADO]`.

**Manifest** (`…/configurando-o-android-manifest`, FAQ):
- Android 11+: `<queries>` com o pacote `com.ads.lio.uriappclient` (UriApp), no nível de `<application>`.
- Activity de resposta com intent-filter `VIEW` + `DEFAULT` e `<data>` com scheme `order` e host `response` (renomeáveis, desde que
  iguais ao `urlCallback`).
- `<meta-data android:name="cs_integration_type" android:value="uri" />` dentro de `<application>`; sem ele ocorre o erro
  "metadata de integração não encontrado" (FAQ).
- Serviço: `android:exported="false"` e `android:foregroundServiceType="dataSync"`.

**Credenciais** (`…/autenticacao-e-credenciais`): cadastrar o app no Portal de Desenvolvedores Cielo (ícone, nome, descrição), escolher a
API "Cielo Smart - Order Manager" e registrar; Client-ID e Access Token ficam em Perfil > Client-IDs Cadastrados. A FAQ diz que é preciso
cadastro para obter tokens, mesmo para testar. Se o emulador aceita valores fictícios `[NÃO DOCUMENTADO]`. Segredos não devem ir ao
repositório (regra do projeto).

**Serviço em primeiro plano** (`…/servico-em-primeiro-plano`): iniciar o serviço (canal + notificação + `startForeground` em `onCreate`,
`onStartCommand` retorna `START_NOT_STICKY`) antes de lançar o deep link; parar quando não for mais necessário. Motivo (FAQ): o Android
pode encerrar o app em segundo plano e a transação não retornar ao app.

**Retorno** (`…/recuperando-dados`): na activity de resposta, ler `intent.data`, extrair o query param `response`, decodificar Base64 e
fazer parse do JSON.
- Sucesso: JSON do pedido com `id`, `createdAt`, `updatedAt`, `status`, `type`, `reference`, `price`, `paidAmount`, `pendingAmount`,
  `notes`, `items[]`, `payments[]` (bandeira, `authCode`, `cieloCode`, `merchantCode`, `terminal`, número mascarado, `amount`,
  `paymentFields`). `paymentFields.statusCode`: 0 = Pix, 1 = pagamento autorizado, 2 = cancelamento.
- Erro: JSON `{ "code": <número>, "reason": "<texto>" }` (ex.: `1` / "CANCELADO PELO USUÁRIO").
- URI de retorno: `order://response?response=<base64>&responsecode=0`. Nos exemplos de erro e de cancelamento o `responsecode` também
  aparece como `0` (README e `…/cancelamento`); na listagem o exemplo de erro usa `responsecode` 2 (`…/listagem-de-pedidos`).
  Semântica fiel de `responsecode` `[NÃO DOCUMENTADO]`.

## 4. Resultados do pagamento e mapeamento

**Códigos de erro** (`…/codigos-de-erro`):

| Código | Descrição |
|---|---|
| 1 | Cancelado pelo usuário |
| 2 | Erro genérico |
| 3 | Erro no pagamento |
| 4 | Erro de autenticação |

Na consulta avulsa, "Order not found" vem com `code` 2 (`…/consulta-de-pedido-avulso`).

**Mapeamento proposto (inferência, a validar):**

| Retorno | Resultado do case |
|---|---|
| JSON de pedido com `payments[].paymentFields.statusCode = 1` (ou `0` no Pix) e `status` pago | Aprovada |
| `{code:1}` "Cancelado pelo usuário" | Cancelada |
| `{code:3}` "Erro no pagamento" | Negada? `[PRECISA ESCLARECIMENTO]` (a doc não diferencia "negada pelo emissor" de erro) |
| `{code:2}` / `{code:4}` | Erro de integração (genérico / autenticação) |
| Nenhum retorno (timeout, processo morto) | Estado indeterminado: consultar pedido antes de qualquer reenvio |

Status do pedido (`…/fluxo-e-status-do-pedido`): 0 Draft, 1 Entered, 2 Paid, 3 Closed, 4 Re-entered. Valor exato do campo `status`
no JSON de retorno por deep link (texto vs número) `[NÃO DOCUMENTADO]` nas páginas lidas. Como a negação pelo emissor chega
(code 3? payment com status diferente?) `[NÃO DOCUMENTADO]`.

## 5. Cancelamento

Fontes: `…/cancelamento`, `…/cancelando-pagamentos`.
- Chamada: `lio://payment-reversal?request=$base64&urlCallback=order://response`.
- JSON (exemplo; obrigatoriedade não informada): `id` (id da ordem), `clientID`, `accessToken`, `cieloCode`, `authCode`, `value` (número).
- Retorno de sucesso: JSON do pedido; em `payments`, `statusCode` 1 = pagamento e 2 = cancelamento. Erro: `{code, reason}`.
- Prazos no terminal Smart: crédito até 30 dias; débito somente no mesmo dia; Pix até 90 dias; cancelamento parcial ou total em todos.
- Em produção são exigidos campos adicionais; o emulador pula essa etapa. Quais campos `[NÃO DOCUMENTADO]`.
- O case não exige cancelamento pós-venda; "cancelada" no case refere-se ao usuário abortar o pagamento (code 1).

## 6. Recuperação e consulta de pedido; anti-duplicidade

- `lio://orders` (`…/listagem-de-pedidos`): JSON `{pageSize, page, clientID, accessToken}`; `page` base 0; `pageSize` máximo 5. Retorna
  `currentPage`, `totalItems`, `totalPages`, `results[]` com itens e transações. Ordenação/filtro por `reference` `[NÃO DOCUMENTADO]`.
- `lio://order` (`…/consulta-de-pedido-avulso`), `urlCallback=order://response`: por `orderId` (+ `clientID`, `accessToken`) ou por
  `amount` + `authCode` + `cieloCode` (+ credenciais). Retorna status, valores, itens e `transactions[]`; não encontrado = `code` 2
  "Order not found".
- Identificadores: `id` (orderId, devolvido no retorno), `reference` (definido pelo app, opcional), `authCode`, `cieloCode` (NSU).
- Idempotência: `[NÃO DOCUMENTADO]` (nenhuma chave de idempotência nem garantia de unicidade de `reference` nas páginas lidas).
- Mecanismos aproveitáveis (a decisão é nossa, não da doc):
  1. Gerar localmente um id de tentativa/compra e enviá-lo em `reference`; persistir a tentativa (Room) como "em andamento" antes de abrir o deep link.
  2. Desabilitar o botão e ignorar eventos repetidos enquanto houver tentativa em andamento (UDF).
  3. Após morte do processo ou retorno ausente, não reenviar: usar `lio://orders` (e filtrar por `reference` no cliente, 5 por página)
     ou `lio://order` com `orderId`/`authCode`+`cieloCode`+`amount` quando conhecidos.
  4. Foreground service para reduzir a perda do retorno (FAQ).
- Lacuna: sem `orderId` antes do retorno, a recuperação depende de varrer `orders` por `reference`; viabilidade `[PRECISA ESCLARECIMENTO]`.

## 7. Integração Local (SDK)

Fonte: README do sample (`github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local`).
- Dependência: `implementation 'com.cielo.lio:order-manager:1.7.1'` (última listada); com deep link usando o SDK, versão 2.1.0 ou superior.
- Repositório: sem Maven público documentado; copiar os diretórios `com` e `cielo` do download do SDK (Portal de Desenvolvedores) para
  `.m2/repository` e usar `mavenLocal()`. Isso conflita com a regra do projeto de dependências aprovadas/estáveis.
- SDK descontinuado, só correções; a partir da 1.17.7-beta exige permissão `INTERNET`.
- minSdk 24 e targetSdk 29 (requisitos de deep link citados no README); app deve estar adaptado à Cielo Smart até 15/10/2025; WebView
  não é permitida na Cielo Smart.
- O sample contempla pagamento, cancelamento e impressão via deep link também (módulo único `app`).
- FAQ: linguagens suportadas Java ou Kotlin.

## 8. Emulador

Fontes: `…/emulador-cielo`, `…/baixando-o-emulador-cielo`, `…/realizando-pagamentos`, `…/cancelando-pagamentos`, `…/integracao-via-deep-link`.
- APK `lio-emulator.apk` v1.61.8. "Só pode ser instalado em dispositivos com Android 7 ou 10" (FAQ: 7.1 ou 10; tablets, celulares ou AVD).
  Pode exigir "fontes desconhecidas". ABI `[NÃO DOCUMENTADO]` (escolher imagem de AVD compatível com o APK; verificar).
- Proibido instalar em terminais debug.
- Pagamento: o app de integração dispara o deep link; o emulador mostra o `amount`, produto primário/secundário (desabilitados se vierem
  na requisição), EC e parcelas (se crédito parcelado). Ao tocar em Pagar, escolhe-se a resposta: Success, Cancelled ou Error.
  Não há simulação de "negada pelo emissor" documentada; Error é genérico.
- Cancelamento: "Cancelar Ordem", "Confirmar Cancelamento" e escolha do tipo de resposta.
- Também simula consulta de pedidos e de meios de pagamento; tela inicial tem Settings (respostas automáticas de impressão) e Clear data.
- Limitação: funciona de forma confiável até Android 10 (FAQ). Credenciais: a página não cita login; se o app de integração precisa de
  token real `[NÃO DOCUMENTADO]`.
- Fornece o app Sample (GitHub) para validar o ambiente.

## 9. Terminais

Fonte: `…/terminais-e-caracteristicas-da-cielo-smart`.

| Modelo | Android | Tela | Resolução |
|---|---|---|---|
| DX8000 | 10 (Security OS) | 5,5" AMOLED | 720x1280 |
| L300 v3 (LIO V3) | 7.1 | 5" AMOLED | 720x1280 |
| L400 | 11 | 6,5" TFT IPS | 720x1600 |
| L300 v4 | 11 | 5" IPS | 480x854 |
| N950U | 12 | 5,99" TFT LCD | 1440x720 |

Restrições: minSdk 24 / targetSdk 29 citados no README (deep link); a página de boas práticas manda configurar corretamente
compileSdk/targetSdk/minSdk e usar navegação nativa e Material Design; Kiosk Mode (Device Owner) ainda indisponível; telas pequenas
(480x854 a 1440x720) e 1 a 2 GB de RAM. Boas práticas: não expor tokens, monitorar crashes, fluxos simples. minSdk 24 cobre Android 7.0+,
mas o L300 v3 roda 7.1 e o emulador 7/10; targetSdk 29 vs. políticas atuais de publicação `[PRECISA ESCLARECIMENTO]`.

## 10. Riscos e perguntas em aberto

- `[PRECISA ESCLARECIMENTO]` Como distinguir "negada" de "erro" (`code` 3 vs payment com status não pago)? O emulador só simula
  Success/Cancelled/Error. Sugestão: tratar Error como "negada/falhou" e documentar a limitação na spec.
- `[PRECISA ESCLARECIMENTO]` Os valores reais de `status`/`statusCode` no retorno de sucesso e `responsecode` (0 também em erro): confirmar
  rodando o emulador e capturando os JSONs.
- `[PRECISA ESCLARECIMENTO]` Idempotência: `reference` é única/garantida? Filtro por `reference` em `orders`? Estratégia de recuperação após
  morte do processo (seção 6).
- `[PRECISA ESCLARECIMENTO]` Credenciais: o avaliador fornecerá Client-ID/Access Token? O emulador aceita valores fictícios? Injeção por
  `local.properties`/BuildConfig sem versionar.
- `[PRECISA ESCLARECIMENTO]` Valores monetários: `value` em centavos (string) e `unitPrice` (unidade?) divergem nos exemplos.
- `[PRECISA ESCLARECIMENTO]` minSdk 24/targetSdk 29 vs. padrão do projeto; Android 7 ou 10 no AVD; ABI do APK.
- `[PRECISA ESCLARECIMENTO]` `paymentCode`: fixar (ex.: `CREDITO_AVISTA`) ou deixar o usuário escolher no terminal?
- Risco: página `pagamento` lida via resumo, sem a seção de URI/callback; strings de URI vêm do README do sample e do cancelamento.
  Conferir texto e códigos na fonte antes do `plan.md`.
- Risco: SDK Local descontinuado; Flutter/React samples não mostram código textual (imagens).
- Risco: o retorno depende do app vivo; usar foreground service e persistir tentativa.
- Não localizado: sample Deep Link dedicado na org DeveloperCielo (o sample Local inclui seção de deep link; há ainda
  `LIO-Hybrid-Integration-Sample-Flutter` e o exemplo React Native `matheus-caldeira/cielo_sample`).

## 11. Fontes

- `…/docs/politica-de-integracao-camada-de-abstracao-obrigatoria`
- `…/docs/modelos-de-integracao`
- `…/docs/emulador-cielo`, `…/docs/baixando-o-emulador-cielo`, `…/docs/realizando-pagamentos`, `…/docs/cancelando-pagamentos`
- `…/docs/deep-link-exemplo-de-codigo`, `…/docs/autenticacao-e-credenciais`, `…/docs/configurando-o-android-manifest`
- `…/docs/codigos-de-erro`, `…/docs/servico-em-primeiro-plano`, `…/docs/pagamento`, `…/docs/recuperando-dados`
- `…/docs/listagem-de-pedidos`, `…/docs/valores-aceitos-no-campo-paymentcode`, `…/docs/consulta-de-pedido-avulso`, `…/docs/cancelamento`
- `…/docs/terminais-e-caracteristicas-da-cielo-smart`, `…/docs/boas-praticas-para-desenvolvedores`, `…/docs/integracao-via-deep-link`
- `…/docs/funcionalidades-1`, `…/docs/fluxo-e-status-do-pedido`, `https://docs.cielo.com.br/cielo-smart/llms.txt`
- https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local
- https://github.com/DeveloperCielo/LIO-Hybrid-Integration-Sample-Flutter
- https://github.com/orgs/DeveloperCielo/repositories (listagem)
