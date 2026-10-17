---
name: summarize
description: Gera resumos ancorados no código — README de módulo (a partir do template), explicação de um fluxo, ou descrição de PR e guia de revisão de uma mudança. Use quando pedirem para documentar/resumir um módulo, explicar como algo funciona, descrever um PR ou quando um módulo novo não tiver README.
argument-hint: "module <caminho> | flow <tema> | change [branch base]"
---

# Code summarization

Padrão obrigatório: `docs/harness/guides/documentation.md` (seção *Resumos gerados por IA*).
Toda afirmação cita arquivo ou símbolo existente; o resumo aponta para a fonte, não a transcreve.

## module <caminho>
1. Leia o `build.gradle.kts` do módulo, as declarações de nível superior sem `internal`/`private`
   em `src/main` (prefira `android studio find-usages` para saber quem as consome), os testes e a
   spec citada neles (`AC-xx`).
2. Preencha `docs/harness/templates/module-README.md` em `<caminho>/README.md`:
   propósito em 1 frase, spec, API pública (só `public`), invariantes reais (verificadas por
   sensor ou testes), comando de teste. Máx. ~60 linhas.
3. Preserve o bloco entre `<!-- module-graph:start -->` e `<!-- module-graph:end -->`; nunca o escreva à mão.
4. README já existe → atualize só o que divergiu e mostre o diff. Mudança de README entra no PR
   da mudança de código que a causou (`docs(<escopo>): ...` só se for PR exclusivo de docs).

## flow <tema>
Explicação no chat (não cria arquivo): até ~20 linhas + `sequenceDiagram` Mermaid com nomes
reais. Se a mesma explicação já foi pedida antes, sugira o nível de documentação onde ela deve morar.

## change [base]
1. Escopo: `git diff --stat <base>...HEAD` (padrão `origin/main`) + `git log <base>..HEAD --oneline`.
2. Redija a descrição no formato de `.github/pull_request_template.md`, com ênfase em:
   - **Resumo:** o porquê em 2–3 linhas, ligado aos `AC-xx`.
   - **Como revisar:** ordem de leitura dos arquivos (contratos e testes primeiro), o que
     merece atenção e o que é mecânico (renomeações, gerados, snapshots).
   - **Riscos e rollback:** o que pode quebrar e como reverter (flag, revert simples).
   - **Título** em Conventional Commits (vira o commit do squash).
3. Se o diff de produção passar de ~400 linhas ou misturar intenções, proponha como dividir.
4. Entregue o texto; não abra o PR (isso é `open-pr`, manual).
