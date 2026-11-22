# Publica uma nova versão do SmartStock no servidor da AWS EC2 (rode no PowerShell, na pasta do projeto).
#
# Uso:
#   .\deploy\publicar.ps1 -Servidor 1.2.3.4 -Chave C:\Users\voce\.ssh\smartstock.pem
#
# O que ele faz:
#   1. gera o backend (backend\target\backend-*.jar) e o frontend (frontend\dist);
#   2. copia os dois para o servidor pelo SSH (scp);
#   3. troca os arquivos em /opt/smartstock e reinicia o backend.
# Se qualquer etapa der erro, ele para ali e não mexe no servidor.
# Seu .env NÃO é enviado: no servidor ele fica em /opt/smartstock/.env (preenchido uma vez, à mão).
# Antes de rodar, pare o backend (Ctrl + C) e o "npm run dev" que estiverem abertos no seu PC.
param(
    [Parameter(Mandatory = $true)][string]$Servidor,   # Elastic IP ou domínio
    [Parameter(Mandatory = $true)][string]$Chave,      # arquivo .pem baixado ao criar a instância
    [string]$Usuario = "ubuntu"
)
$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot

# Roda um programa (mvnw, npm, ssh, scp) e para tudo se ele terminar com erro.
function Rodar([string]$descricao, [scriptblock]$comando) {
    & $comando
    if ($LASTEXITCODE -ne 0) {
        throw "Falhou: $descricao (código $LASTEXITCODE). Nada foi trocado no servidor."
    }
}

Write-Host "== 1/3 Gerando backend e frontend"
Push-Location "$raiz\backend"
try { Rodar "gerar o backend (mvnw)" { .\mvnw.cmd -q clean package -DskipTests } } finally { Pop-Location }
Push-Location "$raiz\frontend"
try {
    Rodar "instalar as dependências do frontend (npm install)" { npm install }
    Rodar "gerar o frontend (npm run build)" { npm run build }
} finally { Pop-Location }
$jar = Get-ChildItem "$raiz\backend\target\*.jar" | Where-Object { $_.Name -notlike "*plain*" } | Select-Object -First 1
if (-not $jar) { throw "Não encontrei o .jar em backend\target." }
if (-not (Test-Path "$raiz\frontend\dist\index.html")) { throw "Não encontrei o frontend gerado em frontend\dist." }

Write-Host "== 2/3 Enviando para $Servidor"
$destino = "$Usuario@${Servidor}"
Rodar "preparar a pasta no servidor" { ssh -i $Chave $destino "rm -rf /tmp/smartstock-novo && mkdir -p /tmp/smartstock-novo" }
Rodar "enviar o backend" { scp -i $Chave $jar.FullName "${destino}:/tmp/smartstock-novo/backend.jar" }
Rodar "enviar o frontend" { scp -i $Chave -r "$raiz\frontend\dist" "${destino}:/tmp/smartstock-novo/frontend" }
if (Test-Path "$raiz\backend\supabase-ca.crt") {
    Rodar "enviar o certificado do Supabase" { scp -i $Chave "$raiz\backend\supabase-ca.crt" "${destino}:/tmp/smartstock-novo/supabase-ca.crt" }
}

Write-Host "== 3/3 Trocando a versão e reiniciando (espera ~30 s)"
$comandos = @"
set -e
sudo install -o smartstock -g smartstock -m 640 /tmp/smartstock-novo/backend.jar /opt/smartstock/backend.jar
if [ -f /tmp/smartstock-novo/supabase-ca.crt ]; then sudo install -o smartstock -g smartstock -m 644 /tmp/smartstock-novo/supabase-ca.crt /opt/smartstock/supabase-ca.crt; fi
sudo rm -rf /opt/smartstock/frontend
sudo cp -r /tmp/smartstock-novo/frontend /opt/smartstock/frontend
sudo chmod -R a+rX /opt/smartstock/frontend
sudo systemctl restart smartstock
for i in `$(seq 1 60); do curl -s -o /dev/null http://127.0.0.1:8080/api/dashboard && break; sleep 2; done
systemctl is-active smartstock
curl -s -o /dev/null -w 'API respondeu: %{http_code} (401 = no ar e protegida)\n' http://127.0.0.1:8080/api/dashboard
"@
# o Linux não aceita a quebra de linha do Windows (CRLF) nos comandos
Rodar "trocar a versão no servidor" { ssh -i $Chave $destino ($comandos -replace "`r", "") }
Write-Host "Publicado: http://$Servidor"
