# API Documentation

## Task Class API

### Core Properties
- `String id` - Unique identifier (UUID)
- `String description` - Task description
- `Priority priority` - Task priority (LOW, MEDIUM, HIGH, CRITICAL)
- `Status status` - Task status (PENDING, IN_PROGRESS, COMPLETED, ARCHIVED)
- `LocalDateTime createdAt` - Creation timestamp
- `LocalDateTime dueDate` - Due date
- `String category` - Task category
- `int estimatedHours` - Estimated hours to complete

### Builder Pattern
```java
Task task = new Task.Builder()
    .description("Task description")
    .priority(Task.Priority.HIGH)
    .category("Development")
    .estimatedHours(4)
    .dueDate(LocalDateTime.now().plusDays(3))
    .build();
```

### Advanced Methods
- `double getUrgencyScore()` - Calculate urgency based on priority, time, and effort
- `long getDaysUntilDue()` - Days remaining until due date
- `boolean isOverdue()` - Check if task is past due date
- `Task withStatus(Status)` - Create copy with new status (immutability)
- `Task withPriority(Priority)` - Create copy with new priority

## TaskManager Class API

### Core Operations
```java
CompletableFuture<Void> addTask(Task task)
CompletableFuture<Optional<Task>> removeTask(String taskId)
CompletableFuture<Optional<Task>> updateTaskStatus(String taskId, Status newStatus)
```

### Querying and Filtering
```java
List<Task> getTasksFiltered(Predicate<Task> filter, SortingStrategy strategy)
List<Task> getOverdueTasks()
List<Task> getTasksByCategory(String category)
List<Task> getTasksByPriority(Priority priority)
List<Task> getCompletedTasks()
List<Task> getRecommendedTasks(int maxTasks)
```

### Sorting Strategies
- `PrioritySortingStrategy` - Sort by priority then due date
- `UrgencySortingStrategy` - Sort by calculated urgency score
- `DueDateSortingStrategy` - Sort by due date

### Factory Methods
```java
TaskFactory.createUrgentTask(String description)
TaskFactory.createQuickTask(String description)
TaskFactory.createLongTermTask(String description, String category)
```

### Analytics
```java
TaskAnalytics analytics = manager.getAnalytics();
double productivityScore = analytics.getProductivityScore();
Map<Priority, Long> priorityDistribution = analytics.getTaskCountByPriority();
List<Task> highRiskTasks = analytics.getPredictedHighRiskTasks();
```

### Observer Pattern
```java
manager.addObserver(new TaskObserver() {
    void onTaskAdded(Task task) { /* handle */ }
    void onTaskUpdated(Task oldTask, Task newTask) { /* handle */ }
    void onTaskRemoved(Task task) { /* handle */ }
    void onTaskCompleted(Task task) { /* handle */ }
});
```

## TaskProcessor Class API

### Task Execution
```java
CompletableFuture<TaskExecutionResult> executeTask(Task task, TaskExecutor executor)
CompletableFuture<List<TaskExecutionResult>> processBatch(List<Task> tasks, 
                                                          TaskExecutor executor, 
                                                          int maxConcurrency)
CompletableFuture<List<TaskExecutionResult>> smartSchedule(TaskManager manager, 
                                                           TaskExecutor executor)
```

### Scheduling
```java
ScheduledFuture<?> scheduleRecurringExecution(String scheduleId,
                                              TaskManager taskManager,
                                              TaskExecutor executor,
                                              long period,
                                              TimeUnit timeUnit)
boolean cancelScheduledTask(String scheduleId)
```

### Performance Monitoring
```java
ProcessorPerformanceReport report = processor.getPerformanceReport();
// Contains:
// - Circuit breaker state
// - Active scheduled tasks count
// - Worker pool information
// - Execution metrics (success rate, average time)
```

### Circuit Breaker
Automatic fault tolerance with configurable:
- Failure threshold (default: 5 failures)
- Timeout period (default: 60 seconds)
- State management (CLOSED, OPEN, HALF_OPEN)

## TaskPersistenceManager Class API

