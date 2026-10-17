---
name: compose-screen
description: Cria ou altera uma tela Jetpack Compose (UiState, UiEvent, ViewModel, Route stateful, Screen stateless, um @Preview por estado) a partir dos estados da spec, com design system e acessibilidade. Use para qualquer tarefa de UI/presentation.
---

# Tela Compose

1. Leia a seção 6 da spec (estados e eventos), a `ui-spec.md` aprovada da feature (telas `SCR-xx`,
   componentes, strings, a11y) e `docs/harness/guides/compose-ui.md`. Abra a prancha
   `design/mockups.html` como referência visual. Sem UI Spec aprovada em feature M/G? Pare e rode `sdd-design`.
2. Use como base `docs/harness/templates/kotlin/FeatureScreen.kt` e `FeatureViewModel.kt`.
3. `UiState`/`UiEvent` **idênticos** à spec (nomes e variantes).
4. `Route`: `hiltViewModel()`, `collectAsStateWithLifecycle()`, efeitos e navegação.
5. `Screen(uiState, onEvent, modifier)`: `when` exaustivo, sem lógica de negócio.
6. Componentes `Ds*` da tabela 4.3 da UI Spec e tokens de `core:designsystem`; strings em `strings.xml`
   com as chaves da tabela 4.4. Divergir da prancha só com nova versão da UI Spec.
7. Acessibilidade: `contentDescription`, 48dp, semântica de heading, fontScale 2f.
8. Um `@Preview` por estado (+ dark mode e fontScale 2f para o estado principal), com o nome do
   estado da UI Spec e os mesmos dados fictícios da prancha (facilita comparar com o screenshot).
9. Valide visualmente quando possível:
   `android studio render-compose-preview` ou build + `android run` + `android screen capture`
   + `android layout --pretty` (checar semântica).
10. Rode `scripts/harness/verify.sh fast`.
