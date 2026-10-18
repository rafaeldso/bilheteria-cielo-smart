# Progresso: 001 — Venda de ingressos com pagamento Cielo Smart

> Estado de trabalho entre sessões e agentes (skill `session-handoff`). Atualize ao
> fim de cada fase e antes de `/clear`. A lista de tarefas continua no [tasks.md](tasks.md).
> Nunca registre segredos, PAN ou PII aqui.

**Tamanho:** G · **Fase SDD:** 5 Implementar · **Spec:** v1.1.0 (Aprovada) · **UI Spec:** v0.4.1 (Aprovada) · **Branch:** `main` (pushed)

## Estado atual
- **Última etapa concluída:** lote C (T12–T14) commitado (ba3c3d9, 2708982, 62fa628) + correções da revisão (5aef7b1, 585b46b)
- **Próxima etapa:** T15 (decidir antes: `di/` vs módulo Hilt por pacote)
- **CI:** correções de `security` enviadas; confirmar verde após o próximo push
- **Arquivos em andamento:** nenhum
- **Último sensor:** `verify.sh standard` → verde em 2026-10-18 (lote C)

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
| 2026-10-18 | Teto de 10 (BR-07) ligado ao `StartPurchase` na integração | T05 e T06 rodaram em paralelo sem dependência | Não |

## Bloqueios e perguntas abertas
- [x] Spoofing de erro sem `reference`: **risco aceito** pelo humano (plan §7, STRIDE). Reavaliar na T25 se o emulador mostrar outro cenário.
- [ ] Sugestões do lote C: `toString` redigido nos DTOs do encoder (token); limitar tamanho de `id`/`authCode`/`brand` no parser; pagamento com `statusCode 2` na mesma ordem; `DateTimeException` no `guarded`; log do `CatalogUnavailable`; `di/` vs módulo por pacote (decidir antes da T15); plano §4 `LookupPage` defasado; teste de JSON aninhado profundo.
- [ ] T25: confirmar percent-encoding do Base64 na URI e numeração de página da Cielo (`FIRST_CIELO_PAGE`).
- [x] F1 (Importante, compose-ui-reviewer): `DsTopAppBar` com `maxLines = 1` + elipse corta "Histórico de compras" em 200% (compose-ui.md:239). Opções: barra própria que cresce com a fonte ou `expandedHeight` dinâmico.
- [ ] Sugestões abertas da revisão: remover `DsIcons.Lock` (sem uso); registrar `DsStatusMessage` em docs/design/components.md; confirmar ícones do `DsStatusBadge` (Error=Block, Neutral=Cancel) com a prancha; papel RadioButton do chip só verificável no dispositivo.
- [x] T11: API do `DsQuantityStepper` aprovada (UI Spec 0.4.1)
- [ ] Tokens sem `background`/`surfaceTint`/`surfaceDim`/`surfaceBright` (derivados em `Color.kt`); detekt não reconhece `@PreviewLightDark` (`config/`).
- [ ] Conferir na fonte (texto literal) as URIs e JSONs do deep link antes do `plan.md` (ressalva do spike).
- [ ] Descobrir qual código o botão "Error" do `lio-emulator` devolve (capturar na implementação; registrar no README).
- [ ] Credenciais: testar valores fictícios no emulador (Q12).

## Log de sessões
| Data | Ferramenta / agente | Resumo |
|---|---|---|
| 2026-10-17 | Claude Code + `general-purpose` (sonnet) | Spike Cielo → research.md |
| 2026-10-17 | Claude Code (sessão principal) | Spec 0.1.0 → 1.0.0 com 12 perguntas respondidas pelo humano |
| 2026-10-18 | Claude Code + 3× `android-implementer` | Lote B (T09 → T10 ∥ T11), sensores fast verdes |
| 2026-10-18 | Claude Code + revisores (arch, compose, test) + `android-implementer` | review-changes: rodada 1 com 15 achados Importantes → correções (DsIcons, DsStatusMessage, Spacing/Sizes, tabular, a11y); rodada 2: arch e test aprovados, compose com F1 pendente |
| 2026-10-18 | Claude Code + 3× `android-implementer` + revisores (arch, test, security) | Lote C (T12 ∥ T13 ∥ T14); T12 bloqueada por detekt → exceção aprovada; correções de teste; standard verde |
