---
status: aceito
date: 2026-10-19
decisores: Rafael Oliveira
consultados: Agente de IA (Claude Code), subagentes de revisão e de dispositivo (sonnet)
informados: Avaliadores do case
---

# ADR-0012: QR Code do ingresso derivado da compra, impresso por imagem e compartilhado por `FileProvider`

## Contexto e Problema
O case pede, como item opcional, o QR Code do ingresso vinculado à compra concluída ([spec 002](../specs/002-qr-code-ingresso/spec.md)).
O humano trouxe ao escopo a impressão no terminal (`lio://print`) e o compartilhamento. Três decisões valem além da feature: de onde vem
o conteúdo do QR Code, como a imagem chega à impressora da Cielo e como ela sai do app para outros apps. A doc da Cielo
([Impressão](https://docs.cielo.com.br/cielo-smart/docs/impressao.md)) não tem operação de QR Code: só texto e **imagem por caminho de arquivo**
(384 px), e a resposta `{code, message}` usa os mesmos `code` 1 e 2 do erro de pagamento.

## Fatores de Decisão
- Nenhum dado de cartão, da transação nem PII no QR Code, na imagem ou no texto compartilhado (Art. V).
- Sem migração do banco e sem nova permissão do app; nada que o case não avalie.
- Impressão de vários ingressos sem duplicar papel quando a impressora falha no meio.
- Uma resposta de impressão nunca pode alterar uma compra (colisão de `code` com o pagamento).

## Opções Consideradas
- QR Code **derivado** do `reference` e persistido ou não; assinado ou não.
- Imprimir como texto, como uma imagem alta com todos os ingressos, ou **uma imagem por ingresso**.
- Entregar a imagem à Cielo por `MediaStore`, por `MANAGE_EXTERNAL_STORAGE` no nosso app ou só em `filesDir`.
- Compartilhar bitmap no `Intent`, ou por `FileProvider`.

## Decisão
1. **Conteúdo:** `bilheteria:1:<reference>:<n>`, **derivado** da compra aprovada e **não persistido**, sem assinatura: sem validador não há quem a verifique,
   e uma chave no APK não é segredo. Biblioteca: **ZXing core 3.5.4** (Java puro, sem câmera nem permissão), só no módulo `data`; o domain
   usa uma matriz própria. Gerar sem biblioteca foi descartado (Art. III.5).
2. **Impressão:** **uma imagem por ingresso**, uma chamada `lio://print` por vez, só avançando com a resposta `SUCCESS`; falha deixa
   `Failed(n impressos)` e a nova tentativa **retoma**. A imagem fica em `MediaStore.Images` (`Pictures/Bilheteria`, nome aleatório), que a partir do Android 10
   não pede permissão ao app que a cria, e é apagada ao fim do passo, no timeout e antes de cada impressão e ao abrir a tela de ingressos.
   Abaixo de API 29, a impressão é tratada como indisponível. Sem `MANAGE_EXTERNAL_STORAGE` no nosso app.
3. **Roteamento da resposta:** a `Activity` de retorno é a mesma do pagamento, então a resposta de impressão é decidida **por estado** (impressão aguardando)
   e a impressão não inicia com compra aberta; uma resposta de impressão nunca chega ao use case de pagamento.
4. **Compartilhar:** `ACTION_SEND_MULTIPLE` com PNGs em `filesDir/shared_tickets`, servidos por um `FileProvider` **não exportado** e sem permissão de armazenamento;
   o destinatário é escolhido no menu do sistema e o app não o recebe.

### Consequências
- **Positivas:** sem migração, sem permissão nova, sem PII; retomada evita ingresso duplicado; QR prova a ida e volta (o leitor do ZXing decodifica a imagem composta).
- **Negativas:** o app da Cielo precisa de acesso de leitura ao armazenamento compartilhado (`MANAGE_EXTERNAL_STORAGE`, Android 11+): sem ele a impressão falha com `EACCES`
  (verificado no AVD; no Android 10 sempre). Por alguns segundos a imagem com o QR fica legível a apps com acesso a mídia (conteúdo: só `reference` e ordem). Em terminal real o
  acesso do app Cielo não foi verificado (pendência aceita). Duas respostas seguidas (reenvio ou app malicioso) avançam o passo seguinte, sem alterar compra.

### Confirmação
Testes por AC (29 a 44), incluindo a ida e volta do QR, o roteamento do callback, a retomada e o timeout; `security-reviewer` sem pendência; Journeys J10 a J13 no AVD
([research.md](../specs/002-qr-code-ingresso/research.md) §4); `verify.sh full` verde.

## Prós e Contras das Opções
### `MANAGE_EXTERNAL_STORAGE` no nosso app
- **A favor:** seguiria o exemplo da doc da Cielo.
- **Contra:** acesso a todos os arquivos, ruim em auditoria; não resolve o Android 10 (a permissão não existe lá) nem o acesso que falta é do app Cielo.

### Uma imagem alta com todos os ingressos
- **A favor:** uma chamada só.
- **Contra:** uma falha perde tudo e impede retomar; limite de altura desconhecido (1152 px funcionou).

## Mais Informações
[Spec 002](../specs/002-qr-code-ingresso/spec.md) · [plano](../specs/002-qr-code-ingresso/plan.md) · [research](../specs/002-qr-code-ingresso/research.md) ·
[ADR-0010](0010-integracao-cielo-smart-por-deep-link.md) · [ADR-0009](0009-escopo-de-case-itens-de-producao-fora.md)
