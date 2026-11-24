# Em palavras simples: gera a documentação OpenAPI da pasta docs/openapi a partir do arquivo que o próprio sistema
# usa (backend/src/main/resources/static/openapi.yaml), para as duas nunca ficarem diferentes.
#
# Uso (na pasta do projeto, com Python 3 e PyYAML: pip install pyyaml):
#   python docs/openapi/gerar_documentacao.py
#
# Gera:
#   openapi.yaml  – a especificação OpenAPI 3 (para abrir no editor.swagger.io, Postman, Insomnia...)
#   openapi.json  – a mesma especificação em JSON
#   swagger.html  – página que mostra a documentação interativa (abre com dois cliques, precisa de internet)
#   README.md     – referência de todas as rotas em texto, com exemplos (o GitHub mostra formatado)
import json
import os
import yaml

AQUI = os.path.dirname(os.path.abspath(__file__))
FONTE = os.path.join(AQUI, '..', '..', 'backend', 'src', 'main', 'resources', 'static', 'openapi.yaml')
SERVIDORES = [
    {'url': 'http://localhost:8080', 'description': 'No computador (backend rodando com java -jar)'},
    {'url': 'http://34.199.252.233', 'description': 'Servidor na AWS EC2 (no ar enquanto o laboratório estiver ligado)'},
]
CABECALHO = ('# Documentação OpenAPI 3 do SmartStock (cópia gerada por docs/openapi/gerar_documentacao.py).\n'
             '# Fonte: backend/src/main/resources/static/openapi.yaml — altere lá e gere de novo.\n')

spec = yaml.safe_load(open(FONTE, encoding='utf-8'))
spec['servers'] = SERVIDORES

with open(os.path.join(AQUI, 'openapi.yaml'), 'w', encoding='utf-8', newline='\n') as f:
    f.write(CABECALHO)
    yaml.safe_dump(spec, f, allow_unicode=True, sort_keys=False, width=120)
with open(os.path.join(AQUI, 'openapi.json'), 'w', encoding='utf-8', newline='\n') as f:
    json.dump(spec, f, ensure_ascii=False, indent=2)
    f.write('\n')

# ---------- swagger.html: página única com a especificação embutida ----------
spec_js = json.dumps(spec, ensure_ascii=False).replace('</', '<\\/')
html = f'''<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>SmartStock API</title>
  <!-- Em palavras simples: abre a documentação interativa da API sem precisar rodar o sistema.
       A especificação está embutida aqui; o visual (Swagger UI) vem da internet. -->
  <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui.css">
  <style>body {{ margin: 0; background: #fff; }} .topbar {{ display: none; }}</style>
</head>
<body>
  <div id="swagger-ui"></div>
  <script src="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-bundle.js"></script>
  <script>
    const spec = {spec_js};
    window.ui = SwaggerUIBundle({{ spec, dom_id: '#swagger-ui', docExpansion: 'list', defaultModelsExpandDepth: -1,
      tagsSorter: 'alpha', tryItOutEnabled: false }});
  </script>
</body>
</html>
'''
open(os.path.join(AQUI, 'swagger.html'), 'w', encoding='utf-8', newline='\n').write(html)

# ---------- README.md: referência em texto ----------
def bloco(obj):
    return '```json\n' + json.dumps(obj, ensure_ascii=False, indent=2) + '\n```'

def ancora(texto):
    import re, unicodedata
    t = texto.lower().strip()
    t = re.sub(r'[^\w\s-]', '', t)
    return re.sub(r'\s', '-', t)

erros = spec['components']['responses']
linhas = []
L = linhas.append
L('# Documentação OpenAPI do SmartStock')
L('')
L('Referência completa da API REST do SmartStock, gerada a partir da especificação **OpenAPI 3** que o próprio '
  'sistema publica. Os exemplos de resposta foram obtidos chamando o sistema de verdade com os dados de exemplo.')
