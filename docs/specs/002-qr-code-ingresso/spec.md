# Spec: QR Code do ingresso

| Campo | Valor |
|---|---|
| **ID** | 002 |
| **Status** | Implementada |
| **Versão** | 1.1.2 |
| **Tamanho** | M (era P na 0.2.0; subiu ao entrar impressão e compartilhamento, Q13) |
| **Módulo** | `feature/ticketing` (o mesmo da 001; o `plan.md` confirma onde cada peça fica) |
| **Autor** | Rafael Oliveira (com Claude Code) |
| **Links** | [case_android.md](../../../case_android.md) · [spec 001](../001-venda-de-ingressos/spec.md) (v1.3.1) · [roadmap](../../roadmap.md) (D2) · Design: [ui-spec.md](ui-spec.md) (v0.2.1) · [prancha](design/mockups.html) |

> Esta spec descreve **o quê** e **por quê**. Detalhes de implementação ficam no `plan.md`.
> Fonte dos requisitos: o case ("Geração de QR Code do ingresso (opcional)" e "Se implementar QR Code, o ingresso
> deve estar vinculado à compra concluída") e a spec 001. Os termos de status da compra (Aprovada, Negada,
> Cancelada, Falhou, Em andamento) são os da 001.
>
> As dúvidas Q1…Q10 foram respondidas pelo humano em 2026-10-19 (recomendações aceitas, seção 10). Na mesma data o
> humano trouxe ao escopo a impressão (`lio://print`) e o compartilhamento (share sheet) e confirmou o tamanho M
> (Q13); os detalhes dessas duas partes foram decididos nas perguntas (Q11, Q12), respondidas com as recomendações; o spike da Q11 (T01 e T16) confirmou o formato e os limites, ver `research.md`.

## 1. Visão Geral
**Problema:** hoje a venda aprovada termina num comprovante de pagamento; o comprador não leva nada que represente o
ingresso em si. O case pede, como item opcional, o QR Code do ingresso, vinculado à compra concluída.

**Objetivo:** a partir de uma compra **Aprovada**, gerar e exibir um QR Code por ingresso, ligado à compra pelo seu
`reference`, acessível pelo comprovante (no fim da venda ou pelo histórico), sem rede e sem expor dado de cartão nem PII;
e permitir **entregar** o ingresso ao comprador, imprimindo-o na impressora do terminal Cielo Smart ou compartilhando-o
pelo menu de compartilhamento do Android.

**Métricas de sucesso:**
- Toda compra Aprovada mostra tantos QR Codes quantos ingressos comprou; nenhuma outra compra mostra QR Code.
- O conteúdo do QR Code não contém dado de cartão, dado da transação nem PII (AC-30).
- Todo AC com teste automatizado; `verify.sh standard` verde.

## 2. Histórias de Usuário
Persona: **operador da bilheteria** (a mesma da 001).

| ID | História | Prioridade (MoSCoW) |
|---|---|---|
| US-01 | Como operador, quero que cada ingresso de uma compra aprovada tenha um QR Code ligado à compra, para entregar ao comprador um ingresso que prove a compra concluída. | Deve ter |
| US-02 | Como operador, quero que compra negada, cancelada ou que falhou não tenha QR Code, para nunca entregar ingresso sem pagamento. | Deve ter |
| US-03 | Como operador, quero abrir os QR Codes de uma compra aprovada também pelo histórico, para reapresentar o ingresso depois da venda. | Deveria ter |
| US-04 | Como operador, quero imprimir os ingressos na impressora do terminal, para entregar ao comprador um ingresso em papel. | Deveria ter |
| US-05 | Como operador, quero compartilhar os ingressos pelo menu de compartilhamento do Android (WhatsApp, e-mail e outros), para enviar o ingresso ao comprador sem o app coletar seus dados. | Deveria ter |

## 3. Escopo
**Dentro do escopo:**
- Geração do QR Code de cada ingresso de uma compra Aprovada, offline.
- Tela de ingressos com os QR Codes, aberta a partir do comprovante (venda ou histórico).
- Erro explícito quando os QR Codes não puderem ser exibidos.
- **Impressão** dos ingressos na impressora do terminal pelo Deep Link `lio://print`, com tratamento de falha.
- **Compartilhamento** dos ingressos (imagem do QR Code) pelo menu de compartilhamento do sistema, que escolhe o aplicativo e o destinatário.

**Fora do escopo:**
- **Leitura e validação do QR Code** na entrada do evento (câmera, controle de uso único, "ingresso já utilizado"):
  exige um validador compartilhado ou backend, que o case não tem (roadmap, sem backend). Vai para "Melhorias futuras".
