---
status: aceito
date: 2026-10-16
decisores: Rafael Oliveira
---

# ADR-0001: Adotar Spec-Driven Development como processo de desenvolvimento

## Contexto e Problema
Precisamos de um processo que garanta que o código entregue corresponda ao que
foi combinado, que seja rastreável para revisão e auditoria e que funcione bem
com assistentes de IA. Sem uma especificação prévia, requisitos ambíguos viram
retrabalho, e decisões técnicas se perdem.

## Fatores de Decisão
- Rastreabilidade entre requisitos, código e testes
- Redução de ambiguidade antes da implementação
- Requisitos de segurança e conformidade (OWASP MASVS, PCI DSS, LGPD)
- Uso de assistentes de IA de forma previsível

## Opções Consideradas
- Spec-Driven Development com artefatos próprios (constitution, spec, plano, tarefas, ADRs)
- Adotar uma ferramenta pronta (GitHub Spec Kit ou Kiro) sem adaptação
- Manter o desenvolvimento orientado apenas por tickets

## Decisão
Opção escolhida: "Spec-Driven Development com artefatos próprios", porque
combina as melhores práticas do Spec Kit (constitution, fases e análise de
consistência), do Kiro (requisitos em EARS) e do MADR (registro de decisões),
adaptadas ao contexto Android e de meios de pagamento, sem prender o projeto a
uma ferramenta.

### Consequências
- **Positivas:** requisitos sem ambiguidade, testes ligados aos critérios de
  aceite, decisões documentadas e segurança considerada desde o plano.
- **Negativas:** custo inicial maior por feature. Mitigado pela regra de
  proporcionalidade: features pequenas usam documentação enxuta.

## Mais Informações
- [Constitution](../constitution.md)
- [Templates de spec, plano e tarefas](../specs/_templates/)
- Instruções do fluxo: `AGENTS.md` na raiz do projeto (importado pelo `CLAUDE.md`)
