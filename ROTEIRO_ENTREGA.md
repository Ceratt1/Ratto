# Ratto
## Aprendendo com IA

## Participantes do Projeto
1. Gabriel Ceratti Cabral

## Definição do Problema
Muitos estudantes e candidatos a concursos enfrentam dificuldade para compreender conteúdos densos, revisar materiais extensos e identificar por que uma alternativa está correta enquanto as demais estão erradas. Isso torna o estudo mais lento, cansativo e, em muitos casos, pouco eficiente para retenção de longo prazo.

Esse projeto surge como resposta direta a essa dor. Eu, Gabriel Ceratti, senti na prática a diferença de estudar com IA, principalmente quando comecei a transformar conteúdos complexos em perguntas com explicações claras sobre o motivo de uma alternativa estar correta e as outras estarem erradas. Esse formato tornou meu estudo mais ativo, mais leve e muito mais eficiente, inclusive no meu processo de aprendizagem de AWS. A proposta nasce justamente dessa experiência e busca levar esse mesmo ganho para estudantes, concurseiros e pessoas de diferentes idades que querem aprender com mais autonomia, clareza e constância.

## Proposta de Solução
A solução proposta é um software capaz de receber arquivos PDF, extrair seu conteúdo e gerar perguntas e respostas explicadas com apoio de inteligência artificial. A ideia central é não apenas indicar a resposta correta, mas também explicar por que ela está correta e por que as demais opções estão erradas, tornando o processo de estudo mais claro e eficiente.

Embora o foco inicial esteja em materiais de estudo, provas e conteúdos para concursos, a arquitetura foi pensada de forma genérica para permitir uso em múltiplos domínios. Com pequenas adaptações nas regras de geração e no formato de saída, o mesmo pipeline pode ser aplicado a contratos, laudos, manuais e outros documentos.

O MVP da disciplina terá como escopo:
- upload de um ou mais arquivos PDF;
- envio do material para processamento assíncrono;
- extração do conteúdo textual dos documentos;
- geração de perguntas e respostas explicadas a partir do texto extraído;
- consulta do status do processamento e do resultado final.

Ficam fora do escopo inicial funcionalidades mais robustas de produção, como OCR avançado para documentos muito degradados, autenticação completa com Keycloak, endurecimento de segurança e outras evoluções de infraestrutura.

## Proposta de Tecnologia (stack)
O projeto será desenvolvido com uma stack voltada para processamento assíncrono, escalabilidade e integração entre serviços:

- **Java 21** como linguagem principal do projeto.
- **Spring Boot** para acelerar a construção dos serviços e APIs.
- **Spring WebFlux** para lidar com fluxos reativos e processamento não bloqueante.
- **Apache Kafka** para comunicação assíncrona entre as etapas do pipeline.
- **Maven** para gerenciamento de dependências e build do projeto.
- **Docker Compose** para subir a infraestrutura local de mensageria durante o desenvolvimento.
- **Apache PDFBox** para leitura e extração de texto dos arquivos PDF.

Como evolução futura, a solução poderá incorporar autenticação com Keycloak, OCR mais robusto e integração com infraestrutura em nuvem, ampliando seu potencial de uso comercial.

## Cronograma
A condução do projeto será feita semanalmente, com foco em construir um MVP funcional e comercialmente promissor, iniciando pela base técnica e avançando até a demonstração final.

| Semana | Atividade Disciplina | Atividades do Projeto |
|---|---|---|
| 24/04 | Mentoria | Definição do problema, proposta do produto, alinhamento do escopo inicial e organização do roteiro de desenvolvimento. |
| 08/05 | Mentoria | Montagem do ambiente, estruturação do repositório, configuração inicial dos módulos e preparação da infraestrutura local com Docker Compose e Kafka. |
| 15/05 | Seminário Andamento | Apresentação da arquitetura inicial, desenho do fluxo do sistema e prototipação da entrada de PDFs e das saídas esperadas. |
| 22/05 | Seminário Andamento | Desenvolvimento da etapa de upload de arquivos PDF, recebimento pela API e envio do processamento para a fila. |
| 29/05 | Mentoria | Implementação da extração de conteúdo dos PDFs, organização inicial do fluxo assíncrono e integração entre os componentes principais. |
| 12/06 | Mentoria | Desenvolvimento da geração de perguntas e respostas explicadas com base no conteúdo extraído, refinando o valor de estudo da solução. |
| 19/06 | Mentoria | Ajustes de integração, validações, testes do fluxo ponta a ponta e melhoria da qualidade das respostas geradas. |
| 26/06 | Entrega 3 | Consolidação do MVP, revisão da documentação, preparação da demonstração funcional e fechamento da entrega parcial. |
| 03/07 | Apresentação Final | Estruturação da apresentação final, definição da narrativa do problema, da solução e da proposta de valor do produto. |
| 10/07 | Apresentação Final | Refinamento da demonstração, ajustes visuais e revisão dos principais pontos técnicos e comerciais da apresentação. |
| 17/07 | Apresentação Final | Apresentação final do projeto com demonstração do MVP e visão de evolução do produto para uso comercial. |

## Considerações Finais
O projeto Ratto busca unir inteligência artificial e aprendizado ativo para transformar conteúdos densos em experiências de estudo mais acessíveis, eficientes e escaláveis. Além de atender à proposta acadêmica da disciplina, ele também representa uma solução em que eu realmente acredito porque senti na prática como a IA pode melhorar a forma de estudar. Por isso, a proposta também foi pensada com potencial de continuidade após a entrega, abrindo espaço para validação de mercado e futura comercialização.
