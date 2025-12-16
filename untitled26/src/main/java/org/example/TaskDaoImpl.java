package org.example;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.time.ZoneId;

public class TaskDaoImpl implements TaskDao {
    private final ConnectionPool connectionPool;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER_SQLITE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER_ISO =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TaskDaoImpl(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();

        task.setId(rs.getLong("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setCompleted(rs.getBoolean("completed"));

        String dueDateStr = rs.getString("due_date");
        System.out.println("due_date из БД: '" + dueDateStr + "'");

        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
            if (dueDateStr.matches("\\d+")) {
                long millis = Long.parseLong(dueDateStr);
                LocalDate date = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                task.setDueDate(date);
            } else {
                try {
                    task.setDueDate(LocalDate.parse(dueDateStr));
                } catch (Exception e) {
                    task.setDueDate(LocalDate.now());
                }
            }
        } else {
            task.setDueDate(LocalDate.now());
        }

        String priorityStr = rs.getString("priority");
        if (priorityStr != null && !priorityStr.trim().isEmpty()) {
            try {
                task.setPriority(Task.Priority.valueOf(priorityStr));
            } catch (IllegalArgumentException e) {
                task.setPriority(Task.Priority.MEDIUM);
            }
        } else {
            task.setPriority(Task.Priority.MEDIUM);
        }

        task.setCategory(rs.getString("category"));

        task.setCreatedAt(LocalDateTime.now());

        return task;
    }

    @Override
    public Optional<Task> findById(Long id) {
        String sql = "SELECT id, title, description, completed, due_date, priority, category " +
                "FROM tasks WHERE id = ?";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, id);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return Optional.of(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске задачи по ID: " + e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, title, description, completed, due_date, priority, category " +
                "FROM tasks ORDER BY due_date ASC";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return tasks;
    }

    @Override
    public Long save(Task task) {
        String sql = "INSERT INTO tasks (title, description, completed, due_date, priority, category) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, task.getTitle());
                stmt.setString(2, task.getDescription());
                stmt.setBoolean(3, task.isCompleted());

                stmt.setDate(4, java.sql.Date.valueOf(task.getDueDate()));

                stmt.setString(5, task.getPriority().name());
                stmt.setString(6, task.getCategory());

                int affectedRows = stmt.executeUpdate();
                System.out.println("Добавлено строк: " + affectedRows);

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            Long id = generatedKeys.getLong(1);
                            System.out.println("Задаче присвоен ID: " + id);
                            return id;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при сохранении задачи: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return null;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, id);
                int affectedRows = stmt.executeUpdate();
                System.out.println("Удалено: " + affectedRows + " строка (ID=" + id + ")");
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return false;
    }

    @Override
    public boolean update(Task task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, completed = ?, " +
                "due_date = ?, priority = ?, category = ? WHERE id = ?";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setString(1, task.getTitle());
                stmt.setString(2, task.getDescription());
                stmt.setBoolean(3, task.isCompleted());
                stmt.setString(4, task.getDueDate().format(DATE_FORMATTER));
                stmt.setString(5, task.getPriority().name());
                stmt.setString(6, task.getCategory());
                stmt.setLong(7, task.getId());

                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;

            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return false;
    }

    @Override
    public List<Task> findByCompleted(boolean completed) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, title, description, completed, due_date, priority, category " +
                "FROM tasks WHERE completed = ? ORDER BY due_date ASC";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setBoolean(1, completed);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> findByDueDate(LocalDate dueDate) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE due_date = ? ORDER BY priority DESC";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setString(1, dueDate.format(DATE_FORMATTER));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске задач по дате выполнения: " + e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> findOverdueTasks() {
        List<Task> tasks = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT * FROM tasks WHERE due_date < ? AND completed = FALSE ORDER BY due_date ASC";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setString(1, today.format(DATE_FORMATTER));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> findTodayTasks() {
        List<Task> tasks = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT * FROM tasks WHERE due_date = ? AND completed = FALSE ORDER BY priority DESC";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setString(1, today.format(DATE_FORMATTER));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return tasks;
    }

    @Override
    public boolean markAsCompleted(Long id) {
        String sql = "UPDATE tasks SET completed = TRUE WHERE id = ?";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setLong(1, id);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;

            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return false;
    }

    @Override
    public List<Task> findTasksByCategory(String category) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE category = ? ORDER BY due_date ASC";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setString(1, category);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println( e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return tasks;
    }

    @Override
    public List<Task> findTasksByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE due_date >= ? AND due_date <= ? ORDER BY due_date ASC, priority DESC";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setString(1, startDate.format(DATE_FORMATTER));
                stmt.setString(2, endDate.format(DATE_FORMATTER));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return tasks;
    }

    @Override
    public long getTaskCount() {
        String sql = "SELECT COUNT(*) FROM tasks";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
        return 0;
    }

    @Override
    public List<String> getAllCategories() {
        return List.of();
    }

    public void checkDatabaseFormat() {
        String sql = "SELECT id, created_at, typeof(created_at) as type FROM tasks LIMIT 5";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String createdAt = rs.getString("created_at");
                    String type = rs.getString("type");

                    System.out.println("ID: " + id +
                            ", created_at: '" + createdAt + "'" +
                            ", тип: " + type +
                            ", длина: " + (createdAt != null ? createdAt.length() : 0));
                }

            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
    }

    public List<Task> findAllSimple() {
        System.out.println("findAllSimple() - обход ошибки парсинга");
        List<Task> tasks = new ArrayList<>();

        String sql = "SELECT id, title FROM tasks";
        Connection connection = null;

        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Task task = new Task();
                    task.setId(rs.getLong("id"));
                    task.setTitle(rs.getString("title"));
                    task.setDueDate(LocalDate.now());
                    task.setPriority(Task.Priority.MEDIUM);
                    task.setCreatedAt(LocalDateTime.now());
                    tasks.add(task);
                    System.out.println("Простая задача: " + task.getTitle());
                }

            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }

        return tasks;
    }
}