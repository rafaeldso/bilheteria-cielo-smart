---
name: sdd-specify
description: Cria ou atualiza a spec de uma feature (Fases 1 e 2 do SDD) com histórias INVEST, regras EARS, critérios Gherkin, estados de UI e classificação de dados. Use quando o usuário pedir uma nova feature, tela, fluxo ou mudança de comportamento sem spec aprovada.
---

# SDD — Especificar e Esclarecer

1. Procure spec existente: `ls docs/specs/`. Se houver, atualize-a (nova versão + changelog) em vez de criar outra.
2. Proponha o tamanho (Isento / P / M / G, ver `AGENTS.md`) e aguarde confirmação se houver dúvida.
3. Crie `docs/specs/NNN-nome-kebab/spec.md` copiando `docs/specs/_templates/spec.md`.
   `NNN` = próximo número livre com 3 dígitos. Preencha o campo **Módulo** (`feature/<nome>`).
4. Preencha, nesta ordem:
   - Histórias `US-xx` (INVEST, MoSCoW).
   - Regras `BR-xx` usando um dos 5 padrões EARS; cada BR liga a uma US.
   - Critérios `AC-xx` em Gherkin, cada um com `(BR-xx)` no título. Cubra caminho feliz, erros e bordas.
   - Estados de UI (`sealed interface`) e eventos; todo estado tem um AC.
   - Classificação de dados (PCI / PII / segredo / público) — ver `docs/harness/guides/security.md`.
5. Tudo indefinido vira `[PRECISA ESCLARECIMENTO: pergunta objetiva]`. Não invente regra de negócio.
6. Fase 2: liste as perguntas abertas de forma numerada, curta e com uma recomendação para cada.
7. Só peça aprovação quando o checklist de Definition of Ready estiver completo.

**Não escreva código de produção nesta skill.**