- Assinatura criptográfica do conteúdo (sem validador, não há quem verifique; ver Q1).
- Envio feito pelo próprio app: coletar e-mail ou telefone do comprador, enviar e-mail ou mensagem (exige PII, base legal e backend), e integração direta com a API do WhatsApp. O compartilhamento é só o menu do sistema.
- Geração de arquivo PDF do ingresso.
- Imprimir o comprovante de pagamento (a 001 já o deixou fora); só os ingressos são impressos.
- Aviso ao operador de aprovação tardia (mantém a decisão da Q14 da 001).
- Alterar status, valores ou o fluxo de pagamento da 001.

## 4. Regras de Negócio (EARS)
A numeração continua a da 001 (BR-30… e AC-29…): o sensor S-09 busca `AC-xx` nos testes do módulo `feature/ticketing`, comum às duas specs, e IDs repetidos se confundiriam (Q8). As histórias (`US-xx`) têm numeração própria.

| ID | Regra | História | Prioridade |
|---|---|---|---|
| BR-30 | O sistema deve gerar, para cada compra **Aprovada**, um QR Code por ingresso, na mesma quantidade da compra. | US-01 | Deve ter |
| BR-31 | O conteúdo do QR Code deve identificar a compra pelo seu `reference` e o ingresso pelo número de ordem dentro da compra (ex.: 2 de 3), em formato com versão. O conteúdo não pode incluir número ou dado de cartão, dados da transação (`orderId`, código de autorização, NSU, bandeira), credenciais Cielo nem PII. | US-01 | Deve ter |
| BR-32 | O sistema deve produzir sempre o mesmo conteúdo para o mesmo ingresso da mesma compra, de modo que reabrir a tela mostre os mesmos QR Codes. | US-01, US-03 | Deve ter |
| BR-33 | SE a compra não estiver Aprovada (Negada, Cancelada, Falhou ou Em andamento), ENTÃO o sistema não deve gerar nem exibir QR Code dela. | US-02 | Deve ter |
| BR-34 | QUANDO o comprovante de uma compra Aprovada for exibido, seja ao fim da venda ou aberto pelo histórico, o sistema deve oferecer o acesso aos QR Codes dos ingressos; nos comprovantes de compra Negada ou Cancelada, não. | US-01, US-02, US-03 | Deve ter |
| BR-35 | QUANDO o operador abrir os ingressos, o sistema deve exibir cada QR Code com a identificação "Ingresso n de q" e o nome, a data e o local do evento da compra. | US-01 | Deve ter |
| BR-36 | SE a compra não puder ser lida, não estiver Aprovada no momento da abertura ou o QR Code não puder ser gerado, ENTÃO o sistema deve exibir erro com a opção de tentar novamente, sem exibir nenhum QR Code. | US-01, US-02 | Deve ter |
| BR-37 | QUANDO uma compra passar a Aprovada por aprovação tardia (BR-29 da 001), o sistema deve passar a oferecer os QR Codes dela como a qualquer compra Aprovada, sem aviso ao operador. | US-01, US-03 | Deve ter |
| BR-38 | QUANDO o operador solicitar a impressão numa compra Aprovada, o sistema deve enviar à impressora do terminal Cielo Smart, pelo Deep Link `lio://print`, os ingressos da compra, cada um com "Ingresso n de q", o nome, a data e o local do evento e o QR Code (BR-31). A impressão não pode incluir dados da transação nem de cartão. | US-04 | Deveria ter |
| BR-39 | ENQUANTO uma impressão estiver em andamento, o sistema deve ignorar novas solicitações de impressão e manter indisponíveis as ações de imprimir e de compartilhar. | US-04 | Deveria ter |
| BR-40 | SE a impressão não puder ser iniciada (app Cielo ausente ou aparelho sem suporte à impressão) ou a Cielo devolver erro (impressora sem papel, falha da impressora, código desconhecido ou nenhuma resposta no prazo), ENTÃO o sistema deve exibir mensagem específica do motivo, informando quantos ingressos já foram impressos, com a opção de tentar novamente; a nova tentativa deve retomar a partir do primeiro ingresso não impresso, sem reimprimir os anteriores e sem alterar a compra nem os QR Codes. | US-04 | Deveria ter |
| BR-41 | QUANDO o operador solicitar o compartilhamento numa compra Aprovada, o sistema deve abrir o menu de compartilhamento do Android com a imagem do QR Code de cada ingresso e a identificação "Ingresso n de q", evento, data e local. O app não coleta nem guarda destinatário, e-mail ou telefone. | US-05 | Deveria ter |
| BR-42 | O sistema deve compartilhar as imagens sem exigir permissão de armazenamento e dando acesso de leitura apenas ao aplicativo escolhido, apenas por essa operação; as imagens compartilhadas contêm só o que a BR-31 permite. | US-05 | Deveria ter |
| BR-44 | QUANDO a Cielo confirmar a impressão de todos os ingressos da compra, o sistema deve exibir a confirmação "Ingressos impressos" e voltar ao estado de ações disponíveis. | US-04 | Deveria ter |
| BR-43 | SE o compartilhamento não puder ser iniciado (falha ao gerar a imagem ou nenhum aplicativo disponível), ENTÃO o sistema deve exibir mensagem de erro com a opção de tentar novamente, sem alterar a compra. | US-05 | Deveria ter |

