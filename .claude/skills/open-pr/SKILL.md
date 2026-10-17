---
name: open-pr
description: Prepara a entrega — roda o gate de qualidade (verify standard, rastreabilidade, revisão), propõe commits em Conventional Commits e redige o PR pelo template com a declaração de uso de IA. Use somente quando o usuário pedir para commitar, abrir PR ou entregar.
argument-hint: "[docs/specs/NNN-nome]"
disable-model-invocation: true
---

# Abrir PR

1. **Gate** (pare no primeiro vermelho e corrija):
   - `scripts/harness/verify.sh standard` (M/G: `full`)
   - `scripts/harness/check-traceability.sh docs/specs/NNN-nome`
   - skill `review-changes` concluída com **APROVADO** nesta sessão (senão, rode-a)
2. **Commits** (regras em `CONTRIBUTING.md`): agrupe por camada/intenção. Mensagem em inglês, Conventional Commits,
   escopo = módulo (`feat(balance-ui): ...`), corpo citando `AC-xx`. Mostre o plano de
   commits e **peça confirmação** antes de `git commit`.
3. **PR:** gere o rascunho com a skill `summarize change` e preencha `.github/pull_request_template.md` por completo — resumo, spec e versão,
   tamanho, ACs, uso de IA (ferramentas e agentes usados), evidências (saída resumida dos
   sensores; screenshots de `verify-on-device` para UI).
4. **Publicação:** `git push` é bloqueado para o agente. Entregue o texto do PR e o comando
   para o usuário executar (`git push -u origin <branch>` e `gh pr create --fill`), ou
   execute `gh pr create` apenas se o usuário pedir explicitamente.