L('')
L('| Arquivo | Para que serve |')
L('|---------|----------------|')
L('| [`openapi.yaml`](openapi.yaml) | Especificação OpenAPI 3.0 em YAML. Abra em <https://editor.swagger.io> (*File › Import file*) para ver e testar |')
L('| [`openapi.json`](openapi.json) | A mesma especificação em JSON, para importar no **Postman** ou **Insomnia** (*Import › File*) |')
L('| [`swagger.html`](swagger.html) | Documentação interativa (Swagger UI) que abre com dois cliques no navegador, sem rodar o sistema |')
L('| Este arquivo | Todas as rotas em texto, com permissões e exemplos |')
L('')
L('Com o sistema rodando, a mesma documentação fica em **`/swagger-ui.html`** '
  '(ex.: <http://localhost:8080/swagger-ui.html>), onde dá para testar as rotas de verdade.')
L('')
L('> Estes arquivos são gerados por [`gerar_documentacao.py`](gerar_documentacao.py) a partir de '
  '`backend/src/main/resources/static/openapi.yaml`. Para mudar a documentação, altere esse arquivo e rode '
  '`python docs/openapi/gerar_documentacao.py`.')
L('')
L('## Visão geral')
L('')
L('| | |')
L('|-|-|')
L(f"| **Versão da API** | {spec['info']['version']} |")
L('| **Padrão** | OpenAPI 3.0.3 · REST · JSON (UTF-8) |')
L('| **Endereço base** | `http://localhost:8080` (computador) · `http://34.199.252.233` (AWS) — todas as rotas começam com `/api` |')
L(f"| **Rotas** | {sum(len(v) for v in spec['paths'].values())} operações em {len(spec['paths'])} endereços |")
L('| **Autenticação** | Token JWT no cabeçalho `Authorization: Bearer <token>` (vale 8 horas), obtido no login em etapas |')
L('| **Permissões** | Cada rota exige a aba correspondente na conta (Estoque, Movimentações, Progresso, Fornecedores, Financeiro, Usuários) ou perfil Administrador |')
L('')
L('## Como autenticar')
L('')
L('O login tem etapas. Cada resposta diz a próxima `etapa` e traz um `desafio` (token curto, 10 minutos) para usar '
  'nela. O token de acesso só vem na etapa `CONCLUIDO`.')
L('')
L('```mermaid')
L('flowchart LR')
L('    A["POST /api/auth/login<br/>usuário + senha"] -->|conta sem e-mail| B["POST /api/auth/email/cadastrar<br/>desafio + e-mail"]')
L('    A -->|conta com e-mail<br/>código enviado| C["POST /api/auth/email/verificar<br/>desafio + código"]')
L('    B -->|código enviado| C')
L('    C -->|senha de exemplo ou<br/>definida pelo admin| D["POST /api/auth/trocar-senha<br/>desafio + nova senha"]')
L('    C -->|CONCLUIDO| E(["token de acesso"])')
L('    D -->|CONCLUIDO| E')
L('```')
L('')
L('Exemplo com `curl`:')
L('')
L('```bash')
L('# 1) usuário e senha -> o código de 6 dígitos vai para o e-mail da conta')
L("curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \\")
L("     -d '{\"username\":\"admin\",\"senha\":\"SUA_SENHA\"}'")
L('# {"etapa":"CODIGO_EMAIL","desafio":"eyJ...","emailMascarado":"re*********@gmail.com",...}')
L('')
L('# 2) código que chegou no e-mail -> token de acesso')
L("curl -s -X POST http://localhost:8080/api/auth/email/verificar -H 'Content-Type: application/json' \\")
L("     -d '{\"desafio\":\"eyJ...\",\"codigo\":\"123456\"}'")
L('# {"etapa":"CONCLUIDO","token":"eyJ...","role":"ADMIN","permissoes":[...]}')
L('')
L('# 3) usar o token')
L("curl -s http://localhost:8080/api/pastilhas -H 'Authorization: Bearer eyJ...'")
L('```')
L('')
L('Sem e-mail configurado no servidor (`MAIL_USUARIO`/`MAIL_SENHA`), a resposta vem com `"modoTeste": true` e o '
  'código aparece no terminal do backend.')