## 5. Critérios de Aceite (Gherkin)
Valores ilustrativos (nomes, quantidades) são dados de teste, não regra.

### AC-29 — Um QR Code por ingresso da compra Aprovada (BR-30, BR-31)
```gherkin
Dado uma compra Aprovada de 3 ingressos
Quando o sistema gera os QR Codes da compra
Então são gerados 3 QR Codes
E o conteúdo de cada um identifica o reference da compra e a ordem do ingresso (1 de 3, 2 de 3 e 3 de 3)
E os 3 conteúdos são diferentes entre si
```

### AC-30 — Conteúdo sem dado sensível (BR-31)
```gherkin
Dado uma compra Aprovada com bandeira, código de autorização, NSU e orderId da Cielo
Quando o sistema gera o QR Code de um ingresso
Então o conteúdo não contém nenhum desses valores
E não contém o nome do evento nem dado do comprador
```

### AC-31 — Mesmo ingresso, mesmo conteúdo (BR-32)
```gherkin
Dado uma compra Aprovada de 2 ingressos
Quando o sistema gera os QR Codes duas vezes (por exemplo, ao reabrir a tela)
Então o conteúdo do ingresso 1 e o do ingresso 2 são idênticos nas duas gerações
```

### AC-32 — Compra que não está Aprovada não tem QR Code (BR-33)
```gherkin
Dado uma compra Negada
Quando o sistema tenta gerar os QR Codes da compra
Então nenhum QR Code é gerado
Dado uma compra Cancelada, Falhou ou Em andamento
Quando o sistema tenta gerar os QR Codes da compra
Então nenhum QR Code é gerado
```

### AC-33 — Acesso aos ingressos pelo comprovante (BR-34)
```gherkin
Dado o comprovante de uma compra Aprovada, ao fim da venda
Então o operador vê a ação para abrir os ingressos
Dado o mesmo comprovante aberto pelo histórico
Então o operador vê a mesma ação
Dado o comprovante de uma compra Negada ou Cancelada
Então a ação para abrir os ingressos não é oferecida
```

### AC-34 — Tela de ingressos (BR-35)
```gherkin
Dado uma compra Aprovada de 2 ingressos do evento "Show A"
Quando o operador abre os ingressos
Então vê 2 QR Codes
E cada um com "Ingresso 1 de 2" e "Ingresso 2 de 2" e o nome, a data e o local do evento
E, enquanto os QR Codes são gerados, vê o indicador de carregamento
```

### AC-35 — Falha ao ler a compra ou gerar o QR Code (BR-36)
```gherkin
Dado que a leitura da compra falha ou que a geração do QR Code falha
Quando o operador abre os ingressos
Então vê o estado de erro com a ação "Tentar novamente" e nenhum QR Code
Quando toca em "Tentar novamente" e a operação funciona
Então vê os QR Codes dos ingressos
```

### AC-36 — Compra não Aprovada aberta pela tela de ingressos (BR-36)
```gherkin
Dado uma compra que não está Aprovada (ou que não existe)
Quando a tela de ingressos é aberta para ela
Então vê o estado de erro com a ação "Tentar novamente" e nenhum QR Code
```

### AC-37 — Aprovação tardia libera os QR Codes (BR-37)
```gherkin
Dado uma compra registrada como Falhou por "pagamento anterior não encontrado"
Quando a Cielo retorna pagamento autorizado com o reference e o valor dessa compra (BR-29 da 001)
Então a compra passa a Aprovada
E o comprovante dela, aberto pelo histórico, oferece os ingressos
E a tela de ingressos mostra um QR Code por ingresso
E nenhum aviso é exibido ao operador
```

