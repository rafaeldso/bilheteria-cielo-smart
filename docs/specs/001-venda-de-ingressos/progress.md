# Progresso: 001 — Venda de ingressos com pagamento Cielo Smart

> Estado de trabalho entre sessões e agentes (skill `session-handoff`). Atualize ao
> fim de cada fase e antes de `/clear`. A lista de tarefas continua no [tasks.md](tasks.md).
> Nunca registre segredos, PAN ou PII aqui.

**Tamanho:** G · **Fase SDD:** 5 Implementar · **Spec:** v1.3.0 (Aprovada) · **UI Spec:** v0.4.2 (Aprovada) · **Branch:** `main` (commits locais do lote C em diante, sem push)

## Estado atual
- **Última etapa concluída:** lote E (T20–T23) e T24 (navegação) commitados localmente; lote E revisado em 2 rodadas (compose-ui, architecture, ui-design) sem bloqueante
- **Próxima etapa:** fechar a T25 com o que falta no dispositivo (TalkBack, modo escuro, AVD API 30+, terminal real, J8); depois T26 (`review-changes`). Extras desta sessão: splash nativa (`core-splashscreen` 1.2.0, aprovada) e ícone do app.
- **CI:** correções de `security` enviadas; confirmar verde após o próximo push
- **Arquivos em andamento:** nenhum
- **Último sensor:** `verify.sh standard` → verde (exit 0) e `assertModuleGraph` verde em 2026-10-18 (T24)

## Decisões da implementação
| Data | Decisão | Motivo | Afeta spec/plano? |
|---|---|---|---|
| 2026-10-17 | Integração por Deep Link (`lio://`), sem SDK | Política Cielo de camada de abstração obrigatória; SDK Local descontinuado ([research.md](research.md)) | Plano |
| 2026-10-17 | Status interno "Falhou" para falhas técnicas; código 3 = Negada | Q8 opção A; explicação vai em KDoc no mapeamento | Spec (BR-13, BR-14) |
| 2026-10-17 | Disponível = catálogo − compras aprovadas, por aparelho | Q11; limitação nos trade-offs do README | Spec (BR-17) |
| 2026-10-18 | `StartPurchase` devolve a falha do gateway mesmo se o `update` para Failed falhar | A compra que ficar Pending é resolvida pela recuperação (BR-15); revisar no `review-changes` | Não |
| 2026-10-18 | Pedido na listagem com o nosso `reference` e valor diferente → `Failed`/NotFound | Enviamos o valor exato; divergência indica resposta forjada; aprovar abriria fraude | Não (anti-spoofing do plano §7) |
| 2026-10-18 | T10/T11 rodaram após T09 (dependem de cores estendidas e `DsIconButton`) | Lote B não era totalmente independente | Não |
| 2026-10-18 | detekt `ForbiddenImport` excluído em testes data/ui + Konsist (JUnit 4 só com `@RunWith`; sem Log/Date) | Room in-memory exige Robolectric/JUnit 4 (aprovado pelo humano) | testing.md |
| 2026-10-18 | Erro Cielo forjado sem `reference` = risco aceito | Consulta não distingue em andamento de cancelado; exige app malicioso no terminal gerenciado | Plano §7 |
| 2026-10-18 | Módulos Hilt do data na pasta única `di/` (como no plano §2) | Decisão humana; mesmo padrão de `core:*` | Não |
| 2026-10-18 | Callback Cielo sem `BROWSABLE` | security-reviewer: link/QR acionaria o callback exportado; validar na T25 | Plano §4 |
| 2026-10-18 | Teto de 10 (BR-07) ligado ao `StartPurchase` na integração | T05 e T06 rodaram em paralelo sem dependência | Não |
| 2026-10-18 | Novo `ObserveOpenPurchaseUseCase` no domain; `PurchaseUiState.Completed(purchaseId)` | VM só consome use cases; navegação ao comprovante como estado | Plano §3/§4 |
| 2026-10-18 | `Completed` navega com `popUpTo<PurchaseRoute>{inclusive}` (T24), sem evento de consumo | Estado deriva do Room | Plano §4; requisito da T24/T25 |
| 2026-10-18 | Estados presos viram estados: `Unavailable`, `ReceiptUiState.Error`, `Error(RecoveryFailed)` | Achados de revisão; retry da consulta nunca paga (BR-10) | Spec 1.2.0 (BR-26..28, AC-25..27), UI Spec 0.4.2 |
| 2026-10-18 | Compra aberta ilegível + evento ausente → `Unavailable` (reabrir a compra) | Sem "Pagar", sem risco de dinheiro | Nota no AC-26 e plano §5 |
| 2026-10-18 | `PurchaseViewModel` coleta no `init` (exceção ao AP-22) | Recuperação da BR-15 roda sem UI assinando | Plano §4 |
| 2026-10-18 | Binding mínimo do `ReferenceGenerator` (UUID v4) antecipado da T24 | `standard` quebrava o Hilt do app | Não |
| 2026-10-18 | Rodada extra de revisão (3ª) com worker Sonnet, autorizada pelo humano | Achado de cobrança dupla no `settle()` | Não |
| 2026-10-18 | Origem do comprovante (venda × histórico) derivada da pilha de navegação, sem argumento extra na rota | Argumentos de rota são só IDs; sobrevive à morte do processo | Plano §4 |
| 2026-10-18 | `OpenPurchaseRedirect` (VM por activity, estado + evento de consumo) leva a SCR-01 → SCR-02 se abrir com compra aberta | Fluxo da UI Spec §3 (`A -.-> Recovering`) | Plano §4 |
| 2026-10-18 | `ClockModule` movido de `data/di` para `core:common/di` com `systemDefaultZone` (como o `DispatchersModule`) | Agrupamento por mês (domain) e exibição (UI) no mesmo fuso; `app` agrega o grafo | Não |
| 2026-10-18 | `androidx.navigation.compose` adicionado ao `app` (já no catálogo) | `NavHost` no `app` (architecture.md) | Não |

