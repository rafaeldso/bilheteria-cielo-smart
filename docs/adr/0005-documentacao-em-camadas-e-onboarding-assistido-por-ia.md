---
status: proposto
date: 2026-10-16
decisores: Rafael Oliveira
consultados:
informados:
---

# ADR-0005: Documentação em camadas, code summarization ancorado e onboarding assistido por IA

## Contexto e Problema
O harness (ADR-0002/0004) explica o projeto muito bem para **agentes**, mas não havia porta
de entrada para **pessoas**: sem `README`, sem mapa de arquitetura curto, sem trilha de
onboarding, sem padrão de documentação de módulo e sem guia de code review. Commits e PRs
tinham só um hook de formato e um template.

O mercado em 2026 aponta em duas direções. De um lado, wikis geradas por IA a partir do
repositório (Google Code Wiki, DeepWiki) e empacotadores de contexto (Repomix), que exigem
enviar o código a terceiros, inviável sob PCI DSS sem aprovação de segurança. De outro,
documentação como código em camadas: Diátaxis, `ARCHITECTURE.md` curto (matklad), README por
módulo com grafo gerado (Now in Android), KDoc/Dokka para referência, Conventional Commits,
Conventional Comments e as práticas de review do Google. A pesquisa DX mede ramp-up por
*tempo até o 10º PR* e observa queda expressiva com uso diário de IA.

## Fatores de Decisão
- Pessoa nova produtiva rápido, sem depender de um sênior para cada dúvida
- Documentação que não apodrece: gerada quando possível, revisada quando escrita
- Confidencialidade do código (PCI DSS, LGPD)
- Fonte única (Constitution Art. VIII.1) e proporcionalidade (Art. VII.3)
- Mesmos artefatos úteis para humanos e agentes

## Opções Consideradas
- Documentação em camadas no repositório + skills locais de resumo e onboarding
- Wiki gerada por serviço externo de IA (Code Wiki, DeepWiki)
- Wiki manual fora do repositório (Confluence/Notion)

## Decisão
Opção escolhida: "Documentação em camadas no repositório + skills locais", porque mantém o
código dentro do perímetro aprovado, reaproveita o harness e trata documentação como código
(versionada, revisada, verificável).

| Componente | Itens |
|---|---|
| Entrada humana | `README.md`, `ARCHITECTURE.md`, `CONTRIBUTING.md` na raiz |
| Padrão de summarization | [guides/documentation.md](../harness/guides/documentation.md): níveis, KDoc, README de módulo, regras para resumos de IA |
| Onboarding | [docs/onboarding/](../onboarding/README.md): trilha de 30 dias e glossário de domínio |
| Commits / PR / review | `CONTRIBUTING.md`, `.gitmessage`, `.github/CODEOWNERS`, template de PR, [code-review.md](../code-review.md) |
| Skills | `onboarding` (tour, where, flow, explain) e `summarize` (module, flow, change) |
| Template | [templates/module-README.md](../harness/templates/module-README.md) |

### Consequências
- **Positivas:** pessoa nova tem caminho claro para o 1º PR; dúvidas recorrentes viram
  documentação; descrições de PR padronizadas com ordem de leitura; review com vocabulário comum.
- **Negativas:** README por módulo é mais um artefato a manter (mitigado: grafo gerado, limite de
  ~60 linhas, item no template de PR); CODEOWNERS só vale depois de os times serem definidos.

### Confirmação
- Template de PR exige atualizar README/KDoc/glossário quando a API pública mudar.
- Proposto (exige aprovação, pois altera `scripts/harness/` e `config/`): sensor `check-docs.sh`
  (todo módulo de `settings.gradle.kts` tem `README.md` com as seções do template; links
  relativos válidos) e regra detekt `UndocumentedPublicClass/Function` restrita a `feature:*:api`.
- Proposto (exige aprovação, altera CI): checagem do título do PR em Conventional Commits.
- Métricas de [docs/onboarding](../onboarding/README.md#métricas-da-trilha) revisadas por turma.

## Mais Informações
[Diátaxis](https://diataxis.fr/) · [Conventional Commits 1.0](https://www.conventionalcommits.org/pt-br/v1.0.0/) ·
[Conventional Comments](https://conventionalcomments.org/) · [Google eng-practices](https://google.github.io/eng-practices/) ·
[Now in Android: modularização](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md) ·
[Dokka](https://kotlinlang.org/docs/dokka-gradle.html) · [DX: AI e onboarding](https://getdx.com/blog/ai-cuts-developer-onboarding-time-in-half/)
