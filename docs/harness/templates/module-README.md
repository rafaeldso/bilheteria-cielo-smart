# `:feature:balance:domain`

<!-- Template de README de módulo (guia: docs/harness/guides/documentation.md).
     Copie para <módulo>/README.md, troque o exemplo e apague este comentário. Máx. ~60 linhas. -->

Regras de negócio da consulta de saldo do EC, em Kotlin puro.

**Spec:** [docs/specs/001-consultar-saldo](../../../docs/specs/001-consultar-saldo/spec.md) ·
**Camada:** domain · **Owners:** ver [CODEOWNERS](../../../.github/CODEOWNERS)

## API pública
| Símbolo | Para quê |
|---|---|
| `GetBalanceUseCase` | Saldo disponível do EC, incluindo valores bloqueados (BR-01) |
| `BalanceRepository` | Contrato implementado por `:feature:balance:data` |
| `Balance` | Saldo do EC, com valores em `Money` |
| `BalanceError` | Falhas de domínio da consulta (`AppError`) |

## Dependências
<!-- module-graph:start -->
<!-- Gerado no build; não edite à mão. -->
<!-- module-graph:end -->

## Invariantes
- Nenhum `import android.*` (Konsist S-06).
- Dinheiro sempre em `Money` (`BigDecimal`); nunca `Double`.

## Como testar
```bash
./gradlew :feature:balance:domain:test
```
