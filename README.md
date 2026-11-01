# SmartStock

Sistema web para **controle de estoque, produção e entregas** de uma metalúrgica, criado para o desafio da
**DDA Metalúrgica** no Projeto Aplicado III (SENAI/SC – Fraiburgo).

Com ele a empresa acompanha o estoque de materiais (barras, tubos, tintas, eletrodos, discos, ferragens...), registra
entradas e saídas, conduz cada pedido por uma **ordem de produção** em etapas, controla as entregas em um calendário,
vê o lado financeiro (preços por fornecedor, vendas e margem) e conversa entre os setores dentro do próprio sistema.

> O sistema foi pensado a partir de "pastilhas industriais", por isso o código e as abas ainda usam o nome
> *Pastilhas* para os itens de estoque. Os dados de exemplo mostram materiais variados de uma metalúrgica.

## Sumário

1. [O que cada parte faz](#1-o-que-cada-parte-faz)
2. [O que é preciso para rodar](#2-o-que-é-preciso-para-rodar)
3. [Instalando as ferramentas](#3-instalando-as-ferramentas)
4. [Baixando e rodando o sistema](#4-baixando-e-rodando-o-sistema)
5. [Entrando no sistema](#5-entrando-no-sistema)
6. [Roteiro rápido de uso](#6-roteiro-rápido-de-uso)
7. [Como o sistema funciona por dentro](#7-como-o-sistema-funciona-por-dentro)
8. [Funcionalidades em detalhe](#8-funcionalidades-em-detalhe)
9. [Estrutura do projeto](#9-estrutura-do-projeto)
10. [API (resumo)](#10-api-resumo)
11. [Configurações](#11-configurações)
12. [Problemas comuns](#12-problemas-comuns)
13. [Próximos passos e créditos](#13-próximos-passos-e-créditos)
14. [Publicando na AWS (EC2)](#14-publicando-na-aws-ec2)
15. [Documentação do projeto (relatório)](#15-documentação-do-projeto-relatório)

---

## 1. O que cada parte faz

| Aba | Para que serve | Quem acessa* |
|-----|----------------|--------------|
| **Painel** | Visão geral: totais, itens com estoque crítico e últimas movimentações. | Todos (mostra só o que a conta pode ver) |
| **Pastilhas** | Cadastro dos itens de estoque: código, descrição, fabricante, estoque mínimo, quantidade e foto. Busca e galeria de imagens. | Estoque |
| **Movimentações** | Registro de **entradas** (inclusive várias de uma vez, como uma compra geral) e **saídas** de estoque. | Movimentações |
| **Progresso** | Ordem de produção em 4 colunas (Aguardando → Em produção → Em estoque → Pronto para entrega), com checklist, materiais, cliente e envio do pedido. Permite repetir projetos e usar modelos. | Progresso |
| **Calendário** | Pedidos enviados, meta de entrega, calendário do mês e histórico de entregas. | Progresso |
| **Fornecedores** | Cadastro de fabricantes e fornecedores e **o que cada um vende, com o preço**. | Fornecedores |
| **Financeiro** | Preço de cada item por fornecedor, vendas e planos feitos para clientes, custo e margem dos projetos. | Financeiro |
| **Mensagens** | Conversa entre as contas (cada conta é um setor), com imagens e emojis. | Todos |
| **Usuários** | O administrador cria contas e escolhe a quais abas cada uma tem acesso. | Administrador |

\* *As permissões são por aba e definidas pelo administrador. O administrador acessa tudo.*

## 2. O que é preciso para rodar

| Ferramenta | Versão | Para quê |
|------------|--------|----------|
| **JDK (Java)** | **21** | Roda o servidor (backend) |
| **Node.js** (com npm) | **20.19+** ou **22.12+** (testado com a versão 24) | Roda a interface (frontend) |
| **Git** | qualquer recente | Baixar o projeto |
| Navegador | Chrome, Edge ou Firefox atuais | Usar o sistema |
| Internet | sempre | Baixar as dependências (Maven e npm) e acessar o banco no Supabase |
| Conta no **Supabase** | plano gratuito serve | Banco de dados PostgreSQL na nuvem |

**Não é preciso instalar Maven nem banco de dados no computador:** o projeto traz o *Maven Wrapper* (`mvnw`), que
baixa o Maven sozinho na primeira execução, e o banco é um **PostgreSQL no Supabase** (na nuvem). Os dados ficam salvos
lá e não se perdem ao reiniciar. Para rodar sem internet existe o perfil `h2` (banco em memória, veja o passo 4.2).

Portas usadas: **8080** (backend) e **5173** (frontend). Elas precisam estar livres.

## 3. Instalando as ferramentas

Se você já tem Java 21, Node.js e Git, pule para o [passo 4](#4-baixando-e-rodando-o-sistema). Para conferir:

```bash
java -version   # deve mostrar a versão 21
node -v         # deve mostrar v20.19+ ou v22.12+ (ex.: v24)
git --version
```

**Windows** (PowerShell; feche e abra o terminal depois de instalar):

```powershell
winget install Microsoft.OpenJDK.21
winget install OpenJS.NodeJS.LTS
winget install Git.Git
```

**macOS** (com [Homebrew](https://brew.sh)):

```bash
brew install --cask temurin@21
brew install node git
```

**Ubuntu / Debian:**

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk git
# Node.js atual, via nvm:
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
# (reabra o terminal) e então:
nvm install --lts
```

## 4. Baixando e rodando o sistema

São **dois programas**, cada um em um terminal: o backend (servidor) e o frontend (interface).

### 4.1 Baixar o projeto

```bash
git clone https://github.com/Roj10/SmartStock.git
cd SmartStock
```

### 4.2 Terminal 1 – Backend (porta 8080)

**Antes da primeira vez: o arquivo `.env`.** O backend usa três segredos que não ficam no código, porque o repositório
é público: a **senha do banco** (*Project Settings › Database*), a **chave secreta** do projeto (*Project Settings ›
API Keys › Secret keys*, começa com `sb_secret_`), usada para guardar fotos e arquivos no Supabase Storage, e o
**`SMARTSTOCK_SECRET`**, um segredo aleatório do próprio sistema que assina os logins e cifra os dados do 2FA (o
`.env.example` mostra o comando para gerar). Copie `backend/.env.example` para `backend/.env` e preencha os três. O
`.env` é ignorado pelo git. A conexão com
o banco é pelo *Session pooler* (`aws-0-us-east-2.pooler.supabase.com`, usuário `postgres.brlmkluxhgeaavzaojis`).

```powershell
cd backend
copy .env.example .env      # macOS/Linux: cp .env.example .env
notepad .env                # preencha SUPABASE_DB_PASSWORD, SMARTSTOCK_SECRET e SUPABASE_SERVICE_KEY
```

**Recomendado: conexão verificada com o banco.** Baixe o certificado do Supabase em *Database Settings › SSL
Configuration › Download certificate* e salve como `backend/supabase-ca.crt`. Com ele, o backend usa `verify-full`:
além de criptografar, confere que do outro lado é mesmo o Supabase. Sem ele, a conexão é só criptografada (o terminal
avisa). Na mesma tela dá para ligar *Enforce SSL on incoming connections*, para o Supabase recusar conexões sem
criptografia.

Na primeira execução o backend cria as tabelas (no schema `smartstock`), os buckets do Storage (`pastilhas`, `mensagens`
e `projetos`) e grava os dados de exemplo. Nas próximas, ele usa o que já está lá: os exemplos não são recriados. Se
ainda houver arquivos de versões antigas nas pastas `backend/uploads` e `backend/uploads-privado`, eles são enviados
ao Storage e apagados do computador (só depois de enviados com sucesso).

**Windows (PowerShell):**

```powershell
cd backend
.\mvnw.cmd clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

**macOS / Linux:**

```bash
cd backend
chmod +x mvnw
./mvnw clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Aguarde aparecer a mensagem `Started BackendApplication`. A primeira execução demora mais, porque baixa as
dependências. **Rode sempre a partir da pasta `backend`** (é onde fica o `.env`).

**Sem internet / só para testar:** `java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=h2` usa um
banco em memória, que volta aos dados de exemplo a cada reinício e não toca no Supabase.

### 4.3 Terminal 2 – Frontend (porta 5173)

```bash
cd frontend
npm install
npm run dev
```

(`npm install` só é necessário na primeira vez.)

### 4.4 Abrir no navegador

Acesse **http://localhost:5173** e entre com uma das contas da próxima seção.

### 4.5 Parar e reiniciar

- Para parar cada programa, use **Ctrl + C** no terminal dele.
- **Os dados ficam salvos no Supabase**: reiniciar o backend não apaga nada. Para voltar aos dados de exemplo, apague o
  schema `smartstock` no Supabase (*SQL Editor*: `drop schema smartstock cascade;`) e inicie o backend de novo.
- Depois de alterar o código do backend, rode de novo o `clean package` e o `java -jar`. Se o `clean` falhar dizendo
  que o arquivo `.jar` está em uso, pare o backend antes (Ctrl + C).

## 5. Entrando no sistema

O login tem **duas etapas para todas as contas**: a senha e um código de 6 dígitos do **app autenticador** no celular
(Google Authenticator, Microsoft Authenticator ou Authy). No primeiro login de cada conta o sistema mostra um QR code
para configurar o app e entrega **8 códigos de recuperação** (cada um vale uma vez, para quando a pessoa estiver sem o
celular). Quem perder o celular e os códigos pede ao administrador para **Redefinir 2FA** na aba Usuários.

Contas de demonstração, criadas automaticamente (no primeiro login **a troca da senha é obrigatória**: as senhas abaixo
são públicas e deixam de funcionar):

| Usuário | Senha | Perfil | Abas liberadas |
|---------|-------|--------|----------------|
| `admin` | `admin123` | Administrador | Todas, incluindo Usuários |
| `operador` | `operador123` | Funcionário | Pastilhas, Movimentações, Progresso e Calendário (estoque e produção) |
| `financeiro` | `financeiro123` | Funcionário | Financeiro e Fornecedores |

A aba **Mensagens** é liberada para todas as contas. As senhas ficam criptografadas no banco (BCrypt) e nunca são
mostradas: o administrador só pode redefini-las, e quem recebe uma senha definida por outra pessoa troca no login.
Senhas novas precisam ter 8 caracteres ou mais, com letras e números.

> Dica: o login é guardado **por aba do navegador**. Para ver duas contas conversando, abra uma aba para cada conta
> (ou uma janela anônima).

## 6. Roteiro rápido de uso

1. Entre como **admin** e abra **Pastilhas**: veja os itens, as fotos (digite `imagens` na busca para ver a galeria)
   e os alertas de estoque crítico.
2. Em **Movimentações**, registre uma **entrada** com vários itens de uma vez (como uma compra geral) e uma **saída**.
3. Em **Progresso**, clique em **+ Novo projeto**: comece em branco, **repetindo** um projeto ou usando um **modelo**.
   Avance o projeto pelas etapas; na última, informe o cliente e **envie o pedido** (o estoque é baixado sozinho).
4. No **Calendário**, veja o pedido na data da meta de entrega, clique no dia para localizá-lo e marque como entregue.
5. No **Financeiro**, confira preços por fornecedor, o custo dos materiais de cada projeto e a margem das vendas.
6. Em **Mensagens**, converse com outro setor; em **Usuários**, crie uma conta nova e escolha as abas dela.

## 7. Como o sistema funciona por dentro

```
 Navegador (React, porta 5173)  ──HTTP/JSON + token JWT──▶  API Spring Boot (porta 8080)  ──▶  PostgreSQL (Supabase)
        telas e formulários                                  regras de negócio e permissões      + Supabase Storage
```

- **Frontend (React):** só mostra as telas e chama a API. Guarda o token de login e esconde do menu as abas que a
  conta não pode acessar.
- **Backend (Spring Boot):** valida o login (JWT), confere a permissão de cada rota e aplica as regras do negócio.
  Mesmo que alguém tente chamar a API "na mão", sem a permissão recebe erro 403.
- **Banco de dados (PostgreSQL no Supabase):** guarda usuários, itens, movimentações, projetos, vendas e mensagens, no
  schema `smartstock` (fora do `public`, que o Supabase expõe pela API REST dele). As tabelas são criadas e atualizadas
  por **migrations** (Flyway): arquivos SQL numerados em `backend/src/main/resources/db/migration` (`V1__...`,
  `V2__...`), aplicados em ordem, uma única vez cada, ao iniciar o backend. Para mudar o banco, cria-se um arquivo novo
  (`V3__...`) — nunca se altera um que já rodou. O Hibernate só **confere** se as tabelas batem com o código. Os dados de
  exemplo entram só quando o banco está vazio.
- **Imagens e arquivos (Supabase Storage):** fotos dos itens ficam no bucket público `pastilhas` (abrem direto pelo
  endereço). Imagens das conversas e arquivos dos projetos ficam nos buckets **privados** `mensagens` e `projetos`: só o
  backend lê, com a chave secreta, e entrega a quem tem permissão. O tipo de cada arquivo é conferido pelo conteúdo.

**Caminho de um pedido (ordem de produção):**

```
Aguardando início → Em produção → Em estoque → Pronto para entrega ──(enviar pedido)──▶ Pedido enviado ──▶ Entregue
   (fila)           (checklist)   (já feito,      (informa cliente)    baixa o estoque       (Calendário)    (histórico)
                                   não saiu)                           dos materiais
```

- Só se avança ou volta **uma etapa por vez**, e as etapas do checklist só podem ser marcadas em **Em produção**.
- **Editar** ou **excluir** um projeto pede a **senha do administrador**; mudar de etapa e enviar o pedido, não.
- Ao **enviar o pedido**, o sistema dá baixa no estoque com os materiais do projeto. Se faltar qualquer item, o envio é
  bloqueado e **nada** é baixado.

## 8. Funcionalidades em detalhe

**Acesso e usuários**
- Login com JWT. O administrador cria contas e define a quais abas cada uma tem acesso. Contas sem permissão recebem
  403 da API e nem veem a aba no menu.

**Segurança**
- **2FA obrigatório** (TOTP, padrão dos apps autenticadores) com QR code gerado pelo próprio backend, códigos de
  recuperação de uso único e proteção contra reuso do mesmo código. O segredo de cada conta fica **cifrado** no banco
  (AES-256-GCM) com a chave do servidor.
- **Login em etapas** com tokens curtos (5 min) entre uma etapa e outra: o token de acesso só sai depois da senha, do
  2FA e da troca de senha (quando exigida). O login vale 8 horas; **Sair** derruba todas as sessões da conta, e trocar a
  senha ou redefinir o 2FA também.
- **Limite de tentativas**: 5 erros seguidos de senha, de código do 2FA ou da senha do administrador bloqueiam aquela
  conta por 15 minutos.
- Quem tem só a aba Usuários **não pode** aumentar as próprias permissões nem dar a outros abas que não tem.
- **Eventos de segurança** (logins, falhas, bloqueios, 2FA, senhas, contas, uso da senha do administrador) ficam
  registrados com data, conta e IP, sem senhas nem códigos; o administrador vê em *Usuários › Eventos de segurança*.
- **Caminho dos dados**: CORS só para o endereço do frontend; cabeçalhos de segurança (sem `iframe`, sem `Referer`,
  CSP); erros sem detalhes internos; conexão com o banco por TLS (verificada com o certificado do Supabase);
  **RLS ligado** em todas as tabelas e nenhum acesso pelas chaves públicas do Supabase (`anon`/`authenticated`);
  arquivos privados só passam pelo backend.

**Pastilhas (itens de estoque)**
- Cadastro com código, descrição, fabricante, estoque mínimo, estoque atual (inicial) e imagem.
- Imagem por upload, **arrastando** o arquivo ou clicando; as fotos podem ser ampliadas com um clique.
- Digitar `imagem` ou `imagens` na busca mostra a galeria; clicar numa foto leva até o item na tabela.
- Tabela com barra de estoque, faixa vermelha nos itens críticos e rolagem só dentro da tabela.

**Movimentações**
- Duas colunas (**Entradas** e **Saídas**), com os 30 registros mais recentes de cada.
- A entrada aceita vários itens de uma vez, cada linha com quantidade e fornecedor; se uma linha falhar, nenhuma é
  gravada. A saída valida estoque insuficiente.

**Progresso de produção**
- Quadro de 4 colunas com número de ordem (OP-001...), fila dos mais antigos primeiro, checklist editável e materiais
  ligados ao cadastro de itens (com aviso quando a quantidade pedida passa do estoque).
- **Repetir e modelos:** o botão **+ Novo projeto** pergunta como começar (em branco, repetindo um projeto existente —
  inclusive entregues — ou usando um modelo padrão). Os campos vêm preenchidos para ajustar. Cada card tem o atalho
  **Repetir**, e é possível **salvar um projeto como modelo** (se o nome já existir, o modelo é atualizado).
- **Arquivos do projeto:** no **+ Novo projeto** dá para anexar PDFs, desenhos de CAD (DWG, DXF, STEP, STL...),
  imagens, documentos, planilhas ou ZIP — arrastando ou clicando, vários de uma vez, até 20 MB cada e 20 por projeto.
  Os arquivos aparecem no card: PDF e imagens abrem numa nova aba; os demais são baixados. Ao **Repetir** um projeto,
  os arquivos vêm junto (dá para tirar algum antes de salvar). Incluir ou remover arquivos de um projeto que já existe
  é feito pelo **Editar** e, como toda edição, pede a senha do administrador. Os arquivos ficam no bucket privado
  `projetos` do Supabase Storage, sem link público: só quem tem acesso ao Progresso consegue abri-los.
- **Editar e excluir exigem a senha do administrador:** ao editar um projeto, o formulário só salva com a senha de uma
  conta administradora; ao excluir, ela é pedida num pop-up. A senha é conferida no servidor. Criar, repetir, avançar
  ou voltar etapas, marcar o checklist e enviar o pedido continuam sem senha.

**Calendário de entregas**
- Calendário mensal com o dia colorido (laranja = meta de entrega, verde = entregue) e os projetos dentro do dia.
- Clicar num dia com pedido destaca o pedido em **Pedidos em aberto**. Pedidos com meta vencida ganham o selo
  **Atrasado**. Datas do pedido e da meta são editáveis; há o botão **Marcar como entregue** e o histórico.

**Fornecedores**
- Cadastro de fabricantes e fornecedores, com a lista de **produtos que cada um entrega e o preço praticado**.

**Financeiro**
- *Peças e valores:* preço de cada fornecedor por item (o menor em destaque), estoque e valor parado em estoque.
- *Vendas e planos:* planos feitos para clientes sobre os projetos; ao fechar, viram venda. Mostra custo e margem.
- *Projetos:* custo dos materiais (usa o menor preço entre os fornecedores) frente ao valor planejado ou vendido.

**Mensagens**
- Conversa direta entre contas, com setor, última mensagem, contador de não lidas no menu, aviso de nova mensagem
  e atualização automática. Enter envia e Shift + Enter quebra a linha.
- **Emojis** (seletor por categorias) e **imagens** (botão, arrastar ou colar com Ctrl + V; PNG, JPG, WEBP ou GIF
  até 5 MB). Imagens são privadas: só quem enviou ou recebeu consegue baixá-las.

## 9. Estrutura do projeto

```
SmartStock/
├── backend/                         Servidor (Java 21 + Spring Boot 3.5)
│   ├── pom.xml                      Dependências do backend
│   ├── mvnw / mvnw.cmd              Maven Wrapper (dispensa instalar o Maven)
│   └── src/main/
│       ├── java/com/smartstock/backend/
│       │   ├── controller/          Rotas da API (uma por módulo)
│       │   ├── service/             Regras de negócio
│       │   ├── repository/          Acesso ao banco (Spring Data JPA)
│       │   ├── model/               Entidades (tabelas)
│       │   ├── dto/                 Formatos de entrada e saída da API
│       │   ├── security/            Login JWT e leitura do usuário logado
│       │   ├── config/              Segurança, CORS, arquivos e dados de exemplo (DataSeeder)
│       │   └── exception/           Tratamento de erros
│       └── resources/
│           ├── application.properties   Configurações
│           ├── db/migration/            Migrations do banco (Flyway): V1 esquema inicial, V2 segurança/2FA
│           ├── static/openapi.yaml      Documentação da API com exemplos (aberta em /swagger-ui.html)
│           └── seed-images/             Fotos dos dados de exemplo (+ CREDITOS.md)
└── frontend/                        Interface (React 19 + Vite)
    ├── package.json                 Dependências e comandos do frontend
    └── src/
        ├── pages/                   Uma tela por aba (Pastilhas, Progresso, Calendário...)
        ├── components/              Peças reutilizáveis (menu, pop-up, seletor de emojis...)
        ├── context/                 Estado de login e permissões
        ├── api/client.js            Configuração das chamadas à API
        └── index.css, chat.css      Estilos
├── deploy/                          Publicação na AWS EC2 (Nginx, serviço systemd, scripts)
└── docs/                            Relatório: requisitos, casos de uso, diagramas, descrição comercial
```

## 10. API (resumo)

Todas as rotas começam com `/api` e, exceto as do login, exigem o token de acesso no cabeçalho
`Authorization: Bearer ...`. Criações respondem **201 Created**, exclusões **204 No Content** e erros vêm sempre no
formato `{"status": 409, "message": "...", "timestamp": "..."}`.

**Documentação interativa (Swagger):** com o backend rodando, abra **http://localhost:8080/swagger-ui.html**. Lá estão
todas as 66 rotas, com descrição, quem pode usar, exemplos de envio e respostas reais. Para testar pela página: faça o
login em `POST /api/auth/login` (botão *Try it out*), siga as etapas do 2FA, copie o `token` final, clique em
**Authorize** e cole. No seletor do topo há também a versão gerada automaticamente do código (*Gerado do codigo*).

**Exemplo com `curl`** (login em etapas e um cadastro):

```bash
# 1) usuário e senha -> devolve a próxima etapa e um "desafio" (vale 5 minutos)
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d '{"username":"admin","senha":"SUA_SENHA"}'
# {"etapa":"CODIGO_2FA","desafio":"eyJ...","token":null,...}

# 2) código de 6 dígitos do app autenticador -> devolve o token de acesso (vale 8 horas)
curl -s -X POST http://localhost:8080/api/auth/2fa/verificar -H "Content-Type: application/json" \
     -d '{"desafio":"eyJ...","codigo":"123456"}'
# {"etapa":"CONCLUIDO","token":"eyJ...","username":"admin","role":"ADMIN",...}

# 3) usar o token: cadastrar um fornecedor (201 Created) e excluir (204 No Content)
TOKEN="eyJ..."
curl -s -X POST http://localhost:8080/api/fornecedores -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" -d '{"nome":"Aço Sul","tipo":"FORNECEDOR","telefone":"(49) 3246-1000"}'
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE http://localhost:8080/api/fornecedores/5 -H "Authorization: Bearer $TOKEN"
```

| Módulo | Rotas principais | Permissão |
|--------|------------------|-----------|
| Login | `POST /auth/login`, `/auth/2fa/configurar`, `/auth/2fa/ativar`, `/auth/2fa/verificar`, `/auth/trocar-senha` (etapas, com o token curto `desafio`); `POST /auth/sair` | pública / conta logada |
| Painel | `GET /dashboard` | conta logada |
| Pastilhas | `GET/POST /pastilhas`, `PUT/DELETE /pastilhas/{id}`, `POST/DELETE /pastilhas/{id}/imagem`, `GET /pastilhas/alertas` | leitura: Estoque, Movimentações, Progresso ou Fornecedores; escrita: Estoque |
| Movimentações | `GET /movimentacoes`, `POST /movimentacoes/entrada`, `/entrada/lote`, `/saida` | Movimentações |
| Fornecedores | `GET/POST /fornecedores`, `PUT/DELETE /fornecedores/{id}`, `GET /fornecedores/produtos`, `PUT /fornecedores/{id}/produtos` | Fornecedores |
| Projetos | `GET /projetos/progresso`, `/projetos/calendario`, `POST /projetos`, `PUT/DELETE /projetos/{id}` (com `senhaAdmin` no corpo), `GET /projetos/{id}/anexos/{anexoId}`, `POST /projetos/{id}/anexos`, `DELETE /projetos/{id}/anexos/{anexoId}`, `PATCH /projetos/{id}/status`, `POST /projetos/{id}/enviar-pedido`, `POST /projetos/{id}/entregar` | Progresso |
| Modelos | `GET/POST /modelos-projeto`, `GET/PUT/DELETE /modelos-projeto/{id}` | Progresso |
| Financeiro | `GET /financeiro/resumo`, `/pecas`, `/projetos`; `GET/POST /vendas`, `GET/PUT/DELETE /vendas/{id}`, `POST /vendas/{id}/fechar` | Financeiro |
| Mensagens | `GET /mensagens/contatos`, `/nao-lidas`, `GET/POST /mensagens/conversa/{usuarioId}`, `GET /mensagens/{id}/imagem` | conta logada |
| Usuários | `GET/POST /usuarios`, `PUT/DELETE /usuarios/{id}`, `POST /usuarios/{id}/redefinir-2fa` (admin); `GET /seguranca/eventos` (admin) | Administrador ou "Usuários" |

Para ver os dados: no Supabase, *Table Editor* (escolha o schema `smartstock`) ou *SQL Editor*. No perfil `h2` existe o
console em `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:smartstock`, usuário `sa`, senha em branco).

## 11. Configurações

**Backend** – arquivo `backend/src/main/resources/application.properties`:

| Chave | O que controla |
|-------|----------------|
| `server.port` | Porta do servidor (padrão 8080) |
| `spring.datasource.*` | Conexão com o Supabase. A senha vem de `SUPABASE_DB_PASSWORD` (arquivo `backend/.env` ou variável de ambiente); endereço e usuário podem ser trocados com `SUPABASE_DB_URL` e `SUPABASE_DB_USER` |
| `app.seguranca.segredo` | Vem de `SMARTSTOCK_SECRET` (`.env`): assina os logins e cifra os segredos do 2FA. Se mudar, todos entram de novo e reconfiguram o 2FA |
| `jwt.expiration-ms` | Validade do login (padrão 8 h) |
| `app.cors.origens` | Endereços do frontend que podem chamar a API (`CORS_ORIGENS`; padrão `http://localhost:5173`) |
| `app.supabase.db-ca` | Certificado do Supabase para a conexão verificada (padrão `supabase-ca.crt`, na pasta `backend`) |
| `app.supabase.url` / `app.supabase.service-key` | Endereço do projeto e chave secreta para o Supabase Storage (a chave vem de `SUPABASE_SERVICE_KEY`, no `.env`) |
| `app.armazenamento` | `supabase` (padrão) ou `local` (perfil `h2`: arquivos em `backend/armazenamento-local`) |
| `spring.servlet.multipart.max-file-size` | Tamanho máximo de cada arquivo enviado (20 MB; imagens de itens e conversas seguem limitadas a 5 MB) |

**Frontend** – o frontend chama a API sempre no **mesmo endereço do site** (`/api/...`). No computador, o Vite repassa
essas chamadas para `http://localhost:8080` (veja `frontend/vite.config.js`); no servidor, quem repassa é o Nginx. Se o
backend estiver em outro endereço, defina `VITE_API_URL` (ex.: `https://api.exemplo.com/api`) num arquivo
`frontend/.env` antes do `npm run build`, ou `SMARTSTOCK_BACKEND` ao rodar o `npm run dev`.

**Dados de exemplo:** são criados pelo `DataSeeder` na inicialização e simulam o estoque de uma metalúrgica. As fotos
estão em `backend/src/main/resources/seed-images/` e são de uso livre (Wikimedia Commons); autor e licença de cada uma
estão em [CREDITOS.md](backend/src/main/resources/seed-images/CREDITOS.md).

## 12. Problemas comuns

| Sintoma | O que fazer |
|---------|-------------|
| `java: command not found` ou versão diferente de 21 | Instale o JDK 21 e reabra o terminal (veja o [passo 3](#3-instalando-as-ferramentas)). |
| `Port 8080 was already in use` | Outro programa usa a porta. Pare-o ou mude `server.port` (e o endereço no frontend). |
| A tela abre, mas o login dá erro ou nada carrega | O backend não está rodando. Confira o Terminal 1 e a mensagem `Started BackendApplication`. |
| `npm run dev` falha logo de início | Confira `node -v` (precisa de 20.19+ ou 22.12+; o Node 21 não serve) e rode `npm install` de novo. |
| `mvnw: Permission denied` (macOS/Linux) | Rode `chmod +x mvnw`. |
| `clean` falha: arquivo `.jar` em uso (Windows) | Pare o backend (Ctrl + C) e rode o comando de novo. |
| `Could not resolve placeholder 'SUPABASE_DB_PASSWORD'` | Falta o arquivo `backend/.env` (passo 4.2) ou o backend não foi iniciado de dentro da pasta `backend`. |
| `password authentication failed` | Senha errada no `.env`. Confira no Supabase (*Project Settings › Database*) ou redefina a senha lá. Se a senha tiver `\`, escreva `\\`. |
| `Connection refused`, `timeout` ou `UnknownHost` ao iniciar | Sem internet ou projeto do Supabase pausado (o plano gratuito pausa após dias sem uso: entre no painel e clique em *Restore*). |
| Dados que eu cadastrei sumiram | Confira se o backend não foi iniciado com o perfil `h2` (banco em memória). |
| `Defina SMARTSTOCK_SECRET no arquivo backend/.env` | Gere um segredo com o comando do `.env.example` e coloque na linha `SMARTSTOCK_SECRET=` do `.env`. |
| "Código incorreto" no 2FA com o código certo | Acerte o relógio do celular (data e hora automáticas). O código muda a cada 30 s e cada um só vale uma vez. |
| Perdi o celular | Use um código de recuperação no login; sem eles, peça ao administrador para **Redefinir 2FA** em *Usuários*. |
| "Muitas tentativas erradas" | 5 erros seguidos bloqueiam por 15 minutos. Espere ou reinicie o backend. |
| A tela abre, mas tudo pede login de novo | O login vale 8 h e "Sair" encerra todas as sessões da conta. Entre de novo. |
| `Could not resolve placeholder 'SUPABASE_SERVICE_KEY'` | Falta a linha `SUPABASE_SERVICE_KEY=...` no `backend/.env` (veja o `.env.example`). |
| `O Supabase recusou a chave SUPABASE_SERVICE_KEY` | A chave no `.env` está errada ou é a *publishable*. Use a **secret** (`sb_secret_...`) de *Project Settings › API Keys*. |
| Fotos dos itens não aparecem | Confira se o backend iniciou sem erros (ele prepara o Storage ao iniciar) e se há internet. |
| Sessão perdida ao abrir outra aba | O login é por aba (de propósito). Entre de novo na aba nova. |
| `Validate failed: Migrations have failed validation` | Um arquivo de migration que já rodou foi alterado. Desfaça a alteração e crie um arquivo novo (`V3__...`). |
| `Schema-validation: missing column` | O banco está atrás do código: confira se as migrations rodaram (tabela `flyway_schema_history` no schema `smartstock`). |
| `/swagger-ui.html` dá 404 | O backend é de uma versão antiga: gere de novo com `.\mvnw.cmd clean package -DskipTests`. |

## 13. Próximos passos e créditos

**Próximos passos sugeridos**
- Registrar um domínio para o servidor da AWS e ativar o HTTPS com o certbot (veja a [seção 14](#14-publicando-na-aws-ec2)).
- Relatórios de consumo por período (a estrutura de movimentações já suporta consultas por data).
- Integração futura com ERP/compras e renomear "Pastilhas" para algo mais geral (ex.: *Materiais*).

**Créditos**
Projeto Aplicado III – SENAI/SC, Fraiburgo – desenvolvido por **Renan Jussiani** para a DDA Metalúrgica.
As fotos de exemplo pertencem aos respectivos autores, listados em
[CREDITOS.md](backend/src/main/resources/seed-images/CREDITOS.md).

## 14. Publicando na AWS (EC2)

O passo a passo completo, para quem nunca usou a AWS, está em
**[docs/implantacao-aws-ec2.md](docs/implantacao-aws-ec2.md)**. Em resumo:

```
 Internet ──▶ Elastic IP ──▶ Security Group (só 22 do seu IP, 80 e 443) ──▶ EC2 Ubuntu
                                                                            ├─ Nginx: telas do frontend + repassa /api
                                                                            └─ backend (systemd) em 127.0.0.1:8080
                                                                                   └──▶ Supabase (banco + arquivos)
```

1. Criar a instância EC2 (Ubuntu 24.04, `t3.micro`), com um par de chaves SSH (`.pem`) e um Security Group que libera a
   porta **22 só para o seu IP** e as portas **80/443** para todos (a **8080 fica fechada**).
2. Associar um **Elastic IP** (endereço fixo) à instância.
3. Entrar por SSH, copiar a pasta `deploy` e rodar `bash deploy/instalar-ec2.sh` (Java 21, Nginx, serviço, firewall).
4. Preencher `/opt/smartstock/.env` no servidor (mesmas chaves do `backend/.env`, mais `CORS_ORIGENS`).
5. No seu computador: `.\deploy\publicar.ps1 -Servidor SEU_ELASTIC_IP -Chave caminho\da\chave.pem`.
6. Com um domínio apontando para o Elastic IP: `sudo certbot --nginx -d seu-dominio` para ativar o HTTPS.

## 15. Documentação do projeto (relatório)

A pasta [docs/](docs/README.md) reúne o material do relatório: descrição comercial, requisitos funcionais e não
funcionais, casos de uso, diagramas (arquitetura, banco de dados, sequência do login, estados do projeto) e o guia de
implantação na AWS.
