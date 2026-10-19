---
status: aceito
date: 2026-10-17
decisores: Rafael Oliveira
consultados: Agente de IA (Claude Code), subagente de pesquisa (spike Cielo)
informados: Avaliadores do case
---

# ADR-0010: Integração de pagamento Cielo Smart por Deep Link, com o banco local como fonte da verdade

## Contexto e Problema
O app precisa cobrar pelo ecossistema Cielo Smart / LIO e registrar o resultado (aprovada, negada, cancelada) sem cobrar
duas vezes ([spec 001](../specs/001-venda-de-ingressos/spec.md)). A Cielo oferece integração Local (SDK `order-manager`)
e por Deep Link (`lio://`). A [política de camada de abstração](https://docs.cielo.com.br/cielo-smart/docs/politica-de-integracao-camada-de-abstracao-obrigatoria)
proíbe acesso direto ao hardware e exige `lio://`; o SDK está descontinuado e só é distribuído por repositório Maven local
([research.md](../specs/001-venda-de-ingressos/research.md) §1, §7).

No Deep Link, o pagamento acontece em outro app e o resultado volta por intent em `order://response`, possivelmente depois
que o Android encerrou o nosso processo. A doc não oferece chave de idempotência. A listagem `lio://orders` devolve o
`reference` de cada pedido (research §12), o que permite descobrir o resultado de um pagamento cujo retorno se perdeu.
A integração também exige enviar `clientID` e `accessToken` em cada chamada.

## Fatores de Decisão
- Conformidade com a política da Cielo e com a stack do projeto (sem `mavenLocal`, só dependências estáveis).
- Nenhuma cobrança duplicada, mesmo com toque repetido, rotação, morte do processo ou resposta ilegível.
- Resultado nunca perdido nem forjado por outro app no aparelho.
- Domain testável sem Android nem app da Cielo.

## Opções Consideradas
- **A.** SDK Integração Local com listeners e estado em memória.
- **B.** Deep Link com estado do pagamento em memória (ViewModel).
- **C.** Deep Link com o banco local (Room) como fonte da verdade, compra gravada antes do pagamento, callback validado e recuperação por `reference`.

## Decisão
Opção escolhida: **"C"**, porque é a única que cumpre a política da Cielo e mantém o resultado consistente quando o processo morre:

1. **Abstração:** interface `PaymentGateway` no domain; implementação `CieloDeepLinkGateway` em `feature:ticketing:data`
   (pacote `cielo`), única parte do código que conhece `lio://`, Base64 e o JSON da Cielo.
2. **Fonte da verdade:** a compra é gravada como `Pending` com `reference` UUID **antes** do `lio://payment`; só uma compra
   aberta por vez. As telas observam o Room.
3. **Callback:** `CieloCallbackActivity` (exportada só para `order://response`, sem UI) decodifica a resposta, extrai apenas
   os campos do comprovante e aplica o resultado **somente** se houver compra aberta com o mesmo `reference` e valor.
   A resposta bruta nunca é logada nem persistida (ela contém `bin`, cartão mascarado e CNPJ).
4. **Recuperação:** compra aberta sem resultado (reabertura do app) ou resposta ilegível → `lio://orders` paginado
   (até 4 páginas), filtrando pelo `reference`; não encontrado → `Failed` e nova tentativa liberada.
5. **Retorno garantido:** foreground service `dataSync` ativo enquanto há pagamento aberto, como pede a FAQ da Cielo.
6. **Credenciais:** propriedades Gradle locais `cielo.clientId` / `cielo.accessToken` (nunca versionadas) viram `BuildConfig`
   do módulo data, com valores fictícios por padrão para o emulador.

### Consequências
- **Positivas:** sem dependência descontinuada; anti-duplicidade e recuperação testáveis em JVM; um único ponto de contato com a Cielo.
- **Negativas:** callback exportado é superfície de ataque (mitigada pela validação de `reference` e valor); a recuperação
  depende da ordenação de `lio://orders`, não documentada; **o `accessToken` fica extraível do APK**.
- **Melhoria futura (registrada no [roadmap](../roadmap.md#melhorias-futuras)):** provisionar `clientID`/`accessToken`
  por configuração gerenciada (Managed Configurations/MDM da Cielo Store ou backend próprio que emite credencial de curta
  duração), em vez de `BuildConfig`. Fora do escopo do case por exigir infraestrutura que o case não avalia.

### Confirmação
- Teste de arquitetura (Konsist, a criar com a feature): `lio://`, `Base64` e DTOs da Cielo só aparecem em `..data.cielo..`.
- Testes do `HandlePaymentOutcomeUseCase` com respostas forjadas (reference ou valor divergentes, sem compra aberta) e do
  `StartPurchaseUseCase` (segunda compra aberta recusada).
- detekt `SensitiveDataInLog` (S-05) e revisão do `security-reviewer` em toda mudança no pacote `cielo`.

- Callback sem `BROWSABLE` (2026-10-18, `security-reviewer`): `CieloCallbackActivity` é exportada para receber `order://response`, mas
  sem a categoria `BROWSABLE` um link ou QR em navegador não a aciona. Verificado no AVD (T25) que o retorno da Cielo chega.
  Detalhes no plano §4 e §7.
- Aprovação tardia (BR-29): resposta aprovada para uma compra `Failed`/`NotFound` só vale com `reference` e valor iguais;
  callback e recuperação são serializados pelo `PurchaseWriteLock` (plano §4).

## Prós e Contras das Opções

### A. SDK Integração Local
- **A favor:** API tipada (`PaymentListener`).
- **Contra:** viola a política de camada de abstração; descontinuado; exige `mavenLocal()` (proibido no projeto).

### B. Deep Link com estado em memória
- **A favor:** menos código.
- **Contra:** morte do processo perde o resultado; o operador pode cobrar de novo um pagamento já aprovado.

### C. Deep Link com Room como fonte da verdade
- **A favor:** consistente com morte do processo; correlação trivial (uma compra aberta); domain testável.
- **Contra:** mais peças (callback, serviço, recuperação paginada).

## Mais Informações
[plan.md](../specs/001-venda-de-ingressos/plan.md) §2–§7 · [research.md](../specs/001-venda-de-ingressos/research.md) §12 ·
[ADR-0007](0007-stack-de-bibliotecas-observabilidade-e-politicas-de-qualidade.md) · [ADR-0009](0009-escopo-de-case-itens-de-producao-fora.md)
