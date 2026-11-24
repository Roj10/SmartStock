# Usabilidade do SmartStock

Avaliação das telas do SmartStock pelas **10 heurísticas de usabilidade de Jakob Nielsen**, as mais usadas para
avaliar interfaces. Para cada heurística: o que ela pede e onde o sistema atende, com o arquivo da tela.

| # | Heurística | Como o SmartStock atende | Onde |
|---|------------|--------------------------|------|
| 1 | **Visibilidade do estado do sistema** — o usuário sempre sabe o que está acontecendo | Botões mudam para *Entrando...*, *Salvando...*, *Enviando...*, *Verificando...* enquanto a ação acontece; listas mostram *Carregando...*; barra de nível colorida em cada item de estoque; contador de etapas do checklist em cada projeto; aviso de mensagens não lidas no menu e no título da aba do navegador; contagem regressiva de "Enviar outro código" no login | `pages/Pastilhas.jsx`, `pages/Progresso.jsx`, `components/Layout.jsx`, `pages/Login.jsx` |
| 2 | **Correspondência com o mundo real** — linguagem e conceitos do usuário | Tudo em português e com os termos da metalúrgica: *ordem de produção*, *pronto para entrega*, *enviar pedido*, *estoque mínimo*, *fornecedor*, *plano/venda*; o quadro de Progresso segue as etapas reais (Aguardando → Em produção → Em estoque → Pronto para entrega); valores em R$ e datas no formato brasileiro | `pages/Progresso.jsx`, `utils/format.js` |
| 3 | **Controle e liberdade do usuário** — saídas claras para desfazer | Etapas do projeto podem **voltar** uma posição; todo formulário abre em janela com **Cancelar**; no login há *Voltar*, *Cancelar* e *Digitei o e-mail errado*; o código por e-mail pode ser reenviado | `pages/Progresso.jsx`, `components/Modal.jsx`, `pages/Login.jsx` |
| 4 | **Consistência e padrões** — mesmas ações, mesma aparência | Todas as telas seguem o mesmo layout (menu lateral, título, tabela ou quadro, botão principal no topo à direita); mesmos botões (*Editar*, *Excluir*, *Salvar*, *Cancelar*) e as mesmas cores para o mesmo significado (vermelho = crítico/perigo, verde = ok); mensagens de erro sempre no mesmo lugar e formato | `index.css`, `components/Layout.jsx`, `components/Modal.jsx` |
| 5 | **Prevenção de erros** — evitar que o erro aconteça | Confirmação antes de excluir qualquer registro, fechar uma venda ou enviar um pedido; aviso se o checklist ainda tem etapas pendentes ao concluir; senha do administrador para editar/excluir projetos; o servidor recusa saída maior que o estoque e envio de pedido com material faltando (*tudo ou nada*); campos obrigatórios marcados; o código do login só aceita números e libera o botão com 6 dígitos | `pages/*.jsx` (`window.confirm`), `service/ProjetoService.java`, `service/MovimentacaoService.java` |
| 6 | **Reconhecer em vez de lembrar** — informação à vista | Listas de seleção para itens, fornecedores e modelos (não é preciso decorar códigos); materiais do projeto mostram o estoque atual ao passar o mouse e ficam vermelhos se faltar; exemplos nos campos (*Ex: BAR-FE-01*, *Ex: Cerca de metal - Cliente ABC*); fotos dos itens; o login mostra para qual e-mail o código foi (*re\*\*\*\*@gmail.com*) | `pages/Pastilhas.jsx`, `pages/Progresso.jsx`, `pages/Login.jsx` |
| 7 | **Flexibilidade e eficiência de uso** — atalhos para quem já sabe | **Modelos de projeto** e **Repetir projeto** criam pedidos parecidos em segundos; **entrada em lote** registra uma compra inteira de uma vez; filtros por item nas movimentações e busca nas mensagens; o código do e-mail é reconhecido pelo celular (*one-time-code*) | `pages/Progresso.jsx`, `pages/Movimentacoes.jsx`, `pages/Mensagens.jsx` |
| 8 | **Estética e design minimalista** — só o necessário | Cada conta vê **só as abas que tem permissão**; painel inicial com três números e duas listas; informações secundárias (fotos ampliadas, arquivos do projeto) aparecem só quando pedidas | `components/Layout.jsx`, `pages/Dashboard.jsx` |
| 9 | **Ajudar a reconhecer, diagnosticar e corrigir erros** — mensagens claras | Mensagens em linguagem simples e com a solução: *"Estoque insuficiente para a pastilha BAR-FE-01. Disponível: 3"*, *"Não foi possível concluir: este registro está ligado a outros cadastros"*, *"Aguarde 42 segundos para pedir outro código"*, *"A senha deve ter letras e números."*; erros técnicos nunca aparecem para o usuário | `exception/GlobalExceptionHandler.java`, serviços do backend |
| 10 | **Ajuda e documentação** | Textos de orientação nas telas (ex.: "Como começar?" ao criar projeto, explicação do 2FA no primeiro login, dica de conferir o spam); dicas ao passar o mouse; README com roteiro de uso; documentação da API em `/swagger-ui.html` e guia para leigos | `pages/Progresso.jsx`, `pages/Login.jsx`, `README.md`, `docs/como-ler-o-codigo.md` |

## Acessibilidade e uso no celular

- **Responsivo:** o layout se reorganiza em telas pequenas (regras `@media` em `index.css`), então o sistema funciona
  no celular e no tablet do chão de fábrica sem instalar nada.
- Campos com rótulo (`<label>`) e tipo adequado (`email`, `password`, `numeric`), o que ativa o teclado certo no celular
  e o preenchimento automático do navegador.
- Contraste alto entre texto e fundo; estados importantes não dependem só da cor (o estoque crítico também mostra os
  números e aparece na lista de alertas).

## Pontos a evoluir

- Atalhos de teclado para as ações mais frequentes.
- Desfazer uma exclusão por alguns segundos (hoje há confirmação antes, mas não "desfazer" depois).
- Teste formal com usuários da DDA Metalúrgica (tempo para concluir tarefas e taxa de erro) para medir a usabilidade.
