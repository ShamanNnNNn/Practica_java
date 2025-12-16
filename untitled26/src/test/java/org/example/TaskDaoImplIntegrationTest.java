package org.example;

import com.sun.jdi.connect.spi.Connection;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskDaoImplIntegrationTest {
    private static final String TEST_DB_PATH = "test_database/test.db";
    private ConnectionPool connectionPool;
    private TaskDaoImpl taskDao;

    @BeforeAll
    void setUpAll() throws Exception {
        Path dbDir = Path.of("test_database");
        if (!Files.exists(dbDir)) {
            Files.createDirectories(dbDir);
        }

        Files.deleteIfExists(Path.of(TEST_DB_PATH));
    }

    @BeforeEach
    void setUp() throws Exception {
        connectionPool = new ConnectionPool() {
            @Override
            public java.sql.Connection getConnection() throws SQLException {
                return (java.sql.Connection) DriverManager.getConnection("jdbc:sqlite:" + TEST_DB_PATH);
            }
        };

        taskDao = new TaskDaoImpl(connectionPool);

        try (var conn = connectionPool.getConnection();
             var stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    completed BOOLEAN DEFAULT 0,
                    due_date TEXT NOT NULL,
                    priority TEXT DEFAULT 'MEDIUM',
                    category TEXT
                )
                """);

            stmt.execute("DELETE FROM tasks");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connectionPool != null) {
            connectionPool.closeAllConnections();
        }

        Files.deleteIfExists(Path.of(TEST_DB_PATH));
    }

    @Test
    void integrationTest_SaveAndRetrieve() {
        Task task = new Task();
        task.setTitle("Integration Test Task");
        task.setDescription("Test Description");
        task.setDueDate(LocalDate.now().plusDays(5));
        task.setPriority(Task.Priority.HIGH);
        task.setCategory("Integration");
        task.setCompleted(false);

        Long savedId = taskDao.save(task);

        assertNotNull(savedId);

        var retrievedOpt = taskDao.findById(savedId);
        assertTrue(retrievedOpt.isPresent());

        Task retrieved = retrievedOpt.get();
        assertEquals("Integration Test Task", retrieved.getTitle());
        assertEquals("Test Description", retrieved.getDescription());
        assertEquals(LocalDate.now().plusDays(5), retrieved.getDueDate());
        assertEquals(Task.Priority.HIGH, retrieved.getPriority());
        assertEquals("Integration", retrieved.getCategory());
        assertFalse(retrieved.isCompleted());
    }

    @Test
    void integrationTest_ConcurrentOperations() {
        for (int i = 0; i < 10; i++) {
            Task task = new Task();
            task.setTitle("Task " + i);
            task.setDueDate(LocalDate.now().plusDays(i));
            task.setPriority(Task.Priority.MEDIUM);
            taskDao.save(task);
        }

        List<Task> allTasks = taskDao.findAll();
        assertEquals(10, allTasks.size());

        Task firstTask = allTasks.get(0);
        firstTask.setTitle("Updated Task");
        firstTask.setCompleted(true);

        boolean updated = taskDao.update(firstTask);
        assertTrue(updated);

        var updatedTask = taskDao.findById(firstTask.getId());
        assertTrue(updatedTask.isPresent());
        assertEquals("Updated Task", updatedTask.get().getTitle());
        assertTrue(updatedTask.get().isCompleted());

        for (int i = 0; i < 5; i++) {
            taskDao.delete(allTasks.get(i).getId());
        }

        List<Task> remainingTasks = taskDao.findAll();
        assertEquals(5, remainingTasks.size());
    }
}