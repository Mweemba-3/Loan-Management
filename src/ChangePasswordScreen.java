import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ChangePasswordScreen extends JPanel {
    private int employeeId;
    private String userRole;
    private String employeeName;
    private JPasswordField currentPasswordField, newPasswordField, confirmPasswordField;
    private JButton saveButton, cancelButton;
    
    public ChangePasswordScreen(int employeeId, String userRole, String employeeName) {
        this.employeeId = employeeId;
        this.userRole = userRole;
        this.employeeName = employeeName;
        System.out.println("ChangePasswordScreen created - Employee: " + employeeName + ", Role: " + userRole);
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(240, 242, 245));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(new Color(240, 242, 245));
        
        JButton backButton = new JButton("← Back");
        styleButton(backButton, new Color(100, 100, 100), new Color(80, 80, 80));
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.addActionListener(e -> goBackToDashboard());
        headerPanel.add(backButton, BorderLayout.WEST);
        
        JLabel titleLabel = new JLabel("CHANGE PASSWORD", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(44, 62, 80));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Main Content Panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(new Color(240, 242, 245));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(40, 40, 40, 40)
        ));
        
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(15, 15, 15, 15);
        formGbc.anchor = GridBagConstraints.WEST;
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Current Password
        formGbc.gridx = 0; formGbc.gridy = 0;
        JLabel currentLabel = new JLabel("Current Password:");
        currentLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(currentLabel, formGbc);
        
        formGbc.gridx = 1;
        currentPasswordField = new JPasswordField(25);
        stylePasswordField(currentPasswordField);
        formPanel.add(currentPasswordField, formGbc);
        
        // New Password
        formGbc.gridx = 0; formGbc.gridy = 1;
        JLabel newLabel = new JLabel("New Password:");
        newLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(newLabel, formGbc);
        
        formGbc.gridx = 1;
        newPasswordField = new JPasswordField(25);
        stylePasswordField(newPasswordField);
        formPanel.add(newPasswordField, formGbc);
        
        // Confirm New Password
        formGbc.gridx = 0; formGbc.gridy = 2;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(confirmLabel, formGbc);
        
        formGbc.gridx = 1;
        confirmPasswordField = new JPasswordField(25);
        stylePasswordField(confirmPasswordField);
        formPanel.add(confirmPasswordField, formGbc);
        
        // Password requirements
        formGbc.gridx = 0; formGbc.gridy = 3; formGbc.gridwidth = 2;
        JLabel requirementsLabel = new JLabel("• Password must be at least 4 characters long");
        requirementsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        requirementsLabel.setForeground(new Color(100, 100, 100));
        formPanel.add(requirementsLabel, formGbc);
        
        // Button Panel
        formGbc.gridx = 0; formGbc.gridy = 4; formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        saveButton = new JButton("💾 Save Password");
        styleButton(saveButton, new Color(46, 125, 50), new Color(39, 105, 42));
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveButton.addActionListener(e -> changePassword());
        buttonPanel.add(saveButton);
        
        cancelButton = new JButton("❌ Cancel");
        styleButton(cancelButton, new Color(120, 120, 120), new Color(100, 100, 100));
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.addActionListener(e -> goBackToDashboard());
        buttonPanel.add(cancelButton);
        
        formPanel.add(buttonPanel, formGbc);
        
        gbc.gridx = 0; gbc.gridy = 0;
        contentPanel.add(formPanel, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private void stylePasswordField(JPasswordField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(250, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }
    
    private void styleButton(JButton button, Color bgColor, Color hoverColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 24, 12, 24));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
    }
    
    private void goBackToDashboard() {
        System.out.println("Navigating back to dashboard - Role: " + userRole);
        
        try {
            if ("admin".equalsIgnoreCase(userRole)) {
                AdminDashboard dashboard = new AdminDashboard(employeeId, employeeName);
                ScreenManager.getInstance().showScreen(dashboard);
            } else {
                EmployeeDashboard dashboard = new EmployeeDashboard(employeeId, employeeName);
                ScreenManager.getInstance().showScreen(dashboard);
            }
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error navigating to dashboard", "Navigation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void changePassword() {
        String currentPassword = new String(currentPasswordField.getPassword()).trim();
        String newPassword = new String(newPasswordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
        
        // Validation
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all password fields");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("New passwords do not match");
            return;
        }
        
        if (newPassword.length() < 4) {
            showError("Password must be at least 4 characters long");
            return;
        }
        
        if (newPassword.equals(currentPassword)) {
            showError("New password must be different from current password");
            return;
        }
        
        // Process password change
        saveButton.setEnabled(false);
        saveButton.setText("Processing...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return updatePasswordInDatabase(currentPassword, newPassword);
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(ChangePasswordScreen.this, 
                            "Password changed successfully!", "Success", 
                            JOptionPane.INFORMATION_MESSAGE);
                        clearForm();
                        logActivity("Password Changed", "Password updated successfully");
                        goBackToDashboard(); // Navigate back after success
                    } else {
                        showError("Current password is incorrect or update failed");
                    }
                } catch (Exception ex) {
                    showError("Error: " + ex.getMessage());
                } finally {
                    saveButton.setEnabled(true);
                    saveButton.setText("💾 Save Password");
                }
            }
        };
        worker.execute();
    }
    
    private boolean updatePasswordInDatabase(String currentPassword, String newPassword) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            
            // First verify current password
            String verifySql = "SELECT password FROM employees WHERE employee_id = ? AND is_active = TRUE";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setInt(1, employeeId);
            ResultSet rs = verifyStmt.executeQuery();
            
            if (!rs.next()) {
                return false; // Employee not found
            }
            
            String storedPassword = rs.getString("password");
            if (!currentPassword.equals(storedPassword)) {
                return false; // Current password doesn't match
            }
            
            // Update password
            String updateSql = "UPDATE employees SET password = ? WHERE employee_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, newPassword);
            updateStmt.setInt(2, employeeId);
            
            int rowsUpdated = updateStmt.executeUpdate();
            return rowsUpdated > 0;
            
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }
    
    private void clearForm() {
        currentPasswordField.setText("");
        newPasswordField.setText("");
        confirmPasswordField.setText("");
    }
    
    private void logActivity(String action, String details) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO audit_logs (employee_id, employee_name, action, details) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, employeeId);
            stmt.setString(2, employeeName);
            stmt.setString(3, action);
            stmt.setString(4, details);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Audit log error: " + ex.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}