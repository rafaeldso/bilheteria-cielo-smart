---
name: sdd-design
description: Fase "Desenhar" do SDD — a partir da spec aprovada, gera a UI Spec (telas SCR-xx, estados, componentes Ds*, textos, acessibilidade) e a prancha HTML com protótipo clicável para aprovação humana; também conduz a escolha da direção visual do design system. Use após aprovar uma spec com UI, quando pedirem esboço/mockup/protótipo de tela, ou ao ajustar um design já aprovado.
argument-hint: "<docs/specs/NNN-nome> | direcao-visual"
---

# SDD — Desenhar (UI/UX)

Você é o designer de UI/UX; o humano aprova. Leia antes: `docs/harness/guides/ui-design.md`,
`docs/design/DESIGN.md`, `docs/design/components.md`. **Não escreva código de produção nesta skill.**

## Modo `direcao-visual` (uma vez, antes da 1ª feature com UI, ou rebranding)
1. Leia a seção 1 de `DESIGN.md` e pergunte o que faltar sobre marca (logo, cor, fonte oficiais).
2. Gere 2–3 direções com cor semente, fonte e forma diferentes, aplicadas à **mesma** tela de
   exemplo do kit: copie `docs/design/mockup-kit/template.html` para
   `docs/design/directions/<a|b|c>.html` e troque só as variáveis `--md-*` e `--font-*`.
3. Apresente lado a lado com uma recomendação. ⏸ Aguarde a escolha.
4. Escolhida: gere a paleta no Material Theme Builder (peça ao humano a exportação, ou derive os papéis
   M3 a partir da semente e diga que são aproximados), atualize `tokens/*.tokens.json`, as variáveis do
   `template.html` e `DESIGN.md` (versão + status). Registre no changelog do `DESIGN.md`.

## Modo feature (`docs/specs/NNN-nome`)
1. **Pré-condição:** `spec.md` com status Aprovada e seção 6 (estados e eventos) completa. Se não,
   pare e volte para `sdd-specify`.
2. **Fidelidade:** P → wireframe; M/G → prancha hi-fi + protótipo (tabela em ui-design.md §2).
3. **UI Spec:** copie `docs/specs/_templates/ui-spec.md` para `docs/specs/NNN-nome/ui-spec.md` e preencha:
   inventário `SCR-xx` → fluxo Mermaid → por tela: estrutura, estados (nomes idênticos ao `UiState`),
   componentes `Ds*`, strings com chave, interações (nomes idênticos ao `UiEvent`), acessibilidade,
   dados sensíveis. Componente ou token novo → ficha na seção 5.
4. **Prancha (M/G):** copie `docs/design/mockup-kit/template.html` para
   `docs/specs/NNN-nome/design/mockups.html`; edite só o bloco DADOS DA FEATURE (`META`, `SCREENS`,
   `BOARDS`, `PROTOTYPE`, `FLOW`). Uma prancha por estado, `refs` com US/BR/AC, notas numeradas.
   Dados fictícios sempre. Atualize o título (`<title>`) para o nome da feature.
5. **Lacuna na spec?** Estado, regra ou evento faltando → pare, proponha a mudança na spec
   (nova versão + changelog) e só continue após aprovação.
6. **Revisão por IA (M/G):** delegue ao subagente `ui-design-reviewer` com o caminho da spec, da UI Spec
   e da prancha. Corrija bloqueantes (máx. 2 rodadas). Registre o veredito na seção 9 da UI Spec.
7. **Checkpoint ⏸:** confira o checklist "Design Ready" (seção 8). Apresente em 3–5 linhas: telas e
   estados, decisões que pedem opinião, pendências. Entregue o caminho da prancha e, se houver a
   ferramenta de artifact, publique-a como link privado (dados fictícios conferidos antes).
8. **Feedback:** ajustes viram nova versão da UI Spec e da prancha (republique no mesmo link).
   Comentários feitos no artifact são pedidos de ajuste do humano.
9. **Aprovado:** registre na seção 9 (versão, data, "Aprovado"), mude o status para Aprovada, marque
   "Design disponível" no Definition of Ready da spec e preencha o link de Design na spec.
   Atualize `progress.md`. Próximo passo: `sdd-plan` (M/G) ou `sdd-tasks` (P).

## Ajuste em design aprovado
Mesmo fluxo a partir do passo 3, editando a UI Spec existente: nova versão, changelog e novo
checkpoint. Se o código já existe, a tarefa de UI regrava o screenshot Roborazzi no mesmo PR.

## Saída ao usuário
2–3 linhas por passo; no checkpoint, a lista de telas × estados e o que precisa de decisão.
