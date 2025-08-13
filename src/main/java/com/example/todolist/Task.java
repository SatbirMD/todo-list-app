package com.example.todolist;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Enhanced Task class implementing sophisticated object-oriented design principles.
 * Demonstrates immutability concepts, proper encapsulation, and advanced Java features.
 */
public class Task implements Comparable<Task>, Cloneable, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public enum Priority {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);
        
        private final int value;
        
        Priority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, ARCHIVED
    }
    
    private final String id;
    private final String description;
    private final Priority priority;
    private final LocalDateTime createdAt;
    private final LocalDateTime dueDate;
    private final Status status;
    private final String category;
    private final int estimatedHours;

    // Builder pattern for complex object construction
    public static class Builder {
        private String description;
        private Priority priority = Priority.MEDIUM;
        private LocalDateTime dueDate = LocalDateTime.now().plusDays(7);
        private Status status = Status.PENDING;
        private String category = "General";
        private int estimatedHours = 1;
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder dueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
            return this;
        }
        
        public Builder status(Status status) {
            this.status = status;
            return this;
        }
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Builder estimatedHours(int hours) {
            this.estimatedHours = hours;
            return this;
        }
        
        public Task build() {
            return new Task(this);
        }
    }
    
    private Task(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.description = Objects.requireNonNull(builder.description, "Description cannot be null");
        this.priority = builder.priority;
        this.createdAt = LocalDateTime.now();
        this.dueDate = builder.dueDate;
        this.status = builder.status;
        this.category = builder.category;
        this.estimatedHours = builder.estimatedHours;
    }
    
    // Legacy constructor for backward compatibility
    public Task(String description) {
        this.id = UUID.randomUUID().toString();
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.priority = Priority.MEDIUM;
        this.createdAt = LocalDateTime.now();
        this.dueDate = LocalDateTime.now().plusDays(7);
        this.status = Status.PENDING;
        this.category = "General";
        this.estimatedHours = 1;
    }

    // Getters with defensive copying where appropriate
    public String getId() { return id; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public LocalDateTime getCreatedAt() { return LocalDateTime.from(createdAt); }
    public LocalDateTime getDueDate() { return LocalDateTime.from(dueDate); }
    public Status getStatus() { return status; }
    public String getCategory() { return category; }
    public int getEstimatedHours() { return estimatedHours; }
    
    // Functional methods for creating modified copies (immutability pattern)
    public Task withStatus(Status newStatus) {
        return new Builder()
            .description(this.description)
            .priority(this.priority)
            .dueDate(this.dueDate)
            .status(newStatus)
            .category(this.category)
            .estimatedHours(this.estimatedHours)
            .build();
    }
    
    public Task withPriority(Priority newPriority) {
        return new Builder()
            .description(this.description)
            .priority(newPriority)
            .dueDate(this.dueDate)
            .status(this.status)
            .category(this.category)
            .estimatedHours(this.estimatedHours)
            .build();
    }
    
    // Advanced utility methods
    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(dueDate) && status != Status.COMPLETED;
    }
    
    public long getDaysUntilDue() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
    }
    
    public double getUrgencyScore() {
        long daysLeft = getDaysUntilDue();
        return priority.getValue() * (1.0 / Math.max(1, daysLeft)) * estimatedHours;
    }

    @Override
    public int compareTo(Task other) {
        // Compare by urgency score, then by priority, then by due date
        int urgencyComparison = Double.compare(other.getUrgencyScore(), this.getUrgencyScore());
        if (urgencyComparison != 0) return urgencyComparison;
        
        int priorityComparison = other.priority.compareTo(this.priority);
        if (priorityComparison != 0) return priorityComparison;
        
        return this.dueDate.compareTo(other.dueDate);
    }

    @Override
    public Task clone() {
        try {
            return (Task) super.clone();
        } catch (CloneNotSupportedException e) {
            // This should never happen since we implement Cloneable
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format("Task{id='%s', description='%s', priority=%s, status=%s, due=%s, urgency=%.2f}", 
            id.substring(0, 8), description, priority, status, 
            dueDate.format(formatter), getUrgencyScore());
    }
}
