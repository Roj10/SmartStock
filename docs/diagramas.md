# Diagramas do SmartStock

## 1. Arquitetura (visão geral)

Quem fala com quem, e por onde passam os dados. Todo o tráfego que sai do servidor é cifrado (HTTPS/TLS).

```mermaid
flowchart LR
    subgraph Usuario["Computador ou celular do usuário"]
        nav["Navegador<br/>(telas em React)"]
        cel["App autenticador<br/>(código 2FA)"]
    end

    subgraph AWS["AWS EC2 (Ubuntu) – Elastic IP"]
        nginx["Nginx<br/>portas 80/443<br/>telas + repasse /api"]
        api["Backend Spring Boot<br/>127.0.0.1:8080<br/>regras, login, permissões"]
    end

    subgraph Supabase["Supabase (nuvem)"]
        db[("PostgreSQL<br/>schema smartstock")]
        st[["Storage<br/>pastilhas (público)<br/>mensagens, projetos (privados)"]]
    end

    nav -- "HTTPS" --> nginx
    nginx -- "/api (rede interna)" --> api
    api -- "JDBC + TLS verify-full" --> db
    api -- "HTTPS + chave secreta" --> st
    nav -. "fotos dos itens (HTTPS)" .-> st
    cel -. "o usuário digita o código" .-> nav
```

## 2. Camadas do backend

Cada pedido percorre as camadas de cima para baixo. Cada camada tem uma única responsabilidade.

```mermaid
flowchart TB
    req["Pedido HTTP (ex.: POST /api/movimentacoes/saida)"]
    f["security/JwtAuthFilter<br/>confere o token (crachá)"]
    c["controller/<br/>recebe o pedido, confere a permissão da aba (@PreAuthorize)"]
    s["service/<br/>aplica as regras do negócio (ex.: há estoque suficiente?)"]
    r["repository/<br/>lê e grava no banco (Spring Data JPA)"]
    m["model/<br/>as tabelas como classes Java"]
    db[("PostgreSQL")]
    e["exception/GlobalExceptionHandler<br/>transforma erros em respostas claras (400, 404, 409, 422...)"]

    req --> f --> c --> s --> r --> db
    r -.- m
    s -. "erro de regra" .-> e
```

## 3. Banco de dados (modelo entidade-relacionamento)

Tabelas criadas pelas migrations `V1__esquema_inicial.sql` e `V2__seguranca_2fa.sql` (Flyway).

```mermaid
erDiagram
    USUARIOS {
        bigint id PK
        varchar nome
        varchar username UK
        varchar senha "hash BCrypt"
        varchar role "ADMIN | OPERADOR"
        boolean dois_fatores_ativo
        varchar totp_segredo "cifrado AES-GCM"
        int token_versao
        boolean trocar_senha
    }
    USUARIO_PERMISSOES {
        bigint usuario_id FK
        varchar modulo "PASTILHAS, MOVIMENTACOES..."
    }
    USUARIO_CODIGOS_RECUPERACAO {
        bigint usuario_id FK
        varchar codigo_hash
    }
    EVENTOS_SEGURANCA {
        bigint id PK
        timestamp data_hora
        varchar tipo
        varchar usuario
        varchar ip
    }
    FORNECEDORES {
        bigint id PK
        varchar nome
        varchar tipo "FABRICANTE | FORNECEDOR | AMBOS"
        varchar telefone
        varchar email
    }
    PASTILHAS {
        bigint id PK
        varchar codigo UK
        varchar descricao
        int estoque_minimo
        int quantidade_atual
        bigint fabricante_id FK
        varchar imagem_url
    }
    FORNECEDOR_PRODUTOS {
        bigint id PK
        bigint fornecedor_id FK
        bigint pastilha_id FK
        numeric preco
    }
    MOVIMENTACOES {
        bigint id PK
        varchar tipo "ENTRADA | SAIDA"
        int quantidade
        timestamp data_hora
        bigint pastilha_id FK
        bigint fornecedor_id FK
        bigint usuario_id FK
    }
    PROJETOS {
        bigint id PK
        varchar nome
        varchar status
        varchar cliente
        date data_pedido
        date meta_entrega
        timestamp data_entrega
    }
    CHECKLIST_ITENS {
        bigint id PK
        bigint projeto_id FK
        varchar texto
        boolean concluido
    }
    PROJETO_MATERIAIS {
        bigint id PK
        bigint projeto_id FK
        bigint pastilha_id FK
        int quantidade
    }
    PROJETO_ANEXOS {
        bigint id PK
        bigint projeto_id FK
        varchar nome_original
        varchar arquivo "caminho no Storage"
        bigint tamanho
    }
    MODELOS_PROJETO {
        bigint id PK
        varchar nome
    }
    MODELO_PROJETO_ETAPAS {
        bigint modelo_id FK
        int ordem
        varchar texto
    }
    MODELO_PROJETO_MATERIAIS {
        bigint modelo_id FK
        int ordem
        bigint pastilha_id
        int quantidade
    }
    VENDAS {
        bigint id PK
        varchar cliente
        numeric valor
        varchar status "PLANO | VENDA"
        bigint projeto_id FK
    }
    MENSAGENS {
        bigint id PK
        bigint remetente_id FK
        bigint destinatario_id FK
        varchar texto
        varchar imagem_arquivo
        boolean lida
    }

    USUARIOS ||--o{ USUARIO_PERMISSOES : "tem"
    USUARIOS ||--o{ USUARIO_CODIGOS_RECUPERACAO : "tem"
    USUARIOS ||--o{ MOVIMENTACOES : "registra"
    USUARIOS ||--o{ MENSAGENS : "envia"
    USUARIOS ||--o{ MENSAGENS : "recebe"
    FORNECEDORES ||--o{ PASTILHAS : "fabrica"
    FORNECEDORES ||--o{ FORNECEDOR_PRODUTOS : "vende"
    PASTILHAS ||--o{ FORNECEDOR_PRODUTOS : "tem preço em"
    PASTILHAS ||--o{ MOVIMENTACOES : "movimenta"
    FORNECEDORES |o--o{ MOVIMENTACOES : "fornece"
    PROJETOS ||--o{ CHECKLIST_ITENS : "tem"
    PROJETOS ||--o{ PROJETO_MATERIAIS : "usa"
    PASTILHAS ||--o{ PROJETO_MATERIAIS : "é usada em"
    PROJETOS ||--o{ PROJETO_ANEXOS : "tem arquivos"
    MODELOS_PROJETO ||--o{ MODELO_PROJETO_ETAPAS : "tem"
    MODELOS_PROJETO ||--o{ MODELO_PROJETO_MATERIAIS : "tem"
    PROJETOS |o--o{ VENDAS : "origina"
```

