# DEPLOYMENT.md — Guia de Implantação em Produção

Este documento substitui a versão que foi acidentalmente removida; contém instruções completas e verificadas para colocar a aplicação GED em produção usando Docker, Nginx (reverse proxy), SSL (Let's Encrypt), backups e monitoramento.

Siga os passos na ordem: preparar infraestrutura -> build -> deploy -> monitorar -> backup.

---

SUMÁRIO
- Pré-requisitos
- Arquivos de configuração essenciais
- Exemplo de `.env.prod`
- Docker: build e push de imagem
- `docker-compose.prod.yml` (exemplo)
- Nginx: configuração de reverse proxy e SSL
- Let's Encrypt (certbot)
- Backup / Restore (scripts)
- Healthcheck e monitoramento
- Segurança e boas práticas
- Troubleshooting rápido
- Checklist de deploy

---

## 1. Pré-requisitos

- Servidor Linux (Ubuntu 20.04+ recomendado) ou provedor em cloud (AWS, GCP, Azure).
- Docker Engine (20.10+) instalado.
- Docker Compose v2 (ou Compose plugin) instalado.
- Domínio público com DNS apontando para o servidor.
- Acesso SSH ao servidor e permissão para executar comandos administrativos.
- Porta 80 e 443 liberadas (HTTP/HTTPS).

Instalação rápida (Ubuntu):

```bash
# atualizar
sudo apt update && sudo apt upgrade -y

# instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# instalar Docker Compose plugin
sudo apt-get install -y docker-compose-plugin

# instalar certbot (Let's Encrypt)
sudo apt install -y certbot
```

---

## 2. Arquivos de configuração essenciais

No diretório do projeto em produção (`/opt/desafio-uds` por exemplo) mantenha:

- `.env.prod` (variáveis de ambiente sensíveis, NÃO commitar)
- `docker-compose.prod.yml` (definição de serviços para produção)
- `nginx.conf` (configuração do reverse proxy)
- `full-backup.sh` (script de backup)
- `restore.sh` (script de restauração)

**NUNCA** versionar `.env.prod` com segredos. Use secret manager quando possível (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault).

---

## 3. Exemplo de `.env.prod`

Crie `.env.prod` com permissões restritas (chmod 600) e preencha valores reais:

```
# Spring Boot
official_SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/api

# Database
action_SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/desafio_uds_prod
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=UmaSenhaMuitoForteAqui123!

# JWT
JWT_SECRET=GerarComOpenSSL_32+chars
JWT_EXPIRATION=86400000

# CORS
official_CORS_ALLOWED_ORIGINS=https://seu-dominio.com

# S3 (opcional)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=
AWS_S3_BUCKET=

# Outros
enable_metrics=true
```

Substitua valores por segredos gerenciados quando possível.

---

## 4. Build da imagem Docker e push

No ambiente de build (CI) execute as etapas abaixo. A pipeline do GitHub Actions deve:

1. Compilar e rodar testes (mvn test)
2. Gerar artifact (JAR)
3. Build da imagem Docker
4. Tag e push para registry (Docker Hub / ECR / ACR)

Exemplo de comandos locais (assumindo Docker e Maven no host de build):

```bash
# build jar
mvn -Pprod clean package -DskipTests

# build docker image
docker build -t seu-registry/desafio-uds:1.0.0 .

docker tag seu-registry/desafio-uds:1.0.0 seu-registry/desafio-uds:latest

# push
docker push seu-registry/desafio-uds:1.0.0
```

No CI (GitHub Actions) configure secrets para `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `REGISTRY` e `JWT_SECRET`.

---

## 5. `docker-compose.prod.yml` (exemplo)

Salve este arquivo como `docker-compose.prod.yml` no servidor. Ajuste nomes e variáveis conforme seu ambiente.

```yaml
version: '3.9'
services:
  backend:
    image: seu-registry/desafio-uds:1.0.0
    container_name: ged-backend
    restart: unless-stopped
    env_file: .env.prod
    ports:
      - "8080:8080"
    volumes:
      - ./uploads:/app/uploads
      - ./logs:/app/logs
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD-SHELL","curl -f http://localhost:8080/api/auth/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3

  postgres:
    image: postgres:15-alpine
    container_name: ged-postgres
    environment:
      POSTGRES_USER: ${SPRING_DATASOURCE_USERNAME}
      POSTGRES_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      POSTGRES_DB: desafio_uds_prod
    restart: unless-stopped
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL","pg_isready -U ${SPRING_DATASOURCE_USERNAME} -d desafio_uds_prod || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5

  nginx:
    image: nginx:alpine
    container_name: ged-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres-data:
```

Ajuste `seu-registry/desafio-uds:1.0.0` para o registry usado.

---

## 6. Nginx: reverse proxy e SSL

Exemplo mínimo de `nginx.conf` (ajuste paths de certificados):

```nginx
user  nginx;
worker_processes  auto;
error_log  /var/log/nginx/error.log warn;
pid        /var/run/nginx.pid;

events { worker_connections 1024; }

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    upstream backend {
        server backend:8080;
    }

    server {
        listen 80;
        server_name seu-dominio.com www.seu-dominio.com;
        # redireciona para https
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name seu-dominio.com www.seu-dominio.com;

        ssl_certificate /etc/letsencrypt/live/seu-dominio.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/seu-dominio.com/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_prefer_server_ciphers on;

        client_max_body_size 10M; # limite de upload

        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_http_version 1.1;
            proxy_set_header Connection "";
        }
    }
}
```

Lembre-se de manter certificados atualizados e montar `/etc/letsencrypt` no container Nginx (leitura apenas).

---

## 7. Let's Encrypt (certbot)

Se o servidor tiver porta 80 aberta, você pode gerar certificados com certbot:

```bash
sudo apt install certbot
sudo certbot certonly --standalone -d seu-dominio.com -d www.seu-dominio.com

