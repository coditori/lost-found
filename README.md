# Lost & Found Service

A comprehensive Lost & Found management system built with Spring Boot 3, featuring file upload capabilities, basic authentication, and role-based access control.

## Features

- 🔐 **Basic Authentication & Authorization** - Role-based access control (USER/ADMIN)
- 📄 **PDF File Processing** - Upload and parse PDF files containing lost item records
- 🔍 **Advanced Search** - Search lost items by name and location
- 🎯 **Claim Management** - Users can claim items with concurrency control
- 📊 **Admin Dashboard** - Administrative functions for managing claims and items
- 🔄 **Retry Mechanism** - Handles optimistic locking failures automatically
- 📚 **API Documentation** - Interactive Swagger UI documentation
- 🛡️ **Exception Handling** - Comprehensive error handling and validation

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.0**
- **Spring Security** - Basic authentication
- **Spring Data JPA** - Data persistence
- **MySQL** - Primary database
- **H2** - Testing database
- **Apache PDFBox** - PDF processing
- **Springdoc OpenAPI** - API documentation
- **Spring Retry** - Retry mechanism
- **Lombok** - Reduced boilerplate code
- **Maven** - Dependency management

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- MySQL 8.0+ (for production)
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd lostfound
   ```

2. **Configure Database**
   
   For development with MySQL:
   ```bash
   # Create database
   mysql -u root -p
   CREATE DATABASE lostfound_db;
   CREATE USER 'lostfound_user'@'localhost' IDENTIFIED BY 'lostfound_password';
   GRANT ALL PRIVILEGES ON lostfound_db.* TO 'lostfound_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

   For quick testing with H2 (current default configuration):
   - The application is pre-configured to use H2 in-memory database
   - No additional setup required

3. **Build the application**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

### Configuration

#### Environment Variables

You can override default configurations using environment variables:

```bash
# Database Configuration (for MySQL)
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password

# Admin Configuration
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=admin123

# File Upload
export UPLOAD_DIR=./uploads
```

#### Application Profiles

- **Default (H2)**: Ready to run with in-memory database
- **MySQL**: Uncomment MySQL configuration in `application.yml`

## API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## API Endpoints

### User Endpoints
- `GET /api/user/profile` - Get user profile
- `GET /api/user/items` - Browse available items
- `GET /api/user/items/search` - Search items
- `GET /api/user/items/{id}` - Get item details
- `POST /api/user/claims` - Create a claim
- `GET /api/user/claims` - Get user's claims
- `GET /api/user/claims/{id}` - Get claim details

### Admin Endpoints
- `POST /api/admin/upload` - Upload PDF file with lost items
- `GET /api/admin/claims` - Get all claims
- `GET /api/admin/claims/status/{status}` - Get claims by status
- `PUT /api/admin/claims/{id}/status` - Update claim status
- `GET /api/admin/items` - Get all items
- `GET /api/admin/stats` - Get system statistics

## Usage Examples

### 1. Browse Items (with basic authentication)

```bash
curl -X GET http://localhost:8080/api/user/items \
  -u username:password
```

### 2. Upload PDF File (Admin only)

```bash
curl -X POST http://localhost:8080/api/admin/upload \
  -u admin:admin123 \
  -F "file=@lost_items.pdf"
```

### 3. Create a Claim

```bash
curl -X POST http://localhost:8080/api/user/claims \
  -u username:password \
  -H "Content-Type: application/json" \
  -d '{
    "lostItemId": 1,
    "claimedQuantity": 2,
    "notes": "I think this is mine"
  }'
```

## PDF File Format

The system supports PDF files with lost item records in the following formats:

### Format 1: Structured Format
```
Item Name: Laptop
Quantity: 1
Place: Library

Item Name: USB Drive
Quantity: 3
Place: Computer Lab
```

### Format 2: Comma-Separated Format
```
Laptop, 1, Library
USB Drive, 3, Computer Lab
Water Bottle, 2, Cafeteria
```

## Default Users

The application uses basic authentication. You can create users through the database or use the default admin credentials:

- **Username**: admin
- **Password**: admin123
- **Role**: ADMIN

## Future Enhancements

- **JWT Authentication**: Planned implementation for stateless authentication
- **OAuth2 Integration**: Social login capabilities
- **Real-time Notifications**: WebSocket-based notifications
- **Mobile App**: React Native mobile application

## Development

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package -Pprod
```

### Code Quality

The project includes:
- Comprehensive exception handling
- Input validation
- Logging with SLF4J
- Transaction management
- Optimistic locking for concurrency control

## Monitoring and Health Checks

The application includes Actuator endpoints:

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Info**: http://localhost:8080/actuator/info

## Security Features

- **Basic Authentication**
- **Role-based Access Control**
- **Password Encryption** with BCrypt
- **Input Validation**
- **SQL Injection Prevention**
- **CORS Configuration**

## Error Handling

The application provides comprehensive error handling with:
- Standardized error responses
- Validation error details
- Appropriate HTTP status codes
- Detailed logging for debugging

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:
- Email: support@lostfound.example.com
- Documentation: http://localhost:8080/swagger-ui.html 