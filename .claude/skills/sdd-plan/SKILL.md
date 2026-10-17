---
name: sdd-plan
description: Gera o plan.md de uma spec aprovada (Fase 3 do SDD), começando pelo Constitution Check, com módulos, diagramas Mermaid, contratos, matriz de erros, segurança MASVS/STRIDE e estratégia de testes. Use após a aprovação da spec.
---

# SDD — Planejar

Pré-condição: spec com status **Aprovada**. Se não estiver, pare e informe.

1. Leia `docs/constitution.md`, a spec e os guias de `docs/harness/guides/` relevantes.
2. Copie `docs/specs/_templates/plan.md` para a pasta da spec.
3. **Constitution Check primeiro** (Artigos I–VIII). Qualquer ❌ → justificativa + proposta de ADR (`create-adr`).
4. Antes de propor componentes, procure no código padrões existentes (`grep`/Glob por `UseCase`, `Repository`, módulos `feature/*`) e reutilize.
5. Preencha: módulos/arquivos (respeitando `architecture.md`), diagramas, contratos
   (assinaturas Kotlin de domain/data/presentation), matriz de erros (`error-handling.md`),
   alternativas, MASVS + STRIDE (`security.md`), observabilidade sem PII, rollout (feature flag?),
   testes por AC (`testing.md`), riscos.
6. Tamanho P: apenas seções 1, 2, 4 e 10.
7. Dependência nova? Liste-a explicitamente como pendente de aprovação.
8. Pare e peça aprovação.
