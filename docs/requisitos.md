# Requisitos do SmartStock

Legenda de prioridade: **Alta** (essencial para o uso), **Média** (importante), **Baixa** (desejável).
A coluna *Onde* aponta o arquivo principal no código (`backend/src/main/java/com/smartstock/backend/...` e
`frontend/src/...`).

## Requisitos funcionais (RF)

### Acesso e usuários

| Id | Requisito | Prioridade | Onde |
|----|-----------|------------|------|
| RF01 | O sistema deve exigir login com usuário e senha para qualquer acesso. | Alta | `controller/AuthController`, `pages/Login.jsx` |
| RF02 | Toda conta deve usar verificação em duas etapas (2FA) com aplicativo autenticador (código de 6 dígitos que muda a cada 30 s). Na primeira entrada, o sistema mostra um QR code para configurar o app. | Alta | `service/AuthService`, `security/Totp`, `security/QrCode` |
| RF03 | Ao ativar o 2FA, o sistema deve gerar 8 códigos de recuperação de uso único, para quem perder o celular. | Alta | `service/AuthService` |
| RF04 | O sistema deve obrigar a troca de senha no primeiro acesso e sempre que o administrador definir uma senha. | Alta | `service/AuthService`, `service/PoliticaSenha` |
| RF05 | O administrador deve poder criar, editar e excluir contas, definir o perfil (Administrador/Operador) e escolher quais abas cada conta acessa. | Alta | `controller/UsuarioController`, `pages/Usuarios.jsx` |
| RF06 | O administrador deve poder redefinir o 2FA de uma conta (celular perdido). | Média | `UsuarioController` (`/redefinir-2fa`) |
| RF07 | O usuário deve poder sair do sistema, encerrando todas as sessões abertas da conta. | Média | `AuthController` (`/sair`) |
| RF08 | O sistema deve registrar os eventos de segurança (logins, falhas, bloqueios, 2FA, troca de senha) e mostrá-los ao administrador. | Média | `service/AuditoriaService`, `controller/SegurancaController` |

### Estoque

| Id | Requisito | Prioridade | Onde |
|----|-----------|------------|------|
| RF09 | Cadastrar, consultar, editar e excluir itens de estoque com código único, descrição, fabricante, estoque mínimo, quantidade inicial e foto. | Alta | `controller/PastilhaController`, `pages/Pastilhas.jsx` |
| RF10 | Alertar os itens cuja quantidade atual está igual ou abaixo do estoque mínimo (painel e lista de alertas). | Alta | `PastilhaService`, `pages/Dashboard.jsx` |
| RF11 | Registrar entradas de estoque (uma ou várias de uma vez), com fornecedor e observação, somando à quantidade do item. | Alta | `controller/MovimentacaoController` |
| RF12 | Registrar saídas de estoque, recusando a saída quando não houver quantidade suficiente. | Alta | `service/MovimentacaoService` |
| RF13 | Manter o histórico de movimentações com data, tipo, quantidade, item, fornecedor e usuário responsável, com filtro por item. | Alta | `pages/Movimentacoes.jsx` |
| RF14 | Impedir a exclusão de itens e fornecedores que já estão em uso (movimentações, projetos), avisando o motivo. | Média | `exception/GlobalExceptionHandler` (409) |

### Fornecedores

| Id | Requisito | Prioridade | Onde |
|----|-----------|------------|------|
| RF15 | Cadastrar, consultar, editar e excluir fornecedores/fabricantes com contato, telefone e e-mail. | Alta | `controller/FornecedorController` |
| RF16 | Registrar quais itens cada fornecedor vende e por qual preço. | Média | `FornecedorController` (`/produtos`) |

### Produção (Progresso) e entregas (Calendário)

| Id | Requisito | Prioridade | Onde |
|----|-----------|------------|------|
| RF17 | Criar projetos (ordens de produção) com nome, descrição, checklist de etapas e lista de materiais. | Alta | `controller/ProjetoController`, `pages/Progresso.jsx` |
| RF18 | Anexar arquivos ao projeto (PDF, desenhos CAD, imagens, documentos), abrir/baixar e remover. | Alta | `ProjetoController` (`/anexos`), `components/AnexosProjeto.jsx` |
| RF19 | Conduzir o projeto pelas etapas Aguardando → Em produção → Em estoque → Pronto para entrega, uma etapa por vez, nos dois sentidos. | Alta | `service/ProjetoService` |
| RF20 | Marcar as etapas do checklist apenas enquanto o projeto está "Em produção". | Média | `ProjetoService` |
| RF21 | Exigir a senha de um administrador para editar ou excluir um projeto e para anexar/remover arquivos. | Alta | `service/SenhaAdminService` |
| RF22 | Enviar o pedido ao cliente informando cliente e meta de entrega, dando baixa automática nos materiais; se faltar qualquer item, nada é baixado. | Alta | `ProjetoService.enviarPedido` |
| RF23 | Mostrar os pedidos enviados em um calendário, permitir alterar data e meta, e marcar como entregue. | Alta | `pages/Calendario.jsx` |
| RF24 | Salvar e reaproveitar modelos de projeto (checklist e materiais), e repetir um projeto existente com seus arquivos. | Média | `controller/ModeloProjetoController` |

