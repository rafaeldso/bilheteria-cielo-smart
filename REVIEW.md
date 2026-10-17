# Review instructions

> Instruções **somente de revisão de PR** para revisores de IA (Claude Code Review e
> workflows do projeto). Contexto geral e regras do código ficam no `AGENTS.md` / `CLAUDE.md`;
> não duplique aqui. Este arquivo vale a partir da branch padrão protegida.
> Escala de severidade: docs/security/triage.md.

## Contexto
App Android de meios de pagamento (Kotlin, Compose, Clean Architecture modularizada).
Trata dados de cartão (PCI DSS) e pessoais (LGPD). Baseline de segurança: OWASP MASVS v2.1.
Boa parte do código é escrita por agentes de IA a partir de uma spec em `docs/specs/`.

## What Important means here
Reserve Important (🔴) para achados que quebram comportamento, vazam dado ou impedem rollback:
- PAN, CVV, trilha, PIN, token ou PII em log, analytics, crash report, `toString()`,
  `Bundle`/`SavedStateHandle`, cache HTTP, notificação ou fixture.
- Dado sensível persistido sem cifra via `core:security` + Android Keystore, ou CVV persistido de qualquer forma.
- TLS enfraquecido: `TrustManager`/`HostnameVerifier` permissivo, cleartext, pinning sem pin de backup.
- Componente com `android:exported="true"` sem justificativa; intent/deep link usado sem validação;
  `PendingIntent` mutável; intent redirection.
- Autorização decidida só no cliente; ação de pagamento sem sessão válida e confirmação.
- Algoritmo fora da lista permitida em `docs/harness/guides/security.md`.
- Lógica que diverge de um `BR-xx`/`AC-xx` da spec do PR; estado de erro da spec não tratado.
- Corrotina sem cancelamento correto ou dispatcher fixo que causa bug observável.
Estilo, nomes e refatorações são Nit no máximo.

## Cap the nits
Report at most five Nits per review.

## Do not report
- O que o CI já garante: formatação (Spotless/ktlint), detekt, Android Lint, Konsist,
  Gitleaks, OSV-Scanner, mobsfscan, zizmor.
- Arquivos gerados (`**/build/**`, `**/generated/**`), `*.lockfile`, `gradle/verification-metadata.xml`.
- PANs de teste conhecidos (`4111111111111111` e afins) dentro de `src/test`, `src/androidTest` e `core/testing`.
- Ausência de cobertura de teste em geral (há sensor de rastreabilidade AC ↔ teste).
- Negação de serviço, rate limiting e open redirect sem impacto demonstrado.
- Em `docs/` e `scripts/`, só reporte se for quase certo e grave.

## Always check
- **Dependência nova** em `gradle/libs.versions.toml` ou `build.gradle.kts`: o artefato existe no
  Maven Central/Google Maven com esse groupId exato (risco de pacote alucinado/typosquatting),
  tem manutenção recente e foi aprovado (PR declara; ADR se estrutural). Sem isso, é Important.
- **API inexistente ou de versão diferente** da do catálogo de versões (sinal típico de código gerado).
- Toda classe com PAN, CVV ou PII sobrescreve `toString()` com máscara.
- `AndroidManifest.xml`: `exported` explícito, permissões novas justificadas na spec,
  `allowBackup`/`dataExtractionRules` excluindo tokens e bancos.
- Telas com dado de cartão usam o efeito de `FLAG_SECURE` do projeto, inclusive em dialogs e bottom sheets.
- Mudança em `.github/workflows/`: `permissions` mínimas, actions fixadas por SHA, nada de
  `pull_request_target` com checkout do PR, nenhum `${{ github.event.* }}` interpolado em `run:`.
- Teste que não pode falhar (assert no próprio fake, `verify` sem efeito observável).

## Evidence
- Afirmações sobre comportamento citam `arquivo:linha` no código-fonte, não inferência a partir de nomes.
- Achado de segurança descreve o cenário de exploração em uma frase (quem, como, o que vaza).
- Se o achado contradiz a spec do PR, cite o `BR-xx`/`AC-xx`; se a spec estiver errada, diga isso
  em vez de pedir código que diverge dela.
