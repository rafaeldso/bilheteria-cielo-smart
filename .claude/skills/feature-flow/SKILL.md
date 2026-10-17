---
name: feature-flow
description: Orquestra uma demanda de ponta a ponta — classifica o tamanho (Isento/P/M/G), escolhe o pipeline, encadeia as skills do SDD com checkpoints humanos e mantém o progress.md. Use quando o usuário trouxer uma demanda nova ("quero a tela X", "implemente Y") ou pedir para continuar uma feature.
argument-hint: "<demanda ou docs/specs/NNN-nome>"
---

# Orquestrar feature

Você é o **orquestrador**: decide o caminho e delega; não pula checkpoints.
Pipelines e papéis: [docs/harness/workflows/README.md](../../../docs/harness/workflows/README.md).

## 1. Situar
- Argumento é uma pasta de spec? Rode a skill `session-handoff` em modo **resume** e siga do ponto salvo.
- Senão, procure spec existente em `docs/specs/` que cubra a demanda (Gestão de Mudanças se houver conflito).

## 2. Classificar (roteamento)
Proponha o tamanho com uma linha de justificativa (tabela "Escopo e tamanho" do `AGENTS.md`)
e **peça confirmação**. Depois siga o pipeline:

| Tamanho | Pipeline |
|---|---|
| Bug | skill `fix-bug` (encerra aqui) |
| Isento | implementar → `verify` (standard) → informar que o SDD foi pulado e por quê |
| P | `sdd-specify` (enxuta) ⏸ → [`sdd-design` wireframe ⏸] → `sdd-tasks` (plano só seções 1, 2, 4, 10) ⏸ → Implementar → Fechar |
| M / G | `sdd-specify` ⏸ → [`sdd-design` prancha + protótipo ⏸] → `sdd-plan` ⏸ → `sdd-tasks` ⏸ → Implementar → Fechar |

⏸ = checkpoint: pare e peça aprovação explícita. Nunca encadeie duas fases sem ela.
[ ] = só quando a demanda tem UI nova ou mudança visual. Antes da 1ª feature com UI, rode
`sdd-design direcao-visual` se o `docs/design/DESIGN.md` ainda estiver como Rascunho.

## 3. Implementar
- Crie `docs/specs/NNN-nome/progress.md` a partir de `docs/specs/_templates/progress.md`.
- Para cada fase do `tasks.md`:
  - Tarefas sequenciais → skill `implement-task`, uma por vez.
  - Bloco de 2+ tarefas `[P]` independentes (arquivos disjuntos) e repositório git → skill `implement-parallel`.
  - UI → skill `compose-screen` (a partir da `ui-spec.md` aprovada); testes → `write-tests`.
- Ao fim de cada fase: `scripts/harness/verify.sh fast`, atualize `progress.md`.
- Contexto passou de ~60% ou a sessão vai acabar → `session-handoff` (save) e sugira `/clear`.

## 4. Fechar
1. Skill `review-changes` (revisores em paralelo, corrige bloqueantes).
2. Skill `consistency-check`.
3. Mudança de UI → skill `verify-on-device` (quando houver emulador).
4. `scripts/harness/verify.sh standard` (M/G: `full`).
5. Ofereça a skill `open-pr`. Não faça commit nem PR sem pedido.

## Regras
- Uma feature por sessão; entre features, `/clear`.
- Saída de cada passo ao usuário: 2–3 linhas (o que foi feito, sensor, próximo passo).
