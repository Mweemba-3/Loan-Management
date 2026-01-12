import javax.swing.*;

public class ApplicationMain {
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Global exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("UNCAUGHT EXCEPTION in " + thread.getName());
            throwable.printStackTrace();
            
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                    "System Error\n\n" +
                    "The application encountered an error:\n" +
                    throwable.getMessage() + "\n\n" +
                    "This may cause freezing. If problem persists,\n" +
                    "please restart the application.",
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
            });
        });
        
        // Start application on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize database connection
                DatabaseConnection.testConnection();
                
                // Show login screen
                new LoginScreen().setVisible(true);
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Failed to start application:\n" + e.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}