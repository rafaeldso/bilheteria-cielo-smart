---
name: android-feature-scaffold
description: Cria a estrutura de módulos de uma nova feature (api, domain, data, ui) com convention plugins, namespaces, pacotes e registro no settings.gradle.kts. Use quando o plano aprovado exigir uma feature/módulo novo.
---

# Scaffold de feature

Requer aprovação (criar módulo está em "Pergunte antes").

1. Confirme o nome (`kebab` para pasta, `camelCase` para pacote) e os módulos previstos no plano.
2. Crie, seguindo `docs/harness/guides/architecture.md` e `gradle.md`:
   ```
   feature/<nome>/api/build.gradle.kts      → plugins { alias(libs.plugins.app.android.library) }
   feature/<nome>/domain/build.gradle.kts   → plugins { alias(libs.plugins.app.jvm.library) }
   feature/<nome>/data/build.gradle.kts     → plugins { alias(libs.plugins.app.android.feature.data) }
   feature/<nome>/ui/build.gradle.kts       → plugins { alias(libs.plugins.app.android.feature.ui) }
   ```
   Copie o `build.gradle.kts` de uma feature existente se houver.
3. `namespace = "<base.package>.feature.<nome>.<camada>"`; pacotes Kotlin iguais.
4. Dependências: `ui → domain, api`; `data → domain`; `domain → core:common`. Nada de feature → feature (exceto `:api`).
5. Adicione os `include(...)` em `settings.gradle.kts`.
   Crie o `README.md` de cada módulo a partir de `docs/harness/templates/module-README.md`
   (skill `summarize module` depois da primeira implementação).
6. Rode `scripts/harness/verify.sh fast` (Konsist valida estrutura) e `./gradlew :feature:<nome>:ui:assembleDebug`.
