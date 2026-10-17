---
name: review-changes
description: Revisão agêntica das mudanças atuais — escolhe os revisores pelo tipo de arquivo alterado, roda-os em paralelo, consolida um relatório único e corrige os achados bloqueantes (no máximo 2 rodadas). Use ao fechar tarefas M/G, antes de PR ou quando o usuário pedir revisão.
argument-hint: "[arquivos | branch base]"
---

# Revisar mudanças (routing + evaluator-optimizer)

1. **Escopo:** `git diff --name-only <base>...HEAD` mais arquivos não commitados
   (sem git: arquivos indicados pelo usuário ou listados no `progress.md`).
2. **Roteamento** — selecione os revisores:

   | Condição no diff | Revisor |
   |---|---|
   | Qualquer `.kt` de produção | `architecture-reviewer` |
   | Testes alterados ou tarefa de teste | `test-reviewer` |
   | `**/ui/**`, `@Composable`, ViewModel | `compose-ui-reviewer` |
   | `**/data/**`, rede, storage, logs, `AndroidManifest.xml`, auth, cripto, dados de cartão/PII | `security-reviewer` |
   | Fechamento de feature (`docs/specs/NNN` com tarefas concluídas) | `spec-consistency-analyzer` |

3. **Execute em paralelo** (uma mensagem, um subagente por revisor). Passe a cada um a
   lista de arquivos, a pasta da spec e o tamanho da mudança.
4. **Consolide** em uma tabela única, sem duplicatas, ordenada por severidade:
   `Severidade | Arquivo:linha | Problema | Revisor | Regra`. Escreva cada problema no formato
   Conventional Comments de `docs/code-review.md` (`issue (blocking): ...`), pronto para virar comentário de PR.
5. **Corrija** os itens *Bloqueante* e *Importante* que estiverem dentro da spec; rode
   `scripts/harness/verify.sh fast`; repita **somente** os revisores que reprovaram.
   Limite: 2 rodadas. O que sobrar vai para o usuário decidir.
6. Sugestões fora da spec não são aplicadas: liste-as para o usuário.
7. Achado que um sensor computacional poderia pegar → proponha `harness-improve`.

Veredito final: **APROVADO** ou **MUDANÇAS NECESSÁRIAS** (com o que falta).
