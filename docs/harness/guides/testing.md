# Guia: Testes

> Constitution Art. IV (testes) e Art. VIII.3 (não enfraquecer sensores).
> Baseado em [Testing in Android](https://developer.android.com/training/testing),
> [Testing coroutines](https://developer.android.com/kotlin/coroutines/test),
> [Testing Compose](https://developer.android.com/develop/ui/compose/testing)
> e na skill oficial `testing-setup` do Android CLI.
> **Verificado por:** S-07 testes unitários, S-09 `check-traceability.sh`, S-10 Roborazzi,
> S-11 Kover, S-06 Konsist, S-18 `test-reviewer`.
> **Anti-padrões:** AP-09, 32, 41, 45–48. **Skill:** `write-tests`.
> Na dúvida sobre uma API de teste, rode `android docs <consulta>` antes de escrever.

## 1. Pirâmide e onde cada teste vive
| Nível | O que cobre | Source set | Ferramentas | Proporção | Roda em |
|---|---|---|---|---|---|
| Unitário | use cases, regras (`BR-xx`), ViewModel, mappers, formatadores | `src/test` (JVM puro) | JUnit 5, MockK, Turbine, `kotlinx-coroutines-test` | ~70% | `standard` |
| Integração | repositório + data source, rede (MockWebServer), Room, DataStore | `src/test` (Robolectric se precisar de Android) | JUnit 5; Robolectric | ~20% | `standard` |
| UI de componente | `Screen` stateless: estados, interações, semântica | `src/test` com Robolectric | Compose UI Test | ~10% (junto com screenshot) | `standard` |
| Screenshot | um `@Preview` por estado e variante | source set de screenshot do `app.android.compose` | Roborazzi + Robolectric | ↑ | `full` |
| Instrumentado / E2E | só fluxos críticos da spec (ex.: pagamento ponta a ponta) | `src/androidTest` | Compose UI Test, Hilt testing | poucos | CI (quando configurado) |
| Arquitetura | camadas, nomes, estrutura | `:quality:architecture-tests` | Konsist | sempre | `fast` |

**Regra de escolha:** use o nível mais baixo que prove o comportamento. Só desça para
Robolectric/dispositivo quando o comportamento depender do framework Android.

| Vou testar… | Tipo de teste |
|---|---|
| Regra de negócio, validação, cálculo de valor | Unitário do use case/entidade, com `@ParameterizedTest` nos limites da `BR-xx` |
| Transição de `UiState` após um `UiEvent` | Unitário do ViewModel com Turbine |
| Código HTTP ou erro de rede → `AppError` | Integração com MockWebServer |
| Mapeamento DTO → domínio | Unitário do mapper (inclui campo nulo, desconhecido ou ausente) |
| Query, DAO ou migração de banco | Integração Room (in-memory) e `MigrationTestHelper` |
| Clique, digitação, mensagem de erro na tela | UI de componente na `Screen` |
| Aparência de cada estado (tema, fonte 2x) | Screenshot |
| Navegação entre telas, Hilt, permissões, deep link | Instrumentado, só se o fluxo for crítico na spec |

## 2. Regras gerais
1. **Todo `AC-xx` tem ao menos um teste com o ID no início do nome** (S-09):
   `` `AC-03 - dado CPF invalido quando enviar entao exibe erro no campo` ``.
   Nomes sem acento. Em `src/androidTest` com `minSdk` < 30 o DEX não aceita
   espaços em nomes de método. Nesse caso, use
   `` `AC-03-dado_cpf_invalido_quando_enviar_entao_exibe_erro` ``, que o S-09 também reconhece.
2. **Dado/quando/então** no nome e no corpo (blocos separados por linha em branco).
   Cada `BR-xx` com limite numérico ou de data é testada **no limite, logo abaixo e logo acima**.
3. **Determinismo** (Art. IV.3, AP-45, AP-41): sem rede real, relógio real,
   `Thread.sleep`, `delay` real, locale ou fuso do sistema, nem dependência de
   ordem entre testes. Use `Clock` fixo injetado, `Locale("pt", "BR")` explícito,
   `TestDispatcher` e `runTest`. Teste que só passa isolado é bug.
4. **Teste comportamento, não implementação.** Verifique o `UiState` emitido, o
   `DataResult` retornado ou o efeito observável. `verify` de chamada só quando a
   interação é o próprio requisito (ex.: "não chamar a API de novo em duplo toque").
5. **Um conceito por teste.** Vários `assert` só quando descrevem o mesmo resultado.
   Compare o objeto inteiro (`assertEquals(expectedState, actual)`) em vez de campo a campo.
6. **Caminho triste é obrigatório** (AP-47): um teste por estado de UI da spec e
   por linha relevante da matriz de erros do plano.
7. **Não teste o framework** (getters, `data class` gerada, Hilt, Room gerado).
8. **JUnit 5 em todo teste novo** (AP-48). A única exceção são testes que precisam
   de runner JUnit 4 (Robolectric, `createComposeRule`, Roborazzi). Eles rodam via
   `junit-vintage-engine` e ficam restritos a módulos `:ui`.
9. **Teste instável é bug, não azar.** Proibido `@Disabled`/`@Ignore`, retry automático
   ou aumentar timeout/tolerância para passar (Art. VIII.3). Corrija a causa (seção 9)
   e registre no [steering loop](../steering-loop.md).

## 3. Coroutines e Flow
- **Um único `TestCoroutineScheduler` por teste.** A `MainDispatcherExtension`
  (`core:testing`) troca o `Main` por um `StandardTestDispatcher`, e o `runTest` reaproveita
  esse scheduler. Dispatchers injetados no SUT devem ser `StandardTestDispatcher(testScheduler)`,
  nunca um dispatcher criado à parte, que roda fora do tempo virtual.
- **`StandardTestDispatcher` por padrão.** Avance explicitamente com `runCurrent()`,
  `advanceTimeBy(ms)` ou `advanceUntilIdle()`. `UnconfinedTestDispatcher` só
  em coletores auxiliares, porque ele pode esconder estados intermediários.
- **Timeout, debounce e retry** são testados com tempo virtual (`advanceTimeBy`),
  nunca esperando de verdade.
- **Turbine para todo `Flow`:** `awaitItem()` para cada emissão esperada,
  `expectNoEvents()` para provar que nada mais aconteceu e `turbineScope { flow.testIn(backgroundScope) }`
  para observar vários flows ao mesmo tempo.
- **`StateFlow` é conflado:** valores iguais em sequência não reemitem, e estados
  intermediários podem ser perdidos se o dispatcher não for controlado. Se um teste
  "pula" o `Loading`, revise o dispatcher antes de mexer no ViewModel.
- **Coletas infinitas** (flows de banco, `WhileSubscribed`) vão em `backgroundScope`,
  que o `runTest` cancela sozinho ao terminar.

```kotlin
@Test
fun `AC-05 - dado busca digitada quando passar o debounce entao consulta uma unica vez`() = runTest {
    val viewModel = createViewModel()

    viewModel.uiState.test {
        skipItems(1) // Idle
        viewModel.onEvent(SearchUiEvent.QueryChanged("loja"))
        advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)
        expectNoEvents()

        advanceTimeBy(1)
        assertEquals(SearchUiState.Loading, awaitItem())
        assertEquals(SearchUiState.Content(results), awaitItem())
    }
    assertEquals(1, repository.calls)
}
```
Esqueleto completo: [templates/kotlin/FeatureViewModelTest.kt](../templates/kotlin/FeatureViewModelTest.kt).

## 4. Dublês de teste
| Preferência | Quando | Exemplo |
|---|---|---|
| 1. Real | Objetos puros e baratos: entidades, mappers, use cases simples | `GetBalanceUseCase(repository)` real no teste do ViewModel |
| 2. Fake | Repositórios, data sources, `Clock`, analytics, qualquer interface com estado | [FakeBalanceRepository.kt](../templates/kotlin/FakeBalanceRepository.kt) |
| 3. Mock (MockK) | Fronteira sem fake e quando a interação é o requisito | `coVerify(exactly = 1) { api.confirm(any()) }` |

- **Onde ficam os fakes:** em `testFixtures` do domain da feature (ou `feature:X:testing`).
  O `core:testing` guarda só utilitários genéricos (`MainDispatcherExtension`,
  `TestClock`, builders comuns), porque não pode depender de feature
  ([architecture.md](architecture.md)).
- **Fakes implementam a interface real** e falham alto quando mal configurados
  (`check(...)` com mensagem), como no template.
- **MockK:** sem `relaxed = true` por padrão. `coEvery`/`coVerify` para `suspend`.
  Não mocke `data class`, value objects, `Flow` nem o próprio SUT (AP-46).
- **Dados de teste** vêm de builders com valores padrão válidos (`aBalance(available = ...)`),
  para que cada teste declare só o que importa para ele.

## 5. Por camada
### Domain
- Kotlin puro, sem Robolectric. Cobre cada `BR-xx` (EARS) com valores-limite via
  `@ParameterizedTest` + `@CsvSource`/`@MethodSource`.
- Valores monetários com `BigDecimal` e escala explícita. Teste arredondamento e zero.

### Presentation (ViewModel)
- Um teste por estado de UI da spec e por `UiEvent`. Inclui retry, duplo toque
  (idempotência) e consumo de efeito pendente (navegação/snackbar como estado).
- **Morte de processo** (AP-32): crie o ViewModel com um `SavedStateHandle` preenchido
  e verifique que o formulário foi restaurado, sem PAN/CVV no handle.
- Teste o ViewModel com o use case real e o repositório fake. Mockar o use case só
  repete a implementação.

### Data
- **Rede com MockWebServer:** cada linha da matriz de erros do plano vira um teste
  (`200`, `400`, `401`, `404`, `409`, `5xx`, timeout, corpo malformado, sem conexão)
  que verifica o `AppError` resultante ([error-handling.md](error-handling.md)).
  Verifique também o request enviado (método, path, headers obrigatórios, corpo).
- **Fixtures JSON** em `src/test/resources`, copiadas do contrato e **sanitizadas**
  (ver seção 7). Inclua campos extras desconhecidos para provar que o parser tolera evolução da API.
- **Mappers:** campo nulo, ausente, enum desconhecido e formatos de data/moeda.
- **Room:** banco in-memory (`allowMainThreadQueries()` só em teste). Toda nova versão
  de schema tem teste de migração com `MigrationTestHelper` sobre o schema exportado.
  Migração sem teste não entra.
- **DataStore/arquivos:** use o `@TempDir` do JUnit 5, nunca o storage real.
- **Cache e offline-first:** teste a ordem cache → rede → atualização do cache e o
  comportamento quando a rede falha com cache presente.

### UI (Compose)
- Teste a **`Screen` stateless**, não a `Route`: passe o `UiState` e capture os
  `UiEvent` emitidos em uma lista.
- **Busque por semântica, como o usuário e o TalkBack enxergam:** `onNodeWithText`,
  `onNodeWithContentDescription`, `hasClickAction()`. Use `testTag` só quando não
  houver semântica estável.
- Verifique o requisito de acessibilidade (Art. VI.2): `contentDescription`, `heading()`,
  role, estado desabilitado e erro anunciado no campo. Ative as verificações automáticas de
  acessibilidade do Compose Test quando disponíveis no projeto (confirme a API com `android docs`).
- **Tempo e animação:** `composeTestRule.mainClock.autoAdvance = false` e avanço manual
  para shimmer/loading. Sem `waitUntil` com tempo real quando o estado é controlado pelo teste.

### Screenshot (Roborazzi)
- Cada `@Preview` de estado gera uma imagem de referência. Variantes mínimas por tela:
  tema claro, tema escuro e `fontScale = 2f`. Adicione telas pequena/expandida quando a
  spec previr layout adaptativo.
- Determinismo: `@GraphicsMode(NATIVE)`, locale `pt-BR`, `Clock` fixo, dados fixos e
  nenhum valor aleatório ou "agora".
- `./gradlew recordRoborazziDebug` grava e `verifyRoborazziDebug` compara (S-10, nível `full`).
- **Regravar a referência é mudança visual:** só com aprovação humana e com a spec
  indicando a mudança. Aumentar a tolerância de diff para passar é enfraquecer o sensor.

### Instrumentado / E2E
- Reservado aos fluxos críticos marcados na spec. Não duplique o que o unitário já prova.
- `@HiltAndroidTest` + `@TestInstallIn` para trocar rede/storage por fakes. Nada de
  backend real em CI.
- Isolamento entre testes (Test Orchestrator com `clearPackageData`) e execução em
  Gradle Managed Devices ou device farm, quando configurados no CI.
- Testes de release/manifest exigidos pela segurança (ex.: `FLAG_SECURE`) seguem
  [security.md](security.md).

## 6. Cenários mobile que a spec deve cobrir
Teste só o que a spec pede (Art. VII). Se algum item abaixo se aplicar à feature e
não estiver na spec, **sinalize a lacuna** em vez de inventar comportamento.

| Cenário | Pergunta para a spec | Onde testar |
|---|---|---|
| Rotação / morte de processo | O que precisa sobreviver? | ViewModel com `SavedStateHandle` |
| Offline, timeout, rede lenta | Qual estado e qual ação de retry? | ViewModel + MockWebServer |
| Duplo toque / reenvio | A operação é idempotente? | ViewModel (`verify` de chamada única) |
| Sessão expirada (`401`) | Para onde o usuário vai? | Repositório + ViewModel |
| Lista vazia, paginação, fim da lista | Há estado `Empty`? | ViewModel + screenshot |
| Fonte ampliada, tema escuro, TalkBack | Alguma restrição de layout? | Screenshot + UI de componente |
| Moeda, data e fuso (`pt-BR`, `America/Sao_Paulo`) | Formato exato exibido? | Unitário do formatador (AP-40) |
| Permissão negada / negada permanentemente | Qual fallback? | Instrumentado, se crítico |

## 7. Dados de teste e segurança
- **Nunca dados reais** de clientes, lojistas ou da produção, nem em fixtures, screenshots
  ou nomes de teste. O S-02 varre também os fixtures.
- **Cartão:** só números de teste das bandeiras (ex.: `4111 1111 1111 1111`). Mesmo
  assim, a UI e o screenshot devem exibir apenas o PAN mascarado (`**** 1111`), e
  existe um teste que prova o mascaramento.
- **PII:** CPF/CNPJ gerados (válidos pelo dígito verificador, sem pertencer a ninguém),
  nomes e e-mails fictícios (`cliente@example.com`).
- **Sem segredos:** tokens e chaves de teste são valores fictícios e explícitos (`"test-token"`).
- Teste que **logs, `toString()` e analytics não carregam PAN/CPF** sempre que a classe
  manipular esses dados ([security.md](security.md)).

## 8. Cobertura (S-11 Kover, verificada no `full`)
| Camada | Linhas (mínimo) |
|---|---|
| domain | 90% |
| presentation (ViewModel) | 85% |
| data | 75% |
| ui | screenshot por estado (sem meta de linha) |

- Cobertura é consequência dos ACs testados, não meta. Teste sem assert para subir
  percentual é AP-09.
- Exclusões (código gerado, módulos Hilt, `@Preview`) ficam centralizadas no
  `app.quality`. Nunca exclua classes avulsas para passar no limite.

## 9. Diagnóstico de teste instável
| Sintoma | Causa provável | Correção |
|---|---|---|
| Passa isolado, falha na suíte | Estado compartilhado (singleton, `companion`, fake reaproveitado) | Instância nova por teste; nada mutável em `companion object` |
| Falha às vezes com timeout do Turbine | Dispatcher fora do scheduler de teste | Injetar `StandardTestDispatcher(testScheduler)` |
| Estado intermediário "some" | `StateFlow` conflado ou `UnconfinedTestDispatcher` | Ver seção 3 |
| Falha em outro fuso/idioma ou no CI | Locale/fuso do sistema | `Clock` e `Locale` explícitos |
| Screenshot com diff pequeno e aleatório | Animação, data atual ou fonte | `mainClock` controlado, dados fixos |

Reproduza com `--rerun-tasks` antes de concluir que corrigiu.

## 10. Comandos
```bash
./gradlew :feature:<nome>:ui:testDebugUnitTest                                   # testes de um módulo
./gradlew :feature:<nome>:ui:testDebugUnitTest --tests "*<Nome>ViewModelTest*"   # uma classe
./gradlew recordRoborazziDebug / verifyRoborazziDebug                            # screenshots
./gradlew koverHtmlReport -Pharness.coverage=true                                # relatório de cobertura
scripts/harness/check-traceability.sh docs/specs/NNN-feature                     # AC ↔ testes (S-09)
scripts/harness/verify.sh standard                                               # antes de concluir
```

## 11. Checklist antes de concluir (o que o `test-reviewer` verifica)
- [ ] Todo `AC-xx` da spec aparece em ao menos um nome de teste e o teste **falharia** sem o código.
- [ ] Todos os estados de UI e as linhas da matriz de erros têm teste.
- [ ] Nenhum tempo real, rede real, locale/fuso implícito ou dependência de ordem.
- [ ] Fakes antes de mocks; nenhum mock de `data class` ou do SUT.
- [ ] Nenhum dado real, PAN sem máscara na UI ou segredo em fixtures.
- [ ] Nenhum `@Disabled`, retry, tolerância aumentada ou exclusão de cobertura ad hoc.
- [ ] `verify.sh standard` executado e passando.
