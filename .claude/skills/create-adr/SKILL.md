---
name: create-adr
description: Registra uma decisão de arquitetura em docs/adr no formato MADR (português), com contexto, opções, decisão e confirmação verificável. Use quando uma decisão valer além de uma feature, para exceções à constitution ou ao adotar dependência estrutural.
---

# Criar ADR

1. Próximo número: `ls docs/adr/` → `NNNN` sequencial com 4 dígitos.
2. Copie `docs/adr/_template.md` para `docs/adr/NNNN-titulo-em-kebab.md`.
3. Status inicial `proposto`; `decisores` = usuário. Nunca marque `aceito` sem aprovação explícita.
4. Contexto em 2–3 parágrafos; ao menos 2 opções reais; decisão ligada aos fatores.
5. **Confirmação**: como verificar automaticamente (sensor, regra Konsist/detekt, teste). Se não houver, proponha um.
6. Se alterar a constitution: incremente a versão (MAJOR/MINOR/PATCH) e adicione linha no Histórico.
