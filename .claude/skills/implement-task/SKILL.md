---
name: implement-task
description: Implementa a próxima tarefa pendente do tasks.md de uma feature (Fase 5 do SDD), seguindo templates e guias do projeto e fechando com os sensores do harness. Use quando o usuário pedir para implementar, continuar ou executar tarefas aprovadas.
---

# Implementar tarefa

Pré-condição: spec, plano e tarefas aprovados.

1. Abra `tasks.md` e pegue a primeira tarefa `- [ ]` (ou a indicada pelo usuário).
2. Leia somente o necessário: trecho da spec com os IDs da tarefa, contrato no `plan.md`,
   o guia da camada (`docs/harness/guides/`) e o template correspondente (`docs/harness/templates/kotlin/`).
3. Procure um exemplo já existente no código da mesma natureza e siga o padrão dele.
4. Implemente o mínimo que satisfaz a tarefa. Nada fora da spec.
5. Rode o sensor mais próximo:
   - módulo: `./gradlew :feature:<x>:<camada>:testDebugUnitTest` (ou `:test` para JVM)
   - geral: `scripts/harness/verify.sh fast`
6. Se um sensor falhar: leia a mensagem `Como corrigir`, corrija a causa, rode de novo.
   Nunca suprima, ignore ou crie baseline.
7. Marque `- [x]` na tarefa e informe em 2–3 linhas o que mudou e o resultado do sensor.
8. Se encontrar conflito com a spec: pare e sinalize (Gestão de Mudanças).
