# Tarefas: Venda de ingressos com pagamento Cielo Smart

**Spec:** [spec.md](spec.md) (1.1.0) · **Plano:** [plan.md](plan.md) · **UI Spec:** [ui-spec.md](ui-spec.md) (0.4.0) · **ADR:** [0010](../../adr/0010-integracao-cielo-smart-por-deep-link.md)

> Regras:
> - Tarefas em ordem de execução. `[P]` = pode rodar em paralelo com as outras `[P]` **do mesmo lote**
>   (arquivos disjuntos), via `implement-parallel` (máx. 4 workers `android-implementer` por lote).
> - Workers não editam `build-logic/`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `config/`, `quality/`:
>   por isso a fundação (Fase 0) é sequencial, na sessão principal.
> - Cada tarefa fecha com `scripts/harness/verify.sh fast`; nome de teste cita o `AC-xx`.
> - Pacote base: `com.rafaeloliveira.bilheteria`. Feature: `feature/ticketing/{api,domain,data,ui}`.

## Fase 0 — Fundação da feature (sequencial, sessão principal)
- [x] **T01** Validar o emulador Cielo antes de codar: instalar `lio-emulator.apk` 1.61.8 no AVD Android 10, descobrir o pacote
  (`adb shell pm list packages`), abrir um `lio://payment` montado à mão com credenciais fictícias e ver se a tela de pagamento aparece;
  testar se `lio://orders` abre UI ou responde direto · Riscos do plano §11 · **Pronto quando:** achados registrados no `research.md` §13
  (pacote, credenciais, comportamento do `orders`)
- [x] **T02** Catálogo e `build-logic`: Navigation Compose, `kotlinx-serialization` (lib + plugin), Room (+ KSP, schema export),
  `kotlinx-collections-immutable`, `desugar_jdk_libs` (core library desugaring nos módulos Android), `hilt-navigation-compose`,
  Robolectric, Compose UI Test, `junit-vintage-engine`; versões estáveis consultadas na fonte · Plano §2 ·
  **Pronto quando:** `./gradlew help` ok, nenhuma versão fora do catálogo, `verify.sh fast` verde
- [x] **T03** Scaffold (skill `android-feature-scaffold`): `feature:ticketing:{api,domain,data,ui}` e `core:database`, registro no
  `settings`, dependências no `app`; Konsist da ADR-0010 (`lio://`, `Base64` e DTOs Cielo só em `..data.cielo..`) ·
  **Pronto quando:** módulos compilam vazios, grafo de módulos (`assertModuleGraph`) e Konsist verdes
- [x] **T04** Contratos do domain (plano §4): `model/` (Money, PurchaseReference, Event, AvailableEvent, EventMonth, Purchase,
  PurchaseStatus, FailureReason, TransactionData, PaymentOutcome com KDoc do mapeamento Q8), `repository/`, `payment/PaymentGateway`,
  `error/TicketingError` (sem stubs de use case: corpos `TODO()` seriam barrados pelo detekt; cada worker cria o seu) · BR-06, BR-08, BR-13, BR-14 · **Pronto quando:** compila em JVM puro e
  bate com o plano; fakes de `PurchaseRepository`/`PaymentGateway`/`EventCatalogRepository` em `testFixtures` do domain

## Fase 1 — Domain (lote A, paralelo) e design system (lote B, paralelo)
**Lote A** (`feature:ticketing:domain`, um arquivo de use case + teste por tarefa):
- [x] **T05** [P] `ObserveAvailableEventsUseCase`, `GroupEventsByMonthUseCase`, `QuantityLimits` + testes ·
  BR-01, BR-02, BR-05–07, BR-17, BR-22–24 · AC-01, AC-02, AC-04, AC-05, AC-06, AC-17, AC-22, AC-23, AC-24 ·
  **Pronto quando:** testes `AC-xx` passam com `Clock` fixo, limites testados no valor, abaixo e acima
- [x] **T06** [P] `StartPurchaseUseCase` + testes · BR-08–10, BR-14 (app ausente) · AC-07, AC-08, AC-12 ·
  **Pronto quando:** compra gravada antes do gateway; segunda compra aberta recusada; app ausente → `Failed`
