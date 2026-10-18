# `:feature:ticketing:api`

Contrato público da feature de venda de ingressos: rotas de navegação type-safe.

**Spec:** [docs/specs/001-venda-de-ingressos](../../../docs/specs/001-venda-de-ingressos/spec.md) ·
**Camada:** api · **Plano:** [plan.md](../../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
`EventListRoute`, `PurchaseRoute`, `ReceiptRoute`, `PurchaseHistoryRoute` (a criar na T24).

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
