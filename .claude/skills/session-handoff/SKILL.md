---
name: session-handoff
description: Salva ou retoma o estado de trabalho de uma feature em docs/specs/NNN/progress.md para que outra sessão ou outro agente continue sem perder contexto. Use antes de /clear ou /compact, ao encerrar o dia, ao trocar de ferramenta, ou quando o usuário pedir para continuar/retomar uma feature.
argument-hint: "save|resume [docs/specs/NNN-nome]"
---

# Handoff de sessão

O estado vive em arquivos, não na conversa. Fonte: `docs/specs/NNN-nome/progress.md`
(template em `docs/specs/_templates/progress.md`). `tasks.md` continua sendo a lista de tarefas.

## save
1. Atualize no `progress.md`: fase atual, última tarefa concluída, próxima tarefa,
   decisões tomadas na sessão (com motivo), bloqueios/perguntas abertas, resultado do
   último sensor e arquivos em andamento.
2. Acrescente uma linha no **Log de sessões** (data, ferramenta/agente, resumo de 1 linha).
3. Confirme que `tasks.md` reflete o que foi marcado `[x]`.
4. Responda com o caminho do arquivo e a frase para retomar: `/session-handoff resume docs/specs/NNN-nome`.

## resume
1. Leia, nesta ordem: `progress.md`, `tasks.md`, `git status`/`git log -5` (se houver git),
   e apenas os trechos da spec/plano ligados à próxima tarefa.
2. Rode `scripts/harness/verify.sh light` para confirmar que o estado salvo ainda é válido.
3. Resuma em 3 linhas onde parou e qual é o próximo passo; peça confirmação antes de seguir.

Nunca registre segredos, PAN ou PII no `progress.md`.
