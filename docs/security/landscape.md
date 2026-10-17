# Panorama de mercado: revisão de código e varredura de segurança com IA

> Pesquisa feita em 2026-10 para embasar o [ADR-0006](../adr/0006-code-review-e-security-scanning-com-ia.md).
> Revise a cada 6 meses: este mercado muda rápido. Boa parte das comparações públicas é
> feita por fornecedores; números de benchmark abaixo são indicativos, não definitivos.

## 1. Como o mercado organiza o problema
A prática consolidada em 2025–2026 combina quatro famílias, cada uma com papel diferente:

| Família | Pergunta que responde | Exemplos de mercado |
|---|---|---|
| **SAST** (análise estática do fonte) | "Este código tem um padrão vulnerável?" | CodeQL, Semgrep/Opengrep, SonarQube, mobsfscan, detekt, Android Lint |
| **SCA** (dependências) | "Alguma biblioteca tem CVE ou é maliciosa?" | OSV-Scanner, Dependabot, Renovate, OWASP Dependency-Check, Snyk |
| **Segredos** | "Há credencial no código ou no histórico?" | Gitleaks, TruffleHog, GitHub secret scanning |
| **Binário / MAST** (mobile) | "O APK final está endurecido?" | MobSF, NowSecure, Oversecured, AppKnox |
| **Supply chain de CI** | "O pipeline pode ser sequestrado?" | zizmor, OpenSSF Scorecard, Harden-Runner, pinning de actions por SHA |
| **Revisão por IA** | "A mudança tem bug de lógica, falha de autorização, desvio de intenção?" | Claude Code Review, CodeRabbit, Copilot code review, Qodo, Greptile |

A IA **não substitui** SAST/SCA: ela cobre o que regra não expressa (lógica, contexto,
intenção) e erra de formas não determinísticas. Ferramentas determinísticas continuam
sendo o gate; a IA é uma camada de comentário.

## 2. Revisão de código com IA
| Ferramenta | Pontos fortes | Limitações | Configuração no repositório |
|---|---|---|---|
| **Claude Code Review** (gerenciado, desde mar/2026) | Vários agentes em paralelo + etapa de verificação que filtra falso positivo; comenta inline com severidade; nunca aprova nem bloqueia | Planos Team/Enterprise; só GitHub; indisponível com Zero Data Retention; custo por token (≈ US$ 15–25 por revisão, segundo terceiros) | `REVIEW.md` (raiz e subpastas, lido da branch base protegida) + `CLAUDE.md` |
| **Claude Code em GitHub Actions** (`anthropics/claude-code-action`) | Roda no seu runner, usa seus subagentes e skills; qualquer plano com chave de API | Você paga e controla turnos/ferramentas | Prompt do workflow + `.claude/` |
| **`anthropics/claude-code-security-review`** | Foco em segurança, filtro de falso positivo customizável, comenta no PR | **Não é endurecido contra prompt injection**: só para PRs confiáveis | Arquivos de instruções de varredura e de filtro |
| **CodeRabbit** | Maior base instalada declarada; GitHub, GitLab, Bitbucket, Azure; resumos e diagramas de PR | Assinatura por dev (≈ US$ 24/mês); código vai para o fornecedor | `.coderabbit.yaml` (`path_instructions`); lê `AGENTS.md` automaticamente |
| **GitHub Copilot code review** | Atrito zero para quem já paga Copilot | Só GitHub; raciocínio entre arquivos mais raso, segundo comparativos | `.github/copilot-instructions.md`, `.github/instructions/*.instructions.md` (`applyTo`, `excludeAgent`); lê `AGENTS.md` |
| **Qodo, Greptile, Cursor BugBot** | Alternativas com indexação do repositório | Benchmarks conflitantes, quase todos feitos pelo próprio fornecedor | Arquivos próprios |