### Financeiro

| Id | Requisito | Prioridade | Onde |
|----|-----------|------------|------|
| RF25 | Calcular o valor parado em estoque e o custo de cada projeto pelo menor preço entre os fornecedores. | Média | `service/FinanceiroService` |
| RF26 | Cadastrar planos (orçamentos) para clientes, editá-los, fechá-los como venda e excluí-los. | Média | `controller/VendaController` |
| RF27 | Mostrar resumo financeiro: total em estoque, vendas, planos em aberto e margem dos projetos. | Média | `pages/Financeiro.jsx` |

### Comunicação e painel

| Id | Requisito | Prioridade | Onde |
|----|-----------|------------|------|
| RF28 | Trocar mensagens de texto e imagens entre as contas, com contagem de não lidas. | Baixa | `controller/MensagemController`, `pages/Mensagens.jsx` |
| RF29 | Exibir um painel inicial com totais, itens críticos e últimas movimentações, conforme as permissões da conta. | Média | `controller/DashboardController` |

## Requisitos não funcionais (RNF)

| Id | Categoria | Requisito | Como foi atendido |
|----|-----------|-----------|-------------------|
| RNF01 | Segurança | Senhas guardadas apenas como *hash* BCrypt; política de senha forte (8+ caracteres, letras e números, sem conter o usuário). | `SecurityConfig.passwordEncoder`, `PoliticaSenha` |
| RNF02 | Segurança | Autenticação sem estado com token JWT assinado (HS256), validade de 8 h, revogável ("Sair", troca de senha, redefinição de 2FA). | `JwtService`, campo `token_versao` |
| RNF03 | Segurança | Segredos do 2FA cifrados no banco (AES-256-GCM); códigos de recuperação com hash; cada código do app aceito uma única vez. | `Segredos`, `Totp` |
| RNF04 | Segurança | Bloqueio de 15 minutos após 5 tentativas erradas (senha, 2FA, senha de administrador). | `TentativasService` |
| RNF05 | Segurança | Controle de acesso por perfil e por aba em todas as rotas da API (não só nas telas). | `@PreAuthorize` nos controllers |
| RNF06 | Segurança | Tráfego cifrado: TLS com verificação de certificado até o banco (Supabase); HTTPS no servidor público (Nginx + certbot). | `BancoSeguro`, `deploy/nginx-smartstock.conf` |
| RNF07 | Segurança | Banco protegido contra acesso direto pela API pública do Supabase (RLS ligado e permissões revogadas); arquivos privados só passam pelo backend. | `BancoSeguro`, `SupabaseStorage` |
| RNF08 | Segurança | Nenhuma senha ou chave no código: tudo em `.env` (fora do Git) ou variáveis de ambiente. | `.env.example`, `.gitignore` |
| RNF09 | Segurança | Arquivos enviados conferidos pelo conteúdo (não só pela extensão) e com tamanho limitado (5 MB imagens, 20 MB anexos). | `storage/TipoArquivo`, `AnexoProjetoService` |
| RNF10 | Segurança | Cabeçalhos de proteção do navegador (CSP, X-Frame-Options, Referrer-Policy) e mensagens de erro sem detalhes internos. | `SecurityConfig`, `GlobalExceptionHandler` |
| RNF11 | Rastreabilidade | Movimentações e eventos de segurança registrados com data, usuário e IP. | `Movimentacao`, `EventoSeguranca` |
| RNF12 | Integridade | Operações compostas são "tudo ou nada" (transações): envio de pedido, entrada em lote, projeto com arquivos. | `@Transactional` nos serviços |
| RNF13 | Manutenibilidade | Banco versionado por migrations (Flyway); o Hibernate só valida o esquema. Código em camadas (controller → service → repository). | `db/migration`, `ddl-auto=validate` |
| RNF14 | Documentação | API documentada em OpenAPI 3 com exemplos reais, disponível no próprio sistema (`/swagger-ui.html`). | `static/openapi.yaml`, springdoc |
| RNF15 | Padrões REST | Respostas com códigos HTTP corretos: 200, 201 (criado), 204 (excluído), 400, 401, 403, 404, 409, 422, 429; erros em JSON padronizado. | controllers, `GlobalExceptionHandler` |
| RNF16 | Usabilidade | Interface em português, responsiva (computador e celular), menus mostrando só as abas permitidas. | `index.css`, `components/Layout.jsx` |
| RNF17 | Disponibilidade | Publicação em AWS EC2 com reinício automático do backend (systemd) e Elastic IP fixo. | `deploy/smartstock.service` |
| RNF18 | Desempenho | Respostas comuns em menos de 1 s para até 15 usuários simultâneos numa instância `t3.micro`; pool de 5 conexões com o banco. | `application.properties` |
| RNF19 | Portabilidade | Uso pelo navegador (Chrome, Edge, Firefox) sem instalar nada; backend em Java 21 roda em Windows e Linux. | — |
| RNF20 | Privacidade (LGPD) | Coleta só os dados necessários (nome, usuário, contato de fornecedores); senhas e segredos nunca aparecem em respostas ou logs. | DTOs de resposta |
