---
name: onboarding
description: Guia uma pessoa nova pelo repositório — tour do projeto, "onde eu mudo X?", explicação de fluxo de uma feature ou de um termo — sempre ancorado em arquivos reais, sem alterar código. Use quando alguém disser que é novo no projeto, pedir um tour, perguntar onde fica algo, como uma feature funciona ou o que significa um termo do domínio.
argument-hint: "tour | where \"<mudança>\" | flow <feature> | explain \"<termo>\""
---

# Onboarding assistido

Somente leitura: não edite arquivos, exceto se a pessoa pedir para corrigir uma lacuna de
documentação (passo final). Público: humano novo no time; responda em português, sem jargão
não explicado. Padrão de documentação: `docs/harness/guides/documentation.md`.

## Regras de resposta
1. **Ancorada:** toda afirmação cita `arquivo:linha` ou um símbolo que existe. Não achou → diga
   "não encontrei" em vez de supor.
2. **Código vence documento.** Se divergirem, mostre os dois e marque como lacuna.
3. **Curta e em camadas:** resposta em até ~15 linhas + "para ir mais fundo, leia…".
4. Busca ampla (muitos arquivos) → subagente `Explore`; traga só a conclusão.
5. Nunca leia segredos nem envie código a serviços externos.
6. Antes do bootstrap (sem código Gradle), use specs, ADRs e templates e avise que o código ainda não existe.

## tour
Leia `README.md`, `ARCHITECTURE.md`, `settings.gradle.kts` (se existir) e a lista de
`docs/specs/`. Entregue:
1. O que o app faz, em 3 linhas.
2. Mapa dos módulos existentes (tabela: módulo · propósito · spec), a partir dos `README.md` dos módulos.
3. Uma feature recomendada para estudar primeiro (a mais simples e completa) e a ordem de leitura.
4. Os 3 comandos do dia a dia (`verify.sh fast/standard`, testes de um módulo).
5. Próximo passo da [trilha](../../../docs/onboarding/README.md).

## where "<mudança>"
1. Traduza a mudança em termos do glossário (`docs/onboarding/glossary.md`) e encontre a spec dona.
2. Localize a camada: comportamento visual → `ui`; regra → `domain`; origem do dado → `data`.
3. Responda: arquivos a mudar (em ordem), testes a mudar, `BR/AC` afetados, se a spec precisa
   mudar antes (mudança de comportamento ⇒ sim) e o tamanho provável (Isento/P/M/G).

## flow <feature>
Siga um evento do toque até a rede e de volta: `Screen` → `UiEvent` → `ViewModel` → use case →
repositório → data source → mapper → `UiState`. Entregue um `sequenceDiagram` Mermaid com os
nomes reais e uma tabela `estado da spec → onde é produzido → @Preview`.

## explain "<termo>"
Glossário primeiro; depois onde o termo aparece em specs e código (identificador em inglês).
Termo ausente do glossário → é lacuna.

## Fechamento (sempre)
Liste lacunas encontradas (documento ausente, desatualizado ou contraditório) com o nível certo
da tabela do guia de documentação e pergunte se a pessoa quer abrir um PR `docs(...)` corrigindo.
