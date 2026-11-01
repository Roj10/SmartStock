# Descrição comercial do SmartStock

## O problema

Pequenas e médias metalúrgicas, como a DDA Metalúrgica, costumam controlar estoque, produção e entregas com planilhas,
cadernos e mensagens de WhatsApp. Isso causa:

- **falta de material no meio da produção** — ninguém percebe que a barra ou o eletrodo acabou até precisar dele;
- **compras no escuro** — não se sabe qual fornecedor vende mais barato nem quanto dinheiro está parado no estoque;
- **pedidos sem dono** — não fica claro em que etapa cada portão, estrutura ou peça está, nem quando deve ser entregue;
- **informação espalhada** — o desenho do cliente está no e-mail de um, a foto no celular de outro;
- **pouca segurança** — senhas compartilhadas e planilhas que qualquer pessoa pode alterar ou apagar.

## A solução

O **SmartStock** é um sistema web, usado pelo navegador (computador, tablet ou celular), que reúne num só lugar:

| Módulo | O que resolve |
|--------|---------------|
| **Estoque** | Cadastro dos materiais com foto, estoque mínimo e alerta automático quando chega ao limite |
| **Movimentações** | Toda entrada e saída fica registrada com data, quantidade, fornecedor e quem fez |
| **Fornecedores** | Quem fabrica e quem vende cada item, e por qual preço |
| **Progresso (produção)** | Cada pedido vira uma ordem de produção com etapas, checklist, materiais e os arquivos do projeto (PDF, CAD) |
| **Calendário** | Pedidos enviados com data prometida de entrega; ao enviar, o estoque é baixado sozinho |
| **Financeiro** | Valor parado em estoque, custo de cada projeto pelo menor preço, planos (orçamentos) e vendas, margem |
| **Mensagens** | Conversa interna entre os setores, com envio de imagens |
| **Usuários e segurança** | Uma conta por setor, cada uma só com as abas que precisa; login com app autenticador (2FA) |

## Para quem é

- **Público principal:** metalúrgicas, serralherias e pequenas indústrias de transformação (5 a 50 funcionários) que
  produzem sob encomenda.
- **Usuários dentro da empresa:** dono/gestor (visão geral e financeiro), produção (Progresso), almoxarifado (estoque e
  movimentações), compras (fornecedores) e administrativo (vendas).

## Benefícios

- **Menos parada de produção:** o painel mostra os itens críticos antes de faltarem.
- **Compra mais barata:** comparação de preço por fornecedor e custo real de cada projeto.
- **Prazo sob controle:** quadro de produção e calendário de entregas com a meta de cada pedido.
- **Tudo do projeto num lugar só:** desenhos, PDFs e fotos anexados ao pedido certo.
- **Rastreabilidade:** cada movimentação e cada evento de segurança registrados com data e responsável.
- **Segurança de nível bancário:** senha + código do celular (2FA), senhas fortes obrigatórias, bloqueio após
  tentativas erradas, dados criptografados no tráfego e arquivos privados.

## Diferenciais

- Feito a partir do dia a dia real de uma metalúrgica da região (DDA Metalúrgica, Fraiburgo/SC).
- Baixa de estoque **automática** ao enviar o pedido, com a regra "tudo ou nada": se faltar um item, nada é baixado.
- Editar ou excluir um projeto exige a **senha do administrador**, evitando alterações acidentais.
- **Modelos de projeto** reaproveitáveis (ex.: "portão deslizante padrão") para abrir pedidos parecidos em segundos.
- Roda na nuvem (AWS + Supabase), sem servidor na empresa e sem instalar nada nos computadores.

## Modelo de oferta (proposta)

| Plano | Para quem | Inclui |
|-------|-----------|--------|
| **Essencial** | até 5 contas | Estoque, movimentações, fornecedores, progresso e calendário |
| **Profissional** | até 15 contas | Tudo do Essencial + financeiro, modelos de projeto e mensagens |
| **Implantação** | pagamento único | Cadastro inicial dos materiais e fornecedores, treinamento da equipe |

Cobrança mensal por empresa (SaaS). O custo de infraestrutura de uma empresa pequena cabe numa instância `t3.micro` da
AWS e no plano gratuito/básico do Supabase.

## Tecnologias

Frontend em **React 19** (Vite), backend em **Java 21 + Spring Boot 3.5** (Spring Security, JWT, JPA), banco
**PostgreSQL** (Supabase) com migrations **Flyway**, arquivos no **Supabase Storage**, documentação da API em
**OpenAPI/Swagger**, publicação em **AWS EC2** com **Nginx**.
