package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task(
                "Test Task",
                "Test Description",
                LocalDate.of(2024, 12, 31),
                Task.Priority.HIGH,
                "Test Category"
        );
        task.setId(1L);
    }

    @Test
    void testTaskCreation() {
        assertNotNull(task);
        assertEquals("Test Task", task.getTitle());
        assertEquals("Test Description", task.getDescription());
        assertEquals(LocalDate.of(2024, 12, 31), task.getDueDate());
        assertEquals(Task.Priority.HIGH, task.getPriority());
        assertEquals("Test Category", task.getCategory());
        assertFalse(task.isCompleted());
        assertNotNull(task.getCreatedAt());
    }

    @Test
    void testTaskEqualsAndHashCode() {
        Task task1 = new Task();
        task1.setId(1L);

        Task task2 = new Task();
        task2.setId(1L);

        Task task3 = new Task();
        task3.setId(2L);

        assertEquals(task1, task1);

        assertEquals(task1, task2);
        assertEquals(task2, task1);

        Task task4 = new Task();
        task4.setId(1L);
        assertEquals(task1, task2);
        assertEquals(task2, task4);
        assertEquals(task1, task4);

        assertNotEquals(task1, task3);
        assertNotEquals(task1, null);
        assertNotEquals(task1, new Object());

        // HashCode
        assertEquals(task1.hashCode(), task2.hashCode());
        assertNotEquals(task1.hashCode(), task3.hashCode());
    }

    @Test
    void testTaskSetters() {
        task.setTitle("New Title");
        task.setDescription("New Description");
        task.setDueDate(LocalDate.of(2025, 1, 1));
        task.setPriority(Task.Priority.LOW);
        task.setCategory("New Category");
        task.setCompleted(true);

        assertEquals("New Title", task.getTitle());
        assertEquals("New Description", task.getDescription());
        assertEquals(LocalDate.of(2025, 1, 1), task.getDueDate());
        assertEquals(Task.Priority.LOW, task.getPriority());
        assertEquals("New Category", task.getCategory());
        assertTrue(task.isCompleted());
    }

    @Test
    void testPriorityEnum() {
        assertEquals("Низкий", Task.Priority.LOW.getDisplayName());
        assertEquals("Средний", Task.Priority.MEDIUM.getDisplayName());
        assertEquals("Высокий", Task.Priority.HIGH.getDisplayName());
    }


}