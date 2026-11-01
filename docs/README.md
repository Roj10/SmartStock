# Documentação do SmartStock

Material de apoio ao relatório do Projeto Aplicado III (SENAI/SC – Fraiburgo), desafio da **DDA Metalúrgica**.

| Documento | Conteúdo |
|-----------|----------|
| [Descrição comercial](descricao-comercial.md) | Problema, solução, público, benefícios, diferenciais e modelo de oferta |
| [Requisitos](requisitos.md) | Requisitos funcionais (RF) e não funcionais (RNF), com onde cada um está no código |
| [Casos de uso](casos-de-uso.md) | Atores, diagrama de casos de uso e descrição dos principais casos |
| [Diagramas](diagramas.md) | Arquitetura, banco de dados (ER), sequência do login com 2FA, estados do projeto, implantação |
| [Implantação na AWS EC2](implantacao-aws-ec2.md) | Passo a passo: instância, Elastic IP, Security Group, SSH, Nginx, systemd, HTTPS |
| [Como ler o código](como-ler-o-codigo.md) | Guia para leigos: como o sistema se organiza, o caminho de um clique e glossário |

A documentação da **API** (todas as rotas, com exemplos reais) fica no próprio sistema, em `/swagger-ui.html`
(arquivo-fonte: `backend/src/main/resources/static/openapi.yaml`).

Os diagramas usam **Mermaid**: o GitHub desenha as figuras sozinho ao abrir os arquivos `.md`. Para colar no relatório
em Word, abra o código de cada diagrama em <https://mermaid.live> e exporte como PNG ou SVG.
