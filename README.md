# Car Park Availability API

A Spring Boot REST API that helps users find the nearest car parks with available parking lots in Singapore. The API uses real-time data from Singapore's data.gov.sg and calculates distances using the Haversine formula.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Database Design](#database-design)
- [API Documentation](#api-documentation)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [Design Decisions & Trade-offs](#design-decisions--trade-offs)
- [Future Improvements](#future-improvements)

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

### Key Components

1. **Controllers** (`controller/`)
   - `CarParkController` - Main API endpoint for finding nearest car parks
   - `AdminController` - Administrative operations (data sync)

2. **Services** (`service/`)
   - `CarParkService` - Business logic for finding and ranking car parks

3. **Repositories** (`repository/`)
   - `CarParkRepository` - Custom queries with Haversine distance calculation
   - `CarParkAvailabilityRepository` - Availability data management

4. **Models** (`model/`)
   - `CarPark` - Static car park information entity
   - `CarParkAvailability` - Real-time availability entity

5. **Tasks** (`task/`)
   - `CarParkDataImportTask` - Imports car park data from CSV on startup
   - `CarParkAvailabilitySyncTask` - Syncs availability from external API

6. **Utilities** (`util/`)
   - `DistanceCalculator` - Haversine formula implementation
   - `CoordinateConverter` - SVY21 to WGS84 conversion

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

## API Documentation

### Find Nearest Car Parks

Returns a list of car parks sorted by distance from the given coordinates, filtered to only include those with available parking.

**Endpoint:** `GET /carparks/nearest`

**Query Parameters:**

| Parameter  | Type   | Required | Validation          | Description                    |
|------------|--------|----------|---------------------|--------------------------------|
| latitude   | Double | Yes      | -90 to 90           | User's latitude (WGS84)        |
| longitude  | Double | Yes      | -180 to 180         | User's longitude (WGS84)       |
| page       | Integer| No       | >= 1 (default: 1)   | Page number (1-indexed)        |
| per_page   | Integer| No       | 1-100 (default: 10) | Results per page               |

**Success Response (200 OK):**

```json
[
  {
    "address": "BLK 401-413, 460-463 HOUGANG AVENUE 10",
    "latitude": 1.37429,
    "longitude": 103.896,
    "total_lots": 693,
    "available_lots": 182
  },
  {
    "address": "BLK 351-357 HOUGANG AVENUE 7",
    "latitude": 1.37234,
    "longitude": 103.899,
    "total_lots": 249,
    "available_lots": 143
  }
]
```

**Error Response (400 Bad Request):**

```json
{
  "timestamp": "2025-10-18T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Required parameter 'latitude' is missing",
  "path": "/carparks/nearest"
}
```

**Example Requests:**

```bash
# Basic request
curl "http://localhost:8080/carparks/nearest?latitude=1.37326&longitude=103.897"

# With pagination
curl "http://localhost:8080/carparks/nearest?latitude=1.37326&longitude=103.897&page=1&per_page=3"
```

### Sync Availability Data

Manually trigger a sync of car park availability data from the external API.

**Endpoint:** `POST /admin/sync-availability`

**Success Response (200 OK):**

```json
{
  "status": "success",
  "message": "Car park availability data synced successfully"
}
```

**Example Request:**

```bash
curl -X POST http://localhost:8080/admin/sync-availability
```

### Health Check

**Endpoint:** `GET /actuator/health`

```bash
curl http://localhost:8080/actuator/health
```

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

### Option 2: Local Development

If you want to run the application locally without Docker:

```bash
# 1. Start MySQL database
# Ensure MySQL is running on localhost:3306
# Create database: CREATE DATABASE carpark_db;
# Create user with appropriate privileges

# 2. Update application.properties
# Change datasource URL from 'mysql:3306' to 'localhost:3306'

# 3. Build the application
./mvnw clean package -DskipTests

# 4. Run the application
./mvnw spring-boot:run

# 5. In another terminal, sync availability data
curl -X POST http://localhost:8080/admin/sync-availability
```

## Testing

### Run All Tests

```bash
./mvnw test
```

### Run Specific Test Class

```bash
./mvnw test -Dtest=DistanceCalculatorTest
./mvnw test -Dtest=CarParkControllerIntegrationTest
```

### Test Coverage

The project includes:
- **Unit tests** for utility classes (distance calculation, coordinate conversion)
- **Integration tests** for REST endpoints with MockMvc
- **Repository tests** for custom queries

## Design Decisions & Trade-offs

### 1. Haversine Formula in SQL Query

**Decision:** Calculate distance directly in the database query
**Rationale:**
- Enables sorting by distance at the database level
- Better performance for large datasets
- Avoids loading all records into memory

**Trade-off:** SQL query becomes more complex, less portable across databases

### 2. Coordinate Conversion Approach

**Decision:** Simplified SVY21 to WGS84 conversion
**Rationale:**
- Quick implementation for MVP
- Adequate accuracy for finding nearest car parks

**Trade-off:** Not as accurate as using proper projection libraries or OneMap API
**Future improvement:** Use OneMap API or proj4j library for production

### 3. Pagination Implementation

**Decision:** 1-indexed pagination for API, 0-indexed internally
**Rationale:**
- More intuitive for API consumers (page=1 is first page)
- Follows common REST API conventions

### 4. Availability Data Storage

**Decision:** Separate table with foreign key relationship
**Rationale:**
- Allows multiple lot types per car park (cars, motorcycles, heavy vehicles)
- Historical tracking capabilities (if needed later)
- Updates don't affect static car park data

**Trade-off:** Requires JOIN queries, slightly more complex

### 5. Manual Sync Trigger

**Decision:** Admin endpoint for manual sync, no automatic scheduling
**Rationale:**
- Requirements stated scheduling is out of scope
- Gives operators control over when to sync
- Easier to test and debug

**Future improvement:** Add Spring Scheduler for periodic automatic syncing

### 6. Only Return Car Parks with Availability

**Decision:** Filter WHERE available_lots > 0
**Rationale:**
- Meets business requirements
- Reduces unnecessary data transfer
- Better user experience

### 7. MySQL Instead of PostgreSQL

**Decision:** Use MySQL 8.0
**Rationale:**
- Requirement specified relational database
- MySQL is widely used and well-supported
- PostGIS would be overkill for this use case

**Note:** For production with heavy spatial queries, consider PostgreSQL with PostGIS extension

## Future Improvements

Given more time, here are enhancements I would consider:

### High Priority

1. **Caching Layer**
   - Redis cache for frequently accessed car park data
   - Cache invalidation strategy
   - Would significantly reduce database load

2. **Accurate Coordinate Conversion**
   - Integrate with OneMap API or use proj4j library
   - More accurate SVY21 to WGS84 conversion

3. **Automated Scheduling**
   - Spring Scheduler to sync availability every 5 minutes
   - Configurable sync intervals
   - Better error handling and retry logic

4. **Rate Limiting**
   - Prevent API abuse
   - Use bucket4j or similar

5. **API Documentation**
   - Swagger/OpenAPI integration
   - Interactive API explorer

### Medium Priority

6. **More Comprehensive Tests**
   - Service layer unit tests with mocked repositories
   - Test edge cases (empty results, database errors)
   - Performance tests

7. **Monitoring & Observability**
   - Prometheus metrics
   - Grafana dashboards
   - Distributed tracing (Zipkin/Jaeger)

8. **Security Enhancements**
   - API authentication (JWT tokens)
   - HTTPS/TLS
   - Input sanitization

9. **Advanced Filtering**
   - Filter by car park type
   - Filter by minimum available lots
   - Filter by distance radius

10. **Historical Data**
    - Track availability trends
    - Predict busy times
    - Analytics dashboard

### Low Priority

11. **Multi-region Support**
    - Support car parks in different countries
    - Time zone handling

12. **GraphQL API**
    - Allow clients to request exactly what they need
    - Reduce over-fetching

## Performance Considerations

- **Database Indexes**: Created on frequently queried columns (latitude, longitude, car_park_no)
- **Batch Processing**: CSV import and API sync use batch operations (100-200 records at a time)
- **Pagination**: Prevents loading entire result sets into memory
- **Connection Pooling**: Spring Boot's default HikariCP for efficient connection management
- **Lazy Loading**: Used for JPA relationships to avoid unnecessary queries

## Scalability Considerations

For handling increased load:

1. **Horizontal Scaling**: Stateless application design allows easy horizontal scaling
2. **Database Read Replicas**: Separate read and write operations
3. **Load Balancer**: Distribute traffic across multiple instances
4. **Caching**: Redis for frequently accessed data
5. **CDN**: For static assets (if added later)

## Project Timeline

This project was completed in approximately **6-7 hours**:

- Architecture & Design: 1 hour
- Core Implementation: 3 hours
- Docker Setup: 0.5 hours
- Testing: 1 hour
- Documentation: 1.5 hours

---

**Author:** [Your Name]
**Contact:** [Your Email]
**Date:** October 2025
**Version:** 1.0.0
