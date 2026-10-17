# Guia: Kotlin e Clean Code

> Vale para todo código Kotlin de produção (`app`, `core/*`, `feature/*`). Regras de
> UI ficam em [compose-ui.md](compose-ui.md), de erros em [error-handling.md](error-handling.md)
> e de dados sensíveis em [security.md](security.md); aqui só há o que é da linguagem.
>
> **Verificado por:** S-01 `lint-file.sh` (hook, a cada edição), S-03 Spotless/ktlint
> (formatação) e S-04 detekt ([detekt.yml](../../../config/detekt/detekt.yml)). Estado
> de cada sensor: [anti-patterns.md](anti-patterns.md#estado-dos-sensores-citados).
> Itens marcados com `AP-xx` estão no catálogo de anti-padrões.

## Formatação
- Estilo `ktlint_official` via Spotless. **Não formate à mão:** rode `./gradlew spotlessApply`.
- [`.editorconfig`](../../../.editorconfig) é a fonte das regras: 4 espaços, linha de
  até **120** colunas, LF e newline final.
- Vírgula final (*trailing comma*) em declarações e chamadas com várias linhas:
  diffs menores e reordenação segura.
- Sem import com curinga (`import foo.*`) (detekt `WildcardImport`). A ordem dos imports
  fica por conta do ktlint.
- Um tipo público principal por arquivo, com o arquivo nomeado como o tipo. Extensões
  e funções de topo relacionadas vão em `<Assunto>.kt` (ex.: `AmountFormatting.kt`).

## Cabeçalho de licença
Todo arquivo criado no projeto (`.kt`, `.kts`, e também `.xml` de recurso quando o formato
permitir) começa com o cabeçalho Apache 2.0 de
[license-header.txt](../templates/license-header.txt), com `$YEAR` = ano de criação do
arquivo. Ao editar um arquivo existente, não mude o ano. Arquivos gerados (`build/`) não
recebem cabeçalho. Sensor previsto: Spotless `licenseHeaderFile` (backlog em
[anti-patterns.md](anti-patterns.md#lacunas-de-sensores-backlog-do-steering-loop)).

## Nomenclatura
| Elemento | Padrão | Exemplo |
|---|---|---|
| Pacotes | minúsculo, sem `_`, espelhando o módulo | `com.example.app.feature.payment.domain` |
| Classes / interfaces | PascalCase, substantivo; sem prefixo `I` nem sufixo `Impl` quando houver nome melhor | `PaymentRepository`, `RemotePaymentRepository` |
| Siglas | tratadas como palavra | `HttpClient`, `PanMasker` (não `HTTPClient`) |
| Use cases | Verbo + objeto + `UseCase`, invocados com `operator fun invoke` | `ConfirmPaymentUseCase` |
| Funções | camelCase, verbo | `loadStatement()` |
| Mappers | extensão `toX()` na camada de origem | `PaymentDto.toDomain()` |
| Composables | PascalCase, substantivo | `BalanceCard()` |
| `UiEvent` | ação do usuário no passado ou fato ocorrido | `ConfirmClicked`, `AmountChanged` |
| Constantes | `UPPER_SNAKE_CASE` em `const val` de topo ou `companion object` | `MAX_RETRIES` |
| Booleans | prefixo `is`/`has`/`should`/`can` | `isLoading`, `hasInstallments` |
| Propriedade de apoio | `_` + nome público, sempre `private` | `_uiState` / `uiState` |
| Flows | substantivo, sem sufixo `Flow` em propriedade pública | `val uiState: StateFlow<UiState>` |
| Testes | classe `<Sut>Test`; método em backticks citando o AC | `` `AC-02 - dado saldo zero quando abrir entao exibe vazio` `` |
| Dublês de teste | `Fake<Interface>` | `FakeBalanceRepository` |

## Clean code
Limites do detekt. São tetos, não metas.

| Métrica | Limite | Observação |
|---|---|---|
| Complexidade ciclomática | ≤ 10 | por função |
| Tamanho de função | ≤ 40 linhas | testes excluídos |
| Tamanho de classe | ≤ 300 linhas | |
| Parâmetros | função ≤ 6, construtor ≤ 8 | parâmetros com valor padrão e `@Composable` não contam |
| `return` por função | ≤ 3 | |
| Funções por arquivo/classe | ≤ 15 | privadas e `@Preview` não contam |

- Funções pequenas, um nível de abstração por função, um motivo para mudar por classe.
- Retorno antecipado e `require`/`check` no início em vez de `if` aninhado.
- Sem números ou strings mágicos: constantes nomeadas, recursos (`strings.xml`) ou
  tokens do design system.
- Nomes revelam intenção. Comentários explicam **por quê**, nunca **o quê**.
- Sem código morto, código comentado ou `TODO` sem ticket: use `TODO(PROJ-123)` (AP-44).
- Sem abstração especulativa (Constitution, Art. VII): só generalize depois do terceiro uso.
- Dependências entram pelo construtor (`@Inject constructor`). Nada de service locator,
  `object` com estado mutável ou singleton manual.
- `lateinit` só em classes de framework (`Activity`, `Fragment`, `Application`) (AP-42).
- **Sem duplicação:** a segunda cópia de uma lógica é um alerta; a terceira vira função,
  extensão ou componente compartilhado no módulo certo (`core:common`, `core:ui`,
  `core:designsystem`). Duplicar entre features é preferível a criar dependência entre elas.

## Não reinvente a roda
Antes de escrever infraestrutura própria (cache, retry, parser, formatador, criptografia,
máscara, paginação, agendamento, carregamento de imagem):
1. Procure no projeto e na [stack de referência](../../adr/0007-stack-de-bibliotecas-observabilidade-e-politicas-de-qualidade.md#stack-de-referência).
2. Procure na plataforma (AndroidX, Kotlin stdlib, `java.time`, `android.icu`).
3. Se houver solução madura de mercado, registre no `plan.md` (seção de alternativas) um
   quadro curto: maturidade/manutenção, licença, tamanho no APK, regras R8, segurança/CVEs,
   custo de adoção × custo de manter código próprio.
4. Adote quando os prós vencerem (pedindo aprovação, pois é dependência nova). Implemente
   por conta própria só quando a solução existente não couber, justificando no plano.
5. **Criptografia, TLS e autenticação nunca são implementadas à mão:** use as APIs da
   plataforma (Keystore, `security-crypto`/Tink, Credential Manager) conforme [security.md](security.md).

## Recursos do sistema e vazamentos de memória
| Regra | Como |
|---|---|
| Nada de I/O ou CPU pesado na main thread | `withContext(ioDispatcher/defaultDispatcher)` no data/domain (AP-25). StrictMode `detectAll().penaltyLog()` ligado no `debug` (com `penaltyDeath` no CI de instrumentação) |
| `Context` com o tempo de vida certo | Objeto de longa duração (singleton, repositório, `ViewModel`) recebe `@ApplicationContext`; nunca guarda `Activity`, `View`, `Fragment` ou `Composable` (AP-21). Contexto de `Activity` só para UI (tema, diálogos, permissões) |
| Listeners e callbacks liberados | Registro/remoção pareados no ciclo de vida: `DisposableEffect { onDispose { } }`, `LifecycleEventObserver`, `callbackFlow { awaitClose { } }` |
| Recursos fechados | `use { }` em `Closeable` (streams, `Cursor`); nada de `Bitmap` grande em memória (Coil faz cache e downsampling) |
| Trabalho em background no contexto certo | Tela: `viewModelScope`. Sobrevive à tela, mas não ao processo: `CoroutineScope` de aplicação injetado. Garantido/diferível: WorkManager com constraints (rede, bateria). Nunca `Service` para trabalho curto |
| Bateria e rede | Sem polling: prefira push/Flow; agrupe requisições; respeite constraints do WorkManager; cache HTTP do OkHttp quando a spec permitir |
| Detecção | LeakCanary em `debugImplementation`: um vazamento reportado é bug e entra no PR atual ou num ticket |

## Nomenclatura

## Modelagem de tipos
Faça o compilador garantir as regras: estado inválido não deve ser representável.

- **`sealed interface`** para estados, resultados e erros finitos. Use `when` exaustivo
  **sem `else`**, para que uma variante nova quebre a compilação em vez de cair no padrão.
  A única exceção é `AppError`, que é uma interface aberta (ver [error-handling.md](error-handling.md#modelo-de-erro-corecommon)).
- **`data object`** para variantes sem dados (`data object Loading : UiState`): o
  `toString()` fica legível e `equals` funciona como esperado.
- **`value class`** para identificadores e valores com regra (`PaymentId`, `Amount`,
  `MaskedPan`). Evita trocar parâmetros do mesmo tipo e centraliza a validação.
- **Invariantes no construtor** com `init { require(...) }`. Um objeto existente é um
  objeto válido.
- **`enum class`** para conjuntos fixos sem dados por variante. Itere com `entries`,
  nunca com `values()`.
- **Imutabilidade por padrão:** `val`, `data class` + `copy`, `List`/`Map` (não
  `Mutable*`) em APIs. No `UiState`, use `ImmutableList` (ver [compose-ui.md](compose-ui.md#7-performance-de-recomposição)).
- **Modelos com dado sensível** sobrescrevem `toString()` com máscara, porque
  `data class` imprime tudo em log, crash report e mensagem de erro (AP-07).

```kotlin
@JvmInline
value class Amount(val cents: Long) {
    init { require(cents >= 0) { "Amount must not be negative" } } // regra da spec
    operator fun plus(other: Amount) = Amount(Math.addExact(cents, other.cents))
}

@JvmInline
value class CardToken(private val value: String) {
    override fun toString() = "CardToken(***)"
}
```

## Idiomático
- **Null-safety:** prefira tipos não nulos por construção; depois `?.`, `?:` e
  `requireNotNull(x) { "motivo" }`. `!!` é proibido sem comentário que justifique a
  invariante (AP-42, detekt `UnsafeCallOnNullableType`).
- **Pré-condições:** `require` para argumento inválido, `check` para estado inválido e
  `error("...")` para ramo impossível. As mensagens não podem conter PII nem dado de cartão.
- **Expressões:** use `if`/`when`/`try` como expressão e corpo de expressão (`= ...`)
  em funções de uma linha.
- **Tipos explícitos** em toda declaração `public`/`internal` exposta entre camadas.
  Inferência só em código local.
- **Extensões** para mapeamento e conveniência, nunca para esconder regra de negócio,
  que fica no use case (AP-19).
- **Scope functions** com propósito claro e sem aninhar:

  | Função | Use para |
  |---|---|
  | `let` | transformar valor nulo (`x?.let { ... }`) |
  | `apply` | configurar objeto (builders) |
  | `also` | efeito lateral sem alterar o valor |
  | `run` / `with` | agrupar chamadas sobre o mesmo receptor |

- **Coleções:** operadores funcionais (`map`, `filter`, `associateBy`) em vez de laços
  com acumulador; `buildList { }` para montagem condicional; `asSequence()` só para
  coleções grandes ou cadeias longas, depois de medir.
- **Dinheiro:** centavos em `Long` ou `BigDecimal` criado a partir de `String`/`Long`,
  com escala e `RoundingMode` explícitos definidos na spec. **Nunca** `Double`/`Float`
  nem `BigDecimal(double)` (AP-08). Formatação com `Locale` explícito (AP-40).
- **Tempo:** `java.time` (`Instant`, `LocalDate`) com `Clock` injetado. Proibido usar
  `System.currentTimeMillis()`, `java.util.Date` e `Calendar` (AP-41).

## Coroutines e Flow
**Concorrência estruturada**
- Toda coroutine pertence a um escopo com dono: `viewModelScope` na presentation,
  funções `suspend` no domain/data e `CoroutineScope` injetado para trabalho que precisa
  sobreviver à tela. Proibido: `GlobalScope`, `runBlocking` fora de testes e `Thread.sleep` (AP-02).
- Paralelismo com `coroutineScope { async { } ... }`. Use `supervisorScope` só quando a
  falha de um filho não deve cancelar os outros, e trate cada falha explicitamente.
- Laços longos ou CPU-bound chamam `ensureActive()` para responder ao cancelamento.
- Limite de tempo com `withTimeoutOrNull`, mapeando `null` para erro. `withTimeout`
  lança `TimeoutCancellationException`, que é cancelamento e encerra a coroutine em
  silêncio ([error-handling.md](error-handling.md#coroutines-e-flow)).

**Dispatchers e main-safety**
- Dispatchers sempre injetados (`@IoDispatcher`, `@DefaultDispatcher` de `core:common`).
  `Dispatchers.*` literal só em módulos `di/` (AP-01).
- Toda função `suspend` é **main-safe**: quem faz I/O troca de contexto internamente com
  `withContext(ioDispatcher)` no data, e o chamador nunca precisa saber disso (AP-25).
- Não marque como `suspend` uma função que não suspende (`RedundantSuspendModifier`).

**Cancelamento e erros**
- `CancellationException` é **sempre** relançada. Não use `runCatching` nem
  `catch (e: Exception)` em código `suspend` (AP-16).
- Na borda do data, use `suspendRunCatching` de `core:common` ou capture exceções
  específicas, e converta para `DataResult` antes de sair do repositório. `kotlin.Result`
  não atravessa camadas ([error-handling.md](error-handling.md#modelo-de-erro-corecommon)).

**Flow**
- Flows são frios por padrão: exponha `fun observeX(): Flow<T>`, e não uma
  `suspend fun` que retorna `Flow` (AP-24).
- `flowOn(ioDispatcher)` fica no **produtor** (data), nunca no coletor (AP-25).
- Flows do data expõem `Flow<DataResult<T>>` e não terminam por exceção. Use `catch { }`
  antes de `stateIn`/`collect`, emitindo uma falha. Uma exceção que chega ao `stateIn`
  mata o fluxo, e a tela para de atualizar sem aviso.
- Exponha `StateFlow`/`Flow` imutáveis. `MutableStateFlow` é `private` e é alterado com
  `update { }` (atômico), nunca `value = value.copy(...)`.
- Estado derivado: `combine(...).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` (AP-22).
- Use `distinctUntilChanged()` quando a fonte repete valores iguais (`StateFlow` já
  faz isso sozinho).
- Na UI, colete com `collectAsStateWithLifecycle()` (AP-26). Evento único é estado
  consumível no `UiState`, não `Channel`/`SharedFlow` (AP-23).

```kotlin
internal class RemoteStatementRepository @Inject constructor(
    private val api: StatementApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StatementRepository {

    override suspend fun load(period: Period): DataResult<Statement> = withContext(ioDispatcher) {
        suspendRunCatching { api.fetch(period.toQuery()) }
            .fold(
                onSuccess = { DataResult.Success(it.toDomain()) },
                onFailure = { DataResult.Failure(it.toAppError()) }, // matriz de erros do plano
            )
    }
}
```

## Visibilidade e contratos de módulo
- Visibilidade mínima: `private` → `internal` → `public`. Em módulos de feature, tudo é
  `internal` exceto o que é contrato.
- O contrato público entre features vive no módulo `:api` e muda só com aprovação
  ([architecture.md](architecture.md)). Tipos do `:api` são estáveis: adicione campos
  com valor padrão em vez de quebrar construtores.
- APIs públicas com KDoc curto: o que faz, pré-condições e erros possíveis.
  Implementação interna dispensa KDoc se o nome já explica.

## Checklist rápido
- [ ] `./gradlew spotlessApply` e `verify.sh fast` sem violações
- [ ] Nenhum `!!`, `lateinit`, `Dispatchers.*` fixo ou `catch` genérico sem justificativa
- [ ] Estados e erros em `sealed interface` com `when` exaustivo sem `else`
- [ ] Dinheiro em `Long`/`BigDecimal`, tempo com `Clock` injetado
- [ ] Modelos com dado sensível com `toString()` mascarado
- [ ] Visibilidade mínima e tipos explícitos na API exposta
- [ ] Cabeçalho de licença em todo arquivo novo
- [ ] Sem lógica duplicada; nenhuma infraestrutura reinventada sem quadro de prós e contras
- [ ] Nada de I/O na main; nenhum `Context` de `Activity` retido; listeners liberados
