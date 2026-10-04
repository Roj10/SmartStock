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
| Internet | só na primeira vez | Baixar as dependências (Maven e npm) |

**Não é preciso instalar Maven nem banco de dados:** o projeto traz o *Maven Wrapper* (`mvnw`), que baixa o Maven
sozinho na primeira execução, e usa um banco **H2 em memória** que já vem embutido.

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
dependências. **Rode sempre a partir da pasta `backend`** (é onde ficam as pastas de imagens).

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
- **Os dados voltam ao estado de exemplo a cada reinício do backend**, porque o banco fica na memória. Isso é útil
  para demonstrações (basta reiniciar para "zerar" os testes).
- Depois de alterar o código do backend, rode de novo o `clean package` e o `java -jar`. Se o `clean` falhar dizendo
  que o arquivo `.jar` está em uso, pare o backend antes (Ctrl + C).

## 5. Entrando no sistema

Contas de demonstração, criadas automaticamente:

| Usuário | Senha | Perfil | Abas liberadas |
|---------|-------|--------|----------------|
| `admin` | `admin123` | Administrador | Todas, incluindo Usuários |
| `operador` | `operador123` | Funcionário | Pastilhas, Movimentações, Progresso e Calendário (estoque e produção) |
| `financeiro` | `financeiro123` | Funcionário | Financeiro e Fornecedores |

A aba **Mensagens** é liberada para todas as contas. As senhas ficam criptografadas no banco (BCrypt) e nunca são
mostradas: o administrador só pode redefini-las.

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
 Navegador (React, porta 5173)  ──HTTP/JSON + token JWT──▶  API Spring Boot (porta 8080)  ──▶  Banco H2 (memória)
        telas e formulários                                  regras de negócio e permissões      + pastas de imagens
```

- **Frontend (React):** só mostra as telas e chama a API. Guarda o token de login e esconde do menu as abas que a
  conta não pode acessar.
- **Backend (Spring Boot):** valida o login (JWT), confere a permissão de cada rota e aplica as regras do negócio.
  Mesmo que alguém tente chamar a API "na mão", sem a permissão recebe erro 403.
- **Banco de dados (H2 em memória):** guarda usuários, itens, movimentações, projetos, vendas e mensagens. É recriado,
  com dados de exemplo, a cada início do backend (veja [Próximos passos](#13-próximos-passos-e-créditos)).
- **Imagens:** fotos de itens ficam em `backend/uploads/`; imagens das conversas ficam em
  `backend/uploads-privado/`, sem link público.

**Caminho de um pedido (ordem de produção):**

```
Aguardando início → Em produção → Em estoque → Pronto para entrega ──(enviar pedido)──▶ Pedido enviado ──▶ Entregue
   (fila)           (checklist)   (já feito,      (informa cliente)    baixa o estoque       (Calendário)    (histórico)
                                   não saiu)                           dos materiais
```

- Só se avança ou volta **uma etapa por vez**, e as etapas do checklist só podem ser marcadas em **Em produção**.
- Ao **enviar o pedido**, o sistema dá baixa no estoque com os materiais do projeto. Se faltar qualquer item, o envio é
  bloqueado e **nada** é baixado.

## 8. Funcionalidades em detalhe

**Acesso e usuários**
- Login com JWT. O administrador cria contas e define a quais abas cada uma tem acesso. Contas sem permissão recebem
  403 da API e nem veem a aba no menu.

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
│           └── seed-images/             Fotos dos dados de exemplo (+ CREDITOS.md)
└── frontend/                        Interface (React 19 + Vite)
    ├── package.json                 Dependências e comandos do frontend
    └── src/
        ├── pages/                   Uma tela por aba (Pastilhas, Progresso, Calendário...)
        ├── components/              Peças reutilizáveis (menu, pop-up, seletor de emojis...)
        ├── context/                 Estado de login e permissões
        ├── api/client.js            Configuração das chamadas à API
        └── index.css, chat.css      Estilos
```

