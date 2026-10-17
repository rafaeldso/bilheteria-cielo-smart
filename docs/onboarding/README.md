# Trilha de onboarding

> Objetivo: **primeiro PR na 1ª semana** e autonomia em uma tarefa P no 1º mês.
> A IA acelera a leitura; o buddy e os testes confirmam o entendimento.

## Antes do 1º dia (gestor/buddy)
- [ ] Acesso ao repositório, CI e board; pessoa adicionada ao time no `CODEOWNERS`, se for o caso
- [ ] Buddy definido (pessoa do time, não o gestor)
- [ ] Uma tarefa **Isenta** reservada para o primeiro PR (typo, string, teste faltando, doc)

## Dia 1 — Entender o mapa
- [ ] Ambiente: seção *Rodando localmente* do [README](../../README.md); `scripts/harness/verify.sh fast` verde
- [ ] Ler: [ARCHITECTURE.md](../../ARCHITECTURE.md) → [constitution.md](../constitution.md) → [glossário](glossary.md)
- [ ] Abrir o agente na raiz e rodar `/onboarding tour`: tour guiado do repositório real
- [ ] Rodar o app no emulador e navegar pelas telas existentes

## Semana 1 — Seguir um fio de ponta a ponta
- [ ] Escolher uma feature pronta e ler, nesta ordem: `spec.md` → `plan.md` → `README.md` dos
      módulos → testes → código. Use `/onboarding flow <feature>` como guia
- [ ] Explicar a feature ao buddy em 5 minutos, sem olhar o código (teste de entendimento)
- [ ] Ler [CONTRIBUTING.md](../../CONTRIBUTING.md) e [code-review.md](../code-review.md)
- [ ] **Primeiro PR** com a tarefa Isenta reservada
- [ ] Acompanhar 2 reviews do buddy

## Semanas 2–4 — Contribuir com o fluxo completo
- [ ] Uma tarefa P via `/feature-flow`, com o buddy aprovando os checkpoints
- [ ] Revisar 3 PRs usando Conventional Comments
- [ ] Corrigir na fonte uma lacuna de documentação que você encontrou (ver abaixo)
- [ ] Retro de 15 min com o buddy: o que faltou na trilha?

## Perguntando para a IA
`/onboarding` responde com base no código atual e cita arquivos. Perguntas que rendem:

| Você quer… | Pergunte |
|---|---|
| Achar onde mudar | `/onboarding where "mostrar a data da última venda no extrato"` |
| Entender um fluxo | `/onboarding flow pagamento-pix` |
| Entender um termo | `/onboarding explain "antecipação"` |
| Ver o impacto | "O que quebra se eu mudar o contrato de `BalanceRepository`?" |
| Resumir um módulo | `/summarize module feature/balance/data` |

**Confie, mas confirme:** abra os arquivos citados. Se a resposta não citar arquivo, ou se o
teste disser outra coisa, o teste está certo.

## Achou uma lacuna? Conserte a fonte
Sua confusão de hoje é a do próximo colega. Se uma dúvida não foi respondida pela documentação
(ou o `/onboarding` precisou adivinhar), abra um PR `docs(...)` no nível certo — a tabela de
níveis está no [guia de documentação](../harness/guides/documentation.md#os-níveis).

## Métricas da trilha
| Métrica | Meta | Referência |
|---|---|---|
| Tempo até o 1º PR mergeado | ≤ 5 dias úteis | — |
| Tempo até o 10º PR mergeado | Acompanhar a tendência por turma | Indicador de ramp-up da pesquisa DX |
| Lacunas corrigidas por pessoa nova | ≥ 1 | Saúde da documentação |
| Nota da retro (1–5) | ≥ 4 | — |
