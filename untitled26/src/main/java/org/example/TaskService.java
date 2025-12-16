package org.example;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskService {
    private final TaskDao taskDao;

    private List<Task> cachedTasks = null;
    private boolean cacheValid = false;
    private long lastCacheTime = 0;
    private static final long CACHE_TIMEOUT_MS = 30000; // 30 секунд

    public TaskService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public Long save(Task task) {
        invalidateCache();
        return taskDao.save(task);
    }

    public boolean update(Task task) {
        invalidateCache();
        return taskDao.update(task);
    }

    public boolean delete(Long id) {
        invalidateCache();
        return taskDao.delete(id);
    }

    public Task findById(Long id) {
        return taskDao.findById(id).orElse(null);
    }

    public List<Task> findAll() {
        System.out.println("TaskService.findAll() - " + Thread.currentThread().getName());

        if (cacheValid && cachedTasks != null &&
                System.currentTimeMillis() - lastCacheTime < CACHE_TIMEOUT_MS) {
            System.out.println("Используем кэшированные задачи: " + cachedTasks.size());
            return new ArrayList<>(cachedTasks); // Возвращаем копию
        }

        try {
            List<Task> tasks = taskDao.findAll();
            System.out.println("Загружено из БД: " + tasks.size() + " задач");

            cachedTasks = new ArrayList<>(tasks);
            cacheValid = true;
            lastCacheTime = System.currentTimeMillis();

            return tasks;

        } catch (Exception e) {
            System.err.println("Ошибка в findAll: " + e.getMessage());
            e.printStackTrace();

            if (taskDao instanceof TaskDaoImpl) {
                List<Task> tasks = ((TaskDaoImpl) taskDao).findAllSimple();
                cachedTasks = new ArrayList<>(tasks);
                cacheValid = true;
                lastCacheTime = System.currentTimeMillis();
                return tasks;
            }

            return new ArrayList<>();
        }
    }

    public List<Task> findByCompleted(boolean completed) {
        try {
            return taskDao.findByCompleted(completed);
        } catch (Exception e) {
            System.err.println("Ошибка в findByCompleted, используем фильтрацию: " + e.getMessage());
            return findAll().stream()
                    .filter(task -> task.isCompleted() == completed)
                    .limit(100) // Ограничиваем для производительности
                    .collect(Collectors.toList());
        }
    }

    public List<Task> findTodayTasks() {
        try {
            return taskDao.findTodayTasks();
        } catch (Exception e) {
            System.err.println("Ошибка в findTodayTasks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Task> findOverdueTasks() {
        try {
            return taskDao.findOverdueTasks();
        } catch (Exception e) {
            System.err.println("Ошибка в findOverdueTasks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean markAsCompleted(Long id) {
        invalidateCache();
        return taskDao.markAsCompleted(id);
    }

    public List<Task> findTasksByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return new ArrayList<>();
        }

        if (cacheValid && cachedTasks != null) {
            return cachedTasks.stream()
                    .filter(task -> category.equalsIgnoreCase(task.getCategory()))
                    .collect(Collectors.toList());
        }

        try {
            return taskDao.findTasksByCategory(category);
        } catch (Exception e) {
            System.err.println("Ошибка в findTasksByCategory: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Task> findTasksByPriority(Task.Priority priority) {
        if (priority == null) {
            return new ArrayList<>();
        }

        if (cacheValid && cachedTasks != null) {
            return cachedTasks.stream()
                    .filter(task -> priority.equals(task.getPriority()))
                    .collect(Collectors.toList());
        }

        return findAll().stream()
                .filter(task -> priority.equals(task.getPriority()))
                .collect(Collectors.toList());
    }

    public long getTotalTaskCount() {
        try {
            return taskDao.getTaskCount();
        } catch (Exception e) {
            System.err.println("Ошибка в getTotalTaskCount: " + e.getMessage());
            return findAll().size();
        }
    }

    public long getCompletedTaskCount() {
        return findByCompleted(true).size();
    }

    public long getPendingTaskCount() {
        return findByCompleted(false).size();
    }

    public long getOverdueTaskCount() {
        return findOverdueTasks().size();
    }

    public long getTodayTaskCount() {
        return findTodayTasks().size();
    }

    public Map<String, Long> getCategoryStatistics() {
        if (cacheValid && cachedTasks != null) {
            return cachedTasks.stream()
                    .filter(task -> task.getCategory() != null && !task.getCategory().trim().isEmpty())
                    .collect(Collectors.groupingBy(
                            Task::getCategory,
                            Collectors.counting()
                    ));
        }

        return findAll().stream()
                .filter(task -> task.getCategory() != null && !task.getCategory().trim().isEmpty())
                .limit(1000) // Защита от переполнения
                .collect(Collectors.groupingBy(
                        Task::getCategory,
                        Collectors.counting()
                ));
    }

    private void invalidateCache() {
        cacheValid = false;
        cachedTasks = null;
        System.out.println("Кэш задач инвалидирован");
    }


    public List<Task> findTasksByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            return taskDao.findTasksByDateRange(startDate, endDate);
        } catch (Exception e) {
            System.err.println("Ошибка в findTasksByDateRange: " + e.getMessage());
            return findAll().stream()
                    .filter(task -> task.getDueDate() != null)
                    .filter(task -> !task.getDueDate().isBefore(startDate) &&
                            !task.getDueDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }
    }
}