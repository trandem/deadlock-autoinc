package com.wego.carpark.config;

import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * Configuration for Virtual Threads (Project Loom).
 * Virtual threads are lightweight threads managed by the JVM rather than the OS.
 * They provide excellent scalability for I/O-bound operations with minimal overhead.
 *
 * This configuration enables virtual threads for:
 * 1. Spring Web (Tomcat) - handles HTTP requests using virtual threads
 * 2. @Async methods - async operations run on virtual threads
 * 3. Custom concurrent operations - explicit virtual thread executor
 *
 * Benefits:
 * - Create millions of virtual threads without significant memory overhead
 * - Each thread consumes only ~1KB of memory (vs ~1MB for platform threads)
 * - Automatic scheduling and context switching by the JVM
 * - Perfect for concurrent I/O operations (HTTP calls, database queries, file operations)
 * - Handle thousands of concurrent HTTP requests efficiently
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    /**
     * Configures Tomcat to use virtual threads for handling HTTP requests.
     * This means each incoming HTTP request will be processed on a virtual thread,
     * allowing your application to handle thousands of concurrent requests efficiently.
     *
     * @return TomcatProtocolHandlerCustomizer that enables virtual threads
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    /**
     * Creates an AsyncTaskExecutor using virtual threads.
     * This replaces the default Spring Boot task executor with one backed by virtual threads.
     * All @Async methods will execute on virtual threads.
     *
     * @return AsyncTaskExecutor that uses virtual threads
     */
    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Creates a dedicated virtual thread executor for custom concurrent operations.
     * Use this bean when you need explicit control over concurrent task execution.
     *
     * Example usage:
     * <pre>
     * {@code
     * @Autowired
     * @Qualifier("virtualThreadExecutor")
     * private ExecutorService virtualThreadExecutor;
     *
     * public void processConcurrently() {
     *     virtualThreadExecutor.submit(() -> {
     *         // Your concurrent task
     *     });
     * }
     * }
     * </pre>
     *
     * @return ExecutorService that creates a new virtual thread for each submitted task
     */
    @Bean(name = "virtualThreadExecutor")
    public java.util.concurrent.ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}