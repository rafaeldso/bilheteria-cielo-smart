---
paths:
  - "**/ui/src/**/*.kt"
  - "**/designsystem/src/**/*.kt"
---
# Camada de UI (Compose)
Antes de editar, leia [compose-ui.md](../../docs/harness/guides/compose-ui.md) e use a skill `compose-screen`.
Padrões: `docs/harness/templates/kotlin/FeatureScreen.kt` e `FeatureViewModel.kt`.
Um `@Preview` por estado da spec; Screen stateless. Fecha com `compose-ui-reviewer` e, se houver emulador, `verify-on-device`.
