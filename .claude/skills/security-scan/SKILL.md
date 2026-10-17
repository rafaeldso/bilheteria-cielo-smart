---
name: security-scan
description: Varredura de segurança das mudanças atuais — roda os scanners computacionais (Gitleaks, OSV-Scanner, zizmor, mobsfscan) e o subagente security-reviewer, classifica os achados pela triagem do projeto e corrige os bloqueantes. Use quando a mudança tocar rede, storage, auth, cripto, manifest, workflows, dependências ou dados de cartão/PII, antes de abrir PR, ou quando o usuário pedir uma revisão de segurança.
argument-hint: "[arquivos | branch base]"
---

# Varredura de segurança

Processo e severidades: `docs/security/README.md` e `docs/security/triage.md`.

1. **Escopo:** `git diff --name-only <base>...HEAD` mais arquivos não commitados (sem git: arquivos
   indicados pelo usuário ou listados no `progress.md`).
2. **Computacional primeiro:** rode `scripts/harness/security-scan.sh` e `scripts/harness/verify.sh light`.
   Ferramenta ausente aparece como `IGNORADO`: informe ao usuário, não invente o resultado dela.
3. **Inferencial:** acione o subagente `security-reviewer` com a lista de arquivos, a pasta da spec e
   os achados do passo 2 (para não repetir o que um scanner já pegou).
4. **Dependência nova?** Confirme no Maven Central/Google Maven que o `group:artifact` exato existe,
   tem release recente e não é um nome parecido com um popular (pacote alucinado/typosquatting).
   Dependência sem aprovação: pare e pergunte (AGENTS.md › Limites).
5. **Classifique** cada achado pela tabela de severidade unificada de `triage.md` e consolide:
   `Severidade | Arquivo:linha | Problema | Origem (S-xx/revisor) | Controle MASVS | Correção`.
6. **Corrija** Bloqueante e Importante dentro da spec; rode de novo os passos 2–3 (máx. 2 rodadas).
   Segredo encontrado: **pare** e peça ao humano para revogar/rotacionar (`triage.md#segredo-vazado`);
   remover do código não basta.
7. **Proibido:** suprimir regra inteira, criar baseline, editar `.github/security/` ou `REVIEW.md`
   para silenciar um achado. Falso positivo segue `triage.md#falso-positivo`.
8. Achado que uma regra determinística pegaria → proponha `harness-improve`.

Veredito: **APROVADO** ou **MUDANÇAS NECESSÁRIAS**, listando o que ficou para o humano decidir e
quais scanners não rodaram localmente.
