# Política de segurança

## Reportar uma vulnerabilidade
**Não abra issue pública.** Use um destes canais:
- GitHub: aba **Security → Report a vulnerability** (*private vulnerability reporting*), ou
- E-mail: `<security@empresa.com.br>` <!-- definir no bootstrap -->

Inclua: versão do app, passos para reproduzir, impacto esperado e, se possível, prova de conceito.
Não teste com dados reais de cartão ou de terceiros, não acesse dados que não sejam seus e não
degrade o serviço.

## O que esperar
| Etapa | Prazo |
|---|---|
| Confirmação de recebimento | 2 dias úteis |
| Triagem e severidade inicial | 5 dias úteis |
| Correção de severidade crítica/alta | conforme [triagem](docs/security/triage.md#sla-de-correção) |
| Retorno a quem reportou | a cada marco, até o fechamento |

## Escopo
- Dentro: o app Android deste repositório e seus builds publicados.
- Fora: SDKs e serviços de terceiros (reporte ao fornecedor), engenharia social, ataques que
  exigem dispositivo com root controlado pelo próprio atacante sem impacto em outros usuários.

## Versões suportadas
Só a versão mais recente publicada na Play Store recebe correções.

## Para o time
Processo interno de varredura, triagem e revisão: [docs/security/](docs/security/README.md).
