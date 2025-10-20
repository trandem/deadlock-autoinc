#!/bin/bash

# Car Park API Startup Script
# This script builds and starts the entire application using Docker Compose

set -e

echo "=========================================="
echo "Car Park Availability API - Startup"
echo "=========================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! docker-compose --version > /dev/null 2>&1; then
    echo "❌ Error: Docker Compose is not installed."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Stop existing containers if any
echo "🧹 Cleaning up existing containers..."
docker-compose down -v 2>/dev/null || true
echo ""

# Build and start services
echo "🏗️  Building and starting services..."
docker-compose up --build -d

# Wait for services to be healthy
echo ""
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check MySQL health
echo "   Checking MySQL..."
until docker-compose exec -T mysql mysqladmin ping -h localhost -u root -prootpassword --silent 2>/dev/null; do
    echo "   MySQL is unavailable - sleeping"
    sleep 2
done
echo "   ✅ MySQL is ready"

# Check application health
echo "   Checking Application..."
max_attempts=30
attempt=0
until curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; do
    attempt=$((attempt+1))
    if [ $attempt -eq $max_attempts ]; then
        echo "   ❌ Application failed to start after ${max_attempts} attempts"
        echo ""
        echo "📋 Application logs:"
        docker-compose logs app
        exit 1
    fi
    echo "   Application is starting... (attempt $attempt/$max_attempts)"
    sleep 2
done
echo "   ✅ Application is ready"
echo ""

# Sync availability data
echo "📡 Syncing car park availability data..."
sleep 2
response=$(curl -s -X POST http://localhost:8080/admin/sync-availability)
echo "   Response: $response"
echo ""

# Success message
echo "=========================================="
echo "✅ Application is running!"
echo "=========================================="
echo ""
echo "🌐 API Endpoint: http://localhost:8080"
echo "🏥 Health Check: http://localhost:8080/actuator/health"
echo ""
echo "📝 Example API call:"
echo "   curl \"http://localhost:8080/carparks/nearest?latitude=1.37326&longitude=103.897&page=1&per_page=3\""
echo ""
echo "🔧 To sync availability data manually:"
echo "   curl -X POST http://localhost:8080/admin/sync-availability"
echo ""
echo "📊 View logs:"
echo "   docker-compose logs -f app"
echo ""
echo "🛑 To stop the application:"
echo "   docker-compose down"
echo ""
