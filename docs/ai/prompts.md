# Prompts do humano

> Na íntegra, em ordem. Prompts de skill (`/android-cli`, `/feature-flow`) entram só pelo nome.
> Os Prompts 0–2 são literais. A seção "Chamadas por tarefa" lista as skills acionadas em cada tarefa da feature 001.

## Prompt 0
`/android-cli` (carrega a skill do Android CLI; verificação do ambiente)

## Prompt 1
> O projeto que nós faremos está descrito no documento @case_android.md sendo assim faça uma leitura criteriosa desse documento e me ajude a juntos das ferramentas que nós adicionamos a esse projeto a criar e dividir tarefas e subtarefas para atender a necessidade do app descrito no documento. O Design do app deve utilizar um design semelhante, mas não igual aos apps Cielo Smart e Cielo Lio, tente utilizar uma paleta de cores semelhante. Talvez a primeira tarefa seja somente criar ou dar o start de um projeto android limpo, a partir aproveitar e criar uma estrutura básica de CI/CD e Devops. O projeto irá para o github, a principio verifique se é possível deixar o projeto privado e depois de concluído colocamos ele como público. Não invente e nem faça suposições com respeito aos requisitos do projeto. Sobre a questão da informações do evento, como é case, podemos gerar um mock padrão json, para utilizar dentro do app. Se tiver uma sugestão melhor para mockar as informações do evento, alguma ferramenta gratuita disponivel, sejam elas remoto ou não, pode sugerir. O importante é que esse mock não deve atrapalhar o funciomento principal ou objetivo do app, se por acaso for uma ferramenta remota, temos que estar preparados para mockar local caso não funcione. O desenvolvimento deve ser o mais próximo e assertivo em relação ao objetivo do app e de seus requisitos. Conforme no documento, temos que informar e documentar como o processo foi auxiliado pela IA, demonstrando como conseguimos fazer isso. Como se trata de um case, certos processos talvez não faça sentido, pois não se trata de um app que vai para produção e iriam onerar a entrega, sendo assim se identificar que determinado ponto não faz sentido, explique o por que para mim e solicite minha aprovação para pular. Organize as tarefas e subtarefas relacionadas ao processo de desenvolvimento para serem o mais otimizadas possível, para que possamos fazer um bom uso do tempo e recursos da IA, afim de não ficarmos parados desnecessariamente. Pra isso faça uma boa organização dessas tarefas e agentes e coloque na melhor ordem de processamento possível.

## Prompt 2
> 1. Sim
> 2. Sim
> 3. Verificar depois se vai ser necessário alterar esse mock para corresponder a informações apresentadas. Exemplo: 50 ingressos disponiveis, um ingresso é comprado no app, então seria apresentado 49 ingressos. Mas isso é só um exemplo, não precisa necessariamente fazê-lo, cabe avaliar se vale pena.
> 4. Pode
> 5. Pode colocar o meu primeiro e ultimo nome Rafael Oliveira. O nome do app pode ser um genérico relacionado ao tema do Case.
> 6. Sim

## Chamadas por tarefa (feature 001)
> **Procedência:** as linhas marcadas **literal** foram informadas pelo autor. As demais foram **reconstruídas** a partir do
> `tasks.md`, do `progress.md`, do [log.md](log.md) e dos commits, na forma `/skill pasta tarefas`. O texto exato digitado
> em cada sessão não foi preservado, e as decisões do humano (respostas, aprovações) estão nas tabelas do `progress.md`.
> Pasta da spec: `docs/specs/001-venda-de-ingressos`, abreviada como `001` na coluna "Chamada".

| Etapa | Tarefas | Chamada | Procedência |
|---|---|---|---|
| Demanda e classificação (G) | — | `/feature-flow` (case + feature 001) | reconstruída |
| Spec, esclarecimento, aprovação | Fases 1–2 | `/sdd-specify 001` | reconstruída |
| Direção visual, UI Spec e prancha | Fase 2.5 | `/sdd-design 001` | reconstruída |
| Plano e ADR-0010 | Fase 3 | `/sdd-plan 001` · `/create-adr` | reconstruída |
| Tarefas | Fase 4 | `/sdd-tasks 001` | reconstruída |
| Validar o emulador Cielo | T01 | `/verify-on-device 001 T01` (Android CLI) | reconstruída |
| Catálogo, convenções e módulos | T02, T03 | `/android-feature-scaffold ticketing` | reconstruída |
| Contratos do domain | T04 | `/implement-task 001 T04` | reconstruída |
| Lote A: use cases | T05–T08 | `/implement-parallel 001 T05,T06,T07,T08` | reconstruída |
| Lote B: design system | T09–T11 | `/implement-parallel 001 T09,T10,T11` · `/review-changes 001` | reconstruída |
| Lote C: dados | T12–T14 | `/implement-parallel 001 T12,T13,T14` · `/review-changes 001` | reconstruída |
| Gateway Cielo, callback e serviço | T15 | `/implement-task 001 T15` · `/security-scan` | reconstruída |
| Lote D: ViewModels | T16–T19 | `/implement-parallel 001 T16,T17,T18,T19` | **literal** |
| Lote E: telas | T20–T23 | `/implement-parallel 001 T20,T21,T22,T23` · `/review-changes 001` | reconstruída |
| Navegação | T24 | `/implement-task 001 T24` | reconstruída |
| Journeys no AVD com `lio-emulator` | T25 | `/verify-on-device 001 T25` | **literal** |
| Aprovação tardia (BR-29) e extras | T29, T30 | `/implement-task 001 T29` · `/implement-task 001 T30` | reconstruída |
| Revisão final | T26 | `/review-changes 001 T26` | **literal** |
| Análise de consistência | T27 | `/consistency-check 001 T27` | **literal** |
| Fechamento: `verify.sh full`, spec Implementada | T28 | `/verify full` | reconstruída |
| PR | — | `/open-pr` (PR #7) | acionada manualmente pelo autor |

### Prompt de handoff da T27 (resumo)
A T27 foi disparada com um prompt de handoff: estado de partida (T01–T26 e T29–T30 concluídas), leitura obrigatória (AGENTS.md,
constitution, guias, spec, plano, tasks), as ações (traceability, `spec-consistency-analyzer`, cruzamentos) e as **proibições**
(não editar `build-logic/`, `config/`, `quality/`, `scripts/harness/` nem CI; não enfraquecer sensores; sem dependência nova;
sem publicar nem alterar o remoto; não ler `local.properties`). O agente parou antes do commit para pedir aprovação, e o humano
autorizou os passos seguintes, incluindo a exclusão de cobertura do `ui` do
[ADR-0011](../adr/0011-cobertura-do-ui-exclui-composables-e-previews.md).

