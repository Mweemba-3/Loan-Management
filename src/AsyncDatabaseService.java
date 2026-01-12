import javax.swing.*;
import java.sql.*;
import java.util.concurrent.*;
import java.util.function.*;

public class AsyncDatabaseService {
    
    // Generic async database operation
    public static <T> void executeAsync(Callable<T> databaseTask, 
                                       Consumer<T> onSuccess, 
                                       Consumer<Exception> onError) {
        
        SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return databaseTask.call();
            }
            
            @Override
            protected void done() {
                try {
                    T result = get();
                    if (onSuccess != null) {
                        SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (onError != null) {
                        SwingUtilities.invokeLater(() -> onError.accept(e));
                    }
                } catch (ExecutionException e) {
                    if (onError != null) {
                        Throwable cause = e.getCause();
                        Exception ex = (cause instanceof Exception) ? 
                                      (Exception) cause : new Exception(cause);
                        SwingUtilities.invokeLater(() -> onError.accept(ex));
                    }
                } catch (CancellationException e) {
                    System.out.println("Database task was cancelled");
                }
            }
        };
        
        worker.execute();
    }
    
    // Quick async log (for audit logs) - USED BY ALL SCREENS
    public static void logAsync(int employeeId, String action, String details) {
        executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return null;
                }
                
                String sql = "INSERT INTO audit_logs (employee_id, employee_name, action, details) " +
                           "SELECT ?, name, ?, ? FROM employees WHERE employee_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, employeeId);
                    stmt.setString(2, action);
                    stmt.setString(3, details);
                    stmt.setInt(4, employeeId);
                    stmt.setQueryTimeout(5);
                    stmt.executeUpdate();
                }
            }
            return null;
        }, null, e -> System.err.println("Async log failed: " + e.getMessage()));
    }
    
    // Helper method for count queries - REUSABLE
    public static void executeCountAsync(String query, Consumer<Integer> onSuccess, Consumer<Exception> onError) {
        executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getInt(1) : 0;
                    }
                }
            }
        }, onSuccess, onError);
    }
    
    // Helper method for sum queries - REUSABLE
    public static void executeSumAsync(String query, Consumer<Double> onSuccess, Consumer<Exception> onError) {
        executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getDouble(1) : 0.0;
                    }
                }
            }
        }, onSuccess, onError);
    }
    
    // Generic fetch list - REUSABLE
    public static void executeListAsync(String query, Consumer<ResultSet> processor, Consumer<Exception> onError) {
        executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    return stmt.executeQuery();
                }
            }
        }, processor::accept, onError);
    }
    
    // Execute update (INSERT/UPDATE/DELETE) - REUSABLE
    public static void executeUpdateAsync(String query, Consumer<Integer> onSuccess, Consumer<Exception> onError) {
        executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    return stmt.executeUpdate();
                }
            }
        }, onSuccess, onError);
    }
}