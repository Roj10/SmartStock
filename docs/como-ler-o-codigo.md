# Como ler o código do SmartStock (guia para leigos)

Este guia explica, sem precisar saber programar, como o sistema está organizado e o que acontece "por trás" de um
clique. Cada arquivo do código começa com um comentário **"Em palavras simples"** dizendo o que ele faz.

## A ideia geral: um restaurante

| No restaurante | No SmartStock | Onde fica |
|----------------|---------------|-----------|
| O salão e o cardápio, onde o cliente escolhe | **Frontend**: as telas no navegador | `frontend/src/pages`, `components` |
| O garçom, que leva o pedido à cozinha | **API**: os endereços `/api/...` chamados pelas telas | `frontend/src/api/client.js` |
| O porteiro, que confere quem pode entrar | **Segurança**: login, 2FA e permissões | `backend/.../security`, `config/SecurityConfig.java` |
| O balcão da cozinha, que recebe os pedidos | **Controllers**: recebem cada pedido da API | `backend/.../controller` |
| O cozinheiro, que segue as receitas | **Services**: as regras do negócio | `backend/.../service` |
| A despensa | **Banco de dados** (PostgreSQL no Supabase) | tabelas em `db/migration` |
| O estoquista, que pega e guarda na despensa | **Repositories**: leem e gravam no banco | `backend/.../repository` |
| A etiqueta de cada pote | **Models**: cada tabela descrita em Java | `backend/.../model` |
| O formulário do pedido | **DTOs**: quais campos cada pedido aceita | `backend/.../dto` |
| O armário de arquivos | **Storage** (fotos, PDFs, desenhos) no Supabase | `backend/.../storage` |

## Um clique, do começo ao fim: "registrar saída de 2 chapas"

1. **Tela** (`pages/Movimentacoes.jsx`): a pessoa escolhe o item, digita 2 e clica em *Registrar*.
2. **Chamada à API** (`api/client.js`): o navegador envia `POST /api/movimentacoes/saida` com
   `{"pastilhaId": 9, "quantidade": 2}` e o "crachá" da pessoa (o token, no cabeçalho `Authorization`).
3. **Portaria** (`security/JwtAuthFilter.java`): confere se o crachá é verdadeiro, se não venceu e se a pessoa não
   clicou em "Sair" depois de recebê-lo. Se algo estiver errado: resposta **401**.
4. **Controller** (`controller/MovimentacaoController.java`): confere se a conta tem a aba *Movimentações*
   (`@PreAuthorize`). Se não tiver: resposta **403**.
5. **Service** (`service/MovimentacaoService.java`): confere se há pelo menos 2 chapas. Se não houver: resposta
   **422** "Estoque insuficiente". Se houver: subtrai 2 e cria o registro da movimentação com data e responsável.
6. **Repository + banco**: grava as duas mudanças juntas ("tudo ou nada").
7. **Resposta**: **201 Created** com a movimentação registrada; a tela atualiza a lista.

## Palavras que aparecem no código

| Palavra | Significado |
|---------|-------------|
| **API** | O conjunto de endereços (`/api/...`) pelos quais as telas pedem e enviam dados ao backend |
| **JSON** | O formato de texto dos dados trocados: `{"nome": "Aço Sul", "tipo": "FORNECEDOR"}` |
| **Token / JWT** | O "crachá digital" que o login entrega. Vale 8 horas e vai junto em cada pedido |
| **2FA / TOTP** | Verificação em duas etapas: além da senha, o código de 6 dígitos do app do celular, que muda a cada 30 s |
| **Hash (BCrypt)** | A senha "embaralhada" sem volta. O sistema não sabe a sua senha, só confere se a digitada gera o mesmo hash |
| **Criptografia (AES, TLS)** | Transformar dados em algo ilegível para quem não tem a chave. TLS é a criptografia da conexão (o cadeado do HTTPS) |
| **@Transactional** | "Tudo ou nada": várias gravações que só valem se todas derem certo |
| **@PreAuthorize** | Trava de permissão em uma rota: "só entra quem tem a aba X" |
| **Migration (Flyway)** | Arquivo SQL numerado que cria ou altera tabelas. Roda uma vez só, em ordem (V1, V2, ...) |
| **Status HTTP** | Número da resposta: 200 ok, 201 criado, 204 excluído, 400 dados inválidos, 401 sem login, 403 sem permissão, 404 não encontrado, 409 em uso, 422 regra recusou, 429 muitas tentativas |
| **Swagger / OpenAPI** | Página que documenta todas as rotas da API, com exemplos (`/swagger-ui.html`) |
| **Nginx** | O "recepcionista" do servidor na nuvem: entrega as telas e repassa os pedidos da API ao backend |
| **Supabase** | Serviço de nuvem que guarda o banco de dados e os arquivos |
| **Pastilha** | Nome, no código, dos itens de estoque (o sistema nasceu para pastilhas industriais) |

## Por onde começar a ler

1. `frontend/src/App.jsx` — quais telas existem.
2. Uma tela simples, como `frontend/src/pages/Fornecedores.jsx`.
3. O controller correspondente: `backend/.../controller/FornecedorController.java`.
4. O service: `backend/.../service/FornecedorService.java`.
5. A tabela: `backend/.../model/Fornecedor.java` e `db/migration/V1__esquema_inicial.sql`.
6. Para a segurança: `docs/diagramas.md` (sequência do login) e `backend/.../service/AuthService.java`.
