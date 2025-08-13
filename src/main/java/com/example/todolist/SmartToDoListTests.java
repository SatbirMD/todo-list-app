package com.example.todolist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive test suite for the Smart To-Do List System.
 * Validates all advanced features and design patterns.
 */
public class SmartToDoListTests {
    
    private static final AtomicInteger testCount = new AtomicInteger(0);
    private static final AtomicInteger passedTests = new AtomicInteger(0);
    
    public static void main(String[] args) {
        System.out.println("🧪 Smart To-Do List System Test Suite");
        System.out.println("=" + "=".repeat(50));
        
        try {
            testTaskBuilderPattern();
            testTaskImmutability();
            testUrgencyCalculation();
            testTaskManagerBasicOperations();
            testSortingStrategies();
            testObserverPattern();
            testFactoryPattern();
            testConcurrentOperations();
            testTaskProcessor();
            testAnalytics();
            testPersistence();
            
            System.out.println("\n" + "=".repeat(60));
            System.out.printf("🎯 Test Results: %d/%d tests passed (%.1f%%)%n", 
                passedTests.get(), testCount.get(), 
                (double) passedTests.get() / testCount.get() * 100);
            
            if (passedTests.get() == testCount.get()) {
                System.out.println("🎉 All tests passed! The smart features are working correctly.");
            } else {
                System.out.println("❌ Some tests failed. Please review the implementation.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Test suite failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testTaskBuilderPattern() {
        System.out.println("\n🔧 Testing Builder Pattern");
        
        assertTest("Builder creates task with all properties", () -> {
            Task task = new Task.Builder()
                .description("Test task")
                .priority(Task.Priority.HIGH)
                .category("Testing")
                .estimatedHours(3)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
            
            return task.getDescription().equals("Test task") &&
                   task.getPriority() == Task.Priority.HIGH &&
                   task.getCategory().equals("Testing") &&
                   task.getEstimatedHours() == 3;
        });
        
        assertTest("Builder provides default values", () -> {
            Task task = new Task.Builder()
                .description("Minimal task")
                .build();
            
            return task.getPriority() == Task.Priority.MEDIUM &&
                   task.getStatus() == Task.Status.PENDING &&
                   task.getCategory().equals("General");
        });
    }
    
    private static void testTaskImmutability() {
        System.out.println("\n🔒 Testing Task Immutability");
        
        assertTest("withStatus creates new instance", () -> {
            Task original = new Task("Test task");
            Task updated = original.withStatus(Task.Status.COMPLETED);
            
            return original != updated &&
                   original.getStatus() == Task.Status.PENDING &&
                   updated.getStatus() == Task.Status.COMPLETED &&
                   original.getDescription().equals(updated.getDescription());
        });
        
        assertTest("withPriority creates new instance", () -> {
            Task original = new Task("Test task");
            Task updated = original.withPriority(Task.Priority.CRITICAL);
            
            return original != updated &&
                   original.getPriority() == Task.Priority.MEDIUM &&
                   updated.getPriority() == Task.Priority.CRITICAL;
        });
    }
    
    private static void testUrgencyCalculation() {
        System.out.println("\n🎯 Testing Urgency Calculation Algorithm");
        
        assertTest("Critical task has higher urgency than low priority", () -> {
            Task criticalTask = new Task.Builder()
                .description("Critical")
                .priority(Task.Priority.CRITICAL)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
            
            Task lowTask = new Task.Builder()
                .description("Low")
                .priority(Task.Priority.LOW)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
            
            return criticalTask.getUrgencyScore() > lowTask.getUrgencyScore();
        });
        
        assertTest("Overdue task detection works", () -> {
            Task overdueTask = new Task.Builder()
                .description("Overdue")
                .dueDate(LocalDateTime.now().minusDays(1))
                .build();
            
            Task futureTask = new Task.Builder()
                .description("Future")
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
            
            return overdueTask.isOverdue() && !futureTask.isOverdue();
        });
    }
    
    private static void testTaskManagerBasicOperations() throws Exception {
        System.out.println("\n📋 Testing TaskManager Basic Operations");
        
        TaskManager manager = new TaskManager();
        
        assertTest("Add and retrieve tasks", () -> {
            try {
                Task task = new Task("Test task");
                manager.addTask(task).get();
                
                return manager.getTaskCount() == 1 &&
                       manager.getAllTasks().contains(task);
            } catch (Exception e) {
                return false;
            }
        });
        
        assertTest("Remove tasks", () -> {
            try {
                Task task = new Task("Task to remove");
                manager.addTask(task).get();
                int countBefore = manager.getTaskCount();
                
                manager.removeTask(task.getId()).get();
                
                return manager.getTaskCount() == countBefore - 1 &&
                       !manager.getAllTasks().contains(task);
            } catch (Exception e) {
                return false;
            }
        });
        
        assertTest("Update task status", () -> {
            try {
                Task task = new Task("Task to update");
                manager.addTask(task).get();
                
                manager.updateTaskStatus(task.getId(), Task.Status.COMPLETED).get();
                
                // Find the updated task
                return manager.getAllTasks().stream()
                    .anyMatch(t -> t.getId().equals(task.getId()) && 
                              t.getStatus() == Task.Status.COMPLETED);
            } catch (Exception e) {
                return false;
            }
        });
        
        manager.shutdown();
    }
    
    private static void testSortingStrategies() throws Exception {
        System.out.println("\n📊 Testing Sorting Strategies");
        
        TaskManager manager = new TaskManager();
        
        // Add tasks with different priorities and urgencies
        Task lowTask = TaskManager.TaskFactory.createQuickTask("Low priority");
        Task highTask = TaskManager.TaskFactory.createUrgentTask("High priority");
        
        manager.addTask(lowTask).get();
        manager.addTask(highTask).get();
        
        assertTest("Priority sorting works", () -> {
            List<Task> sorted = manager.getTasksFiltered(
                task -> true,
                new TaskManager.PrioritySortingStrategy()
            );
            
            return sorted.size() == 2 && 
                   sorted.get(0).getPriority().getValue() >= sorted.get(1).getPriority().getValue();
        });
        
        assertTest("Urgency sorting works", () -> {
            List<Task> sorted = manager.getTasksFiltered(
                task -> true,
                new TaskManager.UrgencySortingStrategy()
            );
            
            return sorted.size() == 2 && 
                   sorted.get(0).getUrgencyScore() >= sorted.get(1).getUrgencyScore();
        });
        
        manager.shutdown();
    }
    
    private static void testObserverPattern() throws Exception {
        System.out.println("\n👁️ Testing Observer Pattern");
        
        TaskManager manager = new TaskManager();
        final AtomicInteger notificationCount = new AtomicInteger(0);
        
        manager.addObserver(new TaskManager.TaskObserver() {
            @Override
            public void onTaskAdded(Task task) {
                notificationCount.incrementAndGet();
            }
            
            @Override
            public void onTaskUpdated(Task oldTask, Task newTask) {
                notificationCount.incrementAndGet();
            }
            
            @Override
            public void onTaskRemoved(Task task) {
                notificationCount.incrementAndGet();
            }
            
            @Override
            public void onTaskCompleted(Task task) {
                notificationCount.incrementAndGet();
            }
        });
        
        assertTest("Observer receives notifications", () -> {
            try {
                Task task = new Task("Observer test");
                manager.addTask(task).get(); // Should trigger onTaskAdded
                
                manager.updateTaskStatus(task.getId(), Task.Status.COMPLETED).get(); 
                // Should trigger onTaskUpdated and onTaskCompleted
                
                return notificationCount.get() >= 2; // At least add and update notifications
            } catch (Exception e) {
                return false;
            }
        });
        
        manager.shutdown();
    }
    
    private static void testFactoryPattern() {
        System.out.println("\n🏭 Testing Factory Pattern");
        
        assertTest("Urgent task factory creates correct task", () -> {
            Task urgent = TaskManager.TaskFactory.createUrgentTask("Urgent test");
            return urgent.getPriority() == Task.Priority.CRITICAL &&
                   urgent.getDaysUntilDue() <= 1;
        });
        
        assertTest("Quick task factory creates correct task", () -> {
            Task quick = TaskManager.TaskFactory.createQuickTask("Quick test");
            return quick.getPriority() == Task.Priority.LOW &&
                   quick.getEstimatedHours() == 1;
        });
        
        assertTest("Long-term task factory creates correct task", () -> {
            Task longTerm = TaskManager.TaskFactory.createLongTermTask("Long test", "Custom");
            return longTerm.getPriority() == Task.Priority.MEDIUM &&
                   longTerm.getCategory().equals("Custom") &&
                   longTerm.getDaysUntilDue() >= 25;
        });
    }
    
    private static void testConcurrentOperations() throws Exception {
        System.out.println("\n⚡ Testing Concurrent Operations");
        
        TaskManager manager = new TaskManager();
        
        assertTest("Concurrent task additions", () -> {
            try {
                CompletableFuture<Void> future1 = manager.addTask(new Task("Concurrent 1"));
                CompletableFuture<Void> future2 = manager.addTask(new Task("Concurrent 2"));
                CompletableFuture<Void> future3 = manager.addTask(new Task("Concurrent 3"));
                
                CompletableFuture.allOf(future1, future2, future3).get();
                
                return manager.getTaskCount() == 3;
            } catch (Exception e) {
                return false;
            }
        });
        
        manager.shutdown();
    }
    
    private static void testTaskProcessor() throws Exception {
        System.out.println("\n🔄 Testing Task Processor");
        
        TaskProcessor processor = new TaskProcessor();
        
        assertTest("Task execution works", () -> {
            try {
                Task task = new Task("Processor test");
                
                TaskProcessor.TaskExecutor executor = t -> {
                    Thread.sleep(10); // Minimal work simulation
                    return TaskProcessor.TaskExecutionResult.success(t, 10);
                };
                
                TaskProcessor.TaskExecutionResult result = 
                    processor.executeTask(task, executor).get();
                
                return result.isSuccess() && 
                       result.getTask().equals(task) &&
                       result.getExecutionTime() >= 0;
            } catch (Exception e) {
                return false;
            }
        });
        
        assertTest("Circuit breaker state is accessible", () -> {
            TaskProcessor.ProcessorPerformanceReport report = 
                processor.getPerformanceReport();
            
            return report.toString().contains("Circuit Breaker State");
        });
        
        processor.shutdown();
    }
    
    private static void testAnalytics() throws Exception {
        System.out.println("\n📊 Testing Analytics");
        
        TaskManager manager = new TaskManager();
        
        // Add sample tasks
        manager.addTask(TaskManager.TaskFactory.createUrgentTask("Critical task")).get();
        manager.addTask(TaskManager.TaskFactory.createQuickTask("Low task")).get();
        
        Task completed = new Task.Builder()
            .description("Completed task")
            .status(Task.Status.COMPLETED)
            .build();
        manager.addTask(completed).get();
        
        TaskManager.TaskAnalytics analytics = manager.getAnalytics();
        
        assertTest("Analytics calculates productivity score", () -> {
            double score = analytics.getProductivityScore();
            return score >= 0 && score <= 100;
        });
        
        assertTest("Analytics provides priority distribution", () -> {
            var distribution = analytics.getTaskCountByPriority();
            return distribution.size() > 0 && 
                   distribution.values().stream().mapToLong(Long::longValue).sum() == 3;
        });
        
        assertTest("Analytics provides category distribution", () -> {
            var distribution = analytics.getTaskCountByCategory();
            return distribution.size() > 0;
        });
        
        manager.shutdown();
    }
    
    private static void testPersistence() throws Exception {
        System.out.println("\n💾 Testing Persistence");
        
        TaskPersistenceManager persistence = new TaskPersistenceManager("./test_data");
        
        List<Task> testTasks = List.of(
            new Task("Persistence test 1"),
            new Task("Persistence test 2")
        );
        
        assertTest("Save and load tasks", () -> {
            try {
                TaskPersistenceManager.PersistenceResult saveResult = 
                    persistence.saveTasks(testTasks).get();
                
                if (!saveResult.isSuccess()) {
                    return false;
                }
                
                TaskPersistenceManager.LoadResult loadResult = 
                    persistence.loadTasks().get();
                
                return loadResult.isSuccess() && 
                       loadResult.getTasks().size() == testTasks.size();
            } catch (Exception e) {
                return false;
            }
        });
        
        assertTest("Export functionality works", () -> {
            try {
                TaskPersistenceManager.PersistenceResult exportResult = 
                    persistence.exportTasks(testTasks,
                        java.nio.file.Paths.get("./test_export.csv"),
                        TaskPersistenceManager.ExportFormat.CSV).get();
                
                return exportResult.isSuccess();
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    private static void assertTest(String testName, TestCondition condition) {
        testCount.incrementAndGet();
        try {
            boolean result = condition.test();
            if (result) {
                passedTests.incrementAndGet();
                System.out.printf("  ✅ %s%n", testName);
            } else {
                System.out.printf("  ❌ %s%n", testName);
            }
        } catch (Exception e) {
            System.out.printf("  ❌ %s (Exception: %s)%n", testName, e.getMessage());
        }
    }
    
    @FunctionalInterface
    private interface TestCondition {
        boolean test() throws Exception;
    }
}