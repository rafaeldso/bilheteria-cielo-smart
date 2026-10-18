# `:feature:ticketing:data`

Catálogo de eventos (JSON em assets), compras (Room) e integração Cielo Smart por Deep Link (ADR-0010).

**Spec:** [docs/specs/001-venda-de-ingressos](../../../docs/specs/001-venda-de-ingressos/spec.md) ·
**Camada:** data · **Plano:** [plan.md](../../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
Nada público: tudo `internal`, ligado por Hilt.

## Dependências
<!-- module-graph:start -->
<!-- Gerado no build; não edite à mão. -->
<!-- module-graph:end -->

## Invariantes
- `lio://`, Base64 e DTOs Cielo só no pacote `cielo` (Konsist). Resposta bruta da Cielo nunca logada nem persistida.

- Credenciais Cielo: propriedades Gradle `cielo.clientId` e `cielo.accessToken` (`~/.gradle/gradle.properties` ou `-P`; nunca
  versionadas) viram `BuildConfig`; sem elas o build usa valores fictícios. O gateway só envia o `lio://` ao pacote resolvido que
  está na allowlist (`com.ads.lio.uriappclient`, `br.com.cielosmart.orderservice`), com intent explícita.

## Fatos verificados no `lio-emulator` (T25, 2026-10-18)
Ambiente: AVD Android 10 (`google_apis`, arm64), `lio-emulator` 1.61.8 (`br.com.cielosmart.orderservice`), credenciais
fictícias do build. O emulador **não** reproduz um terminal real: o que depende dele está marcado.

| # | Pergunta | Resultado |
|---|---|---|
| 1 | Código do botão "Erro" | `{"code":2,"reason":"Falha no processo de pagamento"}`, **sem** `reference` e sem `responsecode`. O app registra `Failed`/`Generic` ("Falhou", com nova tentativa; BR-13). O código 3 (Negada) não é produzido pelo emulador. |
| 2 | Unidade de `unitPrice` | Centavos: `unitPrice`, `amount` e `price` = `8000` para R$ 80,00, igual ao valor exibido pelo emulador. |
| 3 | Listagem de `orders` | A requisição com `page: 0` volta com `currentPage: 1` (o `FIRST_CIELO_PAGE = 1` do parser está certo). A lista do emulador é fixa (um pedido de exemplo `ORDER #1` e só a primeira venda aprovada; cancelados e erros não entram), então a **ordenação não pôde ser determinada**: validar em terminal real antes de confiar em `pageSize = 5` para recuperar vendas antigas. |
| 4 | Percent-encoding do Base64 | O emulador aceita a URI com o `=` de padding literal e responde Base64 sem encoding. As amostras não tinham `+` nem `/`, então esses dois caracteres continuam sem prova. |
| 5 | Callback sem `BROWSABLE` | O callback `order://response` chega (disparado pelo app da Cielo). Link/QR vindo de um navegador **não foi testado** (sem navegador no AVD); decisão do humano (2026-10-18): manter o callback sem `BROWSABLE`, com o risco aceito do plano §7. |
| 6 | `<queries>` só com pacotes | O app da Cielo é resolvido e aberto, **mas no Android 10 não existe filtro de visibilidade de pacotes**: o teste só vale a partir do Android 11 (precisa de AVD API 30+ ou terminal real). |
| 7 | Foreground service e tema do callback | Android 10 não tem `POST_NOTIFICATIONS`; a notificação usa o canal `payment_in_progress` (importância baixa, visibilidade secreta). Android 13+ e o piscar do tema translúcido não foram verificados. |
| 8 | Credenciais fictícias (Q12) | Aceitas para pagamento e listagem (`clientID` fictício). |

Resposta de sucesso: `order://response?response=<Base64>&responsecode=0`; o `reference` devolvido é o enviado, o `status`
é `PAID` e os campos da transação vêm em `payments[]` (o `paymentFields.pan` do emulador é fictício e nunca é lido nem
persistido).

**Pedido abandonado no emulador:** se o app morre com o terminal aberto, o emulador mantém aquele pedido e devolve o
`reference` **antigo** no próximo pagamento. O app o ignora (a compra atual fica `Pending` até a reabertura recuperar pela
BR-15), mas o pedido antigo pode ficar `PAID` na Cielo com a compra local já em `Failed`/`NotFound`. Essa aprovação tardia é
tratada pela BR-29 (spec 1.3.0): o `HandlePaymentOutcomeUseCase` aprova a compra `Failed`/`NotFound` quando chega `PAID` com o
mesmo `reference` e valor, sem tocar a compra aberta.

## Como testar
```bash
./gradlew :feature:ticketing:data:testDebugUnitTest
```