- [x] **T07** [P] `HandlePaymentOutcomeUseCase` e `RecoverPendingPurchaseUseCase` + testes · BR-11–15, BR-25, STRIDE (spoofing) ·
  AC-09, AC-10, AC-11, AC-13, AC-14, AC-15 · **Pronto quando:** cada código Cielo vai ao status certo; resposta forjada
  (reference/valor divergentes, sem compra aberta) é descartada; paginação até 4 páginas e "não encontrado" testados
- [x] **T08** [P] `ObservePurchaseUseCase` e `ObservePurchaseHistoryUseCase` + testes · BR-16, BR-18–21 · AC-16, AC-18, AC-19,
  AC-20, AC-21 · **Pronto quando:** histórico só com Approved/Declined/Cancelled, ordem decrescente; erros propagados

**Lote B** (`core:designsystem`, arquivos disjuntos; um `@Preview` por variante, claro e escuro):
- [x] **T09** [P] Tema com a paleta A (cores dos `tokens/*.tokens.json`, extensões `success`/`warning`), tipografia e formas;
  `DsTopAppBar`, `DsButton` (filled/tonal/outlined, largura total, desabilitado), `DsIconButton`, `DsFilterChip` (borda `outline`) ·
  **Pronto quando:** cores iguais aos tokens (teste de igualdade), detekt e compose-rules verdes
- [x] **T10** [P] `DsListItem`, `DsSummaryCard`, `DsStatusBadge` (sucesso/erro/neutro/alerta), `DsKeyValueList` ·
  **Pronto quando:** previews por variante; semântica (`mergeDescendants`, heading) conforme a UI Spec
- [x] **T11** [P] `DsLoadingSkeleton`, `DsEmptyState`, `DsErrorState`, `DsProgressIndicator`, `DsQuantityStepper` ·
  **Pronto quando:** API igual à ficha da UI Spec §5; reduzir animações respeitado; previews

## Fase 2 — Data (lote C, paralelo; depois T15 sequencial)
- [x] **T12** [P] `core:database` (Room v1: `BilheteriaDatabase`, `PurchaseEntity`, `PurchaseDao`) + `DefaultPurchaseRepository`
  (compra aberta única em transação, histórico, soma de aprovadas) + testes Room in-memory · BR-10, BR-17, BR-18 ·
  AC-08, AC-17, AC-18, AC-20 · **Pronto quando:** schema exportado; testes de repositório passam; matriz de erros (StorageFailure)
- [x] **T13** [P] Catálogo: `assets/events.json` (7+ eventos fictícios de nov/2026 a mar/2027, 3+ meses, um esgotável),
  DTO, mapper, `DefaultEventCatalogRepository` + testes · BR-01, BR-04 · AC-01, AC-03 · **Pronto quando:** JSON inválido →
  `CatalogUnavailable`; mapper testado
- [x] **T14** [P] `CieloRequestEncoder` e `CieloResponseParser` + testes com fixtures da doc da Cielo (sucesso, erro `code` 1–4,
  desconhecido, ilegível, listagem com `reference`, campos sensíveis ignorados, limite de tamanho) · BR-09, BR-11–14, BR-25 ·
  AC-07, AC-09–AC-15 · **Pronto quando:** URI igual ao research §12; `bin`/`mask`/`document` nunca chegam ao modelo
- [x] **T15** `CieloDeepLinkGateway` (intent explícita com allowlist de pacote), `CieloCallbackActivity`, `PaymentForegroundService`,
  `CieloCredentials` (propriedades Gradle → `BuildConfig`, fictícias por padrão), manifest (`<queries>`, `cs_integration_type`,
  permissões FGS) e DI · BR-09, BR-14, BR-15, BR-25 · AC-07, AC-12 · Depende de T12, T14 · **Pronto quando:** testes do gateway
  (app ausente, pacote fora da allowlist) passam; `security-reviewer` sem bloqueante; lint/mobsfscan sem achado novo

