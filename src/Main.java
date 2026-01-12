import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize database connection
                DatabaseConnection.getConnection();
                
                // Set up main frame
                JFrame mainFrame = ScreenManager.getInstance().getMainFrame();
                mainFrame.setVisible(true);
                
                // Show login screen
                ScreenManager.getInstance().showScreen(new LoginScreen());
                
            } catch (Exception e){
                JOptionPane.showMessageDialog(null,
                    "Failed to initialize application: " + e.getMessage(),
                    "System Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}