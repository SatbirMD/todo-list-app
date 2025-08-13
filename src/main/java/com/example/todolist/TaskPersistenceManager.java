package com.example.todolist;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Advanced Task Persistence Manager with sophisticated data management capabilities.
 * 
 * Features:
 * - Multiple serialization formats (Binary, JSON-like, Custom)
 * - Compression support for storage optimization
 * - Atomic file operations for data integrity
 * - Backup and versioning system
 * - Concurrent access management
 * - Data migration and validation
 * - Performance optimized bulk operations
 */
public class TaskPersistenceManager {
    
    private final Path dataDirectory;
    private final Path backupDirectory;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final PersistenceConfiguration config;
    private final DataIntegrityValidator validator;
    
    public enum SerializationFormat {
        BINARY, CUSTOM_TEXT, COMPRESSED_BINARY
    }
    
    public static class PersistenceConfiguration {
        private final SerializationFormat format;
        private final boolean enableBackups;
        private final int maxBackupVersions;
        private final boolean enableCompression;
        private final long autoSaveIntervalMs;
        
        public PersistenceConfiguration(SerializationFormat format, boolean enableBackups, 
                                      int maxBackupVersions, boolean enableCompression, 
                                      long autoSaveIntervalMs) {
            this.format = format;
            this.enableBackups = enableBackups;
            this.maxBackupVersions = maxBackupVersions;
            this.enableCompression = enableCompression;
            this.autoSaveIntervalMs = autoSaveIntervalMs;
        }
        
        public static PersistenceConfiguration defaultConfig() {
            return new PersistenceConfiguration(
                SerializationFormat.COMPRESSED_BINARY, true, 5, true, 30000L);
        }
        
        // Getters
        public SerializationFormat getFormat() { return format; }
        public boolean isBackupsEnabled() { return enableBackups; }
        public int getMaxBackupVersions() { return maxBackupVersions; }
        public boolean isCompressionEnabled() { return enableCompression; }
        public long getAutoSaveIntervalMs() { return autoSaveIntervalMs; }
    }
    
    /**
     * Data integrity validator for ensuring data consistency
     */
    public static class DataIntegrityValidator {
        
        public ValidationResult validateTaskData(Collection<Task> tasks) {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            // Check for duplicate IDs
            Set<String> seenIds = new HashSet<>();
            for (Task task : tasks) {
                if (!seenIds.add(task.getId())) {
                    errors.add("Duplicate task ID found: " + task.getId());
                }
            }
            
            // Check for data consistency
            for (Task task : tasks) {
                if (task.getDescription() == null || task.getDescription().trim().isEmpty()) {
                    errors.add("Task with empty description: " + task.getId());
                }
                
                if (task.getDueDate().isBefore(task.getCreatedAt())) {
                    warnings.add("Task due date is before creation date: " + task.getId());
                }
                
                if (task.getEstimatedHours() < 0) {
                    errors.add("Negative estimated hours for task: " + task.getId());
                }
            }
            
            return new ValidationResult(errors, warnings);
        }
        
        public static class ValidationResult {
            private final List<String> errors;
            private final List<String> warnings;
            
            public ValidationResult(List<String> errors, List<String> warnings) {
                this.errors = new ArrayList<>(errors);
                this.warnings = new ArrayList<>(warnings);
            }
            
