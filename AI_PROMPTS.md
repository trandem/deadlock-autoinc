# AI Tool Usage Documentation

This document details how Claude Code AI was used to complete this coding exercise, including the prompts, techniques, and workflow.

## AI Tool Used

## Initial Prompt

### Additional Context Provided

During the development, I provided the following clarification:
```
i would like to use mysql
```

This led Claude Code to switch from PostgreSQL to MySQL configuration.

## Development Workflow

### Phase 1: Requirements Analysis & Planning

**Claude Code Actions:**
1. Read and analyzed the PDF requirements document
2. Identified key requirements:
   - REST API for finding nearest car parks
   - Java with relational database (MySQL as requested)
   - CSV data import for car park information
   - External API integration for real-time availability
   - Pagination and input validation
   - Docker containerization

3. Created a comprehensive task list using TodoWrite tool:
   - 14 distinct tasks covering all aspects of development
   - Tracked progress throughout implementation
   - Updated status as each component was completed

### Phase 2: Project Setup & Architecture

**Generated Components:**
- Updated `pom.xml` with all required dependencies:
  - Spring Boot 3.5.6 (Web, Data JPA, Validation, Actuator)
  - MySQL connector
  - Flyway for migrations
  - Lombok for cleaner code
  - Apache Commons CSV
  - Testing libraries (JUnit, MockMvc, RestAssured)

**Architecture Decision:**
Claude Code designed a layered architecture:
- Controller → Service → Repository → Database
- Separation of concerns
- DTOs for API responses
- Entity models with JPA annotations

### Phase 3: Database Design

**Claude Code Designed:**

1. **Database Schema (2 tables)**
   ```sql
   car_parks:
   - Static car park information
   - Coordinates in both SVY21 and WGS84
   - Indexes on lat/lon for performance

   car_park_availability:
   - Real-time availability data
   - Foreign key to car_parks
   - Support for multiple lot types
   - Unique constraint per car park + lot type
   ```

2. **Flyway Migration Scripts**
   - V1: Create car_parks table with proper indexes
   - V2: Create car_park_availability table with relationships

**Rationale for Design:**
- Normalized structure separates static from dynamic data
- Indexes optimize spatial queries
- Foreign key ensures data integrity
- Supports multiple parking lot types (cars, motorcycles, heavy vehicles)

### Phase 4: Core Implementation

**Claude Code Implemented:**

1. **Entity Models**
   - `CarPark.java` - JPA entity with lifecycle callbacks
   - `CarParkAvailability.java` - Related entity
   - Used Lombok to reduce boilerplate

2. **DTOs**
   - `CarParkResponseDto.java` - API response format
   - `ErrorResponse.java` - Standardized error handling
   - JSON property annotations for API contract

3. **Repositories**
   - `CarParkRepository.java` - Custom query with Haversine formula
   - `CarParkAvailabilityRepository.java` - Availability management
   - Native SQL for distance calculation in database

4. **Utility Classes**
   - `DistanceCalculator.java` - Haversine formula implementation
   - `CoordinateConverter.java` - SVY21 to WGS84 conversion

5. **Service Layer**
   - `CarParkService.java` - Business logic
   - Distance-based sorting
   - Aggregation of availability data

6. **Controllers**
   - `CarParkController.java` - Main API endpoint
     - Request validation with annotations
     - Pagination support
     - Comprehensive error handling
   - `AdminController.java` - Manual sync trigger

7. **Background Tasks**
   - `CarParkDataImportTask.java` - CSV import on startup
     - Batch processing (100 records at a time)
     - Coordinate conversion
     - Error handling
   - `CarParkAvailabilitySyncTask.java` - API sync
     - HTTP client for external API
     - JSON parsing
     - Batch updates

### Phase 5: Docker Configuration

**Claude Code Created:**

1. **Dockerfile**
   - Multi-stage build for smaller image
   - Maven dependency caching
   - Non-root user for security
   - Health check configuration

2. **docker-compose.yml**
   - MySQL 8.0 service with persistent volume
   - Spring Boot application service
   - Network configuration
   - Health check dependencies
   - Environment variables

3. **init-db.sql**
   - Database initialization script
   - User privileges setup

### Phase 6: Testing

**Claude Code Wrote:**

1. **Unit Tests**
   - `DistanceCalculatorTest.java` - Haversine formula validation
   - Test cases for same point, known distances, edge cases

2. **Integration Tests**
   - `CarParkControllerIntegrationTest.java` - Full API testing
   - Test cases for success, missing params, invalid inputs, pagination
   - MockMvc for HTTP testing

3. **Test Configuration**
   - `application-test.properties` - H2 in-memory database
   - Disabled Flyway for tests (using ddl-auto instead)

### Phase 7: Documentation

**Claude Code Produced:**

1. **README.md** (Comprehensive, ~400 lines)
   - Technology stack overview
   - Architecture diagram
   - Database design with ERD
   - Complete API documentation with examples
   - Getting started guide (Docker & local)
   - Design decisions and trade-offs
   - Future improvements
   - Performance and scalability considerations

2. **AI_PROMPTS.md** (This document)
   - Complete workflow documentation
   - Prompts used
   - Claude Code's decision-making process

## Key Techniques Used by Claude Code

### 1. Incremental Development
- Built components in logical order
- Each piece tested before moving to next
- Maintained working state throughout

### 2. Task Management
- Used TodoWrite tool to track 14 distinct tasks
- Updated status as work progressed
- Ensured nothing was forgotten

### 3. File Operations
- Read existing files before editing
- Checked file structure before creating new ones
- Maintained consistency across codebase

### 4. Context Awareness
- Read PDF requirements thoroughly
- Examined CSV structure before writing parser
- Adapted to user's MySQL preference mid-stream

### 5. Best Practices
- Followed Spring Boot conventions
- Applied SOLID principles
- Used appropriate design patterns (Builder, Repository, DTO)
- Added comprehensive comments and documentation

### 6. Error Handling
- Validation at controller level
- Custom exception handlers
- Graceful degradation (CSV import failures don't crash app)
