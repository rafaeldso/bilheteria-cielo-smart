# Prompts do humano

> Na íntegra, em ordem. Prompts de skill (`/android-cli`, `/feature-flow`) entram só pelo nome.
> Os Prompts 0–2 são literais. A seção "Chamadas por tarefa" lista as skills acionadas em cada tarefa da feature 001;
> a seção "Feature 002" traz os prompts literais da sessão do QR Code e as chamadas por tarefa.

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

## Feature 002 — QR Code do ingresso
> **Procedência:** os prompts abaixo são **literais**, na ordem da sessão de 2026-10-19. O primeiro é o prompt de handoff
> que abriu a sessão (ela não tinha o contexto da anterior); o resto são as respostas e aprovações do humano.

### Prompt 3 (handoff e demanda)
`/feature-flow` com o texto:
> Spec 002: QR Code do ingresso (item opcional do case)
>
> Você é um Engenheiro Android Sênior e Tech Lead (AGENTS.md). Responda em português do Brasil; código, identificadores e commits em inglês (Conventional Commits). Esta sessão NÃO tem o contexto da anterior: tudo de que precisa está abaixo e nos arquivos.
>
> **Objetivo.** Criar a spec 002 (`docs/specs/002-qr-code-ingresso/`), seguindo o SDD do projeto. O case (`case_android.md`) diz: "Geração de QR Code do ingresso (opcional)" e "Se implementar QR Code, o ingresso deve estar vinculado à compra concluída". Hoje o app não gera QR Code.
>
> **Estado de partida (não refaça).** Feature 001 (venda de ingressos) Implementada: spec 1.3, UI Spec 0.4.4, T01–T30 concluídas, `verify.sh full` verde, PR #7–#10 mergeados na `main`; release `v0.1.0` publicada com o APK de debug. A compra aprovada vira `Purchase` com `status = Approved`, `reference` (UUID v4) e `TransactionData`; o comprovante é a SCR-03. Domain em Kotlin puro, dados em Room, UI em Compose com o design system `Ds*`. O roadmap já prevê a D2 (`002-qr-code-ingresso`) como demanda P, "só se houver tempo".
>
> **Leitura obrigatória antes de agir.** `AGENTS.md`, `docs/constitution.md`, `docs/harness/guides/anti-patterns.md`; `docs/specs/001-venda-de-ingressos/` (spec, plan §3–§5 e §7, ui-spec SCR-03, progress); `docs/specs/_templates/`, `docs/harness/guides/security.md` e `ui-design.md`; ADR-0007 e ADR-0009; `docs/onboarding/`.
>
> **O que fazer (Fases 1 e 2 do SDD, parar no checkpoint).** (1) Usar a skill `feature-flow` para classificar o tamanho (minha expectativa: P; justifique se for M) e pedir minha confirmação. (2) Usar `sdd-specify` para criar `spec.md` a partir do template, com histórias (INVEST, MoSCoW), regras EARS (`BR-xx`), critérios Gherkin (`AC-xx` ligados a uma BR), estados de UI, eventos e classificação de dados. (3) Marcar o indefinido com `[PRECISA ESCLARECIMENTO: ...]` e fazer perguntas objetivas, com uma recomendação para cada; sugestões de pontos a decidir (sem decidir sozinho): o que o QR codifica (sem PAN, token Cielo nem PII), um QR por compra ou por ingresso, só compra `Approved`, onde aparece (SCR-03, SCR-04 ou tela própria), validação dentro ou fora do escopo, aprovação tardia (BR-29), biblioteca de geração. (4) Se houver UI, indicar que a próxima fase é `sdd-design`, sem iniciá-la antes da aprovação. (5) Parar no checkpoint e pedir aprovação explícita; sem código de produção, plano nem tarefas nesta sessão.
>
> **Regras.** A spec 001 é viva: não alterar BR/AC existentes; se a 002 exigir mudança na 001, parar, descrever o conflito e propor nova versão. IDs estáveis. Proporcionalidade: P = `spec.md` enxuta + `tasks.md`. Todo AC com teste que cita o ID (S-09).
>
> **Limites.** Perguntar antes de: adicionar dependência (por exemplo, ZXing); criar módulo; alterar `build-logic/`, `config/`, `quality/`, `scripts/harness/`, CI ou a constitution; mudar contrato público de `:api`; migração de banco; usar modelo de topo ou esforço `max` em subagentes. Nunca: commitar segredos ou ler `local.properties`/keystores; logar PAN, CVV, trilha ou PII; fazer push ou alterar o remoto sem pedido; enfraquecer sensores. Só versões estáveis de bibliotecas, consultadas na fonte. Subagentes em `sonnet` por padrão.
>
> **Entrega.** `spec.md` (status "Rascunho") e `progress.md`; lista de perguntas abertas com recomendação; resumo curto (tamanho proposto, impacto na 001, próximo passo); se quiser commitar, mostrar a mensagem (`docs(qr-ticket): ...`, com o rodapé `Co-Authored-By`) e pedir aprovação antes.

