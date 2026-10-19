# `:feature:ticketing:api`

Contrato público da feature de venda de ingressos: rotas de navegação type-safe.

**Spec:** [docs/specs/001-venda-de-ingressos](../../../docs/specs/001-venda-de-ingressos/spec.md) · [002](../../../docs/specs/002-qr-code-ingresso/spec.md) ·
**Camada:** api · **Plano:** [plan.md](../../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
`EventListRoute`, `PurchaseRoute(eventId)`, `ReceiptRoute(purchaseId)`, `PurchaseHistoryRoute`, `TicketsRoute(purchaseId)` (spec 002) (`navigation/TicketingRoutes.kt`). O nome de cada propriedade é a chave do `SavedStateHandle` lida pelos ViewModels; um teste cruzado em `:feature:ticketing:ui` falha se um lado mudar.

## Dependências
<!-- module-graph:start -->
<!-- Gerado no build; não edite à mão. -->
<!-- module-graph:end -->

## Invariantes
- Sem Compose, Hilt, Android ou domain de outra feature; `explicitApi()`.

## Como testar
```bash
./gradlew :feature:ticketing:api:test
```
