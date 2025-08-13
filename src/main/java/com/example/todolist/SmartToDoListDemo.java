package com.example.todolist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simple test class to demonstrate the smart Java features working correctly.
 * This shows advanced programming concepts in action.
 */
public class SmartToDoListDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Smart To-Do List System Demo");
        System.out.println("=" + "=".repeat(50));
        
        try {
            demoBasicTaskOperations();
            demoAdvancedTaskManagement();
            demoTaskProcessing();
            demoPersistence();
            demoAnalytics();
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demoBasicTaskOperations() throws Exception {
        System.out.println("\n📝 Demo: Basic Task Operations");
        System.out.println("-".repeat(40));
        
        // Create sophisticated tasks using Builder pattern
        Task criticalTask = new Task.Builder()
            .description("Implement quantum computing algorithm")
            .priority(Task.Priority.CRITICAL)
            .category("Research")
            .estimatedHours(8)
            .dueDate(LocalDateTime.now().plusDays(2))
            .build();
        
        Task mediumTask = new Task.Builder()
            .description("Optimize database performance")
            .priority(Task.Priority.MEDIUM)
            .category("Development")
            .estimatedHours(4)
            .dueDate(LocalDateTime.now().plusDays(5))
            .build();
        
        // Demonstrate immutability and functional programming
        Task updatedTask = criticalTask.withStatus(Task.Status.IN_PROGRESS);
        
        System.out.printf("✅ Created critical task: %s%n", criticalTask.getDescription());
        System.out.printf("✅ Created medium task: %s%n", mediumTask.getDescription());
        System.out.printf("🔄 Updated task status: %s -> %s%n", 
            criticalTask.getStatus(), updatedTask.getStatus());
        
        // Show advanced calculations
        System.out.printf("🎯 Critical task urgency score: %.2f%n", criticalTask.getUrgencyScore());
        System.out.printf("📅 Days until due: %d%n", criticalTask.getDaysUntilDue());
        System.out.printf("⚠️ Is overdue: %s%n", criticalTask.isOverdue());
    }
    
    private static void demoAdvancedTaskManagement() throws Exception {
        System.out.println("\n🧠 Demo: Advanced Task Management");
        System.out.println("-".repeat(40));
        
        TaskManager manager = new TaskManager();
        
        // Add tasks using Factory pattern
        Task urgentTask = TaskManager.TaskFactory.createUrgentTask("Fix critical security vulnerability");
        Task quickTask = TaskManager.TaskFactory.createQuickTask("Update documentation");
        Task longTermTask = TaskManager.TaskFactory.createLongTermTask("Migrate to microservices", "Architecture");
        
        // Async operations with CompletableFuture
        CompletableFuture.allOf(
            manager.addTask(urgentTask),
            manager.addTask(quickTask),
            manager.addTask(longTermTask)
        ).get();
        
        System.out.printf("📊 Total tasks managed: %d%n", manager.getTaskCount());
        
        // Demonstrate different sorting strategies
        System.out.println("\n🎯 Tasks by urgency (ML-inspired algorithm):");
        List<Task> urgentTasks = manager.getTasksFiltered(
            task -> true, 
            new TaskManager.UrgencySortingStrategy()
        );
        
        urgentTasks.forEach(task -> 
            System.out.printf("  - %s (Score: %.2f)%n", 
                task.getDescription(), task.getUrgencyScore()));
        
        // Smart recommendations
        List<Task> recommended = manager.getRecommendedTasks(3);
        System.out.println("\n🧠 Smart recommendations:");
        recommended.forEach(task -> 
            System.out.printf("  - %s%n", task.getDescription()));
        
        manager.shutdown();
    }
    
    private static void demoTaskProcessing() throws Exception {
        System.out.println("\n⚡ Demo: Concurrent Task Processing");
        System.out.println("-".repeat(40));
        
        TaskProcessor processor = new TaskProcessor();
        
        // Create demo tasks
        Task task1 = TaskManager.TaskFactory.createUrgentTask("Process big data analytics");
        Task task2 = TaskManager.TaskFactory.createQuickTask("Generate performance report");
        
        // Demo executor that simulates work
        TaskProcessor.TaskExecutor demoExecutor = task -> {
            System.out.printf("🔄 Processing: %s%n", task.getDescription());
            Thread.sleep(100); // Simulate work
            return TaskProcessor.TaskExecutionResult.success(task, 100);
        };
        
        // Execute tasks concurrently
        CompletableFuture<TaskProcessor.TaskExecutionResult> future1 = 
            processor.executeTask(task1, demoExecutor);
        CompletableFuture<TaskProcessor.TaskExecutionResult> future2 = 
            processor.executeTask(task2, demoExecutor);
        
        TaskProcessor.TaskExecutionResult result1 = future1.get();
        TaskProcessor.TaskExecutionResult result2 = future2.get();
        
        System.out.printf("✅ Task 1 success: %s (%dms)%n", 
            result1.isSuccess(), result1.getExecutionTime());
        System.out.printf("✅ Task 2 success: %s (%dms)%n", 
            result2.isSuccess(), result2.getExecutionTime());
        
        // Show performance metrics
        TaskProcessor.ProcessorPerformanceReport report = processor.getPerformanceReport();
        System.out.println("\n📊 Performance Report:");
        System.out.println(report.toString());
        
        processor.shutdown();
    }
    
    private static void demoPersistence() throws Exception {
        System.out.println("\n💾 Demo: Advanced Persistence");
        System.out.println("-".repeat(40));
        
        TaskPersistenceManager persistence = new TaskPersistenceManager("./demo_data");
        
        // Create test tasks
        List<Task> tasks = List.of(
            TaskManager.TaskFactory.createUrgentTask("Backup production database"),
            TaskManager.TaskFactory.createQuickTask("Review code changes"),
            new Task.Builder()
                .description("Design neural network architecture")
                .priority(Task.Priority.HIGH)
                .category("AI/ML")
                .estimatedHours(12)
                .build()
        );
        
        // Save with compression and validation
        TaskPersistenceManager.PersistenceResult saveResult = 
            persistence.saveTasks(tasks).get();
        
        System.out.printf("💾 Save result: %s%n", saveResult.getMessage());
        
        // Load back
        TaskPersistenceManager.LoadResult loadResult = 
            persistence.loadTasks().get();
        
        if (loadResult.isSuccess()) {
            System.out.printf("📂 Loaded %d tasks successfully%n", loadResult.getTasks().size());
        }
        
        // Export to different formats
        persistence.exportTasks(tasks, 
            java.nio.file.Paths.get("./demo_export.csv"), 
            TaskPersistenceManager.ExportFormat.CSV).get();
        
        System.out.println("📤 Exported tasks to CSV format");
    }
    
    private static void demoAnalytics() throws Exception {
        System.out.println("\n📊 Demo: Smart Analytics");
        System.out.println("-".repeat(40));
        
        TaskManager manager = new TaskManager();
        
        // Create diverse task portfolio
        manager.addTask(TaskManager.TaskFactory.createUrgentTask("Fix production bug")).get();
        manager.addTask(TaskManager.TaskFactory.createQuickTask("Update README")).get();
        
        Task completedTask = new Task.Builder()
            .description("Implement OAuth integration")
            .priority(Task.Priority.HIGH)
            .category("Security")
            .status(Task.Status.COMPLETED)
            .build();
        manager.addTask(completedTask).get();
        
        // Generate analytics
        TaskManager.TaskAnalytics analytics = manager.getAnalytics();
        
        System.out.printf("📈 Productivity Score: %.1f%%\n", analytics.getProductivityScore());
        System.out.printf("⏱️ Average Completion Time: %.1f hours\n", analytics.getAverageCompletionTime());
        
        System.out.println("\n🎯 Task Distribution by Priority:");
        analytics.getTaskCountByPriority().forEach((priority, count) ->
            System.out.printf("  %s: %d tasks\n", priority, count));
        
        System.out.println("\n📁 Task Distribution by Category:");
        analytics.getTaskCountByCategory().forEach((category, count) ->
            System.out.printf("  %s: %d tasks\n", category, count));
        
        // High-risk prediction
        List<Task> highRisk = analytics.getPredictedHighRiskTasks();
        if (!highRisk.isEmpty()) {
            System.out.println("\n🚨 Predicted High-Risk Tasks:");
            highRisk.forEach(task ->
                System.out.printf("  - %s (Score: %.2f)\n", 
                    task.getDescription(), task.getUrgencyScore()));
        }
        
        manager.shutdown();
    }
}