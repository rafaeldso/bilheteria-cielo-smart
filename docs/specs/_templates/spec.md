# Spec: <Nome da Feature>

| Campo | Valor |
|---|---|
| **ID** | NNN |
| **Status** | Rascunho · Em revisão · Aprovada · Implementada · Obsoleta |
| **Versão** | 0.1.0 |
| **Módulo** | `feature/<nome>` (usado pelo sensor de rastreabilidade) |
| **Autor** | |
| **Links** | Ticket · Design ([ui-spec.md](ui-spec.md) · [prancha](design/mockups.html)) · Documentos relacionados |

> Esta spec descreve **o quê** e **por quê**. Detalhes de implementação ficam no `plan.md`.
> Marque qualquer ponto indefinido com `[PRECISA ESCLARECIMENTO: ...]`. A spec não
> pode ser aprovada enquanto houver marcadores abertos.

## 1. Visão Geral
**Problema:** <qual dor ou necessidade esta feature resolve>

**Objetivo:** <resultado esperado>

**Métricas de sucesso:** <como saberemos que funcionou, ex.: taxa de conversão, redução de erros>

## 2. Histórias de Usuário
Siga o critério INVEST (independente, negociável, valiosa, estimável, pequena, testável).

| ID | História | Prioridade (MoSCoW) |
|---|---|---|
| US-01 | Como <persona>, quero <ação> para <benefício>. | Deve ter |

## 3. Escopo
**Dentro do escopo:**
-

**Fora do escopo:**
-

## 4. Regras de Negócio (EARS)
Use um dos cinco padrões EARS:
- **Ubíquo:** O sistema deve <resposta>.
- **Evento:** QUANDO <gatilho>, o sistema deve <resposta>.
- **Estado:** ENQUANTO <estado>, o sistema deve <resposta>.
- **Indesejado:** SE <condição indesejada>, ENTÃO o sistema deve <resposta>.
- **Opcional:** ONDE <feature/configuração presente>, o sistema deve <resposta>.

| ID | Regra | História | Prioridade |
|---|---|---|---|
| BR-01 | QUANDO o usuário tocar em "Enviar", o sistema deve validar o CPF informado. | US-01 | Deve ter |

## 5. Critérios de Aceite (Gherkin)
Cada critério vira pelo menos um teste automatizado com o ID no nome.

### AC-01 — <título curto> (BR-01)
```gherkin
Dado <contexto>
Quando <ação>
Então <resultado esperado>
```

## 6. Estados de UI e Eventos
**Estados** (`sealed interface`):

| Estado | Quando ocorre | O que a UI exibe |
|---|---|---|
| Loading | | |
| Content | | |
| Empty | | |
| Error | | |

**Eventos do usuário:**

| Evento | Origem | Efeito esperado |
|---|---|---|
| | | |

## 7. Dependências e Contexto
- **Módulos afetados:**
- **APIs/endpoints:**
- **Fontes de dados:**
- **Feature flags:**
- **Dependências de outras equipes:**

## 8. Dados e Privacidade
| Dado | Classificação | Observações |
|---|---|---|
| | Público · Interno · PII (LGPD) · Dado de cartão (PCI) | |

## 9. Requisitos Não Funcionais
- **Performance:** <ex.: tela carrega em até X ms em dispositivo de referência>
- **Acessibilidade:**
- **Offline / conectividade instável:**
- **Analytics:** <eventos a registrar>
- **Segurança:** <requisitos além da constitution>

## 10. Dúvidas em Aberto
| # | Pergunta | Responsável | Status |
|---|---|---|---|
| 1 | | | Aberta |

## 11. Definition of Ready
- [ ] Problema, objetivo e métricas definidos
- [ ] Histórias seguem INVEST e têm prioridade
- [ ] Toda BR está em EARS e tem pelo menos um AC
- [ ] Todos os estados de UI e eventos estão mapeados
- [ ] Dados classificados (PII / cartão)
- [ ] Nenhum `[PRECISA ESCLARECIMENTO]` aberto
- [ ] Design disponível (quando houver UI): `ui-spec.md` aprovada (skill `sdd-design`)

## 12. Changelog
| Versão | Data | Autor | Mudança |
|---|---|---|---|
| 0.1.0 | AAAA-MM-DD | | Versão inicial |
