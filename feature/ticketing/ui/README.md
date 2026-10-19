# `:feature:ticketing:ui`

Telas de eventos, compra, comprovante, histórico e ingressos com QR Code (Compose, UDF).

**Spec:** [docs/specs/001-venda-de-ingressos](../../../docs/specs/001-venda-de-ingressos/spec.md) · [002](../../../docs/specs/002-qr-code-ingresso/spec.md) ·
**Camada:** ui · **Plano:** [plan.md](../../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
`ticketingGraph` e `OpenPurchaseRedirect` (navegação). Telas internas: SCR-01 a SCR-05; a SCR-05 (`tickets/`) é a tela de ingressos da spec 002.

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