## 10. API (resumo)

Todas as rotas começam com `/api` e, exceto o login, exigem o token JWT no cabeçalho `Authorization: Bearer ...`.

| Módulo | Rotas principais | Permissão |
|--------|------------------|-----------|
| Login | `POST /auth/login` | pública |
| Painel | `GET /dashboard` | conta logada |
| Pastilhas | `GET/POST /pastilhas`, `PUT/DELETE /pastilhas/{id}`, `POST/DELETE /pastilhas/{id}/imagem`, `GET /pastilhas/alertas` | leitura: Estoque, Movimentações, Progresso ou Fornecedores; escrita: Estoque |
| Movimentações | `GET /movimentacoes`, `POST /movimentacoes/entrada`, `/entrada/lote`, `/saida` | Movimentações |
| Fornecedores | `GET/POST /fornecedores`, `PUT/DELETE /fornecedores/{id}`, `GET /fornecedores/produtos`, `PUT /fornecedores/{id}/produtos` | Fornecedores |
| Projetos | `GET /projetos/progresso`, `/projetos/calendario`, `POST /projetos`, `PATCH /projetos/{id}/status`, `POST /projetos/{id}/enviar-pedido`, `POST /projetos/{id}/entregar` | Progresso |
| Modelos | `GET/POST /modelos-projeto`, `DELETE /modelos-projeto/{id}` | Progresso |
| Financeiro | `GET /financeiro/resumo`, `/pecas`, `/projetos`; `GET/POST /vendas`, `POST /vendas/{id}/fechar` | Financeiro |
| Mensagens | `GET /mensagens/contatos`, `/nao-lidas`, `GET/POST /mensagens/conversa/{usuarioId}`, `GET /mensagens/{id}/imagem` | conta logada |
| Usuários | `GET/POST /usuarios`, `PUT/DELETE /usuarios/{id}` | Administrador ou "Usuários" |

Console do banco (opcional, para inspecionar): `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:smartstock`,
usuário `sa`, senha em branco.

## 11. Configurações

**Backend** – arquivo `backend/src/main/resources/application.properties`:

| Chave | O que controla |
|-------|----------------|
| `server.port` | Porta do servidor (padrão 8080) |
| `jwt.secret` / `jwt.expiration-ms` | Chave do token de login e validade (padrão 24 h). **Troque a chave em uso real.** |
| `app.uploads.dir` | Pasta das fotos de itens (padrão `uploads`) |
| `app.mensagens.dir` | Pasta privada das imagens das conversas |
| `spring.servlet.multipart.max-file-size` | Tamanho máximo de upload (5 MB) |

**Frontend** – o endereço da API (`http://localhost:8080`) aparece em `src/api/client.js`, `src/utils/format.js` e
`src/pages/Pastilhas.jsx`. Se o backend rodar em outro endereço, altere nesses três lugares.

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
| Dados que eu cadastrei sumiram | É esperado: o banco é em memória e volta ao exemplo quando o backend reinicia. |
| Fotos dos itens não aparecem | Rode o backend **de dentro da pasta `backend`**, para que as pastas de imagens fiquem no lugar certo. |
| Sessão perdida ao abrir outra aba | O login é por aba (de propósito). Entre de novo na aba nova. |

## 13. Próximos passos e créditos

**Próximos passos sugeridos**
- Trocar o H2 em memória por um banco persistente (PostgreSQL ou MySQL) antes de qualquer uso real.
- Relatórios de consumo por período (a estrutura de movimentações já suporta consultas por data).
- Integração futura com ERP/compras e renomear "Pastilhas" para algo mais geral (ex.: *Materiais*).

**Créditos**
Projeto Aplicado III – SENAI/SC, Fraiburgo – desenvolvido por **Renan Jussiani** para a DDA Metalúrgica.
As fotos de exemplo pertencem aos respectivos autores, listados em
[CREDITOS.md](backend/src/main/resources/seed-images/CREDITOS.md).