### Configuration
```java
PersistenceConfiguration config = new PersistenceConfiguration(
    SerializationFormat.COMPRESSED_BINARY,  // Format
    true,                                    // Enable backups
    5,                                       // Max backup versions
    true,                                    // Enable compression
    30000L                                   // Auto-save interval
);
```

### Persistence Operations
```java
CompletableFuture<PersistenceResult> saveTasks(Collection<Task> tasks)
CompletableFuture<LoadResult> loadTasks()
```

### Export Formats
```java
CompletableFuture<PersistenceResult> exportTasks(Collection<Task> tasks,
                                                 Path exportPath,
                                                 ExportFormat format)
```
Supported formats:
- `CSV` - Comma-separated values
- `JSON_LIKE` - JSON-style format
- `XML_LIKE` - XML-style format

### Data Validation
```java
ValidationResult validation = validator.validateTaskData(tasks);
boolean isValid = validation.isValid();
List<String> errors = validation.getErrors();
List<String> warnings = validation.getWarnings();
```

### Serialization Formats
- `BINARY` - Standard Java serialization
- `CUSTOM_TEXT` - Human-readable text format
- `COMPRESSED_BINARY` - GZIP compressed binary (recommended)

## TaskExecutor Interface

Functional interface for task execution:
```java
@FunctionalInterface
public interface TaskExecutor {
    TaskExecutionResult execute(Task task) throws Exception;
}
```

Example implementations:
```java
// Simple logging executor
TaskExecutor loggingExecutor = task -> {
    System.out.println("Processing: " + task.getDescription());
    Thread.sleep(1000); // Simulate work
    return TaskExecutionResult.success(task, 1000);
};

// Complex business logic executor
TaskExecutor businessExecutor = task -> {
    // Perform actual business logic
    validateTask(task);
    processTask(task);
    updateStatus(task);
    return TaskExecutionResult.success(task, executionTime);
};
```

## Exception Handling

### Common Exceptions
- `ValidationException` - Data validation failures
- `PersistenceException` - Storage operation failures
- `ProcessingException` - Task execution failures
- `CircuitBreakerOpenException` - Circuit breaker in open state

### Error Recovery
- Automatic retry mechanisms
- Circuit breaker for fault tolerance
- Graceful degradation
- Comprehensive error reporting

## Performance Characteristics

### Time Complexity
- Task insertion: O(log n) - priority queue
- Task retrieval: O(1) - hash map lookup
- Filtering operations: O(n) - stream processing
- Sorting operations: O(n log n) - efficient comparators

### Memory Usage
- Immutable objects minimize memory leaks
- Defensive copying for data integrity
- Efficient collections (ConcurrentHashMap, PriorityQueue)
- Compression for storage optimization

### Concurrency
- Thread-safe operations throughout
- Non-blocking asynchronous execution
- Optimal thread pool utilization
- Lock-free data structures where possible

## Best Practices

### Task Creation
```java
// Good: Use Builder pattern for complex tasks
Task task = new Task.Builder()
    .description("Comprehensive description")
    .priority(Task.Priority.HIGH)
    .category("Development")
    .estimatedHours(4)
    .dueDate(LocalDateTime.now().plusDays(3))
    .build();

// Better: Use Factory for common patterns
Task urgentTask = TaskManager.TaskFactory.createUrgentTask("Critical bug fix");
```

### Error Handling
```java
// Handle async operations properly
manager.addTask(task)
    .thenRun(() -> System.out.println("Task added"))
    .exceptionally(throwable -> {
        System.err.println("Failed to add task: " + throwable.getMessage());
        return null;
    });
```

### Resource Management
```java
// Always clean up resources
try {
    TaskManager manager = new TaskManager();
    TaskProcessor processor = new TaskProcessor();
    
    // Use resources...
    
} finally {
    manager.shutdown();
    processor.shutdown();
}
```

### Performance Optimization
```java
// Use appropriate sorting strategies
List<Task> tasks = manager.getTasksFiltered(
    task -> task.getPriority() == Priority.HIGH,
    new TaskManager.UrgencySortingStrategy()  // Most efficient for urgency-based queries
);

// Batch operations for better performance
processor.processBatch(tasks, executor, optimalConcurrency);
```