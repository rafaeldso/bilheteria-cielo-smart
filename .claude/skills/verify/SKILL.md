---
name: verify
description: Executa os sensores do harness (verify.sh fast/standard/full, rastreabilidade) e interpreta as falhas para corrigi-las. Use antes de declarar qualquer tarefa pronta, quando o hook Stop falhar ou quando o usuário pedir para validar/checar o código.
---

# Verificar

1. Escolha o nível: `fast` durante o trabalho; `standard` para concluir tarefa; `full` para fechar M/G.
2. Rode `scripts/harness/verify.sh <nível>`.
3. Para cada `SENSOR … FALHOU`: leia `Onde`, `Problema`, `Como corrigir`; corrija a causa raiz.
   Para falhas de Gradle, abra o relatório indicado em `build/reports/` ou o log em `build/harness/<nível>/`.
4. Rode de novo até `RESULTADO: OK`.
5. Proibido: `@Suppress`, `@Ignore`, baseline, `-x <task>`, reduzir limites. Se a regra parecer errada,
   pare e explique ao usuário propondo a mudança (e registre em `docs/harness/steering-loop.md`).
6. Relate o resultado real (comando + OK/FALHOU). Nunca diga que passou sem ter rodado.
