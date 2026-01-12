import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class EmployeesScreen extends JPanel {
    private int userId;
    private String userRole;
    private JTable employeesTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, refreshButton, backButton;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
    
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
    
    public EmployeesScreen(int userId, String userRole) {
        this.userId = userId;
        this.userRole = userRole;
        initUI();
        loadEmployeesDataAsync();
        logActivityAsync("Employees Access", "Accessed employee management");
    }
    
    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBackground(LIGHT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(LIGHT_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Back button
        backButton = new JButton("← Back to Dashboard");
        styleButton(backButton, TEXT_GRAY);
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.addActionListener(e -> goBackToDashboardAsync());
        headerPanel.add(backButton, BorderLayout.WEST);
        
        JLabel titleLabel = new JLabel("👥 EMPLOYEE MANAGEMENT", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY_BLUE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Action Buttons Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionPanel.setBackground(LIGHT_BG);
        
        addButton = new JButton("➕ Add Employee");
        styleButton(addButton, GREEN_SUCCESS);
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addButton.addActionListener(e -> showAddEditDialog(null));
        actionPanel.add(addButton);
        
        editButton = new JButton("✏️ Edit Employee");
        styleButton(editButton, PRIMARY_BLUE);
        editButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        editButton.setEnabled(false);
        editButton.addActionListener(e -> editSelectedEmployee());
        actionPanel.add(editButton);
        
        deleteButton = new JButton("🗑️ Delete Employee");
        styleButton(deleteButton, RED_ALERT);
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedEmployeeAsync());
        actionPanel.add(deleteButton);
        
        refreshButton = new JButton("🔄 Refresh");
        styleButton(refreshButton, TEXT_GRAY);
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.addActionListener(e -> loadEmployeesDataAsync());
        actionPanel.add(refreshButton);
        
        add(actionPanel, BorderLayout.CENTER);
        
        // Table Panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(CARD_BG);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        String[] columns = {"ID", "Name", "Email", "Role", "Phone", "Status", "Created Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Integer.class;
                return String.class;
            }
        };
        
        employeesTable = new JTable(tableModel);
        styleTable(employeesTable);
        
        // Add selection listener
        employeesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(employeesTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD_BG);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        add(tablePanel, BorderLayout.SOUTH);
    }
    
    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(35);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(PRIMARY_BLUE);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        
        // Set column widths
        int[] widths = {60, 150, 180, 100, 120, 80, 150};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        
        // Center align ID, Role, Phone, Status columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 1 && i != 2 && i != 6) { // Center all except Name, Email, Created Date
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }
        
        // Custom renderer for role column
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                
                String role = value != null ? value.toString() : "employee";
                if ("admin".equals(role)) {
                    setForeground(PURPLE);
                } else {
                    setForeground(PRIMARY_BLUE);
                }
                
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                return this;
            }
        });
        
        // Custom renderer for status column
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                
                boolean isActive = "Active".equals(value);
                if (isActive) {
                    setForeground(GREEN_SUCCESS);
                } else {
                    setForeground(TEXT_GRAY);
                }
                
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                return this;
            }
        });
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
                ScreenManager.getInstance().showScreen(new AdminDashboard(userId, employeeName));
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                ScreenManager.getInstance().showScreen(new AdminDashboard(userId, "User"));
            });
        });
    }
    
    private void loadEmployeesDataAsync() {
        showLoading("Loading employees data...");
        
        AsyncDatabaseService.executeAsync(() -> {
            List<Object[]> employees = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT employee_id, name, email, role, phone, is_active, created_at " +
                           "FROM employees ORDER BY created_at DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            employees.add(new Object[]{
                                rs.getInt("employee_id"),
                                rs.getString("name"),
                                rs.getString("email") != null ? rs.getString("email") : "N/A",
                                rs.getString("role"),
                                rs.getString("phone") != null ? rs.getString("phone") : "N/A",
                                rs.getBoolean("is_active") ? "Active" : "Inactive",
                                dateFormat.format(rs.getTimestamp("created_at"))
                            });
                        }
                    }
                }
            }
            return employees;
        }, 
        employees -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (Object[] row : employees) {
                    tableModel.addRow(row);
                }
                hideLoading();
                logActivityAsync("Data Load", "Loaded " + employees.size() + " employee records");
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error loading employees: " + e.getMessage());
            });
        });
    }
    
    private void updateButtonStates() {
        int selectedRow = employeesTable.getSelectedRow();
        boolean hasSelection = selectedRow != -1;
        
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
    }
    
    private void showAddEditDialog(Integer employeeId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                   employeeId == null ? "➕ Add Employee" : "✏️ Edit Employee", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(CARD_BG);
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(CARD_BG);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        int row = 0;
        
        // Name field
        gbc.gridx = 0; gbc.gridy = row;
        JLabel nameLabel = new JLabel("Name *:");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(TEXT_DARK);
        contentPanel.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField();
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(nameField, gbc);
        
        // Email field
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        emailLabel.setForeground(TEXT_DARK);
        contentPanel.add(emailLabel, gbc);
        
        gbc.gridx = 1;
        JTextField emailField = new JTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(emailField, gbc);
        
        // Phone field
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel phoneLabel = new JLabel("Phone:");
        phoneLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        phoneLabel.setForeground(TEXT_DARK);
        contentPanel.add(phoneLabel, gbc);
        
        gbc.gridx = 1;
        JTextField phoneField = new JTextField();
        phoneField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(phoneField, gbc);
        
        // Role field
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel roleLabel = new JLabel("Role *:");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleLabel.setForeground(TEXT_DARK);
        contentPanel.add(roleLabel, gbc);
        
        gbc.gridx = 1;
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"employee", "admin"});
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(roleCombo, gbc);
        
        // Status field (only for edit)
        JComboBox<String> statusCombo = null;
        if (employeeId != null) {
            gbc.gridx = 0; gbc.gridy = ++row;
            JLabel statusLabel = new JLabel("Status:");
            statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            statusLabel.setForeground(TEXT_DARK);
            contentPanel.add(statusLabel, gbc);
            
            gbc.gridx = 1;
            statusCombo = new JComboBox<>(new String[]{"Active", "Inactive"});
            statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            contentPanel.add(statusCombo, gbc);
        }
        
        // Password field
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel passwordLabel = new JLabel(employeeId == null ? "Password *:" : "New Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passwordLabel.setForeground(employeeId == null ? PRIMARY_BLUE : TEXT_DARK);
        contentPanel.add(passwordLabel, gbc);
        
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(passwordField, gbc);
        
        // Confirm password field
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        confirmLabel.setForeground(employeeId == null ? PRIMARY_BLUE : TEXT_DARK);
        contentPanel.add(confirmLabel, gbc);
        
        gbc.gridx = 1;
        JPasswordField confirmPasswordField = new JPasswordField();
        confirmPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(confirmPasswordField, gbc);
        
        dialog.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(CARD_BG);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JButton saveButton = new JButton(employeeId == null ? "➕ Add Employee" : "💾 Save Changes");
        styleButton(saveButton, employeeId == null ? GREEN_SUCCESS : PRIMARY_BLUE);
        
        final JComboBox<String> finalStatusCombo = statusCombo;
        saveButton.addActionListener(e -> {
            if (validateEmployeeForm(nameField, emailField, phoneField, passwordField, 
                                   confirmPasswordField, employeeId != null)) {
                if (employeeId == null) {
                    addEmployeeAsync(nameField.getText(), emailField.getText(), phoneField.getText(),
                                   (String) roleCombo.getSelectedItem(), 
                                   new String(passwordField.getPassword()), dialog);
                } else {
                    boolean isActive = "Active".equals(finalStatusCombo.getSelectedItem());
                    updateEmployeeAsync(employeeId, nameField.getText(), emailField.getText(), 
                                      phoneField.getText(), (String) roleCombo.getSelectedItem(),
                                      new String(passwordField.getPassword()), isActive, dialog);
                }
            }
        });
        
        JButton cancelButton = new JButton("❌ Cancel");
        styleButton(cancelButton, TEXT_GRAY);
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // If editing, load existing data
        if (employeeId != null) {
            loadEmployeeDataAsync(employeeId, nameField, emailField, phoneField, roleCombo, statusCombo, dialog);
        }
        
        dialog.setVisible(true);
    }
    
    private void loadEmployeeDataAsync(int employeeId, JTextField nameField, JTextField emailField, 
                                     JTextField phoneField, JComboBox<String> roleCombo, 
                                     JComboBox<String> statusCombo, JDialog dialog) {
        showLoadingInDialog(dialog, "Loading employee data...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT name, email, phone, role, is_active FROM employees WHERE employee_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, employeeId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return new Object[]{
                                rs.getString("name"),
                                rs.getString("email"),
                                rs.getString("phone"),
                                rs.getString("role"),
                                rs.getBoolean("is_active")
                            };
                        }
                    }
                }
            }
            return null;
        }, 
        data -> {
            SwingUtilities.invokeLater(() -> {
                hideLoadingInDialog(dialog);
                if (data != null) {
                    nameField.setText((String) data[0]);
                    emailField.setText((String) data[1]);
                    phoneField.setText((String) data[2]);
                    roleCombo.setSelectedItem(data[3]);
                    if (statusCombo != null) {
                        statusCombo.setSelectedItem((Boolean) data[4] ? "Active" : "Inactive");
                    }
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoadingInDialog(dialog);
                showError("Error loading employee data: " + e.getMessage());
            });
        });
    }
    
    private boolean validateEmployeeForm(JTextField nameField, JTextField emailField, 
                                       JTextField phoneField, JPasswordField passwordField, 
                                       JPasswordField confirmPasswordField, boolean isEdit) {
        if (nameField.getText().trim().isEmpty()) {
            showError("Please enter employee name");
            return false;
        }
        
        String email = emailField.getText().trim();
        if (!email.isEmpty() && !email.contains("@")) {
            showError("Please enter a valid email address");
            return false;
        }
        
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // For new employees, password is required
        if (!isEdit && password.isEmpty()) {
            showError("Please enter a password");
            return false;
        }
        
        // For new employees, validate password requirements
        if (!isEdit) {
            if (!password.equals(confirmPassword)) {
                showError("Passwords do not match");
                return false;
            }
            
            if (password.length() < 4) {
                showError("Password must be at least 4 characters long");
                return false;
            }
        }
        
        // For edits, password is optional but must match if provided
        if (isEdit && !password.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                showError("Passwords do not match");
                return false;
            }
            
            if (password.length() < 4) {
                showError("Password must be at least 4 characters long");
                return false;
            }
        }
        
        return true;
    }
    
    private void addEmployeeAsync(String name, String email, String phone, String role, 
                                String password, JDialog dialog) {
        showLoading("Adding employee...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "INSERT INTO employees (name, email, phone, role, password) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name.trim());
                    stmt.setString(2, email.trim().isEmpty() ? null : email.trim());
                    stmt.setString(3, phone.trim().isEmpty() ? null : phone.trim());
                    stmt.setString(4, role);
                    stmt.setString(5, password);
                    return stmt.executeUpdate();
                }
            }
        }, 
        result -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, 
                    "✅ Employee added successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                logActivityAsync("Employee Added", "Added new employee: " + name + " (" + role + ")");
                loadEmployeesDataAsync();
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error adding employee: " + e.getMessage());
            });
        });
    }
    
    private void editSelectedEmployee() {
        int selectedRow = employeesTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = employeesTable.convertRowIndexToModel(selectedRow);
            int employeeId = (Integer) tableModel.getValueAt(modelRow, 0);
            showAddEditDialog(employeeId);
        }
    }
    
    private void updateEmployeeAsync(int employeeId, String name, String email, String phone, 
                                   String role, String password, boolean isActive, JDialog dialog) {
        showLoading("Updating employee...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql;
                PreparedStatement stmt;
                
                if (password.isEmpty()) {
                    // Update without password
                    sql = "UPDATE employees SET name = ?, email = ?, phone = ?, role = ?, is_active = ? WHERE employee_id = ?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, name.trim());
                    stmt.setString(2, email.trim().isEmpty() ? null : email.trim());
                    stmt.setString(3, phone.trim().isEmpty() ? null : phone.trim());
                    stmt.setString(4, role);
                    stmt.setBoolean(5, isActive);
                    stmt.setInt(6, employeeId);
                } else {
                    // Update with password
                    sql = "UPDATE employees SET name = ?, email = ?, phone = ?, role = ?, password = ?, is_active = ? WHERE employee_id = ?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, name.trim());
                    stmt.setString(2, email.trim().isEmpty() ? null : email.trim());
                    stmt.setString(3, phone.trim().isEmpty() ? null : phone.trim());
                    stmt.setString(4, role);
                    stmt.setString(5, password);
                    stmt.setBoolean(6, isActive);
                    stmt.setInt(7, employeeId);
                }
                
                return stmt.executeUpdate();
            }
        }, 
        result -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, 
                    "✅ Employee updated successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                logActivityAsync("Employee Updated", "Updated employee: " + name + " (" + role + ")");
                loadEmployeesDataAsync();
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error updating employee: " + e.getMessage());
            });
        });
    }
    
    private void deleteSelectedEmployeeAsync() {
        int selectedRow = employeesTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select an employee to delete");
            return;
        }
        
        int modelRow = employeesTable.convertRowIndexToModel(selectedRow);
        int employeeId = (Integer) tableModel.getValueAt(modelRow, 0);
        String employeeName = (String) tableModel.getValueAt(modelRow, 1);
        String employeeRole = (String) tableModel.getValueAt(modelRow, 3);
        
        // Cannot delete yourself
        if (employeeId == userId) {
            showError("You cannot delete your own account");
            return;
        }
        
        // Cannot delete admins
        if ("admin".equals(employeeRole)) {
            showError("You cannot delete admin accounts");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "🗑️ DELETE EMPLOYEE\n\n" +
            "Employee: " + employeeName + "\n" +
            "ID: " + employeeId + "\n" +
            "Role: " + employeeRole + "\n\n" +
            "Are you sure you want to delete this employee?\n" +
            "This action cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            showLoading("Deleting employee...");
            
            AsyncDatabaseService.executeAsync(() -> {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    // First check if employee has any records
                    String checkSql = "SELECT COUNT(*) FROM clients WHERE created_by = ?";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setInt(1, employeeId);
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                return -1; // Employee has created records
                            }
                        }
                    }
                    
                    // Delete the employee
                    String deleteSql = "DELETE FROM employees WHERE employee_id = ?";
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setInt(1, employeeId);
                        return deleteStmt.executeUpdate();
                    }
                }
            }, 
            result -> {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    if (result == -1) {
                        showError("Cannot delete employee. This employee has created client records.");
                    } else if (result > 0) {
                        JOptionPane.showMessageDialog(this, 
                            "✅ Employee deleted successfully!", 
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                        
                        logActivityAsync("Employee Deleted", "Deleted employee: " + employeeName);
                        loadEmployeesDataAsync();
                    } else {
                        showError("Employee not found or could not be deleted");
                    }
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    showError("Error deleting employee: " + e.getMessage());
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
    
    private void showLoadingInDialog(JDialog dialog, String message) {
        dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
    
    private void hideLoadingInDialog(JDialog dialog) {
        dialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
}