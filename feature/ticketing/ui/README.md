# `:feature:ticketing:ui`

Telas de eventos, compra, comprovante e histórico (Compose, UDF).

**Spec:** [docs/specs/001-venda-de-ingressos](../../../docs/specs/001-venda-de-ingressos/spec.md) ·
**Camada:** ui · **Plano:** [plan.md](../../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
Grafo de navegação da feature (a criar na T24).

## Dependências
<!-- module-graph:start -->
<!-- Gerado no build; não edite à mão. -->
<!-- module-graph:end -->

## Invariantes
- Não depende de `:data`. ViewModel com um `StateFlow` e um `onEvent`; `Screen` stateless.

## Como testar
```bash
./gradlew :feature:ticketing:ui:testDebugUnitTest
```
