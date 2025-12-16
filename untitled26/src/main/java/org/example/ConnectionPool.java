package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConnectionPool {
    private static ConnectionPool instance;
    private final BlockingQueue<Connection> availableConnections;
    private final String url;
    private final int maxPoolSize;
    private int createdConnections = 0;

    ConnectionPool() {
        this.url = "jdbc:sqlite:database/tasks.db";
        this.maxPoolSize = 3;
        this.availableConnections = new ArrayBlockingQueue<>(maxPoolSize);

        initializeDatabaseDirectory();
        loadDriver();
    }

    public static synchronized ConnectionPool getInstance() {
        if (instance == null) {
            instance = new ConnectionPool();
        }
        return instance;
    }

    private void initializeDatabaseDirectory() {
        try {
            java.nio.file.Path dbPath = java.nio.file.Paths.get("database");
            if (!java.nio.file.Files.exists(dbPath)) {
                java.nio.file.Files.createDirectories(dbPath);
            }
        } catch (Exception e) {
            System.err.println("Ошибка создания директории database: " + e.getMessage());
        }
    }

    private void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection conn = availableConnections.poll(3, TimeUnit.SECONDS);
            if (conn != null && isValidConnection(conn)) {
                return conn;
            }

            if (createdConnections < maxPoolSize) {
                Connection newConn = createNewConnection();
                createdConnections++;
                System.out.println("Создано соединение " + createdConnections + "/" + maxPoolSize);
                return newConn;
            }

            conn = availableConnections.poll(5, TimeUnit.SECONDS);
            if (conn != null && isValidConnection(conn)) {
                return conn;
            }

            throw new SQLException("Не удалось получить соединение. Таймаут.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Поток прерван", e);
        }
    }

    private Connection createNewConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);

        try (var stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = 10000");
            stmt.execute("PRAGMA temp_store = MEMORY");
            stmt.execute("PRAGMA mmap_size = 268435456");
        }

        connection.setAutoCommit(true);
        return connection;
    }

    private boolean isValidConnection(Connection conn) {
        if (conn == null) return false;

        try {
            return !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection == null) return;

        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
            connection.clearWarnings();

            if (!availableConnections.offer(connection)) {
                connection.close();
                createdConnections--;
                System.out.println(createdConnections);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            try {
                connection.close();
                createdConnections--;
            } catch (SQLException ex) {
            }
        }
    }

    public synchronized void closeAllConnections() {

        for (Connection conn : availableConnections) {
            closeConnection(conn);
        }
        availableConnections.clear();

        createdConnections = 0;
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public int getAvailableConnectionsCount() {
        return availableConnections.size();
    }

    public int getUsedConnectionsCount() {
        return createdConnections - availableConnections.size();
    }


}