package org.example;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskDao {
    Optional<Task> findById(Long id);
    List<Task> findAll();
    Long save(Task task);
    boolean delete(Long id);
    boolean update(Task task);


    List<Task> findByCompleted(boolean completed);
    List<Task> findByDueDate(LocalDate dueDate);
    List<Task> findOverdueTasks();
    List<Task> findTodayTasks();
    boolean markAsCompleted(Long id);

    List<Task> findTasksByCategory(String category);

    List<Task> findTasksByDateRange(LocalDate startDate, LocalDate endDate);

    long getTaskCount();

    List<String> getAllCategories();
}