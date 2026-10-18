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

## Como testar
```bash
./gradlew :feature:ticketing:data:testDebugUnitTest
```
