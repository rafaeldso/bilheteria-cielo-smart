---
status: aceito
date: 2026-10-17
decisores: Tech Lead
consultados: Agente de IA (Claude Code)
informados: Time Android
---

# ADR-0007: Stack de bibliotecas, observabilidade gratuita e políticas de qualidade

## Contexto e Problema
O projeto tinha regras fortes de arquitetura e build, mas deixava em aberto alguns pontos:
qual biblioteca usar em cada papel (rede, imagem, persistência, logging), se versões
alpha/beta/RC eram aceitas, qual ferramenta de observabilidade adotar, como marcar a
autoria e a licença dos arquivos e como controlar o custo dos modelos de IA usados pelos
agentes. Sem essas decisões, cada feature (ou cada agente) escolhe a seu modo.

## Fatores de Decisão
- Bibliotecas padrão de mercado, com manutenção ativa e documentação oficial do Android.
- Somente versões estáveis; menor superfície de risco num app de meios de pagamento.
- Observabilidade **gratuita**, desde o primeiro release, sem coletar PII nem dado de cartão.
- Build rápido e R8 sem regras amplas.
- Custo de IA proporcional e previsível.

## Opções Consideradas
- **Observabilidade:** Firebase Crashlytics + Performance Monitoring (sem custo) · Sentry (plano gratuito com cota) · ACRA (self-hosted).
- **Stack:** padrão de mercado alinhado ao Now in Android · bibliotecas alternativas (Ktor, Koin, Glide).

## Decisão
1. **Stack de referência** (tabela abaixo). Versões ficam só no catálogo e devem ser
   **estáveis**: alpha, beta, RC, milestone, `-dev` e `-SNAPSHOT` são proibidos, exceto
   com ADR específico (ex.: correção de segurança só disponível em RC).
2. **Observabilidade:** Firebase Crashlytics (crash, não fatais, ANR) e Firebase
   Performance Monitoring (startup, traces de rede), ambos sem custo. Acesso só pelo
   wrapper de `core:common` (`Logger`/`CrashReporter`). Firebase Analytics **não** é
   adotado por este ADR. Coleta desligada até o consentimento exigido pela spec, sem
   coleta de URL com query string e com allowlist de chaves.
3. **Licença:** todo arquivo de código e de build criado no projeto leva o cabeçalho
   Apache 2.0 de [license-header.txt](../harness/templates/license-header.txt).
4. **Reaproveitar antes de criar:** solução madura de mercado é avaliada (prós e contras
   registrados no plano) antes de qualquer implementação própria de infraestrutura.
5. **IA:** subagentes usam `sonnet` por padrão. Qualquer uso de modelo de topo (Opus) ou
   de esforço `max` em subagentes exige aprovação humana prévia ([workflows](../harness/workflows/README.md#roteamento-de-modelo-e-esforço)).

### Stack de referência
| Papel | Biblioteca | Observação |
|---|---|---|
| DI | Hilt (KSP) | ADR-0003 |
| Assíncrono | Kotlin Coroutines + Flow | Constitution Art. III |
| UI | Jetpack Compose (BOM) + Material 3 | Via `core:designsystem` |
| Navegação | Navigation Compose com rotas type-safe | Migração para Navigation 3 exige ADR |
| Ciclo de vida | `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` | `collectAsStateWithLifecycle` |
| Rede | Retrofit + OkHttp (BOM) | Só em `core:network` e `data` |
| Serialização | kotlinx.serialization | Sem reflexão: menos regras R8. Sem Gson/Moshi |
| Banco local | Room (KSP) | `core:database` |
| Preferências | DataStore | Nunca `SharedPreferences` novo |
| Trabalho em background | WorkManager | Trabalho garantido e diferível |
| Paginação | Paging 3 | Só quando a spec pedir listas paginadas |
| Imagem | Coil (Compose) | |
| Coleções imutáveis | kotlinx-collections-immutable | `UiState` estável |
| Data e hora | `java.time` (desugaring se `minSdk` < 26) | `Clock` injetado |
| Logging | Timber, atrás do `Logger` de `core:common` | Sem `Tree` de debug em release |
| Crash e performance | Firebase Crashlytics + Performance | Gratuitos; atrás do wrapper |
| Vazamento de memória | LeakCanary (`debugImplementation`) | Nunca em release |
| Startup | Baseline Profiles + `profileinstaller`, Macrobenchmark | Módulo `:baselineprofile` |
| Splash | `core-splashscreen` | |
| Testes | JUnit 5, MockK, Turbine, coroutines-test, Robolectric, Roborazzi | ADR-0003 |

Cada item entra no catálogo só quando a primeira feature precisar dele (Art. VII). A
entrada em si já está pré-aprovada por este ADR; uma biblioteca **fora** da tabela continua
exigindo aprovação.

### Consequências
- **Positivas:** decisões previsíveis; agentes param de propor alternativas; observabilidade
  sem custo desde o início; menor risco com versões estáveis; custo de IA controlado.
- **Negativas:** dependência do Firebase (Google) como processador de dados de diagnóstico,
  que precisa constar na política de privacidade; recursos novos só chegam quando estáveis.

### Confirmação
- Revisão do catálogo no PR (checklist de [gradle.md](../harness/guides/gradle.md)) e
  Dependabot ignorando pré-releases.
- Cabeçalho de licença: Spotless `licenseHeaderFile` no `app.quality` (backlog em
  [anti-patterns.md](../harness/guides/anti-patterns.md#lacunas-de-sensores-backlog-do-steering-loop)).
- Frontmatter de `.claude/agents/*.md` sem `inherit` nem `opus` sem registro de aprovação.

## Prós e Contras das Opções

### Firebase Crashlytics + Performance
- **A favor:** sem custo e sem cota relevante; padrão de mercado; ANR e não fatais; mapping R8 enviado pelo plugin Gradle.
- **Contra:** dados de diagnóstico vão para o Google; exige `google-services.json` (que é identificador, não segredo).

### Sentry (plano gratuito)
- **A favor:** controle fino de scrubbing; pode ser self-hosted.
- **Contra:** cota mensal de eventos no plano gratuito; self-hosted tem custo de operação.

### ACRA
- **A favor:** open source, dados ficam conosco.
- **Contra:** exige backend próprio; sem performance monitoring.

## Mais Informações
[ADR-0003](0003-stack-de-sensores-de-qualidade.md) · [gradle.md](../harness/guides/gradle.md) ·
[security.md](../harness/guides/security.md) · [error-handling.md](../harness/guides/error-handling.md#observabilidade)
