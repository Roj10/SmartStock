# Casos de uso do SmartStock

## Atores

| Ator | Quem é | O que pode fazer |
|------|--------|------------------|
| **Administrador** | Dono/gestor da metalúrgica | Tudo: contas, permissões, segurança e todas as abas; sua senha autoriza editar/excluir projetos |
| **Operador** | Funcionário de um setor (produção, almoxarifado, compras, financeiro) | Só as abas liberadas para a conta dele |
| **Serviço de e-mail** | Gmail (ator externo) | Entrega à caixa de entrada da pessoa o código de 6 dígitos do 2FA |
| **Supabase** | Serviço de nuvem (ator externo) | Guarda o banco de dados e os arquivos |

## Diagrama de casos de uso

```mermaid
flowchart LR
    adm(["👤 Administrador"])
    op(["👤 Operador"])
    app(["📧 Serviço de e-mail"])

    subgraph SmartStock
        uc1([UC01 Entrar com senha e 2FA])
        uc2([UC02 Gerenciar contas e permissões])
        uc3([UC03 Consultar eventos de segurança])
        uc4([UC04 Gerenciar itens de estoque])
        uc5([UC05 Registrar entrada/saída])
        uc6([UC06 Gerenciar fornecedores e preços])
        uc7([UC07 Criar projeto com arquivos])
        uc8([UC08 Avançar etapas da produção])
        uc9([UC09 Editar/excluir projeto])
        uc10([UC10 Enviar pedido e baixar estoque])
        uc11([UC11 Acompanhar entregas no calendário])
        uc12([UC12 Gerenciar planos e vendas])
        uc13([UC13 Trocar mensagens])
        uc14([Informar senha do administrador])
    end

    adm --- uc1 & uc2 & uc3 & uc9
    op --- uc1 & uc4 & uc5 & uc6 & uc7 & uc8 & uc10 & uc11 & uc12 & uc13
    adm -.->|"também faz tudo<br/>o que o Operador faz"| op
    uc1 --- app
    uc9 -. "«include»" .-> uc14
    uc10 -. "«include»" .-> uc5
```

> O Operador só vê os casos de uso das abas liberadas para a conta dele (ex.: o almoxarifado vê UC04 e UC05).

## Descrição dos principais casos de uso

### UC01 – Entrar com senha e 2FA

- **Atores:** Administrador ou Operador; Serviço de e-mail.
- **Pré-condição:** a conta existe.
- **Fluxo principal:**
  1. O usuário informa usuário e senha.
  2. O sistema confere a senha, envia um código de 6 dígitos para o e-mail da conta e pede esse código.
  3. O usuário abre o e-mail e digita o código.
  4. O sistema confere o código e libera o acesso às abas permitidas por 8 horas.
- **Fluxos alternativos:**
  - *2a. Primeira entrada (conta sem e-mail):* o sistema pede o e-mail, envia o código para ele e, quando o código é
    digitado, passa a usar esse e-mail. Se a senha for de exemplo, o usuário é obrigado a trocá-la.
  - *3a. Código não chegou:* o usuário pede outro código (no máximo um por minuto); o anterior deixa de valer.
  - *3b. Perdeu o acesso ao e-mail:* o administrador troca o e-mail da conta ou redefine o 2FA.
  - *1a/3c. Erro:* senha ou código errado mostram aviso; após 5 erros seguidos, a conta fica bloqueada por 15 minutos.
- **Pós-condição:** evento registrado em *Segurança*.

### UC02 – Gerenciar contas e permissões

- **Ator:** Administrador (ou Operador com a aba Usuários).
- **Fluxo principal:** o administrador cria a conta com nome, usuário, senha provisória, perfil, abas permitidas e,
  opcionalmente, o e-mail; a pessoa troca a senha no primeiro acesso (e confirma o e-mail, se ele não foi informado).
- **Alternativos:** editar (trocar a senha ou o e-mail encerra as sessões da pessoa); redefinir 2FA; excluir.
- **Regras:** não é possível excluir a própria conta nem o único administrador; um Operador só concede abas que ele tem.

### UC05 – Registrar entrada/saída de estoque

- **Ator:** Operador com a aba Movimentações.
- **Fluxo principal:** escolhe o item, o tipo (entrada ou saída), a quantidade e, na entrada, o fornecedor; o sistema
  atualiza a quantidade e registra quem fez.
- **Alternativos:** *entrada em lote* (várias linhas de uma vez: se uma falhar, nenhuma é gravada); *saída maior que o
  estoque*: recusada com a quantidade disponível.
- **Pós-condição:** se o item ficar no mínimo ou abaixo, aparece nos alertas do painel.

### UC07 – Criar projeto com arquivos

- **Ator:** Operador com a aba Progresso.
- **Fluxo principal:** informa nome e descrição, monta o checklist e a lista de materiais (ou parte de um modelo),
  anexa PDFs/desenhos e salva. O projeto entra em "Aguardando início".
- **Alternativos:** *repetir projeto* (copia dados e arquivos); *arquivo recusado* (tipo ou tamanho inválido): o projeto
  não é criado e o motivo é mostrado.

### UC09 – Editar/excluir projeto

- **Ator:** Operador com a aba Progresso + **senha de um Administrador**.
- **Fluxo principal:** o usuário altera o projeto (ou pede a exclusão), o sistema pede a senha do administrador, confere
  e grava. Na exclusão, os arquivos do projeto também são apagados.
- **Alternativo:** senha incorreta → nada é alterado; 5 erros seguidos → bloqueio de 15 minutos.

### UC10 – Enviar pedido e baixar estoque

- **Ator:** Operador com a aba Progresso.
- **Pré-condição:** projeto em "Pronto para entrega".
- **Fluxo principal:** informa cliente e meta de entrega; o sistema dá baixa em todos os materiais do projeto e move o
  pedido para o Calendário.
- **Alternativo:** falta algum material → o envio é recusado, informando o item, e **nada** é baixado.

### UC11 – Acompanhar entregas no calendário

- **Ator:** Operador com a aba Progresso.
- **Fluxo principal:** vê os pedidos enviados por data, ajusta data do pedido e meta, e marca como entregue.
- **Pós-condição:** pedidos entregues ficam no histórico.

### UC12 – Gerenciar planos e vendas

- **Ator:** Operador com a aba Financeiro.
- **Fluxo principal:** cria um plano para um cliente (opcionalmente ligado a um projeto, com custo calculado), ajusta o
  valor e, quando o cliente aceita, fecha o plano como venda.
