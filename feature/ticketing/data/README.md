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

## Como testar
```bash
./gradlew :feature:ticketing:data:testDebugUnitTest
```
