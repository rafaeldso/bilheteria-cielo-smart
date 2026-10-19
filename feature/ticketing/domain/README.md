# `:feature:ticketing:domain`

Regras de negócio da venda de ingressos, em Kotlin puro.

**Spec:** [docs/specs/001-venda-de-ingressos](../../../docs/specs/001-venda-de-ingressos/spec.md) · [002](../../../docs/specs/002-qr-code-ingresso/spec.md) ·
**Camada:** domain · **Plano:** [plan.md](../../../docs/specs/001-venda-de-ingressos/plan.md)

## API pública
Modelos, `PurchaseRepository`, `EventCatalogRepository`, `PaymentGateway` e use cases. Spec 002: `TicketCode`/`Ticket`/`TicketSheet`/`QrMatrix`, `PrintStatus`/`PrintOutcome`, portas `QrEncoder`, `TicketPrinter`, `TicketFiles` e os use cases `GenerateTickets`, `ObserveTickets`, `PrintTickets`, `ShareTickets`, `LoadTicketSet` e `SweepTicketFiles`.

## Dependências
<!-- module-graph:start -->
<!-- Gerado no build; não edite à mão. -->
<!-- module-graph:end -->

## Invariantes
- Nenhum `import android.*` (Konsist S-06). Dinheiro em `Money` (centavos `Long`), tempo em `Instant` com `Clock` injetado.
- O conteúdo do QR Code (`TicketCode`) é só `bilheteria:1:<reference>:<n>` e nunca aparece em `toString()`, log ou exceção.

## Como testar
```bash
./gradlew :feature:ticketing:domain:test
```
