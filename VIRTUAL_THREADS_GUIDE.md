# Virtual Threads (Green Threads) Guide

## Overview

This application now uses **Virtual Threads** (Project Loom), Java's native implementation of green threads. Virtual threads are lightweight threads managed by the JVM instead of the OS, enabling massive concurrency with minimal resource overhead.

## What Are Virtual Threads?

Virtual threads are:
- **Lightweight**: ~1KB memory per thread (vs ~1MB for platform threads)
- **Scalable**: Can create millions of threads without exhausting memory
- **Non-blocking**: Blocking operations don't block carrier threads
- **Simple**: Write synchronous code that runs concurrently

## Configuration

### 1. Java Version
- **Required**: Java 21+
- Already configured in `pom.xml:30-32`

### 2. Spring Boot Configuration
Located in `src/main/java/com/wego/carpark/config/VirtualThreadConfig.java`:

- **Spring Web (Tomcat)**: All HTTP requests handled by virtual threads
- **@Async Methods**: All async operations use virtual threads
- **Custom Executor**: Dedicated virtual thread executor for explicit control

### 3. Application Properties
Located in `src/main/resources/application.properties:40`:

```properties
# Enable virtual threads globally
spring.threads.virtual.enabled=true
```

## Where Virtual Threads Are Used

### 1. HTTP Request Handling (Spring Web)
Every HTTP request to your API is now handled by a virtual thread:

```java
@GetMapping("/carparks/nearest")
public ResponseEntity<List<CarParkResponseDto>> getNearestCarParks(...) {
    // This entire method runs on a virtual thread!
    // Can handle thousands of concurrent requests
}
```

### 2. Async Task Processing
See: `src/main/java/com/wego/carpark/task/CarParkAvailabilitySyncTask.java:107`

```java
// Each car park is processed concurrently in its own virtual thread
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    processCarParkNode(...);
}, virtualThreadExecutor);
```

### 3. CSV Import Processing
See: `src/main/java/com/wego/carpark/task/CarParkDataImportTask.java:108`

```java
// Each CSV record parsed concurrently
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    CarPark carPark = parseCarParkRecord(record);
    carParks.add(carPark);
}, virtualThreadExecutor);
```

### 4. @Async Methods
All methods annotated with `@Async` now run on virtual threads:

```java
@Async
public void processInBackground() {
    // Runs on a virtual thread automatically
}
```

## Testing Virtual Threads

### 1. Run the Demo
```bash
mvn compile exec:java -Dexec.mainClass="com.wego.carpark.util.VirtualThreadDemo"
```

This demonstrates the performance difference between:
- Sequential execution
- Platform threads (traditional)
- Virtual threads (green threads)

**Expected Output:**
```
Sequential: Completed 100 tasks in ~10000ms
Platform Threads: Completed 10000 tasks in ~5000ms using 200 threads
Virtual Threads: Completed 10000 tasks in ~500ms using 10000 virtual threads
```

### 2. Test Web Endpoints

#### Check Thread Info
```bash
curl http://localhost:8080/api/demo/virtual-threads/info
```

**Response:**
```json
{
  "threadName": "VirtualThread-12",
  "threadId": 56,
  "isVirtual": true,
  "message": "This request is handled by: Virtual Thread (Green Thread)"
}
```

#### Test Slow I/O Operation
```bash
curl "http://localhost:8080/api/demo/virtual-threads/slow-operation?delay=1000"
```

#### Test Concurrent Operations
```bash
curl "http://localhost:8080/api/demo/virtual-threads/concurrent?taskCount=10&taskDelay=500"
```

**Response shows:**
```json
{
  "taskCount": 10,
  "totalDurationMs": 520,
  "expectedSequentialDurationMs": 5000,
  "speedup": "9.62x faster"
}
```

#### Load Test
Use Apache Bench to test concurrent request handling:

```bash
# 10,000 requests with 1,000 concurrent connections
ab -n 10000 -c 1000 http://localhost:8080/api/demo/virtual-threads/load-test
```

With virtual threads, your application can handle thousands of concurrent requests efficiently!

## How to Use Virtual Threads in Your Code

