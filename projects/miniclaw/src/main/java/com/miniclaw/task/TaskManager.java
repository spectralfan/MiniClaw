package com.miniclaw.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务管理系统
 */
public class TaskManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    
    private final ExecutorService executorService;
    private final ConcurrentHashMap<String, Task> tasks;
    private final AtomicInteger taskCounter;
    
    public TaskManager(int maxConcurrentTasks) {
        this.executorService = Executors.newFixedThreadPool(maxConcurrentTasks);
        this.tasks = new ConcurrentHashMap<>();
        this.taskCounter = new AtomicInteger(0);
    }
    
    public TaskManager() {
        this(5); // 默认 5 个并发任务
    }
    
    /**
     * 创建新任务
     */
    public Task createTask(String description, TaskExecutor executor) {
        String taskId = generateTaskId();
        Task task = new Task(taskId, description, executor);
        tasks.put(taskId, task);
        logger.info("Created task: {} - {}", taskId, description);
        return task;
    }
    
    /**
     * 异步执行任务
     */
    public Future<TaskResult> executeAsync(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        task.setState(TaskState.RUNNING);
        task.setStartTime(LocalDateTime.now());
        
        return executorService.submit(() -> {
            try {
                TaskResult result = task.getExecutor().execute(task);
                task.setState(TaskState.COMPLETED);
                task.setEndTime(LocalDateTime.now());
                task.setResult(result);
                logger.info("Task {} completed successfully", taskId);
                return result;
            } catch (Exception e) {
                task.setState(TaskState.FAILED);
                task.setEndTime(LocalDateTime.now());
                task.setError(e.getMessage());
                logger.error("Task {} failed: {}", taskId, e.getMessage());
                throw new ExecutionException(e);
            }
        });
    }
    
    /**
     * 同步执行任务（阻塞等待结果）
     */
    public TaskResult executeSync(String taskId) {
        try {
            return executeAsync(taskId).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Task execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取任务状态
     */
    public Task getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 获取所有任务
     */
    public ConcurrentHashMap<String, Task> getAllTasks() {
        return tasks;
    }
    
    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task != null && task.getState() == TaskState.RUNNING) {
            task.setState(TaskState.CANCELLED);
            logger.info("Task {} cancelled", taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 关闭任务管理器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        logger.info("TaskManager shutdown complete");
    }
    
    private String generateTaskId() {
        return "task_" + taskCounter.incrementAndGet() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 任务执行器接口
     */
    public interface TaskExecutor {
        TaskResult execute(Task task);
    }
    
    /**
     * 任务状态枚举
     */
    public enum TaskState {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    /**
     * 任务实体
     */
    public static class Task {
        private final String id;
        private final String description;
        private final TaskExecutor executor;
        private TaskState state;
        private LocalDateTime createTime;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private TaskResult result;
        private String error;
        
        public Task(String id, String description, TaskExecutor executor) {
            this.id = id;
            this.description = description;
            this.executor = executor;
            this.state = TaskState.PENDING;
            this.createTime = LocalDateTime.now();
        }
        
        // Getters
        public String getId() { return id; }
        public String getDescription() { return description; }
        public TaskExecutor getExecutor() { return executor; }
        public TaskState getState() { return state; }
        public LocalDateTime getCreateTime() { return createTime; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public TaskResult getResult() { return result; }
        public String getError() { return error; }
        
        // Setters
        public void setState(TaskState state) { this.state = state; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public void setResult(TaskResult result) { this.result = result; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * 任务结果
     */
    public static class TaskResult {
        private boolean success;
        private String output;
        private Map<String, Object> data;
        
        public static TaskResult success(String output) {
            TaskResult r = new TaskResult();
            r.success = true;
            r.output = output;
            return r;
        }
        
        public static TaskResult success(String output, Map<String, Object> data) {
            TaskResult r = success(output);
            r.data = data;
            return r;
        }
        
        public static TaskResult failure(String error) {
            TaskResult r = new TaskResult();
            r.success = false;
            r.output = error;
            return r;
        }
        
        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public Map<String, Object> getData() { return data; }
    }
}