# Evidencias dos requisitos da entrega

Este documento mostra onde cada requisito da atividade aparece no projeto Ratto e como explicar isso de forma clara durante a apresentacao.

## Resumo rapido

| Requisito | Cumpre? | Onde mostrar |
|---|---:|---|
| Apresentacao clara para o professor | Sim | `ROTEIRO_ENTREGA.md`, `/`, `/app` |
| Buscar lista por API com GET | Sim | `frontend/src/features/study-app/services/study-client.ts` |
| Mostrar dados em tabela ou cards | Sim | `frontend/src/features/study-app/components/study-workspace.tsx` |
| Cadastro/envio por formulario com POST | Sim | `UploadForm`, formulario de pasta e services |
| Criacao adequada de componentes | Sim | `frontend/src/features/**/components` |
| Interface web funcional | Sim | `frontend/src/app/page.tsx`, `frontend/src/app/app/page.tsx` |
| Uso de rotas | Sim, com Next.js App Router | `frontend/src/app` |
| Controle de versao | Sim | repositorio Git e historico de commits |

## 1. Apresentacao clara para o professor

O projeto tem uma narrativa de produto e uma interface visual para demonstracao.

Arquivos principais:

- `ROTEIRO_ENTREGA.md`: explica o problema, a solucao, a stack e o cronograma.
- `frontend/src/app/page.tsx`: pagina publica de apresentacao do Ratto.
- `frontend/src/app/app/page.tsx`: entrada da area autenticada de estudos.
- `frontend/src/features/study-app/components/study-workspace.tsx`: tela principal da experiencia do estudante.

Como explicar:

> O Ratto e um sistema de estudos que transforma PDFs em provas praticas. A apresentacao comeca pela landing page publica, passa pela area do estudante e mostra o fluxo de criar uma prova, praticar perguntas e acompanhar desempenho.

## 2. Busca de dados por API usando GET

O frontend busca listas e detalhes da API usando chamadas `GET`.

Arquivo:

- `frontend/src/features/study-app/services/study-client.ts`

Exemplos:

```ts
export async function listWorkspaces(token: string): Promise<Workspace[]> {
  return studyRequest<Workspace[]>("/api/study/workspaces", { method: "GET" }, token);
}

export async function listProblemSets(
  token: string,
  params: { workspaceId?: string; unassigned?: boolean },
): Promise<ProblemSetSummary[]> {
  return studyRequest<ProblemSetSummary[]>(
    `/api/study/problem-sets?${search.toString()}`,
    { method: "GET" },
    token,
  );
}
```

O que essas chamadas buscam:

- `listWorkspaces`: lista as pastas/areas de estudo do usuario.
- `listProblemSets`: lista as provas geradas a partir dos PDFs.
- `getWorkspacePerformance`: busca o desempenho geral.
- `getProblemSetPerformance`: busca o desempenho de uma prova especifica.

Como explicar:

> A tela principal carrega dados reais da API com GET. Ela busca as areas de estudo, as provas disponiveis e os dados de desempenho do estudante.

## 3. Apresentacao dos dados em tabela ou cards

Os dados retornados pelo GET sao exibidos visualmente na area de estudos.

Arquivo:

- `frontend/src/features/study-app/components/study-workspace.tsx`

Trechos importantes:

- A lista de pastas usa `workspaces.map(...)`.
- A lista de provas usa `visibleProblemSets.map(...)`.
- Cada prova e renderizada como um `article` com classe `problem-row`.
- As provas ficam dentro de um container chamado `problem-table`.

Exemplo conceitual:

```tsx
<div className="problem-table">
  {visibleProblemSets.map((problemSet) => (
    <article className="problem-row" key={problemSet.id}>
      ...
    </article>
  ))}
</div>
```

Como explicar:

> Depois que a API retorna a lista de provas, o React percorre essa lista com `map` e cria uma linha/card para cada item. Cada card mostra nome do arquivo, descricao, quantidade de questoes, idioma e acao para praticar.

## 4. Cadastro/envio de dados com POST a partir de formulario

O projeto tem mais de um fluxo com formulario enviando dados para a API por `POST`.

