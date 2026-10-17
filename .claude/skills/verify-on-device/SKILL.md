---
name: verify-on-device
description: Verifica o comportamento real do app em emulador com o Android CLI — instala, navega, captura tela e árvore de layout, checa acessibilidade e executa Journeys. Use após mudanças de UI/fluxo, para evidência de PR, ou quando o usuário pedir para "ver funcionando".
argument-hint: "<fluxo ou journey> [avd]"
---

# Verificar no dispositivo

Delegue a execução ao subagente `device-tester` quando a saída for longa (logcat,
várias telas); ele devolve só o resumo. Somente emulador/dispositivo de teste.

1. **Preparar:** `android info` (device conectado?). Sem device: `android emulator list`
   e peça confirmação para `android emulator start <avd>`.
2. **Instalar e abrir:** `./gradlew :app:assembleDebug` → `android run` (pede confirmação).
3. **Navegar** até o estado alvo com `android input …` ou, se existir, rode a Journey do fluxo
   (Journeys: skill oficial `android-cli`; ver `docs/harness/execution/README.md`).
4. **Coletar evidência** para cada estado da spec exercitado:
   - `android screen capture -o build/harness/device/<estado>.png`
   - `android layout --pretty` → confira `contentDescription`/rótulos, alvos de toque ≥ 48dp,
     textos sem truncamento; repita com fonte ampliada quando a spec exigir.
5. **Relatar** em tabela `Estado (spec) | Esperado | Observado | Evidência`. Divergência
   vira tarefa/bug (`fix-bug`), nunca ajuste silencioso.

Telas com dados de cartão: use apenas massa de teste; não anexe screenshots com PAN/PII real.
