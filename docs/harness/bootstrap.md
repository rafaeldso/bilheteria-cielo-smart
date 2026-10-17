# Bootstrap: ativando o harness no projeto Android

> Checklist para quando o projeto for criado. Até lá, scripts e hooks detectam a
> ausência do Gradle e apenas avisam, sem bloquear.

## Pré-requisitos
- [x] ADR-0003 (stack de sensores) aprovado em 2026-10-16
- [x] ADR-0006 (code review e security scanning com IA) aprovado em 2026-10-17
- [ ] `<base.package>` definido e registrado em `docs/harness/guides/architecture.md`

## Passos
1. [ ] `git init`, `git config core.hooksPath .githooks` e `git config commit.template .gitmessage`
2. [ ] Criar o projeto: `android create` (ou Android Studio), Kotlin DSL + Version Catalog
3. [ ] Rodar `scripts/harness/bootstrap.sh` (instala skills oficiais, marca scripts como executáveis, configura hooks do git)
4. [ ] Criar `build-logic/convention` com os plugins de [gradle.md](guides/gradle.md),
       incluindo `app.quality` (sensores por módulo) e `app.root` com as tasks `harnessFast`, `harnessCheck`, `harnessFull`
5. [ ] Criar `core:common`, `core:testing`, `core:designsystem`
6. [ ] Criar `quality/architecture-tests` a partir de [templates/kotlin/ArchitectureTest.kt](templates/kotlin/ArchitectureTest.kt)
7. [ ] Criar `quality/detekt-rules` a partir de [templates/kotlin/SensitiveDataInLogRule.kt](templates/kotlin/SensitiveDataInLogRule.kt)
8. [ ] Ligar `config/detekt/detekt.yml` e `config/lint/lint.xml` no `app.quality`
9. [ ] Rodar `scripts/harness/verify.sh full` e deixar verde
10. [ ] Configurar proteção da branch `main`: workflow `ci` obrigatório, 1 aprovação, *Require review
        from Code Owners*, só *squash merge*, apagar branch após merge; definir os times e descomentar
        `.github/CODEOWNERS`
10a. [ ] Gerar o grafo de dependências nos `README.md` de módulo (marcadores `module-graph`) a partir
        da configuração do sensor S-12; preencher *Rodando localmente* no `README.md` da raiz
11. [ ] Workflows agênticos (ADR-0004): confirmar que `.agents/skills` foi versionado como
        link simbólico (`git ls-files -s .agents/skills` → modo `120000`); no Windows,
        `git config core.symlinks true`
12. [ ] Opcional: segredo `ANTHROPIC_API_KEY` (environment `ai-review` com revisores) e rótulos
        `ai-review` e `security-review` no GitHub ([política de IA](../security/ai-review.md#dados-e-conformidade))
12a. [ ] Security scanning ([ADR-0006](../adr/0006-code-review-e-security-scanning-com-ia.md)):
        - fixar todas as actions por SHA: `pinact run` (o job `security / workflows` falha até isso ser feito)
        - *dependency locking* em todos os módulos (`gradle.lockfile`) para o OSV-Scanner
        - `./gradlew --write-verification-metadata sha256,pgp help` → `gradle/verification-metadata.xml`
        - `lintChecks("com.android.security.lint:lint:<versão>")` e plugin `org.cyclonedx.bom` no `app.quality`/`app`
        - secret `MOBSF_API_KEY` (qualquer valor aleatório; usado pelo container do job `binary`)
        - marcar `security / secrets`, `dependencies`, `sast-mobile` e `workflows` como obrigatórios em `main`
        - ativar *private vulnerability reporting* e definir o e-mail em `SECURITY.md`
        - instalar localmente: `brew install gitleaks osv-scanner zizmor && pipx install mobsfscan`;
          `scripts/harness/security-scan.sh --strict` verde
        - opcional: Claude Code Review gerenciado (Team/Enterprise) com `main` protegida, para honrar o `REVIEW.md`
13. [ ] Criar `journeys/` com a Journey do primeiro fluxo crítico (ver [execution](execution/README.md#journeys-testes-e2e-em-linguagem-natural))
14. [ ] Primeira feature pelo fluxo agêntico: `/feature-flow <demanda>` (`docs/specs/001-...`)
15. [ ] Rodar os cenários [EV-01…EV-10](workflows/scenarios.md) e registrar o resultado

## Validação do harness
- [ ] Introduzir de propósito `Dispatchers.IO` num repositório → hook `PostToolUse` acusa
- [ ] Import `android.*` no domain → Konsist falha
- [ ] Teste sem `AC-xx` para um AC da spec → `check-traceability.sh` falha
- [ ] `git push` pelo agente → bloqueado
