# App de Venda de Ingressos com Cielo Lio

| | |
| :--- | :--- |
| **Escopo** | Venda de ingressos para eventos locais com fluxo de compra simples. |
| **Integração** | Pagamento por meio do ecossistema Cielo Smart / Cielo Lio. |
| **Qualidade** | Tratamento de erros, prevenção de duplicidade e testes críticos. |

## Desafio Técnico - Desenvolvedor(a) Mobile / Backend

### Contexto
Você foi contratado(a) para desenvolver um app de venda de ingressos para eventos locais, com foco em fluxo de compra simples e integração de pagamento com Cielo Smart. 

Após a entrega, haverá uma sessão de code review e apresentação para explicar decisões técnicas, trade-offs e possíveis evoluções.

### Objetivo
Construir uma solução funcional para venda de ingressos com as funcionalidades essenciais e integração com o ecossistema Cielo Smart.

### Funcionalidades Esperadas
* Listagem de eventos disponíveis
* Seleção de quantidade de ingressos por evento
* Pagamento via integração com a Cielo
* Geração de QR Code do ingresso (opcional)

### Requisitos Funcionais
Implemente ao menos os seguintes fluxos:
1. Visualizar eventos disponíveis para compra
2. Selecionar a quantidade de ingressos
3. Iniciar e concluir o pagamento via integração com a Cielo
4. Registrar o resultado da compra (aprovada, negada ou cancelada)
5. Exibir comprovante/resumo da compra

Se implementar QR Code, o ingresso deve estar vinculado à compra concluída.

### Requisitos Não-Funcionais
* A aplicação deve ter tratamento explícito de erros de integração e pagamento
* O fluxo de compra deve evitar duplicidade de cobrança em reenvio de ação
* O código deve ser organizado e de fácil manutenção
* O projeto deve possuir testes automatizados para cenários críticos
* Fazer uso de ferramenta(s) de IA como suporte a implementação do sistema de reserva de ingresso é esperado e será avaliado o "como" a IA foi usada para criar a solução.
* Não será avaliado a construção ou não de um backend de apoio para esse app.

### Restrições Técnicas
* Linguagem e framework: Kotlin (Android)
* Banco de dados: livre (se aplicável)
* O projeto deve ser executável com instruções claras no README
* Disponibilize a documentação do harness do agente, incluindo Specs, decisões arquiteturais, uso da IA (prompts e restrições) e os resultados que orientaram a implementação da solução
* É permitido (e recomendado) uso de bibliotecas externas

### Materiais Obrigatórios
* Emulador Cielo Smart: https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo
* Documentação de integração Cielo Smart: https://docs.cielo.com.br/cielo-smart/docs/conheca-a-cielo-smart

### Entrega
Crie um repositório público no GitHub com a solução
Envie o link no prazo combinado
Inclua no README:
* Instruções de execução
* Decisões arquiteturais
* Bibliotecas externas utilizadas e justificativas
* Como foi feita a integração com a Cielo Smart
* Trade-offs considerados
* O que faria com mais tempo

---
*Convertido de Markdown para PDF com formatação limpa.*