### AC-38 — Imprimir os ingressos (BR-38)
```gherkin
Dado uma compra Aprovada de 2 ingressos do evento "Show A"
Quando o operador solicita a impressão na tela de ingressos
Então o sistema envia à impressora do terminal, pelo lio://print, 2 ingressos
E cada um com "Ingresso 1 de 2" e "Ingresso 2 de 2", o nome, a data e o local do evento e o QR Code
E o conteúdo enviado não contém dados da transação nem de cartão
```

### AC-39 — Toque repetido não reimprime (BR-39)
```gherkin
Dado uma impressão em andamento
Quando o operador toca em imprimir novamente (ou a tela é recriada)
Então nenhuma nova impressão é enviada
E as ações de imprimir e de compartilhar continuam indisponíveis até a impressão terminar
```

### AC-40 — Falha na impressão e retomada (BR-40)
```gherkin
Dado que o app de pagamento da Cielo não está instalado, ou que o aparelho não suporta a impressão
Quando o operador solicita a impressão
Então vê a mensagem de que a impressão não está disponível no aparelho, com "Tentar novamente"
Dado uma compra Aprovada de 3 ingressos em que a impressora fica sem papel depois do ingresso 1
Quando a Cielo devolve o erro de falta de papel
Então vê uma mensagem específica de falta de papel, informando que 1 de 3 ingressos foi impresso, com "Tentar novamente"
E a compra e os QR Codes continuam inalterados
Quando coloca papel e toca em "Tentar novamente"
Então só os ingressos 2 e 3 são enviados à impressora
Dado que a Cielo devolve erro da impressora ou um código desconhecido
Quando a impressão termina
Então vê uma mensagem específica do erro, com a contagem de ingressos impressos e "Tentar novamente"
```

### AC-44 — Confirmação da impressão (BR-44)
```gherkin
Dado uma compra Aprovada de 2 ingressos com a impressão em andamento
Quando a Cielo confirma a impressão do ingresso 2
Então vê a confirmação "Ingressos impressos"
E as ações de imprimir e de compartilhar voltam a ficar disponíveis
```

### AC-41 — Compartilhar os ingressos (BR-41)
```gherkin
Dado uma compra Aprovada de 2 ingressos
Quando o operador solicita o compartilhamento na tela de ingressos
Então o menu de compartilhamento do Android abre com 2 imagens de QR Code
E com a identificação de cada ingresso ("Ingresso n de 2"), o evento, a data e o local
E o app não pede nem guarda destinatário, e-mail ou telefone
```

### AC-42 — Compartilhamento sem permissão de armazenamento e sem dado sensível (BR-42)
```gherkin
Dado uma compra Aprovada com bandeira, código de autorização, NSU e orderId da Cielo
Quando o operador compartilha os ingressos
Então nenhuma permissão de armazenamento é solicitada
E o acesso de leitura às imagens é concedido só ao aplicativo escolhido, para essa operação
E as imagens e o texto compartilhados não contêm nenhum desses valores
```

### AC-43 — Falha no compartilhamento (BR-43)
```gherkin
Dado que a imagem do QR Code não pode ser gerada para compartilhar, ou que nenhum aplicativo aceita o compartilhamento
Quando o operador solicita o compartilhamento
Então vê uma mensagem de erro com "Tentar novamente"
E a compra e os QR Codes continuam inalterados
```

## 6. Estados de UI e Eventos
**Tela de ingressos** (`TicketsUiState`; tela nova, **SCR-05** provisória, a UI Spec confirma):

| Estado | Quando ocorre | O que a UI exibe | AC |
|---|---|---|---|
| `Loading` | Leitura da compra e geração dos QR Codes em curso | Indicador de carregamento | AC-34 |
| `Content` | Compra Aprovada e QR Codes gerados | Um QR Code por ingresso, com "Ingresso n de q" e dados do evento | AC-29, AC-31, AC-34, AC-37 |
| `Error` | Compra ilegível, não Aprovada, inexistente, ou geração falhou | Mensagem + "Tentar novamente" | AC-35, AC-36 |

Dentro de `Content`, as ações de imprimir e compartilhar têm o seu próprio estado (`TicketActionState`), que não
substitui o conteúdo: os QR Codes continuam na tela.