## Fase 3 — Presentation (lote D, paralelo)
Cada tarefa: `UiState`/`UiEvent` exatamente como no plano §4, ViewModel com um `StateFlow` e um `onEvent`, testes com Turbine.
- [x] **T16** [P] `EventListViewModel` · AC-01, AC-02, AC-03, AC-17, AC-22, AC-23, AC-24
- [x] **T17** [P] `PurchaseViewModel` (quantidade no `SavedStateHandle`, recuperação ao abrir com compra aberta) · AC-04–AC-08,
  AC-12–AC-15, AC-25, AC-26 (spec 1.2.0)
- [x] **T18** [P] `ReceiptViewModel` · AC-09, AC-10, AC-11, AC-16, AC-19, AC-27 (spec 1.2.0)
- [x] **T19** [P] `PurchaseHistoryViewModel` · AC-18, AC-20, AC-21

**Pronto quando (todas):** testes `AC-xx` passam; Konsist (ViewModel com um `uiState` e um `onEvent`) verde.

## Fase 4 — UI (lote E, paralelo; depois T24 sequencial)
Skill `compose-screen`: `Route` + `Screen` stateless + um `@Preview` por estado da UI Spec, strings em arquivo próprio por tela
(`res/values/strings_<tela>.xml`) para não haver conflito entre workers.
- [x] **T20** [P] SCR-01 Eventos à venda (chips de mês, cabeçalho fixo) + testes de UI · AC-02, AC-03, AC-22, AC-23
- [x] **T21** [P] SCR-02 Compra (stepper, `Loading`, `AwaitingPayment`, `Recovering`, 5 variantes de erro, `Unavailable`, voltar bloqueado) + testes de UI · AC-06, AC-08, AC-26
- [x] **T22** [P] SCR-03 Comprovante (3 estados + `Loading` e `Error`, data absoluta) + testes de UI · AC-16, AC-19, AC-27
- [x] **T23** [P] SCR-04 Histórico + testes de UI · AC-20, AC-21
- [ ] **T24** Navegação: rotas em `:api`, grafo da feature, `NavHost` no `app`, navegação como estado, retorno do callback para a
  tela certa, `Completed` navega com `popUpTo<PurchaseRoute> { inclusive = true }` (plan §4) · **Pronto quando:** app abre na SCR-01 e percorre as 4 telas; `assertModuleGraph` verde

**Pronto quando (T20–T23):** todos os estados da UI Spec com preview; testes de UI passam; `compose-ui-reviewer` sem bloqueante.
> Testes de UI Compose e screenshot de previews ficam fora de foco por decisão do humano; os ACs seguem cobertos pelos testes de ViewModel (T16–T19) e a lógica pura nova por testes JVM.

## Fase 5 — Dispositivo
- [ ] **T25** Journeys no AVD Android 10 com `lio-emulator` (skill `verify-on-device`): aprovado, cancelado, erro, app fechado durante
  o pagamento, filtro de mês; capturar o código real do botão "Error", a unidade de `unitPrice` e a ordenação do `orders`;
  ajustar e registrar no README · AC-07, AC-09, AC-10, AC-13, AC-15 · **Pronto quando:** Journeys verdes e achados documentados

## Fase 6 — Fechamento
- [ ] **T26** `review-changes` (architecture, security, test, compose-ui) e correção dos bloqueantes
- [ ] **T27** `consistency-check` + `scripts/harness/check-traceability.sh docs/specs/001-venda-de-ingressos`
- [ ] **T28** `scripts/harness/verify.sh full` verde; spec → Implementada; changelog; seção da integração Cielo no README

