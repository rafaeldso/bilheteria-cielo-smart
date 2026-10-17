# Tarefas: <Nome da Feature>

**Spec:** [spec.md](spec.md) · **Plano:** [plan.md](plan.md)

> Regras:
> - Tarefas em ordem de execução. `[P]` indica que pode rodar em paralelo com
>   as outras `[P]` da mesma fase.
> - Cada tarefa cita os IDs que atende e tem um critério de pronto verificável.
> - Testes antes da UI. Marque `[x]` ao concluir.

## Fase 1 — Domain
- [ ] **T01** Criar entidade `<Nome>` · BR-01 · Pronto quando: compila e não depende do Android
- [ ] **T02** [P] Criar interface `<Nome>Repository` · BR-01 · Pronto quando: contrato igual ao plano
- [ ] **T03** [P] Criar `<Nome>UseCase` · BR-01, BR-02 · Pronto quando: regras implementadas

## Fase 2 — Data
- [ ] **T04** Criar DTOs e mappers · Pronto quando: mapeamento coberto por teste
- [ ] **T05** Implementar `<Nome>RepositoryImpl` com mapeamento de erros · Pronto quando: segue a matriz de erros do plano

## Fase 3 — Presentation
- [ ] **T06** Criar `UiState` e `UiEvent` · Pronto quando: espelham a seção 6 da spec
- [ ] **T07** Implementar `<Nome>ViewModel` · AC-01, AC-02 · Pronto quando: todos os eventos tratados

## Fase 4 — Testes
- [ ] **T08** [P] Testes do use case · AC-01, AC-02
- [ ] **T09** [P] Testes do ViewModel · AC-01, AC-02, AC-03
- [ ] **T10** [P] Testes do repositório · Matriz de erros

## Fase 5 — UI
- [ ] **T11** Criar `<Nome>Screen` stateless e `<Nome>Route` · Pronto quando: todos os estados renderizados
- [ ] **T12** [P] Criar `@Preview` para cada estado
- [ ] **T13** [P] Testes de UI dos fluxos críticos (se exigido pela spec)

## Fase 6 — Fechamento
- [ ] **T14** Rodar build, testes e análise estática
- [ ] **T15** Análise de consistência (spec ↔ plano ↔ tarefas ↔ código)
- [ ] **T16** Atualizar spec/changelog se algo mudou

## Rastreabilidade
| AC | Tarefas | Testes |
|---|---|---|
| AC-01 | T07, T09 | `AC-01 - ...` |
