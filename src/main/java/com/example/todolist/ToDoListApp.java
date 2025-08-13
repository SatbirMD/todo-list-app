package com.example.todolist;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Enhanced To-Do List Application with sophisticated task management capabilities.
 * 
 * Features:
 * - Advanced task management with priority scheduling
 * - Smart task recommendations using ML-inspired algorithms
 * - Concurrent task processing and analytics
 * - Data persistence with multiple formats
 * - Real-time performance monitoring
 * - Interactive command-line interface with rich functionality
 */
public class ToDoListApp {
    
    private final TaskManager taskManager;
    private final TaskProcessor taskProcessor;
    private final TaskPersistenceManager persistenceManager;
    private final Scanner scanner;
    private final SmartTaskRecommendationEngine recommendationEngine;
    
    // Application state
    private boolean running = true;
    private TaskManager.SortingStrategy currentSortingStrategy = new TaskManager.UrgencySortingStrategy();
    
    public ToDoListApp() {
        this.taskManager = new TaskManager();
        this.taskProcessor = new TaskProcessor();
        this.persistenceManager = new TaskPersistenceManager("./data");
        this.scanner = new Scanner(System.in);
        this.recommendationEngine = new SmartTaskRecommendationEngine();
        
        // Register observers
        setupTaskObservers();
        
        // Load existing tasks
        loadTasksOnStartup();
    }
    
    private void setupTaskObservers() {
        taskManager.addObserver(new TaskManager.TaskObserver() {
            @Override
            public void onTaskAdded(Task task) {
                System.out.printf("📝 Task added: %s (Priority: %s)%n", 
                    task.getDescription(), task.getPriority());
            }
            
            @Override
            public void onTaskUpdated(Task oldTask, Task newTask) {
                System.out.printf("🔄 Task updated: %s -> Status: %s%n", 
                    newTask.getDescription(), newTask.getStatus());
            }
            
            @Override
            public void onTaskRemoved(Task task) {
                System.out.printf("🗑️ Task removed: %s%n", task.getDescription());
            }
            
            @Override
            public void onTaskCompleted(Task task) {
                System.out.printf("✅ Task completed: %s (Urgency Score: %.2f)%n", 
                    task.getDescription(), task.getUrgencyScore());
            }
        });
    }
    