## Rastreabilidade
| AC | Tarefas | Testes (nome começa com) |
|---|---|---|
| AC-01 | T05, T13, T16 | `AC-01 - dado eventos futuros e passados quando abrir entao lista so os disponiveis` |
| AC-02 | T05, T16, T20 | `AC-02 - dado catalogo sem eventos disponiveis quando abrir entao exibe vazio` |
| AC-03 | T13, T16, T20 | `AC-03 - dado falha ao ler catalogo quando tentar de novo entao exibe lista` |
| AC-04 | T05, T17 | `AC-04 - dado evento de 50 reais quando selecionar entao quantidade 1 e total 50` |
| AC-05 | T05, T17 | `AC-05 - dado quantidade 1 quando aumentar para 3 entao total 150` |
| AC-06 | T05, T17, T21 | `AC-06 - dado limite de quantidade quando tentar passar entao acao indisponivel` |
| AC-07 | T06, T14, T15, T17, T25 | `AC-07 - dado compra confirmada quando pagar entao grava pendente antes de abrir a Cielo` |
| AC-08 | T06, T12, T17, T21 | `AC-08 - dado compra em andamento quando pagar de novo entao nao abre segundo pagamento` |
| AC-09 | T07, T14, T18, T25 | `AC-09 - dado retorno autorizado quando processar entao registra aprovada` |
| AC-10 | T07, T14, T18, T25 | `AC-10 - dado codigo 1 quando processar entao registra cancelada` |
| AC-11 | T07, T14, T18 | `AC-11 - dado codigo 3 quando processar entao registra negada` |
| AC-12 | T06, T15, T17 | `AC-12 - dado app Cielo ausente quando pagar entao erro sem cobranca` |
| AC-13 | T07, T14, T17, T25 | `AC-13 - dado codigo 2 ou 4 quando processar entao falhou com nova tentativa` |
| AC-14 | T07, T14, T17 | `AC-14 - dado resposta ilegivel quando processar entao consulta a Cielo antes de cobrar` |
| AC-15 | T07, T14, T17, T25 | `AC-15 - dado compra aberta ao reabrir quando consultar entao aplica resultado ou nao encontrado` |
| AC-16 | T08, T18, T22 | `AC-16 - dado compra aprovada quando exibir comprovante entao mostra campos sem cartao` |
| AC-17 | T05, T12, T16 | `AC-17 - dado 50 ingressos quando aprovar 1 entao mostra 49` |
| AC-18 | T08, T12, T19 | `AC-18 - dado compras registradas quando abrir historico entao ordem decrescente sem falhas` |
| AC-19 | T08, T18, T22 | `AC-19 - dado compra no historico quando selecionar entao abre comprovante` |
| AC-20 | T08, T12, T19, T23 | `AC-20 - dado nenhuma compra quando abrir historico entao exibe vazio` |
| AC-21 | T08, T19, T23 | `AC-21 - dado falha ao ler historico quando abrir entao exibe erro com nova tentativa` |
| AC-22 | T05, T16, T20 | `AC-22 - dado eventos em dois meses quando abrir entao agrupa por mes` |
| AC-23 | T05, T16, T20 | `AC-23 - dado filtro de mes quando selecionar dezembro entao so eventos de dezembro` |
| AC-24 | T05, T16 | `AC-24 - dado mes esgotado quando voltar a lista entao filtro volta para todos` |
| AC-25 | T17 | `AC-25 - dado falha na consulta a Cielo quando tentar de novo entao repete a consulta sem abrir pagamento` |
| AC-26 | T17, T21 | `AC-26 - dado catalogo ilegivel e sem compra aberta quando abrir a compra entao exibe indisponivel` |
| AC-27 | T18, T22 | `AC-27 - dado registro ilegivel quando tentar de novo entao volta a carregar e repete a leitura` |

## Ordem de execução e paralelismo
| Etapa | Tarefas | Quem | Paralelo |
|---|---|---|---|
| 1 | T01 → T02 → T03 → T04 | Sessão principal | Não (build, catálogo e contratos) |
| 2 | Lote A (T05–T08) | 4 × `android-implementer` | Sim |
| 3 | Lote B (T09–T11) | 3 × `android-implementer` | Sim |
| 4 | Lote C (T12–T14) → T15 | 3 workers → sessão principal | Sim / não |
| 5 | Lote D (T16–T19) | 4 workers | Sim |
| 6 | Lote E (T20–T23) → T24 | 4 workers → sessão principal | Sim / não |
| 7 | T25 → T26 → T27 → T28 | Sessão principal + revisores | Revisores em paralelo |

Os lotes A e B não dependem um do outro; rodam em sequência só pelo limite de 4 workers simultâneos.
