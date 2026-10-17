---
name: harness-improve
description: Transforma um erro recorrente de agente (apontado em revisão, CI ou pelo usuário) em melhoria do harness — novo guia, regra, template, sensor ou permissão — e registra no steering loop. Use quando o usuário disser que o agente "sempre erra", "de novo" ou pedir para evitar que um erro se repita.
---

# Melhorar o harness

1. Descreva o erro em uma frase e ache um exemplo concreto (arquivo/linha).
2. Consulte `docs/harness/guides/anti-patterns.md`: já existe AP? Se sim, por que não foi detectado?
3. Escolha a alavanca (tabela "Onde corrigir" em `docs/harness/steering-loop.md`).
   Preferência: sensor computacional > template > guia > instrução no AGENTS.md.
4. Implemente a menor mudança efetiva:
   - Regex simples → `scripts/harness/lint-file.sh` (mensagem no contrato SENSOR).
   - Regra de código → `config/detekt/detekt.yml` (`ForbiddenMethodCall`/`ForbiddenImport` com `reason`) ou `quality/detekt-rules`.
   - Regra estrutural → teste Konsist em `quality/architecture-tests`.
   - Ação perigosa → `.claude/settings.json` ou `.claude/hooks/guard-*.sh`.
5. Prove que funciona: o sensor deve falhar no exemplo ruim e passar no correto.
6. Atualize `anti-patterns.md`, `sensors/README.md` (se novo sensor) e adicione linha no registro do steering loop.
7. Mudanças em `scripts/harness/`, `config/`, `quality/` exigem aprovação: apresente o diff e peça confirmação.
