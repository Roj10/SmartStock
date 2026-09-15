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

| Usuário  | Senha        | Perfil   |
|----------|--------------|----------|
| admin    | admin123     | ADMIN    |
| operador | operador123  | OPERADOR |

Console do H2 (opcional, para inspecionar o banco): `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:smartstock`, usuário `sa`, senha em branco.

## Rodando o frontend

```bash
cd frontend
npm install
npm run dev
```

Acesse `http://localhost:5173`. O frontend espera a API em `http://localhost:8080/api` (configurado em `src/api/client.js`).

## Funcionalidades implementadas

- Login com autenticação JWT.
- Cadastro de pastilhas (código, descrição, fabricante, estoque mínimo).
- Cadastro de fabricantes/fornecedores.
- Registro de entrada e saída de estoque, com validação de estoque insuficiente.
- Consulta de estoque em tempo real e alertas de estoque abaixo do mínimo.
- Histórico de movimentações.
- Painel (dashboard) com indicadores gerais.

## Próximos passos sugeridos

- Trocar o H2 em memória por um banco persistente (PostgreSQL/MySQL) antes de qualquer uso real.
- Relatórios de consumo por período (a estrutura de movimentações já suporta consultas por data).
- Integração futura com ERP/compras, conforme previsto no relatório do projeto.
