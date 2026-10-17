---
status: aceito
date: 2026-10-16
decisores: Rafael Oliveira
---

# ADR-0002: Adotar Harness Engineering para o desenvolvimento com agentes de IA

## Contexto e Problema
O projeto será desenvolvido em grande parte com agentes de IA (Claude Code, Gemini
no Android Studio, Codex, Copilot, Cursor). Sem um ambiente preparado, o agente
repete erros, diverge da arquitetura, inventa convenções e entrega código que só
falha na revisão humana ou em produção. Instruções soltas em prompts não escalam
e não são verificáveis.

Precisamos de um "harness": o conjunto de controles ao redor do modelo que aumenta
a chance de acerto na primeira tentativa e detecta automaticamente o que escapou,
antes de chegar ao revisor humano.

## Fatores de Decisão
- Previsibilidade do agente e redução de retrabalho
- Portabilidade entre ferramentas de IA (não depender de um único fornecedor)
- Verificação determinística sempre que possível (barata, rápida, repetível)
- Requisitos de segurança e conformidade (MASVS, PCI DSS, LGPD)
- Feedback cedo ("shift left"): erro barato é erro detectado no editor

## Opções Consideradas
- Harness em três camadas (guias, sensores, execução), com `AGENTS.md` como fonte canônica
- Instruções apenas em arquivos específicos de cada ferramenta (`CLAUDE.md`, `GEMINI.md`, `.cursorrules`)
- Confiar somente na revisão humana e no CI tradicional

## Decisão
Opção escolhida: "Harness em três camadas com `AGENTS.md` canônico", porque é o
padrão aberto lido pela maioria das ferramentas (Codex, Copilot, Cursor, Gemini no
Android Studio, Claude Code via import), separa claramente o que orienta o agente
(feedforward) do que verifica o resultado (feedback) e permite evoluir o harness
de forma incremental pelo *steering loop*.

| Camada | Função | Onde vive |
|---|---|---|
| Guias (feedforward) | Orientar antes da ação | `AGENTS.md`, `docs/harness/guides/`, skills, templates |
| Sensores (feedback) | Verificar depois da ação | `scripts/harness/`, `config/`, `quality/`, hooks, CI |
| Execução | Ligar o modelo ao mundo real com segurança | `.claude/settings.json`, Android CLI, MCP, sandbox |

Arquivos específicos de ferramenta (`CLAUDE.md`, `.claude/`) apenas importam ou
complementam o `AGENTS.md`; nunca duplicam regras.

### Consequências
- **Positivas:** comportamento consistente entre ferramentas; erros recorrentes
  viram regra verificável; revisão humana concentra-se em decisões, não em estilo.
- **Negativas:** manutenção contínua do harness. Mitigado pelo registro de
  evolução em `docs/harness/steering-loop.md` e pela regra de não duplicação.

### Confirmação
- Constitution, Artigo VIII.
- `scripts/harness/verify.sh` executado em hook local, pre-commit e CI.
- Revisão periódica do `docs/harness/steering-loop.md`.

## Mais Informações
- [Visão geral do harness](../harness/README.md)
- [ADR-0003: Stack de sensores](0003-stack-de-sensores-de-qualidade.md)
- Böckeler, B. *Harness engineering for coding agent users* — martinfowler.com
- Especificação aberta: agents.md
