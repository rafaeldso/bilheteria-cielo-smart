# Constitution do Projeto

> Regras inegociáveis do projeto. Todo plano técnico deve ser validado contra este
> documento (Constitution Check). Exceções exigem um ADR aprovado.

**Versão:** 1.2.0 · **Última atualização:** 2026-10-17

## Artigo I — Spec como fonte da verdade
1. Nenhum código de produção é escrito sem spec aprovada, exceto nos casos isentos
   definidos no `CLAUDE.md`.
2. Código, testes e UI devem ser rastreáveis até os IDs da spec (`BR-xx`, `AC-xx`).
3. A spec é viva: mudanças de comportamento atualizam a spec antes do código.

## Artigo II — Arquitetura
1. Clean Architecture com dependências na direção `presentation → domain ← data`.
2. A camada **domain** é Kotlin puro, sem dependências do Android.
3. Código modularizado: features não dependem diretamente de outras features;
   a comunicação passa por módulos de API/contratos.
4. UI com fluxo de dados unidirecional (UDF): `ViewModel` expõe um único
   `StateFlow<UiState>` e recebe eventos por um ponto de entrada único.
5. Composables de tela são stateless (state hoisting).

## Artigo III — Stack
1. Kotlin, Coroutines/Flow e Jetpack Compose.
2. Seguir as bibliotecas e convenções já adotadas no projeto (DI, testes, rede,
   persistência). Novas dependências exigem aprovação explícita e, se forem
   estruturais, um ADR.
3. Dispatchers sempre injetados, nunca fixos no código.
4. Somente versões estáveis de bibliotecas e plugins. Pré-release só com ADR.
5. Soluções maduras de mercado são avaliadas (prós e contras) antes de qualquer
   implementação própria de infraestrutura.

## Artigo IV — Testes
1. Todo critério de aceite (`AC-xx`) tem pelo menos um teste automatizado que
   cita o ID no nome.
2. Pirâmide de testes: maioria unitária (domain e presentation), integração para
   a camada data e testes de UI apenas para fluxos críticos.
3. Testes devem ser determinísticos: sem dependência de rede, relógio ou ordem
   de execução.

## Artigo V — Segurança e Privacidade
1. Seguir o **OWASP MASVS** como baseline de segurança mobile.
2. Dados de cartão seguem o **PCI DSS**: nunca armazenar, logar ou expor PAN,
   CVV ou trilha. Usar tokenização quando aplicável.
3. Dados pessoais seguem a **LGPD**: coletar o mínimo necessário, não logar PII
   e respeitar o consentimento.
4. Segredos nunca ficam no código-fonte nem no repositório.
5. Comunicação de rede apenas via TLS, com as políticas de segurança de rede
   do projeto.

## Artigo VI — Qualidade
1. Build, testes e análise estática (lint, detekt, ktlint, se configurados)
   devem passar antes de uma tarefa ser considerada pronta.
2. Acessibilidade é requisito, não melhoria: content descriptions, áreas de
   toque mínimas e suporte a fontes ampliadas.
3. Erros são tratados explicitamente e mapeados para estados de UI. Nada de
   falhas silenciosas.
4. Observabilidade desde a primeira release (crash, ANR, performance), sem PII nem
   dado de cartão, e release sempre ofuscado com R8.
5. Todo arquivo criado no projeto leva o cabeçalho de licença Apache 2.0.

## Artigo VII — Simplicidade
1. Implementar apenas o que a spec pede. Sem abstrações especulativas.
2. Preferir soluções existentes no projeto a criar novas.
3. A documentação é proporcional ao tamanho da mudança.

## Artigo VIII — Harness de IA
1. Agentes de IA seguem o `AGENTS.md` e os guias em `docs/harness/guides/`.
   Regras vivem em um único lugar; arquivos de ferramenta apenas os importam.
2. Uma tarefa só está pronta quando os sensores do harness passam
   (`scripts/harness/verify.sh standard`). O resultado é executado, nunca presumido.
3. É proibido enfraquecer um sensor para fazê-lo passar (desativar regra, criar
   baseline, `@Suppress`, `@Ignore`, reduzir limite de cobertura) sem aprovação
   humana explícita e justificativa registrada no código ou no PR.
4. Todo erro recorrente de um agente vira melhoria de guia ou de sensor,
   registrada em `docs/harness/steering-loop.md`.
5. Agentes operam dentro das permissões da camada de execução. Ações destrutivas,
   irreversíveis ou externas (push, publicação, alteração de segredos) exigem
   confirmação humana.
6. Código gerado por IA passa pela mesma revisão que código humano, e o PR
   declara o uso de IA.
7. Modelo e esforço proporcionais à tarefa. Modelo de topo ou esforço máximo em
   subagentes exige aprovação humana prévia.

## Governança
- Alterações nesta constitution exigem um ADR e incremento de versão:
  MAJOR para remoção ou mudança de regra, MINOR para nova regra, PATCH para
  ajuste de texto.

## Histórico
| Versão | Data | Mudança | ADR |
|---|---|---|---|
| 1.0.0 | 2026-10-16 | Versão inicial | [ADR-0001](adr/0001-adotar-spec-driven-development.md) |
| 1.1.0 | 2026-10-16 | Artigo VIII — Harness de IA | [ADR-0002](adr/0002-adotar-harness-engineering-para-agentes-de-ia.md) |
| 1.2.0 | 2026-10-17 | III.4–5, VI.4–5, VIII.7: versões estáveis, reuso, observabilidade, licença, custo de IA | [ADR-0007](adr/0007-stack-de-bibliotecas-observabilidade-e-politicas-de-qualidade.md) |
