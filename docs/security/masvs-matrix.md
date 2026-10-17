# Matriz de cobertura OWASP MASVS v2.1

> Para cada controle: o que verifica automaticamente, o que a IA revisa e o que exige teste
> manual (MASTG v2). Regras de código: [guia de segurança](../harness/guides/security.md).
> IDs de sensor: [catálogo](../harness/sensors/README.md). Atualize quando um sensor novo
> passar a cobrir um controle; a coluna "Manual" deve encolher com o tempo.

**Legenda:** ● cobre bem · ◐ cobre parte · — não cobre

| Controle | Enunciado (resumo) | Automático (gate) | IA (comenta) | Manual por release (MASTG) |
|---|---|---|---|---|
| **STORAGE-1** | Armazena dado sensível com segurança | ◐ S-08 (`SensitiveExternalPath`, `InsecureLegacyExternalStorage`), S-22, S-23 | ● S-17/S-26: o que é persistido, onde, cifrado como | Inspecionar `/data/data/<pkg>` e backup após o fluxo de cartão |
| **STORAGE-2** | Evita vazamento de dado sensível | ● S-01, S-02, S-05 (`SensitiveDataInLogRule`), S-08 (`LogInfoDisclosure`) | ● `toString()`, analytics, crash, `Bundle`, clipboard | Logcat, screenshot de "Recentes", teclado, notificações |
| **CRYPTO-1** | Criptografia forte e bem usada | ● S-08 (`VulnerableCryptoAlgorithm`, `WeakPrng`), S-22 | ● Modo, IV, padding, uso fora de `core:security` | — |
| **CRYPTO-2** | Gestão de chaves correta | ◐ S-02, S-14 (chave no código) | ● Keystore não exportável, StrongBox, invalidação por biometria | Confirmar chaves no Keystore e ausência no backup |
| **AUTH-1** | Autenticação e autorização seguras | — | ● Autorização no servidor, expiração/revogação de sessão | Reuso de token, logout no servidor |
| **AUTH-2** | Autenticação local segura | ◐ S-22 | ● `BiometricPrompt` com `CryptoObject`, `BIOMETRIC_STRONG` | Bypass de biometria com Frida (MASTG) |
| **AUTH-3** | Operações sensíveis com autenticação extra | — | ● Pagamento via deep link/notificação exige sessão e confirmação | Fluxo ponta a ponta |
| **NETWORK-1** | Tráfego seguro | ● S-08 (`DefaultCleartextTraffic`, `DefaultTrustedUserCerts`), S-22, S-23 | ● `TrustManager`/`HostnameVerifier` permissivo, logging HTTP | Proxy (mitmproxy) com CA de usuário em build de release |
| **NETWORK-2** | Pinning dos endpoints próprios | ◐ S-23 (lê `network_security_config`) | ● Pin de backup, `expiration`, plano de rotação | Pinning resiste a proxy; rotação testada |
| **PLATFORM-1** | IPC seguro | ● S-08 (`exported`, `PendingIntent`, `StrandhoggVulnerable`, `MissingAutoVerifyAttribute`), S-22, S-23 | ● Intent redirection, validação de deep link | `adb shell am start` em componentes exportados (Drozer/ADB) |
| **PLATFORM-2** | WebView segura | ● S-08, S-22 | ● JS, `addJavascriptInterface`, allowlist de origens | Carregar origem não permitida |
| **PLATFORM-3** | UI segura | ◐ S-08 (`TapjackingVulnerable`); candidato a regra Konsist para AP-35 (`FLAG_SECURE`) | ● `FLAG_SECURE` em dialogs/sheets, teclado, clipboard | Screenshot e gravação de tela nas telas de cartão |
| **CODE-1** | Plataforma atualizada | ● `minSdk`/`targetSdk` no convention plugin; S-08 | — | — |
| **CODE-2** | Forçar atualização do app | — | ◐ Se a spec exigir (Play In-App Updates) | Versão bloqueada é recusada |
| **CODE-3** | Sem componentes vulneráveis | ● S-15 (OSV + Dependabot), S-24 (verificação de artefatos), S-25 (SBOM) | ● Dependência nova: existe, é mantida, não é alucinada | Revisar SDKs de terceiros no Play SDK Index |
| **CODE-4** | Valida entradas não confiáveis | ◐ S-22 | ● Deep link, intents, respostas do servidor, QR code | Fuzzing de deep links |
| **RESILIENCE-1** | Integridade da plataforma | — | ◐ Veredito do Play Integrity decidido no servidor | Root/emulador |
| **RESILIENCE-2** | Anti-tamper | ◐ S-13 (R8, `debuggable=false`), S-23 | — | Reempacotar o APK e verificar rejeição no servidor |
| **RESILIENCE-3** | Anti-análise estática | ◐ S-13 (R8) | — | Avaliar com ADR se exigido pela auditoria |
| **RESILIENCE-4** | Anti-análise dinâmica | — | — | Avaliar com ADR se exigido pela auditoria |
| **PRIVACY-1** | Minimiza acesso a dados e recursos | ◐ S-08 (permissões), S-23 (permissões finais, inclusive de SDKs) | ● Permissão nova justificada na spec | Data safety do Play Console confere com o app |
| **PRIVACY-2** | Evita identificar o usuário | ◐ S-02 | ● IDs pseudônimos em analytics/crash | Inspecionar payload de analytics |
| **PRIVACY-3** | Transparência sobre coleta | — | ◐ Spec seção 8 × código | Revisão jurídica/LGPD |
| **PRIVACY-4** | Controle do usuário sobre os dados | — | ◐ Exclusão e consentimento implementados conforme spec | Fluxo de exclusão ponta a ponta |

## Gate de release
Antes de cada release para produção:
1. Todos os gates automáticos verdes (incluindo S-23 sem `high` aberto).
2. Linhas com coluna "Manual" preenchida executadas para as áreas **alteradas** desde a
   última release; anualmente (ou antes de auditoria PCI), todas.
3. Resultado registrado na issue/PR de release com link para esta matriz.
