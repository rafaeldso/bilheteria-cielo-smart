---
name: write-tests
description: Escreve testes ligados aos critérios de aceite (AC-xx) da spec — use case, ViewModel com Turbine, repositório com fakes, UI/screenshot — seguindo o guia de testes. Use ao implementar tarefas de teste ou quando um AC estiver sem cobertura.
---

# Escrever testes

1. Liste os ACs da tarefa e leia `docs/harness/guides/testing.md`.
2. Nome: `` `AC-xx - dado <contexto> quando <ação> entao <resultado>` ``.
3. Base: `docs/harness/templates/kotlin/FeatureViewModelTest.kt`.
4. Use fakes de `core:testing` para repositórios; `MainDispatcherExtension` + `StandardTestDispatcher` injetado; `runTest`.
5. Verifique o comportamento observável (`UiState`, retorno do use case), não detalhes internos.
6. Cubra erros da matriz do plano, não só o caminho feliz.
7. Rode o teste do módulo e confirme que **falha** se o comportamento for removido (o teste precisa poder falhar).
8. Rode `scripts/harness/check-traceability.sh docs/specs/<NNN-feature>`.