### Method 1: Inject the Executor
```java
@Service
public class MyService {

    @Autowired
    @Qualifier("virtualThreadExecutor")
    private ExecutorService virtualThreadExecutor;

    public void processConcurrently(List<Item> items) {
        List<CompletableFuture<Void>> futures = items.stream()
            .map(item -> CompletableFuture.runAsync(() -> {
                // Process each item concurrently
                processItem(item);
            }, virtualThreadExecutor))
            .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
```

### Method 2: Use @Async
```java
@Service
public class MyService {

    @Async
    public CompletableFuture<Result> processAsync(Data data) {
        // This runs on a virtual thread
        Result result = heavyOperation(data);
        return CompletableFuture.completedFuture(result);
    }
}
```

### Method 3: Create Virtual Threads Directly
```java
public void runTask() {
    // Method 1: Using Thread.ofVirtual()
    Thread virtualThread = Thread.ofVirtual().start(() -> {
        // Your code here
    });

    // Method 2: Using Executors
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        executor.submit(() -> {
            // Your code here
        });
    }
}
```

## Best Practices

### ✅ Good Use Cases for Virtual Threads
1. **I/O-bound operations**
   - HTTP/REST API calls
   - Database queries
   - File I/O
   - Network operations

2. **High concurrency scenarios**
   - Web servers handling many requests
   - Microservices with many downstream calls
   - Batch processing with concurrent tasks

3. **Blocking operations**
   - Thread.sleep()
   - Blocking I/O
   - JDBC calls

### ❌ When NOT to Use Virtual Threads
1. **CPU-bound operations**
   - Heavy computation
   - Complex algorithms
   - Cryptography
   - (Use platform threads or parallel streams instead)

2. **Synchronized blocks**
   - Can pin virtual threads to carrier threads
   - Consider using `ReentrantLock` instead

3. **ThreadLocal variables**
   - Virtual threads can create millions of ThreadLocals
   - Use scoped values (JEP 429) instead

## Performance Comparison

### Traditional Thread Pool (200 threads)
- Memory: ~200MB (200 threads × 1MB)
- Max concurrent operations: 200
- Context switching: Expensive

### Virtual Threads (10,000 threads)
- Memory: ~10MB (10,000 threads × 1KB)
- Max concurrent operations: Millions
- Context switching: Cheap (JVM-managed)

## Monitoring Virtual Threads

### Check Thread Info in Logs
Virtual thread names appear as: `VirtualThread-nnn`

```
2025-10-18 12:34:56 - Request handled by: VirtualThread-42 (virtual=true)
```

### JVM Flags for Debugging
```bash
java -Djdk.tracePinnedThreads=full -jar app.jar
```

This helps identify when virtual threads are "pinned" to carrier threads (which reduces performance).

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Your Application                      │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌───────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ HTTP Requests │  │ @Async Tasks │  │ Custom Tasks │ │
│  └───────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│          │                  │                  │          │
│          └──────────────────┴──────────────────┘          │
│                             │                             │
│                    ┌────────▼────────┐                   │
│                    │ Virtual Thread  │                   │
│                    │    Executor     │                   │
│                    └────────┬────────┘                   │
│                             │                             │
│           ┌─────────────────┴─────────────────┐          │
│           │                                     │          │
│    ┌──────▼──────┐                      ┌──────▼──────┐ │
│    │   Virtual   │   ...millions...     │   Virtual   │ │
│    │  Thread 1   │                      │ Thread 1M   │ │
│    └──────┬──────┘                      └──────┬──────┘ │
│           │                                     │          │
├───────────┴─────────────────────────────────────┴────────┤
│                    Carrier Threads                        │
│         (Small pool of platform threads)                  │
├───────────────────────────────────────────────────────────┤
│                         JVM                               │
└───────────────────────────────────────────────────────────┘
```

## References

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot Virtual Threads Support](https://spring.io/blog/2022/10/11/embracing-virtual-threads)
- [Project Loom Documentation](https://wiki.openjdk.org/display/loom)

## Summary

Your application now leverages virtual threads for:
1. ✅ All HTTP request handling
2. ✅ Concurrent car park data processing
3. ✅ CSV import with parallel parsing
4. ✅ Async background tasks
5. ✅ Custom concurrent operations

This enables your application to handle **thousands of concurrent operations** with minimal resource overhead!