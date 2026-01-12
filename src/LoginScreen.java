import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.Base64;

public class LoginScreen extends JPanel {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel logoLabel;
    private JLabel statusLabel;
    private boolean isLoggingIn = false;
    private ImageIcon clearLogo;

    public LoginScreen() {
        setLayout(new BorderLayout());
        setBackground(new Color(18, 22, 27));
        initUI();
    }

    private void initUI() {
        // Create clean logo first
        clearLogo = createCleanLogo();
        
        // Main container with clean gradient
        JPanel mainPanel = new JPanel(new BorderLayout(30, 30));
        mainPanel.setBackground(new Color(18, 22, 27));
        mainPanel.setBorder(new EmptyBorder(40, 50, 40, 50));
        mainPanel.setOpaque(true);

        // Left Panel - Brand Section with CLEAN background
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(new Color(25, 30, 40));
        leftPanel.setOpaque(true);
        leftPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
        
        // Logo display - SIMPLE and CLEAN
        logoLabel = new JLabel("", SwingConstants.CENTER);
        logoLabel.setPreferredSize(new Dimension(180, 180));
        
        // Load logo from database asynchronously
        loadLogoFromDatabase();
        
        // Company name
        JLabel companyLabel = new JLabel("MS CODEFORGE");
        companyLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        companyLabel.setForeground(new Color(0, 173, 181));
        companyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Tagline
        JLabel taglineLabel = new JLabel("Loan Management System");
        taglineLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        taglineLabel.setForeground(new Color(180, 180, 180));
        taglineLabel.setHorizontalAlignment(SwingConstants.CENTER);
        taglineLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        // Arrange left panel components
        JPanel leftContent = new JPanel();
        leftContent.setLayout(new BoxLayout(leftContent, BoxLayout.Y_AXIS));
        leftContent.setOpaque(false);
        leftContent.add(Box.createVerticalStrut(20));
        leftContent.add(logoLabel);
        leftContent.add(Box.createVerticalStrut(20));
        leftContent.add(companyLabel);
        leftContent.add(taglineLabel);
        
        leftPanel.add(leftContent);

        // Right Panel - Login Form
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(18, 22, 27));
        rightPanel.setOpaque(true);
        rightPanel.setBorder(new EmptyBorder(0, 60, 0, 0));
        
        // Form container with solid background
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setBackground(new Color(40, 45, 55));
        formContainer.setOpaque(true);
        formContainer.setBorder(new EmptyBorder(40, 40, 40, 40));
        formContainer.setPreferredSize(new Dimension(400, 450));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Secure Login");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 30, 0);
        formContainer.add(titleLabel, gbc);

