---
status: aceito
date: 2026-10-18
decisores: Rafael Oliveira
consultados: Claude Code
informados:
---

# ADR-0011: Cobertura de linhas do `ui` exclui Compose, previews e código gerado do Hilt

## Contexto e Problema
O limite de cobertura de linhas do `ui` é 85% (testing.md §8, S-11). Os testes de UI Compose e de screenshot de previews
estão fora de foco por decisão do humano, então as telas (`*Screen`, `*Route`, conteúdos, previews e grafo de navegação)
não têm cobertura JVM. No `verify.sh full` da feature 001, `feature:ticketing:ui` ficou em 35,5% (mínimo 85%), embora a
lógica (ViewModels, mapeadores, formatadores) esteja testada. Código Compose roda no JVM só com Robolectric ou emulador, e
as previews não são lógica.

## Opções Consideradas
- Reabrir os testes de UI Compose para chegar a 85%.
- Excluir do Kover as funções `@Composable` e `@Preview` do `ui` e manter o limite de 85% sobre o restante.
- Baixar o limite do `ui`.

## Decisão
Opção escolhida: "excluir do Kover, só nos módulos `:ui`, as funções `@Composable`/`@Preview`, os arquivos `*Previews.kt` e as classes geradas pelo Hilt (`*_Factory`, `*_HiltModules*`, `Hilt_*`, `*_MembersInjector`)", mantendo 85% sobre ViewModels,
mapeadores e formatadores. Aprovada pelo humano em 2026-10-18 (opção b). Baixar o limite fica descartado (Constitution
Art. VIII.3).

### Consequências
- **Positivas:** o limite passa a medir a lógica testável no JVM; o sensor continua exigindo 85% sobre ela.
- **Negativas:** UI deixa de ter número de cobertura; a verificação fica por previews, revisão e Journeys no dispositivo.
  Lógica nova escrita dentro de um `@Composable` escapa da medição: extraia-a para função pura.

### Confirmação
`QualityConventionPlugin` aplica o filtro (`annotatedBy` e `classes`) apenas quando o caminho do módulo termina em `:ui`;
`verify.sh full` roda o `koverVerify`. Reavaliar quando os testes de UI Compose voltarem ao foco.

## Mais Informações
[testing.md](../harness/guides/testing.md) §8 · [tasks.md](../specs/001-venda-de-ingressos/tasks.md) T28