## Bloqueios e perguntas abertas
- [ ] **Decisão do humano (lote E):** (1) saída do comprovante em `Error` vindo da venda (hoje só o voltar do sistema; recomendação: seta "Voltar para eventos"); (2) ícone do botão "Voltar para eventos" no `Unavailable` (`DsErrorState` fixa `Refresh`; precisa de `actionIcon` no core); (3) registrar `DsStatusMessage` no catálogo e na §5; (4) UI Spec 0.4.3: chaves reais da §4.4 (`ticketing_status_*`/`common_retry` em `strings_common.xml`, `ticketing_history_datetime_today/yesterday/other`, `ticketing_purchase_event_*`, `*_spoken_*`), cabeçalho do snapshot sem "disponíveis", "Data do evento" absoluta, dia da semana por extenso, ícones substitutos, "Tentar de novo" × "Tentar novamente" (spec §6 × UI Spec).
- [ ] Sugestões do lote E: `spokenBody` no `DsStatusMessage` e rótulo falado do retry do `DsErrorState` (valor por extenso); "de 2026" no falado do comprovante; `heading` do resultado do comprovante com `mergeDescendants` (conferir no TalkBack, T25); `Year` do EventList congelado na virada do ano; `HistoryItem.status` já como `SaleResult` no mapper; `OtherDay` carregar `SpokenDateTime`; mover testes de `SpokenDateTime`; previews Font 2x dos Empty; live region do comprovante re-anuncia na rotação (ver origem na T24); sensores propostos: chaves da §4.4 × `strings_*.xml`, `ZoneId.systemDefault`/`Clock.system*` só em `*Route.kt`, `NumberFormat`/`DateTimeFormatter.ofPattern` só em `ui.format`, `UnusedResources` como erro.
- [x] T24: `ClockModule` agora no `app` (`di/`), com `Clock.systemDefaultZone()`; `MainActivity` injeta o `Clock` e o passa a `ticketingGraph` e às Routes (domain e UI no mesmo fuso).
- [x] Spec 1.2.0 e UI Spec 0.4.2 aprovadas pelo humano em 2026-10-18 (o S-09 passa a cobrir AC-25..27).
- [ ] Backlog do lote D: `UuidReferenceGenerator` + teste de formato/unicidade; `toString` mascarado em `Purchase`/`TransactionData`; `purchaseId` (= `reference`) no `SavedStateHandle` enquanto aberta; `recoverPending` não atômico (`findOpen`+`update`, CAS no DAO); `clampQuantity` → `QuantityLimits`; `Loading` do EventList piscando ao reassinar; filtro de mês fora do `SavedStateHandle`; chaves `"eventId"`/`"purchaseId"` ligadas às rotas `:api` na T24 (`toRoute`); feedback visual em "Consultar de novo"; `MonthGroup` duplicado (ui × domain).
- [ ] Sugestões da revisão final (aprovada): trocar o `check()` de `toFailedState` por ramo defensivo (`Error(RecoveryFailed)`) para não derrubar o app; ignorar no `settle` um `Finished` de id diferente da última tentativa (motivo de falha obsoleto, sem risco de dinheiro); teste `Finished(Failed)` + catálogo `Unknown` → `Loading`; contador de tentativas no `LookupFailingGateway`; renomear o teste "prevalece a compra" sem prefixo AC-26.
- [ ] Sensores propostos (harness-improve): Konsist `*UiState` com `@Immutable`; declarações do `ui` `internal`; regra contra `launch{collect}` em `init`; teste de propriedade `Error` × `retry()`; imports não usados em teste; nome de teste da rastreabilidade existir.
- [x] T24: `popUpTo<PurchaseRoute>{inclusive}` no `Completed`; origem do comprovante derivada da pilha (entrada anterior = histórico); use cases já são `@Inject`, o grafo Hilt do app compila.
- [x] **Decisões do humano (T25, 2026-10-18), recomendações aprovadas:** (1) aprovação tardia → BR-29/AC-28, spec 1.3.0, T29 implementada e conferida no AVD (compra `Failed/NotFound` + pagamento concluído depois → `Approved`, log `late_approval`, tela vai ao comprovante); sem aviso ao operador (Q14 aberta); (2) reabrir após venda concluída abre a lista (fora do escopo da spec); (3) callback segue sem `BROWSABLE` (plano §4 e §7 atualizados); (4) AC-25 ajustado (o erro aparece na tela da compra).
- [x] T25 (achados da revisão da T24): processo morto sem estado salvo com a venda aprovada abre na SCR-01 (decisão 2 acima); `OpenPurchaseRedirect` confirmado (compra `Pending` + reabrir → SCR-02 `Recovering` → `Error` "não encontrado" com nova tentativa, com e sem estado salvo); origem do comprovante preservada na rotação; escala de animação 0 navega normalmente. **Não verificado:** flash da SCR-01 antes do redirect (4 quadros sem ver a SCR-01, inconclusivo), fundo escuro sem piscar (o AVD não entrou em modo noturno por shell), duração das transições × 300 ms.
- [ ] T25: TalkBack/`android layout` não expõem `heading`, live region nem papel (o CLI só mostra texto, `content-desc` e interações): `heading` do resultado do comprovante, foco inicial da SCR-02, papel de botão do "Pagar" e live region sem reanúncio após rotação precisam de TalkBack manual. Verificado: rótulo falado "Pagar 80 reais no terminal", alvos de toque ≥ 48dp (132 px a 420 dpi), "Voltar ao histórico" e demais `content-desc`.
- [x] T25: **D1** (corrigido) em fonte 200% a SCR-01 truncava o nome ("Orquestr a Jove…"), contra a UI Spec §SCR-01; `DsListItem` agora empilha o trailing abaixo do texto com fonte ≥ 1,5× (conferido no AVD). O local ainda corta na 2ª linha, como a UI Spec permite.
- [ ] T25: J8 (evento indisponível) e consulta que falha (J7 só cobriu resposta ilegível → consulta → `NotFound`) não são reproduzíveis no emulador sem alterar o catálogo ou a rede; cobertos por testes de ViewModel (AC-25, AC-26).
- [ ] T25: emulador **retém o pedido abandonado** e devolve o `reference` antigo no pagamento seguinte (ver README do data); limpar com `am force-stop br.com.cielosmart.orderservice` entre cenários.
- [x] Spoofing de erro sem `reference`: **risco aceito** pelo humano (plan §7, STRIDE). Reavaliar na T25 se o emulador mostrar outro cenário.
- [ ] Sugestões do lote C: `toString` redigido nos DTOs do encoder (token); limitar tamanho de `id`/`authCode`/`brand` no parser; pagamento com `statusCode 2` na mesma ordem; `DateTimeException` no `guarded`; log do `CatalogUnavailable`; plano §4 `LookupPage` defasado; teste de JSON aninhado profundo.
- [x] T25: callback sem `BROWSABLE` chega (ver decisão 3); `<queries>` só com pacotes resolve a Cielo, mas no Android 10 isso não prova nada (visibilidade de pacotes só existe no 11+: repetir em AVD API 30+ ou terminal real); FGS no Android 10 ok (canal `payment_in_progress`, importância baixa, visibilidade secreta), Android 13+ e tema translúcido sem piscar não verificados.
- [ ] Sugestões T15 abertas: `queryIntentActivities` com escolha pela allowlist; release falhar sem `cielo.*`; sensor de manifest mesclado.
- [x] T25: numeração de página confirmada (`page: 0` → `currentPage: 1`); percent-encoding: `=` literal aceito, `+` e `/` sem amostra; **ordenação do `orders` indeterminada** (lista fixa no emulador): validar em terminal real antes de depender de `pageSize = 5` (BR-15).
- [x] F1 (Importante, compose-ui-reviewer): `DsTopAppBar` com `maxLines = 1` + elipse corta "Histórico de compras" em 200% (compose-ui.md:239). Opções: barra própria que cresce com a fonte ou `expandedHeight` dinâmico.
- [ ] Sugestões abertas da revisão: remover `DsIcons.Lock` (sem uso); registrar `DsStatusMessage` em docs/design/components.md; confirmar ícones do `DsStatusBadge` (Error=Block, Neutral=Cancel) com a prancha; papel RadioButton do chip só verificável no dispositivo.
- [x] T11: API do `DsQuantityStepper` aprovada (UI Spec 0.4.1)
- [ ] Tokens sem `background`/`surfaceTint`/`surfaceDim`/`surfaceBright` (derivados em `Color.kt`); detekt não reconhece `@PreviewLightDark` (`config/`).
- [ ] Conferir na fonte (texto literal) as URIs e JSONs do deep link antes do `plan.md` (ressalva do spike).
- [x] Botão "Erro" do `lio-emulator`: código 2 ("Falha no processo de pagamento"), sem `reference` (README do data).
- [x] Credenciais fictícias aceitas pelo emulador (Q12).

