# Templates de código de referência

> Guias dizem *o que* fazer; templates mostram *como*. O agente copia o padrão
> daqui enquanto o projeto não tiver um exemplo real equivalente. Assim que existir
> uma feature implementada e revisada, ela passa a ser a referência preferencial.
>
> Pacote placeholder: `com.example.app` → substituir por `<base.package>`.
> Feature de exemplo: **Balance** (consultar saldo).

| Arquivo | Camada / módulo | Demonstra |
|---|---|---|
| [kotlin/CoreCommon.kt](kotlin/CoreCommon.kt) | `core:common` | `DataResult`, `AppError`, qualifiers de dispatcher, `suspendRunCatching` |
| [kotlin/di/DispatchersModule.kt](kotlin/di/DispatchersModule.kt) | `core:common` (DI) | Único lugar com `Dispatchers.*` literal |
| [kotlin/FeatureDomain.kt](kotlin/FeatureDomain.kt) | `feature:balance:domain` | Entidade, value class de dinheiro, interface de repositório, use case |
| [kotlin/FeatureData.kt](kotlin/FeatureData.kt) | `feature:balance:data` | DTO, mapper, data source, repositório com matriz de erros, binding Hilt |
| [kotlin/FeatureViewModel.kt](kotlin/FeatureViewModel.kt) | `feature:balance:ui` | `UiState`, `UiEvent`, ViewModel UDF |
| [kotlin/FeatureScreen.kt](kotlin/FeatureScreen.kt) | `feature:balance:ui` | Route stateful, Screen stateless, previews por estado, a11y |
| [kotlin/FeatureViewModelTest.kt](kotlin/FeatureViewModelTest.kt) | `feature:balance:ui` (test) | Teste com ID de AC, Turbine, fake, dispatcher de teste |
| [kotlin/FakeBalanceRepository.kt](kotlin/FakeBalanceRepository.kt) | `core:testing` | Fake controlável que respeita o contrato |
| [kotlin/ArchitectureTest.kt](kotlin/ArchitectureTest.kt) | `quality:architecture-tests` | Regras Konsist (sensor S-06) |
| [kotlin/SensitiveDataInLogRule.kt](kotlin/SensitiveDataInLogRule.kt) | `quality:detekt-rules` | Regra detekt própria com mensagem para o agente (S-05) |
| [kotlin/QualityConventionPlugin.kt](kotlin/QualityConventionPlugin.kt) | `build-logic` | Tasks `harnessFast/Check/Full` (contrato com `verify.sh`) |
| [module-README.md](module-README.md) | todo módulo | README de módulo: propósito, API pública, grafo gerado, invariantes |

As versões exatas das bibliotecas serão fixadas no Version Catalog após a
aprovação do [ADR-0003](../../adr/0003-stack-de-sensores-de-qualidade.md); ajuste
pequenas diferenças de API na primeira compilação.
