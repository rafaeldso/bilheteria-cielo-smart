<!-- Título em Conventional Commits: feat(balance-ui): show blocked amount (vira o commit do squash).
     Rascunho automático: /summarize change. Guia: CONTRIBUTING.md · Revisão: docs/code-review.md -->

## Resumo
<!-- O que muda e POR QUÊ, em 2–3 linhas. -->

**Spec:** `docs/specs/NNN-...` (versão) · **Tamanho:** Isento / P / M / G
**ACs atendidos:** AC-01, AC-02

## Como revisar
<!-- Ordem de leitura sugerida e onde prestar atenção. Marque o que é mecânico. -->
1. Contratos / testes:
2. Implementação:
- Mecânico (pode passar rápido):

## Riscos e rollback
<!-- O que pode quebrar? Como reverter (revert simples, feature flag)? "Nenhum" é uma resposta válida. -->

## Uso de IA (Constitution Art. VIII.6)
- [ ] Este PR contém código gerado ou alterado por agente de IA
- Ferramenta(s) / agentes:

## Definição de Pronto
- [ ] Uma intenção; ≤ 400 linhas de produção (ou justificativa acima)
- [ ] Todos os ACs implementados e cobertos por testes (`check-traceability.sh` verde)
- [ ] `scripts/harness/verify.sh standard` passando localmente
- [ ] Nenhum sensor enfraquecido (`@Suppress`, `@Ignore`, baseline, `-x`, limites)
- [ ] Nenhuma dependência nova sem aprovação; se houver, `group:artifact` conferido no repositório Maven
- [ ] Constitution respeitada; exceções registradas em ADR
- [ ] Spec e changelog atualizados
- [ ] README do módulo, KDoc e glossário atualizados se a API pública ou o vocabulário mudaram
- [ ] Dados sensíveis revisados (PCI / LGPD): nada em logs, analytics ou fixtures

## Evidências
<!-- Screenshots/gravações para mudanças de UI; saída resumida do verify. -->

## Harness
- [ ] Algum erro recorrente do agente neste PR? Registrado em `docs/harness/steering-loop.md`
