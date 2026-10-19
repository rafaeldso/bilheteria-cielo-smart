# Pesquisa: `lio://print` no emulador Cielo (spike T01, 2026-10-19)

> Fatos observados no AVD `Bilheteria_API29` (Android 10) e no `Medium_Phone_API_37.0` (Android 16), com o `lio-emulator.apk` 1.61.8
> (`br.com.cielosmart.orderservice`, targetSdk 34). Documentação: [Impressão](https://docs.cielo.com.br/cielo-smart/docs/impressao.md),
> [Testes de impressão](https://docs.cielo.com.br/cielo-smart/docs/realizando-testes-de-impressao.md). Evidências (logs e capturas) ficam no
> scratchpad da sessão, não no repositório. Sem tokens nem dados reais.

## 1. O que funciona
| Pergunta | Achado |
|---|---|
| Credenciais no pedido | **Não são necessárias.** O JSON é só `{operation, styles, value}`; o emulador respondeu normalmente sem `clientID` nem `accessToken` |
| Formato | `lio://print?request=<base64>&urlCallback=order://response`; `PRINT_IMAGE` com `value:["/caminho/arquivo.png"]` (caminho, **não** `content://`: `content://media/...` vira `ENOENT`) |
| Altura | Imagem de 384×1152 imprime em **uma** chamada; sem limite aparente (não testei mais alto) |
| Tela | O app Cielo **toma a tela** a cada chamada válida (`PrinterActivity`, escolha de cenário). Ao fim, **devolve o foco ao app do projeto sozinho** (`MAIN/LAUNCHER`, `NEW_TASK`) |
| Cenários do emulador | Tela "Qual cenário deseja simular?": "Impressão realizada", "Erro na impressão", "Sem papel", com "Não perguntar novamente". **Não pergunta o tipo de conteúdo** (a doc cita imagem/QR/código de barras; no 1.61.8 só os 3 cenários) |
| Respostas | Formato `{"code":N,"message":"..."}`: sucesso `{"code":0,"message":"SUCCESS"}`; sem papel `{"code":1,"message":"WITHOUT PAPER"}`; erro `{"code":2,"message":"java.lang.Throwable: PRINT ERROR: PRINT ERROR"}`. Chegam em `order://response?response=<base64>` |
| Efeito colateral | O app Cielo copia cada imagem impressa como JPEG para `/sdcard/Download/<uuid>.jpg` e **não apaga** (uma por chamada) |

## 2. O que NÃO funciona (risco R1 do plano)
| Ambiente | Caminho | Resultado |
|---|---|---|
| API 29 | `/sdcard/Pictures/Bilheteria/*.png` (inclusive indexado no MediaStore; com e sem READ/WRITE concedidos ao app Cielo) | `EACCES`: `{"code":2,"reason":"...open failed: EACCES (Permission denied)"}`, imediato, sem prompt |
| API 29 | `/sdcard/Android/data/<pacote do projeto>/files/*.png` | `EACCES` |
| API 29 | `/data/local/tmp/*.png` (controle, só o shell grava) | **Funciona** (prompt, `code 0`) |
| API 29 | `MANAGE_EXTERNAL_STORAGE` ao app Cielo | **Não existe** no Android 10 (`appops` não conhece a operação) |
| API 37 | `/sdcard/Pictures/...` e `/sdcard/Download/...` sem `MANAGE` | `EACCES` |
| API 37 | com `appops set br.com.cielosmart.orderservice MANAGE_EXTERNAL_STORAGE allow` | O `EACCES` some, mas o emulador falha com `code 2`/`reason`: "One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified…" (bug do emulador 1.61.8 em Android 14+, também com o arquivo de controle) |

**Não observado:** um arquivo criado de fato por um app via `MediaStore` (o shell não consegue: `content write` falha com `SecurityException`), nenhum AVD API 30–33 (só existem imagens 29 e 37 no SDK), e nada num terminal real.

## 3. Consequências para o plano
1. **R1 se confirma no AVD Android 10:** nenhum caminho de armazenamento compartilhado é legível pelo app Cielo (leitor com targetSdk 34 sem acesso por caminho a arquivo alheio). O que muda de chave é a permissão **do app Cielo** (`MANAGE_EXTERNAL_STORAGE`, Android 11+), não do nosso. Num terminal real não está verificado.
2. **Erro de arquivo usa `reason`, não `message`:** `{"code":2,"reason":"...EACCES|ENOENT..."}`, exatamente a forma de um erro de **pagamento** com `code 2`. O discriminador "tem `message`, não tem `reason`" do plano (§4) **não basta**. Atribuição passa a ser por **estado**: enquanto há impressão aguardando resposta, uma resposta `{code, message|reason}` sem `id`, `reference` nem `results` é de impressão; sem impressão em andamento, segue o caminho do pagamento como hoje. O app Cielo ocupa a tela durante a impressão e devolve o foco, o que torna a sobreposição com um pagamento remota; ainda assim a T08 impede iniciar impressão com compra aberta (`Pending`/`Recovering`) e a T09 só abre um trabalho por vez.
3. **Resposta imediata em erro de arquivo** (sem prompt): o timeout de 15 s do plano continua valendo para "sem resposta", e o erro de arquivo vira `PrintFailed(Unknown)` (ou um motivo próprio de arquivo ilegível).
4. **Lixo no aparelho:** cópias JPEG em `/sdcard/Download` são do app Cielo (fora do nosso controle); registrar na spec/README como efeito conhecido do emulador.
5. **Sem credenciais** no `CieloPrintEncoder` (confirmado).

## 4. Verificação com o app real (T16, 2026-10-19)
Journeys no AVD com o app do projeto instalado (arquivo criado de fato pelo nosso app no `MediaStore`, sem permissão nossa):

| Ambiente | Condição do app Cielo | Resultado |
|---|---|---|
| Android 10 (API 29) | sem concessão extra, ou com `READ_EXTERNAL_STORAGE` concedida | **EACCES** imediato (`{"code":2,"reason":"...EACCES..."}`); a tela mostra "Não foi possível imprimir os ingressos." com "Tentar novamente"; arquivo apagado |
| Android 10 (API 29) | `MANAGE_EXTERNAL_STORAGE` | não existe nessa versão (não testável) |
| Android 11 (API 30) | sem concessão extra | **EACCES**, mesmo comportamento |
| Android 11 (API 30) | `appops set br.com.cielosmart.orderservice MANAGE_EXTERNAL_STORAGE allow` | **Imprime**: 3 chamadas `lio://print` (uma por ingresso), `{"code":0,"message":"SUCCESS"}`, "Ingressos impressos" por ~4 s, `Pictures/Bilheteria` vazio; sem papel no 2º ingresso → "Imprimiu 1 de 3" e a nova tentativa envia só os ingressos 2 e 3; erro → mensagem com a dica do papel. A linha do `MediaStore` tem `owner_package_name` = nosso app |

Conclusão: o desenho (uma imagem por ingresso no `MediaStore`, sem permissão nossa, retomada por ingresso) funciona **quando o app Cielo tem acesso de leitura ao armazenamento compartilhado** (`MANAGE_EXTERNAL_STORAGE`, que a doc da Cielo cita). No AVD Android 10 a impressão por imagem não é verificável; o QR Code em si é provado por ida e volta (ZXing) e por captura. Num terminal real o acesso do app Cielo não foi verificado: fica como **pendência aceita** (validar em terminal real). O emulador 1.61.8 não roda `lio://print` em Android 14+ (bug de `RECEIVER_EXPORTED`). Não observado: o texto do compartilhamento no chooser (só as 3 imagens), a repetição de toque durante a impressão no AVD (o prompt do Cielo cobre o app) e Android 12+.

