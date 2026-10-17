# Guia: Documentação e Code Summarization

> Como o projeto se explica para pessoas e agentes sem documentação morta.
> Decisão: [ADR-0005](../../adr/0005-documentacao-em-camadas-e-onboarding-assistido-por-ia.md).
> Base: [Diátaxis](https://diataxis.fr/), ARCHITECTURE.md (matklad), módulos documentados do
> [Now in Android](https://github.com/android/nowinandroid), [KDoc/Dokka](https://kotlinlang.org/docs/kotlin-doc.html).
> Ferramentas: skills `summarize` e `onboarding`.

## Princípios
1. **Zoom, não volume.** Cada nível responde uma pergunta; o leitor desce só até onde precisa.
2. **Código é a fonte; o resto resume.** Nada que possa ser gerado (grafo de módulos, lista de
   classes, referência de API) é mantido à mão.
3. **Documente o que o código não diz:** porquê, invariantes, o que é proibido, o que foi
   descartado. O *como* já está no código.
4. **Resumo desatualizado é bug.** Ele muda no mesmo PR que o invalida.
5. **Um tipo por documento** (Diátaxis): tutorial, how-to, referência e explicação não se misturam.

## Os níveis
| Nível | Pergunta que responde | Artefato | Tipo Diátaxis | Atualize quando… |
|---|---|---|---|---|
| Repositório | O que é e por onde começo? | [README.md](../../../README.md) | — | Mudar setup ou ponto de entrada |
| Sistema | Onde fica cada coisa e o que nunca acontece? | [ARCHITECTURE.md](../../../ARCHITECTURE.md) | Explicação | Criar camada, tipo de módulo ou preocupação transversal |
| Aprendizado | Como fico produtivo? | [docs/onboarding/](../../onboarding/README.md) | Tutorial | Mudar o fluxo de trabalho |
| Tarefa | Como faço X? | [CONTRIBUTING.md](../../../CONTRIBUTING.md), skills | How-to | Mudar processo |
| Feature | O que deve fazer e por quê? | `docs/specs/NNN/spec.md` | Referência | Mudar comportamento (antes do código) |
| Módulo | Para que serve e o que expõe? | `<módulo>/README.md` | Referência | Mudar API pública ou dependências |
| Código | Como uso este tipo? | KDoc | Referência | Mudar assinatura ou contrato |
| Mudança | O que mudou e como revisar? | Commit + descrição do PR | — | Sempre |
| Decisão | Por que é assim? | `docs/adr/` | Explicação | Decisão que vale além de uma feature |
| Vocabulário | O que significa este termo? | [glossary.md](../../onboarding/glossary.md) | Referência | Termo novo em spec ou código |

## README de módulo
Obrigatório em todo módulo Gradle. Copie [templates/module-README.md](../templates/module-README.md).
- **Até ~60 linhas:** propósito (1 frase), spec de origem, API pública, dependências,
  invariantes e como testar.
- **Grafo de dependências gerado** entre os marcadores `<!-- module-graph:start -->` e
  `<!-- module-graph:end -->`, nunca editado à mão. Fonte: a mesma configuração do
  sensor S-12 (module graph assertion), definida no bootstrap.
- Liste só a **API pública** (o que é `public`); o que é `internal` se descobre pelo IDE.
- É o primeiro arquivo que o agente lê ao entrar no módulo: escreva para os dois públicos.

## KDoc
| Onde | Regra |
|---|---|
| `feature:*:api` e tipos `public` do `domain` | **Obrigatório** em classes, interfaces e funções públicas |
| Use cases | Uma frase com o que faz para o negócio + `BR-xx` que implementa |
| Interfaces de repositório | Contrato: o que emite, quando, quais `AppError` retorna |
| `internal`/`private` | Só quando o porquê não é óbvio |
| Composables de tela | Não precisa: a spec e os `@Preview` documentam os estados |

```kotlin
/**
 * Calcula o valor líquido de uma venda após a taxa (MDR) do plano do EC.
 *
 * Implementa BR-04 de docs/specs/003-saldo-recebiveis. Valores sempre em `Money`.
 * @return [DataResult.Failure] com [AppError.Validation] se o plano estiver inativo.
 */
class CalculateNetAmountUseCase(…)
```

Regras de comentário no código:
- Explique o **porquê** (`// A API devolve 204 quando o EC não tem vendas no dia; tratamos como vazio.`).
- `TODO` sempre com dono rastreável: `TODO(003-saldo-recebiveis): remover após BR-07 entrar.`
- Nunca código comentado, nunca histórico ("alterado por…": isso é o git).

## Resumos gerados por IA
A skill `summarize` gera resumos de módulo, fluxo ou mudança. Regras:
1. **Ancorado:** toda afirmação cita arquivo ou símbolo existente. Sem citação, não entra.
2. **Revisado:** resumo que vira arquivo entra por PR e passa por revisão humana como código.
3. **Gerado, não copiado:** o resumo aponta para a fonte em vez de transcrevê-la (sem colar
   tabelas da spec, sem listar classes).
4. **Confidencialidade:** código deste repositório **não** é enviado a serviços externos de
   indexação ou wiki automática (DeepWiki, Code Wiki, upload de *pack* do Repomix etc.)
   sem aprovação de segurança. Contexto PCI DSS. Use só os agentes já aprovados na
   [camada de execução](../execution/README.md).
5. **Pergunta respondida duas vezes vira documento.** Se o `/onboarding` responde a mesma
   dúvida para pessoas diferentes, a resposta vai para o nível certo da tabela acima.

## O que não fazer
| Prática | Por quê não |
|---|---|
| Wiki fora do repositório | Desatualiza; não passa por review; agentes não leem |
| `AGENTS.md` aninhado por módulo | Duplica `.claude/rules/` e os guias (fonte única) |
| `llms.txt` | Feito para sites públicos; aqui o `AGENTS.md` cumpre o papel |
| Diagrama em imagem binária | Não versiona diff; use Mermaid |
| Documentar cada classe | Ruído; esconde o que importa |
| README de feature repetindo a spec | A spec é a fonte; o README aponta para ela |
