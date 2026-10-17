---
status: aceito
date: 2026-10-16
decisores: Rafael Oliveira
consultados:
informados:
---

# ADR-0004: Adotar workflows agênticos orquestrados, com papéis separados e skills portáveis

## Contexto e Problema
O ADR-0002 estabeleceu o harness (guias, sensores, execução) e o ADR-0001, o SDD. Faltava
definir **como** o trabalho flui entre agentes: hoje a sessão principal faz tudo em série,
existem apenas subagentes revisores, o estado de uma feature depende da memória da
conversa e as skills ficam em um diretório lido só pelo Claude Code.

Em 2026 o mercado convergiu para: instruções em `AGENTS.md`; skills no padrão aberto
*Agent Skills* (`SKILL.md`) em `.agents/skills/` (Android Studio, Codex, Gemini CLI,
Copilot); padrões de orquestração descritos pela Anthropic (*chaining, routing,
parallelization, orchestrator-workers, evaluator-optimizer*); subagentes com contexto
isolado e worktrees para paralelismo; estado em arquivos para sessões longas; e o
Android CLI 1.0 (docs, `android studio`, Journeys, skills oficiais) como ponte entre
agentes e o toolchain Android.

## Fatores de Decisão
- Produtividade sem perda de qualidade (paralelismo apenas onde é seguro)
- Separação de responsabilidades: quem implementa não aprova
- Portabilidade entre ferramentas de IA, sem duplicar conteúdo
- Previsibilidade e checkpoints humanos (constitution Art. I e VIII)
- Custo de tokens e de contexto

## Opções Consideradas
- Workflows orquestrados por skills na sessão principal, com workers/especialistas/revisores como subagentes
- Agente único autônomo, sem pipelines (apenas `AGENTS.md` e sensores)
- Framework multiagente externo com papéis fixos (ex.: BMAD) ou IDE dedicada (ex.: Kiro)

## Decisão
Opção escolhida: "Workflows orquestrados por skills", porque reaproveita o SDD e os
sensores existentes, mantém o humano nos checkpoints e adiciona paralelismo e
isolamento de contexto apenas onde trazem ganho.

Componentes (detalhes em [workflows/README.md](../harness/workflows/README.md)):
| Componente | Itens |
|---|---|
| Orquestração | skill `feature-flow` (roteamento por tamanho e pipeline com checkpoints) |
| Paralelismo | skill `implement-parallel` + subagente `android-implementer` (`isolation: worktree`) |
| Avaliação | skill `review-changes` (revisores em paralelo, máx. 2 rodadas de correção) |
| Especialistas | subagentes `build-doctor` e `device-tester` (saída volumosa fora do contexto principal) |
| Fluxos | skills `fix-bug`, `session-handoff`, `open-pr`, `verify-on-device` |
| Estado | `docs/specs/NNN/progress.md` (template em `docs/specs/_templates/`) |
| Contexto | `.claude/rules/*.md` com `paths:` apontando para o guia da camada |
| Portabilidade | `.agents/skills` → link para `.claude/skills`; `.gemini/settings.json` lê `AGENTS.md` |
| CI | `.github/workflows/ai-review.yml`, acionado pelo rótulo `ai-review` |
| Avaliação do harness | [workflows/scenarios.md](../harness/workflows/scenarios.md) (EV-01…EV-10) |

### Consequências
- **Positivas:** tarefas `[P]` executadas em paralelo; revisão sistemática e roteada;
  retomada de trabalho entre sessões e ferramentas; mesma base de skills para todo o time
  independentemente da IDE/agente; custo controlado por modelo e por gatilho no CI.
- **Negativas:** mais artefatos para manter (mitigado pelo steering loop e pelo `selftest.sh`,
  que valida o frontmatter); paralelismo exige git e disciplina de integração; o link
  simbólico precisa ser preservado em clones (git armazena symlinks; no Windows exige
  `core.symlinks=true`); a revisão no CI consome créditos de API.

### Confirmação
- `scripts/harness/selftest.sh` valida frontmatter de skills e agentes e o link `.agents/skills`.
- Cenários EV-01…EV-10 executados após mudanças no harness ou troca de modelo.
- Métricas em [workflows/README.md](../harness/workflows/README.md#medição).

## Prós e Contras das Opções

### Workflows orquestrados por skills
- **A favor:** incremental sobre o que existe; checkpoints explícitos; portável (SKILL.md).
- **Contra:** orquestração depende de o modelo seguir a skill (mitigado por sensores e cenários).

### Agente único autônomo
- **A favor:** zero manutenção adicional.
- **Contra:** sem paralelismo, contexto saturado por logs, autoavaliação do próprio trabalho.

### Framework/IDE externo
- **A favor:** papéis prontos.
- **Contra:** dependência de ferramenta, sobreposição com o SDD já adotado, menos controle de segurança (PCI/LGPD).

## Mais Informações
- [ADR-0001](0001-adotar-spec-driven-development.md), [ADR-0002](0002-adotar-harness-engineering-para-agentes-de-ia.md)
- Anthropic — *Building effective agents*; *Effective harnesses for long-running agents*
- agentskills.io (padrão Agent Skills) · agents.md
- developer.android.com/tools/agents (Android CLI, skills, Journeys)
