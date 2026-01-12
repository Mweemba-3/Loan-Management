import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ScreenManager {
    private static volatile ScreenManager instance;
    private JFrame mainFrame;
    private Map<String, JPanel> screens;
    private JPanel currentScreen;
    
    // Cache for common operations
    private static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    private static final Font DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    private ScreenManager() {
        initializeMainFrame();
        screens = new HashMap<>();
        
        // Set system-wide Swing optimizations
        setSwingOptimizations();
    }

    public static ScreenManager getInstance() {
        if (instance == null) {
            synchronized (ScreenManager.class) {
                if (instance == null) {
                    instance = new ScreenManager();
                }
            }
        }
        return instance;
    }

    private void setSwingOptimizations() {
        // Enable Swing optimizations
        System.setProperty("sun.java2d.opengl", "True");
        System.setProperty("sun.java2d.accthreshold", "0");
        System.setProperty("sun.java2d.d3d", "False");
        System.setProperty("swing.bufferPerWindow", "true");
        System.setProperty("swing.useSystemFontSettings", "true");
        
        // Set UI manager properties for better performance
        UIManager.put("Button.font", DEFAULT_FONT);
        UIManager.put("Label.font", DEFAULT_FONT);
        UIManager.put("TextField.font", DEFAULT_FONT);
        UIManager.put("TextArea.font", DEFAULT_FONT);
        UIManager.put("ComboBox.font", DEFAULT_FONT);
        UIManager.put("Table.font", DEFAULT_FONT);
        UIManager.put("TableHeader.font", DEFAULT_FONT);
        
        // Disable unnecessary UI effects for better performance
        UIManager.put("Button.showMnemonics", Boolean.FALSE);
        UIManager.put("TabbedPane.showTabSeparators", Boolean.FALSE);
        
        // Enable double buffering globally
        RepaintManager.currentManager(null).setDoubleBufferingEnabled(true);
    }

    private void initializeMainFrame() {
        mainFrame = new JFrame("MS CODEFORGE - Loan Management System");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Use screen percentage for better multi-monitor support
        int width = (int) (SCREEN_SIZE.width * 0.8);
        int height = (int) (SCREEN_SIZE.height * 0.8);
        mainFrame.setSize(width, height);
        
        mainFrame.setMinimumSize(new Dimension(1024, 768));
        mainFrame.setLocationRelativeTo(null);
        
        // Enable hardware acceleration for main frame
        mainFrame.setIgnoreRepaint(false);
        
        // Set double buffering for main frame
        mainFrame.getRootPane().setDoubleBuffered(true);
        
        mainFrame.getContentPane().setLayout(new BorderLayout());
        
        // Try to load icon (async to avoid blocking)
        loadIconAsync();
        
        // Set system font anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }
    
    private void loadIconAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    ImageIcon icon = new ImageIcon(getClass().getResource("/MS CodeForge.png"));
                    SwingUtilities.invokeLater(() -> {
                        mainFrame.setIconImage(icon.getImage());
                    });
                } catch (Exception e) {
                    // Icon not found, continue without it
                }
                return null;
            }
        };
        worker.execute();
    }

    public void showScreen(JPanel screen) {
        showScreen(screen, true);
    }
    
    public void showScreen(JPanel screen, boolean animate) {
        if (currentScreen == screen) {
            return; // Already showing this screen
        }
        
        // Use invokeLater to ensure smooth transition
        SwingUtilities.invokeLater(() -> {
            JPanel oldScreen = currentScreen;
            
            if (oldScreen != null) {
                mainFrame.getContentPane().remove(oldScreen);
                // Allow garbage collection of old screen
                oldScreen.removeAll();
                oldScreen = null;
            }
            
            currentScreen = screen;
            
            // Enable double buffering for new screen
            screen.setDoubleBuffered(true);
            
            // Add with constraints for better layout
            mainFrame.getContentPane().add(screen, BorderLayout.CENTER);
            
            // Use lightweight revalidation
            mainFrame.getContentPane().revalidate();
            mainFrame.getContentPane().repaint();
            
            updateWindowTitle(screen);
            
            if (!mainFrame.isVisible()) {
                mainFrame.setVisible(true);
            }
            
            // Force immediate layout update
            screen.doLayout();
        });
    }

    private void updateWindowTitle(JPanel screen) {
        String title = "MS CODEFORGE - Loan Management System";
        
        if (screen instanceof AdminDashboard) {
            title += " - Admin Dashboard";
        } else if (screen instanceof EmployeeDashboard) {
            title += " - Employee Dashboard";
        } else if (screen instanceof ClientsScreen) {
            title += " - Client Management";
        } else if (screen instanceof LoansScreen) {
            title += " - Loan Management";
        } else if (screen instanceof PaymentsScreen) {
            title += " - Payment Management";
        } else if (screen instanceof ActivitiesScreen) {
            title += " - Activity Logs";
        }else if (screen instanceof LogoManagementScreen) {
            title += " - Settings Logs";
        }else if (screen instanceof EmployeesScreen) {
            title += " - Employee Management";
        } else if (screen instanceof ChangePasswordScreen) {
            title += " - Change Password";
        } else if (screen instanceof LoginScreen) {
            title += " - Login";
        }
        
        mainFrame.setTitle(title);
    }

    public void addScreen(String name, JPanel screen) {
        screen.setDoubleBuffered(true); // Enable double buffering
        screens.put(name, screen);
    }

    public JPanel getScreen(String name) {
        return screens.get(name);
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public JPanel getCurrentScreen() {
        return currentScreen;
    }

    public static void showErrorMessage(Component parent, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    public static void showSuccessMessage(Component parent, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public static boolean showConfirmationDialog(Component parent, String message) {
        final boolean[] result = new boolean[1];
        
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                int dialogResult = JOptionPane.showConfirmDialog(parent, message, "Confirmation", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                result[0] = (dialogResult == JOptionPane.YES_OPTION);
            } else {
                SwingUtilities.invokeAndWait(() -> {
                    int dialogResult = JOptionPane.showConfirmDialog(parent, message, "Confirmation", 
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    result[0] = (dialogResult == JOptionPane.YES_OPTION);
                });
            }
        } catch (Exception e) {
            result[0] = false;
        }
        
        return result[0];
    }

    public void refreshCurrentScreen() {
        if (currentScreen != null) {
            SwingUtilities.invokeLater(() -> {
                // Use incremental revalidation for better performance
                currentScreen.revalidate();
                currentScreen.repaint();
            });
        }
    }

    public static void centerDialog(JDialog dialog) {
        SwingUtilities.invokeLater(() -> {
            if (instance != null && instance.mainFrame != null) {
                dialog.setLocationRelativeTo(instance.mainFrame);
            } else {
                dialog.setLocationRelativeTo(null);
            }
        });
    }

    public static Dimension getScreenSize() {
        return SCREEN_SIZE;
    }

    public static JDialog createLoadingDialog(Component parent, String message) {
        JDialog loadingDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), true);
        loadingDialog.setUndecorated(true); // Faster rendering
        loadingDialog.setLayout(new BorderLayout());
        loadingDialog.setSize(300, 100); // Smaller for faster rendering
        loadingDialog.setResizable(false);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        // Enable double buffering
        loadingDialog.getRootPane().setDoubleBuffered(true);
        
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPanel.setBackground(Color.WHITE);
        
        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(200, 20));
        
        contentPanel.add(messageLabel, BorderLayout.CENTER);
        contentPanel.add(progressBar, BorderLayout.SOUTH);
        
        loadingDialog.add(contentPanel);
        
        // Center on EDT
        SwingUtilities.invokeLater(() -> {
            loadingDialog.setLocationRelativeTo(parent);
        });
        
        return loadingDialog;
    }
    
    public static JDialog createQuickLoadingDialog(Component parent, String message) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), true);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(200, 60);
        dialog.setResizable(false);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        panel.setBackground(Color.WHITE);
        
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        panel.add(label, BorderLayout.CENTER);
        dialog.add(panel);
        
        SwingUtilities.invokeLater(() -> {
            dialog.setLocationRelativeTo(parent);
        });
        
        return dialog;
    }
    
    public void disposeCurrentScreen() {
        if (currentScreen != null) {
            SwingUtilities.invokeLater(() -> {
                mainFrame.getContentPane().remove(currentScreen);
                currentScreen.removeAll();
                currentScreen = null;
                System.gc(); // Suggest garbage collection
            });
        }
    }
    
    public static void setGlobalCursor(Cursor cursor) {
        SwingUtilities.invokeLater(() -> {
            Frame[] frames = Frame.getFrames();
            for (Frame frame : frames) {
                frame.setCursor(cursor);
            }
        });
    }
    
    public static void resetGlobalCursor() {
        setGlobalCursor(Cursor.getDefaultCursor());
    }
    
    public void minimizeToTray() {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().createImage("MS CodeForge.png");
                TrayIcon trayIcon = new TrayIcon(image, "MS CODEFORGE");
                trayIcon.setImageAutoSize(true);
                
                trayIcon.addActionListener(e -> {
                    tray.remove(trayIcon);
                    mainFrame.setVisible(true);
                    mainFrame.setExtendedState(JFrame.NORMAL);
                });
                
                tray.add(trayIcon);
                mainFrame.setVisible(false);
                
            } catch (Exception e) {
                // Tray not supported or error
            }
        }
    }
    
    public void restoreFromTray() {
        mainFrame.setVisible(true);
        mainFrame.setExtendedState(JFrame.NORMAL);
    }
    
    // Quick screen switch without full revalidation
    public void switchScreenFast(JPanel newScreen) {
        if (currentScreen == newScreen) return;
        
        Container contentPane = mainFrame.getContentPane();
        
        // Remove old screen
        if (currentScreen != null) {
            contentPane.remove(currentScreen);
        }
        
        // Add new screen
        currentScreen = newScreen;
        contentPane.add(currentScreen, BorderLayout.CENTER);
        
        // Minimal repaint
        currentScreen.revalidate();
        currentScreen.repaint();
        
        updateWindowTitle(newScreen);
    }
    
    // Memory optimization
    public void clearScreenCache() {
        screens.clear();
        if (currentScreen != null) {
            currentScreen.removeAll();
        }
        System.gc(); // Suggest garbage collection
    }
}