| `TicketActionState` | Quando ocorre | O que a UI exibe | AC |
|---|---|---|---|
| `Idle` | Nenhuma ação em curso | Ações "Imprimir" e "Compartilhar" disponíveis | AC-38, AC-41 |
| `Printing` | Impressão enviada, sem retorno | "Imprimindo…"; "Imprimir" e "Compartilhar" indisponíveis | AC-38, AC-39 |
| `PrintFailed(reason, printed, total)` | App Cielo ausente ou aparelho sem suporte, sem papel, erro da impressora ou desconhecido (`reason`), com a contagem de ingressos já impressos | Mensagem específica + contagem + "Tentar novamente" (retoma do primeiro não impresso) | AC-40 |
| `Printed` | A Cielo confirmou todos os ingressos | Confirmação "Ingressos impressos"; volta a `Idle` | AC-44 |
| `ShareFailed` | Falha ao gerar a imagem ou nenhum aplicativo disponível | Mensagem + "Tentar novamente" | AC-43 |

**Tela de comprovante** (`ReceiptUiState`, da 001): o estado `Approved` passa a exibir a ação "Ver ingressos" (AC-33).
`Declined` e `Cancelled` não mudam. Ver "Impacto na 001" na seção 7.

**Eventos:**

| Evento | Origem | Efeito esperado |
|---|---|---|
| `OnOpenTickets` | Comprovante (`Approved`), evento novo na tela da 001 | Abre a tela de ingressos da compra |
| `OnRetry` | Tela de ingressos (`Error`) | Repete a leitura e a geração dos QR Codes |
| `OnPrint` | Tela de ingressos (`Content`) | Envia os ingressos à impressora do terminal (ignorado se `Printing`) |
| `OnShare` | Tela de ingressos (`Content`) | Abre o menu de compartilhamento do Android com as imagens |
| `OnRetryAction` | Tela de ingressos (`PrintFailed`, `ShareFailed`) | Repete a última ação (imprimir ou compartilhar) |
| `OnPrintResult(response)` | Retorno da Cielo do `lio://print` (`{code, message}`: 0 sucesso, 1 sem papel, 2 erro). Entra pela `Activity` de retorno e pelo estado do trabalho de impressão (`PrintStatus`), não pelo `onEvent` da tela | Imprime o próximo ingresso, vai a `Printed` ou a `PrintFailed` |

"Voltar" é navegação tratada na `Route`, não `UiEvent` (como na 001).

## 7. Dependências e Contexto
- **Módulos afetados:** `feature:ticketing:domain` (regra do conteúdo e do vínculo), `feature:ticketing:ui` (tela nova e ação no comprovante), `feature:ticketing:api` (rota nova), `feature:ticketing:data` (impressão pelo Deep Link e arquivos para compartilhar), `app` (manifest, se o retorno da impressão exigir), `core:designsystem` (componente do QR Code, se a UI Spec pedir). Nenhum módulo novo previsto. `core:database` **sem mudança**: os QR Codes são derivados do `reference`, não persistidos (sem migração do Room v1).
- **APIs/endpoints:** Deep Link Cielo Smart `lio://print` (`PRINT_IMAGE` por caminho de arquivo, 384 px; resposta `{code, message}`; ver `research.md`), atrás de interface de domínio como o `PaymentGateway` (a Cielo exige camada de abstração). Share sheet do Android (`ACTION_SEND`/`ACTION_SEND_MULTIPLE`) com `FileProvider` de paths específicos. Sem rede, sem `INTERNET`, sem câmera, sem permissão de armazenamento.
- **Fontes de dados:** a compra Aprovada já registrada (Room), pelo `PurchaseRepository` da 001.
- **Feature flags:** nenhuma.
- **Dependência externa nova (precisa de aprovação):** biblioteca de geração de QR Code. Recomendação: ZXing core (Q7). Não consta na stack de referência do ADR-0007.
- **Restrição da impressão (spike T01/T16):** o `PRINT_IMAGE` lê o arquivo por caminho, então o app Cielo precisa de acesso de leitura ao armazenamento compartilhado (`MANAGE_EXTERNAL_STORAGE`, Android 11+). Sem ele a impressão falha com `EACCES` e o operador vê a mensagem de erro de impressão (BR-40, motivo desconhecido) com a nova tentativa; no AVD Android 10 isso sempre ocorre. O compartilhamento e o QR Code não dependem disso.
- **Dependências externas existentes:** app UriApp da Cielo / `lio-emulator.apk` (mesmo da 001); credenciais `clientID`/`accessToken` **não** são necessárias no `lio://print` (confirmado no emulador). `<queries>` do scheme `lio` já existe na 001.
- **Impacto na 001 (Q9):** nenhuma BR, AC, campo de `Purchase` ou tabela muda. O único toque é a SCR-03 (comprovante) ganhar a ação "Ver ingressos" e o evento `OnOpenTickets`. Proposta: UI Spec da 001 → 0.5.0 (SCR-03) e spec 001 → 1.3.1 (§3 aponta a 002; §6 lista o evento), ambas com entrada no changelog e aprovação sua antes de editar.

