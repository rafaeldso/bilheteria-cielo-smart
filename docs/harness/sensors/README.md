# Catálogo de Sensores (feedback)

> Sensores verificam o trabalho **depois** da ação. Toda falha deve informar
> **o que** falhou, **onde** e **como corrigir**, em formato legível por um LLM.

## Níveis de verificação
| Nível | Comando | Quando roda | Tempo alvo |
|---|---|---|---|
| **file** | `scripts/harness/lint-file.sh <arquivo>` | Hook `PostToolUse` a cada edição | < 1 s |
| **light** | `scripts/harness/verify.sh light` | Pre-commit (arquivos staged, sem Gradle) | ~1 s |
| **fast** | `scripts/harness/verify.sh fast` | Hook `Stop` (configurável por `HARNESS_STOP_LEVEL`) | < 60 s |
| **standard** | `scripts/harness/verify.sh standard` | Antes de declarar tarefa pronta; CI em PR | < 5 min |
| **full** | `scripts/harness/verify.sh full` | Fechamento M/G; CI em `main` e release | < 20 min |
| **security** | `scripts/harness/security-scan.sh` | Mudança sensível (skill `security-scan`); CI em `security.yml` | < 2 min |
| **review** | subagentes revisores | Fechamento M/G; PR | — |
| **contínuo** | CI agendado | Semanal | — |

## Inventário
| ID | Sensor | Tipo | Nível | Verifica | Configuração |
|---|---|---|---|---|---|
| S-01 | `lint-file.sh` | computacional | file | Padrões proibidos (AP-01, 02, 03, 07, 08, 10) no arquivo editado | [scripts/harness/lint-file.sh](../../../scripts/harness/lint-file.sh) |
| S-02 | `scan-sensitive-data.sh` | computacional | fast | Segredos e dados de cartão/PII em código, logs e fixtures | [scripts/harness/scan-sensitive-data.sh](../../../scripts/harness/scan-sensitive-data.sh) |
| S-03 | Spotless + ktlint | computacional | fast | Formatação | `.editorconfig`, `app.quality` |
| S-04 | detekt | computacional | fast | Complexidade, smells, coroutines, chamadas proibidas | [config/detekt/detekt.yml](../../../config/detekt/detekt.yml) |
| S-05 | detekt-rules próprias | computacional | fast | Regras do projeto com mensagens de correção | `quality/detekt-rules` ([template](../templates/kotlin/SensitiveDataInLogRule.kt)) |
| S-06 | Konsist | computacional | fast | Camadas, nomes, estrutura de ViewModel/UseCase/Screen | `quality/architecture-tests` ([template](../templates/kotlin/ArchitectureTest.kt)) |
| S-07 | Testes unitários | computacional | standard | Comportamento (ACs) | `testDebugUnitTest` |
| S-08 | Android Lint | computacional | standard | Plataforma, a11y, recursos, segurança | [config/lint/lint.xml](../../../config/lint/lint.xml) |
| S-09 | `check-traceability.sh` | computacional | standard | Todo `AC-xx` da spec tem teste citando o ID | [scripts/harness/check-traceability.sh](../../../scripts/harness/check-traceability.sh) |
| S-10 | Roborazzi | computacional | full | Regressão visual por estado | `app.android.compose` |
| S-11 | Kover | computacional | full | Cobertura mínima por camada | `app.quality` |
| S-12 | Module graph assertion | computacional | full | Dependências entre módulos | `app.quality` |
| S-13 | Build release (R8) | computacional | full | Ofuscação, regras keep, `debuggable=false` | `app.android.application` |
| S-14 | Gitleaks (CLI) | computacional | CI | Segredos no histórico | `.github/workflows/security.yml`, `security-scan.sh` |
| S-15 | OSV-Scanner v2 + Dependabot | computacional | CI/contínuo | Vulnerabilidades nas versões resolvidas (`gradle.lockfile`) | `.github/workflows/security.yml`, `.github/dependabot.yml` |
| S-16 | `architecture-reviewer` | inferencial | review | Aderência à arquitetura e SOLID | `.claude/agents/` |
| S-17 | `security-reviewer` | inferencial | review | MASVS, PCI, LGPD | `.claude/agents/` |
| S-18 | `test-reviewer` | inferencial | review | Qualidade e significado dos testes | `.claude/agents/` |
| S-19 | `compose-ui-reviewer` | inferencial | review | Estados, previews, a11y, design system | `.claude/agents/` |
| S-20 | `spec-consistency-analyzer` | inferencial | review | spec ↔ plano ↔ tarefas ↔ código | `.claude/agents/` |
| S-21 | zizmor | computacional | CI | Workflows do GitHub Actions: injeção, permissões, actions sem SHA | `.github/workflows/security.yml` |
| S-22 | mobsfscan | computacional | CI | SAST mobile em Kotlin/Java/manifest (regras MobSF) | `.github/workflows/security.yml`, `security-scan.sh` |
| S-23 | MobSF (APK release) | computacional | release | Binário final: manifest mesclado, R8, permissões, `.so` | [scripts/harness/mobsf-scan.sh](../../../scripts/harness/mobsf-scan.sh) |
| S-24 | Gradle dependency verification | computacional | build | Artefatos adulterados (SHA-256 + PGP) | `gradle/verification-metadata.xml` |
| S-25 | SBOM CycloneDX | evidência | release | Inventário de componentes para auditoria | `org.cyclonedx.bom` |
| S-26 | `claude-code-security-review` | inferencial | PR | Segurança do diff (caminhos sensíveis) | `.github/workflows/ai-security-review.yml`, `.github/security/` |
| S-27 | Claude Code Review (opcional) | inferencial | PR | Bugs e regras do `REVIEW.md` | GitHub App + [REVIEW.md](../../../REVIEW.md) |
| S-00 | `selftest.sh` | computacional | CI | O próprio harness: guards e sensores pegam o que devem (e não geram falso positivo) | [scripts/harness/selftest.sh](../../../scripts/harness/selftest.sh) |

## Contrato de mensagem
Todo sensor próprio imprime falhas neste formato (os scripts de `scripts/harness/` já seguem):
```
SENSOR <id> FALHOU: <regra>
  Onde: <arquivo>:<linha>
  Problema: <o que está errado>
  Como corrigir: <ação concreta>
  Referência: <guia ou AP-xx>
```
Ferramentas de terceiros (detekt, Lint) são resumidas pelo `verify.sh`, que
extrai as primeiras ocorrências e aponta o relatório completo.

## Como adicionar um sensor
1. Registre o motivo no [steering loop](../steering-loop.md).
2. Prefira o mais barato e determinístico: `lint-file.sh` (regex) → detekt
   (`ForbiddenMethodCall`, regra própria) → Konsist → teste.
3. Escreva a mensagem no contrato acima.
4. Inclua-o no nível certo do `verify.sh` e nesta tabela, e adicione casos positivo/negativo ao `selftest.sh`.
5. Rode contra o código atual; corrija ou registre as violações existentes antes
   de torná-lo bloqueante.
