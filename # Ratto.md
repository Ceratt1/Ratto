# Ratto

#### Plataforma inteligente de apoio à aprendizagem com IA

Gabriel Ceratti Cabral
---

## Resumo do Projeto

O Ratto é uma plataforma de apoio à aprendizagem que permite ao usuário enviar PDFs e materiais de estudo para gerar questões, simulados e acompanhar seus principais pontos de dificuldade com apoio de inteligência artificial. O projeto busca mudar a forma como o aluno estuda, incentivando uma rotina mais ativa, baseada em testes de conhecimento, feedbacks e identificação de gaps. A solução transforma materiais próprios em questões, análises de desempenho e recomendações de estudo, ajudando o usuário a sair da zona de conforto e reforçar conteúdos que ainda não domina. Como consequência, o Ratto propõe uma experiência de aprendizagem mais personalizada, prática e direcionada.

---

## Definição do Problema

Muitos estudantes utilizam PDFs, apostilas e materiais digitais como principal fonte de estudo, porém nem sempre conseguem validar se realmente compreenderam o conteúdo. Em muitos casos, o estudo acaba sendo passivo, baseado apenas em leitura, marcações ou revisões repetitivas, sem uma análise clara sobre quais assuntos foram dominados e quais ainda precisam de reforço.

Esse cenário dificulta a identificação de lacunas de conhecimento, pois o estudante pode continuar revisando conteúdos que já entende, enquanto deixa de aprofundar temas nos quais possui maior dificuldade. Além disso, nem sempre o aluno sabe quais materiais complementares procurar, quais assuntos priorizar ou como transformar seu próprio conteúdo em uma rotina de estudo mais eficiente.

O Ratto surge como uma proposta para apoiar esse processo, permitindo que o usuário envie seus próprios PDFs, gere questões automaticamente, responda simulados, visualize seu desempenho e receba orientações sobre os pontos que precisam ser reforçados. A plataforma busca tornar o estudo mais ativo, personalizado e guiado pelo próprio desempenho do estudante.

Além do uso individual, o projeto possui potencial futuro para aplicação em ambientes corporativos, funcionando como um hub de treinamentos. Nesse contexto, empresas poderiam disponibilizar materiais internos, gerar avaliações e acompanhar o progresso dos colaboradores em seus estudos e capacitações.

---

## Objetivos

### Objetivo Geral

Desenvolver uma plataforma inteligente de apoio à aprendizagem que permita ao usuário transformar PDFs de estudo em questões, simulados, feedbacks e recomendações personalizadas, auxiliando na identificação de dificuldades e na melhoria da rotina de estudos.

### Objetivos Específicos

* Permitir o cadastro e login de usuários na plataforma;
* Permitir o envio de arquivos PDF de forma privada e segura;
* Armazenar os PDFs enviados em ambiente privado na AWS S3;
* Extrair o texto dos PDFs enviados;
* Gerar questões de múltipla escolha com apoio de inteligência artificial;
* Classificar questões por nível de dificuldade;
* Criar simulados com questões geradas a partir do conteúdo enviado;
* Corrigir respostas e apresentar desempenho básico do usuário;
* Identificar assuntos com maior quantidade de acertos e erros;
* Gerar feedbacks e recomendações de estudo com apoio de IA;
* Registrar histórico de tentativas e evolução do usuário;
* Estruturar uma arquitetura baseada em backend principal, microserviços e mensageria assíncrona.

---

## Stack Tecnológico

O Ratto será desenvolvido utilizando uma stack voltada para aplicações web modernas, processamento assíncrono e integração com serviços de inteligência artificial.

### Frontend

O frontend será desenvolvido com **Node.js** e **Next.js**, permitindo a criação de uma interface web moderna, responsiva e organizada por componentes. A interface será responsável por telas como login, upload de PDF, visualização de simulados, resultados e dashboard básico de desempenho.

### Backend Principal

O backend principal será desenvolvido em **Java com Spring WebFlux**, permitindo a construção de APIs reativas e preparadas para lidar com operações assíncronas e integrações com outros serviços. Esse backend será responsável por funcionalidades centrais como usuários, PDFs, simulados, respostas, histórico e dashboard.

### Microserviços

O projeto utilizará microserviços para separar responsabilidades específicas do processamento dos PDFs. Essa abordagem facilita a manutenção, evolução e escalabilidade de partes críticas do sistema, como extração de texto e geração de questões.

### Banco de Dados

O banco de dados utilizado será o **PostgreSQL**, responsável por armazenar dados dos usuários, PDFs enviados, textos extraídos, questões geradas, tentativas, respostas e histórico de desempenho.

### Mensageria

A comunicação assíncrona entre os serviços será realizada com **Apache Kafka**. O Kafka será utilizado para desacoplar etapas como upload do PDF, extração de texto e geração de questões, evitando que o usuário precise aguardar todo o processamento de forma bloqueante.

### Inteligência Artificial

