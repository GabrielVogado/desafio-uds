# 📋 Estrutura de Arquivos e Propósito de Cada Um

## Documentação Principal

### README.md
**✅ ARQUIVO PRINCIPAL - Leia Este Primeiro**
- Características completas do projeto
- Todos os 14 endpoints com exemplos curl
- Fluxo de uso passo a passo
- Quick start com Docker
- Troubleshooting e suporte

**Quando usar:** Para entender como usar o sistema e chamar os endpoints

---

### ARCHITECTURE.md
**Visão geral da arquitetura do projeto**
- Estrutura de pastas e arquivos
- Diagrama de comunicação entre frontend e backend

---

### DEPLOYMENT.md
**Instruções de Deploy em Produção**
- Docker push para registry
- Nginx como reverse proxy
- SSL/HTTPS com Let's Encrypt
- Variáveis de ambiente de produção
- Backup e restore do banco
- Monitoramento e logging

**Quando usar:** Quando for deploiar a aplicação em produção

---

## Backend (Java/Spring Boot)

### Estrutura
```
src/main/java/br/com/gabrielvogado/desafiouds/
├── DesafioUdsApplication.java           # Main da aplicação
├── config/
│   ├── SecurityConfig.java              # Spring Security + JWT
│   └── WebMvcConfig.java                # CORS e configurações web
├── controller/
│   ├── AuthController.java              # POST /auth/login, register
│   ├── DocumentController.java          # CRUD /documents
│   └── FileVersionController.java       # Upload/download /versions
├── dto/
│   ├── AuthRequest.java                 # { username, password }
│   ├── AuthResponse.java                # { token, username, email, role }
│   ├── DocumentCreateRequest.java       # { title, description, tags }
│   ├── DocumentDTO.java                 # Resposta de documento
│   └── FileVersionDTO.java              # Resposta de versão
├── exception/
│   ├── CustomException.java             # Base para exceções
│   ├── DocumentNotFoundException.java
│   ├── UnauthorizedException.java
│   ├── AuthenticationException.java
│   ├── UserAlreadyExistsException.java
│   ├── InvalidFileException.java
│   └── GlobalExceptionHandler.java      # Handler centralizado
├── model/
│   ├── User.java                        # JPA entity @Entity
│   ├── Document.java                    # JPA entity com Enum status
│   └── FileVersion.java                 # JPA entity versionamento
├── repository/
│   ├── UserRepository.java              # Spring Data JPA
│   ├── DocumentRepository.java          # Queries customizadas
│   └── FileVersionRepository.java
├── security/
│   ├── JwtTokenProvider.java            # Geração/validação JWT
│   ├── JwtAuthenticationFilter.java     # Filter para JWT
│   ├── CustomUserDetails.java           # UserDetails do Spring
│   └── CustomUserDetailsService.java    # Carrega usuário do DB
└── service/
    ├── AuthService.java                 # Login, register, validações
    ├── DocumentService.java             # CRUD docs, filtros, paginação
    └── FileService.java                 # Upload, download, versionamento

src/resources/
├── application.properties                # Dev (local)
├── application-dev.properties            # Perfil dev
├── application-prod.properties           # Perfil prod
└── db/
    └── migration/
        └── V1__initial_schema.sql        # Flyway - cria tabelas

src/test/java/
├── AuthServiceTest.java                 # Testes login/register
├── DocumentServiceTest.java             # Testes CRUD docs
└── FileServiceTest.java                 # Testes upload/download
```

### pom.xml
**Dependências Maven:**
- spring-boot-starter-web (REST)
- spring-boot-starter-security (JWT)
- spring-boot-starter-data-jpa (Hibernate)
- postgresql (driver)
- jjwt (JWT)
- lombok (gerador getters/setters)
- mockito (testes)

---

## Frontend (Angular)

### Estrutura
```
frontend/
├── package.json                          # Deps: @angular, rxjs, etc
├── angular.json                          # Configuração Angular
├── tsconfig.json                         # TypeScript config
├── karma.conf.js                         # Jasmine/Karma test runner
└── src/
    ├── main.ts                           # Bootstrap da app
    ├── index.html                        # HTML raiz
    ├── styles.css                        # CSS global
    └── app/
        ├── app.component.ts              # Nav bar, outlet
        ├── app-routing.ts                # Rotas da aplicação
        ├── auth/
        │   └── login/
        │       └── login.component.ts    # Form login/register
        ├── dashboard/
        │   └── dashboard.component.ts    # Home page
        ├── documents/
        │   ├── document-list/
        │   │   └── document-list.component.ts    # Tabela paginada
        │   ├── document-detail/
        │   │   └── document-detail.component.ts  # View + upload
        │   └── document-edit/
        │       └── document-edit.component.ts    # Criar/editar
        ├── guards/
        │   └── auth.guard.ts             # Protege rotas autenticadas
        ├── interceptors/
        │   └── jwt.interceptor.ts        # Adiciona Authorization header
        └── services/
            ├── auth.service.ts           # Login, register, token
            └── document.service.ts       # CRUD docs, upload, download
```

### Fluxo de Comunicação

