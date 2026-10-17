# MCP (Model Context Protocol)

> MCP é opcional. O Android CLI cobre a maior parte do ciclo device/emulador com
> menos tokens. Adicione um servidor MCP somente quando ele trouxer algo que o CLI
> e os scripts não trazem, e trate-o como dependência (aprovação + avaliação de segurança).

## Critérios para adotar um servidor MCP
- Mantido ativamente, licença compatível, código auditável.
- Escopo mínimo (somente leitura quando possível).
- Não exige credenciais de produção.
- Fixado em versão exata (sem `latest`).

## Candidatos comuns
| Necessidade | Opção | Observação |
|---|---|---|
| Issues/PRs | GitHub MCP oficial | Somente leitura por padrão |
| Design | Figma MCP (Dev Mode) | Só com designer humano: plano gratuito tem 6 chamadas/mês e Code Connect é pago. Ver [ADR-0008](../../adr/0008-fase-de-design-com-ia-e-ponte-design-codigo.md) |
| Design (exploração) | Google Stitch MCP | Opcional para direção visual; produto Labs, cota diária, sem SLA |
| Design (open source) | Penpot MCP | Alternativa aberta ao Figma; ainda experimental |
| Device além do Android CLI | servidores ADB comunitários | Avaliar com cuidado; preferir `android` CLI |
| Documentação de bibliotecas | servidores de docs | Preferir `android docs` para APIs Android |

## Configuração no Claude Code
Arquivo `.mcp.json` na raiz (versionado), por exemplo:
```json
{
  "mcpServers": {
    "github": {
      "type": "http",
      "url": "https://api.githubcopilot.com/mcp/"
    }
  }
}
```
Registre a adoção em ADR quando o servidor fizer parte do fluxo padrão do time.
