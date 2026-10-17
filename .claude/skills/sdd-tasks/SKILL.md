---
name: sdd-tasks
description: Gera o tasks.md (Fase 4 do SDD) com tarefas ordenadas por camada, IDs da spec, critério de pronto verificável, marcador [P] e tabela de rastreabilidade AC → tarefas → testes. Use após a aprovação do plano.
---

# SDD — Quebrar em tarefas

1. Copie `docs/specs/_templates/tasks.md` para a pasta da spec.
2. Ordem: Domain → Data → Presentation → Testes → UI → Fechamento.
3. Cada tarefa: ID `Txx`, IDs atendidos (`BR-xx`, `AC-xx`), critério de pronto que um sensor
   consiga verificar (ex.: "teste `AC-02 - ...` passa", "Konsist verde").
4. `[P]` só para tarefas sem dependência entre si na mesma fase.
5. Tarefas pequenas: cada uma deve caber em um ciclo editar → `verify.sh fast`.
6. Tabela de rastreabilidade: todo AC da spec aparece com tarefas e nome do teste.
7. Inclua a tarefa final: `scripts/harness/verify.sh standard` + `consistency-check`.
8. Pare e peça aprovação.
