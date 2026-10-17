# Camada de Execução

> Ferramentas e ambientes que ligam o modelo ao mundo real, com o menor
> privilégio necessário (Constitution Art. VIII.5).

## Ferramentas
| Ferramenta | Uso pelo agente | Observação |
|---|---|---|
| Gradle Wrapper (`./gradlew`) | Build, testes, sensores | Sempre o wrapper, nunca `gradle` global |
| `scripts/harness/*.sh` | Verificação em níveis (`light`, `fast`, `standard`, `full`) | Interface estável para hooks/CI |
| **Android CLI** (`android`) | Docs oficiais, emulador, deploy, screenshot, árvore de layout, skills | Instalado: v1.0 |
| Android Studio (via `android studio …`) | Inspeções, find-usages, render de preview | Requer Studio aberto com o projeto |
| `git` | Leitura livre; commit quando pedido | Push/publicação sempre com confirmação |
| `gh` | PRs e CI | Escrita somente com pedido explícito |
| MCP (opcional) | Integrações externas | Ver [mcp.md](mcp.md) |

### Android CLI: comandos úteis para o agente
```bash
android docs "<consulta>"                 # buscar na base de conhecimento oficial
android info                              # SDK, dispositivos conectados
android emulator list | start <avd>       # gerenciar AVDs
android run                               # instalar e abrir o app no device/emulador
android screen capture -o out.png         # screenshot para inspeção visual
android layout --pretty                   # árvore de UI (validar semântica/a11y)
android input …                           # tocar, digitar, swipe
android studio render-compose-preview …   # renderizar um @Preview
android studio analyze-file <arquivo>     # inspeções do IDE
```
O ciclo **build → `android run` → `android screen capture` / `android layout`**
permite ao agente verificar a própria UI no emulador (sensor de comportamento manual-assistido).
Skill `verify-on-device` e subagente `device-tester` encapsulam esse ciclo.

Comandos `android studio` adicionais (exigem Android Studio aberto com o projeto):
`check`, `find-declaration <símbolo>`, `find-usages <símbolo>`, `version-lookup`, `open-file`.
Prefira `find-usages` a `grep` para refatorações: a resolução é semântica.

### Journeys (testes E2E em linguagem natural)
Journeys descrevem fluxos do usuário em linguagem natural, salvos no projeto, que o
agente executa no device pelo Android CLI (também no CI). O formato é definido pela skill
oficial `android-cli`; mantenha-as em `journeys/` na raiz, uma por fluxo crítico da spec,
citando o `AC-xx` no título. Consulte `android docs "journeys"` antes de criar a primeira.

### Skills oficiais recomendadas
Instaladas no bootstrap (`scripts/harness/bootstrap.sh`) com `--project=.`; ficam em
`.claude/skills/` (exposto como `.agents/skills/`) ao lado das skills do projeto.
Atualize com `android skills update --all`. Catálogo: github.com/android/skills.
Nunca edite uma skill oficial no lugar (a atualização sobrescreve): copie com outro nome.

| Skill | Por quê |
|---|---|
| `android-cli` | Uso correto do próprio CLI |
| `testing-setup` | Configuração de testes alinhada ao oficial |
| `edge-to-edge`, `adaptive` | UI moderna e adaptativa |
| `jetpack-compose-performance` | Recomposição, estabilidade |
| `navigation-3` | Navegação (se adotada) |
| `android-intent-security`, `android-permissions-security` | MASVS-PLATFORM |
| `r8-analyzer` | Build release |
| `agp-9-upgrade` | Migrações de AGP |

## Permissões (Claude Code: `.claude/settings.json`)
| Política | Exemplos |
|---|---|
| **allow** | `./gradlew` (build/test/check), `scripts/harness/*`, `android docs/info/layout/screen`, `git status/diff/log` |
| **ask** | Editar `build-logic/`, `config/`, `quality/`, `scripts/harness/`, `gradle/`, `.github/`, `.claude/`, `docs/constitution.md`; `git commit`; `android run/install` |
| **deny** | Ler/editar `local.properties`, keystores, `.env*`, `google-services.json`; `git push`; `rm -rf`; `curl`/`wget` arbitrários |

Hooks complementam as permissões com verificações que regras de padrão não cobrem
(ver `CLAUDE.md`). Outras ferramentas: Android Studio/Gemini respeita `AGENTS.md` e
`.aiexclude`; Codex, Copilot e Cursor leem `AGENTS.md`.

## Sandbox e isolamento
1. **Local:** habilite o sandbox do Claude Code (`/sandbox`) para restringir
   escrita ao diretório do projeto e rede a domínios permitidos (Maven Central,
   Google Maven, Gradle Plugin Portal, developer.android.com).
2. **Trabalho paralelo:** um `git worktree` por tarefa/agente; nada de dois agentes no mesmo diretório.
3. **CI:** agentes em CI (ex.: revisão automática de PR) usam token de leitura e
   ambiente efêmero; nunca recebem segredos de assinatura.
4. **Dispositivos:** apenas emulador ou dispositivos de teste; nunca aparelho com
   dados reais de cartão.
5. **Segredos:** injetados por variáveis de ambiente no CI; nunca disponíveis à sessão do agente.
