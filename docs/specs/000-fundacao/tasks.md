# Tarefas: 000 — Fundação

> Infraestrutura sem comportamento de produto (sem `spec.md`, aprovado no [roadmap](../../roadmap.md)).
> Checklist de origem: [bootstrap.md](../../harness/bootstrap.md). Itens S1–S10 do roadmap estão fora.

| ID | Tarefa | Pronto quando | Depende | Status |
|---|---|---|---|---|
| T01 | Ambiente: JDK 21, Android SDK, `gh` autenticado | `java_home -v 21`, `gh auth status` ok | — | ✅ |
| T02 | `git init`, hooks, commit template, identidade noreply | `git config core.hooksPath` = `.githooks` | T01 | ✅ |
| T03 | Projeto base a partir de `android create empty-activity` (pacote `com.rafaeloliveira.bilheteria`, `minSdk 24`) | `./gradlew help` ok; wrapper com `distributionSha256Sum` | T02 | ✅ |
| T04 | Version catalog com a stack do ADR-0007 necessária na fundação (Hilt, KSP, JUnit 5, MockK, Turbine, Timber, sensores) | Só versões estáveis, consultadas na fonte oficial | T03 | ✅ |
| T05 | `build-logic/convention`: `app.android.application`, `app.android.library`, `app.android.compose`, `app.android.feature.ui`, `app.android.feature.data`, `app.jvm.library`, `app.hilt`, `app.quality`, `app.root` | `harnessFast`/`harnessCheck`/`harnessFull` existem | T04 | ✅ |
| T06 | `core:common` (`DataResult`, `AppError`, dispatchers, `Logger`), `core:testing`, `core:designsystem` (tema placeholder) | Módulos compilam; testes de exemplo passam | T05 | ✅ |
| T07 [P] | `quality:architecture-tests` (Konsist) a partir do template | Falha com `android.*` no domain | T05 | ✅ |
| T08 [P] | `quality:detekt-rules` (`SensitiveDataInLogRule`) a partir do template | Regra ativa no detekt | T05 | ✅ |
| T09 | `app`: `Application` Hilt, `MainActivity` com tela placeholder (NavHost entra na 001), cabeçalho de licença via Spotless | App instala no AVD Android 10 | T06 | ✅ |
| T10 | `verify.sh full` verde | Saída do sensor registrada no `progress.md` | T07–T09 | ✅ |
| T11 | CI: ajustar `ci.yml`/`security.yml` aos itens pulados (S4, S5, S7); CD: `release.yml` (APK como artefato no `main`, GitHub Release por tag `v*`) | Workflows válidos (zizmor) | T10 | ✅ |
| T12 | ADR-0009 "Escopo de case": S1–S10 e exceção à Constitution VI.4 | ADR aceito pelo humano | — | ✅ |
| T13 | README: "Rodando localmente" (JDK, SDK, AVD, emulador Cielo) | Passo a passo reproduzível | T10 | ✅ |
| T14 | Repositório privado no GitHub + primeiro push | **Só com pedido explícito do humano** | T10–T13 | ⏳ |
