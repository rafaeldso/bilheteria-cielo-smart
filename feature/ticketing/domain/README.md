# `:feature:ticketing:domain`

Regras de negócio da venda de ingressos, em Kotlin puro.

**Spec:** [docs/specs/001-venda-de-ingressos](../../../docs/specs/001-venda-de-ingressos/spec.md) ·
**Camada:** domain · **Plano:** [plan.md](../../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
Modelos, `PurchaseRepository`, `EventCatalogRepository`, `PaymentGateway` e use cases (a criar na T04).

## Dependências
<!-- module-graph:start -->
<!-- Gerado no build; não edite à mão. -->
<!-- module-graph:end -->

## Invariantes
- Nenhum `import android.*` (Konsist S-06). Dinheiro em `Money` (centavos `Long`), tempo em `Instant` com `Clock` injetado.

## Como testar
```bash
./gradlew :feature:ticketing:domain:test
```