A geração de questões, feedbacks, recomendações e explicações será feita com apoio de IA, inicialmente utilizando **Gemini**, com possibilidade de testes futuros com **DeepSeek**. A IA será responsável por analisar o conteúdo extraído dos PDFs, criar questões, indicar dificuldade e auxiliar na identificação de gaps de conhecimento.

### Armazenamento

Os PDFs enviados pelos usuários serão armazenados no **Amazon S3**, com acesso privado e controlado pela aplicação. Os arquivos não terão acesso público direto, garantindo que apenas o usuário proprietário possa acessá-los por meio da plataforma.

### Autenticação

A autenticação será realizada com **Keycloak**, permitindo cadastro, login e controle de identidade dos usuários.

---

## Descrição da Solução

O Ratto é uma plataforma web onde o usuário pode realizar login, enviar um PDF de estudo e receber questões geradas automaticamente com base no conteúdo do arquivo. Inicialmente, o sistema aceitará apenas arquivos PDF, com limite de 1 PDF por vez e tamanho máximo de 30 MB. Essa limitação existe para controlar custos, evitar abuso da plataforma e proteger os recursos de armazenamento, processamento e IA.

Após o envio, o PDF é armazenado de forma privada no Amazon S3 e uma mensagem é publicada em um tópico Kafka para iniciar o processamento assíncrono. Um serviço específico consome essa mensagem, realiza a extração do texto do PDF e publica o resultado em outro tópico. Em seguida, outro serviço consome o texto extraído e utiliza IA para gerar questões de múltipla escolha.

No MVP, cada PDF gerará inicialmente 5 questões, por controle de custo e simplicidade da primeira versão. As questões serão de múltipla escolha, com 4 alternativas e apenas uma correta. A IA também será responsável por indicar o nível de dificuldade da questão, como fácil, médio ou difícil. Inicialmente, os simulados terão dificuldades misturadas, permitindo avaliar o aluno em diferentes níveis de complexidade.

Depois que as questões forem geradas, o usuário poderá responder um simulado. O sistema corrigirá as respostas, apresentará quantidade de acertos, erros, percentual de aproveitamento e assuntos em que o usuário teve melhor ou pior desempenho. Além disso, o Ratto utilizará IA para explicar por que a alternativa correta está certa e por que as demais estão erradas.

O sistema também manterá o histórico das tentativas do usuário. Cada vez que um simulado for refeito, será registrada uma nova tentativa, armazenando questões, alternativas escolhidas, respostas corretas, erros, acertos, explicações e desempenho. Isso permitirá acompanhar a evolução do aluno ao longo do tempo.

No MVP, o Ratto também contará com recomendações básicas de estudo geradas por IA. Essas recomendações serão baseadas nos gaps identificados no desempenho do usuário e poderão sugerir temas para revisar, livros, vídeos, fóruns, artigos ou outros materiais complementares.

Como funcionalidades futuras, o Ratto poderá permitir organização dos PDFs em pastas, suporte a arquivos TXT e vídeos, geração de questões discursivas, escolha da quantidade de questões pelo usuário e criação de PDFs personalizados com “colas”, dicas, anotações e reforços baseados no desempenho individual do aluno.

---

## Arquitetura

A arquitetura do Ratto será composta por um frontend web, um backend principal em formato de monólito modular, microserviços especializados, mensageria com Kafka, armazenamento em S3, autenticação com Keycloak, banco de dados PostgreSQL e serviços de IA.

### Componentes principais

* **Frontend Next.js:** interface utilizada pelo usuário;
* **Keycloak:** autenticação e gerenciamento de usuários;
* **Backend principal / monólito modular:** gerencia usuários, PDFs, simulados, respostas, feedbacks, histórico e dashboard;
* **Serviço de upload:** envia o PDF para o S3 e publica mensagem no Kafka;
* **Serviço de extração:** consome o tópico de PDFs enviados, extrai o texto e publica o conteúdo extraído;
* **Serviço de geração de questões:** consome o texto extraído, gera questões com IA e publica o resultado;
* **Kafka:** comunicação assíncrona entre os serviços;
* **PostgreSQL:** persistência dos dados da aplicação;
* **Amazon S3:** armazenamento privado dos PDFs;
* **Gemini / DeepSeek:** serviços de IA utilizados para geração de questões, feedbacks e recomendações.

### Fluxo assíncrono com Kafka

1. O usuário envia um PDF pela plataforma;
2. O serviço de upload armazena o arquivo no Amazon S3;
3. Uma mensagem é publicada no tópico `knowledgement-topic`;
4. O serviço de extração escuta o tópico `knowledgement-topic`;
5. O texto do PDF é extraído;
6. O texto extraído é publicado no tópico `pdf-text-extracted-topic`;
7. O serviço de geração de questões escuta o tópico `pdf-text-extracted-topic`;
8. A IA gera questões de múltipla escolha com base no texto;
9. O resultado é publicado no tópico `study-problems-generated-topic`;
10. O monólito modular consome as questões geradas e salva no PostgreSQL, vinculando os dados ao usuário.

