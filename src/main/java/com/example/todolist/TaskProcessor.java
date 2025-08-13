package com.example.todolist;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Advanced Task Processing Engine with concurrent execution capabilities.
 * 
 * Features:
 * - Asynchronous task execution
 * - Thread pool management
 * - Task scheduling and automation
 * - Performance monitoring
 * - Circuit breaker pattern for fault tolerance
 * - Advanced logging and metrics collection
 */
public class TaskProcessor {
    
    private final ScheduledExecutorService scheduler;
    private final ExecutorService workerPool;
    private final TaskExecutionMetrics metrics;
    private final CircuitBreaker circuitBreaker;
    private final Map<String, ScheduledFuture<?>> scheduledTasks;
    
    public TaskProcessor() {
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.workerPool = ForkJoinPool.commonPool();
        this.metrics = new TaskExecutionMetrics();
        this.circuitBreaker = new CircuitBreaker();
        this.scheduledTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * Circuit Breaker pattern implementation for fault tolerance
     */
    public static class CircuitBreaker {
        private enum State { CLOSED, OPEN, HALF_OPEN }
        
        private volatile State state = State.CLOSED;
        private volatile int failureCount = 0;
        private volatile long lastFailureTime = 0;
        private final int failureThreshold = 5;
        private final long timeout = 60000; // 1 minute
        
        public boolean allowRequest() {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > timeout) {
                    state = State.HALF_OPEN;
                    return true;
                }
                return false;
            }
            return true;
        }
        
        public void recordSuccess() {
            failureCount = 0;
            state = State.CLOSED;
        }
        
