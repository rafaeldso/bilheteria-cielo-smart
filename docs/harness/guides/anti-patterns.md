# Guia: Anti-padrões conhecidos

> Catálogo vivo de erros que agentes (e pessoas) já cometeram ou tendem a cometer
> neste projeto, alimentado pelo [steering loop](../steering-loop.md). Leia antes
> de qualquer mudança. Cada item aponta a forma correta, o guia de referência e o
> sensor que o detecta. **Item sem sensor ativo é candidato a virar um.**

## Como usar
- **Agente:** antes de escrever código, percorra a seção da área que vai mexer.
  Ao receber uma mensagem `SENSOR` com `AP-xx`, procure o ID aqui e corrija a causa.
- **Revisor:** cite o ID no comentário (`AP-16`), não reexplique a regra.
- **IDs são estáveis.** Nunca renumere nem reutilize um ID; item obsoleto é
  marcado como ~~riscado~~ com o motivo.

### Severidade
| Nível | Significado | Consequência |
|---|---|---|
| 🔴 **Bloqueante** | Risco de segurança, conformidade (PCI DSS/LGPD), perda de dinheiro ou crash | Nunca entra na `main`. Sem exceção sem ADR. |
| 🟠 **Alto** | Bug latente, vazamento de recurso, quebra de arquitetura | Corrigir na mesma mudança. |
| 🟡 **Médio** | Dívida de manutenção, desempenho, consistência | Corrigir na mesma mudança ou abrir ticket citado no PR. |

### Estado dos sensores citados
Detalhes em [sensors/README.md](../sensors/README.md).

| Sensor | Estado |
|---|---|
| S-01 `lint-file.sh`, S-02 `scan-sensitive-data.sh`, S-09 rastreabilidade, hooks (`guard-files`, `guard-bash`, `Stop`), permissões `ask` | ✅ Ativo |
| S-04 detekt ([detekt.yml](../../../config/detekt/detekt.yml)), S-08 Android Lint ([lint.xml](../../../config/lint/lint.xml)) | ⚙️ Configurado; aguarda o convention plugin `app.quality` em `build-logic/` |
| S-05 regras detekt próprias, S-06 Konsist, compose-rules (`io.nlopez`), S-10…S-13 | ⏳ Planejado (`quality/` e `build-logic/` ainda não existem) |
| S-16…S-19 revisores (`architecture-`, `security-`, `test-`, `compose-ui-reviewer`), `spec-consistency-analyzer` | 👁 Inferencial (não determinístico) |

---