### Artefatos do Projeto

Os artefatos previstos para o projeto são:

1. **Diagrama de arquitetura do projeto**
   Ainda em construção.

2. **Diagrama de fluxo assíncrono com Kafka**
   Demonstra o caminho percorrido desde o upload do PDF até a geração e persistência das questões.
   Artefato renderizado: `docs/fluxo-ratto.svg`.

3. **Histórias de usuário**
   Ainda em construção.

4. **JSON schema**
   Ainda em construção.

5. **Plano de Negócios**
   Ainda em construção.

---

## Validação

A validação do Ratto será realizada com aproximadamente 1 a 5 estudantes que utilizam PDFs, apostilas ou materiais digitais como parte de sua rotina de estudos. O objetivo será verificar se a plataforma ajuda o usuário a estudar de forma mais ativa, identificar dificuldades e compreender melhor quais conteúdos precisam ser reforçados.

No MVP, não haverá uma funcionalidade interna para feedback dos usuários dentro da plataforma. A coleta será realizada externamente, por meio de questionários ou entrevistas simples durante a etapa de validação.

---

## Estratégia

A estratégia de validação será baseada no uso prático da plataforma pelos estudantes. Os participantes poderão enviar PDFs, gerar questões, responder simulados e visualizar seus resultados. Após o uso, serão coletadas percepções sobre facilidade de uso, qualidade das questões, utilidade do feedback, clareza dos gaps apresentados e percepção de melhora na forma de estudar.

Também poderá ser analisado o desempenho dos usuários nos simulados, considerando acertos, erros, percentual de aproveitamento e evolução entre tentativas.

---

## Consolidação dos Dados Coletados

Os dados coletados durante a validação serão organizados em forma de gráficos, médias e tabelas simples. Serão considerados critérios como:

* Facilidade de uso da plataforma;
* Qualidade das questões geradas;
* Clareza dos feedbacks apresentados;
* Utilidade das recomendações de estudo;
* Percepção de melhora na rotina de aprendizagem;
* Quantidade de acertos e erros nos simulados;
* Evolução entre tentativas.

Essas informações serão utilizadas para avaliar se o Ratto conseguiu cumprir seu objetivo de apoiar uma aprendizagem mais ativa, personalizada e direcionada.

---

## Conclusões

O Ratto propõe uma alternativa para estudantes que desejam transformar seus próprios materiais em uma experiência de estudo mais prática e ativa. Ao permitir o envio de PDFs, geração de questões, realização de simulados, análise de desempenho e recomendações personalizadas, a plataforma busca ajudar o usuário a identificar seus pontos fortes e fracos durante o processo de aprendizagem.

A solução também se diferencia por acompanhar a jornada do aluno, registrando tentativas, desempenho e evolução ao longo do tempo. Dessa forma, o Ratto não se limita a gerar perguntas, mas busca orientar o estudante sobre onde precisa melhorar e quais caminhos pode seguir para reforçar seu conhecimento.

---

## Limitações do Projeto e Perspectivas Futuras

Como limitação inicial, o MVP aceitará apenas arquivos PDF, com envio de 1 arquivo por vez e tamanho máximo de 30 MB. Também haverá geração inicial de apenas 5 questões por PDF, considerando controle de custo e uso dos serviços de IA.

Outra limitação é que, inicialmente, as recomendações e validações serão simples, baseadas na IA e no feedback externo dos estudantes durante a validação do projeto.

Como perspectivas futuras, o Ratto poderá incluir:

* Organização dos PDFs em pastas;
* Suporte a arquivos TXT;
* Suporte a vídeos;
* Geração de questões discursivas;
* Escolha da quantidade de questões pelo usuário;
* Geração de PDFs personalizados com dicas e “colas” baseadas nos gaps;
* Anotações inteligentes diretamente nos PDFs;
* Extração de imagens, gráficos e tabelas dos materiais;
* Uso em contexto corporativo para treinamentos e acompanhamento de colaboradores;
* Dashboard mais completo de evolução e desempenho.

---

## Referências Bibliográficas

AMAZON WEB SERVICES. **Amazon S3 Documentation**. Disponível em: https://docs.aws.amazon.com/s3/

APACHE SOFTWARE FOUNDATION. **Apache Kafka Documentation**. Disponível em: https://kafka.apache.org/documentation/

GOOGLE. **Gemini API Documentation**. Disponível em: https://ai.google.dev/

KEYCLOAK. **Keycloak Documentation**. Disponível em: https://www.keycloak.org/documentation

POSTGRESQL. **PostgreSQL Documentation**. Disponível em: https://www.postgresql.org/docs/

SPRING. **Spring Framework Documentation**. Disponível em: https://docs.spring.io/spring-framework/reference/

VERCEL. **Next.js Documentation**. Disponível em: https://nextjs.org/docs
