# Glossário

> Um termo por linha, em ordem alfabética. Use exatamente estes nomes em specs, código
> (identificadores em inglês, coluna *No código*) e conversas. Termo novo numa spec → entra aqui
> no mesmo PR. **Sensível** indica dado que segue PCI DSS ou LGPD ([security.md](../harness/guides/security.md)).

## Domínio de pagamentos
| Termo | Significado | No código | Sensível |
|---|---|---|---|
| Adquirente | Empresa que processa e liquida as transações de cartão para o lojista | `Acquirer` | — |
| Agenda de recebíveis | Valores futuros que o EC tem a receber, por data | `Receivables` | — |
| Antecipação | Receber antes do prazo valores da agenda, com desconto de taxa | `Prepayment` | — |
| Bandeira | Rede do cartão (Visa, Mastercard, Elo…) | `CardBrand` | — |
| BIN | Primeiros 6–8 dígitos do cartão; identificam emissor e bandeira | `bin` | Parcial |
| Chargeback | Contestação de uma venda pelo portador, revertendo o valor | `Chargeback` | — |
| CVV | Código de segurança do cartão. **Nunca** armazenado ou logado | — | **PCI** |
| EC | Estabelecimento Comercial: o lojista cliente | `Merchant` | — |
| Emissor | Banco que emitiu o cartão do portador | `Issuer` | — |
| Liquidação | Pagamento efetivo ao EC na data prevista | `Settlement` | — |
| MDR | *Merchant Discount Rate*: taxa descontada do EC por venda | `mdr` | — |
| PAN | Número completo do cartão. Só mascarado (`**** 1234`) ou tokenizado | `maskedPan`, `cardToken` | **PCI** |
| PIX | Pagamento instantâneo do Banco Central | `Pix` | Chave pode ser PII |
| Portador | Pessoa dona do cartão que paga a compra | `Cardholder` | **LGPD** |
| Tokenização | Troca do PAN por um token sem valor fora do contexto autorizado | `Tokenizer` | — |
| TPV | *Total Payment Volume*: volume total transacionado no período | `tpv` | — |
| Trilha | Dados da tarja magnética/chip. **Nunca** tocados pelo app | — | **PCI** |

## Processo e engenharia
| Termo | Significado |
|---|---|
| AC-xx | Critério de aceite em Gherkin; todo AC tem teste que cita o ID |
| ADR | Registro de decisão de arquitetura (`docs/adr/`) |
| BR-xx | Regra de negócio em EARS, numa spec |
| Checkpoint | Ponto em que o humano aprova antes da próxima fase do SDD |
| Constitution | Regras inegociáveis do projeto ([constitution.md](../constitution.md)) |
| Guia | Instrução lida antes de agir (`docs/harness/guides/`) |
| Harness | Guias + sensores + execução que orientam e verificam agentes de IA |
| Isento / P / M / G | Tamanho da mudança; define os artefatos exigidos ([AGENTS.md](../../AGENTS.md#escopo-e-tamanho)) |
| SDD | Spec-Driven Development: spec → plano → tarefas → código |
| Sensor | Verificação automática (lint, testes, Konsist, revisores) que diz como corrigir |
| Skill | Fluxo repetível para agentes (`.claude/skills/`, `/nome`) |
| Steering loop | Processo que transforma erro recorrente em guia ou sensor |
| UDF | Fluxo unidirecional: estado desce (`UiState`), eventos sobem (`UiEvent`) |
