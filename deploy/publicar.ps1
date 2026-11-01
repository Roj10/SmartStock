# Publica uma nova versão do SmartStock no servidor da AWS EC2 (rode no PowerShell, na pasta do projeto).
#
# Uso:
#   .\deploy\publicar.ps1 -Servidor 1.2.3.4 -Chave C:\Users\voce\.ssh\smartstock.pem
#
# O que ele faz:
#   1. gera o backend (backend\target\backend-*.jar) e o frontend (frontend\dist);
#   2. copia os dois para o servidor pelo SSH (scp);
#   3. troca os arquivos em /opt/smartstock e reinicia o backend.
# Seu .env NÃO é enviado: no servidor ele fica em /opt/smartstock/.env (preenchido uma vez, à mão).
param(
    [Parameter(Mandatory = $true)][string]$Servidor,   # Elastic IP ou domínio
    [Parameter(Mandatory = $true)][string]$Chave,      # arquivo .pem baixado ao criar a instância
    [string]$Usuario = "ubuntu"
)
$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot

Write-Host "== 1/3 Gerando backend e frontend"
Push-Location "$raiz\backend";  .\mvnw.cmd -q clean package -DskipTests; Pop-Location
Push-Location "$raiz\frontend"; npm ci; npm run build; Pop-Location
$jar = Get-ChildItem "$raiz\backend\target\*.jar" | Where-Object { $_.Name -notlike "*plain*" } | Select-Object -First 1

Write-Host "== 2/3 Enviando para $Servidor"
$destino = "$Usuario@${Servidor}"
ssh -i $Chave $destino "rm -rf /tmp/smartstock-novo && mkdir -p /tmp/smartstock-novo"
scp -i $Chave $jar.FullName "${destino}:/tmp/smartstock-novo/backend.jar"
scp -i $Chave -r "$raiz\frontend\dist" "${destino}:/tmp/smartstock-novo/frontend"
if (Test-Path "$raiz\backend\supabase-ca.crt") {
    scp -i $Chave "$raiz\backend\supabase-ca.crt" "${destino}:/tmp/smartstock-novo/supabase-ca.crt"
}

Write-Host "== 3/3 Trocando a versão e reiniciando"
$comandos = @"
set -e
sudo install -o smartstock -g smartstock -m 640 /tmp/smartstock-novo/backend.jar /opt/smartstock/backend.jar
[ -f /tmp/smartstock-novo/supabase-ca.crt ] && sudo install -o smartstock -g smartstock -m 644 /tmp/smartstock-novo/supabase-ca.crt /opt/smartstock/supabase-ca.crt
sudo rm -rf /opt/smartstock/frontend && sudo cp -r /tmp/smartstock-novo/frontend /opt/smartstock/frontend
sudo chmod -R a+rX /opt/smartstock/frontend
sudo systemctl restart smartstock
sleep 25
systemctl is-active smartstock && curl -s -o /dev/null -w 'API respondeu: %{http_code} (401 = no ar e protegida)\n' http://127.0.0.1:8080/api/dashboard
"@
# o Linux não aceita a quebra de linha do Windows (CRLF) nos comandos
ssh -i $Chave $destino ($comandos -replace "`r", "")
Write-Host "Publicado: http://$Servidor"