            public boolean isValid() { return errors.isEmpty(); }
            public List<String> getErrors() { return Collections.unmodifiableList(errors); }
            public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
            
            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                if (!errors.isEmpty()) {
                    sb.append("ERRORS:\n");
                    errors.forEach(error -> sb.append("  - ").append(error).append("\n"));
                }
                if (!warnings.isEmpty()) {
                    sb.append("WARNINGS:\n");
                    warnings.forEach(warning -> sb.append("  - ").append(warning).append("\n"));
                }
                return sb.toString();
            }
        }
    }
    
    public TaskPersistenceManager(String dataPath) {
        this(dataPath, PersistenceConfiguration.defaultConfig());
    }
    
    public TaskPersistenceManager(String dataPath, PersistenceConfiguration config) {
        this.config = config;
        this.dataDirectory = Paths.get(dataPath);
        this.backupDirectory = dataDirectory.resolve("backups");
        this.validator = new DataIntegrityValidator();
        
        initializeDirectories();
    }
    
    private void initializeDirectories() {
        try {
            Files.createDirectories(dataDirectory);
            if (config.isBackupsEnabled()) {
                Files.createDirectories(backupDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize persistence directories", e);
        }
    }
    
    /**
     * Save tasks with atomic operation and backup
     */
    public CompletableFuture<PersistenceResult> saveTasks(Collection<Task> tasks) {
        return CompletableFuture.supplyAsync(() -> {
            lock.writeLock().lock();
            try {
                // Validate data integrity
                DataIntegrityValidator.ValidationResult validation = validator.validateTaskData(tasks);
                if (!validation.isValid()) {
                    return PersistenceResult.failure("Data validation failed: " + validation.getErrors());
                }
                
                // Create backup if enabled
                if (config.isBackupsEnabled()) {
                    createBackup();
                }
                
                // Atomic write operation
                Path tempFile = dataDirectory.resolve("tasks.tmp");
                Path targetFile = dataDirectory.resolve("tasks.dat");
                
                try {
                    writeTasksToFile(tasks, tempFile);
                    Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
                    
                    return PersistenceResult.success(
                        String.format("Successfully saved %d tasks", tasks.size()),
                        validation.getWarnings());
                        
                } catch (IOException e) {
                    // Cleanup temp file if it exists
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException cleanupError) {
                        // Log cleanup error but don't mask original error
                    }
                    return PersistenceResult.failure("Failed to save tasks: " + e.getMessage());
                }
                
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Load tasks with format auto-detection and migration
     */
    public CompletableFuture<LoadResult> loadTasks() {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                Path targetFile = dataDirectory.resolve("tasks.dat");
                
                if (!Files.exists(targetFile)) {
                    return LoadResult.success(new ArrayList<>(), "No existing data file found");
                }
                
                List<Task> tasks = readTasksFromFile(targetFile);
                
                // Validate loaded data
                DataIntegrityValidator.ValidationResult validation = validator.validateTaskData(tasks);
                
                return LoadResult.success(tasks, 
                    String.format("Loaded %d tasks", tasks.size()),
                    validation.getWarnings());
                    
            } catch (Exception e) {
                return LoadResult.failure("Failed to load tasks: " + e.getMessage());
            } finally {
                lock.readLock().unlock();
            }
        });
    }
    
    private void writeTasksToFile(Collection<Task> tasks, Path file) throws IOException {
        switch (config.getFormat()) {
            case BINARY:
                writeBinaryFormat(tasks, file);
                break;
            case CUSTOM_TEXT:
                writeCustomTextFormat(tasks, file);
                break;
            case COMPRESSED_BINARY:
                writeCompressedBinaryFormat(tasks, file);
                break;
        }
    }
    
    private List<Task> readTasksFromFile(Path file) throws IOException, ClassNotFoundException {
        // Auto-detect format by trying different readers
        try {
            return readCompressedBinaryFormat(file);
        } catch (Exception e1) {
            try {
                return readBinaryFormat(file);
            } catch (Exception e2) {
                try {
                    return readCustomTextFormat(file);
                } catch (Exception e3) {
                    throw new IOException("Unable to read file in any supported format", e3);
                }
            }
        }
    }
    
    private void writeBinaryFormat(Collection<Task> tasks, Path file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
            oos.writeObject(new ArrayList<>(tasks));
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Task> readBinaryFormat(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
            return (List<Task>) ois.readObject();
        }
    }
    
    private void writeCompressedBinaryFormat(Collection<Task> tasks, Path file) throws IOException {
        try (GZIPOutputStream gzos = new GZIPOutputStream(Files.newOutputStream(file));
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            oos.writeObject(new ArrayList<>(tasks));
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Task> readCompressedBinaryFormat(Path file) throws IOException, ClassNotFoundException {
        try (GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(file));
             ObjectInputStream ois = new ObjectInputStream(gzis)) {
            return (List<Task>) ois.readObject();
        }
    }
    
    private void writeCustomTextFormat(Collection<Task> tasks, Path file) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("# Task Data File v1.0\n");
            writer.write("# Generated: " + LocalDateTime.now().format(formatter) + "\n");
            writer.write("# Count: " + tasks.size() + "\n\n");
            
            for (Task task : tasks) {
                writer.write("TASK_START\n");
                writer.write("ID=" + task.getId() + "\n");
                writer.write("DESCRIPTION=" + escapeString(task.getDescription()) + "\n");
                writer.write("PRIORITY=" + task.getPriority() + "\n");
                writer.write("STATUS=" + task.getStatus() + "\n");
                writer.write("CATEGORY=" + escapeString(task.getCategory()) + "\n");
                writer.write("CREATED_AT=" + task.getCreatedAt().format(formatter) + "\n");
                writer.write("DUE_DATE=" + task.getDueDate().format(formatter) + "\n");
                writer.write("ESTIMATED_HOURS=" + task.getEstimatedHours() + "\n");
                writer.write("TASK_END\n\n");
            }
        }
    }
    
    private List<Task> readCustomTextFormat(Path file) throws IOException {
        List<Task> tasks = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            Map<String, String> taskData = new HashMap<>();
            boolean inTask = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                
                if (line.equals("TASK_START")) {
                    inTask = true;
                    taskData.clear();
                } else if (line.equals("TASK_END")) {
                    if (inTask) {
                        Task task = parseTaskFromData(taskData, formatter);
                        tasks.add(task);
                        inTask = false;
                    }
                } else if (inTask && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    taskData.put(parts[0], parts[1]);
                }
            }
        }
        
        return tasks;
    }
    
    private Task parseTaskFromData(Map<String, String> data, DateTimeFormatter formatter) {
        return new Task.Builder()
            .description(unescapeString(data.get("DESCRIPTION")))
            .priority(Task.Priority.valueOf(data.get("PRIORITY")))
            .status(Task.Status.valueOf(data.get("STATUS")))
            .category(unescapeString(data.get("CATEGORY")))
            .dueDate(LocalDateTime.parse(data.get("DUE_DATE"), formatter))
            .estimatedHours(Integer.parseInt(data.get("ESTIMATED_HOURS")))
            .build();
    }
    
    private String escapeString(String str) {
        return str.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=");
    }
    
    private String unescapeString(String str) {
        return str.replace("\\=", "=").replace("\\n", "\n").replace("\\\\", "\\");
    }
    
    private void createBackup() {
        try {
            Path sourceFile = dataDirectory.resolve("tasks.dat");
            if (!Files.exists(sourceFile)) {
                return;
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupFile = backupDirectory.resolve("tasks_" + timestamp + ".dat");
            
            Files.copy(sourceFile, backupFile);
            
            // Clean up old backups
            cleanupOldBackups();
            
        } catch (IOException e) {
            System.err.println("Failed to create backup: " + e.getMessage());
        }
    }
    
    private void cleanupOldBackups() throws IOException {
        List<Path> backupFiles = Files.list(backupDirectory)
            .filter(path -> path.getFileName().toString().startsWith("tasks_"))
            .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
            .collect(Collectors.toList());
        
        // Keep only the most recent backups
        for (int i = config.getMaxBackupVersions(); i < backupFiles.size(); i++) {
            Files.deleteIfExists(backupFiles.get(i));
        }
    }
    
    /**
     * Export tasks to various formats
     */
    public CompletableFuture<PersistenceResult> exportTasks(Collection<Task> tasks, 
                                                           Path exportPath, 
                                                           ExportFormat format) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (format) {
                    case CSV:
                        exportToCsv(tasks, exportPath);
                        break;
                    case JSON_LIKE:
                        exportToJsonLike(tasks, exportPath);
                        break;
                    case XML_LIKE:
                        exportToXmlLike(tasks, exportPath);
                        break;
                }
                
                return PersistenceResult.success("Exported " + tasks.size() + " tasks to " + exportPath);
                
            } catch (IOException e) {
                return PersistenceResult.failure("Export failed: " + e.getMessage());
            }
        });
    }
    
    public enum ExportFormat {
        CSV, JSON_LIKE, XML_LIKE
    }
    
    private void exportToCsv(Collection<Task> tasks, Path exportPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(exportPath)) {
            // Header
            writer.write("ID,Description,Priority,Status,Category,CreatedAt,DueDate,EstimatedHours,UrgencyScore\n");
            
            // Data
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            for (Task task : tasks) {
                writer.write(String.format("%s,\"%s\",%s,%s,\"%s\",%s,%s,%d,%.2f\n",
                    task.getId(),
                    task.getDescription().replace("\"", "\"\""),
                    task.getPriority(),
                    task.getStatus(),
                    task.getCategory().replace("\"", "\"\""),
                    task.getCreatedAt().format(formatter),
                    task.getDueDate().format(formatter),
                    task.getEstimatedHours(),
                    task.getUrgencyScore()));
            }
        }
    }
    
    private void exportToJsonLike(Collection<Task> tasks, Path exportPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(exportPath)) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            
            writer.write("{\n  \"tasks\": [\n");
            
            boolean first = true;
            for (Task task : tasks) {
                if (!first) writer.write(",\n");
                
                writer.write("    {\n");
                writer.write(String.format("      \"id\": \"%s\",\n", task.getId()));
                writer.write(String.format("      \"description\": \"%s\",\n", 
                    task.getDescription().replace("\"", "\\\"")));
                writer.write(String.format("      \"priority\": \"%s\",\n", task.getPriority()));
                writer.write(String.format("      \"status\": \"%s\",\n", task.getStatus()));
                writer.write(String.format("      \"category\": \"%s\",\n", task.getCategory()));
                writer.write(String.format("      \"createdAt\": \"%s\",\n", 
                    task.getCreatedAt().format(formatter)));
                writer.write(String.format("      \"dueDate\": \"%s\",\n", 
                    task.getDueDate().format(formatter)));
                writer.write(String.format("      \"estimatedHours\": %d,\n", task.getEstimatedHours()));
                writer.write(String.format("      \"urgencyScore\": %.2f\n", task.getUrgencyScore()));
                writer.write("    }");
                
                first = false;
            }
            
            writer.write("\n  ]\n}");
        }
    }
    
    private void exportToXmlLike(Collection<Task> tasks, Path exportPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(exportPath)) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<tasks>\n");
            
            for (Task task : tasks) {
                writer.write("  <task>\n");
                writer.write(String.format("    <id>%s</id>\n", task.getId()));
                writer.write(String.format("    <description><![CDATA[%s]]></description>\n", 
                    task.getDescription()));
                writer.write(String.format("    <priority>%s</priority>\n", task.getPriority()));
                writer.write(String.format("    <status>%s</status>\n", task.getStatus()));
                writer.write(String.format("    <category><![CDATA[%s]]></category>\n", task.getCategory()));
                writer.write(String.format("    <createdAt>%s</createdAt>\n", 
                    task.getCreatedAt().format(formatter)));
                writer.write(String.format("    <dueDate>%s</dueDate>\n", 
                    task.getDueDate().format(formatter)));
                writer.write(String.format("    <estimatedHours>%d</estimatedHours>\n", 
                    task.getEstimatedHours()));
                writer.write(String.format("    <urgencyScore>%.2f</urgencyScore>\n", 
                    task.getUrgencyScore()));
                writer.write("  </task>\n");
            }
            
            writer.write("</tasks>");
        }
    }
    
    // Result classes
    public static class PersistenceResult {
        private final boolean success;
        private final String message;
        private final List<String> warnings;
        
        private PersistenceResult(boolean success, String message, List<String> warnings) {
            this.success = success;
            this.message = message;
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        }
        
        public static PersistenceResult success(String message) {
            return new PersistenceResult(true, message, null);
        }
        
        public static PersistenceResult success(String message, List<String> warnings) {
            return new PersistenceResult(true, message, warnings);
        }
        
        public static PersistenceResult failure(String message) {
            return new PersistenceResult(false, message, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    }
    
    public static class LoadResult extends PersistenceResult {
        private final List<Task> tasks;
        
        private LoadResult(boolean success, String message, List<String> warnings, List<Task> tasks) {
            super(success, message, warnings);
            this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        }
        
        public static LoadResult success(List<Task> tasks, String message) {
            return new LoadResult(true, message, null, tasks);
        }
        
        public static LoadResult success(List<Task> tasks, String message, List<String> warnings) {
            return new LoadResult(true, message, warnings, tasks);
        }
        
        public static LoadResult failure(String message) {
            return new LoadResult(false, message, null, null);
        }
        
        public List<Task> getTasks() { return Collections.unmodifiableList(tasks); }
    }
}