## 1. Arquitetura e modularização
Referência: [architecture.md](architecture.md), [gradle.md](gradle.md#regras-do-grafo-de-módulos-module-graph-assertion)

| ID | Sev. | Anti-padrão | Correto | Sensor |
|---|---|---|---|---|
| AP-03 | 🟠 | Import `android.*`, `androidx.*`, Retrofit ou OkHttp na camada `domain` | Domain em Kotlin puro; dependa de interface declarada no domain e implementada no data | S-01, S-06 ⏳ |
| AP-06 | 🟠 | DTO ou entidade de banco chegando ao domain ou à UI | Mapper `toDomain()` no data; a UI consome modelo de domínio ou `UiModel` | S-06 ⏳, S-16 |
| AP-17 | 🟠 | Feature dependendo da implementação de outra feature | Depender apenas do módulo `:api` da outra feature | S-12 ⏳ |
| AP-19 | 🟠 | Regra de negócio no `ViewModel`, no `@Composable` ou no repositório | Regra no use case (domain); `ViewModel` só orquestra e mapeia para `UiState` | S-16 |
| AP-20 | 🟡 | Use case "passa-recado" criado só por cerimônia, ou repositório que vaza detalhes de paginação/cache para o domain | Use case só quando há regra ou combinação de fontes; contrato do repositório expressa a intenção do domínio | S-16 |
| AP-21 | 🟠 | `Context`, `Activity`, `View` ou `Lifecycle` guardados em `ViewModel`, singleton ou objeto de longa duração | Injetar `@ApplicationContext` só no data; nunca reter referência de `Activity` | S-08 `StaticFieldLeak` ⚙️, S-16 |

## 2. Coroutines e Flow
Referência: [kotlin-style.md](kotlin-style.md#coroutines-e-flow), [error-handling.md](error-handling.md)

| ID | Sev. | Anti-padrão | Correto | Sensor |
|---|---|---|---|---|
| AP-01 | 🟠 | `Dispatchers.IO`/`Default`/`Main` fixo em repositório, data source ou use case | Injetar `@IoDispatcher private val ioDispatcher: CoroutineDispatcher`; fixo só em `di/` | S-01, S-04 `InjectDispatcher` ⚙️ |
| AP-02 | 🔴 | `GlobalScope`, `runBlocking` ou `Thread.sleep` em produção | `viewModelScope`, `CoroutineScope` injetado ou função `suspend`; `delay` em vez de `sleep` | S-01, S-04 `GlobalCoroutineUsage`, `ForbiddenMethodCall`, `SleepInsteadOfDelay` ⚙️ |
| AP-16 | 🔴 | `catch (e: Exception)`/`Throwable` ou `runCatching` em código `suspend` engolindo `CancellationException` | Capturar exceções específicas; se precisar capturar genérica, relançar `CancellationException` antes (ver exemplo) | S-04 `TooGenericExceptionCaught` ⚙️, S-16 |
| AP-22 | 🟠 | `stateIn(scope, SharingStarted.Eagerly, …)` ou `Lazily` sem motivo; `.value` de `StateFlow` lido para montar estado | `SharingStarted.WhileSubscribed(5_000)` (sobrevive a rotação, para em background); combinar flows com `combine` | S-16 |
| AP-23 | 🟠 | Evento único (navegar, snackbar) modelado com `SharedFlow`/`Channel` que perde emissão sem coletor | Modelar como estado consumível no `UiState` (ex.: `navigation: Destination?` + evento de consumo), conforme spec | S-19 |
| AP-24 | 🟡 | `suspend fun` que retorna `Flow`, ou `suspend` sem nenhuma suspensão real | `fun` que retorna `Flow` (frio); remover `suspend` redundante | S-04 `SuspendFunWithFlowReturnType`, `RedundantSuspendModifier` ⚙️ |
| AP-25 | 🟠 | I/O (rede, Room, DataStore, arquivo) na main thread, ou `flowOn` aplicado no coletor em vez do produtor | `withContext(ioDispatcher)` ou `flowOn(ioDispatcher)` dentro do data; StrictMode ativo em `debug` | S-08 ⚙️, StrictMode (runtime) |

## 3. Presentation e Jetpack Compose
Referência: [compose-ui.md](compose-ui.md)

| ID | Sev. | Anti-padrão | Correto | Sensor |
|---|---|---|---|---|
| AP-04 | 🟠 | `ViewModel` expõe `MutableStateFlow`, vários flows de estado ou funções públicas avulsas | Um `StateFlow<UiState>` imutável e um único `onEvent(event: UiEvent)` | S-06 ⏳, S-19 |
| AP-05 | 🟠 | `Screen` recebe `ViewModel` (impossível de pré-visualizar e testar) | `Route` recebe o `ViewModel`; `Screen` recebe `UiState` e `onEvent` | S-06 ⏳, S-19 |
| AP-13 | 🟡 | Texto hardcoded, cores, `dp`/`sp` e tipografia mágicos | `stringResource`, tokens do design system (`AppTheme.colors`, `AppTheme.spacing`) | S-08 `HardcodedText` ⚙️ (só XML), S-06 ⏳, S-19 |
| AP-26 | 🟠 | `collectAsState()` ou `collect` em `LaunchedEffect` sem considerar ciclo de vida | `collectAsStateWithLifecycle()` na `Route` | S-19 |
| AP-27 | 🟠 | Lançar coroutine, ler `StateFlow.value` ou operar `Flow` durante a composição | `LaunchedEffect`/`rememberCoroutineScope` para efeitos; carga inicial disparada pelo `ViewModel`, não por `LaunchedEffect(Unit)` | S-08 `CoroutineCreationDuringComposition`, `StateFlowValueCalledInComposition`, `FlowOperatorInvokedInComposition` ⚙️ |
| AP-28 | 🟡 | `UiState` com `var`, `MutableList`, `List` de tipo instável ou lambdas recriadas a cada recomposição | `data class` imutável, `ImmutableList` (kotlinx.collections.immutable) ou `@Immutable`; lambdas estáveis via referência ou `remember` | compose-rules ⏳, S-19 |
| AP-29 | 🟡 | `@Composable` público sem parâmetro `modifier: Modifier = Modifier`, ou com o modifier aplicado em filho | `modifier` como primeiro parâmetro opcional, aplicado ao nó raiz | S-08 `ModifierParameter` ⚙️, compose-rules ⏳ |
| AP-30 | 🟠 | Estado de UI da spec sem renderização explícita ou sem `@Preview` (ex.: só `Success`) | `when (state)` exaustivo; um `@Preview` por estado (claro/escuro, fonte grande) | S-10 ⏳, S-19 |
| AP-31 | 🟠 | Acessibilidade ignorada: ícone sem `contentDescription`, alvo de toque < 48dp, `clickable` sem `role`, informação só por cor, cartão mascarado lido como "asterisco asterisco…" | `contentDescription` (ou `null` se decorativo), `minimumInteractiveComponentSize()`, `Modifier.semantics`, texto além da cor, descrição falada "final 1234" | S-08 ⚙️ (só View), S-19 |
| AP-32 | 🟠 | Entrada do usuário perdida em rotação ou morte de processo | Estado de formulário no `ViewModel` com `SavedStateHandle` (sem PAN/CVV); `rememberSaveable` para estado puramente visual | S-19, teste de `SavedStateHandle` |

## 4. Segurança, privacidade e dados de cartão
Referência: [security.md](security.md). Base: OWASP MASVS, PCI DSS, LGPD.

| ID | Sev. | Anti-padrão | Correto | Sensor |
|---|---|---|---|---|
| AP-07 | 🔴 | `Log.*`, `println`, `printStackTrace` ou logger interpolando PII/cartão (`"user=$user"`, `toString()` de data class com PAN) | Logger do projeto, só tipo de evento e ID de correlação; `toString()` mascarado em modelos sensíveis | S-01, S-02, S-04 `ForbiddenImport` ⚙️, S-17 |
| AP-33 | 🔴 | PAN completo, CVV ou trilha persistidos (DataStore, Room, `SavedStateHandle`, cache HTTP, arquivo) | Nunca persistir CVV/trilha; PAN só tokenizado ou mascarado (`**** 1234`); limpar da memória ao sair do fluxo | S-02, S-17 |
| AP-34 | 🔴 | Token ou segredo em `SharedPreferences`/DataStore em texto puro, ou chave criptográfica no código/`BuildConfig` | Cifrar com chave não exportável do Android Keystore; segredos de build via CI, nunca no repositório | S-01, S-02, S-14, S-17 |
| AP-35 | 🔴 | Tela com dados de cartão sem `FLAG_SECURE`, ou dado sensível visível no "Recentes" | `FLAG_SECURE` na janela das telas sensíveis; ocultar conteúdo ao ir para background | S-17 |
| AP-36 | 🔴 | `TrustManager`/`HostnameVerifier` permissivos, cleartext liberado, pinning desligado "para testar" | `network_security_config.xml` sem cleartext; pinning conforme plano; configuração de debug isolada em `src/debug` | S-08 `TrustAllX509TrustManager`, `BadHostnameVerifier`, `InsecureBaseConfiguration` ⚙️, S-17 |
| AP-37 | 🔴 | `Activity`/`Service`/`Receiver`/`Provider` exportado sem necessidade, `PendingIntent` mutável ou deep link sem validar parâmetros | `exported="false"` por padrão; `FLAG_IMMUTABLE`; validar e sanitizar todo dado vindo de `Intent`/URI | S-08 `Exported*`, `UnsafeIntentLaunch` ⚙️, S-17 |
| AP-38 | 🟠 | `allowBackup` habilitado ou WebView com JavaScript/`addJavascriptInterface` sem necessidade | `allowBackup="false"` (ou regras de backup excluindo dados sensíveis); WebView mínima e justificada no plano | S-08 `AllowBackup`, `SetJavaScriptEnabled`, `JavascriptInterface` ⚙️ |
| AP-39 | 🟠 | Build release sem R8 ou sem regras `keep` para modelos serializados; `debuggable=true` | R8 ativo, regras `keep` geradas pela lib de serialização, release validado | S-08 `HardcodedDebugMode` ⚙️, S-13 ⏳ |

## 5. Dinheiro, tempo e localização
Referência: [kotlin-style.md](kotlin-style.md#idiomático)

| ID | Sev. | Anti-padrão | Correto | Sensor |
|---|---|---|---|---|
| AP-08 | 🔴 | `Double`/`Float` para dinheiro; `BigDecimal(0.1)` a partir de `Double`; `divide` sem `RoundingMode` | Centavos em `Long` ou `BigDecimal` criado de `String`/`Long`, com escala e `RoundingMode` explícitos (definidos na spec) | S-01 (heurística), S-17 |
| AP-40 | 🟠 | Formatar moeda, número ou data com `String.format`/`toString()` ou `Locale.getDefault()` implícito | `NumberFormat.getCurrencyInstance(Locale("pt", "BR"))` ou formatador do design system; datas com `java.time` e formatador explícito | S-16, teste unitário |
| AP-41 | 🟡 | `System.currentTimeMillis()`, `java.util.Date` ou `Calendar` direto (testes não determinísticos, bugs de fuso) | `Clock` injetado e `java.time` (`Instant`, `ZonedDateTime`); fuso explícito quando for regra de negócio | S-04 `ForbiddenMethodCall`, `ForbiddenImport` ⚙️ |

## 6. Kotlin
Referência: [kotlin-style.md](kotlin-style.md)

| ID | Sev. | Anti-padrão | Correto | Sensor |
|---|---|---|---|---|
| AP-42 | 🟠 | `!!` sem justificativa ou `lateinit` fora de classes de framework | Tipos não nulos por construção, `requireNotNull(x) { "motivo" }`, `?.`/`?:` | S-04 `UnsafeCallOnNullableType`, `LateinitUsage` ⚙️ |
| AP-43 | 🟡 | Exceção usada como controle de fluxo entre camadas; `Result` genérico sem tipo de erro | Erros de domínio em `sealed interface` mapeados no data conforme a matriz do plano | S-16 |
| AP-44 | 🟡 | `TODO`/`FIXME` solto no código | Resolver agora ou `TODO(PROJ-123)` com ticket | S-04 `ForbiddenComment` ⚙️ |

## 7. Testes
Referência: [testing.md](testing.md)

| ID | Sev. | Anti-padrão | Correto | Sensor |
|---|---|---|---|---|
| AP-09 | 🟠 | Teste "verde" que não verifica nada, ou sem `AC-xx` no nome | Assert do `UiState`/resultado esperado; nome cita o AC | S-09, S-18 |
| AP-45 | 🟠 | Tempo real em teste (`Thread.sleep`, `delay` real), `Dispatchers.Main` não substituído, ou teste dependente de ordem | `runTest` com `StandardTestDispatcher` injetado, `advanceUntilIdle()`, extensão `MainDispatcher` do `core:testing` | S-04 ⚙️, S-18 |
| AP-46 | 🟡 | Mock de tudo (inclusive data classes e o próprio SUT), verificando chamadas em vez de comportamento | Fakes para repositórios; MockK só em fronteiras; assert de estado observável | S-18 |
| AP-47 | 🟡 | Só caminho feliz testado; estados de erro da matriz sem teste | Um teste por linha relevante da matriz de erros e por estado de UI da spec | S-09, S-18 |
| AP-48 | 🟡 | JUnit 4 (`org.junit.Test`) em teste novo | JUnit 5 (`org.junit.jupiter`) | S-04 `ForbiddenImport` ⚙️ |

## 8. Processo (SDD) e harness
Referência: [AGENTS.md](../../../AGENTS.md), [constitution.md](../../constitution.md)

| ID | Sev. | Anti-padrão | Correto | Sensor |
|---|---|---|---|---|
| AP-10 | 🔴 | `@Suppress`, `@SuppressLint`, `@Ignore`, `@Disabled`, baseline ou limite reduzido para passar | Corrigir a causa ou pedir aprovação humana (`harness:allow <motivo>` na linha) | S-01, permissões `ask` |
| AP-11 | 🟠 | Dependência nova adicionada sem pedir | Propor (motivo, licença, tamanho, manutenção) e esperar aprovação | Permissão `ask` em `libs.versions.toml` |
| AP-12 | 🟠 | Inventar API Android/Compose inexistente ou usar API deprecada | `android docs <consulta>` antes de usar API desconhecida | Build, S-08 ⚙️ |
| AP-14 | 🟠 | Divergir da spec "para melhorar" | Parar, propor nova versão da spec com entrada no changelog | `spec-consistency-analyzer` |
| AP-15 | 🔴 | Declarar "pronto" sem rodar sensores | Rodar `verify.sh standard` e citar o resultado | Hook `Stop`, DoD |
| AP-18 | 🟠 | Editar arquivo gerado (`build/`, `generated/`) | Mudar a fonte que gera o arquivo | Hook `guard-files.sh` |
| AP-49 | 🟡 | Mudança grande em um único passo, sem checkpoint, misturando refatoração e comportamento | Incrementos pequenos por tarefa do `tasks.md`; refatoração em commit separado | Revisão humana |

---

## Exemplos dos erros mais frequentes

**AP-16: cancelamento engolido**
```kotlin
// ❌ runCatching captura CancellationException e a coroutine continua "viva"
suspend fun load() = runCatching { api.fetch() }

// ✅ captura específica; se precisar de catch genérico, relance o cancelamento
suspend fun load(): Result<Data> = try {
    Result.success(api.fetch())
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    Result.failure(e)
}
```

**AP-22 e AP-26: estado com ciclo de vida**
```kotlin
// ViewModel
val uiState: StateFlow<UiState> = repository.observe()
    .map(::toUiState)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

// Route
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

**AP-08: dinheiro**
```kotlin
// ❌ val total: Double = 0.1 + 0.2   // 0.30000000000000004
// ❌ BigDecimal(0.1)                 // 0.1000000000000000055511151231257827…
// ✅
val total = BigDecimal("0.10") + BigDecimal("0.20")
val parcela = total.divide(BigDecimal(3), 2, RoundingMode.HALF_EVEN) // regra da spec
```

---

## Lacunas de sensores (backlog do steering loop)
Itens que hoje dependem só de revisão. Prioridade sugerida para virar sensor
computacional:

1. `build-logic/` com `app.quality` aplicando detekt e Android Lint (ativa todos os ⚙️).
2. Konsist (S-06): AP-04, AP-05, AP-06, AP-28 (`UiState` imutável).
3. compose-rules: AP-26, AP-28, AP-29.
4. Regra detekt própria (S-05): AP-16 (`runCatching` em `suspend`), AP-40 (formatação sem `Locale`).
5. Teste de manifest/release: AP-35 (`FLAG_SECURE` nas telas marcadas como sensíveis na spec), AP-37, AP-39.
6. Spotless `licenseHeaderFile(docs/harness/templates/license-header.txt)` no `app.quality`
   (cabeçalho de licença, ADR-0007) e checagem de versões pré-release no catálogo.
7. Script do harness que falha se `.claude/agents/*.md` tiver `model: inherit`/`opus`.
8. Design (ADR-0008): gerar `core:designsystem` e as variáveis `--md-*` do mockup-kit a partir de
   `docs/design/tokens/` (Style Dictionary v5, exige aprovação de dependência) ou, antes disso, um
   script que compare os valores dos três lugares; `check-traceability.sh` cobrindo estado da
   `ui-spec.md` ↔ `@Preview`.

## Como adicionar ou alterar um item
1. Siga a skill `harness-improve` e registre a ocorrência no [steering loop](../steering-loop.md).
2. Use o próximo ID livre (o último é **AP-49**) e coloque o item na seção da área.
3. Preencha severidade, forma correta, guia e sensor. Sem sensor, adicione à lista de lacunas.
4. Se criou sensor, atualize [sensors/README.md](../sensors/README.md) e a mensagem
   do sensor para citar o `AP-xx`.
