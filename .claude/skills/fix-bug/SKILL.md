---
name: fix-bug
description: Corrige um bug com disciplina — reproduz com um teste que falha, localiza a causa raiz, aplica a correção mínima e prova com sensores; decide se é Isento (restaura comportamento da spec) ou exige mudança de spec. Use quando o usuário relatar bug, crash, regressão ou teste quebrando.
argument-hint: "<descrição, stack trace ou issue>"
---

# Corrigir bug (reproduzir → provar → corrigir)

1. **Entender:** comportamento esperado vs. observado. Ache a regra (`BR-xx`/`AC-xx`) na
   spec. Sem regra que defina o esperado → é mudança de comportamento: pare e use `feature-flow`.
2. **Reproduzir com teste** no menor nível possível (use case < ViewModel < repositório < UI).
   Nome: `` `AC-xx - regressao <resumo>` `` (ou `BUG-<id>` se não houver AC). Rode e
   **confirme que falha pelo motivo certo**. Não conseguiu reproduzir → informe e peça dados.
3. **Causa raiz:** saída longa de build/teste/logcat → delegue ao subagente `build-doctor`.
   Crash em device → `android` CLI + logcat filtrado (nunca exponha PAN/PII da saída).
4. **Corrija o mínimo** que faz o teste passar. Sem refatoração oportunista.
5. **Prove:** teste do passo 2 verde; `scripts/harness/verify.sh standard`.
6. **Feche:** informe causa, correção e teste (3 linhas). Se o bug foi introduzido por
   agente e a causa é sistêmica → skill `harness-improve`.

Proibido: `@Ignore`/desativar o teste que falha, `try/catch` que engole a exceção, "corrigir" o teste em vez do código.
