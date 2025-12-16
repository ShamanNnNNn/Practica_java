package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionPoolTest {
    private ConnectionPool connectionPool;

    @BeforeEach
    void setUp() {
        try {
            var field = ConnectionPool.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        connectionPool = ConnectionPool.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (connectionPool != null) {
            connectionPool.closeAllConnections();
        }
    }

    @Test
    void testGetInstance() {
        ConnectionPool firstInstance = ConnectionPool.getInstance();
        ConnectionPool secondInstance = ConnectionPool.getInstance();

        assertNotNull(firstInstance);
        assertNotNull(secondInstance);
        assertSame(firstInstance, secondInstance, "Должен возвращаться один и тот же");
    }

    @Test
    void testGetConnection() throws SQLException {
        Connection connection = connectionPool.getConnection();

        assertNotNull(connection, "не null");
        assertFalse(connection.isClosed(), "должно быть открытым");

        connectionPool.releaseConnection(connection);
    }

    @Test
    void testReleaseConnection() throws SQLException {
        Connection connection = connectionPool.getConnection();

        int availableBefore = connectionPool.getAvailableConnectionsCount();
        int usedBefore = connectionPool.getUsedConnectionsCount();

        connectionPool.releaseConnection(connection);

        int availableAfter = connectionPool.getAvailableConnectionsCount();
        int usedAfter = connectionPool.getUsedConnectionsCount();

        assertEquals(availableBefore + 1, availableAfter, "соединений должно стать больше");
        assertEquals(usedBefore - 1, usedAfter, "Используемых соединений должно стать меньше");
    }






}