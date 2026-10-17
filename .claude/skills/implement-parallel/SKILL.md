---
name: implement-parallel
description: Executa em paralelo um bloco de tarefas [P] do tasks.md, delegando cada uma a um subagente android-implementer em git worktree isolado, e integra o resultado com os sensores. Use quando houver 2+ tarefas [P] independentes na fase atual.
argument-hint: "<docs/specs/NNN-nome> [T08,T09,T10]"
disable-model-invocation: true
---

# Implementar tarefas em paralelo (orchestrator-workers)

Pré-condições: `tasks.md` aprovado, repositório git inicializado, árvore de trabalho limpa
(`git status`). Se alguma faltar, use `implement-task` em sequência.

1. **Selecione o lote:** tarefas `[P]` pendentes da mesma fase (ou as indicadas). Máximo 4.
   Descarte do lote as que tocam os mesmos arquivos ou dependem uma da outra.
2. **Despache** um subagente `android-implementer` por tarefa, todos na mesma mensagem.
   O prompt de cada um deve ser autocontido (subagentes não herdam esta conversa):
   - caminho da spec e ID da tarefa (`T08`), IDs atendidos (`AC-xx`/`BR-xx`) e critério de pronto;
   - arquivos/módulos que pode tocar e os que **não** pode;
   - lembrete: sem commit, push, dependência nova ou edição de `build-logic/`, `config/`, `quality/`.
3. **Integre** cada worktree que voltou com mudanças, uma por vez:
   leia o relatório do subagente, revise o diff (`git diff <branch>`), faça o merge local
   (pede confirmação) e rode `scripts/harness/verify.sh fast`.
   Conflito ou sensor vermelho → resolva antes de integrar o próximo.
4. **Feche:** marque `- [x]` nas tarefas integradas, atualize `progress.md` e informe
   em uma tabela `Tarefa | Resultado | Sensor`.

Se um subagente falhar duas vezes na mesma tarefa, não insista: traga a tarefa para
a sessão principal com `implement-task` e registre a causa no `progress.md`.
