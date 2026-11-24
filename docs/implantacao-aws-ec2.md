# Implantação do SmartStock na AWS EC2

Guia passo a passo para colocar o sistema na internet, escrito para quem nunca usou a AWS.

**Como vai ficar:**

```
 Navegador ──HTTPS──▶ Elastic IP (endereço fixo) ──▶ Security Group (firewall da AWS)
                                                       │  22  só do seu IP (SSH, administração)
                                                       │  80/443 todos (site)
                                                       ▼
                                             EC2 Ubuntu (t3.micro)
                                             ├─ Nginx: entrega as telas e repassa /api
                                             └─ backend (systemd) em 127.0.0.1:8080 ──TLS──▶ Supabase
```

O banco de dados e os arquivos **continuam no Supabase**; na AWS fica só o programa. Os arquivos usados estão na pasta
[`deploy/`](../deploy):

| Arquivo | Para que serve |
|---------|----------------|
| `instalar-ec2.sh` | Prepara o servidor uma única vez (Java 21, Nginx, certbot, usuário de serviço, firewall) |
| `smartstock.service` | Faz o backend ligar sozinho com o servidor e reiniciar se cair (systemd) |
| `nginx-smartstock.conf` | Configuração do Nginx: telas, repasse da API, cabeçalhos de segurança |
| `publicar.ps1` | No seu Windows: gera e envia uma versão nova para o servidor |

> **Implantação atual:** AWS Academy (Learner Lab), região `us-east-1`, instância `t3.micro` com Ubuntu, Elastic IP
> **34.199.252.233** → <http://34.199.252.233>. O site fica no ar enquanto a sessão do laboratório estiver ligada.

### Se você usa o AWS Academy (Learner Lab)

O passo a passo abaixo vale igual, com estas diferenças:

| Item | No AWS Academy |
|------|----------------|
| Abrir o console | Página do Learner Lab › **Start Lab** › esperar a bolinha ficar verde › clicar em **AWS** |
| Região | **N. Virginia (us-east-1)** — o lab não deixa usar São Paulo |
| Par de chaves | Escolha **vockey** ao criar a instância. A chave é o `labsuser.pem`: Learner Lab › **AWS Details** › *SSH key* › **Download PEM** (não use o PPK no PowerShell) |
| Sessão | Dura 4 horas (o cronômetro aparece no topo). **Start Lab** de novo renova o tempo. Quando zera, a AWS **desliga** a instância; no próximo Start Lab ela liga sozinha e o backend sobe em ~1 minuto, no mesmo Elastic IP |
| Custo | Sai dos créditos do lab ("Used $x of $50"). Uma `t3.micro` gasta pouco, mas **End Lab** quando não precisar |

---

## 1. Criar a instância EC2

