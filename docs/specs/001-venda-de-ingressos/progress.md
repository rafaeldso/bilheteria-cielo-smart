# Progresso: 001 — Venda de ingressos com pagamento Cielo Smart

> Estado de trabalho entre sessões e agentes (skill `session-handoff`). Atualize ao
> fim de cada fase e antes de `/clear`. A lista de tarefas continua no [tasks.md](tasks.md).
> Nunca registre segredos, PAN ou PII aqui.

**Tamanho:** G · **Fase SDD:** 3 Planejar · **Spec:** v1.1.0 (Aprovada) · **UI Spec:** v0.4.0 (Aprovada) · **Branch:** `main` (pushed)

## Estado atual
- **Última etapa concluída:** Fase 2.5 — design aprovado (UI Spec 0.4.0, 3 rodadas do `ui-design-reviewer`); spec 1.1.0 (navegação por mês, BR-25)
- **Próxima etapa:** aprovação do `tasks.md` → Fase 0 (T01–T04) na sessão principal
- **CI:** `ci`/`release` verdes no GitHub; `security` com correções aplicadas localmente (cache do release, `taskAffinity`, gitleaks `-v`), ainda não enviadas
- **Feito na Fase 3:** URIs/JSONs conferidos na fonte (research §12); `plan.md` escrito
- **Pendente de código:** aplicar a paleta A em `core:designsystem` (tokens mudam em 3 lugares; a skill de design não escreve código) — primeira tarefa de UI do `tasks.md`
- **Arquivos em andamento:** nenhum
- **Último sensor:** não se aplica (só documentos)

## Decisões da implementação
| Data | Decisão | Motivo | Afeta spec/plano? |
|---|---|---|---|
| 2026-10-17 | Integração por Deep Link (`lio://`), sem SDK | Política Cielo de camada de abstração obrigatória; SDK Local descontinuado ([research.md](research.md)) | Plano |
| 2026-10-17 | Status interno "Falhou" para falhas técnicas; código 3 = Negada | Q8 opção A; explicação vai em KDoc no mapeamento | Spec (BR-13, BR-14) |
| 2026-10-17 | Disponível = catálogo − compras aprovadas, por aparelho | Q11; limitação nos trade-offs do README | Spec (BR-17) |

## Bloqueios e perguntas abertas
- [ ] Conferir na fonte (texto literal) as URIs e JSONs do deep link antes do `plan.md` (ressalva do spike).
- [ ] Descobrir qual código o botão "Error" do `lio-emulator` devolve (capturar na implementação; registrar no README).
- [ ] Credenciais: testar valores fictícios no emulador (Q12).

## Log de sessões
| Data | Ferramenta / agente | Resumo |
|---|---|---|
| 2026-10-17 | Claude Code + `general-purpose` (sonnet) | Spike Cielo → research.md |
| 2026-10-17 | Claude Code (sessão principal) | Spec 0.1.0 → 1.0.0 com 12 perguntas respondidas pelo humano |
