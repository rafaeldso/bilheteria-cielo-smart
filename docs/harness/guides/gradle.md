# Guia: Gradle, Dependências e Módulos

> Baseado em [Gradle best practices](https://docs.gradle.org/current/userguide/best_practices.html),
> [Configure your build](https://developer.android.com/build),
> [Optimize build speed](https://developer.android.com/build/optimize-your-build),
> [Version catalogs](https://developer.android.com/build/migrate-to-catalogs) e no
> [Now in Android](https://github.com/android/nowinandroid/tree/main/build-logic).
> Implementa a Constitution Art. III, V.4 e VI. Stack aprovada em [ADR-0003](../../adr/0003-stack-de-sensores-de-qualidade.md).
> Verificado por: S-03 Spotless, S-08 Android Lint, S-12 module graph assertion,
> S-13 build release (R8), S-15 OSV-Scanner + Dependabot.
> Ver também: [architecture.md](architecture.md) · [security.md](security.md) · [testing.md](testing.md).
> Skills oficiais úteis: `agp-9-upgrade`, `r8-analyzer`.

## Princípios
1. **Convention plugins em `build-logic/` (included build)** são o único lugar com
   configuração compartilhada: SDKs, toolchain, Kotlin, Compose, Hilt, sensores e testes.
   O `build.gradle.kts` de um módulo só aplica plugins, define `namespace` e declara
   dependências. Não use `buildSrc` (invalida o cache de todo o build a cada mudança).
2. **Version Catalog** (`gradle/libs.versions.toml`) é a única fonte de versões e
   coordenadas. Nada de versão inline, versão dinâmica (`+`, `latest.release`) ou
   `-SNAPSHOT`. Bibliotecas com BOM (Compose, OkHttp, Firebase) entram via `platform(...)`.
   **Só versões estáveis:** alpha, beta, RC, milestone e `-dev` são proibidos (inclusive
   em plugins e no BOM), salvo ADR específico. Prefira a última estável ([ADR-0007](../../adr/0007-stack-de-bibliotecas-observabilidade-e-politicas-de-qualidade.md)).
3. **Build reprodutível.** Mesmo comando, mesmo resultado, local e no CI: wrapper
   versionado com checksum, toolchain JDK fixa, repositórios declarados só no `settings`.
4. **Configuração preguiçosa e isolada.** APIs lazy (`register`, `Provider`), sem
   `afterEvaluate`, sem `allprojects {}`/`subprojects {}` e sem um módulo ler o estado
   de outro. Isso mantém o configuration cache válido e prepara o *project isolation*.
5. **Nova dependência exige aprovação** (Art. III). Usar em outro módulo uma biblioteca
   que **já está no catálogo** não é dependência nova; adicionar uma entrada ao catálogo é.
6. **Não altere `build-logic/`, `gradle/` (wrapper, catálogo, verificação) ou
   `settings.gradle.kts` sem pedir** (ver `AGENTS.md`, Limites).

## Estrutura do build
```text
settings.gradle.kts          # pluginManagement, repositórios, includeBuild("build-logic"), include(...)
gradle.properties            # flags de desempenho e do AGP (ver Desempenho)
gradle/
  libs.versions.toml         # Version Catalog
  wrapper/                   # gradle-wrapper.properties com distributionSha256Sum
  verification-metadata.xml  # checksums de dependências (ver Cadeia de suprimentos)
build-logic/convention/      # convention plugins (Kotlin, nunca Groovy)
```

### `settings.gradle.kts`
```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS") // projects.core.common em vez de project(":core:common")
```
- `FAIL_ON_PROJECT_REPOS` impede repositórios declarados por módulo.
- Filtro de conteúdo no `google()` evita resolver artefato de terceiro no repositório errado
  (mitiga *dependency confusion*) e acelera a resolução.
- Proibidos: `mavenLocal()`, `jcenter()` e repositórios HTTP.

## Convention plugins
| ID | Aplica em | Configura |
|---|---|---|
| `app.android.application` | `:app` | SDKs, build types, R8, assinatura via variáveis de ambiente, versionamento, flavors (se houver) |
| `app.android.library` | módulos Android | SDKs, toolchain, Kotlin, testes JUnit 5, `buildConfig = false` |
| `app.android.feature.ui` | `feature:*:ui` | library + compose + hilt + deps de `core:ui`/`designsystem`/`testing` |
| `app.android.feature.data` | `feature:*:data` | library + hilt + deps de `core:network`/`database` |
| `app.jvm.library` | `feature:*:{api,domain}`, `core:common`, `core:model` | Kotlin JVM puro, toolchain, JUnit 5 |
| `app.android.compose` | qualquer módulo com Compose | plugin do compilador Compose, arquivo de estabilidade, previews, Roborazzi |
| `app.hilt` | módulos com DI | Hilt + KSP (nunca `kapt`) |
| `app.quality` | todos, via os plugins acima | Spotless, detekt (+ regras próprias), Kover, Android Lint, module graph assertion |
| `app.root` | raiz | Só as tasks agregadas `harnessFast`/`harnessCheck`/`harnessFull` |

A configuração de qualidade entra pelos plugins de cada módulo, nunca por
`subprojects {}` na raiz. A raiz só agrega tasks.

### Padrões comuns a todos os módulos
| Item | Padrão | Motivo |
|---|---|---|
| Toolchain | `kotlin { jvmToolchain(17) }` em todos os módulos; daemon com critério fixo em `gradle/gradle-daemon-jvm.properties` | Mesmo JDK em qualquer máquina e no CI; 17 é o mínimo do AGP |
| Kotlin | AGP 9 já traz Kotlin embutido: não aplicar `org.jetbrains.kotlin.android`. Compose via `org.jetbrains.kotlin.plugin.compose` | Evita plugin duplicado e versão divergente |
| Warnings | `allWarningsAsErrors = true` quando `-Pci=true` | Warning novo não entra na `main` sem travar o dev local |
| API pública | `explicitApi()` nos módulos `:api` | Todo símbolo do contrato é declarado como público de propósito |
| `buildConfig` | Desligado por padrão; ligar só no módulo que precisa | Menos geração de código; `BuildConfig` nunca guarda segredo (é decompilável) |
| Recursos | Recursos no módulo que os usa; `androidResources` desligado em módulos sem recursos | Build incremental e R menor |
| Namespace | `<base.package>.feature.<nome>.<camada>` / `<base.package>.core.<nome>` | Único por módulo; validado pelo Konsist |

### SDKs e versionamento do app
| Item | Regra |
|---|---|
| `compileSdk` | A última versão estável. Atualizada junto com o AGP. |
| `targetSdk` | Acompanha a exigência anual do Google Play (até um ano após cada versão do Android). Subir exige rodar o checklist de mudanças de comportamento da versão. |
| `minSdk` | Decisão de produto, registrada no bootstrap. Baixá-lo exige ADR. |
| `versionCode` | Inteiro monotônico gerado pelo CI (ex.: número do build). Nunca editado à mão. |
| `versionName` | SemVer (`MAJOR.MINOR.PATCH`), definido na release. |

Todos os valores vêm do catálogo (`[versions]`) e são lidos só nos convention plugins.

## Dependências
### Version Catalog
```toml
[versions]
androidx-compose-bom = "…"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidx-compose-bom" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" } # versão vem do BOM

[plugins]
app-android-library = { id = "app.android.library" } # convention plugins também no catálogo
```
- Nomes em kebab-case com prefixo do grupo (`androidx-`, `kotlinx-`, `square-`).
- Bibliotecas e plugins de build-logic (AGP, KGP, KSP) também no catálogo, compartilhado
  com `build-logic` via `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`.
- Remova do catálogo o que deixar de ser usado no mesmo PR.

### Escopos
| Escopo | Use quando |
|---|---|
| `implementation` | Padrão. A dependência é detalhe interno do módulo. |
| `api` | Só quando o tipo aparece na API pública do módulo (raro; justificar no PR). |
| `testImplementation` / `testFixtures` | Fakes e utilitários de teste (`core:testing`, `testFixtures` do módulo). |
| `debugImplementation` | Ferramentas só de debug (ex.: `ui-tooling`, `ui-test-manifest`). |
| `ksp` | Processadores de anotação (Hilt, Room). `kapt` é proibido. |

`api` em excesso propaga recompilação para todos os consumidores e vaza camadas.

### Stack de referência
A biblioteca de cada papel (rede, serialização, banco, imagem, logging, crash etc.) está na
tabela do [ADR-0007](../../adr/0007-stack-de-bibliotecas-observabilidade-e-politicas-de-qualidade.md#stack-de-referência).
Use a que está lá; não proponha alternativa equivalente (ex.: Ktor, Koin, Glide, Gson,
Moshi) sem ADR. Itens da tabela entram no catálogo quando a primeira feature precisar.

### Antes de propor uma dependência nova
1. Algo no projeto, na stack de referência ou na plataforma (AndroidX, Kotlin stdlib) já resolve?
2. Licença compatível (Apache 2.0, MIT, BSD). Copyleft (GPL, LGPL) exige aprovação jurídica.
3. Manutenção ativa, releases recentes, sem CVE aberto (consulte o OSV).
4. Impacto no tamanho do APK e no método count; precisa de regras R8?
5. Versão consultada com `android studio version-lookup` ou na fonte oficial, nunca de memória.
6. Se for estrutural (rede, DI, persistência, navegação), registre um ADR.

### Cadeia de suprimentos
| Controle | Como |
|---|---|
| Wrapper | `distributionSha256Sum` no `gradle-wrapper.properties`; o CI valida o wrapper antes de executar `./gradlew` |
| Dependências | `gradle/verification-metadata.xml` com checksums SHA-256; atualizado com `./gradlew --write-verification-metadata sha256 help` no mesmo PR que muda o catálogo |
| CVEs | S-15: OSV-Scanner no CI e Dependabot para atualizações |
| Repositórios | Só os do `settings`, com filtro de conteúdo |

## Build types e release
| Build type | Configuração |
|---|---|
| `debug` | `applicationIdSuffix = ".debug"`, sem R8, StrictMode e ferramentas de debug ligados |
| `release` | `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`, logs removidos |

- **R8:** cada módulo declara suas regras em `consumer-rules.pro` (nunca concentradas no `app`).
  Regras `-keep` amplas (`-keep class **`, pacote inteiro) são proibidas; mantenha o mínimo.
  Use a skill `r8-analyzer` para investigar.
- **Mapping:** o `mapping.txt` de cada release é arquivado pelo CI junto ao `versionCode`;
  sem ele não há stack trace legível em produção.
- **Assinatura:** só no CI, com keystore e senhas vindas de variáveis de ambiente/secrets.
  Nunca no repositório, em `gradle.properties` ou em `BuildConfig`. Distribuição pela Play
  com Play App Signing (o CI usa só a upload key). Sem as variáveis, `assembleRelease` gera
  um artefato não assinado, suficiente para o sensor S-13.
- **Flavors:** só com necessidade real da spec (ex.: ambientes). Cada dimensão multiplica
  variantes e tempo de build.

## Tasks agregadas (contrato estável com o harness)
`scripts/harness/verify.sh` chama apenas estas tasks; o mapeamento interno fica no `app.root`/`app.quality`.

| Task | Inclui |
|---|---|
| `harnessFast` | `spotlessCheck`, `detekt`, `:quality:architecture-tests:test` |
| `harnessCheck` | `harnessFast`, `testDebugUnitTest`, `lintDebug` |
| `harnessFull` | `harnessCheck`, `verifyRoborazziDebug`, `koverVerify`, `assertModuleGraph`, `assembleRelease` |

Renomear ou remover uma dessas tasks é mudança de contrato: atualize `verify.sh`,
o CI e este guia no mesmo PR.

## Criando um módulo (skill `android-feature-scaffold`)
1. Pastas `feature/<nome>/{api,domain,data,ui}` com o plugin correspondente.
2. Incluir em `settings.gradle.kts` (`include(":feature:<nome>:ui")` etc.).
3. `namespace = "<base.package>.feature.<nome>.<camada>"`.
4. Dependências entre módulos por *type-safe accessors* (`projects.feature.<nome>.domain`).
5. Rodar `verify.sh fast` (Konsist valida a estrutura).

Exemplo de `build.gradle.kts` completo de um módulo:
```kotlin
plugins {
    alias(libs.plugins.app.android.feature.ui)
}

android {
    namespace = "<base.package>.feature.balance.ui"
}

dependencies {
    implementation(projects.feature.balance.domain)
    implementation(projects.feature.statement.api)
}
```

## Regras do grafo de módulos (module graph assertion)
As expressões do plugin comparam origem e destino separadamente, por isso a regra
"feature só enxerga outra feature via `:api`" é **gerada por feature** no `app.quality`,
com *negative lookahead* para excluir a própria feature:

```kotlin
moduleGraphAssert {
    maxHeight = 6
    restricted = arrayOf(
        ":core:.* -X> :feature:.*",
        ".*:domain -X> .*:(data|ui)",
        ".*:ui -X> .*:data",                      // UI nunca enxerga data, nem para DI
        ".*:api -X> .*:(domain|data|ui)",          // contrato não depende de implementação
    ) + featureNames.map { name ->                 // lido de settings/diretório feature/
        ":feature:$name:.* -X> :feature:(?!$name:)[^:]+:(domain|data|ui)"
    }
}
```
Sem a geração por feature, uma regra genérica como `":feature:.* -X> :feature:.*:domain"`
bloquearia também `feature:X:ui → feature:X:domain`, que é obrigatória.

## Desempenho no dia a dia
Meta: `verify.sh fast` abaixo de 60 s com o daemon aquecido. Se passar disso,
registre no [steering loop](../steering-loop.md) e otimize antes de enfraquecer qualquer sensor.

### `gradle.properties` (raiz)
```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx3g
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.parallel=true
kotlin.code.style=official
```
Padrões do AGP 8+/Kotlin 2 que **não** devem ser desligados: `android.nonTransitiveRClass`,
`android.nonFinalResIds`, `kotlin.incremental`. Proibido: `org.gradle.configureondemand`
(incompatível com configuration cache) e `--no-build-cache` em scripts.

### Modularização a favor do build
- Módulos pequenos e coesos: mudar uma tela recompila só `feature:X:ui` e quem depende
  dele por `implementation`. Uma mudança em `:api` ou `core:*` recompila muitos módulos,
  por isso esses contratos mudam pouco e com aprovação.
- Módulos JVM puros (`app.jvm.library`) sempre que não houver Android: compilam mais
  rápido e não passam por AGP/R.
- Quebre um módulo quando ele virar gargalo de build (medido com `--profile`) ou quando
  duas features precisarem dele, não por antecipação.
- **Baseline Profiles** (módulo `:baselineprofile` + `profileinstaller`) gerados para a
  jornada de startup e fluxos críticos; regenerados a cada release.

### Regras para o `app.quality`
| Sensor | Regra de desempenho |
|---|---|
| detekt | Usar a task `detekt` (sem *type resolution*) no `fast`; `detektMain` com *type resolution* só no `full`, se adotado. Tasks cacheáveis por módulo. |
| Spotless | Localmente, `ratchetFrom("origin/main")` verifica só o que mudou em relação à `main`. No CI, o checkout precisa do histórico da `main` (`fetch-depth: 0`); na própria `main`, verificação completa, sem ratchet. |
| Konsist | O Konsist lê o código do disco, então o Gradle não sabe quando a entrada mudou. Declare as fontes como `inputs.files(...)` da task `test` de `:quality:architecture-tests`; assim ela fica *up-to-date*/cacheada quando nada mudou e reexecuta quando algo mudou. |
| Kover | Instrumentação de cobertura somente quando `-Pharness.coverage=true` (passado pelo `harnessFull`/CI). A instrumentação deixa os testes unitários mais lentos. |
| Roborazzi | Testes de screenshot em source set/tag próprios, executados só no `full`. |
| Android Lint | Só no `standard` (é o sensor local mais lento); `checkDependencies = false` e `lintDebug` por módulo para aproveitar o cache. |
| Testes | `maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)`; testes Robolectric isolados dos JVM puros. |

### Diagnóstico de build lento ou quebrado
| Pergunta | Comando |
|---|---|
| Quem traz esta dependência/versão? | `./gradlew :app:dependencyInsight --dependency <nome> --configuration releaseRuntimeClasspath` |
| Árvore de dependências de um módulo | `./gradlew :feature:<nome>:ui:dependencies --configuration debugRuntimeClasspath` |
| Onde está o tempo? | `./gradlew <task> --profile` (relatório local em `build/reports/profile/`) |
| Por que o configuration cache foi invalidado/recusado? | Relatório em `build/reports/configuration-cache/` |
| Recomposição/estabilidade do Compose | `-Pcompose.reports=true` (relatórios do compilador, se configurado no `app.android.compose`) |

`--scan` publica dados do build num serviço externo: só com pedido explícito do usuário.

### Ajuste do hook `Stop` (por desenvolvedor)
Em `.claude/settings.local.json` (não versionado):
```json
{ "env": { "HARNESS_STOP_LEVEL": "light" } }
```
`fast` (padrão) roda Gradle ao fim de cada turno com mudança em Kotlin; `light` roda só
os scripts (~1 s); `off` desliga. Com `light` ou `off`, rodar `verify.sh standard`
antes de concluir a tarefa continua obrigatório.

## Anti-padrões de build
| Não faça | Faça |
|---|---|
| Versão inline, `+`, `-SNAPSHOT` | Entrada no catálogo com versão fixa |
| Versão alpha/beta/RC "porque tem a API nova" | Última estável; pré-release só com ADR |
| Bloco `android {}` copiado entre módulos | Convention plugin |
| `allprojects {}`, `subprojects {}`, `afterEvaluate {}` | Plugin aplicado por módulo; APIs lazy |
| `tasks.create`, `tasks.getByName` | `tasks.register`, `tasks.named` |
| `System.getenv`/`File.readText` em tempo de configuração | `providers.environmentVariable(...)`, `providers.fileContents(...)` |
| `kapt` | `ksp` |
| `api(...)` por conveniência | `implementation(...)` |
| Segredo em `gradle.properties`, `BuildConfig` ou `resValue` | Secret do CI + armazenamento seguro em runtime ([security.md](security.md)) |
| `-keep` amplo para "fazer o release funcionar" | Regra mínima no `consumer-rules.pro` do módulo |
| Desligar cache, configuration cache ou sensor para passar | Corrigir a causa (Art. VIII.3) |

## Checklist de revisão
- [ ] `build.gradle.kts` de módulo só com `plugins`, `namespace` e `dependencies`.
- [ ] Nenhuma versão fora do catálogo; nenhuma entrada órfã no catálogo.
- [ ] Só versões estáveis; biblioteca da stack de referência quando o papel já tem dono.
- [ ] Arquivos `.kts` novos com o cabeçalho de licença.
- [ ] Dependência nova aprovada (e ADR se estrutural); licença e CVEs verificados.
- [ ] `verification-metadata.xml` atualizado quando o catálogo mudou.
- [ ] `implementation` por padrão; cada `api(...)` justificado.
- [ ] Nenhum segredo, keystore ou credencial no build ou no repositório.
- [ ] Regras R8 novas no `consumer-rules.pro` do módulo, mínimas.
- [ ] Configuration cache sem problemas novos.
- [ ] `verify.sh fast` passando e, em mudança de build ou M/G, `verify.sh full` (grafo de módulos e release).