**Conclusão:** padronizar **arquivos neutros** (`AGENTS.md` para contexto, `REVIEW.md`
para regras de revisão) mantém o projeto portável entre ferramentas. Copilot e CodeRabbit
já leem o `AGENTS.md`; Claude lê `CLAUDE.md` (que importa o `AGENTS.md`) e `REVIEW.md`.
Não criamos `copilot-instructions.md` nem `.coderabbit.yaml` até que a ferramenta seja
adotada; se for, eles **apontam** para `REVIEW.md` em vez de duplicar regras.

## 3. Segurança no contexto Android
| Necessidade | Escolha | Por quê | Descartado |
|---|---|---|---|
| Baseline de requisitos | **OWASP MASVS v2.1** + **MASWE v1.0** (ago/2026) + **MASTG v2** | Padrão de fato para apps móveis; desde o MASTG v2 cada teste aponta para uma fraqueza MASWE e um controle MASVS. Os antigos níveis L1/L2/R saíram do MASVS e viraram "MAS Testing Profiles" no MASWE | — |
| SAST de plataforma | **Android Lint** + **`com.android.security.lint:lint`** (Google) | Checks oficiais com correção rápida: tapjacking, StrandHogg, cleartext, certificados de usuário, cripto fraca, `autoVerify` ausente | — |
| SAST mobile no fonte | **mobsfscan** | Regras do MobSF sobre Kotlin/Java/manifest, saída SARIF, gratuito, roda em segundos | Semgrep CE puro: as regras Pro de framework para Kotlin não funcionam no CE e as regras oficiais têm licença restritiva |
| SAST profundo (fluxo de dados) | **CodeQL** — *opcional* | Melhor análise de fluxo; Kotlin exige build (`build-mode: none` ignora Kotlin) | Obrigatório só se houver GitHub Code Security (pago em repositório privado) |
| Binário | **MobSF** estático no APK/AAB de release (Docker) | Vê o que só existe no artefato: manifest mesclado com SDKs, R8, `.so`, permissões finais | NowSecure/Oversecured/AppKnox: superiores em DAST e cobertura, porém pagos; reavaliar antes de go-live com auditoria PCI |
| Cobertura realista | Automação cobre ≈ 60–70% do MASVS | O resto é teste manual MASTG por release ([masvs-matrix.md](masvs-matrix.md)) | — |

## 4. Supply chain (dependências e pipeline)
| Risco | Controle | Fonte da prática |
|---|---|---|
| CVE em dependência | **OSV-Scanner v2** sobre `gradle.lockfile` (exige *dependency locking*) + Dependabot | OSV escaneia versões **resolvidas**; manifesto sem lock gera falso negativo |
| Artefato adulterado no repositório Maven | **Gradle dependency verification** (`gradle/verification-metadata.xml`, SHA-256 + PGP) | Doc. oficial do Gradle: protege contra artefato adulterado, **não** contra CVE (por isso complementa o OSV) |
| *Slopsquatting* (pacote alucinado pela IA e registrado por atacante) | Regra no `REVIEW.md` e no guia: toda dependência nova é verificada (existe no Maven Central/Google Maven, mantenedor, idade, downloads) antes do PR; Dependabot com *cooldown* | Estudo USENIX Security 2025: 5,2% (modelos comerciais) a 21,7% (abertos) das amostras citavam pacotes inexistentes; caso real em 2026 com 237 repositórios referenciando um pacote alucinado |
| Action comprometida (caso `tj-actions/changed-files`, CVE-2025-30066, mar/2025) | Actions **fixadas por SHA** (`pinact`), `permissions` mínimas, **zizmor** no CI, Dependabot para `github-actions` | Recomendação consolidada de StepSecurity, Grafana, OpenSSF |
| Inventário para auditoria | **SBOM CycloneDX** gerado no release (plugin `org.cyclonedx.bom`) | Pedido crescente em auditorias; o MASTG cita SBOM CycloneDX + Dependency-Track |
| Postura do repositório | OpenSSF Scorecard — *quando o repositório for público* | Publicação de resultados exige repositório público |

## 5. Restrições do projeto que pesaram
- **Custo** (ADR-0003): tudo que é gate é gratuito/open source. Ferramentas pagas
  (GitHub Code Security, CodeRabbit, MAST comercial, Claude Code Review) ficam como
  opcionais, ligadas por decisão explícita.
