---
status: aceito
date: 2026-10-16
decisores: Rafael Oliveira
---

# ADR-0003: Stack de sensores de qualidade e ferramentas de build

## Contexto e Problema
O harness (ADR-0002) depende de sensores computacionais: ferramentas determinísticas
que o agente consegue executar sozinho e cujas mensagens de erro indiquem como
corrigir. A constitution exige aprovação para novas dependências; este ADR
consolida a stack proposta para aprovação única antes da criação do projeto.

## Fatores de Decisão
- Padrão de mercado e do ecossistema oficial Android (Now in Android, guias do developer.android.com)
- Execução local rápida e reprodutível em CI
- Mensagens de erro acionáveis por um LLM
- Manutenção ativa e compatibilidade com Kotlin 2.x e AGP 9

## Opções Consideradas
- Stack proposta (abaixo)
- Somente Android Lint + testes
- Ferramentas comerciais de análise (SonarQube, etc.) como sensor principal

## Decisão
Opção escolhida: "Stack proposta", porque cobre manutenibilidade, arquitetura e
comportamento com ferramentas gratuitas, executáveis pelo agente e amplamente
adotadas.

| Categoria | Ferramenta | Papel no harness |
|---|---|---|
| Build | Gradle Kotlin DSL + Version Catalog + `build-logic` (convention plugins) | Configuração única e previsível |
| DI | Hilt | Padrão oficial Android |
| Formatação | Spotless + ktlint | Sensor de estilo com auto-correção |
| Análise estática | detekt (+ regras próprias em `quality/detekt-rules`) | Code smells, complexidade, padrões proibidos |
| Lint Android | Android Lint (+ Compose lint checks) | Problemas de plataforma, a11y, recursos |
| Regras Compose | `io.nlopez.compose.rules` (plugin detekt) | Boas práticas de Compose (modifier, state, previews) |
| Arquitetura | Konsist (`quality/architecture-tests`) | Regras de camada, nomenclatura e estrutura |
| Grafo de módulos | `com.jraska.module.graph.assertion` | Proíbe dependência feature → feature |
| Testes unitários | JUnit 5, MockK, Turbine, kotlinx-coroutines-test, fakes em `core:testing` | Comportamento de domain e presentation |
| Testes de UI | Compose UI Test + Robolectric | Fluxos críticos |
| Screenshot | Roborazzi | Regressão visual dos `@Preview` |
| Cobertura | Kover | Limite mínimo por camada |
| Segurança | Gitleaks CLI (CI), OSV-Scanner + Dependabot (CI), MobSF (release) | Segredos, CVEs, MASVS |
| Performance | Baseline Profiles + Macrobenchmark | Startup e jank (fase posterior) |

**Custo:** todas as ferramentas são gratuitas e open source (Apache 2.0 ou MIT),
ou serviços gratuitos do GitHub (Actions, Dependabot). Foram evitadas de propósito:
`dependency-review-action`, que exige GitHub Advanced Security (pago) em repositório
privado, e `gitleaks-action`, que exige chave de licença para organizações. No lugar
delas usamos os CLIs equivalentes. O único custo variável são os minutos de GitHub Actions em repositório privado.

### Impacto no desenvolvimento diário
Cada sensor roda no momento em que é mais barato, e o que é caro vai para o CI.

| Momento | O que roda | Custo esperado |
|---|---|---|
| A cada edição do agente (hook) | `lint-file.sh` no arquivo | < 0,1 s (medido) |
| Fim do turno do agente (hook `Stop`) | `verify.sh fast`: Spotless, detekt e Konsist, com cache incremental | 10–40 s com daemon aquecido; só roda se houve mudança em Kotlin. Configurável: `HARNESS_STOP_LEVEL=off\|light\|fast` |
| Commit | `verify.sh light` nos arquivos staged (sem Gradle) | ~1 s (medido: 0,7 s para 3.000 arquivos) |
| Antes de concluir uma tarefa | `verify.sh standard`: testes unitários e Lint | 1–5 min; módulos inalterados vêm do cache |
| CI em PR / `main` | `standard` / `full`: Roborazzi, Kover, grafo de módulos, release, Gitleaks, OSV | Fora da máquina do desenvolvedor |

Medidas que mantêm o ciclo leve estão em [gradle.md](../harness/guides/gradle.md#desempenho-no-dia-a-dia):
configuration cache, build cache, cobertura (Kover) e screenshot (Roborazzi) apenas
no `full`, e entradas declaradas para os testes Konsist.

### Consequências
- **Positivas:** o agente tem um ciclo de feedback completo e local; o CI repete
  os mesmos comandos (sem surpresas).
- **Negativas:** tempo de build maior no CI e alguns segundos no fim de cada turno
  do agente. Mitigado pelos níveis de verificação (`light`, `fast`, `standard`, `full`),
  pelo cache de build e pela opção de reduzir o nível do hook `Stop`.

### Confirmação
`./gradlew harnessCheck` agrega todos os sensores e roda no CI em todo PR.

## Prós e Contras das Opções

### Somente Android Lint + testes
- **A favor:** zero dependências extras.
- **Contra:** não verifica arquitetura nem estilo; o agente deriva com facilidade.

### Ferramentas comerciais
- **A favor:** dashboards e histórico.
- **Contra:** feedback lento e fora do loop do agente; custo.

## Mais Informações
- Segurança estendida pelo [ADR-0006](0006-code-review-e-security-scanning-com-ia.md) (SAST mobile, binário, supply chain, revisão de segurança por IA)
- [Catálogo de sensores](../harness/sensors/README.md)
- [Guia de Gradle](../harness/guides/gradle.md)