        public void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            if (failureCount >= failureThreshold) {
                state = State.OPEN;
            }
        }
        
        public State getState() { return state; }
    }
    
    /**
     * Task execution metrics collector
     */
    public static class TaskExecutionMetrics {
        private final Map<String, ExecutionStats> stats = new ConcurrentHashMap<>();
        
        public static class ExecutionStats {
            private long totalExecutions = 0;
            private long totalExecutionTime = 0;
            private long successCount = 0;
            private long failureCount = 0;
            private long lastExecutionTime = 0;
            
            public synchronized void recordExecution(long executionTime, boolean success) {
                totalExecutions++;
                totalExecutionTime += executionTime;
                lastExecutionTime = System.currentTimeMillis();
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
            
            public double getAverageExecutionTime() {
                return totalExecutions > 0 ? (double) totalExecutionTime / totalExecutions : 0;
            }
            
            public double getSuccessRate() {
                return totalExecutions > 0 ? (double) successCount / totalExecutions : 0;
            }
            
            @Override
            public String toString() {
                return String.format("ExecutionStats{executions=%d, avgTime=%.2fms, successRate=%.2f%%}", 
                    totalExecutions, getAverageExecutionTime(), getSuccessRate() * 100);
            }
        }
        
        public void recordExecution(String operation, long executionTime, boolean success) {
            stats.computeIfAbsent(operation, k -> new ExecutionStats())
                  .recordExecution(executionTime, success);
        }
        
        public ExecutionStats getStats(String operation) {
            return stats.get(operation);
        }
        
        public Map<String, ExecutionStats> getAllStats() {
            return Collections.unmodifiableMap(stats);
        }
    }
    
    /**
     * Execute a task asynchronously with performance monitoring
     */
    public CompletableFuture<TaskExecutionResult> executeTask(Task task, TaskExecutor executor) {
        if (!circuitBreaker.allowRequest()) {
            return CompletableFuture.completedFuture(
                TaskExecutionResult.failure(task, new RuntimeException("Circuit breaker is OPEN")));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String operation = "task_execution";
            
            try {
                TaskExecutionResult result = executor.execute(task);
                long executionTime = System.currentTimeMillis() - startTime;
                
                metrics.recordExecution(operation, executionTime, result.isSuccess());
                if (result.isSuccess()) {
                    circuitBreaker.recordSuccess();
                } else {
                    circuitBreaker.recordFailure();
                }
                
                return result;
                
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                metrics.recordExecution(operation, executionTime, false);
                circuitBreaker.recordFailure();
                
                return TaskExecutionResult.failure(task, e);
            }
        }, workerPool);
    }
    
    /**
     * Schedule recurring task processing
     */
    public ScheduledFuture<?> scheduleRecurringExecution(
            String scheduleId,
            TaskManager taskManager,
            TaskExecutor executor,
            long period,
            TimeUnit timeUnit) {
        
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            processHighPriorityTasks(taskManager, executor);
        }, 0, period, timeUnit);
        
        scheduledTasks.put(scheduleId, future);
        return future;
    }
    
    private void processHighPriorityTasks(TaskManager taskManager, TaskExecutor executor) {
        List<Task> highPriorityTasks = taskManager.getTasksByPriority(Task.Priority.CRITICAL);
        
        List<CompletableFuture<TaskExecutionResult>> futures = highPriorityTasks.stream()
            .map(task -> executeTask(task, executor))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        // Wait for all tasks to complete and log results
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                futures.forEach(future -> {
                    try {
                        TaskExecutionResult result = future.get();
                        logExecutionResult(result);
                    } catch (Exception e) {
                        System.err.println("Error processing task: " + e.getMessage());
                    }
                });
            });
    }
    
    /**
     * Batch process multiple tasks with parallel execution
     */
    public CompletableFuture<List<TaskExecutionResult>> processBatch(
            List<Task> tasks, 
            TaskExecutor executor,
            int maxConcurrency) {
        
        // Use a semaphore to limit concurrency
        Semaphore semaphore = new Semaphore(maxConcurrency);
        
        List<CompletableFuture<TaskExecutionResult>> futures = tasks.stream()
            .map(task -> CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    return executeTask(task, executor).get();
                } catch (Exception e) {
                    return TaskExecutionResult.failure(task, e);
                } finally {
                    semaphore.release();
                }
            }, workerPool))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
    }
    
    /**
     * Smart task scheduling based on urgency and dependencies
     */
    public CompletableFuture<List<TaskExecutionResult>> smartSchedule(
            TaskManager taskManager, 
            TaskExecutor executor) {
        
        // Get recommended tasks using the task manager's ML-inspired algorithm
        List<Task> recommendedTasks = taskManager.getRecommendedTasks(10);
        
        // Create execution plan with optimal ordering
        List<Task> executionPlan = optimizeExecutionOrder(recommendedTasks);
        
        // Execute tasks with adaptive concurrency
        int optimalConcurrency = calculateOptimalConcurrency(executionPlan);
        
        return processBatch(executionPlan, executor, optimalConcurrency);
    }
    
    private List<Task> optimizeExecutionOrder(List<Task> tasks) {
        // Sort tasks by a sophisticated algorithm considering:
        // 1. Urgency score
        // 2. Estimated execution time
        // 3. Dependencies (simulated)
        return tasks.stream()
            .sorted((t1, t2) -> {
                double score1 = calculateExecutionPriority(t1);
                double score2 = calculateExecutionPriority(t2);
                return Double.compare(score2, score1);
            })
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    private double calculateExecutionPriority(Task task) {
        double urgencyFactor = task.getUrgencyScore() * 0.6;
        double timeFactor = (10.0 - task.getEstimatedHours()) / 10.0 * 0.3; // Prefer shorter tasks
        double ageFactor = Math.min(1.0, task.getDaysUntilDue() / 7.0) * 0.1;
        
        return urgencyFactor + timeFactor + ageFactor;
    }
    
    private int calculateOptimalConcurrency(List<Task> tasks) {
        // Adaptive concurrency based on system resources and task characteristics
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        double averageTaskComplexity = tasks.stream()
            .mapToInt(Task::getEstimatedHours)
            .average()
            .orElse(1.0);
        
        // Scale concurrency based on task complexity
        if (averageTaskComplexity > 4) {
            return Math.max(2, availableProcessors / 2);
        } else if (averageTaskComplexity > 2) {
            return availableProcessors;
        } else {
            return Math.min(10, availableProcessors * 2);
        }
    }
    
    /**
     * Cancel a scheduled task
     */
    public boolean cancelScheduledTask(String scheduleId) {
        ScheduledFuture<?> future = scheduledTasks.remove(scheduleId);
        return future != null && future.cancel(false);
    }
    
    /**
     * Get comprehensive performance report
     */
    public ProcessorPerformanceReport getPerformanceReport() {
        return new ProcessorPerformanceReport(
            metrics.getAllStats(),
            circuitBreaker.getState(),
            scheduledTasks.size(),
            workerPool.toString()
        );
    }
    
    private void logExecutionResult(TaskExecutionResult result) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        
        if (result.isSuccess()) {
            System.out.printf("[%s] ✓ Task executed successfully: %s (%.2fms)%n", 
                timestamp, result.getTask().getDescription(), result.getExecutionTime());
        } else {
            System.out.printf("[%s] ✗ Task execution failed: %s - %s%n", 
                timestamp, result.getTask().getDescription(), result.getError().getMessage());
        }
    }
    
    /**
     * Graceful shutdown
     */
    public void shutdown() {
        // Cancel all scheduled tasks
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();
        
        // Shutdown schedulers
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Functional interface for task execution
    @FunctionalInterface
    public interface TaskExecutor {
        TaskExecutionResult execute(Task task) throws Exception;
    }
    
    // Task execution result wrapper
    public static class TaskExecutionResult {
        private final Task task;
        private final boolean success;
        private final Exception error;
        private final long executionTime;
        private final LocalDateTime completedAt;
        
        private TaskExecutionResult(Task task, boolean success, Exception error, long executionTime) {
            this.task = task;
            this.success = success;
            this.error = error;
            this.executionTime = executionTime;
            this.completedAt = LocalDateTime.now();
        }
        
        public static TaskExecutionResult success(Task task, long executionTime) {
            return new TaskExecutionResult(task, true, null, executionTime);
        }
        
        public static TaskExecutionResult failure(Task task, Exception error) {
            return new TaskExecutionResult(task, false, error, 0);
        }
        
        // Getters
        public Task getTask() { return task; }
        public boolean isSuccess() { return success; }
        public Exception getError() { return error; }
        public long getExecutionTime() { return executionTime; }
        public LocalDateTime getCompletedAt() { return completedAt; }
    }
    
    // Performance report data structure
    public static class ProcessorPerformanceReport {
        private final Map<String, TaskExecutionMetrics.ExecutionStats> metrics;
        private final CircuitBreaker.State circuitBreakerState;
        private final int activeScheduledTasks;
        private final String workerPoolInfo;
        
        public ProcessorPerformanceReport(Map<String, TaskExecutionMetrics.ExecutionStats> metrics,
                                        CircuitBreaker.State circuitBreakerState,
                                        int activeScheduledTasks,
                                        String workerPoolInfo) {
            this.metrics = metrics;
            this.circuitBreakerState = circuitBreakerState;
            this.activeScheduledTasks = activeScheduledTasks;
            this.workerPoolInfo = workerPoolInfo;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Task Processor Performance Report ===\n");
            sb.append(String.format("Circuit Breaker State: %s\n", circuitBreakerState));
            sb.append(String.format("Active Scheduled Tasks: %d\n", activeScheduledTasks));
            sb.append(String.format("Worker Pool: %s\n", workerPoolInfo));
            sb.append("\nExecution Metrics:\n");
            
            metrics.forEach((operation, stats) -> {
                sb.append(String.format("  %s: %s\n", operation, stats));
            });
            
            return sb.toString();
        }
    }
}