### 4.1 Criacao de pasta/area de estudo

Arquivos:

- `frontend/src/features/study-app/components/study-workspace.tsx`
- `frontend/src/features/study-app/services/study-client.ts`

No componente `StudyWorkspace`, existe um formulario para criar ou editar uma pasta:

```tsx
<form className="inline-folder-form" onSubmit={saveWorkspace}>
  ...
</form>
```

Quando o formulario e enviado, a funcao `saveWorkspace` chama:

```ts
createWorkspace(token, workspaceName, workspaceDescription)
```

E o service envia:

```ts
export async function createWorkspace(
  token: string,
  name: string,
  description?: string,
): Promise<Workspace> {
  return studyRequest<Workspace>("/api/study/workspaces", {
    method: "POST",
    body: JSON.stringify({ name, description: description || undefined }),
  }, token);
}
```

Como explicar:

> O usuario preenche nome e objetivo da pasta. Ao enviar o formulario, o frontend faz um POST para cadastrar essa entidade na API.

### 4.2 Envio de PDF para gerar prova

Arquivos:

- `frontend/src/features/pdf-upload/components/upload-form.tsx`
- `frontend/src/features/pdf-upload/services/upload-client.ts`
- `frontend/src/app/api/uploads/prepare/route.ts`
- `frontend/src/app/api/uploads/confirm/route.ts`

O formulario principal e o `UploadForm`. Ele coleta:

- objetivo do estudo;
- idioma da prova;
- arquivo PDF.

Ao enviar, chama:

```ts
uploadPdfs(files, description, studyLanguage, await getToken(), workspaceId)
```

O service usa POST para preparar e confirmar o envio:

```ts
async function prepare(request: DirectUploadRequest, token: string): Promise<PreparedUpload> {
  return apiRequest<PreparedUpload>("/api/uploads/prepare", request, token);
}

async function confirm(command: ConfirmUploadCommand, token: string): Promise<UploadResult> {
  return apiRequest<UploadResult>("/api/uploads/confirm", command, token);
}
```

E `apiRequest` envia:

```ts
const response = await fetch(path, {
  method: "POST",
  headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
  body: JSON.stringify(body),
});
```

Como explicar:

> Alem do cadastro de pastas, existe o formulario de envio de material. O estudante informa os dados do estudo e envia o PDF. O frontend faz POST para preparar o envio e depois outro POST para confirmar a criacao da prova.

## 5. Criacao adequada de componentes

O frontend esta organizado por features e componentes reutilizaveis.

Componentes principais:

- `frontend/src/features/study-app/components/study-workspace.tsx`: tela principal da area de estudos.
- `frontend/src/features/study-app/components/app-shell.tsx`: estrutura da area autenticada.
- `frontend/src/features/pdf-upload/components/upload-form.tsx`: formulario de envio de PDF.
- `frontend/src/features/pdf-upload/components/file-list.tsx`: lista dos arquivos selecionados.
- `frontend/src/features/pdf-upload/components/upload-result-card.tsx`: resultado do envio.
- `frontend/src/features/auth/components/auth-provider.tsx`: contexto de autenticacao.
- `frontend/src/features/auth/components/auth-actions.tsx`: acoes de login/logout.
- `frontend/src/components/ui/button.tsx`: botao compartilhado.

Como explicar:

> A aplicacao nao esta toda em um unico arquivo. Ela separa responsabilidades por componentes: formulario, lista de arquivos, card de resultado, shell da aplicacao, autenticacao e tela principal.

## 6. Interface web funcional

Arquivos principais da interface:

- `frontend/src/app/page.tsx`: pagina publica.
- `frontend/src/app/app/page.tsx`: area de estudos.
- `frontend/src/app/styles.css`: estilos globais.
- `frontend/src/features/study-app/components/study-workspace.tsx`: fluxo principal.
- `frontend/src/features/pdf-upload/components/upload-form.tsx`: envio de material.

Fluxos que podem ser demonstrados:

