# Progresso: 001 — Venda de ingressos com pagamento Cielo Smart

> Estado de trabalho entre sessões e agentes (skill `session-handoff`). Atualize ao
> fim de cada fase e antes de `/clear`. A lista de tarefas continua no [tasks.md](tasks.md).
> Nunca registre segredos, PAN ou PII aqui.

**Tamanho:** G · **Fase SDD:** 5 Implementar · **Spec:** v1.1.0 (Aprovada) · **UI Spec:** v0.4.0 (Aprovada) · **Branch:** `main` (pushed)

## Estado atual
- **Última etapa concluída:** Fase 1, lote A (T05–T08): 8 use cases + `QuantityLimits`, 99 testes no domain, cobertura ≥ 90%
- **Próxima etapa:** lote B (T09–T11, design system) via `/implement-parallel`
- **CI:** correções de `security` enviadas; confirmar verde após o próximo push
- **Pendente de código:** aplicar a paleta A em `core:designsystem` (tokens mudam em 3 lugares; a skill de design não escreve código) — T09 (lote B)
- **Arquivos em andamento:** nenhum
- **Último sensor:** `verify.sh full` → verde em 2026-10-18

## Decisões da implementação
| Data | Decisão | Motivo | Afeta spec/plano? |
|---|---|---|---|
| 2026-10-17 | Integração por Deep Link (`lio://`), sem SDK | Política Cielo de camada de abstração obrigatória; SDK Local descontinuado ([research.md](research.md)) | Plano |
| 2026-10-17 | Status interno "Falhou" para falhas técnicas; código 3 = Negada | Q8 opção A; explicação vai em KDoc no mapeamento | Spec (BR-13, BR-14) |
| 2026-10-17 | Disponível = catálogo − compras aprovadas, por aparelho | Q11; limitação nos trade-offs do README | Spec (BR-17) |
| 2026-10-18 | `StartPurchase` devolve a falha do gateway mesmo se o `update` para Failed falhar | A compra que ficar Pending é resolvida pela recuperação (BR-15); revisar no `review-changes` | Não |
| 2026-10-18 | Pedido na listagem com o nosso `reference` e valor diferente → `Failed`/NotFound | Enviamos o valor exato; divergência indica resposta forjada; aprovar abriria fraude | Não (anti-spoofing do plano §7) |
| 2026-10-18 | Teto de 10 (BR-07) ligado ao `StartPurchase` na integração | T05 e T06 rodaram em paralelo sem dependência | Não |

## Bloqueios e perguntas abertas
- [ ] Conferir na fonte (texto literal) as URIs e JSONs do deep link antes do `plan.md` (ressalva do spike).
- [ ] Descobrir qual código o botão "Error" do `lio-emulator` devolve (capturar na implementação; registrar no README).
- [ ] Credenciais: testar valores fictícios no emulador (Q12).

## Log de sessões
| Data | Ferramenta / agente | Resumo |
|---|---|---|
| 2026-10-17 | Claude Code + `general-purpose` (sonnet) | Spike Cielo → research.md |
| 2026-10-17 | Claude Code (sessão principal) | Spec 0.1.0 → 1.0.0 com 12 perguntas respondidas pelo humano |