## 8. Dados e Privacidade
| Dado | Classificação | Observações |
|---|---|---|
| `reference` da compra (UUID v4) e ordem do ingresso | Interno | Único dado no QR Code. É identificador de correlação, sem valor fora do app. Quem fotografar o QR Code obtém só esse identificador |
| Conteúdo do QR Code (imagem e texto) | Interno | Derivado, não persistido. Não vai para log, analytics nem crash report |
| Dados da transação (`orderId`, autorização, NSU, bandeira) | Interno (transação) | **Não entram** no QR Code (BR-31) |
| Nome, data e local do evento | Público | Aparecem na tela ao lado do QR Code, não dentro dele |
| PAN, CVV, trilha, número do cartão | Dado de cartão (PCI) | Nunca presentes (o app não os recebe) |
| `clientID`, `accessToken` Cielo | Segredo | Nunca no QR Code |
| E-mail/CPF/telefone do comprador | PII (LGPD) | Não coletados. No compartilhamento, o destinatário é escolhido no app do sistema (WhatsApp, e-mail…); o app não o recebe nem o guarda |
| Imagem do QR Code compartilhada (arquivo temporário) | Interno | Só em área privada do app (`filesDir`), com acesso de leitura temporário ao app escolhido; apagada depois (o plano define quando). Nunca em armazenamento externo |
| Imagem do ingresso para impressão (arquivo temporário) | Interno | `PRINT_IMAGE` da Cielo exige arquivo legível pelo app Cielo, então fica no armazenamento compartilhado (`MediaStore`), **só em API ≥ 29**, com nome aleatório, e é apagada ao fim da impressão, no timeout e na próxima abertura do app. Contém só o QR Code (`reference` + ordem) e o texto do evento |
| QR Code impresso ou enviado a outro app | Interno | Sai do controle do app; o risco é o mesmo da captura de tela e é aceito enquanto não houver validação (Q5, Q10) |
| `clientID`, `accessToken` | Segredo | O `lio://print` não os leva (spike); continuam só no pagamento, fora do repositório e do log |

## 9. Requisitos Não Funcionais
- **Performance:** gerar até 10 QR Codes sem bloquear a main thread (dispatcher injetado, como na 001).
- **Acessibilidade:** constitution Art. VI.2. O QR Code é imagem: `contentDescription` do tipo "QR Code do ingresso 2 de 3, Show A" (nunca lê o conteúdo codificado); toque ≥ 48dp; fonte até 200%. O QR Code fica escuro sobre fundo claro com margem em qualquer tema, para ser legível por leitores. A UI Spec detalha.
- **Offline / conectividade:** a geração e a exibição funcionam sem rede.
- **Analytics:** nenhum (ADR-0009, S1). Logs via `Logger`, sem o conteúdo do QR Code.
- **Impressão e compartilhamento:** geração das imagens fora da main thread; a impressão não bloqueia a tela (`Printing`); arquivos temporários sobrevivem só à operação. Offline: a impressão depende só do app Cielo no aparelho; o compartilhamento, só do app escolhido.
- **Segurança (impressão/compartilhamento):** `FileProvider` com `exported=false` e `grantUriPermissions`, paths específicos; `Intent` explícita para o Deep Link da Cielo; nenhum `Intent` montado a partir de dado externo (AP-37); `security-reviewer` obrigatório (manifest, intents, arquivos).
- **Segurança:** o conteúdo é entrada não confiável para quem o ler; esta spec não lê. Sem `FLAG_SECURE` (nenhum dado PCI/PII na tela); o risco de captura de tela copiar o QR Code é aceito porque não há validação (Q5, Q10).

