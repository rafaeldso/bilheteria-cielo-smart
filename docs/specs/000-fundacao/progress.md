# Progresso: 000 — Fundação

> Estado de trabalho entre sessões e agentes (skill `session-handoff`). A lista de tarefas está no
> [tasks.md](tasks.md). Nunca registre segredos, PAN ou PII aqui.

**Tamanho:** Infra (sem spec) · **Fase:** 5 Implementar (concluída, aguarda push) · **Branch:** `main` (sem commit)

## Estado atual
- **Última tarefa concluída:** T13 — README "Rodando localmente"
- **Próxima tarefa:** T14 — repositório privado + primeiro push (**só com pedido explícito**)
- **Arquivos em andamento:** nenhum
- **Último sensor:** `verify.sh full` → verde (S-01, S-02, `harnessFull`, S-09) em 2026-10-17

## Prova dos sensores (substitui EV-01…EV-10, ADR-0009 S10)
Arquivo temporário em `feature/probe/domain` com `import android.util.Base64` e `logger.info(cardNumber)`:
| Sensor | Resultado |
|---|---|
| Hook `PostToolUse` (S-01) | Bloqueou na edição: AP-03 domain impuro |
| detekt `SensitiveDataInLog` (S-05) | Falhou: `'cardNumber' parece conter dado sensível…` |
| Konsist (S-06) | Falhou: `domain nao importa Android nem infraestrutura` |
Arquivo removido em seguida. App instalado e aberto no AVD Android 10 (API 29).

## Decisões da implementação
| Data | Decisão | Motivo | Afeta spec/plano? |
|---|---|---|---|
| 2026-10-17 | Base do projeto gerada por `android create empty-activity` e reorganizada nos convention plugins | Template oficial = fonte das versões AndroidX | Não |
| 2026-10-17 | AGP 9.4.1, Kotlin 2.4.21, KSP 2.3.12, Gradle 9.8.1, Compose BOM 2026.09.00 | Últimas estáveis consultadas nos repositórios oficiais | Não |
| 2026-10-17 | `compileSdk 37`, `targetSdk 36`, `minSdk 24` | BOM exige compile 37; target sobe só com checklist; terminais Cielo a partir de Android 7.1 (research.md) | D1 confirma minSdk no plano |
| 2026-10-17 | detekt **1.23.8** (não 2.x) + compose-rules 0.6.8 | detekt 2.x só existe em alpha (ADR-0007); classpath do detekt fixado na versão de Kotlin dele | Não |
| 2026-10-17 | JUnit 5 nos módulos Android via `useJUnitPlatform()`, sem plugin `android-junit5` | Testes do D0 são JVM; uma dependência a menos | Instrumentados, se houver, reavaliam |
| 2026-10-17 | Navigation, serialization e desugaring ficam para a 001 | Art. VII: entram com a primeira feature que usa | Plano da 001 |
| 2026-10-17 | `detekt-rules` analisado com o próprio jar | Sem isso o detekt rejeita a seção `project-rules` do `detekt.yml` sem alterar `config/` | Não |
| 2026-10-17 | Konsist ignora `docs/` (templates `com.example`) e só aplica a regra de camadas quando existem domain/data/ui do pacote base | Konsist recusa camada vazia | Não |
| 2026-10-17 | Spotless sem `ratchetFrom` | Ainda não há `origin/main` | Ativar após o primeiro push |
| 2026-10-17 | Actions dos workflows fixadas por SHA (resolvidos com `gh api`) | zizmor (S-21) exige | Não |
| 2026-10-17 | CD: `release.yml` (APK debug como artefato; GitHub Release em tag `v*`) | Avaliador instala sem compilar; sem keystore (S3) | Não |

## Bloqueios e perguntas abertas
- [ ] Humano: autorizar criação do repositório privado e o primeiro push (T14).
- [ ] Humano: aprovar correção do falso positivo do `guard-bash` (steering loop 2026-10-17).
- [ ] zizmor não foi executado localmente (não instalado); primeira execução será no CI.

## Log de sessões
| Data | Ferramenta / agente | Resumo |
|---|---|---|
| 2026-10-17 | Claude Code (sessão principal) | T01–T13; `verify.sh full` verde; app rodando no AVD API 29 |