- **PCI DSS / LGPD:** código enviado a um serviço de IA é dado saindo da empresa. Ver
  [ai-review.md](ai-review.md#dados-e-conformidade).
- **Agentes escrevem código:** o risco de dependência alucinada e de prompt injection
  vindo do próprio PR é real e precisa de controle explícito.

## Fontes
- Anthropic — [Code Review](https://code.claude.com/docs/en/code-review), [Customize Code Review (REVIEW.md)](https://code.claude.com/docs/en/code-review-customize), [claude-code-security-review](https://github.com/anthropics/claude-code-security-review), [Automate security reviews with Claude Code](https://www.anthropic.com/news/automate-security-reviews-with-claude-code)
- GitHub — [Copilot repository custom instructions](https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions), [CodeQL: Kotlin detected in no build](https://docs.github.com/pt/enterprise-server@3.17/code-security/reference/code-scanning/troubleshoot-analysis-errors/kotlin-detected-in-no-build)
- CodeRabbit — [Path instructions](https://docs.coderabbit.ai/configuration/path-instructions)
- Comparativos (viés de fornecedor) — [Monterail](https://www.monterail.com/blog/ai-code-review-tools-compared-how-to-choose-best), [Sourcegraph](https://sourcegraph.com/blog/automated-code-review-tools), [buildmvpfast](https://www.buildmvpfast.com/blog/best-ai-code-review-tools-anthropic-2026), [kunalganglani](https://www.kunalganglani.com/blog/ai-code-review-tools-2026-compared)
- OWASP MAS — [MASVS](https://mas.owasp.org/MASVS/), [MASTG releases](https://github.com/OWASP/mastg/releases), [MASWE](https://devguide.owasp.org/en/05-implementation/04-maswe/), [MASTG-TECH-0130 (SBOM)](https://mas.owasp.org/MASTG/techniques/android/MASTG-TECH-0130/), [NowSecure sobre MASWE 1.0](https://www.nowsecure.com/press-releases/nowsecure-unveils-first-automated-owasp-masvs-v2-1-mobile-app-security-and-new-privacy-testing/)
- Android — [android-security-lints](https://github.com/google/android-security-lints), [Making security easy (Android Developers Blog)](https://android-developers.googleblog.com/2024/07/making-security-easy-vuln-remediation.html)
- Mobile SAST/MAST — [MobSF](https://appsecsanta.com/mobsf), [NowSecure vs MobSF](https://appsecsanta.com/mobile-security-tools/nowsecure-vs-mobsf), [Oversecured: SAST mobile](https://oversecured.com/blog/best-sast-tools-for-mobile-apps)
- Semgrep/Opengrep — [Semgrep vs Opengrep](https://semgrep.dev/docs/faq/comparisons/opengrep), [Kotlin no Semgrep](https://cloudflare.semgrep.dev/docs/languages/kotlin), [InfoQ](https://www.infoq.com/news/2025/02/semgrep-forked-opengrep)
- Supply chain — [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html), [CycloneDX Gradle plugin](https://plugins.gradle.org/plugin/org.cyclonedx.bom/3.4.0), [OSV-Scanner v2](https://security.googleblog.com/2025/03/announcing-osv-scanner-v2-vulnerability.html), [OSV: manifesto vs lockfile](https://oneuptime.com/blog/post/2026-07-23-manifest-vs-lockfile-osv/view)
- CI — [zizmor na Grafana](https://grafana.com/blog/2025/06/25/how-to-detect-vulnerable-github-actions-at-scale-with-zizmor/), [OpenSSF Scorecard action](https://github.com/marketplace/actions/ossf-scorecard-action), [Checklist GitHub Actions](https://www.stingrai.io/blog/github-actions-security-checklist)
- IA e supply chain — [CSA: Slopsquatting](https://labs.cloudsecurityalliance.org/research/csa-research-note-slopsquatting-ai-supply-chain-20260419/), [Endor Labs](https://www.endorlabs.com/learn/slopsquatting-when-ai-agents-hallucinate-malicious-packages)