        // Username field
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 8, 0);
        formContainer.add(userLabel, gbc);

        usernameField = createStyledTextField();
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        formContainer.add(usernameField, gbc);

        // Password field
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 8, 0);
        formContainer.add(passLabel, gbc);

        passwordField = createStyledPasswordField();
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 30, 0);
        formContainer.add(passwordField, gbc);

        // Login Button
        loginButton = new JButton("LOGIN");
        styleLoginButton(loginButton);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 15, 0);
        formContainer.add(loginButton, gbc);

        // Status label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(255, 100, 100));
        gbc.gridy = 6;
        gbc.insets = new Insets(5, 0, 10, 0);
        formContainer.add(statusLabel, gbc);

        // Simple forgot password
        JPanel forgotPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        forgotPanel.setBackground(new Color(40, 45, 55));
        forgotPanel.setOpaque(true);
        JLabel forgotLabel = new JLabel("Need help? Contact Admin: +260 123 456 789");
        forgotLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        forgotLabel.setForeground(new Color(150, 150, 150));
        forgotLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(LoginScreen.this,
                    "Contact System Administrator:\nPhone: +260 123 456 789\nEmail: admin@mscodeforge.com",
                    "Support", JOptionPane.INFORMATION_MESSAGE);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                forgotLabel.setForeground(new Color(0, 173, 181));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                forgotLabel.setForeground(new Color(150, 150, 150));
            }
        });
        forgotPanel.add(forgotLabel);
        gbc.gridy = 7;
        gbc.insets = new Insets(10, 0, 0, 0);
        formContainer.add(forgotPanel, gbc);

        rightPanel.add(formContainer);
        
        // Add panels to main
        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Add enter key listener
        passwordField.addActionListener(e -> performLogin());
        
        // Set initial logo
        logoLabel.setIcon(clearLogo);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(320, 45));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(new Color(50, 55, 65));
        field.setForeground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 75, 85), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        field.setCaretColor(new Color(0, 173, 181));
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 173, 181), 2),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 75, 85), 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
        });
        
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setPreferredSize(new Dimension(320, 45));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(new Color(50, 55, 65));
        field.setForeground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 75, 85), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        field.setCaretColor(new Color(0, 173, 181));
        field.setEchoChar('•');
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 173, 181), 2),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 75, 85), 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
        });
        
        return field;
    }

    private void styleLoginButton(JButton button) {
        button.setPreferredSize(new Dimension(320, 50));
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBackground(new Color(0, 173, 181));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0, 190, 200));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(0, 173, 181));
            }
        });
        
        button.addActionListener(e -> performLogin());
    }

    private void loadLogoFromDatabase() {
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "SELECT setting_value FROM system_settings WHERE setting_key = 'LOGO_PRIMARY'";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String base64Logo = rs.getString("setting_value");
                                if (base64Logo != null && !base64Logo.isEmpty()) {
                                    // Decode base64 image
                                    byte[] imageBytes = Base64.getDecoder().decode(base64Logo);
                                    ImageIcon icon = new ImageIcon(imageBytes);
                                    
                                    // Resize to 180x180
                                    Image img = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                                    return new ImageIcon(img);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error loading logo from database: " + e.getMessage());
                }
                return null; // Return null if no logo found
            }
            
            @Override
            protected void done() {
                try {
                    ImageIcon logo = get();
                    if (logo != null) {
                        // Set the loaded logo
                        logoLabel.setIcon(logo);
                    } else {
                        // Use the clear logo if database logo not found
                        logoLabel.setIcon(clearLogo);
                    }
                } catch (Exception e) {
                    logoLabel.setIcon(clearLogo);
                }
            }
        };
        worker.execute();
    }

    private ImageIcon createCleanLogo() {
        BufferedImage img = new BufferedImage(180, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Clean circular background
        g2d.setColor(new Color(0, 173, 181));
        g2d.fillOval(0, 0, 180, 180);
        
        // Clean white text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 36));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "MS";
        
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int x = (180 - textWidth) / 2;
        int y = (180 - textHeight) / 2 + fm.getAscent() - 20;
        
        g2d.drawString(text, x, y);
        
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
        fm = g2d.getFontMetrics();
        text = "CF";
        textWidth = fm.stringWidth(text);
        x = (180 - textWidth) / 2;
        y = (180 - textHeight) / 2 + fm.getAscent() + 30;
        
        g2d.drawString(text, x, y);
        
        g2d.dispose();
        return new ImageIcon(img);
    }

    private void performLogin() {
        if (isLoggingIn) return;
        
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password");
            return;
        }

        isLoggingIn = true;
        loginButton.setEnabled(false);
        loginButton.setText("AUTHENTICATING...");
        statusLabel.setText("");

        SwingWorker<LoginResult, Void> worker = new SwingWorker<LoginResult, Void>() {
            @Override
            protected LoginResult doInBackground() throws Exception {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "SELECT employee_id, name, role FROM employees WHERE name = ? AND password = ? AND is_active = TRUE";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, username);
                        stmt.setString(2, password);
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int id = rs.getInt("employee_id");
                                String name = rs.getString("name");
                                String role = rs.getString("role");
                                return new LoginResult(true, null, new UserData(id, name, role));
                            } else {
                                return new LoginResult(false, "Invalid username or password", null);
                            }
                        }
                    }
                } catch (SQLException ex) {
                    return new LoginResult(false, "Database connection error", null);
                }
            }

            @Override
            protected void done() {
                try {
                    LoginResult result = get();
                    if (result.success) {
                        statusLabel.setText("Login successful!");
                        statusLabel.setForeground(new Color(0, 200, 100));
                        
                        Timer timer = new Timer(500, e -> {
                            if ("admin".equals(result.userData.role)) {
                                ScreenManager.getInstance().showScreen(new AdminDashboard(result.userData.id, result.userData.name));
                            } else {
                                ScreenManager.getInstance().showScreen(new EmployeeDashboard(result.userData.id, result.userData.name));
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                        
                    } else {
                        statusLabel.setText(result.message);
                        statusLabel.setForeground(new Color(255, 100, 100));
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Login failed. Try again.");
                    statusLabel.setForeground(new Color(255, 100, 100));
                } finally {
                    isLoggingIn = false;
                    loginButton.setEnabled(true);
                    loginButton.setText("LOGIN");
                }
            }
        };
        worker.execute();
    }

    private static class LoginResult {
        boolean success;
        String message;
        UserData userData;
        
        LoginResult(boolean success, String message, UserData userData) {
            this.success = success;
            this.message = message;
            this.userData = userData;
        }
    }
    
    private static class UserData {
        int id;
        String name;
        String role;
        
        UserData(int id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }
    }
}