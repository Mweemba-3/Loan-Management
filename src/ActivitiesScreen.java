import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ActivitiesScreen extends JPanel {
    private int userId;
    private String userRole;
    private JTable activitiesTable;
    private DefaultTableModel tableModel;
    private JButton clearButton, refreshButton, backButton;
    private JComboBox<String> filterCombo;
    private JTextField searchField;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    
    // Color scheme matching your database screens
    private final Color PRIMARY_BLUE = new Color(0, 100, 200);
    private final Color LIGHT_BG = new Color(245, 245, 245);
    private final Color CARD_BG = Color.WHITE;
    private final Color TEXT_DARK = new Color(50, 50, 50);
    private final Color TEXT_GRAY = new Color(100, 100, 100);
    private final Color RED_ALERT = new Color(220, 80, 80);
    private final Color GREEN_SUCCESS = new Color(80, 180, 80);
    private final Color ORANGE_WARNING = new Color(220, 140, 60);
    private final Color BORDER_COLOR = new Color(220, 220, 220);
    
    public ActivitiesScreen(int userId, String userRole) {
        this.userId = userId;
        this.userRole = userRole;
        initUI();
        loadActivitiesDataAsync();
        logActivityAsync("Activities Access", "Accessed activity logs");
    }
    
    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBackground(LIGHT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(LIGHT_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Title
        JLabel titleLabel = new JLabel("📊 ACTIVITY LOGS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY_BLUE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Back button - FIXED: Added to header panel
        backButton = new JButton("← Back to Dashboard");
        styleButton(backButton, TEXT_GRAY);
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.addActionListener(e -> goBackToDashboard());
        headerPanel.add(backButton, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Control Panel
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBackground(LIGHT_BG);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setBackground(LIGHT_BG);
        
        // Search field
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchLabel.setForeground(TEXT_DARK);
        filterPanel.add(searchLabel);
        
        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.setToolTipText("Search by employee name, action, or details");
        searchField.addActionListener(e -> filterActivitiesAsync());
        filterPanel.add(searchField);
        
        // Filter by action type
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        filterLabel.setForeground(TEXT_DARK);
        filterPanel.add(filterLabel);
        
        filterCombo = new JComboBox<>(new String[]{"All Activities", "LOGIN", "LOGOUT", "CLIENT_CREATED", 
                                                  "CLIENT_UPDATED", "LOAN_CREATED", "LOAN_APPROVED", 
                                                  "LOAN_REJECTED", "PAYMENT_INITIATED", "PAYMENT_APPROVED",
                                                  "PAYMENT_REJECTED", "USER_MANAGEMENT", "REPORT_GENERATED",
                                                  "SETTINGS_UPDATED", "DASHBOARD_ACCESS"});
        filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        filterCombo.addActionListener(e -> filterActivitiesAsync());
        filterPanel.add(filterCombo);
        
        controlPanel.add(filterPanel, BorderLayout.CENTER);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(LIGHT_BG);
        
        refreshButton = new JButton("🔄 Refresh");
        styleButton(refreshButton, PRIMARY_BLUE);
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.addActionListener(e -> loadActivitiesDataAsync());
        buttonPanel.add(refreshButton);
        
        // Clear button (admin only)
        if ("admin".equals(userRole)) {
            clearButton = new JButton("🗑️ Clear Logs");
            styleButton(clearButton, RED_ALERT);
            clearButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            clearButton.addActionListener(e -> clearActivityLogsAsync());
            buttonPanel.add(clearButton);
        }
        
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.CENTER);
        
        // Table Panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(CARD_BG);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        String[] columns = {"Log ID", "Employee", "Action", "Date", "Details"};
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
        
        activitiesTable = new JTable(tableModel);
        styleTable(activitiesTable);
        
        // FIXED: Removed unnecessary table to the right
        JScrollPane scrollPane = new JScrollPane(activitiesTable);
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS); // FIXED: Better column sizing
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(PRIMARY_BLUE);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        
        // Set column widths
        int[] widths = {70, 120, 150, 140, 350}; // FIXED: Adjusted for better fit
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        
        // Center align ID, Employee, Action, Date columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        for (int i = 0; i < table.getColumnCount() - 1; i++) { // All except details
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Custom renderer for details column with wrapping
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                
                JTextArea textArea = new JTextArea(value != null ? value.toString() : "");
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setEditable(false);
                textArea.setOpaque(true);
                
                if (isSelected) {
                    textArea.setBackground(table.getSelectionBackground());
                    textArea.setForeground(table.getSelectionForeground());
                } else {
                    textArea.setBackground(table.getBackground());
                    textArea.setForeground(TEXT_DARK);
                }
                
                textArea.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                textArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                return textArea;
            }
        });
        
        // FIXED: Set table size to fill available space
        table.setPreferredScrollableViewportSize(new Dimension(900, 400));
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
    
    private void goBackToDashboard() {
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
                showError("Error loading user details: " + e.getMessage());
                // Still go back even if error
                if ("admin".equals(userRole)) {
                    ScreenManager.getInstance().showScreen(new AdminDashboard(userId, "User"));
                } else {
                    ScreenManager.getInstance().showScreen(new EmployeeDashboard(userId, "User"));
                }
            });
        });
    }
    
    private void loadActivitiesDataAsync() {
        showLoading("Loading activity logs...");
        
        AsyncDatabaseService.executeAsync(() -> {
            List<Object[]> activities = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT log_id, employee_name, action, action_date, details " +
                           "FROM audit_logs ORDER BY action_date DESC LIMIT 500";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            activities.add(new Object[]{
                                rs.getInt("log_id"),
                                rs.getString("employee_name"),
                                rs.getString("action"),
                                dateFormat.format(rs.getTimestamp("action_date")),
                                rs.getString("details")
                            });
                        }
                    }
                }
            }
            return activities;
        }, 
        activities -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (Object[] row : activities) {
                    tableModel.addRow(row);
                }
                hideLoading();
                logActivityAsync("Data Load", "Loaded " + activities.size() + " activity records");
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error loading activities: " + e.getMessage());
            });
        });
    }
    
    private void filterActivitiesAsync() {
        String searchTerm = searchField.getText().trim();
        String filter = (String) filterCombo.getSelectedItem();
        
        showLoading("Filtering activities...");
        
        AsyncDatabaseService.executeAsync(() -> {
            List<Object[]> activities = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getConnection()) {
                StringBuilder sql = new StringBuilder(
                    "SELECT log_id, employee_name, action, action_date, details " +
                    "FROM audit_logs WHERE 1=1 "
                );
                
                if (!searchTerm.isEmpty()) {
                    sql.append("AND (employee_name ILIKE ? OR action ILIKE ? OR details ILIKE ?) ");
                }
                
                if (!"All Activities".equals(filter)) {
                    sql.append("AND action = ? ");
                }
                
                sql.append("ORDER BY action_date DESC LIMIT 500");
                
                try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                    int paramIndex = 1;
                    
                    if (!searchTerm.isEmpty()) {
                        String likeTerm = "%" + searchTerm + "%";
                        stmt.setString(paramIndex++, likeTerm);
                        stmt.setString(paramIndex++, likeTerm);
                        stmt.setString(paramIndex++, likeTerm);
                    }
                    
                    if (!"All Activities".equals(filter)) {
                        stmt.setString(paramIndex++, filter);
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            activities.add(new Object[]{
                                rs.getInt("log_id"),
                                rs.getString("employee_name"),
                                rs.getString("action"),
                                dateFormat.format(rs.getTimestamp("action_date")),
                                rs.getString("details")
                            });
                        }
                    }
                }
            }
            return activities;
        }, 
        activities -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (Object[] row : activities) {
                    tableModel.addRow(row);
                }
                hideLoading();
                logActivityAsync("Activity Filter", 
                    "Filtered: '" + searchTerm + "' | Action: " + filter + 
                    " | Found: " + activities.size() + " records");
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error filtering activities: " + e.getMessage());
            });
        });
    }
    
    private void clearActivityLogsAsync() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "🗑️ CLEAR ALL ACTIVITY LOGS\n\n" +
            "This action will permanently delete ALL activity logs from the system.\n" +
            "This cannot be undone.\n\n" +
            "Are you absolutely sure you want to proceed?",
            "Confirm Clear Logs", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) return;
        
        showLoading("Clearing activity logs...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String deleteSql = "DELETE FROM audit_logs";
                int rowsDeleted;
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    rowsDeleted = stmt.executeUpdate();
                }
                return rowsDeleted;
            }
        }, 
        rowsDeleted -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                if (rowsDeleted > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "✅ Successfully cleared " + rowsDeleted + " activity logs", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadActivitiesDataAsync();
                    logActivityAsync("Clear Logs", "Cleared all " + rowsDeleted + " activity logs");
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "No activity logs to clear.", 
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error clearing activity logs: " + e.getMessage());
            });
        });
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