    private void loadTasksOnStartup() {
        try {
            CompletableFuture<TaskPersistenceManager.LoadResult> loadFuture = persistenceManager.loadTasks();
            TaskPersistenceManager.LoadResult result = loadFuture.get();
            
            if (result.isSuccess()) {
                for (Task task : result.getTasks()) {
                    taskManager.addTask(task).get();
                }
                System.out.printf("📂 Loaded %d tasks from storage%n", result.getTasks().size());
                
                if (!result.getWarnings().isEmpty()) {
                    System.out.println("⚠️ Warnings during load:");
                    result.getWarnings().forEach(warning -> System.out.println("  - " + warning));
                }
            } else {
                System.out.println("📂 No existing tasks found, starting fresh");
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading tasks: " + e.getMessage());
        }
    }
    
    public void run() {
        System.out.println("🚀 Welcome to the Advanced To-Do List Application!");
        System.out.println("================================================================");
        
        while (running) {
            displayMainMenu();
            int choice = getIntInput("Enter your choice: ");
            
            try {
                handleMenuChoice(choice);
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
        
        shutdown();
    }
    
    private void displayMainMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📋 MAIN MENU");
        System.out.println("=".repeat(60));
        System.out.println("1.  📝 Add Task");
        System.out.println("2.  📝 Add Quick Task");
        System.out.println("3.  📝 Add Urgent Task");
        System.out.println("4.  🗑️  Remove Task");
        System.out.println("5.  📋 List All Tasks");
        System.out.println("6.  🎯 List Tasks by Priority");
        System.out.println("7.  📁 List Tasks by Category");
        System.out.println("8.  ⚠️  Show Overdue Tasks");
        System.out.println("9.  🔄 Update Task Status");
        System.out.println("10. 🧠 Smart Recommendations");
        System.out.println("11. 📊 Analytics Dashboard");
        System.out.println("12. ⚡ Process High Priority Tasks");
        System.out.println("13. 💾 Save Tasks");
        System.out.println("14. 📤 Export Tasks");
        System.out.println("15. ⚙️  Settings");
        System.out.println("16. 🔍 Search Tasks");
        System.out.println("0.  🚪 Quit");
        System.out.println("=".repeat(60));
        
        // Show quick stats
        int totalTasks = taskManager.getTaskCount();
        long pendingTasks = taskManager.getAllTasks().stream()
            .mapToLong(task -> task.getStatus() == Task.Status.PENDING ? 1 : 0)
            .sum();
        long overdueTasks = taskManager.getOverdueTasks().size();
        
        System.out.printf("📊 Quick Stats: %d total, %d pending, %d overdue%n", 
            totalTasks, pendingTasks, overdueTasks);
    }
    
    private void handleMenuChoice(int choice) throws Exception {
        switch (choice) {
            case 1 -> addAdvancedTask();
            case 2 -> addQuickTask();
            case 3 -> addUrgentTask();
            case 4 -> removeTask();
            case 5 -> listAllTasks();
            case 6 -> listTasksByPriority();
            case 7 -> listTasksByCategory();
            case 8 -> showOverdueTasks();
            case 9 -> updateTaskStatus();
            case 10 -> showSmartRecommendations();
            case 11 -> showAnalyticsDashboard();
            case 12 -> processHighPriorityTasks();
            case 13 -> saveTasks();
            case 14 -> exportTasks();
            case 15 -> showSettings();
            case 16 -> searchTasks();
            case 0 -> running = false;
            default -> System.out.println("❌ Invalid choice. Please try again.");
        }
    }
    
    private void addAdvancedTask() throws Exception {
        System.out.println("\n📝 Creating Advanced Task");
        System.out.println("-".repeat(30));
        
        String description = getStringInput("Task description: ");
        
        System.out.println("Priority levels: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL");
        int priorityLevel = getIntInputWithRange("Priority (1-4): ", 1, 4);
        Task.Priority priority = Task.Priority.values()[priorityLevel - 1];
        
        String category = getStringInput("Category (or press Enter for 'General'): ");
        if (category.trim().isEmpty()) {
            category = "General";
        }
        
        int estimatedHours = getIntInputWithRange("Estimated hours (1-24): ", 1, 24);
        
        LocalDateTime dueDate = getDueDateInput();
        
        Task task = new Task.Builder()
            .description(description)
            .priority(priority)
            .category(category)
            .estimatedHours(estimatedHours)
            .dueDate(dueDate)
            .build();
        
        taskManager.addTask(task).get();
        System.out.printf("✅ Task created with ID: %s%n", task.getId().substring(0, 8));
    }
    
    private void addQuickTask() throws Exception {
        String description = getStringInput("Quick task description: ");
        Task task = TaskManager.TaskFactory.createQuickTask(description);
        taskManager.addTask(task).get();
        System.out.println("✅ Quick task added!");
    }
    
    private void addUrgentTask() throws Exception {
        String description = getStringInput("Urgent task description: ");
        Task task = TaskManager.TaskFactory.createUrgentTask(description);
        taskManager.addTask(task).get();
        System.out.println("🚨 Urgent task added!");
    }
    
    private void removeTask() throws Exception {
        if (taskManager.getTaskCount() == 0) {
            System.out.println("📭 No tasks to remove.");
            return;
        }
        
        listAllTasks();
        String taskId = getStringInput("Enter task ID to remove (first 8 characters): ");
        
        // Find task by partial ID
        Optional<Task> taskToRemove = taskManager.getAllTasks().stream()
            .filter(task -> task.getId().startsWith(taskId))
            .findFirst();
        
        if (taskToRemove.isPresent()) {
            taskManager.removeTask(taskToRemove.get().getId()).get();
            System.out.println("✅ Task removed successfully!");
        } else {
            System.out.println("❌ Task not found.");
        }
    }
    
    private void listAllTasks() {
        List<Task> tasks = taskManager.getTasksFiltered(task -> true, currentSortingStrategy);
        displayTaskList("All Tasks", tasks);
    }
    
    private void listTasksByPriority() {
        System.out.println("Priority levels: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL");
        int priorityLevel = getIntInputWithRange("Select priority (1-4): ", 1, 4);
        Task.Priority priority = Task.Priority.values()[priorityLevel - 1];
        
        List<Task> tasks = taskManager.getTasksByPriority(priority);
        displayTaskList(priority + " Priority Tasks", tasks);
    }
    
    private void listTasksByCategory() {
        // Show available categories
        Set<String> categories = taskManager.getAllTasks().stream()
            .map(Task::getCategory)
            .collect(Collectors.toSet());
        
        if (categories.isEmpty()) {
            System.out.println("📭 No categories found.");
            return;
        }
        
        System.out.println("Available categories:");
        categories.forEach(cat -> System.out.println("  - " + cat));
        
        String category = getStringInput("Enter category: ");
        List<Task> tasks = taskManager.getTasksByCategory(category);
        displayTaskList("Tasks in '" + category + "'", tasks);
    }
    
    private void showOverdueTasks() {
        List<Task> overdueTasks = taskManager.getOverdueTasks();
        displayTaskList("⚠️ Overdue Tasks", overdueTasks);
        
        if (!overdueTasks.isEmpty()) {
            System.out.printf("💡 Tip: %d tasks need immediate attention!%n", overdueTasks.size());
        }
    }
    
    private void updateTaskStatus() throws Exception {
        if (taskManager.getTaskCount() == 0) {
            System.out.println("📭 No tasks to update.");
            return;
        }
        
        listAllTasks();
        String taskId = getStringInput("Enter task ID to update (first 8 characters): ");
        
        // Find task by partial ID
        Optional<Task> taskToUpdate = taskManager.getAllTasks().stream()
            .filter(task -> task.getId().startsWith(taskId))
            .findFirst();
        
        if (taskToUpdate.isEmpty()) {
            System.out.println("❌ Task not found.");
            return;
        }
        
        System.out.println("Status options: 1=PENDING, 2=IN_PROGRESS, 3=COMPLETED, 4=ARCHIVED");
        int statusChoice = getIntInputWithRange("Select new status (1-4): ", 1, 4);
        Task.Status newStatus = Task.Status.values()[statusChoice - 1];
        
        taskManager.updateTaskStatus(taskToUpdate.get().getId(), newStatus).get();
        System.out.println("✅ Task status updated!");
    }
    
    private void showSmartRecommendations() {
        System.out.println("\n🧠 Smart Task Recommendations");
        System.out.println("-".repeat(40));
        
        List<Task> recommended = taskManager.getRecommendedTasks(5);
        
        if (recommended.isEmpty()) {
            System.out.println("🎉 No pending tasks found!");
            return;
        }
        
        System.out.println("Based on urgency, priority, and deadlines:");
        for (int i = 0; i < recommended.size(); i++) {
            Task task = recommended.get(i);
            System.out.printf("%d. %s (Score: %.2f, Due: %s)%n",
                i + 1,
                task.getDescription(),
                task.getUrgencyScore(),
                task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        }
        
        System.out.println("\n💡 Consider working on these tasks in order!");
    }
    
    private void showAnalyticsDashboard() {
        System.out.println("\n📊 Analytics Dashboard");
        System.out.println("=".repeat(50));
        
        TaskManager.TaskAnalytics analytics = taskManager.getAnalytics();
        
        // Overall statistics
        System.out.printf("📈 Productivity Score: %.1f%%%n", analytics.getProductivityScore());
        System.out.printf("⏱️ Average Completion Time: %.1f hours%n", analytics.getAverageCompletionTime());
        
        // Priority distribution
        System.out.println("\n🎯 Tasks by Priority:");
        analytics.getTaskCountByPriority().forEach((priority, count) ->
            System.out.printf("  %s: %d tasks%n", priority, count));
        
        // Category distribution
        System.out.println("\n📁 Tasks by Category:");
        analytics.getTaskCountByCategory().forEach((category, count) ->
            System.out.printf("  %s: %d tasks%n", category, count));
        
        // High-risk tasks
        List<Task> highRiskTasks = analytics.getPredictedHighRiskTasks();
        if (!highRiskTasks.isEmpty()) {
            System.out.println("\n🚨 High Risk Tasks (May Miss Deadline):");
            highRiskTasks.stream().limit(3).forEach(task ->
                System.out.printf("  - %s (Due in %d days, Score: %.2f)%n",
                    task.getDescription(), task.getDaysUntilDue(), task.getUrgencyScore()));
        }
        
        // Processor performance
        TaskProcessor.ProcessorPerformanceReport perfReport = taskProcessor.getPerformanceReport();
        System.out.println("\n⚡ System Performance:");
        System.out.println(perfReport.toString());
    }
    
    private void processHighPriorityTasks() throws Exception {
        System.out.println("\n⚡ Processing High Priority Tasks");
        System.out.println("-".repeat(40));
        
        // Demo task executor
        TaskProcessor.TaskExecutor demoExecutor = task -> {
            System.out.printf("🔄 Processing: %s...%n", task.getDescription());
            
            // Simulate processing time based on estimated hours
            long processingTime = task.getEstimatedHours() * 100; // 100ms per hour
            Thread.sleep(processingTime);
            
            return TaskProcessor.TaskExecutionResult.success(task, processingTime);
        };
        
        CompletableFuture<List<TaskProcessor.TaskExecutionResult>> processingFuture = 
            taskProcessor.smartSchedule(taskManager, demoExecutor);
        
        List<TaskProcessor.TaskExecutionResult> results = processingFuture.get();
        
        System.out.printf("✅ Processed %d tasks%n", results.size());
        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        System.out.printf("📊 Success rate: %d/%d (%.1f%%)%n", 
            successCount, results.size(), 
            results.isEmpty() ? 0 : (double) successCount / results.size() * 100);
    }
    
    private void saveTasks() throws Exception {
        System.out.println("\n💾 Saving tasks...");
        
        CompletableFuture<TaskPersistenceManager.PersistenceResult> saveFuture = 
            persistenceManager.saveTasks(taskManager.getAllTasks());
        
        TaskPersistenceManager.PersistenceResult result = saveFuture.get();
        
        if (result.isSuccess()) {
            System.out.println("✅ " + result.getMessage());
            if (!result.getWarnings().isEmpty()) {
                System.out.println("⚠️ Warnings:");
                result.getWarnings().forEach(warning -> System.out.println("  - " + warning));
            }
        } else {
            System.out.println("❌ " + result.getMessage());
        }
    }
    
    private void exportTasks() throws Exception {
        if (taskManager.getTaskCount() == 0) {
            System.out.println("📭 No tasks to export.");
            return;
        }
        
        System.out.println("\n📤 Export Tasks");
        System.out.println("Export formats: 1=CSV, 2=JSON-like, 3=XML-like");
        int formatChoice = getIntInputWithRange("Select format (1-3): ", 1, 3);
        
        TaskPersistenceManager.ExportFormat[] formats = TaskPersistenceManager.ExportFormat.values();
        TaskPersistenceManager.ExportFormat selectedFormat = formats[formatChoice - 1];
        
        String filename = String.format("tasks_export_%s.%s", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
            selectedFormat.name().toLowerCase().replace("_like", ""));
        
        CompletableFuture<TaskPersistenceManager.PersistenceResult> exportFuture = 
            persistenceManager.exportTasks(taskManager.getAllTasks(), 
                java.nio.file.Paths.get("./exports/" + filename), selectedFormat);
        
        TaskPersistenceManager.PersistenceResult result = exportFuture.get();
        
        if (result.isSuccess()) {
            System.out.println("✅ " + result.getMessage());
        } else {
            System.out.println("❌ " + result.getMessage());
        }
    }
    
    private void showSettings() {
        System.out.println("\n⚙️ Application Settings");
        System.out.println("-".repeat(30));
        System.out.println("1. Change Sorting Strategy");
        System.out.println("2. View Current Configuration");
        System.out.println("0. Back to Main Menu");
        
        int choice = getIntInput("Select option: ");
        
        switch (choice) {
            case 1 -> changeSortingStrategy();
            case 2 -> showCurrentConfiguration();
            case 0 -> { /* Return to main menu */ }
            default -> System.out.println("❌ Invalid choice.");
        }
    }
    
    private void changeSortingStrategy() {
        System.out.println("\nSorting strategies:");
        System.out.println("1. By Urgency Score (Smart)");
        System.out.println("2. By Priority Level");
        System.out.println("3. By Due Date");
        
        int choice = getIntInputWithRange("Select strategy (1-3): ", 1, 3);
        
        switch (choice) {
            case 1 -> {
                currentSortingStrategy = new TaskManager.UrgencySortingStrategy();
                System.out.println("✅ Sorting by urgency score (smart algorithm)");
            }
            case 2 -> {
                currentSortingStrategy = new TaskManager.PrioritySortingStrategy();
                System.out.println("✅ Sorting by priority level");
            }
            case 3 -> {
                currentSortingStrategy = new TaskManager.DueDateSortingStrategy();
                System.out.println("✅ Sorting by due date");
            }
        }
    }
    
    private void showCurrentConfiguration() {
        System.out.println("\n📋 Current Configuration:");
        System.out.println("  Sorting Strategy: " + currentSortingStrategy.getClass().getSimpleName());
        System.out.println("  Total Tasks: " + taskManager.getTaskCount());
        System.out.println("  Data Directory: ./data");
        System.out.println("  Backup Directory: ./data/backups");
        System.out.println("  Export Directory: ./exports");
    }
    
    private void searchTasks() {
        String searchTerm = getStringInput("Enter search term: ");
        
        List<Task> matchingTasks = taskManager.getAllTasks().stream()
            .filter(task -> task.getDescription().toLowerCase().contains(searchTerm.toLowerCase()) ||
                           task.getCategory().toLowerCase().contains(searchTerm.toLowerCase()))
            .sorted(currentSortingStrategy.getComparator())
            .collect(Collectors.toList());
        
        displayTaskList("Search Results for '" + searchTerm + "'", matchingTasks);
    }
    
    private void displayTaskList(String title, List<Task> tasks) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(Math.max(title.length(), 50)));
        
        if (tasks.isEmpty()) {
            System.out.println("📭 No tasks found.");
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            String status = getStatusIcon(task.getStatus());
            String priority = getPriorityIcon(task.getPriority());
            String overdue = task.isOverdue() ? " ⚠️" : "";
            
            System.out.printf("%d. %s %s [%s] %s%s%n", 
                i + 1, status, priority, task.getId().substring(0, 8), 
                task.getDescription(), overdue);
            System.out.printf("   📁 %s | 📅 Due: %s | ⏱️ %dh | 🎯 %.2f%n",
                task.getCategory(), task.getDueDate().format(formatter), 
                task.getEstimatedHours(), task.getUrgencyScore());
            
            if (i < tasks.size() - 1) {
                System.out.println();
            }
        }
    }
    
    private String getStatusIcon(Task.Status status) {
        return switch (status) {
            case PENDING -> "⏳";
            case IN_PROGRESS -> "🔄";
            case COMPLETED -> "✅";
            case ARCHIVED -> "📦";
        };
    }
    
    private String getPriorityIcon(Task.Priority priority) {
        return switch (priority) {
            case LOW -> "🟢";
            case MEDIUM -> "🟡";
            case HIGH -> "🟠";
            case CRITICAL -> "🔴";
        };
    }
    
    private LocalDateTime getDueDateInput() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        while (true) {
            String input = getStringInput("Due date (yyyy-MM-dd HH:mm, or 'tomorrow', 'next week'): ");
            
            try {
                return switch (input.toLowerCase()) {
                    case "tomorrow" -> LocalDateTime.now().plusDays(1).withHour(17).withMinute(0);
                    case "next week" -> LocalDateTime.now().plusWeeks(1).withHour(17).withMinute(0);
                    default -> LocalDateTime.parse(input, formatter);
                };
            } catch (DateTimeParseException e) {
                System.out.println("❌ Invalid date format. Please try again.");
            }
        }
    }
    
    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    private int getIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("❌ Please enter a valid number.");
            }
        }
    }
    
    private int getIntInputWithRange(String prompt, int min, int max) {
        while (true) {
            int value = getIntInput(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("❌ Please enter a number between %d and %d.%n", min, max);
        }
    }
    
    private void shutdown() {
        System.out.println("\n🔄 Shutting down...");
        
        // Auto-save before exit
        try {
            saveTasks();
        } catch (Exception e) {
            System.err.println("❌ Error saving tasks on exit: " + e.getMessage());
        }
        
        // Shutdown services
        taskManager.shutdown();
        taskProcessor.shutdown();
        
        System.out.println("👋 Goodbye! Thanks for using the Advanced To-Do List App!");
    }
    
    // Demo smart recommendation engine
    private static class SmartTaskRecommendationEngine {
        // Placeholder for more sophisticated ML algorithms
        // In a real implementation, this could include:
        // - User behavior analysis
        // - Task completion pattern recognition
        // - Workload optimization algorithms
        // - Integration with calendar systems
    }
    
    public static void main(String[] args) {
        try {
            // Create necessary directories
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("./data"));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("./exports"));
            
            ToDoListApp app = new ToDoListApp();
            app.run();
            
        } catch (Exception e) {
            System.err.println("❌ Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