1. Abrir a pagina publica `/`.
2. Entrar na area de estudos `/app`.
3. Criar uma pasta de estudo.
4. Enviar um PDF para gerar prova.
5. Ver a prova aparecer na lista.
6. Abrir uma prova e responder questoes.
7. Ver desempenho/acertos/erros.

Como explicar:

> A interface e funcional porque permite navegar, preencher formularios, buscar dados da API, renderizar listas, iniciar uma pratica e acompanhar desempenho.

## 7. Uso de rotas e explicacao do Next.js App Router

O requisito fala em React-Router, mas este projeto usa Next.js. Em projetos Next.js modernos, o sistema de rotas oficial e o **App Router**.

### O que e o Next.js App Router?

O App Router e o sistema de roteamento do Next.js baseado na pasta `src/app`.

Em vez de declarar rotas manualmente com `react-router-dom`, o Next cria as rotas a partir da estrutura de arquivos:

```txt
frontend/src/app/page.tsx          -> rota /
frontend/src/app/app/page.tsx      -> rota /app
frontend/src/app/app/loading.tsx   -> estado de carregamento da rota /app
frontend/src/app/app/error.tsx     -> estado de erro da rota /app
frontend/src/app/api/.../route.ts  -> rotas de API internas do Next
```

Ou seja:

- `page.tsx` define uma tela acessivel por URL.
- `layout.tsx` define estrutura compartilhada entre paginas.
- `loading.tsx` define o que aparece enquanto a rota carrega.
- `error.tsx` define o tratamento visual de erro da rota.
- `route.ts` define endpoints HTTP, como GET e POST.

### Rotas existentes no projeto

Rotas de pagina:

- `/`: definida em `frontend/src/app/page.tsx`.
- `/app`: definida em `frontend/src/app/app/page.tsx`.

Rotas de API/BFF:

- `/api/auth/config`: `frontend/src/app/api/auth/config/route.ts`.
- `/api/users/me`: `frontend/src/app/api/users/me/route.ts`.
- `/api/uploads/prepare`: `frontend/src/app/api/uploads/prepare/route.ts`.
- `/api/uploads/confirm`: `frontend/src/app/api/uploads/confirm/route.ts`.
- `/api/study/...`: `frontend/src/app/api/study/[...path]/route.ts`.
- `/api/study/processing-status`: `frontend/src/app/api/study/processing-status/route.ts`.

### Como defender esse ponto para o professor

Se o professor pediu "React-Router" no sentido generico de "usar rotas no React", o projeto cumpre usando o roteador oficial do Next.js.

Explicacao sugerida:

> Como o projeto foi feito em Next.js, eu usei o App Router, que e o sistema de rotas oficial do framework. Ele substitui a necessidade de configurar `react-router-dom` manualmente. As rotas sao criadas pela estrutura da pasta `src/app`: a pagina publica esta em `/`, a area autenticada esta em `/app`, e as integracoes com a API ficam em `/api/...`.

Ponto de atencao:

> Se a exigencia for literalmente instalar e usar `react-router-dom`, entao este projeto nao usa essa biblioteca. Ele usa uma alternativa equivalente e mais adequada para Next.js: o App Router.

## 8. Controle de versao com Git

O projeto esta em um repositorio Git.

Como provar:

```bash
git status
git log --oneline -5
```

Evidencia observada:

```txt
cae64fc feat: add environment configuration for EC2 and update CORS settings for S3
c70c09f feat: update AuthProvider to use check-sso for login flow and handle login redirection
d274371 feat: update Keycloak configuration to disable PKCE method and add empty attributes
d27c40c feat: enhance AuthProvider to handle authentication errors and improve loading states
ad59065 feat: simplify landing authentication by removing Keycloak integration
```

Como explicar:

> O projeto usa Git para versionamento. O historico mostra commits incrementais com mensagens descrevendo as alteracoes realizadas.

## Observacao final para apresentacao

O projeto cumpre os requisitos principais de frontend, API, formulario, listagem, componentes, rotas e versionamento.

O unico cuidado e a forma de apresentar o requisito de rotas: em vez de React Router puro, o projeto usa Next.js App Router. Isso e tecnicamente correto para uma aplicacao Next.js, mas vale explicar claramente para evitar confusao.
