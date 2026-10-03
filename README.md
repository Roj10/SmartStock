# SmartStock

Sistema de controle de estoque de pastilhas industriais, desenvolvido para o desafio da DDA Metalúrgica (SENAI/SC - Fraiburgo). Ver [SmartStock_Relatorio_PJA_III.docx](SmartStock_Relatorio_PJA_III.docx) para o relatório completo do projeto.

## Stack

- **Backend:** Java 21 + Spring Boot 3.5 (Web, Data JPA, Security, Validation), banco H2 em memória, autenticação JWT.
- **Frontend:** React 19 + Vite, React Router, Axios.

## Pré-requisitos

- JDK 21 (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Node.js 18+ (`node -version`)

## Rodando o backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

A API sobe em `http://localhost:8080`. O banco H2 é em memória e é recriado (com dados de exemplo) a cada reinício.

Usuários de demonstração (criados automaticamente):

| Usuário  | Senha        | Perfil        | Acesso                                   |
|----------|--------------|---------------|-------------------------------------------|
| admin    | admin123     | ADMIN         | Todas as abas, incluindo Usuários         |
| operador | operador123  | Funcionário   | Pastilhas (estoque), Movimentações e Progresso/Calendário (produção) |

Console do H2 (opcional, para inspecionar o banco): `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:smartstock`, usuário `sa`, senha em branco.

Imagens enviadas pelas telas de Pastilhas são salvas em `backend/uploads/` (fora do controle de versão — ver `.gitignore`).

## Rodando o frontend

```bash
cd frontend
npm install
npm run dev
```

Acesse `http://localhost:5173`. O frontend espera a API em `http://localhost:8080/api` (configurado em `src/api/client.js`).

## Funcionalidades implementadas

- Login com autenticação JWT.
- Controle de acesso por usuário: um ADMIN pode cadastrar outras contas e definir exatamente a quais abas cada
  uma tem acesso (Pastilhas/Estoque, Movimentações/Produção, Fornecedores, Usuários). Contas sem a permissão de
  um módulo recebem 403 da API e nem veem o item no menu. Apenas ADMIN (ou quem tiver a permissão "Usuários")
  acessa a tela de gerenciamento de contas; senhas ficam sempre criptografadas (nunca são exibidas, só redefinidas).
- Cadastro de pastilhas (código, descrição, fabricante, estoque mínimo, imagem do produto).
- Upload de imagem por pastilha, com galeria de busca: digitar "imagem" ou "imagens" no campo de busca da aba
  Pastilhas mostra todas as fotos cadastradas, e clicar em uma rola a tela até o produto correspondente na tabela.
- Cadastro de fabricantes/fornecedores.
- Registro de entrada e saída de estoque, com validação de estoque insuficiente. A entrada aceita várias
  pastilhas de uma vez (por exemplo, uma compra geral), cada linha com sua quantidade e seu fornecedor; se alguma
  linha falhar, nenhuma é gravada.
- Consulta de estoque em tempo real e alertas de estoque abaixo do mínimo.
- Histórico de movimentações.
- Painel (dashboard) com indicadores gerais, adaptado às permissões de quem está logado.
- **Progresso de produção:** ordem de produção em sequência, em 4 colunas — (1) Aguardando início, (2) Em produção
  (checklist editável do que deve ser feito), (3) Em estoque (projeto pronto que ainda não saiu) e (4) Pronto para
  entrega (informa-se o cliente e envia-se o pedido). Cada projeto tem um número de ordem (OP-001...), a fila mostra
  os mais antigos primeiro e só é possível avançar ou voltar uma etapa por vez. Os materiais necessários são
  vinculados ao cadastro de Pastilhas, com aviso quando a quantidade excede o estoque atual.
  Ao enviar o pedido, o sistema registra automaticamente a saída de estoque com exatamente as quantidades
  solicitadas no projeto (identificada pelo número da OP e pelo cliente); se faltar estoque de algum item, o envio
  é bloqueado e nada é baixado.
- **Calendário de entregas:** ao enviar o pedido, o projeto passa para o Calendário com o nome do cliente e a data
  do pedido preenchida automaticamente; a meta de entrega e a data do pedido continuam editáveis. Há ainda o botão
  "Marcar como entregue", o histórico de entregas e um calendário mensal em que o dia inteiro fica colorido
  (laranja = meta de entrega, verde = entregue) com os projetos listados dentro do dia.

## Próximos passos sugeridos

- Trocar o H2 em memória por um banco persistente (PostgreSQL/MySQL) antes de qualquer uso real.
- Relatórios de consumo por período (a estrutura de movimentações já suporta consultas por data).
- Integração futura com ERP/compras, conforme previsto no relatório do projeto.
