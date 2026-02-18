#!/bin/bash

# Script to initialize the project

echo "=== GED - Gestão Eletrônica de Documentos ==="
echo ""
echo "Inicializando o ambiente de desenvolvimento..."
echo ""

# Check prerequisites
echo "Verificando pré-requisitos..."

if ! command -v docker &> /dev/null; then
    echo "❌ Docker não está instalado"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "⚠️  Docker Compose não está instalado"
    exit 1
fi

echo "✓ Docker está instalado"

# Create uploads directory if it doesn't exist
if [ ! -d "uploads" ]; then
    echo "Criando diretório de uploads..."
    mkdir -p uploads
fi

# Copy .env.example to .env if it doesn't exist
if [ ! -f ".env" ]; then
    echo "Criando arquivo .env..."
    cp .env.example .env
    echo "⚠️  Edite o arquivo .env com suas configurações"
fi

# Build and start containers
echo ""
echo "Iniciando containers..."
docker-compose up -d

echo ""
echo "✓ Ambiente iniciado com sucesso!"
echo ""
echo "Recursos disponíveis:"
echo "  • Backend: http://localhost:8080/api"
echo "  • Frontend: http://localhost:4200 (após npm install && npm start em frontend/)"
echo "  • PostgreSQL: localhost:5432"
echo ""
echo "Próximos passos:"
echo "  1. Aguarde as migrations do Flyway executarem (~30 segundos)"
echo "  2. Registre um novo usuário: POST /api/auth/register"
echo "  3. Acesse o frontend em http://localhost:4200"
echo ""
echo "Para ver os logs:"
echo "  docker-compose logs -f backend"
echo ""
echo "Para parar os containers:"
echo "  docker-compose down"
echo ""