# Os certificados ficarão em /etc/letsencrypt/live/seu-dominio.com/
```

Automatize renovação com cron/ systemd timer (certbot já cria timer em muitos sistemas):

```bash
sudo systemctl enable --now certbot.timer
```

Após gerar o certificado, reinicie o Nginx para carregar os novos certificados.

---

## 8. Backup / Restore

### full-backup.sh (exemplo)

Crie `full-backup.sh` em `/opt/desafio-uds` e torne executável (chmod +x):

```bash
#!/bin/bash
BACKUP_DIR="/backups/desafio-uds"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
mkdir -p "$BACKUP_DIR"

# Dump do banco
docker-compose -f docker-compose.prod.yml exec -T postgres pg_dump -U ${SPRING_DATASOURCE_USERNAME} desafio_uds_prod | gzip > "$BACKUP_DIR/db_$TIMESTAMP.sql.gz"

# Arquivos (uploads)
tar -czf "$BACKUP_DIR/uploads_$TIMESTAMP.tar.gz" ./uploads

# Logs
tar -czf "$BACKUP_DIR/logs_$TIMESTAMP.tar.gz" ./logs || true

# Opcional: sync para S3
# aws s3 cp "$BACKUP_DIR" s3://seu-bucket/backups/ --recursive

echo "Backup completo criado em $BACKUP_DIR (timestamp: $TIMESTAMP)"
```

### restore.sh (exemplo)

```bash
#!/bin/bash
# uso: ./restore.sh db_20250218_120000.sql.gz uploads_20250218.tar.gz
DB_FILE=$1
UPLOADS_FILE=$2

if [ -z "$DB_FILE" ] || [ -z "$UPLOADS_FILE" ]; then
  echo "Uso: ./restore.sh <db_dump.sql.gz> <uploads.tar.gz>"
  exit 1
fi

# Parar containers
docker-compose -f docker-compose.prod.yml down

