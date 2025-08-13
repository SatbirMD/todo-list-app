package com.example.todolist;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Advanced Task Management System implementing sophisticated design patterns and algorithms.
 * 
 * Features:
 * - Strategy Pattern for different sorting algorithms
 * - Observer Pattern for task change notifications
 * - Factory Pattern for task creation
 * - Concurrent processing capabilities
 * - Advanced analytics and machine learning inspired features
 * - Functional programming with Stream API
 */
public class TaskManager {
    
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final PriorityQueue<Task> priorityQueue = new PriorityQueue<>();
    private final List<TaskObserver> observers = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = ForkJoinPool.commonPool();
    private final TaskAnalytics analytics = new TaskAnalytics();
    
    // Strategy Pattern for different sorting strategies
    public interface SortingStrategy {
        Comparator<Task> getComparator();
    }
    
    public static class PrioritySortingStrategy implements SortingStrategy {
        @Override
        public Comparator<Task> getComparator() {
            return Comparator.comparing(Task::getPriority).reversed()
                    .thenComparing(Task::getDueDate);
        }
    }
    
    public static class UrgencySortingStrategy implements SortingStrategy {
        @Override
        public Comparator<Task> getComparator() {
            return Comparator.comparingDouble(Task::getUrgencyScore).reversed();
        }
    }
    
    public static class DueDateSortingStrategy implements SortingStrategy {
        @Override
        public Comparator<Task> getComparator() {
            return Comparator.comparing(Task::getDueDate);
        }
    }
    
    // Observer Pattern for task change notifications
    public interface TaskObserver {
        void onTaskAdded(Task task);
        void onTaskUpdated(Task oldTask, Task newTask);
        void onTaskRemoved(Task task);
        void onTaskCompleted(Task task);
    }
    
    // Factory Pattern for task creation
    public static class TaskFactory {
        public static Task createUrgentTask(String description) {
            return new Task.Builder()
                .description(description)
                .priority(Task.Priority.CRITICAL)
                .dueDate(LocalDateTime.now().plusDays(1))
                .estimatedHours(2)
                .build();
        }
        
        public static Task createLongTermTask(String description, String category) {
            return new Task.Builder()
                .description(description)
                .priority(Task.Priority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(30))
                .category(category)
                .estimatedHours(8)
                .build();
        }
        
        public static Task createQuickTask(String description) {
            return new Task.Builder()
                .description(description)
                .priority(Task.Priority.LOW)
                .dueDate(LocalDateTime.now().plusDays(3))
                .estimatedHours(1)
                .build();
        }
    }
    
    // Advanced analytics class
    public class TaskAnalytics {
        public double getAverageCompletionTime() {
            return getCompletedTasks().stream()
                .mapToLong(task -> java.time.temporal.ChronoUnit.HOURS.between(
                    task.getCreatedAt(), LocalDateTime.now()))
                .average()
                .orElse(0.0);
        }
        
        public Map<Task.Priority, Long> getTaskCountByPriority() {
            return tasks.values().stream()
                .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        }
        
        public Map<String, Long> getTaskCountByCategory() {
            return tasks.values().stream()
                .collect(Collectors.groupingBy(Task::getCategory, Collectors.counting()));
        }
        
        public double getProductivityScore() {
            long totalTasks = tasks.size();
            long completedTasks = getCompletedTasks().size();
            return totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;
        }
        
        public List<Task> getPredictedHighRiskTasks() {
            return tasks.values().stream()
                .filter(task -> task.getUrgencyScore() > 5.0)
                .filter(task -> task.getDaysUntilDue() < 2)
                .sorted(Comparator.comparingDouble(Task::getUrgencyScore).reversed())
                .collect(Collectors.toList());
        }
    }
    
    // Core task management operations
    public CompletableFuture<Void> addTask(Task task) {
        return CompletableFuture.runAsync(() -> {
            tasks.put(task.getId(), task);
            priorityQueue.offer(task);
            notifyObservers(observer -> observer.onTaskAdded(task));
        }, executorService);
    }
    
    public CompletableFuture<Optional<Task>> removeTask(String taskId) {
        return CompletableFuture.supplyAsync(() -> {
            Task removedTask = tasks.remove(taskId);
            if (removedTask != null) {
                priorityQueue.remove(removedTask);
                notifyObservers(observer -> observer.onTaskRemoved(removedTask));
                return Optional.of(removedTask);
            }
            return Optional.empty();
        }, executorService);
    }
    