1. Entre no [console da AWS](https://console.aws.amazon.com/) e escolha a região **São Paulo (sa-east-1)** no canto
   superior direito (menor atraso para usuários no Brasil).
2. Vá em **EC2 › Instâncias › Executar instâncias** e preencha:
   - **Nome:** `smartstock`
   - **Imagem (AMI):** *Ubuntu Server* LTS mais recente (24.04 ou 26.04, 64 bits x86) — o script funciona nos dois
   - **Tipo:** `t3.micro` (ou `t2.micro`, elegíveis ao nível gratuito)
   - **Par de chaves:** *Criar novo par de chaves* → nome `smartstock`, tipo **ED25519**, formato **.pem**. O arquivo
     `smartstock.pem` é baixado **uma única vez**: guarde em `C:\Users\SEU_USUARIO\.ssh\` e não compartilhe (quem tem
     esse arquivo entra no servidor).
   - **Armazenamento:** 20 GiB gp3.
3. Em **Configurações de rede › Editar**, crie um **Security Group** chamado `smartstock-sg` (próximo passo).

## 2. Security Group (firewall da AWS)

O Security Group decide quais portas do servidor podem ser acessadas e de onde. Regras de **entrada**:

| Tipo | Porta | Origem | Por quê |
|------|-------|--------|---------|
| SSH | 22 | **Meu IP** | Só você administra o servidor. Nunca use `0.0.0.0/0` aqui |
| HTTP | 80 | `0.0.0.0/0` e `::/0` | Acesso ao site (e validação do certificado HTTPS) |
| HTTPS | 443 | `0.0.0.0/0` e `::/0` | Acesso ao site com criptografia |

**Não** crie regra para a porta **8080**: o backend só escuta dentro do servidor e é acessado pelo Nginx. Regras de
saída: deixe o padrão (tudo liberado), o backend precisa falar com o Supabase.

> **Atenção ao "Meu IP":** se a sua internet tiver IPv6, a AWS pode preencher a regra 22 com um endereço IPv6
> (ex.: `2804:d57::/32`), e o `ssh` (que usa IPv4) dá *timed out*. Confira a origem da regra: se começar com números e
> dois-pontos, **exclua** essa regra e crie outra SSH com origem *Personalizado* = o número que aparece em
> <https://checkip.amazonaws.com> seguido de `/32` (ex.: `177.0.206.240/32`). A AWS não deixa trocar uma regra IPv6
> por IPv4 editando: tem de excluir e criar.
>
> Se o seu IP mudar (internet residencial muda de tempos em tempos), o SSH para de funcionar: atualize essa regra com
> o IP novo.

Clique em **Executar instância**.

## 3. Elastic IP (endereço fixo)

Sem Elastic IP, o endereço público da instância muda toda vez que ela é desligada.

1. **EC2 › Rede e segurança › IPs elásticos › Alocar endereço IP elástico › Alocar**.
2. Selecione o IP criado › **Ações › Associar endereço IP elástico** › escolha a instância `smartstock` › **Associar**.
3. Anote o endereço (ex.: `18.231.10.20`). É ele que você vai usar daqui em diante.

> O Elastic IP é gratuito só enquanto estiver associado a uma instância ligada. Se apagar a instância, **libere** o IP.

## 4. Entrar no servidor por SSH

No PowerShell do Windows (o SSH já vem instalado no Windows 10/11):

```powershell
# a chave precisa ser legível só por você (senão o ssh recusa)
icacls $env:USERPROFILE\.ssh\smartstock.pem /inheritance:r /grant:r "$($env:USERNAME):R"

ssh -i $env:USERPROFILE\.ssh\smartstock.pem ubuntu@SEU_ELASTIC_IP
```

Na primeira vez ele pergunta se confia no servidor: digite `yes`. Para sair do servidor: `exit`.

## 5. Preparar o servidor (uma vez)

No seu computador, na pasta do projeto, envie a pasta `deploy` e o modelo do `.env`:

```powershell
scp -i $env:USERPROFILE\.ssh\smartstock.pem -r deploy backend\.env.example ubuntu@SEU_ELASTIC_IP:~/
```

No servidor:

```bash
mkdir -p ~/backend && mv ~/.env.example ~/backend/
bash ~/deploy/instalar-ec2.sh
```

O script instala Java 21, Nginx e certbot, cria o usuário `smartstock` (sem senha e sem acesso ao terminal), a pasta
`/opt/smartstock`, o serviço do backend, a configuração do Nginx e liga o firewall do Ubuntu (`ufw`).

## 6. Senhas e chaves no servidor (.env)

Ainda no servidor, edite o arquivo (ele só pode ser lido pelo usuário do serviço):

```bash
sudo nano /opt/smartstock/.env
```

Preencha as mesmas linhas do seu `backend/.env` e acrescente o endereço público:

```properties
SUPABASE_DB_PASSWORD=...
SMARTSTOCK_SECRET=...
SUPABASE_SERVICE_KEY=...
MAIL_USUARIO=...
MAIL_SENHA=...
CORS_ORIGENS=http://SEU_ELASTIC_IP
```

Salve com `Ctrl+O`, `Enter`, e saia com `Ctrl+X`. **Nunca** coloque esse arquivo no GitHub.

**Atalho:** em vez de digitar tudo, envie o seu `backend\.env` e instale no lugar certo:

```powershell
scp -i $env:USERPROFILE\.ssh\smartstock.pem backend\.env ubuntu@SEU_ELASTIC_IP:~/
```
```bash
# no servidor
sudo install -o smartstock -g smartstock -m 600 ~/.env /opt/smartstock/.env && rm ~/.env
printf '\nCORS_ORIGENS=http://SEU_ELASTIC_IP\n' | sudo tee -a /opt/smartstock/.env
```

O `\n` no começo garante que o `CORS_ORIGENS` fique numa linha própria: se a última linha do `.env` não terminar com
Enter, sem ele o texto gruda no fim da linha anterior (por exemplo, na `MAIL_SENHA`) e o envio de e-mail falha com
`535`. Use o **mesmo**
`SMARTSTOCK_SECRET` do seu computador se o banco for o mesmo (senão todos precisam entrar de novo).

## 7. Publicar o sistema

No seu computador (PowerShell, na pasta do projeto), **com o backend e o `npm run dev` parados** (Ctrl + C) — o
`npm run dev` aberto trava os arquivos do frontend e o build falha:

```powershell
.\deploy\publicar.ps1 -Servidor SEU_ELASTIC_IP -Chave $env:USERPROFILE\.ssh\smartstock.pem
```

O script gera o backend e o frontend, envia para o servidor, troca a versão e reinicia o serviço. No final deve
aparecer `active` e `API respondeu: 401` (401 = no ar e protegida por login). Se alguma etapa falhar, o script para e
mostra `Falhou: ...` sem mexer no servidor. Abra `http://SEU_ELASTIC_IP` no navegador. A documentação da API fica em
`http://SEU_ELASTIC_IP/swagger-ui.html`.

> Se o PowerShell bloquear o script, rode antes: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

Para cada versão nova, basta repetir este passo.

## 8. HTTPS (obrigatório para uso real)

Sem HTTPS, a senha e o código do 2FA viajam sem criptografia entre o navegador e o servidor. Para ter HTTPS é preciso
um **domínio** apontando para o Elastic IP:

1. Use um domínio próprio (ex.: registro.br) ou um subdomínio gratuito (ex.: [DuckDNS](https://www.duckdns.org/)) e
   crie um registro **A** apontando para o Elastic IP.
2. No servidor:
   ```bash
   sudo certbot --nginx -d smartstock.seudominio.com.br --redirect -m seu-email@exemplo.com --agree-tos
   ```
   O certbot gera o certificado gratuito (Let's Encrypt), configura o Nginx e passa a redirecionar `http` para `https`.
   A renovação é automática.
3. Atualize `CORS_ORIGENS=https://smartstock.seudominio.com.br` no `/opt/smartstock/.env` e rode
   `sudo systemctl restart smartstock`.

## 9. Comandos do dia a dia (no servidor)

| Para… | Comando |
|-------|---------|
| Ver se o backend está rodando | `sudo systemctl status smartstock` |
| Ver o log ao vivo | `sudo journalctl -u smartstock -f` |
| Reiniciar o backend | `sudo systemctl restart smartstock` |
| Testar e recarregar o Nginx | `sudo nginx -t && sudo systemctl reload nginx` |
| Atualizar o Ubuntu | `sudo apt update && sudo apt upgrade -y` |
| Ver o firewall | `sudo ufw status` |

## 10. Problemas comuns

| Sintoma | O que fazer |
|---------|-------------|
| O código de login não chega por e-mail | Veja `sudo journalctl -u smartstock -n 100` (`Falha ao enviar e-mail`). A instância precisa sair pela porta **465** para o Gmail: o Security Group libera saída por padrão; confira se não foi alterado. |
| `ssh: connect to host ... timed out` | A regra 22 não libera o seu IPv4 (ficou IPv6 ou o IP mudou): veja o aviso do passo 2. Confira também se a instância está *Executando* (no AWS Academy, a sessão pode ter acabado). |
| Login dá "Não foi possível enviar o código por e-mail" só no servidor | Veja o motivo: `sudo journalctl -u smartstock --no-pager \| grep -i "Falha ao enviar" \| tail -3`. `535` = `MAIL_USUARIO`/`MAIL_SENHA` errados no `/opt/smartstock/.env` (confira se o `CORS_ORIGENS` não grudou na linha da senha: `sudo grep -o '^[A-Z_]*=' /opt/smartstock/.env` deve mostrar um nome por linha). Depois de corrigir: `sudo systemctl restart smartstock`. |
| `'vite' não é reconhecido` ao publicar | O `npm run dev` estava aberto. Pare com Ctrl + C e publique de novo. |
| `Permissions for 'smartstock.pem' are too open` | Rode o comando `icacls` do passo 4. |
| Site abre, mas "Erro ao entrar"/nada carrega | O backend não subiu: veja `sudo journalctl -u smartstock -n 100`. Normalmente falta uma linha no `/opt/smartstock/.env`. |
| `502 Bad Gateway` | O backend está iniciando (espere ~30 s) ou caiu (veja o log). |
| `413 Request Entity Too Large` | Arquivo maior que o limite: o Nginx aceita até 100 MB por envio e o backend 20 MB por arquivo. |
| Backend reinicia sozinho por falta de memória | Em `t2.micro`/`t3.micro`, crie 1 GB de swap: `sudo fallocate -l 1G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile && echo '/swapfile none swap sw 0 0' \| sudo tee -a /etc/fstab` |
| Certbot falha | Confira se o domínio já aponta para o Elastic IP (`nslookup seu-dominio`) e se a porta 80 está liberada. |

## Checklist de segurança

- [x] Porta 22 liberada só para o seu IP; acesso por chave (`.pem`), sem senha.
- [x] Porta 8080 fechada; backend escuta só em `127.0.0.1`.
- [x] Backend roda com usuário próprio, sem shell, com proteções do systemd (`ProtectSystem`, `NoNewPrivileges`).
- [x] Senhas e chaves só em `/opt/smartstock/.env` (`chmod 600`), fora do Git.
- [x] Firewall do Ubuntu (`ufw`) além do Security Group.
- [x] Cabeçalhos de segurança no Nginx (CSP, X-Frame-Options, nosniff) e versão do Nginx escondida.
- [ ] HTTPS com certbot (depende de um domínio — passo 8).
