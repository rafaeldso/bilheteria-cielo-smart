# Guia: Design de UI/UX (agente como designer)

> Fase **Desenhar** do SDD: entre a spec aprovada e o plano. O agente produz a UI Spec e a prancha;
> o `ui-design-reviewer` avalia; o humano aprova. Decisão: [ADR-0008](../../adr/0008-fase-de-design-com-ia-e-ponte-design-codigo.md).
> Fontes: [Material 3](https://m3.material.io/), [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility),
> [Design tokens DTCG 2025.10](https://www.designtokens.org/).
> **Skill:** `sdd-design` · **Verificado por:** `ui-design-reviewer` (antes da aprovação),
> `compose-ui-reviewer` e S-10 Roborazzi (depois de implementado).

## 1. Princípios
1. **A spec manda.** Design representa estados, eventos e regras já especificados. Estado ou regra
   nova descoberta no design → pare e atualize a spec (Gestão de Mudanças) antes de desenhar.
2. **Design system antes de tela.** Só tokens de [tokens/](../../design/tokens/) e componentes de
   [components.md](../../design/components.md). Faltou algo? Proponha na seção 5 da UI Spec.
3. **Todo estado é desenhado.** Loading, Content, Empty, Error e qualquer outro da spec, cada um com
   prancha e nome idêntico ao `UiState`. Nada de tela em branco.
4. **Mesmo vocabulário do design ao código.** `SCR-xx` ↔ `Route`/`Screen`; estado ↔ variante de
   `UiState` ↔ `@Preview`; evento ↔ `UiEvent`; `Ds*` ↔ composable; chave de texto ↔ `strings.xml`.
5. **Material 3 como base, sem reinventar.** Customize por tokens; componente custom só com ficha aprovada.
6. **Barato antes de bonito.** Fidelidade proporcional ao tamanho da mudança (seção 2).

## 2. Fidelidade por tamanho
| Tamanho | Entregável de design | Checkpoint |
|---|---|---|
| Isento | Nenhum (ou screenshot antes/depois no PR, se visual) | — |
| P (mudança visual pequena) | UI Spec enxuta (seções 2, 4.1, 4.2, 4.4, 9) com wireframe ASCII | ⏸ aprovação do wireframe |
| M / G (tela ou fluxo novo) | UI Spec completa + prancha hi-fi + protótipo clicável | `ui-design-reviewer` → ⏸ aprovação |
| Direção visual (uma vez, ou rebranding) | 2–3 pranchas com direções diferentes sobre a mesma tela | ⏸ escolha → tokens regerados |

## 3. Heurísticas que o agente aplica (e o revisor cobra)
| Tema | Regra |
|---|---|
| Hierarquia | Uma informação dominante por tela (normalmente o valor); uma ação primária (filled) |
| Visibilidade do estado | Toda espera tem feedback (skeleton); todo resultado tem confirmação (snackbar, mudança visível) |
| Prevenção de erro | Ação destrutiva ou financeira irreversível pede confirmação com o valor na frase |
| Recuperação | Erro diz o que houve e o que fazer, com ação para resolver ("Tentar de novo") |
| Reconhecer, não lembrar | Filtros e seleções visíveis; nada de estado escondido em menu |
| Consistência | Mesma ação, mesmo componente, mesmo lugar em todas as telas |
| Ergonomia | Ações frequentes na metade inferior, alcançáveis com o polegar; toque ≥ 48dp |
| Leitura sob luz forte | Contraste AA no mínimo; nada essencial em `outline` ou cinza claro |
| Densidade | Lista de transações em `ListItem` de 2 linhas (72dp); valor alinhado à direita, dígitos tabulares |

## 4. Layout
- Grade de 4dp; margens de 16dp (compact) e 24dp (medium/expanded); conteúdo com largura máxima.
- Desenhe primeiro em **compact (360×800dp)**. Para medium/expanded, descreva o padrão canônico
  (list-detail, supporting pane, `NavigationSuiteScaffold`) na seção 4.1; prancha extra só se a spec pedir tablet.
- Edge-to-edge: barra de status sobre a superfície; nada interativo sob a barra de gestos.
- Teclado: formulários mostram a ação principal acima do teclado (`imePadding`).

## 5. Conteúdo (UX writing pt-BR)
Regras de voz em [DESIGN.md §9](../../design/DESIGN.md#9-voz-e-tom-pt-br). Além disso:
- Todo texto tem chave de recurso na seção 4.4 da UI Spec, no formato `<feature>_<tela>_<elemento>`.
- Botões dizem o que acontece ("Compartilhar comprovante"), nunca "OK"/"Confirmar" sozinhos.
- Plurais e números formatados pelo locale (`plurals`, formatadores de `core:ui`), nunca concatenados.
- Escreva o texto final na prancha. Lorem ipsum é proibido: o texto faz parte do que é aprovado.

## 6. Acessibilidade (anotada no design, não descoberta no código)
Cada tela documenta na seção 4.6: ordem de leitura, rótulos falados (principalmente valores e
cartão mascarado), headings, o que é anunciado ao mudar de estado, para onde vai o foco e o
comportamento com fonte 200%. Regras de implementação: [compose-ui.md §10](compose-ui.md#10-acessibilidade-requisito-constitution-art-vi2).
A prancha precisa ser legível com o controle "Fonte 200%" e nos dois temas.

## 7. Dados sensíveis
- Prancha, protótipo e previews usam **somente dados fictícios** (valores, NSU, nomes, final de cartão).
- Cartão aparece só como "final 1234"/"•••• 1234". Telas com dado de cartão marcadas com `FLAG_SECURE` na seção 4.7.
- Nunca publique uma prancha com dado real, nem como link privado.

## 8. Prancha (mockup-kit)
1. Copie [template.html](../../design/mockup-kit/template.html) para `docs/specs/NNN-nome/design/mockups.html`.
2. Edite só o bloco **DADOS DA FEATURE**: `META`, `SCREENS` (funções estado → HTML), `BOARDS`
   (uma prancha por estado, com `refs` de US/BR/AC e notas numeradas) e `PROTOTYPE` (eventos).
3. Use só as classes do kit e variáveis `--md-*`. Componente novo aprovado → acrescente a classe ao
   template (não só à cópia), para as próximas features.
4. Uma nota numerada por decisão relevante: componente, tokens, a11y, dado sensível.
5. Antes do checkpoint: confira nos dois temas, fonte 200% e os cenários do protótipo.
6. Apresente ao humano: caminho do arquivo e, se ele pedir ou já usar, link privado do artifact.
   A versão aprovada fica congelada; ajustes posteriores geram nova versão na seção 9 da UI Spec.

## 9. Do design ao código
| UI Spec / prancha | Código Compose | Quem garante |
|---|---|---|
| `SCR-xx` | `<Nome>Route` + `<Nome>Screen` | `compose-screen` |
| Estado (`Loading`, `Content`…) | Variante do `UiState` + ramo do `when` + `@Preview` com o mesmo nome | `compose-screen`, `compose-ui-reviewer` |
| `UiEvent` da tabela 4.5 | `UiEvent` e `onEvent` | `compose-screen`, testes de UI |
| `Ds*` da tabela 4.3 | Composable de `core:designsystem`/`core:ui` | `compose-ui-reviewer` |
| Chave da tabela 4.4 | `strings.xml` | Android Lint, `compose-ui-reviewer` |
| Notas de a11y 4.6 | Semântica, foco, live regions | Testes de UI por semântica, `verify-on-device` |
| Prancha aprovada | Screenshot Roborazzi do `@Preview` | `compose-ui-reviewer` compara prancha × screenshot |

Ao implementar, o agente pode usar a prancha como imagem de referência (Gemini no Android Studio:
"Match UI to Target Image"), mas a fonte da verdade continua sendo a UI Spec + tokens + componentes.
Divergência visual intencional entre prancha e Compose exige nova versão da UI Spec.

## 10. Checklist do agente antes de pedir aprovação
É a seção 8 ("Design Ready") do template da UI Spec. Não peça aprovação com item aberto.
