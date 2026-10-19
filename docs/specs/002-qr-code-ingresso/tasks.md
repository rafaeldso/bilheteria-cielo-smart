# Tarefas: QR Code do ingresso

**Spec:** [spec.md](spec.md) (1.1.2) · **Plano:** [plan.md](plan.md) · **UI Spec:** [ui-spec.md](ui-spec.md) (0.2.1) · **Estende:** [001](../001-venda-de-ingressos/tasks.md)

> Regras:
> - Tarefas em ordem de execução. `[P]` = pode rodar em paralelo com as outras `[P]` **do mesmo lote**
>   (arquivos disjuntos), via `implement-parallel` (máx. 4 workers `android-implementer` por lote, modelo sonnet).
> - Workers não editam `build-logic/`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `config/`, `quality/`: por isso a
>   Fase 0 é sequencial, na sessão principal.
> - Cada tarefa fecha com `scripts/harness/verify.sh fast`; nome de teste cita o `AC-xx` (a numeração continua a da 001).
> - Pacote base: `com.rafaeloliveira.bilheteria`. Módulos existentes: `feature/ticketing/{api,domain,data,ui}`, `core/designsystem`.
> - Arquivo novo leva o cabeçalho de licença Apache 2.0. Testes de UI Compose e screenshot de previews continuam **fora de foco**
>   (decisão do humano, 2026-10-18): os ACs de UI ficam em ViewModel e lógica pura.
> - Branch sugerida: `feat/002-qr-code-ingresso` (criada só a pedido; não há commit nem push sem pedido).

## Fase 0 — Spike e fundação (sequencial, sessão principal)
- [x] **T01** *(spike executado em 2026-10-19; achados em [research.md](research.md); R1 confirmado no AVD Android 10: decisão do humano = tentar o MediaStore na T09 e, se falhar no AVD, testar um AVD Android 11 a 13)* Spike do `lio://print` no AVD Android 10 com `lio-emulator` (skill `verify-on-device`; subagente `device-tester`):
  montar `lio://print` à mão com `PRINT_IMAGE` e **(a)** testar se o app Cielo lê um PNG gravado em `MediaStore` (`Pictures/Bilheteria`)
  e por qual caminho; **(b)** ver se o pedido leva credenciais ou só `{operation, styles, value}`; **(c)** limite de altura da imagem
  (um ingresso × vários); **(d)** se o app Cielo toma a tela a cada chamada; **(e)** respostas nos cenários sucesso, sem papel e erro;
  **(f)** o QR Code impresso aparece legível na escolha "imagem/QR" do emulador. Conferir também no Maven Central o
  `group:artifact` e a versão **estável** do ZXing core · Riscos R1, R3, R4, R6, R8 do plano · BR-38, BR-40, BR-44 ·
  **Pronto quando:** achados registrados em `docs/specs/002-qr-code-ingresso/research.md`. **Se R1 se confirmar** (só
  `MANAGE_EXTERNAL_STORAGE` funciona): parar e pedir sua decisão antes de qualquer permissão (ADR-0012); se algo mudar BR-38/BR-40, nova versão da spec
- [x] **T02** Catálogo e `build-logic`: `zxing-core` (versão estável da T01) no `libs.versions.toml` e dependência só em
  `feature:ticketing:data`; OSV-Scanner sem CVE · Plano §2, Art. III · **Pronto quando:** `./gradlew help` ok, nenhuma dependência
  fora do catálogo, `scripts/harness/security-scan.sh` (OSV) sem achado, `verify.sh fast` verde
- [x] **T03** Contratos do domain (plano §4): `model/` (`TicketCode` com `toString` mascarado, `Ticket`, `TicketSet`, `QrMatrix`,
  `PrintStatus`, `PrintOutcome`, `PrintFailureReason`), portas `QrEncoder`, `TicketPrinter`, `TicketFiles` (`ShareBundle`), casos novos de
  `TicketingError` (`TicketsUnavailable`, `TicketGenerationFailed`); fakes `FakeQrEncoder`, `FakeTicketPrinter`, `FakeTicketFiles` em
  `testFixtures` · BR-30, BR-31, BR-38, BR-40 · **Pronto quando:** compila em JVM puro (sem `android.*`), bate com o plano, Konsist verde

