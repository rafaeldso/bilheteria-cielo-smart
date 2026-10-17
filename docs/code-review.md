# Guia de code review

> Baseado em [Google Engineering Practices](https://google.github.io/eng-practices/review/)
> e [Conventional Comments](https://conventionalcomments.org/). Vale para revisores humanos
> e é a régua dos revisores de IA (`.claude/agents/*-reviewer.md`).

## Princípios
1. **Aprove quando o PR melhora a saúde do código**, mesmo que não esteja perfeito.
   Perfeição bloqueia; melhoria contínua não.
2. **Velocidade importa.** Primeira resposta em até **1 dia útil**. Se não puder revisar,
   diga e indique alguém.
3. **Revise o código, não a pessoa.** Pergunte antes de afirmar; explique o porquê.
4. **Sensor antes de opinião.** Formatação, estilo e regras que um sensor pega não são
   assunto de review. Se você apontou algo que uma máquina poderia ter pego, abra
   `harness-improve` em vez de repetir o comentário no próximo PR.
5. **Código de IA tem a mesma régua** (Constitution Art. VIII.6). Quem abriu o PR responde
   por ele, independentemente de quem digitou.

## Ordem de leitura
1. **Descrição e spec:** o PR resolve o `AC-xx` declarado? A spec mudou junto, se precisava?
2. **Testes:** leia antes do código. Eles provam o AC? Falhariam se o código estivesse errado?
3. **Contratos:** `UiState`, `UiEvent`, interfaces de repositório, `:api`. Erro aqui custa caro.
4. **Implementação:** do domain para a UI.
5. **Detalhes:** nomes, legibilidade, comentários.

## Checklist Android
| Área | Pergunte |
|---|---|
| Ciclo de vida | Coleta com `collectAsStateWithLifecycle`? Sobrevive a rotação e morte de processo (`SavedStateHandle`)? |
| Estado | Um único `StateFlow<UiState>`? Todos os estados da spec renderizados, inclusive erro e vazio? |
| Concorrência | Dispatcher injetado? `suspend` main-safe? Cancelamento respeitado? |
| Compose | `Screen` stateless? Parâmetros estáveis, `remember` correto, sem trabalho pesado na composição? |
| Acessibilidade | `contentDescription`, área de toque ≥ 48 dp, fonte ampliada, ordem do TalkBack? |
| Segurança | PAN/CVV/PII fora de logs e analytics? Componente `exported`? Entrada de deep link validada? |
| Erros | Cada falha da matriz do plano vira estado de UI? Nada engolido em `catch`? |
| Release | Classes serializadas/reflexão cobertas por regras R8? Strings em `strings.xml`? |
| Testes | Nome cita o `AC-xx`? Determinístico (sem rede, relógio real ou ordem)? Fake em vez de mock onde possível? |

### Sinais típicos de código gerado por IA
- API que não existe ou de versão diferente da do catálogo de versões.
- Abstração especulativa (interface com uma implementação, *helper* genérico sem segundo uso).
- Teste que não pode falhar (assert no próprio fake, `verify` sem efeito observável).
- Comportamento além da spec, "porque é melhor". Deve virar proposta de mudança de spec.
- Comentário que repete o código em vez de explicar o porquê.

## Como escrever comentários — Conventional Comments
```
<rótulo> [decoração]: <assunto>

<discussão opcional: porquê, alternativa, link>
```
| Rótulo | Uso | Bloqueia? |
|---|---|---|
| `issue` | Problema concreto. Traga uma sugestão sempre que possível | Sim, por padrão |
| `suggestion` | Melhoria proposta, com o quê e o porquê | Use `(blocking)` ou `(non-blocking)` |
| `question` | Dúvida genuína; não é crítica disfarçada | Não |
| `nitpick` | Preferência trivial | Nunca |
| `thought` | Ideia para o futuro, fora deste PR | Nunca |
| `chore` | Tarefa simples antes do merge (ex.: atualizar changelog) | Sim |
| `praise` | Reconhecimento sincero. Ao menos um por review | Não |

Exemplo:
```
issue (blocking): o CVV vai para o log quando a tokenização falha.

`TokenizeCardUseCase` interpola `request` no `Timber.e`. Use `request.masked()` (core:security).
Ref.: Constitution Art. V.2.
```

## Veredito
| Estado | Quando |
|---|---|
| **Approve** | Nenhum comentário bloqueante, ou só ajustes triviais que você confia que o autor fará |
| **Request changes** | Há `issue`/`suggestion (blocking)` ou `chore` pendente |
| **Comment** | Só perguntas; você ainda não tem opinião formada |

## Papel do autor
- Responda **todo** comentário: corrigido (com o commit), discordo (com motivo) ou fora do
  escopo (com issue/tarefa criada). Não resolva a conversa do revisor por ele.
- Discordância depois de duas trocas vai para uma conversa síncrona; o resultado volta ao PR.

## Revisão por IA
| Camada | Quem | Quando |
|---|---|---|
| Sensores | `verify.sh`, CI | Sempre |
| Revisores de IA | skill `review-changes` / `security-scan` local; rótulo `ai-review`; `ai-security-review.yml` em caminho sensível | Mudanças M/G ou sensíveis |
| Humano | 1 aprovação + code owner do caminho | Sempre; **a IA não aprova PR** |

Achado da IA é sugestão: o revisor humano decide se bloqueia. Regras dos revisores de IA de PR:
[REVIEW.md](../REVIEW.md). Política (confiança, prompt injection, dados, custo):
[docs/security/ai-review.md](security/ai-review.md). Severidade e SLA: [triage.md](security/triage.md).
