# `:core:database`

Banco local Room do app (ADR-0007).

**Spec:** [docs/specs/001-venda-de-ingressos](../../docs/specs/001-venda-de-ingressos/spec.md) ·
**Camada:** data (core) · **Plano:** [plan.md](../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
`BilheteriaDatabase` (Room v1), `PurchaseDao`, `PurchaseEntity`, `EventQuantity`; módulo Hilt `DatabaseModule`. Expõe `room-runtime` (`api`) porque os tipos estendem classes do Room.

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