## 4. Sequência do login com 2FA

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant F as Frontend (React)
    participant B as Backend
    participant D as Banco
    participant A as App autenticador

    U->>F: usuário e senha
    F->>B: POST /api/auth/login
    B->>D: busca a conta e confere o hash da senha
    alt senha errada
        B-->>F: 401 (5 erros seguidos = bloqueio de 15 min)
    else primeira vez (2FA ainda não configurado)
        B-->>F: etapa CONFIGURAR_2FA + desafio (vale 5 min)
        F->>B: POST /api/auth/2fa/configurar
        B->>D: guarda o segredo novo (cifrado)
        B-->>F: QR code
        U->>A: lê o QR code
        A-->>U: código de 6 dígitos
        U->>F: digita o código
        F->>B: POST /api/auth/2fa/ativar
        B-->>F: 8 códigos de recuperação + próxima etapa
    else 2FA já configurado
        B-->>F: etapa CODIGO_2FA + desafio
        A-->>U: código de 6 dígitos
        U->>F: digita o código
        F->>B: POST /api/auth/2fa/verificar
    end
    opt senha de exemplo ou definida pelo administrador
        B-->>F: etapa TROCAR_SENHA
        U->>F: senha nova
        F->>B: POST /api/auth/trocar-senha
    end
    B->>D: registra o evento LOGIN_OK
    B-->>F: etapa CONCLUIDO + token de acesso (8 h)
    F->>F: guarda o token na aba (sessionStorage)
    F->>B: próximas chamadas com "Authorization: Bearer token"
```

## 5. Estados de um projeto (ordem de produção)

```mermaid
stateDiagram-v2
    [*] --> AGUARDANDO: criar projeto
    AGUARDANDO --> EM_PRODUCAO: iniciar
    EM_PRODUCAO --> AGUARDANDO: voltar
    EM_PRODUCAO --> EM_ESTOQUE: concluir produção
    EM_ESTOQUE --> EM_PRODUCAO: voltar
    EM_ESTOQUE --> PRONTO_ENTREGA: liberar para entrega
    PRONTO_ENTREGA --> EM_ESTOQUE: voltar
    PRONTO_ENTREGA --> PEDIDO_ENVIADO: enviar pedido (baixa o estoque)
    PEDIDO_ENVIADO --> ENTREGUE: marcar como entregue
    ENTREGUE --> [*]

    note right of EM_PRODUCAO: só aqui o checklist pode ser marcado
    note right of PRONTO_ENTREGA: se faltar material, o envio é recusado e nada é baixado
```

## 6. Implantação (deploy)

```mermaid
flowchart TB
    dev["Computador do desenvolvedor<br/>deploy/publicar.ps1"]
    gh["GitHub<br/>Roj10/SmartStock"]
    subgraph SG["Security Group"]
        direction TB
        r1["22 (SSH): só o IP do desenvolvedor"]
        r2["80 e 443: qualquer endereço"]
        r3["8080: fechada"]
    end
    subgraph EC2["Instância EC2 – Ubuntu 24.04 – Elastic IP"]
        ufw["Firewall ufw"]
        ng["Nginx"]
        svc["smartstock.service (systemd)<br/>java -jar backend.jar"]
        fe["/opt/smartstock/frontend"]
        env["/opt/smartstock/.env (chmod 600)"]
    end
    sb[("Supabase")]

    dev -- "git push" --> gh
    dev -- "scp + ssh (chave .pem)" --> SG --> ufw --> ng
    ng --> fe
    ng -- "127.0.0.1:8080" --> svc
    svc -. "lê" .-> env
    svc -- "TLS" --> sb
```
