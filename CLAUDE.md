# CLAUDE.md

@AGENTS.md

## Específico do Claude Code
As regras do projeto estão no `AGENTS.md` (importado acima). Este arquivo contém
apenas o que é exclusivo do Claude Code. Não duplique regras aqui.

### Harness ativo (`.claude/`)
| Recurso | O que faz |
|---|---|
| `settings.json` → permissões | Libera comandos de leitura/build/teste; pede confirmação para mudar sensores e build; bloqueia segredos |
| Hook `PreToolUse` (Bash) | Bloqueia comandos destrutivos e push/publicação |
| Hook `PreToolUse` (Edit/Write) | Bloqueia edição de segredos e de arquivos gerados |
| Hook `PostToolUse` (Edit/Write) | Roda o sensor rápido no arquivo editado e devolve violações para correção |
| Hook `Stop` | Se houve mudança em Kotlin, roda `verify.sh fast` antes de encerrar o turno (nível ajustável por `HARNESS_STOP_LEVEL`, ver [gradle.md](docs/harness/guides/gradle.md#desempenho-no-dia-a-dia)) |

Quando um hook devolver uma mensagem `SENSOR`, corrija a causa e não contorne o hook.

### Skills (`.claude/skills/`)
| Skill | Use quando |
|---|---|
| `sdd-specify` | Criar ou atualizar uma spec (Fases 1 e 2) |
| `sdd-design` | UI Spec + prancha/protótipo das telas para aprovação; direção visual do design system (Fase 2.5) |
| `sdd-plan` | Gerar `plan.md` com Constitution Check (Fase 3) |
| `sdd-tasks` | Gerar `tasks.md` com rastreabilidade (Fase 4) |
| `implement-task` | Implementar a próxima tarefa do `tasks.md` (Fase 5) |
| `android-feature-scaffold` | Criar os módulos de uma feature nova |
| `compose-screen` | Criar Route/Screen/Previews de um estado da spec |
| `write-tests` | Escrever testes ligados a ACs |
| `verify` | Rodar os sensores e interpretar falhas |
| `consistency-check` | Fase 6: spec ↔ plano ↔ tarefas ↔ código |
| `create-adr` | Registrar uma decisão de arquitetura |
| `harness-improve` | Transformar um erro recorrente em guia ou sensor |
| `feature-flow` | **Entrada de toda demanda**: classifica o tamanho e orquestra o pipeline |
| `implement-parallel` | Tarefas `[P]` em paralelo com workers em worktrees (só manual) |
| `review-changes` | Revisores roteados pelo diff, em paralelo, com rodada de correção |
| `security-scan` | Scanners de segurança + `security-reviewer`, triagem e correção (mudança sensível) |
| `fix-bug` | Bug: teste que falha → causa raiz → correção mínima |
| `session-handoff` | Salvar/retomar estado em `progress.md` (antes de `/clear`) |
| `verify-on-device` | Verificar no emulador com Android CLI (telas, a11y, Journeys) |
| `open-pr` | Gate de qualidade, commits e PR (só manual) |
| `onboarding` | Tour do repositório, "onde mudo X?", fluxo de feature, termo do domínio (somente leitura) |
| `summarize` | README de módulo, explicação de fluxo, descrição de PR com ordem de revisão |

As skills ficam em `.claude/skills/` e são expostas a outras ferramentas por
`.agents/skills` (link simbólico). Skills oficiais do Android CLI (`android skills add …`)
complementam estas; ver [docs/harness/execution/README.md](docs/harness/execution/README.md).

### Regras por caminho (`.claude/rules/`)
Carregadas quando o agente lê arquivos da camada (domain, data, ui, testes, Gradle, specs);
apenas apontam para o guia correspondente. Não coloque regras novas nelas: a regra vai no guia.

### Subagentes (`.claude/agents/`)
| Papel | Agentes | Escreve? |
|---|---|---|
| Revisores | `architecture-reviewer`, `security-reviewer`, `test-reviewer`, `compose-ui-reviewer`, `ui-design-reviewer`, `spec-consistency-analyzer` | Não |
| Worker | `android-implementer` (worktree isolado, uma tarefa) | Sim |
| Especialistas | `build-doctor` (falhas de build), `device-tester` (emulador) | Não |

Use revisores ao fechar mudanças M/G (via `review-changes`); especialistas sempre que a
saída for volumosa. Subagentes não herdam esta conversa: o prompt de delegação leva spec,
IDs, escopo de arquivos e proibições. Detalhes: [workflows/README.md](docs/harness/workflows/README.md).
