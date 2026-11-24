# Documentação do SmartStock

Material de apoio ao relatório do Projeto Aplicado III (SENAI/SC – Fraiburgo), desafio da **DDA Metalúrgica**.

| Documento | Conteúdo |
|-----------|----------|
| [Descrição comercial](descricao-comercial.md) | Problema, solução, público, benefícios, diferenciais e modelo de oferta |
| [Requisitos](requisitos.md) | Requisitos funcionais (RF) e não funcionais (RNF), com onde cada um está no código |
| [Casos de uso](casos-de-uso.md) | Atores, diagrama de casos de uso e descrição dos principais casos |
| [Diagramas](diagramas.md) | Arquitetura, banco de dados (ER), sequência do login com 2FA, estados do projeto, implantação |
| [Implantação na AWS EC2](implantacao-aws-ec2.md) | Passo a passo: instância, Elastic IP, Security Group, SSH, Nginx, systemd, HTTPS |
| [Usabilidade](usabilidade.md) | As telas avaliadas pelas 10 heurísticas de Nielsen, acessibilidade e uso no celular |
| [Telas](telas.md) | Os 21 prints das telas para o relatório (`docs/telas/`), com legendas e galeria |
| [Como ler o código](como-ler-o-codigo.md) | Guia para leigos: como o sistema se organiza, o caminho de um clique e glossário |
| [Documentação OpenAPI](openapi/README.md) | Todas as 67 rotas da API com permissões, parâmetros e exemplos reais; especificação [`openapi.yaml`](openapi/openapi.yaml) / [`openapi.json`](openapi/openapi.json) e página interativa [`swagger.html`](openapi/swagger.html) |

A documentação da **API** também fica no próprio sistema, em `/swagger-ui.html` (ex.: <http://34.199.252.233/swagger-ui.html>),
onde dá para testar as rotas. O arquivo-fonte é `backend/src/main/resources/static/openapi.yaml`; a pasta
[`openapi/`](openapi/README.md) é gerada a partir dele por `python docs/openapi/gerar_documentacao.py`.

Os diagramas usam **Mermaid**: o GitHub desenha as figuras sozinho ao abrir os arquivos `.md`. Para colar no relatório
em Word, abra o código de cada diagrama em <https://mermaid.live> e exporte como PNG ou SVG.
