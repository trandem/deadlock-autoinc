
## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Database Design](#database-design)
- [API Documentation](#api-documentation)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)

## Features

✅ Find nearest car parks based on user's GPS coordinates
✅ Real-time parking availability data
✅ Pagination support for large result sets
✅ Input validation and error handling
✅ Distance calculation using Haversine formula
✅ Coordinate conversion from SVY21 to WGS84
✅ Docker containerization for easy deployment
✅ Comprehensive test coverage
✅ Health check endpoints

## Technology Stack

- **Java 17** - Latest LTS version with modern language features
- **Spring Boot 3.5.6** - Rapid application development framework
- **Spring Data JPA** - Data access layer abstraction
- **MySQL 8.0** - Relational database for persistent storage
- **Flyway** - Database migration and versioning
- **Lombok** - Reduces boilerplate code
- **Docker & Docker Compose** - Containerization and orchestration
- **Maven** - Dependency management and build tool
- **JUnit 5 & MockMvc** - Testing framework

## Architecture

The application follows a layered architecture pattern:

```
┌─────────────────────────────────────────┐
│         REST Controller Layer           │  ← Request validation & routing
├─────────────────────────────────────────┤
│           Service Layer                 │  ← Business logic & distance calc
├─────────────────────────────────────────┤
│         Repository Layer                │  ← Data access with spatial queries
├─────────────────────────────────────────┤
│            Database (MySQL)             │  ← Persistent storage
└─────────────────────────────────────────┘
```

## Database Design

### Entity Relationship Diagram

```
┌─────────────────────┐         ┌────────────────────────────┐
│     car_parks       │         │  car_park_availability     │
├─────────────────────┤         ├────────────────────────────┤
│ id (PK)             │────┐    │ id (PK)                    │
│ car_park_no (UQ)    │    └───<│ car_park_id (FK)           │
│ address             │         │ car_park_no                │
│ latitude            │         │ total_lots                 │
│ longitude           │         │ available_lots             │
│ x_coord             │         │ lot_type                   │
│ y_coord             │         │ update_datetime            │
│ car_park_type       │         │ created_at                 │
│ ...                 │         └────────────────────────────┘
│ created_at          │
│ updated_at          │
└─────────────────────┘
```

### Design Rationale

- **Normalized structure** separates static car park info from dynamic availability
- **Indexes** on latitude/longitude and car_park_no for query performance
- **Foreign key** constraint ensures data integrity
- **Unique constraint** prevents duplicate availability records per car park and lot type
## Getting Started

### Prerequisites

- **Docker** and **Docker Compose** (recommended)
- OR **Java 17+** and **MySQL 8.0+** (for local development)
- **Maven 3.9+** (if building manually)

### Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/wego/carpark/
│   │   │   ├── controller/          # REST endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Data access
│   │   │   ├── model/               # JPA entities
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── task/                # Background tasks
│   │   │   ├── util/                # Utility classes
│   │   │   └── CarparkApiApplication.java
│   │   └── resources/
│   │       ├── db/migration/        # Flyway SQL scripts
│   │       ├── HDBCarparkInformation.csv
│   │       └── application.properties
│   └── test/                        # Test classes
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## Running the Application

### Option 1: Using Docker Compose (Recommended)

This is the easiest way to run the application with all dependencies.

```bash
# 1. Clone the repository
git clone <repository-url>
cd demo

# 2. Build and start all services
docker-compose up --build

# 3. Wait for services to start (check logs)
# The app will be available at http://localhost:8080

# 4. Sync availability data (first time)
curl -X POST http://localhost:8080/admin/sync-availability

# 5. Test the API
curl "http://localhost:8080/carparks/nearest?latitude=1.37326&longitude=103.897&page=1&per_page=3"
```

To stop the application:

```bash
docker-compose down
```

To stop and remove volumes (clean slate):

```bash
docker-compose down -v
```
