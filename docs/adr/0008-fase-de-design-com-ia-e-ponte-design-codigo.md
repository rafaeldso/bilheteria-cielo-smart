---
status: proposto
date: 2026-10-17
decisores: Tech Lead
consultados: Agente de IA (Claude Code)
informados: Time Android
---

# ADR-0008: Fase de design com IA e ponte design → código Compose

## Contexto e Problema
O SDD do projeto vai da spec (o quê) direto ao plano e ao código. A spec já lista estados de UI e
eventos, mas não diz **como** a tela é: hierarquia, componentes, textos, acessibilidade, comportamento
adaptativo. Sem isso, o agente decide o visual durante a implementação, o humano só vê a tela pronta
e cada ajuste visual custa um ciclo de código, teste e screenshot.

Não há designer dedicado: o papel de design será de um agente de IA, e o Tech Lead aprova os
esboços. A solução precisa ser gratuita, versionada no repositório, legível por qualquer ferramenta
de agente (`AGENTS.md`, Agent Skills) e terminar em código Compose rastreável à spec.

## Fatores de Decisão
- Reduzir o atrito entre esboço aprovado e Compose funcional (mesmo vocabulário, verificação visual).
- Custo zero e sem dependência de conta paga.
- Padrões de mercado onde existem (Material 3, tokens DTCG 2025.10) e convenções onde não existem.
- Aprovação humana simples: ver telas no navegador ou no celular, comentar, aprovar.
- Rastreabilidade `US/BR/AC` → `SCR` → estado → `@Preview` → screenshot.
- Política de versões estáveis (ADR-0007) e stack de sensores (ADR-0003).

## Opções Consideradas
1. **Figma-first:** design no Figma, Dev Mode MCP + Code Connect para o agente gerar Compose.
2. **Stitch-first:** Google Stitch gera telas por prompt; MCP + `DESIGN.md` alimentam o agente.
3. **Design no repositório (design-as-code):** o agente gera UI Spec (Markdown) + prancha HTML com os
   tokens do design system + protótipo clicável; o humano aprova; Compose implementa com o mesmo
   vocabulário e o Roborazzi fecha o ciclo visual.
4. **Direto para `@Preview`:** sem mockup; o agente escreve a `Screen` e o humano aprova screenshots.

## Decisão
Opção escolhida: **3, design no repositório**, com 2 e 1 como adaptadores opcionais, porque é a única
gratuita sem limite prático, versionada junto com a spec, independente de fornecedor e que permite
aprovar antes de escrever código de produção.

1. **Nova fase "Desenhar" no SDD** (entre Especificar e Planejar) para demandas com UI, com
   checkpoint humano. Skill `sdd-design`; revisor `ui-design-reviewer` (somente leitura) antes do humano.
2. **Artefatos por feature:** `docs/specs/NNN/ui-spec.md` (template em `docs/specs/_templates/ui-spec.md`)
   e `docs/specs/NNN/design/mockups.html` (do kit em `docs/design/mockup-kit/`). Fidelidade
   proporcional: wireframe ASCII em P; prancha hi-fi + protótipo em M/G.
3. **Design system como fonte da verdade:** `docs/design/DESIGN.md` (convenção de seções do Stitch),
   `docs/design/tokens/*.tokens.json` (DTCG 2025.10) e `docs/design/components.md` (catálogo `Ds*`).
   Paleta gerada no Material Theme Builder.
4. **Vocabulário comum:** `SCR-xx`; nomes de estado = `UiState`; eventos = `UiEvent`; componentes `Ds*`
   = composables; chaves de texto = `strings.xml`.
5. **Verificação visual:** screenshots Roborazzi dos `@Preview` comparados com a prancha aprovada pelo
   `compose-ui-reviewer`.
6. **Opcionais, sem mudar o fluxo:** Google Stitch para explorar direção visual; Gemini no Android
   Studio ("Match UI to Target Image") para implementar a partir da prancha; Figma/Penpot via MCP só se
   houver designer humano (exige ADR próprio, plano pago para Code Connect).
7. **Não adotar:** Relay (descontinuado em 2025), Compose Preview Screenshot Testing (alpha, plugin
   standalone depreciado), Paparazzi (Roborazzi já cobre).

### Consequências
- **Positivas:** aprovação antes do código; ajustes visuais custam minutos; design versionado e
  revisável em PR; agente implementa com contexto estruturado em vez de interpretar imagem; mesma
  estrutura funciona em Claude Code, Gemini, Codex e Copilot.
- **Negativas:** a prancha HTML aproxima, mas não é idêntica, ao render do Compose (mitigado por
  tokens e componentes mapeados 1:1 e pela comparação com Roborazzi); tokens mantidos em três lugares
  (JSON, kit, Kotlin) até haver geração automática; um passo a mais no fluxo das features com UI.

### Confirmação
- Definition of Ready da spec exige UI Spec aprovada quando houver UI; `feature-flow` inclui o checkpoint.
- `ui-design-reviewer` verifica o checklist "Design Ready" antes da aprovação humana.
- `compose-ui-reviewer` verifica estado ↔ `@Preview` ↔ prancha e uso de `Ds*`/tokens.
- Backlog do steering loop: gerar Kotlin e CSS a partir dos tokens (Style Dictionary v5, exige
  aprovação de dependência) e checar `SCR-xx` no `check-traceability.sh`.

## Prós e Contras das Opções

### 1. Figma-first
- **A favor:** padrão da indústria; MCP oficial; Code Connect suporta Compose; ótimo com designer humano.
- **Contra:** plano gratuito com 6 chamadas MCP/mês; Code Connect só em plano pago; design fora do
  repositório; o agente edita o canvas com menos controle que edita texto.

### 2. Stitch-first
- **A favor:** gratuito; gera telas e protótipos rapidamente; MCP, skills e `DESIGN.md`.
- **Contra:** produto do Google Labs, sem SLA e com cota que muda; não exporta Compose; `DESIGN.md` não
  é padrão aberto com esquema publicado; design fora do repositório.

### 3. Design no repositório
- **A favor:** gratuito; versionado e revisável em PR; o agente controla 100% do artefato; reaproveita
  tokens e catálogo; funciona offline e em qualquer ferramenta.
- **Contra:** fidelidade aproximada; o kit precisa evoluir junto com o design system.

### 4. Direto para `@Preview`
- **A favor:** sem artefato intermediário; o que é aprovado é o código.
- **Contra:** exige o projeto Android pronto; aprovação depois de escrever código; iterar é mais caro.
  Continua válido para ajustes visuais pequenos em telas existentes (screenshot antes/depois).

## Mais Informações
[Pesquisa de mercado](../design/research.md) · [docs/design/](../design/README.md) ·
[guia ui-design.md](../harness/guides/ui-design.md) · [ADR-0003](0003-stack-de-sensores-de-qualidade.md) ·
[ADR-0007](0007-stack-de-bibliotecas-observabilidade-e-politicas-de-qualidade.md)
