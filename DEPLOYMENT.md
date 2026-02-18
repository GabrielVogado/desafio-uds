# 🚀 DEPLOYMENT.md — Guia de Implantação em Produção

Guia completo para deploy da aplicação **Desafio UDS GED** (Gestão Eletrônica de Documentos) em produção usando Docker, PostgreSQL, GitHub Actions e boas práticas de DevOps.

---

## 📋 SUMÁRIO

1. [Pré-requisitos](#1-pré-requisitos)
2. [Estrutura do Projeto](#2-estrutura-do-projeto)
3. [Configuração de Ambiente](#3-configuração-de-ambiente)
4. [Build e Deploy via Docker](#4-build-e-deploy-via-docker)
5. [GitHub Actions (CI/CD)](#5-github-actions-cicd)
6. [Configuração de Produção](#6-configuração-de-produção)
7. [Backup e Restore](#7-backup-e-restore)
8. [Monitoramento e Logs](#8-monitoramento-e-logs)
9. [Segurança](#9-segurança)
10. [Troubleshooting](#10-troubleshooting)
11. [Checklist de Deploy](#11-checklist-de-deploy)


---

## 1. Pré-requisitos

### Infraestrutura

- **Servidor Linux**: Ubuntu 20.04+ ou similar
- **Docker Engine**: 20.10+
- **Docker Compose**: v2.x
- **Memória RAM**: Mínimo 2GB (recomendado 4GB)
- **Disco**: 20GB livres
- **Portas**: 8080 (backend), 5432 (postgres)

### Instalação Rápida (Ubuntu)

```bash
# Atualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Instalar Docker Compose
sudo apt-get install -y docker-compose-plugin

# Verificar instalação
docker --version
docker compose version
```

### Ferramentas de Desenvolvimento

- **Java 17** (Eclipse Temurin)
- **Maven 3.9+**
- **Git**
- **Node.js 18+** (para frontend Angular)

---

## 2. Estrutura do Projeto

```
desafio-uds/
├── .github/
│   └── workflows/
│       ├── ci.yml                    # Pipeline de testes
│       └── cd.yml                    # Pipeline de deploy
├── src/
│   ├── main/
│   │   ├── java/                     # Código backend
│   │   └── resources/
│   │       ├── application.properties           # Config padrão
│   │       ├── application-dev.properties       # Dev
│   │       ├── application-prod.properties      # Produção
│   │       └── db/migration/                    # Flyway migrations
│   └── test/                         # Testes unitários
├── frontend/                         # Angular app
├── uploads/                          # Arquivos enviados
├── docker-compose.yml                # Ambiente local/dev
├── Dockerfile                        # Build da aplicação
├── pom.xml                          # Maven config
└── README.md                        # Documentação

```

---

## 3. Configuração de Ambiente

### 3.1. Variáveis de Ambiente

O projeto usa **profiles do Spring Boot** para separar ambientes:

- `dev` - Desenvolvimento local
- `prod` - Produção

### 3.2. Arquivo `application-prod.properties`

Localização: `src/main/resources/application-prod.properties`

```properties
# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.hikari.maximum-pool-size=20

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway Migrations
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

# Server
server.servlet.context-path=/api
server.port=8080
server.compression.enabled=true

# File Upload
file.storage.path=/app/uploads
file.max-size=10485760
file.allowed-types=application/pdf,image/png,image/jpeg

# CORS
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200}

# Logging
logging.level.root=INFO
logging.level.br.com.gabrielvogado.desafiouds=INFO
```

### 3.3. Variáveis de Ambiente Obrigatórias

Crie um arquivo `.env.prod` (NÃO versionar):

```bash
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/desafio_uds
DATABASE_USER=postgres
DATABASE_PASSWORD=SenhaForteAqui123!

# JWT Secret (gerar com: openssl rand -base64 32)
JWT_SECRET=W2wvNCz77hYzwZktjysxmypm6YL2BciREhtKDSogW/A=

# CORS
CORS_ALLOWED_ORIGINS=https://seu-dominio.com

# Profile
SPRING_PROFILES_ACTIVE=prod
```

**⚠️ IMPORTANTE**: Nunca commite o `.env.prod` no Git!


---

## 4. Build e Deploy via Docker

### 4.1. Dockerfile

O projeto possui um `Dockerfile` multi-stage para build otimizado:

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src/ src/
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN mkdir -p uploads
COPY --from=builder /app/target/desafio-uds-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.2. Build Local

```bash
# Build da aplicação
mvn clean package -DskipTests

# Build da imagem Docker
docker build -t desafio-uds:latest .

# Tag para registry
docker tag desafio-uds:latest seu-usuario/desafio-uds:1.0.0
docker tag desafio-uds:latest seu-usuario/desafio-uds:latest

# Push para Docker Hub
docker login
docker push seu-usuario/desafio-uds:1.0.0
docker push seu-usuario/desafio-uds:latest
```

### 4.3. Docker Compose - Desenvolvimento

Arquivo: `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: desafio-uds-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: desafio_uds
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - desafio-network

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: desafio-uds-backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/desafio_uds
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_PROFILES_ACTIVE: dev
      JWT_SECRET: MyVerySecretKeyForJWTTokenGenerationAndValidationInDevEnvironment123!@#
      FILE_STORAGE_PATH: /app/uploads
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - ./uploads:/app/uploads
    networks:
      - desafio-network
    restart: on-failure

volumes:
  postgres_data:

networks:
  desafio-network:
    driver: bridge
```

**Executar localmente:**

```bash
# Subir ambiente
docker-compose up -d

# Ver logs
docker-compose logs -f backend

# Parar ambiente
docker-compose down

# Limpar volumes
docker-compose down -v
```


---

## 5. GitHub Actions (CI/CD)

### 5.1. Pipeline CI (`.github/workflows/ci.yml`)

**Executa:** Build, testes unitários e análise de código

**Triggers:**
- Push em `master` e `develop`
- Pull requests

**Secrets necessários:**
- `JWT_SECRET` - Chave JWT para testes

```yaml
name: CI Pipeline

on:
  push:
    branches: [ master, develop ]
  pull_request:
    branches: [ master, develop ]

permissions:
  contents: read
  checks: write
  pull-requests: write

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: desafio_uds_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v4
      
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build with Maven
        run: mvn clean compile

      - name: Run tests
        run: mvn test
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/desafio_uds_test
          JWT_SECRET: ${{ secrets.JWT_SECRET }}

      - name: Package application
        run: mvn package -DskipTests

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: application-jar
          path: target/*.jar
```

### 5.2. Pipeline CD (`.github/workflows/cd.yml`)

**Executa:** Build, testes, Docker build/push e deploy

**Triggers:**
- Push em `develop` (staging)
- Push em `master` (production)

**Secrets necessários:**
- `JWT_SECRET`
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`
- `STAGING_HOST` (opcional)
- `STAGING_USER` (opcional)
- `STAGING_SSH_KEY` (opcional)
- `PROD_HOST` (opcional)
- `PROD_USER` (opcional)
- `PROD_SSH_KEY` (opcional)

### 5.3. Configurar Secrets no GitHub

1. Acesse: `GitHub → Settings → Secrets and variables → Actions`
2. Clique em: `New repository secret`
3. Adicione os seguintes secrets:

```bash
# JWT Secret (gerar com PowerShell)
$bytes = New-Object byte[] 32
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
# Resultado: W2wvNCz77hYzwZktjysxmypm6YL2BciREhtKDSogW/A=

# Docker Hub
Nome: DOCKER_USERNAME
Valor: seu_username_docker_hub

Nome: DOCKER_PASSWORD
Valor: seu_token_acesso_docker_hub (gerar em hub.docker.com/settings/security)

Nome: JWT_SECRET
Valor: W2wvNCz77hYzwZktjysxmypm6YL2BciREhtKDSogW/A=
```

### 5.4. Executar Workflow Manualmente

1. Acesse: `GitHub → Actions`
2. Selecione o workflow
3. Clique em: `Run workflow`
4. Selecione a branch
5. Clique em: `Run workflow`


---

## 6. Configuração de Produção

### 6.1. Docker Compose Produção

Crie `docker-compose.prod.yml` no servidor:

```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: desafio-uds-postgres-prod
    environment:
      POSTGRES_USER: ${DATABASE_USER}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
      POSTGRES_DB: desafio_uds_prod
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DATABASE_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - desafio-network
    restart: unless-stopped

  backend:
    image: ${DOCKER_USERNAME}/desafio-uds:latest
    container_name: desafio-uds-backend-prod
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/desafio_uds_prod
      DATABASE_USER: ${DATABASE_USER}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
      FILE_STORAGE_PATH: /app/uploads
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - ./uploads:/app/uploads
      - ./logs:/app/logs
    networks:
      - desafio-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/api/actuator/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3

volumes:
  postgres_data:

networks:
  desafio-network:
    driver: bridge
```

### 6.2. Subir Aplicação em Produção

```bash
# Criar diretório no servidor
sudo mkdir -p /opt/desafio-uds
cd /opt/desafio-uds

# Copiar arquivos necessários
# - docker-compose.prod.yml
# - .env.prod

# Dar pull na imagem
docker pull seu-usuario/desafio-uds:latest

# Subir aplicação
docker-compose -f docker-compose.prod.yml up -d

# Ver logs
docker-compose -f docker-compose.prod.yml logs -f backend

# Verificar status
docker-compose -f docker-compose.prod.yml ps
```

### 6.3. Nginx (Opcional - Reverse Proxy)


Se desejar usar Nginx como reverse proxy, crie `nginx.conf`:

```nginx
user nginx;
worker_processes auto;

events {
    worker_connections 1024;
}

http {
    upstream backend {
        server backend:8080;
    }

    server {
        listen 80;
        server_name seu-dominio.com;
        
        client_max_body_size 10M;

        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

Para SSL/HTTPS, adicione certificados Let's Encrypt:

```bash
# Instalar certbot
sudo apt install certbot

# Gerar certificado
sudo certbot certonly --standalone -d seu-dominio.com

# Os certificados ficarão em /etc/letsencrypt/live/seu-dominio.com/
```

Atualize o nginx.conf para usar HTTPS:

```nginx
server {
    listen 80;
    server_name seu-dominio.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name seu-dominio.com;

    ssl_certificate /etc/letsencrypt/live/seu-dominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/seu-dominio.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    client_max_body_size 10M;

    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 7. Backup e Restore

### 7.1. Script de Backup

Crie `full-backup.sh` em `/opt/desafio-uds`:

```bash
#!/bin/bash
BACKUP_DIR="/backups/desafio-uds"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
mkdir -p "$BACKUP_DIR"

# Dump do banco
docker-compose -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U ${DATABASE_USER} desafio_uds_prod | \
  gzip > "$BACKUP_DIR/db_$TIMESTAMP.sql.gz"

# Arquivos (uploads)
tar -czf "$BACKUP_DIR/uploads_$TIMESTAMP.tar.gz" ./uploads

# Logs
tar -czf "$BACKUP_DIR/logs_$TIMESTAMP.tar.gz" ./logs || true

echo "Backup completo: $BACKUP_DIR (timestamp: $TIMESTAMP)"
```

Tornar executável:

```bash
chmod +x full-backup.sh
```

Executar manualmente:

```bash
./full-backup.sh
```

Agendar no cron (diariamente às 2h):

```bash
crontab -e
# Adicionar linha:
0 2 * * * cd /opt/desafio-uds && ./full-backup.sh >> /var/log/backup-desafio-uds.log 2>&1
```

### 7.2. Script de Restore

Crie `restore.sh`:

```bash
#!/bin/bash
DB_FILE=$1
UPLOADS_FILE=$2

if [ -z "$DB_FILE" ] || [ -z "$UPLOADS_FILE" ]; then
  echo "Uso: ./restore.sh <db_dump.sql.gz> <uploads.tar.gz>"
  exit 1
fi

# Parar containers
docker-compose -f docker-compose.prod.yml down

# Restaurar DB
gunzip -c "$DB_FILE" | \
  docker-compose -f docker-compose.prod.yml exec -T postgres \
  psql -U ${DATABASE_USER} -d desafio_uds_prod

# Restaurar uploads
tar -xzf "$UPLOADS_FILE" -C ./uploads

# Subir containers
docker-compose -f docker-compose.prod.yml up -d

echo "Restore completo"
```

Tornar executável:

```bash
chmod +x restore.sh
```

Executar:

```bash
./restore.sh /backups/desafio-uds/db_20260218_020000.sql.gz \
             /backups/desafio-uds/uploads_20260218_020000.tar.gz
```

---

## 8. Monitoramento e Logs

### 8.1. Ver Logs

```bash
# Logs do backend
docker-compose -f docker-compose.prod.yml logs -f backend

# Logs do postgres
docker-compose -f docker-compose.prod.yml logs -f postgres

# Últimas 100 linhas
docker-compose -f docker-compose.prod.yml logs --tail=100 backend
```

### 8.2. Healthcheck

A aplicação expõe endpoint de health:

```bash
curl http://localhost:8080/api/actuator/health
```

Resposta esperada:

```json
{
  "status": "UP"
}
```

### 8.3. Monitoramento (Recomendado)

Para produção, considere:

- **Prometheus + Grafana**: Métricas e dashboards
- **ELK Stack**: Centralização de logs
- **Uptime monitoring**: UptimeRobot, Pingdom

---

## 9. Segurança

### 9.1. Boas Práticas

- ✅ **Não versionar** `.env.prod` ou arquivos com segredos
- ✅ **Senhas fortes**: Mínimo 16 caracteres
- ✅ **JWT Secret**: Gerar com criptografia segura (32+ bytes)
- ✅ **HTTPS**: Obrigatório em produção
- ✅ **Firewall**: Liberar apenas portas necessárias (80, 443, 22)
- ✅ **Backups**: Testar restauração periodicamente
- ✅ **Updates**: Manter imagens Docker atualizadas
- ✅ **Rate Limiting**: Configurar no Nginx se necessário

### 9.2. Firewall (UFW)

```bash
# Habilitar firewall
sudo ufw enable

# Permitir SSH
sudo ufw allow 22/tcp

# Permitir HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Verificar status
sudo ufw status
```

### 9.3. Rotação de Secrets

Rotacione JWT_SECRET periodicamente:

```bash
# Gerar novo secret
$bytes = New-Object byte[] 32
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)

# Atualizar .env.prod
# Reiniciar aplicação
docker-compose -f docker-compose.prod.yml restart backend
```

---

## 10. Troubleshooting

### 10.1. Problemas Comuns

**Problema: Container backend não inicia**

```bash
# Ver logs detalhados
docker-compose -f docker-compose.prod.yml logs backend

# Verificar variáveis de ambiente
docker-compose -f docker-compose.prod.yml config

# Verificar se o postgres está saudável
docker-compose -f docker-compose.prod.yml ps
```

**Problema: Erro de conexão com banco**

```bash
# Testar conexão manual
docker-compose -f docker-compose.prod.yml exec postgres \
  psql -U ${DATABASE_USER} -d desafio_uds_prod -c "SELECT 1"

# Verificar senha
echo $DATABASE_PASSWORD
```

**Problema: Uploads não funcionam**

```bash
# Verificar permissões
ls -la ./uploads

# Corrigir permissões
sudo chmod -R 755 ./uploads
sudo chown -R 1000:1000 ./uploads
```

**Problema: 502 Bad Gateway (Nginx)**

```bash
# Verificar se backend está rodando
curl http://localhost:8080/api/actuator/health

# Verificar logs do Nginx
docker-compose -f docker-compose.prod.yml logs nginx
```

### 10.2. Comandos Úteis

```bash
# Reiniciar aplicação
docker-compose -f docker-compose.prod.yml restart backend

# Recriar containers
docker-compose -f docker-compose.prod.yml up -d --force-recreate

# Limpar recursos não usados
docker system prune -a

# Ver uso de recursos
docker stats

# Inspecionar container
docker inspect desafio-uds-backend-prod
```

---

## 11. Checklist de Deploy

### Pré-Deploy

- [ ] Servidor provisionado (Ubuntu 20.04+)
- [ ] Docker e Docker Compose instalados
- [ ] DNS configurado (se usar domínio)
- [ ] Firewall configurado (UFW)
- [ ] `.env.prod` criado com secrets fortes
- [ ] Imagem Docker buildada e publicada
- [ ] `docker-compose.prod.yml` criado

### Deploy

- [ ] Copiar arquivos para `/opt/desafio-uds`
- [ ] Executar `docker-compose up -d`
- [ ] Verificar logs: `docker-compose logs -f`
- [ ] Testar health check: `curl http://localhost:8080/api/actuator/health`
- [ ] Testar endpoints da API

### Pós-Deploy

- [ ] Configurar Nginx (se usado)
- [ ] Gerar certificado SSL (Let's Encrypt)
- [ ] Configurar backups automáticos (cron)
- [ ] Testar restore de backup
- [ ] Configurar monitoramento
- [ ] Documentar credenciais (em local seguro)
- [ ] Rotacionar secrets padrão

### Manutenção

- [ ] Backups diários funcionando
- [ ] Teste mensal de restore
- [ ] Atualizar imagens Docker (mensalmente)
- [ ] Renovar certificados SSL (automático com certbot)
- [ ] Revisar logs de segurança
- [ ] Monitorar uso de disco/memória

---

## 12. Comandos Rápidos

```bash
# Subir aplicação
docker-compose -f docker-compose.prod.yml up -d

# Ver logs
docker-compose -f docker-compose.prod.yml logs -f backend

# Parar aplicação
docker-compose -f docker-compose.prod.yml down

# Reiniciar backend
docker-compose -f docker-compose.prod.yml restart backend

# Atualizar imagem
docker pull seu-usuario/desafio-uds:latest
docker-compose -f docker-compose.prod.yml up -d --no-deps --build backend

# Backup
./full-backup.sh

# Restore
./restore.sh backup.sql.gz uploads.tar.gz

# Health check
curl http://localhost:8080/api/actuator/health
```

---

## 13. Suporte

Para problemas ou dúvidas:

1. Verificar logs: `docker-compose logs -f`
2. Consultar documentação: `README.md`
3. Verificar issues: GitHub Issues
4. Contato: [seu-email@example.com]

---

**Status**: ✅ Documentação atualizada em 18/02/2026

**Última revisão**: Deploy com Docker Compose, GitHub Actions CI/CD, e testes em português

