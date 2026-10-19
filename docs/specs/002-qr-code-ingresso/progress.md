# Progresso: 002 — QR Code do ingresso

> Estado de trabalho entre sessões e agentes (skill `session-handoff`). Atualize ao
> fim de cada fase e antes de `/clear`. A lista de tarefas continua no [tasks.md](tasks.md).
> Nunca registre segredos, PAN ou PII aqui.

**Tamanho:** M (confirmado em 2026-10-19; era P) · **Fase SDD:** 6 Fechar · **Spec:** v1.1.2 (Implementada) · **UI Spec:** v0.2.1 · **Plano:** Aprovado · **Branch:** `feat/002-qr-code-ingresso` (a criar; hoje no trabalho em `docs/add-license`, nada commitado)

## Estado atual
- **Última etapa concluída:** T01–T18 concluídas; `review-changes` (2 rodadas) e `verify.sh full` verdes; spec → Implementada
- **Próxima etapa:** commit e PR pelo autor (`/open-pr`), depois tag e APK novos; validar a impressão em terminal real
- **Arquivos em andamento:** `docs/specs/002-qr-code-ingresso/spec.md`, `progress.md`
- **Último sensor:** `verify.sh full` → verde em 2026-10-19

## Decisões da implementação
| Data | Decisão | Motivo | Afeta spec/plano? |
|---|---|---|---|
| 2026-10-19 | Ações da SCR-03 `Approved` e da SCR-05 em barra inferior fixa; Compartilhar indisponível em `Printing`; `qr.*` iguais nos dois temas | Revisão 1 do `ui-design-reviewer` (I1, I3, I7) | UI Spec 0.1.1 e 001 0.5.0 |
| 2026-10-19 | Os QR Codes são derivados de `reference` + ordem, não persistidos | Sem migração do Room v1; recomendação da Q2 | Spec (BR-30, BR-32) |

## Bloqueios e perguntas abertas
- [x] Q1…Q10 respondidas em 2026-10-19 (recomendações aceitas). Q8: IDs continuam a 001.
- [x] Tamanho M confirmado (impressão e compartilhamento no escopo). Exige plano completo e UI Spec com prancha.
- [x] Q11 e Q12 respondidas (recomendações); Q11 depende do spike do `lio://print` (pendências P1–P4 da UI Spec).
- [x] Spec 1.0.0 aprovada pelo humano (2026-10-19).
- [x] Design aprovado em 2026-10-19 (UI Spec 002 0.1.1 e 001 0.5.0; D1, D2 e D3).
- [x] Plano e mudanças M1–M4 aprovados (2026-10-19): spec 1.1.0, UI Spec 0.2.0.
- [x] `tasks.md` aprovado e T01–T18 concluídas.
- [x] Spec 1.1.2 e UI Spec 0.2.1 aprovadas pelo humano (2026-10-19).
- [ ] Terminal real: impressão por imagem e legibilidade do QR impresso (pendência aceita).
- [x] **R1:** resolvido com ressalva (2026-10-19): `MediaStore` funciona em API 30 quando o app Cielo tem `MANAGE_EXTERNAL_STORAGE`; EACCES sem isso e no Android 10. Pendência aceita: validar em terminal real

## Log de sessões
| Data | Ferramenta / agente | Resumo |
|---|---|---|
| 2026-10-19 | Claude Code (`feature-flow`, `sdd-specify`) | Leitura de constitution, anti-patterns, spec/plan/ui-spec da 001, security.md e ADR-0007/0009; spec 002 em Rascunho e perguntas abertas |
| 2026-10-19 | Claude Code (`sdd-design`) | UI Spec e prancha da 002; edição da 001 (spec 1.3.1, UI Spec 0.5.0); revisão 1 aplicada |
| 2026-10-19 | Claude Code (`sdd-design`) | Design aprovado; spec 1.0.1 (D3); tokens, catálogo e kit atualizados |
| 2026-10-19 | Claude Code (`sdd-plan`) | Plano completo da 002; achados da doc Cielo (PRINT_IMAGE por caminho de arquivo, resposta `message`, colisão de `code` com o pagamento) |
| 2026-10-19 | Claude Code (`sdd-plan`, `sdd-tasks`) | Plano aprovado; spec 1.1.0 (BR-44, AC-44, BR-40), UI Spec 0.2.0 e prancha atualizadas; tasks.md T01–T18 |
| 2026-10-19 | Claude Code + `device-tester` (sonnet) | T01 concluída: spike do `lio://print` em API 29 e 37; research.md |
| 2026-10-19 | Claude Code (`implement-task`) + `device-tester` (sonnet) | T02–T14 implementadas; T16 no AVD API 29 e 30; spec 1.1.1 |
| 2026-10-19 | Claude Code | Docs do case: README (tela, QR Code, limites), READMEs de módulo, ADR-0012, prompts e log de IA no padrão da 001; capturas 06 e 07; tag/APK pendentes (depois do PR) |

