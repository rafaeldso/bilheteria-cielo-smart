# `:core:database`

Banco local Room do app (ADR-0007).

**Spec:** [docs/specs/001-venda-de-ingressos](../../docs/specs/001-venda-de-ingressos/spec.md) ·
**Camada:** data (core) · **Plano:** [plan.md](../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
`BilheteriaDatabase`, `PurchaseDao`, `PurchaseEntity` (a criar na T12).

## Dependências
<!-- module-graph:start -->
<!-- Gerado no build; não edite à mão. -->
<!-- module-graph:end -->

## Invariantes
- Nenhum dado de cartão. Migrações versionadas e testadas; schema exportado em `schemas/`.

## Como testar
```bash
./gradlew :core:database:testDebugUnitTest
```
