# Catálogo de componentes

> Vocabulário comum entre UI Spec, prancha e código. Cada linha é um composable de
> `core:designsystem` (genérico) ou `core:ui` (domínio compartilhado). A UI Spec cita o nome `Ds*`;
> a prancha usa a classe do kit; o código usa o composable. Regras de API e visibilidade:
> [compose-ui.md §5](../harness/guides/compose-ui.md#5-design-system-e-tema).

**Status:** `Planejado` (só no catálogo/kit) → `Implementado` (existe em código, com previews e
screenshot test). Componente novo entra como `Proposto` na seção 5 da UI Spec e só vira `Planejado`
após aprovação.

| Componente | Módulo | Base Material 3 | Classe no kit | Variantes | A11y embutida | Status |
|---|---|---|---|---|---|---|
| `DsTopAppBar` | designsystem | `TopAppBar` | `.topbar` | com/sem navegação | título como heading; ícones com rótulo | Planejado |
| `DsButton` | designsystem | `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton` | `.btn.filled/.tonal/.outlined` | Filled, Tonal, Outlined, Text; com ícone; carregando | 48dp; estado desabilitado/carregando anunciado | Planejado |
| `DsIconButton` | designsystem | `IconButton` | `.icon-btn` | padrão, tonal | `contentDescription` obrigatório | Planejado |
| `DsFilterChip` | designsystem | `FilterChip` | `.chip` | seleção única/múltipla | estado "selecionado"; 48dp | Planejado |
| `DsListItem` | designsystem | `ListItem` | `.item` | 1, 2 ou 3 linhas; com avatar/ícone; com trailing | clicável inteiro com `mergeDescendants` | Planejado |
| `DsSummaryCard` | designsystem | `Card` | `.summary` | — | lido como grupo único | Planejado |
| `DsStatusBadge` | designsystem | — (custom sobre `Surface`) | `.status.ok/.warn/.bad` | sucesso, alerta, erro, neutro | cor + ícone + texto | Planejado |
| `DsKeyValueList` | designsystem | `ListItem` | `.kv` | — | par chave/valor lido junto | Planejado |
| `DsLoadingSkeleton` | designsystem | — | `.sk`, `.sk-row` | lista, card | anuncia "Carregando…"; respeita reduzir animações | Planejado |
| `DsEmptyState` | designsystem | — | `.state-msg` | com/sem ação | título como heading | Planejado |
| `DsErrorState` | designsystem | — | `.state-msg` + `.glyph.err` | com ação de tentar de novo | anunciado como alerta; foco na ação | Planejado |
| `DsQuantityStepper` | designsystem | `IconButton` ×2 + `Text` | `.stepper` | — | grupo rotulado; valor como região viva; botão no limite desabilitado | Planejado (spec 001) |
| `DsProgressIndicator` | designsystem | `LinearProgressIndicator` | `.progress` | indeterminado | `progressbar` com rótulo; estático com reduzir animações | Planejado (spec 001) |
| `DsSnackbar` | designsystem | `Snackbar` | `.snackbar` | com/sem ação | live region educada | Planejado |
| `DsTextField` | designsystem | `OutlinedTextField` | — (adicionar ao kit quando a 1ª spec precisar) | texto, numérico, sensível | erro anunciado; rótulo sempre visível | Planejado |
| `AmountText` | ui | `Text` | `.num` + tipo | tamanhos da escala | leitura "189 reais e 90 centavos" | Planejado |
| `MaskedCardText` | ui | `Text` | — | — | leitura "final 4821"; nunca recebe PAN | Planejado |

## Ficha de componente (para propor um novo)
Copie para a seção 5 da UI Spec:
```markdown
### Ds<Nome> (Proposto)
- **Problema:** por que nenhum componente existente serve
- **Anatomia:** partes (container, ícone, rótulo…) e tokens de cada uma
- **Variantes e estados:** habilitado, desabilitado, pressionado, foco, erro, carregando
- **Comportamento:** gestos, animação, tamanho mínimo/máximo
- **Acessibilidade:** papel, rótulo, estado anunciado, área de toque
- **API proposta:** `@Composable fun DsNome(…, modifier: Modifier = Modifier)`
- **Classe no kit:** `.nome` (adicionar ao template se aprovado)
```
