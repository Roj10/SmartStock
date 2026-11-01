#!/usr/bin/env bash
# Prepara um servidor Ubuntu (22.04 ou 24.04) da AWS EC2 para rodar o SmartStock.
#
# Rode UMA vez, dentro do servidor, depois de copiar a pasta "deploy" para ele:
#   bash deploy/instalar-ec2.sh
#
# O que ele faz, passo a passo:
#   1. instala o Java 21 (para o backend), o Nginx (servidor web) e o Certbot (certificado HTTPS gratuito);
#   2. cria o usuário "smartstock", só para rodar o backend;
#   3. cria a pasta /opt/smartstock e protege o arquivo .env (só o dono lê);
#   4. instala o serviço (smartstock.service) e a configuração do Nginx;
#   5. liga o firewall do Ubuntu deixando passar só SSH (22) e web (80/443).
# Depois disso, envie o backend.jar, o .env e o build do frontend (veja docs/implantacao-aws-ec2.md).
set -euo pipefail
cd "$(dirname "$0")"

echo "== 1/5 Instalando Java 21, Nginx e Certbot"
sudo apt-get update -y
sudo apt-get install -y openjdk-21-jre-headless nginx certbot python3-certbot-nginx ufw

echo "== 2/5 Criando o usuário de serviço 'smartstock'"
id smartstock >/dev/null 2>&1 || sudo useradd --system --no-create-home --shell /usr/sbin/nologin smartstock

echo "== 3/5 Criando /opt/smartstock"
sudo mkdir -p /opt/smartstock/frontend
if [ ! -f /opt/smartstock/.env ]; then
  sudo cp ../backend/.env.example /opt/smartstock/.env 2>/dev/null || sudo touch /opt/smartstock/.env
  echo "   -> edite /opt/smartstock/.env com: sudo nano /opt/smartstock/.env"
fi
sudo chown -R smartstock:smartstock /opt/smartstock
sudo chmod 600 /opt/smartstock/.env
# o Nginx precisa ler as telas do frontend
sudo chmod 755 /opt/smartstock /opt/smartstock/frontend

echo "== 4/5 Instalando o serviço e o Nginx"
sudo cp smartstock.service /etc/systemd/system/smartstock.service
sudo systemctl daemon-reload
sudo systemctl enable smartstock
sudo cp nginx-smartstock.conf /etc/nginx/sites-available/smartstock
sudo ln -sf /etc/nginx/sites-available/smartstock /etc/nginx/sites-enabled/smartstock
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx

echo "== 5/5 Firewall do Ubuntu (além do Security Group da AWS)"
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw --force enable

echo
echo "Pronto. Falta: enviar backend.jar e o frontend, preencher /opt/smartstock/.env e rodar"
echo "  sudo systemctl restart smartstock"