```
1. Login
   └─ LoginComponent.login()
      └─ AuthService.login(username, password)
         └─ HttpClient.post('/auth/login')
            └─ Backend valida credenciais
               └─ Retorna { token, username, email, role }
                  └─ localStorage.setItem('auth_token', token)
                     └─ Router.navigate(['/documents'])

2. Listar Documentos
   └─ DocumentListComponent.loadDocuments()
      └─ DocumentService.listDocuments(page, size, title, status)
         └─ HttpClient.get('/documents?page=0&size=10...')
            └─ JwtInterceptor adiciona header: Authorization: Bearer <token>
               └─ Backend valida JWT
                  └─ Retorna Page<DocumentDTO>
                     └─ Renderiza tabela com paginação

3. Upload de Arquivo
   └─ DocumentDetailComponent.uploadFile()
      └─ DocumentService.uploadFile(docId, file)
         └─ HttpClient.post('/documents/{id}/versions/upload', formData)
            └─ JwtInterceptor adiciona Authorization header
               └─ Backend recebe multipart
                  └─ Salva arquivo em /uploads/{uuid}
                     └─ Cria FileVersion no DB
                        └─ Retorna FileVersionDTO
                           └─ Recarrega versões na lista
```

---

## DevOps

### docker-compose.yml
```yaml
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: desafio_uds
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    build: .                              # Dockerfile multi-stage
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/desafio_uds
    ports:
      - "8080:8080"
    depends_on:
      - postgres

volumes:
  postgres_data:
```

### Dockerfile
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ src/
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### .github/workflows/ci.yml
```yaml
name: CI/CD

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: mvn clean package
      - run: cd frontend && npm install && npm test
```

---

## Banco de Dados

### Schema (Flyway V1)

```sql
-- Users
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Documents
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    owner_id INTEGER NOT NULL REFERENCES users(id),
    tags TEXT[],  -- Array de tags
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP  -- Soft delete
);

-- File Versions
CREATE TABLE file_versions (
    id SERIAL PRIMARY KEY,
    document_id INTEGER NOT NULL REFERENCES documents(id),
    file_name VARCHAR(255),
    content_type VARCHAR(50),
    file_size BIGINT,
    file_key VARCHAR(255),  -- Path no storage
    uploaded_by_id INTEGER NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_documents_owner_id ON documents(owner_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_deleted_at ON documents(deleted_at);
CREATE INDEX idx_file_versions_document_id ON file_versions(document_id);
```

---

## Variáveis de Ambiente

### Development (.env)
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/desafio_uds
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
JWT_SECRET=dev-secret-key-change-in-prod
JWT_EXPIRATION=86400000
UPLOAD_DIR=./uploads
MAX_FILE_SIZE=10485760
ANGULAR_API_URL=http://localhost:8080/api
```

### Production (.env.prod)
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/desafio_uds
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${SECRET_KEY}  # Mínimo 256 bits
JWT_EXPIRATION=3600000    # 1 hora
UPLOAD_DIR=/var/uploads   # Volume persistente
MAX_FILE_SIZE=52428800    # 50 MB
ANGULAR_API_URL=https://api.yourdomain.com
```

---

## Fluxo de Build e Deploy

```
1. Local Development
   ├─ Backend: mvn spring-boot:run
   └─ Frontend: npm start

2. Docker Local
   ├─ docker-compose up --build
   └─ Tudo pronto em http://localhost:4200

3. CI/CD (GitHub Actions)
   ├─ Push → GitHub
   ├─ Workflow executa:
   │  ├─ mvn clean package
   │  ├─ npm test
   │  └─ docker build
   └─ Docker image pushed to registry

4. Production Deployment
   ├─ Pull latest docker image
   ├─ docker-compose -f docker-compose.prod.yml up -d
   ├─ Nginx reverse proxy → backend:8080
   ├─ HTTPS com SSL certificado
   └─ Persistência de dados em volumes
```

---

## ✅ Checklist de Funcionalidades

- [x] Autenticação JWT
- [x] Login/Register
- [x] Criar documento
- [x] Listar com paginação
- [x] Filtrar por título e status
- [x] Ordenar por campo customizado
- [x] Visualizar documento detalhado
- [x] Editar metadados
- [x] Deletar documento (soft delete)
- [x] Mudar status (DRAFT → PUBLISHED → ARCHIVED)
- [x] Upload de arquivo
- [x] Versionamento automático
- [x] Download de arquivo
- [x] Deletar versão específica
- [x] Testes unitários
- [x] Documentação README
- [x] Docker Compose
- [x] CI/CD com GitHub Actions
- [x] CORS configurado
- [x] Tratamento de erros global
- [x] Validação de entrada
- [x] Role-based access control

---

## 🎓 Para Novos Desenvolvedores

1. **Leia primeiro:** README.md (20 min)
2. **Configure local:** `docker-compose up` (5 min)
3. **Faça um teste:** Login e crie um documento (5 min)
4. **Explore o código:**
   - Backend: `src/main/java/...`
   - Frontend: `frontend/src/app/`
5. **Execute testes:** `mvn test` e `npm test`

---

**Última atualização:** Fevereiro 2026
**Versão:** 1.0.0