## Fase 1 — Domain (lote A) e design system (lote B), em paralelo
**Lote A** (`feature:ticketing:domain`, um arquivo de use case + teste por tarefa):
- [x] **T04** [P] `GenerateTicketsUseCase` e `ObserveTicketsUseCase` + testes · BR-30–33, BR-36, BR-37 ·
  AC-29, AC-30, AC-31, AC-32, AC-35, AC-36, AC-37 · **Pronto quando:** compra Approved de 3 gera 3 `TicketCode` distintos e estáveis
  (`bilheteria:1:<reference>:<n>`), sem nenhum campo da transação; Declined, Cancelled, Failed e Pending não geram; leitura falha →
  `StorageFailure`, codificação falha → `TicketGenerationFailed`; aprovação tardia (Failed→Approved no `FakePurchaseRepository`) passa a emitir os tickets
- [x] **T05** [P] `PrintTicketsUseCase` e `ShareTicketsUseCase` + testes · BR-38, BR-39, BR-41, BR-42, BR-44 ·
  AC-38, AC-39, AC-41, AC-42, AC-44 · **Pronto quando:** só compra Approved imprime/compartilha; `Printing` ignora nova impressão e
  compartilhar; retoma de `PrintStatus.Failed.printed`; `Done` vira `Printed`; texto do compartilhamento só com evento, data absoluta e local

**Lote B** (`core:designsystem`, arquivos disjuntos; um `@Preview` por variante, claro e escuro):
- [x] **T06** [P] `DsQrCode` + `QrMatrix` do design system (módulo de tamanho inteiro em pixels, margem de 4 módulos, preto sobre branco
  fixo), tokens `qr.foreground`/`qr.background` no tema (espelham `base.tokens.json`), estado **ocupado** do `DsButton` (contêiner `primary`,
  toque bloqueado, `stateDescription`), `DsSnackbar` com ação (se ainda não existir em código) · ficha da UI Spec 002 §5 ·
  **Pronto quando:** cores iguais aos tokens (teste de igualdade); previews por variante; detekt e compose-rules verdes

## Fase 2 — Data (lote C, paralelo; depois T09 e T11 sequenciais)
- [x] **T07** [P] `ZxingQrEncoder` + `TicketImageRenderer` (384 px e 1080 px, mesma `QrMatrix`, margem e cores de `qr.*`) + testes ·
  BR-30, BR-31 · AC-29, AC-30 · **Pronto quando:** teste de **ida e volta** (renderiza e decodifica com o leitor do ZXing e compara com o
  `TicketCode`); módulo inteiro em pixels; erro de codificação → `TicketGenerationFailed`
- [x] **T08** [P] Roteamento do callback (risco R2, crítico): `CieloResponseParser` ganha o ramo de impressão e `CieloCallbackHandler`
  roteia **por estado** (o spike mostrou que o erro de arquivo usa `{code:2,reason}`, igual a um erro de pagamento; research.md §3): com
  impressão aguardando resposta, `{code, message|reason}` sem `id`/`reference`/`results` é de impressão; sem impressão em andamento segue o
  pagamento como hoje; resposta de impressão sem trabalho é descartada (`print_result_discarded`); a impressão não inicia com compra aberta
  (`Pending`/`Recovering`) · BR-40, STRIDE (spoofing cruzado) · AC-40 · **Pronto quando:** teste prova que `{code:1,message:"WITHOUT PAPER"}` e
  `{code:2,reason:"...EACCES..."}` com impressão em andamento **não** alteram uma compra Pending, que as respostas de pagamento existentes
  continuam iguais (testes da 001 verdes) e que resposta de impressão sem trabalho é descartada
- [x] **T10** [P] `DefaultTicketFiles` (PNG em `filesDir/shared_tickets/`, nome aleatório, limpeza de 1 h) + `FileProvider`
  (`<provider>` no manifest do `data`, `exported=false`, `grantUriPermissions`, `res/xml/ticket_file_paths.xml` só do subdiretório) +
  testes · BR-41–43 · AC-41, AC-42, AC-43 · **Pronto quando:** URI aponta para `filesDir/shared_tickets`; teste do manifest: provider
  `exported=false` e **nenhuma** permissão de armazenamento; texto sem dado da transação; falha de gravação → `TicketGenerationFailed`
