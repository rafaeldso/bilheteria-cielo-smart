# Pesquisa: design → código com IA (mercado, out/2026)

> Insumo do [ADR-0008](../adr/0008-fase-de-design-com-ia-e-ponte-design-codigo.md). Pesquisa feita em
> 2026-10-17. Ferramentas de IA mudam rápido: revalide preços, limites e status antes de adotar
> algo novo e atualize esta página com a data.

## 1. Panorama geral (software como um todo)
| Tendência | O que é | Evidência |
|---|---|---|
| **Design como contexto para o agente (MCP)** | A ferramenta de design expõe a estrutura da tela (layout, variáveis, componentes, screenshot) para o agente de código, que escreve a primeira versão usando os componentes do projeto. Não é gerador "um clique": o humano revisa. | [Figma MCP server](https://www.figma.com/blog/introducing-figma-mcp-server/) · [docs](https://developers.figma.com/docs/figma-mcp-server/) |
| **Mapeamento componente de design ↔ componente de código** | Figma Code Connect liga um componente do Figma ao componente real (suporta Jetpack Compose), para o código gerado usar a biblioteca do projeto. Recurso de plano pago. | [Code Connect](https://help.figma.com/hc/en-us/articles/23920389749655-Code-Connect) · [Compose](https://developers.figma.com/docs/code-connect/compose) |
| **Geração de UI por IA a partir de texto** | Google Stitch gera telas e protótipos a partir de prompt, exporta HTML/Figma, tem servidor MCP, SDK, skills e o arquivo `DESIGN.md` (regras de design legíveis por agentes). Gratuito no Google Labs, com cota diária. | [Blog Google (mar/2026)](https://blog.google/innovation-and-ai/models-and-research/google-labs/stitch-ai-ui-design/) · [Codelab Stitch MCP](https://codelabs.developers.google.com/design-to-code-with-antigravity-stitch) · [cotas](https://www.banani.co/blog/google-stitch-pricing-and-credits) |
| **Design tokens padronizados** | Formato DTCG (W3C Community Group) chegou à 1ª versão estável **2025.10**: temas, multi-marca, cores modernas, aliases. Style Dictionary v5 lê DTCG e gera Compose. | [Anúncio W3C](https://www.w3.org/community/design-tokens/2025/10/28/design-tokens-specification-reaches-first-stable-version/) · [Format 2025.10](https://www.designtokens.org/) |
| **Open source** | Penpot (alternativa aberta ao Figma) tem servidor MCP oficial, ainda experimental. | [Smashing (jan/2026)](https://smashingmagazine.com/2026/01/penpot-experimenting-mcp-servers-ai-powered-design-workflows/) |

**Consenso:** o gargalo não é "gerar código a partir de imagem", e sim dar ao agente **contexto
estruturado** (tokens, componentes, estados, regras) e um **ciclo de verificação visual**. Resultados
de IA para Compose são bons mas menos consistentes que para React; revisão continua obrigatória
([fonte](https://alexbobes.com/tech/figma-mcp-the-cto-guide-to-design-to-code-in-2026/)).

## 2. Específico de Android / Jetpack Compose
| Ferramenta | Situação | Uso no projeto |
|---|---|---|
| **Gemini no Android Studio: código a partir de imagem** | Oficial. Anexa PNG/esboço → gera Compose + preview; "AI Actions → Match UI to Target Image" ajusta a UI à imagem de referência. Imagem só no tier sem custo. | Opcional ao implementar: a imagem é o mockup aprovado. [Docs](https://developer.android.com/studio/gemini/generate-ui-with-images) |
| **Material Theme Builder** | Oficial, gratuito. Gera esquema de cores M3 e exporta `Color.kt`/`Theme.kt` e tokens. | Fonte dos papéis de cor. [Docs](https://developer.android.com/develop/ui/compose/designsystems/material3) |
| **Roborazzi** | Estável, JVM + Robolectric, captura `@Preview`. | Já adotado (ADR-0003, S-10). Vira o "screenshot do design implementado". |
| **Compose Preview Screenshot Testing (Google)** | **Alpha**; plugin standalone já depreciado em favor de "AGP test suites". | Não adotar: viola a política de versões estáveis (ADR-0007). |
| **Paparazzi** | Estável, mas incompatível com Robolectric e sem interação. | Não adotar: Roborazzi já cobre. |
| **Relay (Figma → Compose)** | Descontinuado em 30/04/2025. | Não usar. [Fonte](https://blogs.perficient.com/2025/03/31/comparing-figma-to-compose-conversion-methods-for-android-development) |
| **Showkase (catálogo de componentes)** | Manutenção incerta em 2026. | Reavaliar quando o design system crescer (exige aprovação). |
| **Android CLI** | `android studio render-compose-preview`, `android screen capture`, `android layout`. | Já no harness: renderiza previews e captura o app para comparar com o mockup. |

## 3. Existe um padrão de "spec de UI"?
Não existe um padrão formal único. O mercado converge para quatro peças, que adotamos:
1. **Tokens** em formato DTCG (o único com especificação estável).
2. **Especificação de componente** no estilo Material: anatomia, variantes, estados, acessibilidade.
3. **Especificação de tela** com todos os estados (loading, vazio, erro, conteúdo), anotações de
   espaçamento/tipo/interação e notas de acessibilidade ([exemplo](https://uxpilot.ai/blogs/design-specifications)).
4. **Regras de design legíveis por agente** (`DESIGN.md`, popularizado pelo Stitch; não é padrão
   aberto com esquema publicado, então usamos as seções dele como convenção, não como contrato).

Protocolos como A2UI/AG-UI descrevem UI gerada em tempo de execução por agentes; não se aplicam à
especificação de telas de um app nativo.

## 4. Ferramentas gratuitas para apresentar telas
| Ferramenta | Custo | Limite relevante | Papel |
|---|---|---|---|
| Prancha HTML do kit ([template](mockup-kit/template.html)) | Grátis | Fidelidade aproximada ao Compose | **Padrão**: o agente gera, você aprova no navegador ou no link privado |
| Google Stitch | Grátis (Labs) | Cota diária; produto experimental, sem SLA | Opcional: explorar direção visual no início do design system |
| Figma Starter + MCP | Grátis | **6 chamadas MCP/mês**; Code Connect só em plano pago | Só se um designer humano entrar no projeto |
| Penpot | Grátis, open source | MCP experimental | Alternativa aberta ao Figma |
| Material Theme Builder | Grátis | — | Gerar/alterar paleta |
| Compose `@Preview` + Roborazzi | Grátis | Exige o projeto Android criado | Fonte da verdade depois de implementado |

Limites do Figma: [planos e acesso](https://developers.figma.com/docs/figma-mcp-server/plans-access-and-permissions/).

## 5. Implicações para este projeto
1. **Design no repositório, não numa ferramenta externa.** UI Spec em Markdown + tokens DTCG +
   pranchas HTML versionadas junto com a spec; nenhuma conta paga necessária.
2. **O agente é o designer; o humano aprova.** O mesmo modelo de papéis do SDD: um agente produz,
   um revisor de IA avalia, o humano decide no checkpoint.
3. **Mesmo vocabulário do design ao código:** IDs `SCR-xx`, nomes de estado iguais ao `UiState`,
   eventos iguais ao `UiEvent`, componentes `Ds*` iguais aos de `core:designsystem`.
4. **Verificação visual fecha o ciclo:** screenshots Roborazzi dos `@Preview` comparados com a
   prancha aprovada.
5. **Adaptadores opcionais** (Figma, Penpot, Stitch) entram por ADR, sem mudar o fluxo.
