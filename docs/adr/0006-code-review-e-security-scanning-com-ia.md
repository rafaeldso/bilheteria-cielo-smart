---
status: aceito
date: 2026-10-17
decisores: Rafael Oliveira
consultados:
informados:
---

# ADR-0006: Estrutura de code review e security scanning com IA

## Contexto e Problema
O projeto é um app Android de pagamentos (PCI DSS, LGPD) desenvolvido em boa parte por agentes de
IA. O ADR-0003 definiu os sensores de qualidade e uma segurança mínima (Gitleaks, OSV-Scanner,
Dependabot, MobSF "no release"), e o ADR-0004 trouxe revisores de IA locais. Faltavam: SAST
específico de mobile, verificação do binário de release, proteção da supply chain (dependências
e GitHub Actions), revisão de segurança por IA no PR, regras de revisão portáveis entre
ferramentas de IA, e um processo de triagem com severidade e SLA únicos.

Agentes de IA trazem dois riscos novos: dependências alucinadas registradas por atacantes
(*slopsquatting*) e prompt injection pelo conteúdo do próprio PR contra o revisor de IA.

## Fatores de Decisão
- Cobertura do OWASP MASVS v2.1 rastreável por controle
- Gates determinísticos e gratuitos (premissa de custo do ADR-0003); IA só como camada de comentário
- Portabilidade entre ferramentas de revisão por IA (Claude, Copilot, CodeRabbit)
- Mesmo scanner no CI e na máquina do desenvolvedor/agente
- Conformidade: código enviado a provedores de IA é dado saindo da empresa

## Opções Consideradas
- **A.** Camadas: scanners gratuitos como gate + IA como comentário + `REVIEW.md` neutro
- **B.** Plataforma comercial única (ex.: GitHub Advanced Security + CodeRabbit, ou MAST comercial)
- **C.** Manter o ADR-0003 como está e confiar nos revisores de IA locais

## Decisão
Opção escolhida: **A**, porque cobre cada classe de risco com a ferramenta mais barata capaz de
pegá-la, mantém gates determinísticos e não amarra o projeto a um fornecedor de IA.
Pesquisa completa: [docs/security/landscape.md](../security/landscape.md).

| Sensor | Ferramenta | Onde | Gate? |
|---|---|---|---|
| S-08 (amplia) | `com.android.security.lint:lint` (checks de segurança do Google para Android Lint) | `app.quality` | Sim |
| S-14 (move) | Gitleaks CLI | `security.yml`, `security-scan.sh` | Sim |
| S-15 (atualiza) | OSV-Scanner **v2** sobre `gradle.lockfile` (exige *dependency locking*) + Dependabot com *cooldown* | `security.yml` | Sim |
| S-21 (novo) | zizmor (workflows do GitHub Actions) + actions fixadas por SHA | `security.yml` | Sim |
| S-22 (novo) | mobsfscan (SAST mobile no fonte) | `security.yml`, `security-scan.sh` | Sim |
| S-23 (novo) | MobSF estático no APK de release | `security.yml` (main/semanal) | Sim, `high` |
| S-24 (novo) | Gradle dependency verification (`verification-metadata.xml`, SHA-256 + PGP) | Gradle | Sim |
| S-25 (novo) | SBOM CycloneDX (`org.cyclonedx.bom`) | Release | Evidência |
| S-26 (novo) | `anthropics/claude-code-security-review` | `ai-security-review.yml` | Não |
| S-27 (opcional) | Claude Code Review gerenciado, lendo `REVIEW.md` | GitHub App | Não |

Artefatos de processo: `REVIEW.md`, `SECURITY.md`, `docs/security/` (programa, matriz MASVS,
triagem, política de IA), skill `security-scan`.

**Dependências novas que exigem aprovação:** plugin `org.cyclonedx.bom` e lint checks
`com.android.security.lint:lint` (Gradle); os demais são binários de CI, fora do build do app.

**Custo:** gates gratuitos. Variáveis: minutos de Actions (job de binário só em `main`/semanal) e
tokens de API das revisões de IA (por rótulo ou caminho sensível). Claude Code Review gerenciado
e ferramentas comerciais ficam desligados até decisão explícita.

### Consequências
- **Positivas:** cada controle MASVS tem dono (sensor, IA ou teste manual); supply chain coberta
  de ponta a ponta; regras de revisão num só arquivo, lido por qualquer ferramenta; IA nunca
  bloqueia nem aprova sozinha.
- **Negativas:** mais jobs no CI; *dependency locking* e verification metadata exigem atualizar
  arquivos a cada bump (o Dependabot atualiza o lockfile; a verificação exige regenerar); actions
  fixadas por SHA dão mais trabalho de manutenção (mitigado por Dependabot e `pinact`).

### Confirmação
- `security.yml` com os jobs `secrets`, `dependencies`, `sast-mobile` e `workflows` marcados como
  obrigatórios na proteção de `main`.
- `scripts/harness/security-scan.sh --strict` verde na máquina de quem mantém o harness.
- Matriz MASVS revisada a cada release ([masvs-matrix.md](../security/masvs-matrix.md)).

## Prós e Contras das Opções

### B. Plataforma comercial única
- **A favor:** dashboards, CodeQL com fluxo de dados, suporte.
- **Contra:** custo por dev/repositório; CodeQL para Kotlin exige build; amarra ao fornecedor.
  Reavaliar MAST comercial (NowSecure/Oversecured) antes do go-live com auditoria PCI.

### C. Manter como está
- **A favor:** nenhum trabalho novo.
- **Contra:** sem SAST mobile, sem verificação do binário, sem proteção do pipeline nem contra
  dependência alucinada; revisores de IA sem gate determinístico por trás.

## Mais Informações
- [docs/security/README.md](../security/README.md) · [REVIEW.md](../../REVIEW.md) · [SECURITY.md](../../SECURITY.md)
- Revisar este ADR em 6 meses (mercado de revisão por IA muda rápido).
