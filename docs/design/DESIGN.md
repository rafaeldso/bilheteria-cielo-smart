# DESIGN.md

> Regras de design do app, escritas para humanos **e** agentes. As seções seguem a convenção do
> `DESIGN.md` do Google Stitch (Overview, Colors, Typography, Elevation, Components, Do's and Don'ts),
> para poder ser importado/exportado por ferramentas que a entendem. Valores exatos ficam em
> [tokens/](tokens/); este arquivo explica **quando** e **por quê** usar cada um.

| Campo | Valor |
|---|---|
| **Status** | Rascunho: direção visual a aprovar (tokens são placeholder) |
| **Versão** | 0.1.0 |
| **Base** | Material Design 3 + tokens DTCG 2025.10 |

## 1. Overview
- **Produto:** app Android para lojistas acompanharem e operarem vendas com cartão e Pix.
- **Contexto de uso:** balcão, rua, luz forte, uma mão, interrupções frequentes; aparelhos comuns e
  terminais de pagamento Android com tela pequena.
- **Personalidade:** confiável, direto, calmo. Números primeiro; nada decorativo compete com o valor.
- **Identidade de marca:** `[PRECISA ESCLARECIMENTO: há manual de marca oficial (logo, cor primária, fonte)? Se sim, ele substitui as cores e fontes placeholder.]`

## 2. Colors
Só papéis semânticos do Material 3 (`primary`, `surface`, `onSurface`, `error`…) mais as extensões
`success` e `warning`. Valores em `tokens/color.light.tokens.json` e `tokens/color.dark.tokens.json`.

| Papel | Uso |
|---|---|
| `primary` / `onPrimary` | Ação principal da tela (um botão filled por tela, no máximo) |
| `primaryContainer` | Destaque de resumo (ex.: total vendido) |
| `secondaryContainer` | Seleção (chip selecionado), ícones de lista, botão tonal |
| `surface`, `surfaceContainer*` | Fundos e camadas; profundidade por tom, não por sombra |
| `onSurfaceVariant` | Texto de apoio, metadados, ícones não interativos |
| `success*` / `warning*` / `error*` | Status de venda/transação. Sempre com texto ou ícone junto |
| `outlineVariant` | Divisores |

- Tema claro e escuro obrigatórios; cor dinâmica (Material You) **desligada** para manter a identidade.
- Contraste mínimo WCAG AA (4,5:1 texto; 3:1 texto grande e componentes).

## 3. Typography
Escala do Material 3 (`tokens/base.tokens.json` → `typography.*`). Unidade `sp`; nunca tamanho solto.

| Estilo | Uso |
|---|---|
| `displaySmall` | Valor monetário principal da tela |
| `titleLarge` | Título da top app bar |
| `titleMedium` | Títulos de seção e de estados (vazio, erro) |
| `bodyLarge` / `bodyMedium` | Texto de lista e corrido |
| `labelLarge` | Botões, chips, valores em lista |
| `labelMedium` | Status, contadores, rótulos de seção |

- Valores e horários com dígitos tabulares (`fontFeatureSettings = "tnum"`).
- Fonte ampliada até 200% sem cortar conteúdo.

## 4. Layout e espaçamento
- Grade de 4dp; espaçamentos só de `spacing.*` (4, 8, 12, 16, 24, 32, 48).
- Margem lateral 16dp em telas compactas, 24dp em médias; conteúdo limitado a `size.contentMaxWidth`.
- Área de toque mínima 48×48dp (`size.touchTargetMin`).
- Layout adaptativo por `WindowSizeClass`; edge-to-edge sempre.

## 5. Elevation & Depth
- Profundidade por **tom de superfície** (`surfaceContainerLow` → `Highest`), como no Material 3.
- Sombra só em elementos flutuantes (FAB, menus, snackbar). Níveis em `elevation.*`.

## 6. Shapes
| Token | Uso |
|---|---|
| `shape.small` (8dp) | Chips, badges de status |
| `shape.medium` (12dp) | Cards e listas chave-valor |
| `shape.large` (16dp) | Card de resumo |
| `shape.extraLarge` (28dp) | Bottom sheets, diálogos |
| `shape.full` | Botões |

## 7. Components
Catálogo completo em [components.md](components.md). Regra: toda tela usa componentes `Ds*`; um
componente novo é proposto na UI Spec (seção 5) e só existe depois de aprovado.

## 8. Iconografia e movimento
- Ícones **Material Symbols Outlined**, 24dp (18dp dentro de chips/botões). Preenchido só para estado selecionado.
- Durações `motion.duration.*` (150/300/500 ms) e easing `standard`/`emphasized`.
- Animação não essencial respeita "remover animações" do sistema.

## 9. Voz e tom (pt-BR)
- Fale com o lojista na segunda pessoa, frases curtas, verbo no imperativo nas ações ("Tentar de novo", "Compartilhar comprovante").
- Erro diz o que aconteceu e o que fazer, sem código técnico nem culpa ("Não foi possível carregar suas vendas. Verifique a conexão…").
- Valores com `R$` e separadores pt-BR (`R$ 1.250,00`); datas "hoje às 14:32", "ontem", depois `dd/MM`.
- Cartão sempre "final 1234"; nunca exibir PAN, CVV ou trilha.

## 10. Do's and Don'ts
| Faça | Não faça |
|---|---|
| Valor monetário como elemento dominante da tela | Competir com o valor usando ilustrações ou cores fortes |
| Todo estado (loading, vazio, erro, conteúdo) desenhado | Tela em branco ou spinner sem contexto |
| Status com cor **e** texto/ícone | Informação transmitida só por cor |
| Uma ação primária por tela | Dois botões filled lado a lado |
| Skeleton com a geometria do conteúdo | Spinner centralizado para listas |
| Tokens e `Ds*` | `Color(0xFF…)`, `dp`/`sp` mágicos, componente ad hoc |
| Dados fictícios em mockups e previews | Dados reais de lojista, portador ou cartão |
