# GED - Sistema de Gestão Eletrônica de Documentos

Um sistema completo de gestão eletrônica de documentos (GED) com suporte a upload, versionamento, pesquisa e controle de acesso. Desenvolvido com Java Spring Boot no backend e Angular no frontend.

## 📋 Índice

- [Características](#características)
- [Arquitetura](#arquitetura)
- [Requisitos](#requisitos)
- [Instalação e Execução](#instalação-e-execução)
- [API Endpoints](#api-endpoints)
- [Autenticação](#autenticação)
- [Fluxo de Uso](#fluxo-de-uso)
- [Decisões Técnicas](#decisões-técnicas)
- [Limitações](#limitações)

## 🎯 Características

### Funcionais (MVP)
- ✅ **Autenticação JWT** - Login e registro de usuários
- ✅ **Perfis de Acesso** - ADMIN e USER com permissões diferenciadas
- ✅ **Gestão de Documentos** - Criar, editar, visualizar, deletar
- ✅ **Metadados** - Título, descrição, tags, status (DRAFT, PUBLISHED, ARCHIVED)
- ✅ **Upload de Arquivos** - PDF, PNG, JPEG
- ✅ **Versionamento** - Histórico automático de versões com metadados
- ✅ **Busca e Filtros** - Por título e status com paginação
- ✅ **Download** - De versões específicas de arquivos
- ✅ **Timestamps** - Criação e atualização automáticas

### Não Funcionais
- ✅ **Docker Compose** - Ambiente completo (backend + PostgreSQL)
- ✅ **PostgreSQL** - Banco de dados relacional com migrations
- ✅ **Testes Unitários** - Mínimo 3 testes por serviço
- ✅ **CI/CD** - GitHub Actions com build e testes
- ✅ **Frontend Angular** - Interface funcional e responsiva
- ✅ **Documentação** - README com instruções e decisões técnicas

## 🏗️ Arquitetura

### Backend (Java/Spring Boot)
```
src/main/java/br/com/gabrielvogado/desafiouds/
├── config/              # Configuração de segurança e CORS
├── controller/          # REST endpoints (Auth, Document, FileVersion)
├── dto/                 # Data Transfer Objects (request/response)
├── exception/           # Tratamento centralizado de exceções
├── model/               # Entidades JPA (User, Document, FileVersion)
├── repository/          # JPA repositories
├── security/            # JWT e autenticação
├── service/             # Lógica de negócio
└── DesafioUdsApplication.java
```

### Frontend (Angular)
```
frontend/src/app/
├── auth/                # Login e registro
├── dashboard/           # Página inicial
├── documents/           # CRUD de documentos
│   ├── document-list/   # Listagem com paginação
│   ├── document-detail/ # Visualização e upload
│   └── document-edit/   # Criação e edição
├── guards/              # Auth guard para rotas
├── interceptors/        # JWT interceptor
├── services/            # Chamadas HTTP
└── app-routing.ts       # Rotas
```

### Banco de Dados
```
users
├── id (PK)
├── username (UNIQUE)
├── email (UNIQUE)
├── password_hash
├── role (ADMIN/USER)
└── created_at

documents
├── id (PK)
├── title
├── description
├── status (DRAFT/PUBLISHED/ARCHIVED)
├── owner_id (FK -> users)
├── tags (JSON/Array)
├── created_at
├── updated_at
└── deleted_at

file_versions
├── id (PK)
├── document_id (FK -> documents)
├── file_name
├── content_type
├── file_size
├── file_key (storage reference)
├── uploaded_by_id (FK -> users)
└── uploaded_at
```

## 📦 Requisitos

- **Docker** e **Docker Compose** (recomendado)
- Ou:
  - **Java 17+**
  - **Maven 3.9+**
  - **Node.js 18+** (para frontend)
  - **PostgreSQL 15+**

## 🚀 Instalação e Execução

### Opção 1: Docker Compose (Recomendado)

```bash
# Clonar o repositório
git clone <seu-repositorio>
cd desafio-uds

# Subir ambiente completo
docker-compose up --build

# Sistema estará disponível em:
# Backend: http://localhost:8080/api
# Frontend: http://localhost:4200
```

### Opção 2: Execução Local

#### 1. Setup do Banco de Dados

```bash
# PostgreSQL deve estar rodando
createdb desafio_uds

# Migrations são executadas automaticamente pelo Flyway
```

#### 2. Backend

```bash
cd desafio-uds
mvn clean install
mvn spring-boot:run

# Ou
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Backend em: http://localhost:8080/api
```

#### 3. Frontend

```bash
cd frontend
npm install
npm start

# Frontend em: http://localhost:4200
```

## 🔐 Autenticação

### JWT (JSON Web Token)

Todos os endpoints exceto `/auth/login` e `/auth/register` requerem token JWT no header:

```
Authorization: Bearer <token>
```

### Credenciais Padrão (pode criar novas)

```
Username: admin
Email: admin@example.com
Password: admin123
Role: ADMIN
```

## 📡 API Endpoints

### 1. Autenticação

#### Login
```bash
curl --location 'http://localhost:8080/api/auth/login' \
--header 'Content-Type: application/json' \
--data '{
  "username": "admin",
  "password": "admin123"
}'
```

**Response (201):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "email": "admin@example.com",
  "role": "ADMIN"
}
```

#### Registro
```bash
curl --location 'http://localhost:8080/api/auth/register' \
--header 'Content-Type: application/json' \
--data '{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123"
}'
```

**Response (201):** Mesmo formato do login

#### Health Check
```bash
curl --location 'http://localhost:8080/api/auth/health'
```

---

### 2. Documentos

#### Criar Documento
```bash
curl --location 'http://localhost:8080/api/documents' \
--header 'Authorization: Bearer <token>' \
--header 'Content-Type: application/json' \
--data '{
  "title": "Contrato de Serviços",
  "description": "Contrato entre partes A e B",
  "tags": ["contrato", "2026", "importante"]
}'
```

**Response (201):**
```json
{
  "id": 1,
  "title": "Contrato de Serviços",
  "description": "Contrato entre partes A e B",
  "tags": ["contrato", "2026", "importante"],
  "ownerUsername": "admin",
  "status": "DRAFT",
  "createdAt": "2026-02-18T10:30:00Z",
  "updatedAt": "2026-02-18T10:30:00Z"
}
```

#### Listar Documentos com Paginação
```bash
# Todos os documentos
curl --location 'http://localhost:8080/api/documents?page=0&size=10' \
--header 'Authorization: Bearer <token>'

# Com filtros
curl --location 'http://localhost:8080/api/documents?page=0&size=10&title=Contrato&status=PUBLISHED&sortBy=createdAt&direction=DESC' \
--header 'Authorization: Bearer <token>'
```

**Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Contrato de Serviços",
      "description": "Contrato entre partes A e B",
      "tags": ["contrato"],
      "ownerUsername": "admin",
      "status": "PUBLISHED",
      "createdAt": "2026-02-18T10:30:00Z",
      "updatedAt": "2026-02-18T10:35:00Z"
    }
  ],
  "totalPages": 1,
  "totalElements": 1,
  "size": 10,
  "number": 0
}
```

#### Obter Documento por ID
```bash
curl --location 'http://localhost:8080/api/documents/1' \
--header 'Authorization: Bearer <token>'
```

**Response (200):** Documento individual em JSON

#### Atualizar Documento
```bash
curl --location --request PUT 'http://localhost:8080/api/documents/1' \
--header 'Authorization: Bearer <token>' \
--header 'Content-Type: application/json' \
--data '{
  "title": "Contrato Atualizado",
  "description": "Nova descrição",
  "tags": ["contrato", "2026"]
}'
```

**Response (200):** Documento atualizado

#### Deletar Documento
```bash
curl --location --request DELETE 'http://localhost:8080/api/documents/1' \
--header 'Authorization: Bearer <token>'
```

**Response (204):** Sem conteúdo (sucesso)

#### Alterar Status do Documento
```bash
curl --location --request PUT 'http://localhost:8080/api/documents/1/status?status=PUBLISHED' \
--header 'Authorization: Bearer <token>'
```

**Status válidos:** `DRAFT`, `PUBLISHED`, `ARCHIVED`

**Response (200):** Documento com novo status

---

### 3. Versionamento de Arquivos

#### Upload de Arquivo
```bash
curl --location --request POST 'http://localhost:8080/api/documents/1/versions/upload' \
--header 'Authorization: Bearer <token>' \
--form 'file=@"/caminho/para/arquivo.pdf"'
```

**Formatos aceitos:** PDF, PNG, JPG, JPEG
**Tamanho máximo:** 10 MB (configurável)

**Response (201):**
```json
{
  "id": 1,
  "documentId": 1,
  "fileName": "arquivo.pdf",
  "contentType": "application/pdf",
  "fileSize": 2048,
  "uploadedByUsername": "admin",
  "uploadedAt": "2026-02-18T10:40:00Z"
}
```

#### Listar Versões do Documento
```bash
curl --location 'http://localhost:8080/api/documents/1/versions' \
--header 'Authorization: Bearer <token>'
```

**Response (200):**
```json
[
  {
    "id": 1,
    "documentId": 1,
    "fileName": "arquivo.pdf",
    "contentType": "application/pdf",
    "fileSize": 2048,
    "uploadedByUsername": "admin",
    "uploadedAt": "2026-02-18T10:40:00Z"
  },
  {
    "id": 2,
    "documentId": 1,
    "fileName": "arquivo_v2.pdf",
    "contentType": "application/pdf",
    "fileSize": 2100,
    "uploadedByUsername": "admin",
    "uploadedAt": "2026-02-18T10:50:00Z"
  }
]
```

#### Obter Versão Mais Recente
```bash
curl --location 'http://localhost:8080/api/documents/1/versions/latest' \
--header 'Authorization: Bearer <token>'
```

**Response (200):** Versão mais recente em JSON

#### Download de Arquivo
```bash
curl --location 'http://localhost:8080/api/documents/versions/1/download' \
--header 'Authorization: Bearer <token>' \
--output arquivo.pdf
```

**Response (200):** Arquivo em binary (blob)

#### Deletar Versão
```bash
curl --location --request DELETE 'http://localhost:8080/api/documents/versions/1' \
--header 'Authorization: Bearer <token>'
```

**Response (204):** Sem conteúdo (sucesso)

---

## 🔄 Fluxo de Uso Completo

### 1. Autenticar
```bash
TOKEN=$(curl -s -X POST 'http://localhost:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' | jq -r '.token')

echo "Token: $TOKEN"
```

### 2. Criar Documento
```bash
DOC_ID=$(curl -s -X POST 'http://localhost:8080/api/documents' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Meu Documento",
    "description": "Descrição do documento",
    "tags": ["importante"]
  }' | jq -r '.id')

echo "Documento criado: $DOC_ID"
```

### 3. Fazer Upload de Arquivo
```bash
curl -X POST "http://localhost:8080/api/documents/$DOC_ID/versions/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/caminho/para/arquivo.pdf"
```

### 4. Publicar Documento
```bash
curl -X PUT "http://localhost:8080/api/documents/$DOC_ID/status?status=PUBLISHED" \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Listar Documentos
```bash
curl -X GET 'http://localhost:8080/api/documents?page=0&size=10' \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 6. Download de Arquivo
```bash
# Primeiro obter ID da versão
VERSION_ID=$(curl -s -X GET "http://localhost:8080/api/documents/$DOC_ID/versions" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# Depois fazer download
curl -X GET "http://localhost:8080/api/documents/versions/$VERSION_ID/download" \
  -H "Authorization: Bearer $TOKEN" \
  -o documento_baixado.pdf
```

---

## 🛡️ Permissões por Perfil

| Operação | USER | ADMIN |
|----------|------|-------|
| Login/Registro | ✅ | ✅ |
| Criar Documento | ✅ | ✅ |
| Listar Próprios Docs | ✅ | ✅ (todos) |
| Editar Próprio Doc | ✅ | ✅ (qualquer) |
| Deletar Próprio Doc | ✅ | ✅ (qualquer) |
| Upload de Arquivo | ✅ | ✅ |
| Download de Arquivo | ✅ | ✅ |
| Mudar Status | ✅ | ✅ |

---

## 🧪 Testes

### Backend (JUnit + Mockito)

```bash
# Executar todos os testes
mvn test

# Executar teste específico
mvn test -Dtest=DocumentServiceTest

# Com cobertura
mvn test jacoco:report
```

**Testes inclusos:**
- `AuthServiceTest` - Login e registro
- `DocumentServiceTest` - CRUD de documentos
- `FileServiceTest` - Upload e download

### Frontend (Jasmine + Karma)

```bash
cd frontend
npm test

# Com cobertura
ng test --code-coverage
```

---

## 🔧 Decisões Técnicas

### 1. **JWT para Autenticação**
- Stateless, escalável e simples
- Adequado para aplicações REST/SPA
- Token armazenado no localStorage do navegador

### 2. **Spring Security 7.0.2 + Interceptor Filter**
- Autenticação centralizada com JwtAuthenticationFilter
- Proteção CSRF desabilitada (API stateless)
- CORS configurado para localhost (desenvolvimento)

### 3. **JPA/Hibernate com PostgreSQL**
- ORM padrão do Spring Data
- Migrations com Flyway (versionamento automático)
- Índices em campos críticos (username, email)

### 4. **Versionamento Automático**
- Cada upload cria nova FileVersion
- Histórico completo preservado
- Download de qualquer versão anterior

### 5. **Soft Delete com Hibernate**
- Campo `deleted_at` para documentos
- Não remove dados, apenas marca como deletado
- Melhor auditoria e compliance

### 6. **DTO Pattern**
- Separação entre camada HTTP e lógica
- Validação centralizada com Jakarta Validation
- Resposta padronizada

### 7. **Frontend Angular Standalone**
- Componentes standalone (sem NgModules)
- Interceptores funcionais (novo padrão Angular 15+)
- Guards de rota com injeção de dependência

---

## ⚠️ Limitações

### Conhecidas
1. **Upload de Arquivos** - Armazenado em disco local (`/uploads`). Para produção, usar S3/Cloud Storage
2. **Concorrência** - Sem otimistic locking. Melhorar com versionamento otimista
3. **Busca** - Apenas full-text simples. Para melhor performance, usar Elasticsearch
4. **Rate Limiting** - Não implementado. Adicionar para proteção contra abuso
5. **Logs** - Básicos. Melhorar com ELK Stack
6. **Segurança** - Senha apenas com bcrypt. Adicionar 2FA em produção

### Funcionalidades Futuras
- [ ] Compartilhamento de documentos entre usuários
- [ ] Assinatura digital de documentos
- [ ] Workflow/aprovação de documentos
- [ ] Integração com sistemas externos
- [ ] Backup automático
- [ ] Auditoria completa de ações
- [ ] Suporte a OCR para PDFs
- [ ] Notificações por email
- [ ] Dashboard com estatísticas
- [ ] API GraphQL

---

## 📊 Estrutura de Configuração

### Application Properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://postgres:5432/desafio_uds
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate

# JWT
jwt.secret=sua_chave_secreta_muito_comprida_aqui
jwt.expiration=86400000  # 24 horas

# Upload
app.upload.max-size=10485760  # 10 MB
app.upload.dir=/uploads

# Server
server.servlet.context-path=/api
server.port=8080
```

---

## 🐛 Troubleshooting

### Erro: Connection refused (PostgreSQL)
```bash
# Verificar se PostgreSQL está rodando
docker ps | grep postgres

# Ou iniciar:
docker-compose up postgres
```

### Erro: 401 Unauthorized
- Token expirado? Fazer login novamente
- Token malformado? Verificar Bearer no header
- Token não enviado? Adicionar `Authorization: Bearer <token>`

### Erro: CORS blocked
- Frontend está em porta diferente?
- Verificar `origins` em `@CrossOrigin` do controller
- Desenvolvimento: `"http://localhost:4200"`

### Erro: File upload fails
- Tipo de arquivo não permitido? Usar PDF, PNG, JPG
- Arquivo muito grande? Máximo 10 MB
- Pasta `/uploads` tem permissão? `chmod 777 uploads`

---

## 📝 Variáveis de Ambiente

Criar arquivo `.env` na raiz:

```env
# Database
DB_NAME=desafio_uds
DB_USER=postgres
DB_PASSWORD=postgres
DB_HOST=postgres
DB_PORT=5432

# Backend
SERVER_PORT=8080
JWT_SECRET=sua_chave_super_secreta_aqui
JWT_EXPIRATION=86400000

# Upload
UPLOAD_DIR=/uploads
MAX_FILE_SIZE=10485760

# Frontend
ANGULAR_API_URL=http://localhost:8080/api
```

---

## 📞 Suporte

Para dúvidas ou problemas:
1. Verificar logs: `docker logs desafio-uds-backend`
2. Verificar erros do browser: DevTools (F12)
3. Testar endpoints com Postman/curl
4. Verificar configurações em `application-dev.properties`

---

## 📄 Licença

MIT License - Veja LICENSE para detalhes

---

**Última atualização:** Fevereiro 2026

