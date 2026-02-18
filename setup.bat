@echo off
REM Script to initialize the project on Windows

echo.
echo === GED - Gestão Eletrônica de Documentos ===
echo.
echo Inicializando o ambiente de desenvolvimento...
echo.

REM Check prerequisites
echo Verificando pré-requisitos...

where docker >nul 2>nul
if errorlevel 1 (
    echo ❌ Docker nao esta instalado
    exit /b 1
)

where docker-compose >nul 2>nul
if errorlevel 1 (
    echo ⚠️  Docker Compose nao esta instalado
    exit /b 1
)

echo ✓ Docker esta instalado

REM Create uploads directory if it doesn't exist
if not exist "uploads" (
    echo Criando diretorio de uploads...
    mkdir uploads
)

REM Copy .env.example to .env if it doesn't exist
if not exist ".env" (
    echo Criando arquivo .env...
    copy .env.example .env
    echo ⚠️  Edite o arquivo .env com suas configuracoes
)

REM Build and start containers
echo.
echo Iniciando containers...
docker-compose up -d

echo.
echo ✓ Ambiente iniciado com sucesso!
echo.
echo Recursos disponiveis:
echo   • Backend: http://localhost:8080/api
echo   • Frontend: http://localhost:4200 (apos npm install ^&^& npm start em frontend\)
echo   • PostgreSQL: localhost:5432
echo.
echo Proximos passos:
echo   1. Aguarde as migrations do Flyway executarem (~30 segundos)
echo   2. Registre um novo usuario: POST /api/auth/register
echo   3. Acesse o frontend em http://localhost:4200
echo.
echo Para ver os logs:
echo   docker-compose logs -f backend
echo.
echo Para parar os containers:
echo   docker-compose down
echo.

