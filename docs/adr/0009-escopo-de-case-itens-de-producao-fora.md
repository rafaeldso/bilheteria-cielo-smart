---
status: aceito
date: 2026-10-17
decisores: Rafael Oliveira
consultados: Agente de IA (Claude Code)
informados: Avaliadores do case
---

# ADR-0009: Escopo de case: itens de produção fora da entrega

## Contexto e Problema
O projeto é um case técnico ([case_android.md](../../case_android.md)), não um app que vai para loja.
O harness foi desenhado para produção (ADR-0003, ADR-0006, ADR-0007) e exige itens que não mudam o que
o case avalia (funcionalidade, tratamento de erros, prevenção de duplicidade, testes críticos, organização
e uso de IA), mas custam tempo de entrega, contas externas ou dinheiro. Alguns também não funcionam num
repositório privado no plano Free do GitHub.

Constitution Art. VI.4 exige observabilidade (crash, ANR, performance) desde a primeira release; pular
o Firebase é uma exceção e, pela governança, precisa deste ADR.

## Fatores de Decisão
- Foco no que o case avalia; tempo de entrega.
- Custo zero: sem contas pagas nem chaves de API.
- Nada que enfraqueça sensores de código (detekt, Konsist, lint, testes, cobertura continuam).
- Reversível: cada item volta sem retrabalho de arquitetura.

## Opções Consideradas
- Manter o harness completo de produção.
- Retirar itens de produção e operação, registrando cada um com motivo.

## Decisão
Opção escolhida: "retirar itens de produção e operação", aprovada pelo humano em 2026-10-17 item a item:

| # | Item fora | Motivo | Substituto no case |
|---|---|---|---|
| S1 | Firebase Crashlytics e Performance (exceção ao Art. VI.4) | Projeto Firebase, `google-services.json` e fluxo de consentimento, sem valor para a avaliação | `Logger` (`core:common`) + Timber só em debug |
| S2 | `:baselineprofile` e Macrobenchmark | Otimização de produção | — |
| S3 | Assinatura de release e publicação | Não vai para loja | Build release com R8 continua no `harnessFull`; APK de debug no CD |
| S4 | Workflows `ai-review` / `ai-security-review` configurados | Exigem `ANTHROPIC_API_KEY` paga | Arquivos mantidos; revisão agêntica local (`review-changes`) |
| S5 | Job MobSF `binary` (S-23) e SBOM | Container e secret extras | Gitleaks, OSV-Scanner, mobsfscan e zizmor |
| S6 | Proteção de branch, CODEOWNERS, checks obrigatórios | Indisponível em repositório privado no Free; uma pessoa | — |
| S7 | `verification-metadata.xml` e dependency locking | Atrito a cada dependência nova | Dependabot; OSV sem lockfile vira aviso |
| S8 | Testes de screenshot (Roborazzi) | Foco em cenários críticos | Unitários (domain, ViewModel) + teste de UI do fluxo principal |
| S9 | Layout adaptativo (WindowSizeClass) | Terminal Smart tem tela fixa | Tema claro/escuro e acessibilidade mantidos |
| S10 | Cenários EV-01…EV-10 do harness | Meta-teste do harness | Prova manual dos sensores registrada no `progress.md` do 000 |

### Consequências
- **Positivas:** entrega concentrada nos requisitos do case; custo zero; sensores de código intactos.
- **Negativas:** sem telemetria de crash; sem gate de cadeia de suprimentos por checksum; regressões
  visuais dependem de revisão humana e do `verify-on-device`.

### Confirmação
- `build-logic/` não aplica Firebase, Roborazzi nem baseline profile; `security.yml` sem o job `binary`.
- Reverter um item = remover a linha da tabela acima neste ADR (novo ADR que o substitui) e seguir o
  passo correspondente de [bootstrap.md](../harness/bootstrap.md).

## Mais Informações
[Roadmap](../roadmap.md) · [ADR-0003](0003-stack-de-sensores-de-qualidade.md) ·
[ADR-0006](0006-code-review-e-security-scanning-com-ia.md) ·
[ADR-0007](0007-stack-de-bibliotecas-observabilidade-e-politicas-de-qualidade.md)
