# Guia: Segurança e Privacidade

> **Base normativa:** Constitution Art. V · [OWASP MASVS v2](https://mas.owasp.org/MASVS/)
> (verificação via [MASTG](https://mas.owasp.org/MASTG/)) · PCI DSS v4 · LGPD (Lei 13.709/2018) ·
> [Android App Security Best Practices](https://developer.android.com/privacy-and-security/security-tips).
> **Skills oficiais:** `android-intent-security`, `android-permissions-security`.
> **Anti-padrões:** [AP-07, AP-33 a AP-39](anti-patterns.md#4-segurança-privacidade-e-dados-de-cartão).
> **Sensores:** S-01 `lint-file.sh`, S-02 `scan-sensitive-data.sh`, S-04 detekt, S-08 Android Lint,
> S-13 build release, S-14 Gitleaks, S-15 OSV-Scanner, S-17 `security-reviewer`, S-21…S-26
> ([catálogo](../sensors/README.md)). **Processo de varredura e triagem:** [docs/security/](../../security/README.md).

Este app lida com dados de cartão e de pessoas. A regra geral: **o melhor dado
sensível é o que o app não tem.** Na dúvida, não colete, não persista, não registre
e pergunte.

## Classificação de dados
| Classe | Exemplos | Regras |
|---|---|---|
| **Cartão: dados de autenticação (SAD/PCI)** | CVV/CVC, trilha, PIN/PIN block | **Nunca** persistir, logar ou transmitir fora do canal especificado, nem mesmo cifrados. Ficam em memória só até o envio e são descartados depois. |
| **Cartão: dados do portador (CHD/PCI)** | PAN; validade e nome **quando junto com o PAN** | PAN completo só em memória durante o fluxo. Para persistir ou exibir, use apenas token ou máscara (`**** 1234`, no máximo os 4 últimos). Nunca em log, analytics, crash report, `toString()`, `Bundle` ou screenshot de teste. |
| **PII (LGPD)** | CPF, nome, e-mail, telefone, endereço, geolocalização | Só o mínimo necessário, com base legal ou consentimento definidos na spec. Nunca em logs. Mascare na UI quando a spec pedir. Retenção e exclusão conforme a spec. |
| **PII sensível (LGPD art. 5º, II)** | Biometria, dados de saúde, documentos com foto | Tratamento só com spec e plano explícitos. Biometria nunca sai do dispositivo: use `BiometricPrompt` e não guarde nenhum template. |
| **Segredos** | Access/refresh token, chaves de API, certificados, senhas | Nunca no código, no `BuildConfig` ou no repositório. Em repouso, só cifrados com chave do Android Keystore. |
| **Público** | Textos, configurações não sensíveis | Sem restrição. |

Cada spec classifica os seus dados (seção 8). O plano trata MASVS + STRIDE (seção 7).

### Onde cada dado pode ficar
| Dado | Memória (ViewModel) | `SavedStateHandle`/`Bundle` | DataStore/arquivo | Room | Log/analytics/crash |
|---|---|---|---|---|---|
| CVV, trilha, PIN | só até o envio | 🚫 | 🚫 | 🚫 | 🚫 |
| PAN completo | só durante o fluxo | 🚫 | 🚫 | 🚫 | 🚫 |
| Token do cartão, últimos 4 | ✅ | ✅ | ✅ | ✅ | 🚫 (só ID de correlação) |
| Access token | ✅ | 🚫 | só cifrado (Keystore) | 🚫 | 🚫 |
| Refresh token | 🚫 | 🚫 | só cifrado (Keystore) | 🚫 | 🚫 |
| PII | conforme a spec | evitar (prefira ID) | só cifrado, se a spec exigir persistência | só com ADR | 🚫 |

`SavedStateHandle` e `Bundle` podem ir para disco e para o sistema. Trate-os como
armazenamento **não seguro**.

## Regras para o agente

### 1. Logs, analytics e crash reports (MASVS-STORAGE, PRIVACY)
- Use o logger do projeto (`core:common`, wrapper sobre Timber). Em release, nenhuma
  `Tree` de debug é plantada. Nunca use `println`, `Log.*` ou `printStackTrace()` (AP-07).
- Registre apenas **tipo de evento e ID de correlação**. Nunca interpole objetos de
  domínio, DTOs, `Intent`s, URLs com query ou corpos HTTP.
- `HttpLoggingInterceptor` só em `debug`, no nível `HEADERS` no máximo, com
  `redactHeader("Authorization")` e `redactHeader("Cookie")`. Nunca `BODY`.
- Mensagens de exceção podem carregar dado do servidor ou do usuário. Mapeie para um
  erro de domínio antes de registrar (ver [error-handling.md](error-handling.md)).
- Analytics e crash usam **allowlist** de parâmetros. `setUserId` recebe só ID
  pseudônimo. Custom keys e breadcrumbs não levam PII.

### 2. Modelos com dado sensível
Toda classe que carrega PAN, CVV ou PII sobrescreve `toString()`. `data class` gera
`toString()` com todos os campos, e é assim que o dado vaza em log e crash.

```kotlin
@JvmInline
value class Pan(private val digits: String) {
    val last4: String get() = digits.takeLast(4)
    override fun toString(): String = "Pan(**** $last4)"
}

data class CardInput(val pan: Pan, val cvv: String) {
    override fun toString(): String = "CardInput(pan=$pan, cvv=***)"
}
```
Cubra a máscara com teste unitário. Ao sair do fluxo de cartão, limpe o estado do
`ViewModel`.

### 3. Rede (MASVS-NETWORK)
- Somente HTTPS/TLS 1.2+. `network_security_config.xml` com
  `cleartextTrafficPermitted="false"` e trust anchors do sistema. Nunca aceite
  CAs instaladas pelo usuário em release.
- **Pinning** conforme o plano: pin do certificado intermediário ou da chave pública,
  sempre com **pin de backup** e `expiration` alinhada ao plano de rotação. Pinning sem
  plano de rotação derruba o app em produção.
- Proxy e inspeção de tráfego só via `<debug-overrides>` (vale apenas em builds
  `debuggable`). Nunca desligue o pinning "para testar".
- Nunca crie `TrustManager` permissivo, `HostnameVerifier` que retorna `true`,
  `SSLSocketFactory` customizado ou `onReceivedSslError { handler.proceed() }` (AP-36).
- Respostas sensíveis não entram no cache HTTP (`Cache-Control: no-store`). Imagens
  sensíveis, como documentos, não vão para o cache do carregador de imagens.

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors><certificates src="system" /></trust-anchors>
    </base-config>
    <domain-config>
        <domain includeSubdomains="true">api.exemplo.com.br</domain>
        <pin-set expiration="2027-06-30">
            <pin digest="SHA-256">PIN_PRINCIPAL_BASE64=</pin>
            <pin digest="SHA-256">PIN_BACKUP_BASE64=</pin>
        </pin-set>
    </domain-config>
    <debug-overrides>
        <trust-anchors><certificates src="user" /></trust-anchors>
    </debug-overrides>
</network-security-config>
```

### 4. Armazenamento e criptografia (MASVS-STORAGE, CRYPTO)
- Toda cifra passa por `core:security`. Features não usam `Cipher`/`KeyStore` direto.
- Padrão: **DataStore + AES-256-GCM** com chave **não exportável** do Android Keystore
  (StrongBox quando houver). O IV é gerado pelo Keystore e gravado junto do texto
  cifrado. `EncryptedSharedPreferences`/`security-crypto` está **depreciada** pelo
  AndroidX: não use em código novo.
- Nada sensível em `SharedPreferences` comum, armazenamento externo, `cacheDir`
  compartilhado ou Room sem criptografia. Room cifrado (ex.: SQLCipher) exige ADR.
- Arquivos só em `filesDir`, compartilhados via `FileProvider` com paths específicos.
- Backup: `android:allowBackup="false"` ou `dataExtractionRules`/`fullBackupContent`
  excluindo tokens e bancos (AP-38). Chaves do Keystore não vão no backup; dado
  restaurado sem a chave precisa ser tratado como inválido.
- Logout e revogação de sessão limpam tudo: tokens, DataStore, Room, caches, cookies,
  `WorkManager` pendente e chaves associadas.

| Permitido | Proibido |
|---|---|
| AES-256-GCM, HMAC-SHA256, SHA-256+, ECDSA P-256, RSA ≥ 2048 com OAEP/PSS, `SecureRandom` | ECB, DES/3DES, RC4, MD5/SHA-1 para segurança, `java.util.Random` para segurança, IV fixo, chave no código ou no `BuildConfig`, criptografia própria |

### 5. Autenticação e sessão (MASVS-AUTH)
- A autorização é decidida no **servidor**. O cliente nunca é a única barreira.
- Access token curto e só em memória quando possível. O refresh token fica cifrado.
  Expiração e renovação seguem o plano.
- Biometria: `BiometricPrompt` com `BIOMETRIC_STRONG` e **`CryptoObject`** (a
  biometria destrava a chave; um `onAuthenticationSucceeded` sozinho não prova nada).
  Chave criada com `setUserAuthenticationRequired(true)` e
  `setInvalidatedByBiometricEnrollment(true)`.
- Ações de pagamento iniciadas por deep link, notificação ou widget exigem sessão
  válida e confirmação explícita do usuário.

### 6. UI sensível (MASVS-PLATFORM)
- `FLAG_SECURE` nas telas com dados de cartão, aplicado por um único efeito
  reutilizável do projeto (AP-35). **Dialogs, bottom sheets e popups são janelas
  separadas:** use `securePolicy = SecureFlagPolicy.SecureOn` nas respectivas
  `*Properties`.
- Oculte o conteúdo no "Recentes" (`FLAG_SECURE` ou `setRecentsScreenshotEnabled(false)`
  no API 33+).
- Teclado: `KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.NumberPassword)`
  para PAN/CVV. CVV sempre mascarado.
- Clipboard: não copie PAN nem CVV. Se a spec exigir cópia (ex.: código Pix), marque
  o clip como sensível (`ClipDescription.EXTRA_IS_SENSITIVE`, API 33+).
- Tapjacking: em telas de confirmação de pagamento, filtre toques com a janela
  obscurecida (`filterTouchesWhenObscured`) ou use `setHideOverlayWindows(true)` (API 31+).
- Notificações sem PAN nem valores sensíveis na tela de bloqueio (`VISIBILITY_PRIVATE`
  com versão pública). Payload de push nunca carrega dado sensível.

### 7. Componentes, intents e deep links (MASVS-PLATFORM)
- `android:exported` sempre explícito, `false` por padrão (AP-37). Exportado só com
  justificativa no plano e proteção (permissão `signature` quando aplicável).
- Todo dado de `Intent`, URI ou deep link é **entrada não confiável**: valide tipo,
  formato e faixa (skill `android-intent-security`).
- Não lance `Intent` extraída de extras (*intent redirection*). Use intents explícitas
  e `PendingIntent` com `FLAG_IMMUTABLE`.
- Deep links via App Links verificados (`android:autoVerify="true"`). Parâmetros nunca
  disparam ação sensível sem autenticação e confirmação.
- Permissões mínimas, solicitadas em contexto (skill `android-permissions-security`).
  Prefira o Photo Picker a permissões de mídia e localização aproximada à precisa.

### 8. WebView (MASVS-PLATFORM)
- Use só quando a spec exigir e o plano justificar. JavaScript desligado salvo
  necessidade (AP-38).
- `allowFileAccess = false` e `allowContentAccess = false`. Conteúdo local via
  `WebViewAssetLoader`. Navegação restrita a uma allowlist de origens HTTPS.
- Nunca use `addJavascriptInterface` com dado ou ação sensível. Nunca coloque token em
  query string.

### 9. Código, build e release (MASVS-CODE)
- R8 ativo, `isDebuggable = false`, regras `keep` só para o necessário (AP-39, S-13).
- **Ofuscação pronta desde o início:**
  - R8 em modo completo (padrão do AGP 8+; não desligue `android.enableR8.fullMode`),
    `proguard-android-optimize.txt` como base.
  - Prefira bibliotecas sem reflexão (kotlinx.serialization, Hilt/KSP, Room/KSP); elas
    trazem as próprias regras. DTO com reflexão exige regra mínima no `consumer-rules.pro`.
  - Remoção de logs em release: só a `Tree` de release é plantada; `-assumenosideeffects`
    para `android.util.Log` como defesa extra.
  - `mapping.txt` arquivado pelo CI e enviado ao Crashlytics (plugin Gradle) a cada
    release; sem isso, stack traces de produção ficam ilegíveis.
  - O build `release` (S-13) roda no CI a cada PR M/G para pegar quebra de R8 cedo.
  - Ofuscação não protege segredo: nada sensível no APK (ver acima). Proteção extra
    (anti-tamper, Play Integrity) só por decisão em ADR.
- Endpoints, flags e telas de debug só nos source sets `debug`. Nada disso no `main`.
- Segredos de build vêm do CI (variáveis e secrets). Nunca de `local.properties`
  versionado nem de literais. Lembre que tudo que vai no APK pode ser extraído: chave
  de API no app não é segredo, é identificador.
- Dependências novas só com aprovação. O CI verifica CVEs (S-15), integridade dos artefatos
  (S-24) e segredos no histórico (S-14).
- Antes de propor uma dependência, confirme no Maven Central/Google Maven que o `group:artifact`
  **exato** existe e é mantido. Agentes de IA inventam nomes plausíveis, e atacantes registram
  esses nomes (*slopsquatting*).

### 10. Resiliência (MASVS-RESILIENCE)
- Integridade do app e do dispositivo via **Play Integrity API** (requisição
  *standard* ligada à ação por `requestHash`), com o veredito avaliado **no
  servidor**. Detecção de root, debugger ou emulador no cliente é só um sinal, nunca a
  decisão.
- Proteções anti-tamper, ofuscação adicional ou SDK de RASP exigem ADR e aprovação.

### 11. Arquivos proibidos para o agente
`local.properties`, `*.jks`, `*.keystore`, `google-services.json`, `.env*`, `secrets.*`
são bloqueados por permissão e hook. Não tente contornar.

## Pare e pergunte antes de
- adicionar biblioteca de criptografia, segurança, autenticação ou integridade;
- alterar `network_security_config.xml`, pins ou a configuração do cliente HTTP;
- exportar componente, adicionar permissão, deep link ou WebView;
- persistir um dado sensível novo ou mudar onde ele fica;
- mudar fluxo de login, sessão, biometria ou logout;
- aceitar pagamento no próprio dispositivo (Tap to Pay/PIN no celular): isso cai no
  escopo do **PCI MPoC** e exige ADR.

## Como validar
| O quê | Como |
|---|---|
| PAN/CPF/segredos no código e em fixtures | S-02 (`scan-sensitive-data.sh`). Em testes, use só PANs de teste conhecidos (`4111111111111111`). |
| Log direto ou com dado sensível | S-01 e S-04, a cada edição |
| Manifest, TLS, WebView, `PendingIntent` | S-08 Android Lint (`verify.sh standard`) |
| Release endurecido | S-13 (`verify.sh full`) |
| Máscara de `toString()`, mapeamento de erro sem dado do servidor | Teste unitário citando o `AC-xx` |
| Mudança que toca dado sensível, rede, storage, auth ou manifest | Subagente `security-reviewer` (S-17) |
| Releases relevantes | Checklist MASTG manual (fora do loop do agente) |

## Checklist MASVS (para o plan.md, seção 7)
| Grupo | Perguntas |
|---|---|
| STORAGE | Algum dado sensível é persistido? Onde, cifrado como, e quem limpa no logout? Está fora do backup? |
| CRYPTO | Usa `core:security` + Keystore? Algoritmo da lista permitida? Nenhuma chave no código? |
| AUTH | Autorização no servidor? Sessão expira e é revogável? Biometria com `CryptoObject`? |
| NETWORK | TLS sem cleartext? Pinning com backup e plano de rotação? Sem cache de resposta sensível? |
| PLATFORM | Componentes exportados, intents, deep links, WebView, permissões mínimas, `FLAG_SECURE` (inclusive em dialogs)? |
| CODE | Entradas validadas? Dependências sem CVE? R8 e release sem debug? |
| RESILIENCE | A spec exige Play Integrity ou detecção de ambiente? O veredito é decidido no servidor? |
| PRIVACY | Dados mínimos, base legal e consentimento? Sem PII em logs, analytics e crash? Retenção e exclusão definidas? |

Para cada ameaça STRIDE relevante, o plano aponta a mitigação e o teste ou sensor que
a comprova.
