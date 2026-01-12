import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public final class DatabaseConnection {

    // Your Supabase Configuration
    private static final String HOST = "aws-1-eu-west-1.pooler.supabase.com";
    private static final String PORT = "6543";
    private static final String DATABASE = "postgres";
    private static final String USER = "postgres.yfvmblalmzsgftdfgpxg";
    private static final String PASSWORD = "Taonga@3000";
    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE;

    // Connection Pool Settings
    private static final int MAX_POOL_SIZE = 10; // INCREASED FROM 5
    private static final int INITIAL_POOL_SIZE = 3; // INCREASED FROM 2
    private static final long VALIDATION_TIMEOUT = 5; // seconds
    
    // The connection pool
    private static final BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
    private static final Set<Connection> usedConnections = Collections.synchronizedSet(new HashSet<>());
    
    // Track total connections to prevent leaks
    private static volatile int totalConnections = 0;
    
    // Timeout executor for query cancellation
    private static final ExecutorService timeoutExecutor = Executors.newCachedThreadPool();

    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL JDBC Driver registered.");
            initializePool();
            
            // Start connection monitor
            startConnectionMonitor();
            
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Database driver not found", e);
        }
        
        // Add shutdown hook to close all connections
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::closeAllConnections));
    }

    private DatabaseConnection() {}

    private static void initializePool() {
        try {
            for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
                Connection conn = createNewConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                }
            }
            System.out.println("Connection pool initialized with " + connectionPool.size() + " connections");
        } catch (SQLException e) {
            System.err.println("Failed to initialize connection pool: " + e.getMessage());
        }
    }

    private static Connection createNewConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("ssl", "false");
        props.setProperty("tcpKeepAlive", "true");
        props.setProperty("connectTimeout", "5"); // REDUCED FROM 10
        props.setProperty("socketTimeout", "15"); // REDUCED FROM 30
        props.setProperty("loginTimeout", "5");
        
        Connection conn = DriverManager.getConnection(URL, props);
        totalConnections++;
        return conn;
    }

    // MAIN METHOD: Automatically manages connections without changing your queries
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = null;
            
            // Try to get from pool first (wait up to 3 seconds)
            conn = connectionPool.poll(3, TimeUnit.SECONDS);
            
            if (conn != null) {
                // Validate connection before returning
                if (!conn.isValid(2)) {
                    closeConnectionQuietly(conn);
                    conn = createNewConnection();
                }
            } else if (totalConnections < MAX_POOL_SIZE) {
                // Create new connection if under limit
                conn = createNewConnection();
            } else {
                // Wait for a connection to become available
                try {
                    conn = connectionPool.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for connection", e);
                }
            }
            
            usedConnections.add(conn);
            
            // Return a wrapper that automatically returns connection to pool when closed
            return new ConnectionWrapper(conn);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while getting connection", e);
        }
    }

    // Your existing closeConnection method - now returns connection to pool
    public static void closeConnection(Connection conn) {
        if (conn == null) return;
        
        // If it's our wrapper, let it handle the pooling
        if (conn instanceof ConnectionWrapper) {
            try {
                conn.close(); // This will return to pool via wrapper
            } catch (SQLException e) {
                System.err.println("Error closing wrapped connection: " + e.getMessage());
            }
            return;
        }
        
        // Handle raw connections (for backward compatibility)
        returnConnectionToPool(conn);
    }

    private static void returnConnectionToPool(Connection conn) {
        if (conn == null) return;
        
        usedConnections.remove(conn);
        
        try {
            if (conn.isClosed()) {
                totalConnections--;
                return;
            }
            
            // Reset connection state
            if (!conn.getAutoCommit()) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
            
            // Return to pool if there's space
            if (connectionPool.size() < MAX_POOL_SIZE && conn.isValid(2)) {
                connectionPool.offer(conn);
            } else {
                // Close if pool is full or connection is invalid
                conn.close();
                totalConnections--;
            }
        } catch (SQLException e) {
            System.err.println("Error returning connection to pool: " + e.getMessage());
            closeConnectionQuietly(conn);
        }
    }

    private static void closeConnectionQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                totalConnections--;
            }
        } catch (SQLException e) {
            System.err.println("Error quietly closing connection: " + e.getMessage());
        }
    }

    private static void closeAllConnections() {
        System.out.println("Shutting down connection pool...");
        
        // Close all connections in pool
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            closeConnectionQuietly(conn);
        }
        
        // Close all used connections
        synchronized (usedConnections) {
            for (Connection usedConn : usedConnections) {
                closeConnectionQuietly(usedConn);
            }
            usedConnections.clear();
        }
        
        // Shutdown timeout executor
        timeoutExecutor.shutdown();
        
        System.out.println("Connection pool shutdown complete");
    }

    // NEW: Execute query with timeout
    public static ResultSet executeQueryWithTimeout(PreparedStatement stmt, int timeoutSeconds) throws SQLException {
        stmt.setQueryTimeout(timeoutSeconds);
        return stmt.executeQuery();
    }
    
    // NEW: Execute update with timeout
    public static int executeUpdateWithTimeout(PreparedStatement stmt, int timeoutSeconds) throws SQLException {
        stmt.setQueryTimeout(timeoutSeconds);
        return stmt.executeUpdate();
    }

    // NEW: Monitor connections
    private static void startConnectionMonitor() {
        Timer monitorTimer = new Timer(true);
        monitorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("=== CONNECTION POOL STATUS ===");
                System.out.println("Pool size: " + connectionPool.size());
                System.out.println("Used connections: " + usedConnections.size());
                System.out.println("Total connections: " + totalConnections);
                System.out.println("==============================");
            }
        }, 60000, 60000); // Log every minute
    }

    // Connection wrapper that automatically returns to pool when closed
    private static class ConnectionWrapper implements Connection {
        private final Connection delegate;
        private boolean closed = false;

        ConnectionWrapper(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws SQLException {
            if (!closed) {
                closed = true;
                returnConnectionToPool(delegate);
            }
        }

        @Override
        public boolean isClosed() throws SQLException {
            return closed || delegate.isClosed();
        }

        // Delegate all other methods to the actual connection
        @Override public Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { 
            PreparedStatement stmt = delegate.prepareStatement(sql);
            stmt.setQueryTimeout(10); // DEFAULT TIMEOUT
            return stmt;
        }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { delegate.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.createStatement(resultSetType, resultSetConcurrency); }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency); }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.prepareCall(sql, resultSetType, resultSetConcurrency); }
        @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException { delegate.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { delegate.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void rollback(Savepoint savepoint) throws SQLException { delegate.rollback(savepoint); }
        @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { delegate.releaseSavepoint(savepoint); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return delegate.prepareStatement(sql, autoGeneratedKeys); }
        @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return delegate.prepareStatement(sql, columnIndexes); }
        @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return delegate.prepareStatement(sql, columnNames); }
        @Override public Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return delegate.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws SQLClientInfoException { delegate.setClientInfo(name, value); }
        @Override public void setClientInfo(Properties properties) throws SQLClientInfoException { delegate.setClientInfo(properties); }
        @Override public String getClientInfo(String name) throws SQLException { return delegate.getClientInfo(name); }
        @Override public Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
        @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { return delegate.createArrayOf(typeName, elements); }
        @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { return delegate.createStruct(typeName, attributes); }
        @Override public void setSchema(String schema) throws SQLException { delegate.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void abort(Executor executor) throws SQLException { delegate.abort(executor); }
        @Override public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException { delegate.setNetworkTimeout(executor, milliseconds); }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
    }

    // Test method
    public static void testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 as test")) {
            
            if (rs.next()) {
                System.out.println("Connection test successful: " + rs.getInt("test"));
            }
            
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        testConnection();
        
        // Show pool status
        System.out.println("Pool size: " + connectionPool.size());
        System.out.println("Used connections: " + usedConnections.size());
        System.out.println("Total connections: " + totalConnections);
    }
}