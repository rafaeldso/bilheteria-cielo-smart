# Uso de IA no projeto

> O case avalia **como** a IA foi usada. Esta pasta registra isso enquanto acontece, não
> reconstruído no fim. Na entrega (D3), vira a seção de IA do README.

| Arquivo | Conteúdo |
|---|---|
| [log.md](log.md) | Um registro por etapa/checkpoint: pedido, skill/agente, modelo, resultado, sensores, correções |
| [prompts.md](prompts.md) | Prompts do humano, na íntegra, na ordem em que foram feitos |

## Como a IA foi orientada (harness)
- **Instruções:** [AGENTS.md](../../AGENTS.md) (fonte única, lida por todas as ferramentas) e [CLAUDE.md](../../CLAUDE.md).
- **Regras inegociáveis:** [constitution.md](../constitution.md); exceções só com ADR.
- **Processo:** Spec-Driven Development com checkpoints humanos ([ADR-0001](../adr/0001-adotar-spec-driven-development.md)) e workflows agênticos ([ADR-0004](../adr/0004-adotar-workflows-agenticos-orquestrados.md)).
- **Restrições de execução:** hooks e permissões em `.claude/` (bloqueio de push, segredos e arquivos gerados; sensor rápido a cada edição).
- **Sensores:** `scripts/harness/verify.sh` (detekt, Konsist, lint, testes) decide se algo está pronto, não a IA.
- **Papéis:** quem implementa não aprova; revisores são subagentes somente leitura; o humano aprova cada fase.
- **Custo:** subagentes em `sonnet`; modelo de topo só com aprovação ([roteamento](../harness/workflows/README.md#roteamento-de-modelo-e-esforço)).