## 10. Dúvidas em Aberto
| # | Pergunta | Recomendação | Status |
|---|---|---|---|
| Q1 | O que o QR Code codifica? | **Só `reference` + ordem do ingresso**, em texto versionado (ex.: `bilheteria:1:<reference>:<n>`), sem assinatura. Assinar exige um verificador que não existe (sem backend); uma chave no APK não é segredo (security.md §9) e daria falsa sensação de segurança. Nada de PAN, transação, token Cielo nem PII (BR-31) | Respondida (2026-10-19): recomendação aceita |
| Q2 | Um QR Code por compra ou por ingresso (quantidade 1 a 10)? | **Um por ingresso**, derivado de `reference` + ordem (sem persistir, sem migração). O case fala em "QR Code do ingresso", e cada pessoa entra com o seu. Custo: a tela lista até 10 | Respondida (2026-10-19): recomendação aceita |
| Q3 | Só para compra Aprovada? E Negada, Cancelada, Falhou, Em andamento? | **Só Aprovada.** É a exigência do case ("vinculado à compra concluída"). Todas as outras: nenhum QR Code, nem seção vazia | Respondida (2026-10-19): recomendação aceita |
| Q4 | Onde aparece? | **Tela própria (SCR-05) aberta por uma ação "Ver ingressos" no comprovante** (SCR-03), tanto ao fim da venda quanto pelo histórico. Inline no comprovante ficaria longo (até 10 QR Codes) e pequeno demais para leitura. O histórico (SCR-04) não muda | Respondida (2026-10-19): recomendação aceita |
| Q5 | Validar (ler) o QR Code está no escopo? Funciona offline? | **Fora do escopo.** O case pede geração. Validar de verdade exige estado compartilhado (uso único) que não existe sem backend. Registrar como melhoria futura no roadmap (README, "O que faria com mais tempo"). Geração e exibição são offline | Respondida (2026-10-19): recomendação aceita |
| Q6 | O QR Code aparece quando a compra é aprovada tardiamente (BR-29 da 001)? | **Sim, sem aviso**, pois a tela deriva do status Aprovada no Room. Coerente com a Q14 da 001 (sem aviso ao operador). Risco aceito: o operador só vê o QR Code se reabrir o comprovante pelo histórico | Respondida (2026-10-19): recomendação aceita |
| Q7 | Biblioteca de geração? | **ZXing core** (`com.google.zxing:core`): Java puro, Apache 2.0, sem câmera nem permissão, madura. Gerar a matriz sem dependência (Reed-Solomon, máscaras, versões) é risco sem ganho e contraria o Art. III.5. Exige sua aprovação (AP-11); não é decisão estrutural, então sem ADR. A versão estável e o `group:artifact` são conferidos no Maven Central na primeira tarefa (não consegui consultar nesta sessão) | Respondida (2026-10-19): recomendação aceita |
| Q8 | Numeração: a 002 reiniciaria em BR-01/AC-01. O sensor S-09 procura `AC-xx` em testes de todo o módulo `feature/ticketing`, o mesmo da 001; um teste da 001 chamado `AC-01 …` contaria como cobertura do AC-01 da 002 | **Continuar a numeração da 001** (BR-30… e AC-29…): sem colisão, sem mudar o sensor (`scripts/harness/` exige sua autorização). Alternativa descartada: manter 01 e ajustar o S-09 | Respondida (2026-10-19): recomendação aceita |
| Q9 | Impacto na 001: a SCR-03 ganha uma ação e um evento | **UI Spec da 001 → 0.5.0 e spec 001 → 1.3.1** (só §3 e §6, sem BR/AC novos), com changelog, após sua aprovação. Alternativa mínima: só a 002 declara o evento | Respondida (2026-10-19): recomendação aceita |
| Q10 | `FLAG_SECURE` na tela de ingressos? | **Não.** Não há PCI nem PII na tela; o QR Code só carrega o `reference`. Se a validação entrar um dia, reavaliar | Respondida (2026-10-19): recomendação aceita |
| Q11 | Impressão via `lio://print`: o que imprimir, formato, retorno e erros? Funciona no emulador? | **Imprimir todos os ingressos da compra** numa única operação, cada um com "Ingresso n de q", evento, data, local e QR Code. O `research.md` da 001 só lista `print` entre os deep links; o formato do pedido, o retorno e o suporte do `lio-emulator` precisam de um **spike** (tarefa 1 do `tasks.md`, na documentação Cielo e no emulador) antes do plano. Se o emulador não imprimir, a impressão fica sem verificação no AVD e vira "validar em terminal real" (pendência aceita, como J7/J9 da 001). BR-38…40, AC-38…40 e o evento `OnPrintResult` serão ajustados ao que o spike mostrar | Respondida (2026-10-19): recomendação aceita |
| Q12 | Compartilhamento: o que vai no menu do sistema? | **Um único botão "Compartilhar" que envia as imagens de todos os ingressos** (`ACTION_SEND_MULTIPLE`), com um texto curto só com "Ingresso n de q", evento, data e local. Sem texto livre, sem dado da transação, sem o `reference` em claro além do que já está dentro do QR Code | Respondida (2026-10-19): recomendação aceita |
| Q14 | O spike do `lio://print` foi lido na documentação (2026-10-19): o que muda? | `PRINT_IMAGE` por caminho de arquivo; resposta `{code, message}` (0 sucesso, 1 sem papel, 2 erro); imprime-se **um ingresso por chamada**, o que permite retomar. Mudanças M1 a M4 do plano (§12): BR-40 refinada, BR-44 e AC-44 novas, linha de dados da imagem de impressão | Respondida (2026-10-19): aprovada com o plano |
| Q13 | Tamanho | **M**: há integração nova (`lio://print`), arquivo e intent saindo do app. Exige spec, plano completo (MASVS + STRIDE, matriz de erros, riscos), tasks, UI Spec com prancha | Respondida (2026-10-19): M confirmado |

