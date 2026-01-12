import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

public class LogoManagementScreen extends JPanel {
    private int userId;
    private String userRole;
    private JLabel primaryLogoLabel, reportLogoLabel;
    private JButton uploadPrimaryBtn, uploadReportBtn, resetPrimaryBtn, resetReportBtn, backBtn;
    
    // Color scheme matching your database screens
    private final Color PRIMARY_BLUE = new Color(0, 100, 200);
    private final Color LIGHT_BG = new Color(245, 245, 245);
    private final Color CARD_BG = Color.WHITE;
    private final Color TEXT_DARK = new Color(50, 50, 50);
    private final Color TEXT_GRAY = new Color(100, 100, 100);
    private final Color RED_ALERT = new Color(220, 80, 80);
    private final Color GREEN_SUCCESS = new Color(80, 180, 80);
    private final Color ORANGE_WARNING = new Color(220, 140, 60);
    private final Color PURPLE = new Color(148, 85, 211);
    private final Color BORDER_COLOR = new Color(220, 220, 220);
    
    public LogoManagementScreen(int userId, String userRole) {
        this.userId = userId;
        this.userRole = userRole;
        setBackground(LIGHT_BG);
        initUI();
        loadCurrentLogosAsync();
    }
    
    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(LIGHT_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Back button
        backBtn = new JButton("← Back to Dashboard");
        styleButton(backBtn, TEXT_GRAY);
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.addActionListener(e -> goBackToDashboardAsync());
        headerPanel.add(backBtn, BorderLayout.WEST);
        
        JLabel titleLabel = new JLabel("🖼️ LOGO MANAGEMENT", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY_BLUE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Main Content Panel
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        mainPanel.setBackground(LIGHT_BG);
        
        // Primary Logo Card
        JPanel primaryCard = createLogoCard(
            "🎯 PRIMARY LOGO", 
            "Used on dashboards and login screens",
            "Recommended: 200x200px, PNG format"
        );
        
        // Report Logo Card
        JPanel reportCard = createLogoCard(
            "📄 REPORT LOGO", 
            "Used on printed reports and documents",
            "Recommended: 300x150px, PNG format"
        );
        
        mainPanel.add(primaryCard);
        mainPanel.add(reportCard);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Get references from cards
        primaryLogoLabel = getLogoLabelFromCard(primaryCard);
        reportLogoLabel = getLogoLabelFromCard(reportCard);
        uploadPrimaryBtn = getUploadButtonFromCard(primaryCard);
        uploadReportBtn = getUploadButtonFromCard(reportCard);
        resetPrimaryBtn = getResetButtonFromCard(primaryCard);
        resetReportBtn = getResetButtonFromCard(reportCard);
        
        // Add action listeners
        uploadPrimaryBtn.addActionListener(e -> uploadLogoAsync(LogoManager.LOGO_PRIMARY, primaryLogoLabel));
        uploadReportBtn.addActionListener(e -> uploadLogoAsync(LogoManager.LOGO_REPORT, reportLogoLabel));
        resetPrimaryBtn.addActionListener(e -> resetLogoAsync(LogoManager.LOGO_PRIMARY, primaryLogoLabel));
        resetReportBtn.addActionListener(e -> resetLogoAsync(LogoManager.LOGO_REPORT, reportLogoLabel));
        
        // Footer Panel
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(LIGHT_BG);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        JLabel footerLabel = new JLabel("💡 Tips: Use PNG format for transparency, JPG for photos, minimum 200x200 pixels");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footerLabel.setForeground(TEXT_GRAY);
        footerPanel.add(footerLabel);
        
        add(footerPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createLogoCard(String title, String description, String specs) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Title
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(PRIMARY_BLUE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        card.add(titleLabel, BorderLayout.NORTH);
        
        // Logo display area
        JPanel logoContainer = new JPanel(new GridBagLayout());
        logoContainer.setOpaque(false);
        logoContainer.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Logo panel with border
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setPreferredSize(new Dimension(180, 180));
        logoPanel.setBackground(new Color(250, 250, 250));
        logoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Logo label - DISPLAYS ORIGINAL IMAGE WITHOUT MODIFICATION
        JLabel logoLabel = new JLabel("No Logo", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoLabel.setForeground(TEXT_GRAY);
        logoLabel.setVerticalAlignment(SwingConstants.CENTER);
        logoLabel.setPreferredSize(new Dimension(160, 160));
        
        logoPanel.add(logoLabel, BorderLayout.CENTER);
        logoContainer.add(logoPanel);
        card.add(logoContainer, BorderLayout.CENTER);
        
        // Description Panel
        JPanel textPanel = new JPanel(new BorderLayout(5, 5));
        textPanel.setOpaque(false);
        
        JLabel descLabel = new JLabel("<html><div style='text-align: center;'>" + description + "</div></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(TEXT_DARK);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel specsLabel = new JLabel("<html><div style='text-align: center; color: #666;'>" + specs + "</div></html>");
        specsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        specsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        textPanel.add(descLabel, BorderLayout.NORTH);
        textPanel.add(specsLabel, BorderLayout.SOUTH);
        card.add(textPanel, BorderLayout.SOUTH);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        JButton uploadBtn = new JButton("📤 Upload");
        styleSmallButton(uploadBtn, PRIMARY_BLUE);
        
        JButton resetBtn = new JButton("↺ Reset");
        styleSmallButton(resetBtn, TEXT_GRAY);
        
        buttonPanel.add(uploadBtn);
        buttonPanel.add(resetBtn);
        
        // Store references
        card.putClientProperty("logoLabel", logoLabel);
        card.putClientProperty("uploadBtn", uploadBtn);
        card.putClientProperty("resetBtn", resetBtn);
        
        // Wrap card with button panel
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(card, BorderLayout.CENTER);
        container.add(buttonPanel, BorderLayout.SOUTH);
        
        return container;
    }
    
    private JLabel getLogoLabelFromCard(JPanel card) {
        return (JLabel) ((JPanel) card.getComponent(0)).getClientProperty("logoLabel");
    }
    
    private JButton getUploadButtonFromCard(JPanel card) {
        return (JButton) ((JPanel) card.getComponent(0)).getClientProperty("uploadBtn");
    }
    
    private JButton getResetButtonFromCard(JPanel card) {
        return (JButton) ((JPanel) card.getComponent(0)).getClientProperty("resetBtn");
    }
    
    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(adjustColor(bgColor, -30), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(adjustColor(bgColor, -20));
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(adjustColor(bgColor, -40), 1),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
                ));
            }
            
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(adjustColor(bgColor, -30), 1),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15)
                ));
            }
        });
    }
    
    private void styleSmallButton(JButton button, Color bgColor) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(adjustColor(bgColor, -20), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(adjustColor(bgColor, -15));
            }
            
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
    }
    
    private Color adjustColor(Color color, int amount) {
        int r = Math.max(0, Math.min(255, color.getRed() + amount));
        int g = Math.max(0, Math.min(255, color.getGreen() + amount));
        int b = Math.max(0, Math.min(255, color.getBlue() + amount));
        return new Color(r, g, b);
    }
    
    // ==================== ASYNC METHODS ====================
    
    private void goBackToDashboardAsync() {
        showLoading("Returning to dashboard...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT name FROM employees WHERE employee_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getString("name") : "User";
                    }
                }
            }
        }, 
        employeeName -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                if ("admin".equals(userRole)) {
                    ScreenManager.getInstance().showScreen(new AdminDashboard(userId, employeeName));
                } else {
                    ScreenManager.getInstance().showScreen(new EmployeeDashboard(userId, employeeName));
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error: " + e.getMessage());
                // Still go back
                if ("admin".equals(userRole)) {
                    ScreenManager.getInstance().showScreen(new AdminDashboard(userId, "User"));
                } else {
                    ScreenManager.getInstance().showScreen(new EmployeeDashboard(userId, "User"));
                }
            });
        });
    }
    
    private void loadCurrentLogosAsync() {
        showLoading("Loading logos...");
        
        // Load primary logo
        AsyncDatabaseService.executeAsync(() -> {
            return loadLogoFromDatabase(LogoManager.LOGO_PRIMARY);
        }, 
        primaryIcon -> {
            SwingUtilities.invokeLater(() -> {
                if (primaryIcon != null) {
                    // Use HIGH-QUALITY proportional scaling
                    primaryLogoLabel.setIcon(scaleProportionalHighQuality(primaryIcon, 160, 160));
                    primaryLogoLabel.setText("");
                } else {
                    primaryLogoLabel.setIcon(null);
                    primaryLogoLabel.setText("No Logo");
                }
                // Now load report logo
                loadReportLogoAsync();
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                primaryLogoLabel.setIcon(null);
                primaryLogoLabel.setText("Error");
                loadReportLogoAsync();
            });
        });
    }
    
    private void loadReportLogoAsync() {
        AsyncDatabaseService.executeAsync(() -> {
            return loadLogoFromDatabase(LogoManager.LOGO_REPORT);
        }, 
        reportIcon -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                if (reportIcon != null) {
                    reportLogoLabel.setIcon(scaleProportionalHighQuality(reportIcon, 160, 160));
                    reportLogoLabel.setText("");
                } else {
                    reportLogoLabel.setIcon(null);
                    reportLogoLabel.setText("No Logo");
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                reportLogoLabel.setIcon(null);
                reportLogoLabel.setText("Error");
            });
        });
    }
    
    private ImageIcon loadLogoFromDatabase(String logoType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, logoType);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String base64 = rs.getString("setting_value");
                        if (base64 != null && !base64.isEmpty()) {
                            // Decode base64 to get original image bytes
                            byte[] imageBytes = Base64.getDecoder().decode(base64);
                            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                            
                            // Read the ORIGINAL image without any modifications
                            BufferedImage originalImage = ImageIO.read(bis);
                            
                            if (originalImage != null) {
                                return new ImageIcon(originalImage);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading logo from database: " + e.getMessage());
        }
        return null;
    }
    
    private ImageIcon scaleProportionalHighQuality(ImageIcon original, int maxWidth, int maxHeight) {
        if (original == null) return null;
        
        int originalWidth = original.getIconWidth();
        int originalHeight = original.getIconHeight();
        
        // Calculate proportional scaling
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        // Use BufferedImage for HIGH-QUALITY scaling
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        
        // Set maximum quality rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        
        // Draw original image with high-quality scaling
        g2d.drawImage(original.getImage(), 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return new ImageIcon(scaledImage);
    }
    
    private void uploadLogoAsync(String logoType, JLabel logoLabel) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Logo Image");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image Files (PNG, JPG, JPEG, GIF)", "png", "jpg", "jpeg", "gif", "bmp"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Validate file size (max 2MB)
            if (file.length() > 2 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this,
                    "❌ File too large!\nMaximum size: 2MB\nYour file: " + 
                    String.format("%.1f", file.length() / (1024.0 * 1024.0)) + "MB",
                    "File Too Large", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            showLoading("Uploading logo...");
            
            AsyncDatabaseService.executeAsync(() -> {
                try {
                    // Load original image AS IS - NO MODIFICATIONS
                    BufferedImage originalImage = ImageIO.read(file);
                    
                    // Validate image
                    if (originalImage == null) {
                        throw new Exception("Invalid image file or format not supported");
                    }
                    
                    // Create ImageIcon from original image
                    ImageIcon originalIcon = new ImageIcon(originalImage);
                    
                    // Save ORIGINAL image to database - NO MODIFICATIONS
                    boolean saved = saveLogoToDatabase(logoType, originalIcon);
                    if (!saved) {
                        throw new Exception("Failed to save logo to database");
                    }
                    
                    // Return high-quality scaled version for display
                    return scaleProportionalHighQuality(originalIcon, 160, 160);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Upload failed: " + e.getMessage());
                }
            }, 
            displayIcon -> {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    if (displayIcon != null) {
                        logoLabel.setIcon(displayIcon);
                        logoLabel.setText("");
                        JOptionPane.showMessageDialog(this, 
                            "✅ Logo uploaded successfully!\n\n" +
                            "Original size: " + getImageDimensions(file) + "\n" +
                            "Display size: " + displayIcon.getIconWidth() + "x" + displayIcon.getIconHeight() + "\n" +
                            "Format: " + getFileExtension(file) + "\n" +
                            "Saved in original quality",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                        
                        logActivityAsync("Logo Upload", "Uploaded " + logoType + " logo (" + file.getName() + ")");
                    }
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    showError("Upload failed: " + e.getMessage());
                });
            });
        }
    }
    
    private String getImageDimensions(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                return img.getWidth() + "x" + img.getHeight() + " pixels";
            }
        } catch (Exception e) {
            // Ignore error
        }
        return "Unknown";
    }
    
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(lastDot + 1).toUpperCase();
        }
        return "Unknown";
    }
    
    private boolean saveLogoToDatabase(String logoType, ImageIcon icon) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Convert to base64 - preserve original quality
            String base64Image = convertImageToBase64(icon);
            if (base64Image == null) {
                return false;
            }
            
            // Check if setting exists
            String checkSql = "SELECT COUNT(*) FROM system_settings WHERE setting_key = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, logoType);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next() && rs.getInt(1) > 0) {
                    // Update existing
                    String updateSql = "UPDATE system_settings SET setting_value = ?, " +
                                     "last_updated = CURRENT_TIMESTAMP " +
                                     "WHERE setting_key = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, base64Image);
                        updateStmt.setString(2, logoType);
                        return updateStmt.executeUpdate() > 0;
                    }
                } else {
                    // Insert new
                    String insertSql = "INSERT INTO system_settings (setting_key, setting_name, " +
                                     "setting_value, setting_type, category) " +
                                     "VALUES (?, ?, ?, 'IMAGE', 'Branding')";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, logoType);
                        insertStmt.setString(2, getSettingName(logoType));
                        insertStmt.setString(3, base64Image);
                        return insertStmt.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving logo to database: " + e.getMessage());
            return false;
        }
    }
    
    private String convertImageToBase64(ImageIcon icon) {
        try {
            Image image = icon.getImage();
            
            // Create buffered image from original
            BufferedImage bufferedImage;
            if (image instanceof BufferedImage) {
                bufferedImage = (BufferedImage) image;
            } else {
                bufferedImage = new BufferedImage(
                    icon.getIconWidth(), 
                    icon.getIconHeight(), 
                    BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2d = bufferedImage.createGraphics();
                // Draw original image AS IS
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
            }
            
            // Convert to PNG for lossless quality
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", baos);
            
            // Encode to base64
            return Base64.getEncoder().encodeToString(baos.toByteArray());
            
        } catch (Exception e) {
            System.err.println("Error converting image to base64: " + e.getMessage());
            return null;
        }
    }
    
    private String getSettingName(String logoType) {
        if (LogoManager.LOGO_PRIMARY.equals(logoType)) {
            return "Primary Logo";
        } else if (LogoManager.LOGO_REPORT.equals(logoType)) {
            return "Report Logo";
        }
        return "Logo";
    }
    
    private void resetLogoAsync(String logoType, JLabel logoLabel) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "🔄 RESET LOGO\n\n" +
            "This will remove the current logo and set it to empty.\n" +
            "The logo will need to be uploaded again.\n\n" +
            "Are you sure you want to reset this logo?",
            "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            showLoading("Resetting logo...");
            
            AsyncDatabaseService.executeAsync(() -> {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE system_settings SET setting_value = NULL WHERE setting_key = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, logoType);
                        return stmt.executeUpdate() > 0;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Database error: " + e.getMessage());
                }
            }, 
            success -> {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    logoLabel.setIcon(null);
                    logoLabel.setText("No Logo");
                    JOptionPane.showMessageDialog(this, 
                        "✅ Logo reset successfully!\n\n" +
                        "You can now upload a new logo.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                    logActivityAsync("Logo Reset", "Reset " + logoType + " logo");
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    showError("Reset failed: " + e.getMessage());
                });
            });
        }
    }
    
    private void logActivityAsync(String action, String details) {
        AsyncDatabaseService.logAsync(userId, action, details);
    }
    
    // ==================== HELPER METHODS ====================
    
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }
    
    private void showLoading(String message) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
    
    private void hideLoading() {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
}