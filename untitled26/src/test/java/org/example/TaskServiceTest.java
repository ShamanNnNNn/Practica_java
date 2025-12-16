package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    @TempDir
    Path tempDir;

    private TaskService taskService;
    private InMemoryTaskDao taskDao;

    @BeforeEach
    void setUp() {
        taskDao = new InMemoryTaskDao();
        taskService = new TaskService(taskDao);
    }

    @Test
    @DisplayName("Service: Сохранение задачи через сервис")
    void testSaveTaskThroughService() {
        Task task = new Task("Service Test", "Description",
                LocalDate.now().plusDays(3),
                Task.Priority.HIGH, "Service");

        Long id = taskService.save(task);
        assertNotNull(id);
        assertTrue(id > 0);

        List<Task> tasks = taskService.findAll();
        assertEquals(1, tasks.size());
        assertEquals("Service Test", tasks.get(0).getTitle());
    }

    @Test
    @DisplayName("Service: Удаление задачи")
    void testDeleteTaskThroughService() {
        Task task = new Task("To Delete", "Desc",
                LocalDate.now(), Task.Priority.MEDIUM, "Test");
        Long id = taskService.save(task);

        boolean deleted = taskService.delete(id);
        assertTrue(deleted);

        List<Task> tasks = taskService.findAll();
        assertEquals(0, tasks.size());
    }

    @Test
    @DisplayName("Service: Поиск задачи по ID")
    void testFindByIdThroughService() {
        Task task = new Task("Find Me", "Description",
                LocalDate.now(), Task.Priority.MEDIUM, "Test");
        Long id = taskService.save(task);

        Task found = taskService.findById(id);
        assertNotNull(found);
        assertEquals("Find Me", found.getTitle());
        assertEquals(id, found.getId());
    }

    @Test
    @DisplayName("Service: Поиск несуществующей задачи ")
    void testFindByIdNotFoundThroughService() {
        Task found = taskService.findById(999999L);
        assertNull(found);
    }

    @Test
    @DisplayName("Service: Поиск задач на сегодня ")
    void testFindTodayTasksThroughService() {
        LocalDate today = LocalDate.now();

        Task todayTask = new Task("Today", "Due today",
                today, Task.Priority.HIGH, "Today");
        Task tomorrowTask = new Task("Tomorrow", "Due tomorrow",
                today.plusDays(1), Task.Priority.MEDIUM, "Tomorrow");

        taskService.save(todayTask);
        taskService.save(tomorrowTask);

        List<Task> todayTasks = taskService.findTodayTasks();
        assertEquals(1, todayTasks.size());
        assertEquals("Today", todayTasks.get(0).getTitle());
        assertEquals(today, todayTasks.get(0).getDueDate());
    }

    @Test
    @DisplayName("Service: Поиск просроченных задач")
    void testFindOverdueTasksThroughService() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Task overdueTask = new Task("Overdue", "Late",
                yesterday, Task.Priority.HIGH, "Urgent");
        Task futureTask = new Task("Future", "Early",
                LocalDate.now().plusDays(1), Task.Priority.LOW, "Planning");

        taskService.save(overdueTask);
        taskService.save(futureTask);

        List<Task> overdueTasks = taskService.findOverdueTasks();
        assertEquals(1, overdueTasks.size());
        assertEquals("Overdue", overdueTasks.get(0).getTitle());
        assertTrue(overdueTasks.get(0).getDueDate().isBefore(LocalDate.now()));
        assertFalse(overdueTasks.get(0).isCompleted());
    }

    @Test
    @DisplayName("Service: Отметка задачи как выполненной")
    void testMarkAsCompletedThroughService() {
        Task task = new Task("To Complete", "Not done yet",
                LocalDate.now(), Task.Priority.MEDIUM, "Test");
        Long id = taskService.save(task);

        boolean marked = taskService.markAsCompleted(id);
        assertTrue(marked);

        List<Task> completedTasks = taskService.findByCompleted(true);
        assertEquals(1, completedTasks.size());
        assertEquals("To Complete", completedTasks.get(0).getTitle());
        assertTrue(completedTasks.get(0).isCompleted());
    }

    @Test
    @DisplayName("Service: Статистика задач")
    void testStatistics() {
        for (int i = 1; i <= 5; i++) {
            Task task = new Task("Task " + i, "Description " + i,
                    LocalDate.now().plusDays(i),
                    Task.Priority.MEDIUM, "Category");
            if (i % 2 == 0) {
                task.setCompleted(true);
            }
            taskService.save(task);
        }

        taskService.markAsCompleted(1L);

        long total = taskService.getTotalTaskCount();
        long completed = taskService.getCompletedTaskCount();
        long pending = taskService.getPendingTaskCount();

        assertEquals(5, total);
        assertEquals(3, completed); // Задачи 2, 4 + отмеченная 1
        assertEquals(2, pending);   // Задачи 3 и 5
    }

    @Test
    @DisplayName("Service: Поиск задач по приоритету")
    void testFindTasksByPriority() {
        taskService.save(new Task("High 1", "Desc", LocalDate.now(), Task.Priority.HIGH, "Test"));
        taskService.save(new Task("High 2", "Desc", LocalDate.now(), Task.Priority.HIGH, "Test"));
        taskService.save(new Task("Medium 1", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Test"));
        taskService.save(new Task("Low 1", "Desc", LocalDate.now(), Task.Priority.LOW, "Test"));

        List<Task> highPriority = taskService.findTasksByPriority(Task.Priority.HIGH);
        List<Task> mediumPriority = taskService.findTasksByPriority(Task.Priority.MEDIUM);
        List<Task> lowPriority = taskService.findTasksByPriority(Task.Priority.LOW);

        assertEquals(2, highPriority.size());
        assertEquals(1, mediumPriority.size());
        assertEquals(1, lowPriority.size());

        assertTrue(highPriority.stream().allMatch(t -> t.getPriority() == Task.Priority.HIGH));
        assertTrue(mediumPriority.stream().allMatch(t -> t.getPriority() == Task.Priority.MEDIUM));
        assertTrue(lowPriority.stream().allMatch(t -> t.getPriority() == Task.Priority.LOW));
    }

    @Test
    @DisplayName("Service: Поиск задач по категории")
    void testFindTasksByCategory() {
        taskService.save(new Task("Work 1", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Work"));
        taskService.save(new Task("Work 2", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Work"));
        taskService.save(new Task("Personal", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Personal"));
        taskService.save(new Task("No Category", "Desc", LocalDate.now(), Task.Priority.MEDIUM, null));

        List<Task> workTasks = taskService.findTasksByCategory("Work");
        List<Task> personalTasks = taskService.findTasksByCategory("Personal");
        List<Task> unknownTasks = taskService.findTasksByCategory("Unknown");

        assertEquals(2, workTasks.size());
        assertEquals(1, personalTasks.size());
        assertEquals(0, unknownTasks.size());

        assertTrue(workTasks.stream().allMatch(t -> "Work".equals(t.getCategory())));
        assertTrue(personalTasks.stream().allMatch(t -> "Personal".equals(t.getCategory())));
    }

    @Test
    @DisplayName("Service: Статистика по категориям")
    void testGetCategoryStatistics() {
        taskService.save(new Task("Task 1", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Work"));
        taskService.save(new Task("Task 2", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Personal"));
        taskService.save(new Task("Task 3", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Work"));
        taskService.save(new Task("Task 4", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Work"));
        taskService.save(new Task("Task 5", "Desc", LocalDate.now(), Task.Priority.MEDIUM, "Personal"));
        taskService.save(new Task("Task 6", "Desc", LocalDate.now(), Task.Priority.MEDIUM, null));

        var statistics = taskService.getCategoryStatistics();

        assertEquals(2, statistics.size());
        assertEquals(3, statistics.get("Work"));
        assertEquals(2, statistics.get("Personal"));
        assertNull(statistics.get(null));
    }

    @Test
    @DisplayName("Service: Поиск задач по диапазону дат")
    void testFindTasksByDateRange() {
        LocalDate today = LocalDate.now();

        taskService.save(new Task("Today", "Desc", today, Task.Priority.MEDIUM, "Test"));
        taskService.save(new Task("Tomorrow", "Desc", today.plusDays(1), Task.Priority.MEDIUM, "Test"));
        taskService.save(new Task("Yesterday", "Desc", today.minusDays(1), Task.Priority.MEDIUM, "Test"));
        taskService.save(new Task("Next Week", "Desc", today.plusDays(7), Task.Priority.MEDIUM, "Test"));

        List<Task> todayOnly = taskService.findTasksByDateRange(today, today);
        List<Task> thisWeek = taskService.findTasksByDateRange(today.minusDays(1), today.plusDays(7));

        assertEquals(1, todayOnly.size());
        assertEquals("Today", todayOnly.get(0).getTitle());

        assertEquals(4, thisWeek.size());
    }

    @Test
    @DisplayName("Service: Тест с пустой базой данных")
    void testEmptyDatabase() {
        List<Task> allTasks = taskService.findAll();
        List<Task> todayTasks = taskService.findTodayTasks();
        List<Task> overdueTasks = taskService.findOverdueTasks();
        List<Task> completedTasks = taskService.findByCompleted(true);

        assertTrue(allTasks.isEmpty());
        assertTrue(todayTasks.isEmpty());
        assertTrue(overdueTasks.isEmpty());
        assertTrue(completedTasks.isEmpty());

        assertEquals(0, taskService.getTotalTaskCount());
        assertEquals(0, taskService.getCompletedTaskCount());
        assertEquals(0, taskService.getPendingTaskCount());
        assertEquals(0, taskService.getOverdueTaskCount());
        assertEquals(0, taskService.getTodayTaskCount());
    }

    private static class InMemoryTaskDao implements TaskDao {
        private final java.util.List<Task> tasks = new java.util.ArrayList<>();
        private long nextId = 1;

        @Override
        public Long save(Task task) {
            task.setId(nextId++);
            if (task.getCreatedAt() == null) {
                task.setCreatedAt(java.time.LocalDateTime.now());
            }
            tasks.add(task);
            return task.getId();
        }

        @Override
        public boolean update(Task task) {
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId().equals(task.getId())) {
                    tasks.set(i, task);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean delete(Long id) {
            return tasks.removeIf(task -> task.getId().equals(id));
        }

        @Override
        public java.util.Optional<Task> findById(Long id) {
            return tasks.stream()
                    .filter(task -> task.getId().equals(id))
                    .findFirst();
        }

        @Override
        public java.util.List<Task> findAll() {
            return new java.util.ArrayList<>(tasks);
        }

        @Override
        public java.util.List<Task> findByCompleted(boolean completed) {
            return tasks.stream()
                    .filter(task -> task.isCompleted() == completed)
                    .toList();
        }

        @Override
        public List<Task> findByDueDate(LocalDate dueDate) {
            return List.of();
        }

        @Override
        public java.util.List<Task> findTodayTasks() {
            LocalDate today = LocalDate.now();
            return tasks.stream()
                    .filter(task -> task.getDueDate().equals(today))
                    .toList();
        }

        @Override
        public java.util.List<Task> findOverdueTasks() {
            LocalDate today = LocalDate.now();
            return tasks.stream()
                    .filter(task -> task.getDueDate().isBefore(today) && !task.isCompleted())
                    .toList();
        }

        @Override
        public boolean markAsCompleted(Long id) {
            return findById(id).map(task -> {
                task.setCompleted(true);
                return true;
            }).orElse(false);
        }

        @Override
        public java.util.List<Task> findTasksByCategory(String category) {
            return tasks.stream()
                    .filter(task -> category != null && category.equals(task.getCategory()))
                    .toList();
        }

        @Override
        public java.util.List<Task> findTasksByDateRange(LocalDate startDate, LocalDate endDate) {
            return tasks.stream()
                    .filter(task -> !task.getDueDate().isBefore(startDate) && !task.getDueDate().isAfter(endDate))
                    .toList();
        }

        @Override
        public long getTaskCount() {
            return tasks.size();
        }

        @Override
        public List<String> getAllCategories() {
            return List.of();
        }
    }
}