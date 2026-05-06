# Store Application

A Spring Boot + MyBatis application with MySQL.

## Requirements

- Java 21
- Maven
- MySQL 8.0+

## Setup

1. Create MySQL database:
```sql
CREATE DATABASE example_db;
```

2. Update `application.yml` with your MySQL credentials:
```yaml
spring:
  datasource:
    username: your_username
    password: your_password
```

3. Run the application:
```bash
mvn spring-boot:run
```

## API Endpoints

- GET `/api/users` - Get all users
- GET `/api/users/{id}` - Get user by ID
- POST `/api/users` - Create new user
- PUT `/api/users/{id}` - Update user
- DELETE `/api/users/{id}` - Delete user

## Project Structure

```
src/main/java/org/sxk/store/
├── StoreApplication.java    # Main application class
├── controller/              # REST controllers
├── service/                 # Business logic
│   └── impl/               # Service implementations
├── mapper/                  # MyBatis mappers
└── entity/                 # Database entities

src/main/resources/
├── application.yml          # Application configuration
├── mapper/                 # MyBatis XML mappings
└── schema.sql              # Database schema
```