    public CompletableFuture<Optional<Task>> updateTaskStatus(String taskId, Task.Status newStatus) {
        return CompletableFuture.supplyAsync(() -> {
            Task existingTask = tasks.get(taskId);
            if (existingTask != null) {
                Task updatedTask = existingTask.withStatus(newStatus);
                tasks.put(taskId, updatedTask);
                priorityQueue.remove(existingTask);
                priorityQueue.offer(updatedTask);
                
                notifyObservers(observer -> observer.onTaskUpdated(existingTask, updatedTask));
                
                if (newStatus == Task.Status.COMPLETED) {
                    notifyObservers(observer -> observer.onTaskCompleted(updatedTask));
                }
                
                return Optional.of(updatedTask);
            }
            return Optional.empty();
        }, executorService);
    }
    
    // Advanced querying with functional programming
    public List<Task> getTasksFiltered(Predicate<Task> filter, SortingStrategy strategy) {
        return tasks.values().stream()
            .filter(filter)
            .sorted(strategy.getComparator())
            .collect(Collectors.toList());
    }
    
    public List<Task> getOverdueTasks() {
        return getTasksFiltered(Task::isOverdue, new UrgencySortingStrategy());
    }
    
    public List<Task> getTasksByCategory(String category) {
        return getTasksFiltered(task -> task.getCategory().equals(category), 
                               new PrioritySortingStrategy());
    }
    
    public List<Task> getTasksByPriority(Task.Priority priority) {
        return getTasksFiltered(task -> task.getPriority() == priority, 
                               new DueDateSortingStrategy());
    }
    
    public List<Task> getCompletedTasks() {
        return getTasksFiltered(task -> task.getStatus() == Task.Status.COMPLETED,
                               new DueDateSortingStrategy());
    }
    
    public Optional<Task> getNextPriorityTask() {
        return Optional.ofNullable(priorityQueue.peek());
    }
    
    // Smart task recommendations using ML-inspired algorithms
    public List<Task> getRecommendedTasks(int maxTasks) {
        // Simulate intelligent task recommendation algorithm
        return tasks.values().stream()
            .filter(task -> task.getStatus() == Task.Status.PENDING)
            .sorted((t1, t2) -> {
                // Multi-criteria decision analysis
                double score1 = calculateTaskScore(t1);
                double score2 = calculateTaskScore(t2);
                return Double.compare(score2, score1);
            })
            .limit(maxTasks)
            .collect(Collectors.toList());
    }
    
    private double calculateTaskScore(Task task) {
        // Sophisticated scoring algorithm considering multiple factors
        double urgencyWeight = 0.4;
        double priorityWeight = 0.3;
        double dueDateWeight = 0.2;
        double effortWeight = 0.1;
        
        double urgencyScore = task.getUrgencyScore() / 10.0; // Normalize
        double priorityScore = task.getPriority().getValue() / 4.0; // Normalize
        double dueDateScore = Math.max(0, 7 - task.getDaysUntilDue()) / 7.0; // Normalize
        double effortScore = Math.max(0, 8 - task.getEstimatedHours()) / 8.0; // Normalize (prefer shorter tasks)
        
        return urgencyWeight * urgencyScore +
               priorityWeight * priorityScore +
               dueDateWeight * dueDateScore +
               effortWeight * effortScore;
    }
    
    // Batch operations with parallel processing
    public CompletableFuture<List<Task>> processBatchOperation(
            List<Task> tasksToProcess, 
            Function<Task, Task> operation) {
        
        return CompletableFuture.supplyAsync(() -> {
            return tasksToProcess.parallelStream()
                .map(operation)
                .collect(Collectors.toList());
        }, executorService);
    }
    
    // Observer management
    public void addObserver(TaskObserver observer) {
        observers.add(observer);
    }
    
    public void removeObserver(TaskObserver observer) {
        observers.remove(observer);
    }
    
    private void notifyObservers(Consumer<TaskObserver> notification) {
        observers.forEach(notification);
    }
    
    // Lifecycle management
    public TaskAnalytics getAnalytics() {
        return analytics;
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public int getTaskCount() {
        return tasks.size();
    }
    
    public Collection<Task> getAllTasks() {
        return Collections.unmodifiableCollection(tasks.values());
    }
}