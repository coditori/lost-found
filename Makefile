.PHONY: run stop clean test deploy help clean-network

# Default target
help:
	@echo "Available targets:"
	@echo "  run      - Standard production workflow: run tests, then build and deploy if tests pass"
	@echo "  deploy   - Build and run the application with Docker (without running tests)"
	@echo "  stop     - Stop the application containers"
	@echo "  clean    - Stop the application and remove containers, volumes, and builds"
	@echo "  clean-network - Clean up Docker networks to fix network-related issues"
	@echo "  test     - Run tests with H2 database"
	@echo "  help     - Show this help message"

# Clean up Docker networks to fix network issues
clean-network:
	@echo "Cleaning up Docker networks..."
	-docker-compose down --remove-orphans
	-docker network prune -f

# Production workflow: run tests, then build and deploy if tests pass
run: clean-network
	@echo "Running tests..."
	docker pull maven:3.9-eclipse-temurin-21
	if docker run --rm -v "$(shell pwd)":/app -v maven-repo:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn test -Dspring.profiles.active=test; then \
		echo "\nTests passed successfully! Building and deploying application..."; \
		docker pull mysql:8.0; \
		docker-compose down -v; \
		docker-compose up -d --build; \
	else \
		echo "\nTests failed. Application will not be deployed."; \
		exit 1; \
	fi
	@echo "Application started at http://localhost:9095"
	@echo "Use 'docker-compose logs -f app' to view logs"
	@echo "Use 'make stop' to stop the application"

# Build and run the application with Docker (without running tests)
deploy: clean-network
	@echo "Building and deploying application..."
	docker pull mysql:8.0
	docker-compose down -v
	docker-compose up -d --build
	@echo "Application started at http://localhost:9095"
	@echo "Use 'docker-compose logs -f app' to view logs"
	@echo "Use 'make stop' to stop the application"

# Stop application
stop: clean-network
	@echo "Stopping Docker containers..."
	docker-compose down

# Clean application
clean: clean-network
	@echo "Cleaning up Docker containers, volumes, and builds..."
	docker-compose down -v
	docker system prune -f --filter "label=com.example.lostfound=true"
	@echo "To also remove the Maven repository cache, run: docker volume rm maven-repo"

# Run tests with H2 database
test:
	@echo "Running tests with H2 database..."
	docker pull maven:3.9-eclipse-temurin-21
	docker run --rm -v "$(shell pwd)":/app -v maven-repo:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn test -Dspring.profiles.active=test 