- [x] **T09** `CieloPrintEncoder` e `CieloPrintCoordinator` (um ingresso por chamada, retomada, timeout de 15 s, `PaymentSession`
  como serviço em primeiro plano se a T01 indicar, arquivo no `MediaStore` só em API ≥ 29, apagado ao fim, no timeout e na abertura do app)
  + testes · BR-38–40, BR-44 · AC-38, AC-39, AC-40, AC-44 · **Pronto quando:** JSON/URI do `PRINT_IMAGE` correto e **sem credenciais** (se a T01
  confirmar); um passo por vez; `0` avança, `1` → `NoPaper`, `2` → `PrinterError`, ilegível/timeout → `Unknown`/`Timeout`, app ausente e API < 29 →
  `TerminalAppUnavailable`; retomada não reenvia os já impressos; arquivo apagado em todos os caminhos. Depende de T03, T07 e T08
- [x] **T11** Módulo Hilt `TicketDataModule` (`@Binds` de `QrEncoder`, `TicketPrinter`, `TicketFiles`), limpeza de sobras na abertura do app
  · Plano §2 · **Pronto quando:** o grafo do `app` compila; Konsist e `assertModuleGraph` verdes. Depende de T07, T09 e T10

## Fase 3 — Presentation
- [x] **T12** `TicketsRoute(purchaseId)` em `:api`; `ReceiptUiEvent.OnOpenTickets` e callback na `ReceiptRoute`; destino novo em
  `TicketingNavGraph` · BR-34 · AC-33 · **Pronto quando:** teste cruzado da chave `purchaseId` (como na 001); compila
- [x] **T13** `TicketsUiState`, `TicketActionState`, `TicketsUiEvent` e `TicketsViewModel` (um `StateFlow`, um `onEvent`, `combine` com
  `TicketPrinter.status`, `stateIn(WhileSubscribed(5_000))`, `ShareReady` consumido por `OnShareLaunched`, `OnActionDismissed`); mapeador do
  comprovante que só oferece "Ver ingressos" em `Approved` · BR-33–37, BR-39–44 · AC-33, AC-34, AC-35, AC-36, AC-37, AC-38, AC-39, AC-40,
  AC-41, AC-43, AC-44 · **Pronto quando:** testes de ViewModel (Turbine) cobrem cada estado e evento da spec §6, D3 (compartilhar indisponível em `Printing`),
  `OnRetry`/`OnRetryAction` e que `Declined`/`Cancelled` não oferecem a ação. Depende de T03, T04, T05 e T12

## Fase 4 — UI
- [x] **T14** `TicketsScreen` stateless + `TicketsRoute`, `TicketCard`, barra de ações (fixa; rola com `fontScale` ≥ 1,5), "Ver ingressos" e barra fixa na
  SCR-03 `Approved` (UI Spec 001 0.5.0), `strings_tickets.xml` com as chaves da UI Spec §4.4, um `@Preview` por estado (Loading, Idle, Printing,
  PrintFailed por motivo, Printed, ShareFailed, Error, claro/escuro/fonte grande), abertura do menu de compartilhamento (`ACTION_SEND_MULTIPLE`,
  `ClipData`, `FLAG_GRANT_READ_URI_PERMISSION`, `Intent.createChooser`) na `Route` · UI Spec 002 · AC-33, AC-34, AC-41 ·
  **Pronto quando:** todos os estados renderizados, strings sem hardcode, rótulos falados e headings da §4.6, `compose-ui-reviewer` sem bloqueante

## Fase 5 — Fechamento
- [x] **T15** Revisão agêntica (skill `review-changes`, subagentes sonnet) + `security-scan` (obrigatório: manifest, `FileProvider`, intents, callback,
  armazenamento) · **Pronto quando:** sem achado bloqueante; achados triados em `docs/security/`