## Log de sessões
| Data | Ferramenta / agente | Resumo |
|---|---|---|
| 2026-10-18 | Claude Code (sessão principal) | Decisões da T25 aprovadas: spec 1.3.0 (BR-29/AC-28), T29 (aprovação tardia), splash nativa e ícone de ingresso |
| 2026-10-18 | Claude Code (sessão principal, `verify-on-device`) | T25 parcial: J1–J7 no AVD Android 10 com `lio-emulator`, fatos do README do data, D1 (fonte 200% da SCR-01) corrigido, `journeys/` criado; decisões do humano acima |
| 2026-10-18 | Claude Code + 5× `android-implementer` + revisores (compose-ui, arch, ui-design) | Lote E (T20 ∥ T21 ∥ T22 ∥ T23) + 2 rodadas de revisão; standard verde |
| 2026-10-17 | Claude Code + `general-purpose` (sonnet) | Spike Cielo → research.md |
| 2026-10-17 | Claude Code (sessão principal) | Spec 0.1.0 → 1.0.0 com 12 perguntas respondidas pelo humano |
| 2026-10-18 | Claude Code + 3× `android-implementer` | Lote B (T09 → T10 ∥ T11), sensores fast verdes |
| 2026-10-18 | Claude Code + revisores (arch, compose, test) + `android-implementer` | review-changes: rodada 1 com 15 achados Importantes → correções (DsIcons, DsStatusMessage, Spacing/Sizes, tabular, a11y); rodada 2: arch e test aprovados, compose com F1 pendente |
| 2026-10-18 | Claude Code + 5× `android-implementer` + revisores (arch, test, compose, security) | Lote D (T16–T19) + correções em 3 rodadas; standard verde |
| 2026-10-18 | Claude Code + 3× `android-implementer` + revisores (arch, test, security) | Lote C (T12 ∥ T13 ∥ T14); T12 bloqueada por detekt → exceção aprovada; correções de teste; standard verde |