L('')
L('## Respostas e erros')
L('')
L('| Código | Quando acontece |')
L('|--------|-----------------|')
L('| **200 OK** | Consulta ou alteração feita; o corpo traz o registro |')
L('| **201 Created** | Registro criado (cadastros, movimentações, projetos, mensagens, contas) |')
L('| **204 No Content** | Registro excluído; resposta sem corpo |')
for c, r in erros.items():
    L(f"| **{c[4:]}** | {r['description']} |")
L('')
L('Todo erro vem no mesmo formato:')
L('')
L(bloco(erros['Erro409']['content']['application/json']['example']))
L('')
L('## Índice das rotas')
L('')
tags = [t['name'] for t in spec['tags']]
por_tag = {t: [] for t in tags}
for caminho, ops in spec['paths'].items():
    for metodo, op in ops.items():
        por_tag[op['tags'][0]].append((metodo, caminho, op))
desc_tag = {t['name']: t['description'] for t in spec['tags']}
for t in tags:
    L(f'### {t}')
    L('')
    L(desc_tag[t])
    L('')
    L('| Método | Rota | O que faz |')
    L('|--------|------|-----------|')
    for metodo, caminho, op in por_tag[t]:
        titulo = f"{metodo.upper()} {caminho}"
        L(f"| `{metodo.upper()}` | [`{caminho}`](#{ancora(titulo)}) | {op['summary']} |")
    L('')
L('## Detalhe de cada rota')
L('')
for t in tags:
    L(f'### {t}')
    L('')
    for metodo, caminho, op in por_tag[t]:
        L(f"#### {metodo.upper()} {caminho}")
        L('')
        L(f"**{op['summary']}**")
        L('')
        L(op['description'])
        L('')
        params = op.get('parameters', [])
        if params:
            L('| Parâmetro | Onde | Obrigatório | Descrição |')
            L('|-----------|------|-------------|-----------|')
            for p in params:
                onde = {'path': 'caminho', 'query': 'consulta (?)'}.get(p['in'], p['in'])
                L(f"| `{p['name']}` | {onde} | {'sim' if p.get('required') else 'não'} | {p.get('description', '')} |")
            L('')
        corpo = op.get('requestBody', {}).get('content', {})
        if 'application/json' in corpo and 'example' in corpo['application/json']:
            L('Corpo (JSON):')
            L('')
            L(bloco(corpo['application/json']['example']))
            L('')
        if 'multipart/form-data' in corpo:
            props = corpo['multipart/form-data']['schema']['properties']
            req = corpo['multipart/form-data']['schema'].get('required', [])
            L('Corpo (`multipart/form-data`, envio de arquivos):')
            L('')
            L('| Campo | Obrigatório | Descrição |')
            L('|-------|-------------|-----------|')
            for nome, pr in props.items():
                tipo = 'arquivo(s)' if pr.get('format') == 'binary' or pr.get('items', {}).get('format') == 'binary' else 'texto'
                L(f"| `{nome}` ({tipo}) | {'sim' if nome in req else 'não'} | {pr.get('description', '')} |")
            L('')
        for codigo, r in op['responses'].items():
            if '$ref' in r:
                continue
            ex = r.get('content', {}).get('application/json', {}).get('example')
            L(f"Resposta **{codigo}** — {r['description']}")
            L('')
            if ex is not None:
                L('<details><summary>Exemplo</summary>')
                L('')
                L(bloco(ex))
                L('')
                L('</details>')
                L('')
        erros_op = [r['$ref'].split('Erro')[-1] for r in op['responses'].values() if '$ref' in r]
        if erros_op:
            L('Erros possíveis: ' + ', '.join(f'`{e}`' for e in erros_op))
            L('')
open(os.path.join(AQUI, 'README.md'), 'w', encoding='utf-8', newline='\n').write('\n'.join(linhas))
print('Gerado em', AQUI, '-', sum(len(v) for v in spec['paths'].values()), 'rotas')