### Prompts 4 a 14 (respostas, decisões e aprovações)
> 4. pode seguir as recomendações que você sugeriu nas perguntas abertas de 1 a 10
> 5. adicione ao escopo a parte: Envio ou compartilhamento do ingresso (e-mail, WhatsApp, PDF, impressão em lio://print).
> 5a. (resposta às perguntas do agente) canais: "Impressão lio://print (Recomendado)" e "Compartilhar (WhatsApp/e-mail via share sheet)"; tamanho: "Sim, M (Recomendado)"
> 6. sobre a q11 e q12, pode seguir com a sua recomendação. Sobre a spec 0.3.0 tá aprovada.
> 7. UI Spec 002 aprovado
> 8. pode  *(iniciar o plano)*
> 9. aprovado  *(plano e mudanças de spec M1 a M4)*
> 10. aprovo  *(tasks.md)*
> 11. (resposta ao risco R1 do spike) "testar a primeira opção, se não funcionar. Testar a segunda opção." *(MediaStore; se falhar, AVD Android 11)*
> 12. pode commitar e abrir o PR
> 13. não esqueça depois de gerar uma nova tag e uma nova versão da apk, atualizando o Readme com as informações atuais, a nova tela e verificando se existe alguma informação desatualizada.
> 14. verique também a questão da documentação que o case pede, além da questão dos prompts, pode seguir o padrão da spec 001

### Chamadas por tarefa (feature 002)
> Pasta da spec: `docs/specs/002-qr-code-ingresso`, abreviada como `002`. Procedência: **literal** (informada pelo autor ou registrada na sessão) ou **reconstruída** (a partir do `tasks.md`, do `progress.md` e do [log.md](log.md)).

| Etapa | Tarefas | Chamada | Procedência |
|---|---|---|---|
| Demanda, tamanho e spec | Fases 1–2 | `/feature-flow` (Prompt 3) · `/sdd-specify 002` | **literal** |
| UI Spec e prancha | Fase 2.5 | `/sdd-design 002` (+ `ui-design-reviewer`, 2 rodadas) | **literal** |
| Plano | Fase 3 | `/sdd-plan 002` | **literal** |
| Tarefas | Fase 4 | `/sdd-tasks 002` | **literal** |
| Spike do `lio://print` e ZXing | T01 | `/implement-task 002 T01` (WebFetch, `device-tester`) | **literal** |
| Catálogo e contratos do domain | T02, T03 | `/implement-task 002 T02` · `T03` | **literal** |
| Use cases e design system | T04–T06 | `/implement-task 002 T04` … `T06` | reconstruída |
| Dados: QR, imagem, callback, arquivos, impressão, DI | T07–T11 | `/implement-task 002 T07` … `T11` | reconstruída |
| Presentation e UI | T12–T14 | `/implement-task 002 T12` … `T14` | reconstruída |
| Revisão e segurança | T15 | `/review-changes 002` · `/security-scan` (5 revisores, 2 rodadas) | **literal** |
| Journeys no AVD API 29 e 30 | T16 | `/verify-on-device 002` (`device-tester`) | **literal** |
| Fechamento: `verify.sh full`, consistência, docs | T17, T18 | `/verify full` · `/consistency-check 002` | **literal** |
| PR | — | `/open-pr` (a ser acionada pelo autor) | — |

> Implementação feita na sessão principal (sem worktrees): o repositório não tinha commit com o trabalho em andamento, e commit só sob pedido.