## 11. Definition of Ready
- [x] Problema, objetivo e métricas definidos
- [x] Histórias seguem INVEST e têm prioridade
- [x] Toda BR está em EARS e tem pelo menos um AC
- [x] Todos os estados de UI e eventos estão mapeados
- [x] Dados classificados (PII / cartão)
- [x] Nenhum `[PRECISA ESCLARECIMENTO]` aberto (Q1…Q13 respondidas em 2026-10-19)
- [x] Design disponível (há UI): `ui-spec.md` e prancha aprovadas em 2026-10-19 (v0.2.0; v0.2.1 só de registro)

## 12. Changelog
| Versão | Data | Autor | Mudança |
|---|---|---|---|
| 0.1.0 | 2026-10-19 | Rafael Oliveira (com Claude Code) | Versão inicial (rascunho) a partir do case, da spec 001 e do roadmap D2 |
| 0.2.0 | 2026-10-19 | Rafael Oliveira (com Claude Code) | Q1…Q10 respondidas com as recomendações; IDs renumerados para continuar a 001 (BR-30…37, AC-29…37, Q8). Aguarda aprovação |
| 0.3.0 | 2026-10-19 | Rafael Oliveira (com Claude Code) | Pedido do humano: impressão (`lio://print`) e compartilhamento (share sheet) entram no escopo; tamanho M (Q13). US-04, US-05, BR-38…43, AC-38…43, `TicketActionState`, eventos novos; e-mail enviado pelo app, PDF e WhatsApp direto ficam fora. Q11 e Q12 abertas |
| 1.0.0 | 2026-10-19 | Rafael Oliveira | Aprovada. Q11 e Q12 respondidas com as recomendações (imprimir todos os ingressos numa operação, sujeito ao spike do `lio://print`; um botão "Compartilhar" com todas as imagens). Pendência aceita: impressão pode ficar sem verificação no AVD (validar em terminal real) |
| 1.0.1 | 2026-10-19 | Rafael Oliveira (com Claude Code) | Decisão D3 do design, aprovada com a UI Spec 002: BR-39, AC-39 e o estado `Printing` passam a tornar indisponível também "Compartilhar" durante a impressão (uma falha de compartilhamento não pode liberar uma segunda impressão). UI Spec 002 v0.1.1 aprovada. Nenhuma BR ou AC renumerado |
| 1.1.0 | 2026-10-19 | Rafael Oliveira (com Claude Code) | Aprovada com o plano (M1 a M4 do §12): BR-40 e AC-40 refinados (sem papel, contagem, retomada, aparelho sem suporte); BR-44 e AC-44 novas (confirmação "Ingressos impressos", estado `Printed`); §8 com a imagem de impressão no armazenamento compartilhado; Q14. Nenhum ID renumerado |
| 1.1.1 | 2026-10-19 | Rafael Oliveira (com Claude Code) | Registro dos achados da T01/T16 na §7 (restrição da impressão por caminho de arquivo); nenhuma BR ou AC mudou |
| 1.1.2 | 2026-10-19 | Rafael Oliveira (com Claude Code) | Ajustes de registro após o spike, a verificação no AVD e a revisão (aprovadas em 2026-10-19): BR-40 passa a citar o caso "nenhuma resposta no prazo" (já implementado como `Timeout`); `OnPrintResult` entra pelo callback e pelo `PrintStatus`; §7 e §8 sem credenciais no `lio://print`; links atualizados. Nenhuma BR ou AC mudou de significado nem foi renumerado |
| 1.1.2 | 2026-10-19 | Rafael Oliveira (com Claude Code) | Implementada. T01–T18 concluídas, `verify.sh full` verde, 16 ACs (AC-29…AC-44) com teste. Pendências aceitas: impressão em terminal real e legibilidade do QR impresso não verificadas; texto do compartilhamento e Android 12+ não observados no AVD; as notas 1.1.2 e a UI Spec 0.2.1 foram aprovadas na mesma data |
| 1.1.2 | 2026-10-19 | Rafael Oliveira | Aprovada (inclui os registros pós-spike e da revisão); status segue Implementada. Pendência aceita: impressão em terminal real |
