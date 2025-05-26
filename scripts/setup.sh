#!/bin/bash

# Lost & Found Service Setup Script
echo "🚀 Setting up Lost & Found Service..."

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p uploads
mkdir -p logs
mkdir -p sample-data

# Check if Java 21 is installed
echo "☕ Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo "✅ Java $JAVA_VERSION detected"
    else
        echo "❌ Java 21 or higher is required. Current version: $JAVA_VERSION"
        exit 1
    fi
else
    echo "❌ Java is not installed. Please install Java 21 or higher."
    exit 1
fi

# Check if Maven is installed
echo "🔨 Checking Maven..."
if command -v mvn &> /dev/null; then
    echo "✅ Maven is installed"
else
    echo "❌ Maven is not installed. Please install Maven 3.6+."
    exit 1
fi

# Check if Docker is available (optional)
echo "🐳 Checking Docker (optional)..."
if command -v docker &> /dev/null; then
    echo "✅ Docker is available"
    DOCKER_AVAILABLE=true
else
    echo "⚠️  Docker is not available. You can still run the application without Docker."
    DOCKER_AVAILABLE=false
fi

# Build the application
echo "🔨 Building the application..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi

# Offer setup options
echo ""
echo "🎯 Choose how to run the application:"
echo "1. Run with H2 (in-memory database) - Quick start"
echo "2. Run with Docker Compose (MySQL) - Production-like"
echo "3. Setup MySQL manually"

read -p "Enter your choice (1-3): " choice

case $choice in
    1)
        echo "🚀 Starting application with H2 database..."
        echo "The application will be available at: http://localhost:8080"
        echo "Swagger UI: http://localhost:8080/swagger-ui.html"
        echo ""
        mvn spring-boot:run
        ;;
    2)
        if [ "$DOCKER_AVAILABLE" = true ]; then
            echo "🐳 Starting application with Docker Compose..."
            docker-compose up --build
        else
            echo "❌ Docker is not available. Please install Docker first."
            exit 1
        fi
        ;;
    3)
        echo "📋 Manual MySQL setup instructions:"
        echo "1. Install MySQL 8.0+"
        echo "2. Create database and user:"
        echo "   CREATE DATABASE lostfound_db;"
        echo "   CREATE USER 'lostfound_user'@'localhost' IDENTIFIED BY 'lostfound_password';"
        echo "   GRANT ALL PRIVILEGES ON lostfound_db.* TO 'lostfound_user'@'localhost';"
        echo "   FLUSH PRIVILEGES;"
        echo "3. Uncomment MySQL configuration in src/main/resources/application.yml"
        echo "4. Run: mvn spring-boot:run"
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "🎉 Setup complete!"
echo ""
echo "📖 Quick start guide:"
echo "1. Register an admin user: POST /api/auth/register"
echo "2. Login to get JWT token: POST /api/auth/login"
echo "3. Upload PDF file: POST /api/admin/upload"
echo "4. Browse items: GET /api/user/items"
echo ""
echo "📚 For detailed documentation, visit: http://localhost:8080/swagger-ui.html" 