- [x] **T16** *(2026-10-19: J10 e J13 ✅ no AVD API 29; J11 e J12 ✅ no AVD API 30 com `MANAGE_EXTERNAL_STORAGE` ao app Cielo; no API 29 a impressão por caminho dá EACCES, pendência aceita: validar em terminal real; texto do compartilhamento e Android 12+ não observados; ver research.md §4)* Verificação no AVD (skill `verify-on-device`): **J10** abrir ingressos pelo comprovante e pelo histórico; **J11** imprimir com sucesso;
  **J12** sem papel e erro (cenários do emulador) com retomada; **J13** compartilhar e falha · AC-33, AC-34, AC-38, AC-40, AC-41, AC-44 ·
  **Pronto quando:** Journeys executadas com capturas e a11y conferida; o que o AVD não cobrir fica registrado como pendência aceita
- [x] **T17** `scripts/harness/verify.sh full` + `scripts/harness/check-traceability.sh docs/specs/002-qr-code-ingresso` + skill `consistency-check`
  (spec ↔ plano ↔ tarefas ↔ código) · **Pronto quando:** `full` verde e todos os AC-29…44 com teste citando o ID
- [x] **T18** Atualizar docs: spec 002 → Implementada (changelog), `components.md` (`DsQrCode`, `DsSnackbar` → Implementado), roadmap D2 ✅, README
  (QR Code, impressão e compartilhar; limites: sem validação, impressão só em API ≥ 29), `docs/ai/log.md`, UI Specs com as versões finais ·
  **Pronto quando:** links válidos; `verify.sh standard` verde

## Rastreabilidade (AC → tarefas → testes)
| AC | Tarefas | Testes (nome começa com o ID) |
|---|---|---|
| AC-29 | T04, T07 | `AC-29 - dado compra aprovada de 3 quando gerar entao 3 codigos distintos` (domain); `AC-29 - ... ida e volta decodifica o codigo` (data) |
| AC-30 | T04, T07 | `AC-30 - dado compra com dados da transacao entao o codigo nao contem nenhum deles` |
| AC-31 | T04 | `AC-31 - dado mesma compra quando gerar duas vezes entao mesmo conteudo` |
| AC-32 | T04 | `AC-32 - dado compra negada cancelada falhou ou pendente entao nao gera` |
| AC-33 | T12, T13, T14 | `AC-33 - dado comprovante aprovado entao oferece ver ingressos` / `... negada ou cancelada entao nao oferece` |
| AC-34 | T13, T14 | `AC-34 - dado compra aprovada quando abrir entao carrega e mostra um por ingresso` |
| AC-35 | T04, T13 | `AC-35 - dado leitura ou geracao falha entao erro e retry repete` |
| AC-36 | T04, T13 | `AC-36 - dado compra nao aprovada ou inexistente entao erro sem codigo` |
| AC-37 | T04, T13 | `AC-37 - dado aprovacao tardia entao passa a oferecer os ingressos` |
| AC-38 | T05, T09, T13 | `AC-38 - dado compra aprovada quando imprimir entao um ingresso por chamada sem dado de transacao` |
| AC-39 | T05, T09, T13 | `AC-39 - dado impressao em andamento quando tocar de novo entao ignora imprimir e compartilhar` |
| AC-40 | T08, T09, T13 | `AC-40 - dado sem papel depois do 1o quando tentar de novo entao so os ingressos 2 e 3`; `AC-40 - dado impressao aguardando e sem papel quando o callback chegar entao a compra nao muda` |
| AC-41 | T05, T10, T13, T14 | `AC-41 - dado compra aprovada quando compartilhar entao abre o menu com as imagens` |
| AC-42 | T05, T10 | `AC-42 - dado compartilhar entao sem permissao de armazenamento e sem dado sensivel` |
| AC-43 | T10, T13 | `AC-43 - dado falha ao gerar ou sem app entao erro e retry` |
| AC-44 | T05, T09, T13 | `AC-44 - dado impressao concluida entao confirma e libera as acoes` |

Todo BR tem ao menos um AC; BR-33/BR-34 (AC-32, AC-33), BR-35 (AC-34), BR-36 (AC-35, AC-36), BR-37 (AC-37), BR-38/39/40/44 (AC-38…40, AC-44), BR-41/42/43 (AC-41…43), BR-30/31/32 (AC-29…31).
