---
name: consistency-check
description: Fase 6 do SDD — cruza spec, plano, tarefas e código; aponta divergências, ACs sem teste, código sem requisito e estados de UI faltando; atualiza status e changelog. Use ao fechar uma feature ou antes de abrir PR.
---

# Análise de consistência

1. Rode `scripts/harness/check-traceability.sh docs/specs/<NNN-feature>`.
2. Para mudanças M/G, delegue ao subagente `spec-consistency-analyzer` (contexto isolado).
3. Verifique manualmente:
   - Cada `BR-xx` implementada onde o plano diz.
   - `UiState`/`UiEvent` idênticos à seção 6 da spec.
   - Matriz de erros do plano ↔ mapeamentos no data ↔ estados na UI.
   - Todas as tarefas `[x]`; nenhuma classe/pública fora do plano.
4. Rode `scripts/harness/verify.sh standard` (ou `full`).
5. Produza uma tabela: Item | Esperado | Encontrado | Ação.
6. Divergência → proponha correção no código **ou** atualização da spec (nunca silenciosa).
7. Sem divergências: atualize status da spec para "Implementada" e o changelog.
