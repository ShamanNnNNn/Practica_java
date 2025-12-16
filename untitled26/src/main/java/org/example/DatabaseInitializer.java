package org.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private final ConnectionPool connectionPool;

    public DatabaseInitializer(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void initializeDatabase() {

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();

            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    completed BOOLEAN DEFAULT FALSE,
                    due_date TEXT NOT NULL,  -- SQLite хранит даты как TEXT
                    created_at TEXT DEFAULT (datetime('now', 'localtime')),
                    priority TEXT DEFAULT 'MEDIUM',
                    category TEXT
                )
                """;

            String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_due_date ON tasks(due_date)";
            String createStatusIndexSQL = "CREATE INDEX IF NOT EXISTS idx_completed ON tasks(completed)";
            String createPriorityIndexSQL = "CREATE INDEX IF NOT EXISTS idx_priority ON tasks(priority)";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);

                stmt.execute(createIndexSQL);
                stmt.execute(createStatusIndexSQL);
                stmt.execute(createPriorityIndexSQL);

                checkTableStructure(connection);

            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
    }

    private void checkTableStructure(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("PRAGMA table_info(tasks)");
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                boolean notnull = rs.getBoolean("notnull");
                String dfltValue = rs.getString("dflt_value");
                System.out.printf("%-12s %-12s notnull:%-6s default:%s%n",
                        name, type, notnull, dfltValue != null ? dfltValue : "NULL");
            }

            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM tasks");
            if (rs.next()) {
                System.out.println(rs.getInt("count"));
            }

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }


}