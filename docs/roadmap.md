# Roadmap: Bilheteria (case Cielo Smart)

> Plano de execução do case [case_android.md](../case_android.md). Cada demanda segue o pipeline
> do `feature-flow`; o estado de cada uma fica no `progress.md` da sua pasta em `docs/specs/`.
> Uso da IA em cada etapa: [docs/ai/](ai/README.md).

**Aprovado em:** 2026-10-17 · **Responsável:** Rafael Oliveira

## Identidade
| Item | Valor |
|---|---|
| Nome do app | Bilheteria (genérico; sem marca Cielo) |
| Pacote base | `com.rafaeloliveira.bilheteria` |
| Autor / licença | Rafael Oliveira · Apache 2.0 |
| Repositório | GitHub, **privado** durante o desenvolvimento, **público** na entrega (D3) |

## Demandas
| # | Demanda | Tamanho | Artefatos | Status |
|---|---|---|---|---|
| D0 | `000-fundacao`: projeto Android limpo, build-logic, sensores, CI/CD | Infra (sem spec) | `tasks.md`, `progress.md` | ✅ |
| D1 | `001-venda-de-ingressos`: RF1–RF5 + RNFs do case | G | spec, research, ui-spec + prancha, plan, tasks | ✅ Implementada (spec 1.3.0, PR #7) |
| D2 | `002-qr-code-ingresso` (opcional no case) | M (era P: entraram impressão e compartilhamento) | spec, research, ui-spec + prancha, plan, tasks | ✅ Implementada (spec 1.1.2; impressão verificada no AVD Android 11, terminal real pendente) |
| D3 | Entrega: README, uso de IA, repositório público | Isento (docs) | README, `docs/ai/` | 🟡 Docs prontas; D2 feita; falta tornar o repositório público |

## Ordem de execução (duas trilhas)
A trilha A (código) avança enquanto a trilha B (documentos) espera aprovação humana.

| Etapa | Trilha A: agente | Trilha B: humano aprova |
|---|---|---|
| 1 | D0.1–D0.3 git, projeto, build-logic | Spike Cielo → `research.md` |
| 2 | D0.4–D0.6 `core:*`, `quality/`, `verify.sh full` | D1 spec (`sdd-specify`) ⏸ |
| 3 | D0.7–D0.9 CI/CD, ADR de escopo, README | Direção visual ⏸ → UI Spec + prancha D1 ⏸ |
| 4 | AVD Android 10 + `lio-emulator.apk` | D1 plan ⏸ → tasks ⏸ |
| 5 | D1 domain (sequencial: contratos) | Revisa contratos |
| 6 | D1 data/ui em paralelo (`implement-parallel`) | — |
| 7 | Fechamento: `review-changes`, `consistency-check`, `verify-on-device`, `verify.sh full` | ⏸ |
| 8 | D2 (se aprovado) → D3 | Revisa README e relato de IA |

## D0 — Fundação (subtarefas)
1. Ambiente: JDK 21, Android SDK, `adb` no PATH, `gh` autenticado.
2. `git init`, hooks (`.githooks`), `.gitmessage`; repositório privado no GitHub (push só com pedido explícito).
3. `android create` (Kotlin DSL + version catalog) → `scripts/harness/bootstrap.sh`.
4. `build-logic/convention` com `app.quality` e `app.root` (`harnessFast`, `harnessCheck`, `harnessFull`).
5. `core:common`, `core:testing`, `core:designsystem` (tema placeholder até a direção visual).
6. `quality/architecture-tests` (Konsist) e `quality/detekt-rules`; `verify.sh full` verde.
7. CI: `ci.yml` existente; CD novo: APK como artefato no `main` e GitHub Release por tag.
8. ADR-0009: escopo de case (itens S1–S10 abaixo), com a exceção à Constitution VI.4.
9. README: "Rodando localmente".

## Itens de produção pulados (aprovados em 2026-10-17)
| # | Item | Motivo |
|---|---|---|
| S1 | Firebase Crashlytics/Performance | Exige projeto Firebase e consentimento; mantido `Logger` + Timber |
| S2 | `:baselineprofile` / Macrobenchmark | Otimização de produção |
| S3 | Assinatura de release e publicação | Não vai para loja; build release com R8 mantido |
| S4 | Workflows `ai-review` / `ai-security-review` configurados | Exigem `ANTHROPIC_API_KEY` paga; arquivos mantidos |
| S5 | Job MobSF `binary` | Gitleaks, OSV e mobsfscan já cobrem |
| S6 | Proteção de branch, CODEOWNERS, checks obrigatórios | Indisponível em repo privado no plano Free; dev solo |
| S7 | `verification-metadata.xml` e lockfiles | Atrito a cada dependência; Dependabot cobre |
| S8 | Screenshot tests (Roborazzi) | Foco em cenários críticos: unitários + UI test do fluxo principal |
| S9 | Layout adaptativo (WindowSizeClass) | Terminal Smart tem tela fixa; tema escuro e a11y mantidos |
| S10 | Cenários EV-01…EV-10 do harness | Meta-teste do harness, sem valor de produto |

## Melhorias futuras
Alimenta a seção "O que faria com mais tempo" do README (exigida pelo case) na entrega (D3).

| # | Melhoria | Por que não agora | Origem |
|---|---|---|---|
| F1 | **Credenciais Cielo por configuração gerenciada** (Managed Configurations/MDM da Cielo Store, ou backend que emite credencial de curta duração) em vez de `BuildConfig` | Exige infraestrutura que o case não avalia; no case o token é fictício (emulador) | [ADR-0010](adr/0010-integracao-cielo-smart-por-deep-link.md), pedido do humano em 2026-10-17 |
| F2 | Itens de produção pulados (Crashlytics, baseline profiles, assinatura, MobSF, proteção de branch etc.) | Custo sem valor para a avaliação | [ADR-0009](adr/0009-escopo-de-case-itens-de-producao-fora.md) |

## Decisões
| Data | Decisão | Motivo |
|---|---|---|
| 2026-10-17 | Eventos vêm de **JSON local** (`assets/`) atrás de uma interface de data source | Offline e determinístico; remoto gratuito (My JSON Server, raw GitHub) exige repo público; nenhum requisito pede rede |
| 2026-10-17 | Avaliar na spec D1 se a disponibilidade de ingressos é decrementada localmente após compra aprovada (ex.: 50 → 49) | Sugestão do humano; só entra se a spec justificar |
| 2026-10-17 | Integração recomendada pela doc Cielo: **Deep Link** (a confirmar no spike) | [Baixando o emulador](https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo) |
| 2026-10-17 | Emulador Cielo: `lio-emulator.apk` 1.61.8, só Android 7 ou 10 → AVD Android 10 (API 29) arm64 | Mesma fonte |
| 2026-10-17 | Paleta de azuis semelhante à Cielo Smart/LIO, sem logo, nome ou telas copiadas | Pedido do humano; valores na direção visual |