# Restaurar DB
gunzip -c "$DB_FILE" | docker-compose -f docker-compose.prod.yml exec -T postgres psql -U ${SPRING_DATASOURCE_USERNAME} -d desafio_uds_prod

# Restaurar uploads
tar -xzf "$UPLOADS_FILE" -C ./uploads

# Subir containers
docker-compose -f docker-compose.prod.yml up -d

echo "Restore completo"
```

Teste estes scripts manualmente em um ambiente de staging antes de usar em produção.

---

## 9. Healthcheck e monitoramento

- Aplicação expõe `/api/auth/health` que retorna 200 se o serviço estiver pronto.
- Configure o `healthcheck` do `docker-compose` (exemplo no arquivo acima).
- Para monitoramento, recomenda-se integrar Prometheus + Grafana ou usar soluções gerenciadas.
- Centralize logs (filebeat → ELK, ou Docker logging driver).

Exemplo mínimo para Prometheus: exportar métricas via actuator e apontar Prometheus.

---

## 10. Segurança e boas práticas

- Não versionar `.env.prod` ou arquivos com segredos.
- Usar senhas fortes e rotacionar chaves regularmente.
- Executar a imagem como usuário não-root quando possível.
- Habilitar TLS 1.2/1.3 apenas e desabilitar ciphers antigos.
- Use WAF e bloqueio de IP quando aplicável.
- Habilite backups e testes periódicos de restauração.
- Configurar limits e quotas no Nginx (rate limiting) se necessário.

Exemplo de rate limiting (nginx):

```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

server {
  location / {
    limit_req zone=api burst=20 nodelay;
    proxy_pass http://backend;
  }
}
```

---

## 11. Troubleshooting rápido

- Problem: `backend` não sai do estado `starting` - verifique logs do container `docker-compose -f docker-compose.prod.yml logs backend` e o `healthcheck` configurado.
- Problem: Conexão DB falha - verifique `POSTGRES_PASSWORD` e se o volume do Postgres tem permissões corretas.
- Problem: 502 do Nginx - verifique se o `backend` está rodando e se o upstream `backend:8080` é alcançável (use `docker-compose exec nginx ping backend`).
- Problem: Certificados inválidos - teste `sudo certbot renew --dry-run` e reinicie o Nginx.

---

## 12. Checklist de Deploy (resumo)

- [ ] Servidor provisionado (Ubuntu 20.04+)
- [ ] Docker instalado e funcional
- [ ] DNS apontando para o servidor
- [ ] `.env.prod` criado (permissões restritas)
- [ ] Imagem Docker buildada e publicada no registry
- [ ] `docker-compose.prod.yml` no servidor
- [ ] `nginx.conf` configurado e montado
- [ ] Certificado Let's Encrypt gerado
- [ ] Backup script testado (staging)
- [ ] Health check OK
- [ ] Logs sendo coletados
- [ ] Segurança configurada (firewall, TLS)

---

## 13. Comandos Úteis

```bash
# Subir em produção (modo detach)
docker-compose -f docker-compose.prod.yml up -d

# Logs do backend
docker-compose -f docker-compose.prod.yml logs -f backend

# Ver status
docker-compose -f docker-compose.prod.yml ps

# Parar e remover containers
docker-compose -f docker-compose.prod.yml down

# Atualizar imagem: pull → recreate
docker pull seu-registry/desafio-uds:1.0.0
docker-compose -f docker-compose.prod.yml up -d --no-deps --build backend
```

---

## 14. Observações finais

- Teste tudo primeiro em um ambiente de staging antes de aplicar em produção.
- Idealmente mova o storage de arquivos para S3/MinIO para resilência e escalabilidade.
- Use um secret manager em vez de arquivos `.env.prod` em produção.

---

Se quiser, eu crio também uma tarefa pronta do GitHub Actions para deploy automático ao dar push em `main` (com build, push e trigger remoto via SSH para `docker-compose.prod.yml`).

