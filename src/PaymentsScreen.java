import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class PaymentsScreen extends JPanel {
    private int userId;
    private String userRole;
    private JTable paymentsTable;
    private DefaultTableModel tableModel;
    private JButton approveButton, rejectButton, initiateButton, refreshButton, backButton;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    
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
    
    public PaymentsScreen(int userId, String userRole) {
        this.userId = userId;
        this.userRole = userRole;
        initUI();
        loadPaymentsDataAsync();
        logActivityAsync("Payments Access", "Accessed payments management");
    }
    
    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBackground(LIGHT_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(LIGHT_BG);
        
        // Back button
        backButton = new JButton("← Back to Dashboard");
        styleButton(backButton, TEXT_GRAY);
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backButton.addActionListener(e -> goBackToDashboard());
        headerPanel.add(backButton, BorderLayout.WEST);
        
        JLabel titleLabel = new JLabel("📊 PAYMENT MANAGEMENT", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY_BLUE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Main Content Panel
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(LIGHT_BG);
        
        // Search and Action Panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(LIGHT_BG);
        
        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setBackground(LIGHT_BG);
        
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchLabel.setForeground(TEXT_DARK);
        searchPanel.add(searchLabel);
        
        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.setToolTipText("Search by client name, ID number, phone, or loan number");
        searchField.addActionListener(e -> filterPaymentsAsync());
        searchPanel.add(searchField);
        
        // Status filter
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(TEXT_DARK);
        searchPanel.add(statusLabel);
        
        // FIXED: Using correct statuses from database - 'Pending', 'Approved', 'Rejected'
        statusFilterCombo = new JComboBox<>(new String[]{"All", "Pending", "Approved", "Rejected"});
        statusFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusFilterCombo.addActionListener(e -> filterPaymentsAsync());
        searchPanel.add(statusFilterCombo);
        
        JButton searchButton = new JButton("Search");
        styleButton(searchButton, PRIMARY_BLUE);
        searchButton.addActionListener(e -> filterPaymentsAsync());
        searchPanel.add(searchButton);
        
        topPanel.add(searchPanel, BorderLayout.WEST);
        
        // Action Buttons
        JPanel actionTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionTopPanel.setBackground(LIGHT_BG);
        
        refreshButton = new JButton("🔄 Refresh");
        styleButton(refreshButton, TEXT_GRAY);
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.addActionListener(e -> loadPaymentsDataAsync());
        actionTopPanel.add(refreshButton);
        
        initiateButton = new JButton("💳 New Payment");
        styleButton(initiateButton, GREEN_SUCCESS);
        initiateButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        initiateButton.addActionListener(e -> showNewPaymentDialog());
        actionTopPanel.add(initiateButton);
        
        topPanel.add(actionTopPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // Table Panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(CARD_BG);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // FIXED: Correct column names matching payment_receipts table
        String[] columns = {
            "Receipt #", "Loan #", "Client Name", "Amount", "Payment Date", 
            "Payment Mode", "Status", "Received By", "Approved By", "Notes"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) return Double.class; // Amount
                return String.class;
            }
        };
        
        paymentsTable = new JTable(tableModel);
        styleTable(paymentsTable);
        
        JScrollPane scrollPane = new JScrollPane(paymentsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(800, 400));
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        
        // Admin Action Panel
        if ("admin".equals(userRole)) {
            JPanel adminPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            adminPanel.setBackground(LIGHT_BG);
            adminPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            
            approveButton = new JButton("✅ Approve Payment");
            styleButton(approveButton, GREEN_SUCCESS);
            approveButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            approveButton.addActionListener(e -> approvePaymentAsync());
            approveButton.setEnabled(false);
            adminPanel.add(approveButton);
            
            rejectButton = new JButton("❌ Reject Payment");
            styleButton(rejectButton, RED_ALERT);
            rejectButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            rejectButton.addActionListener(e -> rejectPaymentDialog());
            rejectButton.setEnabled(false);
            adminPanel.add(rejectButton);
            
            mainPanel.add(adminPanel, BorderLayout.SOUTH);
        }
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Add selection listener
        paymentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
    }
    
    private void goBackToDashboard() {
        if ("admin".equals(userRole)) {
            // Load employee name async for dashboard
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
                    ScreenManager.getInstance().showScreen(new AdminDashboard(userId, employeeName));
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    ScreenManager.getInstance().showScreen(new AdminDashboard(userId, "User"));
                });
            });
        } else {
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
                    ScreenManager.getInstance().showScreen(new EmployeeDashboard(userId, employeeName));
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    ScreenManager.getInstance().showScreen(new EmployeeDashboard(userId, "User"));
                });
            });
        }
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
        int[] widths = {100, 100, 150, 100, 100, 80, 80, 120, 120, 150};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        
        // Center align all columns except notes
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Status column renderer with colors
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                
                String status = value.toString();
                switch (status) {
                    case "Approved":
                        setForeground(GREEN_SUCCESS);
                        break;
                    case "Rejected":
                        setForeground(RED_ALERT);
                        break;
                    case "Pending":
                        setForeground(ORANGE_WARNING);
                        break;
                    default:
                        setForeground(TEXT_DARK);
                }
                
                return this;
            }
        });
        
        // Amount column renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Double) {
                    setText(String.format("ZMW %,.2f", (Double) value));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                setForeground(TEXT_DARK);
                return this;
            }
        });
    }
    
    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(adjustColor(bgColor, -20));
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
    
    private void loadPaymentsDataAsync() {
        showLoading("Loading payments data...");
        
        AsyncDatabaseService.executeAsync(() -> {
            List<Object[]> payments = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getConnection()) {
                // FIXED: Using correct table joins and columns from your database
                String sql = "SELECT pr.receipt_number, l.loan_number, " +
                           "CONCAT(c.first_name, ' ', c.last_name) as client_name, " +
                           "pr.amount, pr.payment_date, pr.payment_mode, pr.status, " +
                           "e1.name as received_by, COALESCE(e2.name, 'N/A') as approved_by, " +
                           "COALESCE(pr.notes, '') as notes " +
                           "FROM payment_receipts pr " +
                           "JOIN loans l ON pr.loan_id = l.loan_id " +
                           "JOIN clients c ON l.client_id = c.client_id " +
                           "JOIN employees e1 ON pr.received_by = e1.employee_id " +
                           "LEFT JOIN employees e2 ON pr.approved_by = e2.employee_id " +
                           "ORDER BY pr.created_at DESC";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            payments.add(new Object[]{
                                rs.getString("receipt_number"),
                                rs.getString("loan_number"),
                                rs.getString("client_name"),
                                rs.getDouble("amount"),
                                formatDate(rs.getDate("payment_date")),
                                rs.getString("payment_mode"),
                                rs.getString("status"),
                                rs.getString("received_by"),
                                rs.getString("approved_by"),
                                rs.getString("notes")
                            });
                        }
                    }
                }
            }
            return payments;
        }, 
        payments -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (Object[] row : payments) {
                    tableModel.addRow(row);
                }
                hideLoading();
                logActivityAsync("Data Load", "Loaded " + payments.size() + " payment records");
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error loading payments: " + e.getMessage());
            });
        });
    }
    
    private void filterPaymentsAsync() {
        String searchTerm = searchField.getText().trim();
        String statusFilter = (String) statusFilterCombo.getSelectedItem();
        
        showLoading("Searching payments...");
        
        AsyncDatabaseService.executeAsync(() -> {
            List<Object[]> payments = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT pr.receipt_number, l.loan_number, " +
                           "CONCAT(c.first_name, ' ', c.last_name) as client_name, " +
                           "pr.amount, pr.payment_date, pr.payment_mode, pr.status, " +
                           "e1.name as received_by, COALESCE(e2.name, 'N/A') as approved_by, " +
                           "COALESCE(pr.notes, '') as notes " +
                           "FROM payment_receipts pr " +
                           "JOIN loans l ON pr.loan_id = l.loan_id " +
                           "JOIN clients c ON l.client_id = c.client_id " +
                           "JOIN employees e1 ON pr.received_by = e1.employee_id " +
                           "LEFT JOIN employees e2 ON pr.approved_by = e2.employee_id " +
                           "WHERE 1=1 ";
                
                if (!searchTerm.isEmpty()) {
                    sql += "AND (c.first_name ILIKE ? OR c.last_name ILIKE ? OR " +
                          "c.id_number ILIKE ? OR c.phone_number ILIKE ? OR " +
                          "l.loan_number ILIKE ? OR pr.receipt_number ILIKE ?) ";
                }
                
                if (!"All".equals(statusFilter)) {
                    sql += "AND pr.status = ? ";
                }
                
                sql += "ORDER BY pr.created_at DESC";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    int paramIndex = 1;
                    
                    if (!searchTerm.isEmpty()) {
                        String likeTerm = "%" + searchTerm + "%";
                        for (int i = 0; i < 6; i++) {
                            stmt.setString(paramIndex++, likeTerm);
                        }
                    }
                    
                    if (!"All".equals(statusFilter)) {
                        stmt.setString(paramIndex++, statusFilter);
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            payments.add(new Object[]{
                                rs.getString("receipt_number"),
                                rs.getString("loan_number"),
                                rs.getString("client_name"),
                                rs.getDouble("amount"),
                                formatDate(rs.getDate("payment_date")),
                                rs.getString("payment_mode"),
                                rs.getString("status"),
                                rs.getString("received_by"),
                                rs.getString("approved_by"),
                                rs.getString("notes")
                            });
                        }
                    }
                }
            }
            return payments;
        }, 
        payments -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (Object[] row : payments) {
                    tableModel.addRow(row);
                }
                hideLoading();
                logActivityAsync("Payment Search", 
                    "Searched: '" + searchTerm + "' | Status: " + statusFilter + 
                    " | Found: " + payments.size() + " records");
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error filtering payments: " + e.getMessage());
            });
        });
    }
    
    // ==================== NEW PAYMENT DIALOG WITH MULTIPLE LOANS SUPPORT ====================
    
    private void showNewPaymentDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                   "💳 Initialize New Payment", true);
        dialog.setSize(850, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(CARD_BG);
        
        // Title
        JLabel titleLabel = new JLabel("💳 New Payment - Select Client", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(PRIMARY_BLUE);
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Main content with search and results
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setBackground(CARD_BG);
        
        // Search panel
        JPanel searchPanel = createSearchPanel(dialog, mainContent);
        mainContent.add(searchPanel, BorderLayout.NORTH);
        
        // Results panel (will be populated)
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), 
            "Client Search Results"
        ));
        mainContent.add(resultsPanel, BorderLayout.CENTER);
        
        // Payment form panel (initially hidden)
        JPanel paymentFormPanel = new JPanel(new BorderLayout());
        paymentFormPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), 
            "Payment Details"
        ));
        paymentFormPanel.setVisible(false);
        mainContent.add(paymentFormPanel, BorderLayout.SOUTH);
        
        contentPanel.add(mainContent, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setBackground(CARD_BG);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JButton submitButton = new JButton("💳 Submit Payment");
        styleButton(submitButton, GREEN_SUCCESS);
        submitButton.setEnabled(false);
        buttonPanel.add(submitButton);
        
        JButton cancelButton = new JButton("❌ Cancel");
        styleButton(cancelButton, TEXT_GRAY);
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(contentPanel, BorderLayout.CENTER);
        
        // Store references
        final JPanel[] currentResultsPanel = {resultsPanel};
        final JPanel[] currentPaymentForm = {paymentFormPanel};
        final JButton[] currentSubmitButton = {submitButton};
        
        dialog.setVisible(true);
    }
    
    private JPanel createSearchPanel(JDialog dialog, JPanel mainContent) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.setBackground(CARD_BG);
        searchPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), 
            "Client Search"
        ));
        
        JLabel searchLabel = new JLabel("🔍 Search Client:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchLabel.setForeground(TEXT_DARK);
        searchPanel.add(searchLabel);
        
        JTextField clientSearchField = new JTextField(25);
        clientSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clientSearchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        clientSearchField.setToolTipText("Search by name, ID number, or phone");
        searchPanel.add(clientSearchField);
        
        JButton searchButton = new JButton("Search");
        styleSmallButton(searchButton);
        
        searchButton.addActionListener(e -> {
            String searchTerm = clientSearchField.getText().trim();
            if (searchTerm.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter search term", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            searchClientsAsync(searchTerm, dialog, mainContent);
        });
        
        searchPanel.add(searchButton);
        return searchPanel;
    }
    
    // FIXED: Shows all clients with correct loan count
    private void searchClientsAsync(String searchTerm, JDialog dialog, JPanel mainContent) {
        showLoadingInDialog(dialog, "Searching clients...");
        
        AsyncDatabaseService.executeAsync(() -> {
            List<Object[]> clients = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getConnection()) {
                // FIXED: Count only Approved and Due loans (Active loans in new status system)
                String sql = "SELECT c.client_id, " +
                           "CONCAT(c.title, ' ', c.first_name, ' ', c.last_name) as full_name, " +
                           "c.id_number, c.phone_number, " +
                           "COUNT(DISTINCT CASE WHEN l.status IN ('Approved', 'Due') THEN l.loan_id END) as active_loans " +
                           "FROM clients c " +
                           "LEFT JOIN loans l ON c.client_id = l.client_id " +
                           "WHERE (c.first_name ILIKE ? OR c.last_name ILIKE ? OR " +
                           "c.id_number ILIKE ? OR c.phone_number ILIKE ?) " +
                           "GROUP BY c.client_id, c.title, c.first_name, c.last_name, " +
                           "c.id_number, c.phone_number " +
                           "ORDER BY c.first_name, c.last_name";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String likeTerm = "%" + searchTerm + "%";
                    for (int i = 1; i <= 4; i++) {
                        stmt.setString(i, likeTerm);
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            clients.add(new Object[]{
                                rs.getInt("client_id"),
                                rs.getString("full_name"),
                                rs.getString("id_number"),
                                rs.getString("phone_number"),
                                rs.getInt("active_loans")
                            });
                        }
                    }
                }
            }
            return clients;
        }, 
        clients -> {
            SwingUtilities.invokeLater(() -> {
                hideLoadingInDialog(dialog);
                if (clients.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "No clients found matching: " + searchTerm, 
                        "No Results", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                displayClientResults(clients, dialog, mainContent);
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoadingInDialog(dialog);
                JOptionPane.showMessageDialog(dialog, 
                    "Error searching clients: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            });
        });
    }
    
    private void displayClientResults(List<Object[]> clients, JDialog dialog, JPanel mainContent) {
        // Remove existing results panel
        Component[] components = mainContent.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel && ((JPanel)comp).getBorder() != null &&
                ((JPanel)comp).getBorder() instanceof TitledBorder) {
                TitledBorder border = (TitledBorder) ((JPanel)comp).getBorder();
                if ("Client Search Results".equals(border.getTitle())) {
                    mainContent.remove(comp);
                    break;
                }
            }
        }
        
        // Create new results panel
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), 
            "Client Search Results (" + clients.size() + " found)"
        ));
        
        String[] columns = {"Client ID", "Name", "ID Number", "Phone", "Active Loans"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        for (Object[] client : clients) {
            model.addRow(client);
        }
        
        JTable resultsTable = new JTable(model);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setRowHeight(30);
        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        resultsTable.getTableHeader().setBackground(PRIMARY_BLUE);
        resultsTable.getTableHeader().setForeground(Color.WHITE);
        
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultsTable.getSelectedRow();
                    if (row != -1) {
                        int selectedClientId = (int) model.getValueAt(row, 0);
                        String selectedClientName = (String) model.getValueAt(row, 1);
                        loadClientLoansAsync(selectedClientId, selectedClientName, dialog, mainContent);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add results panel to main content
        mainContent.add(resultsPanel, BorderLayout.CENTER);
        dialog.revalidate();
        dialog.repaint();
    }
    
    // FIXED: Loads ALL active loans for the client WITH CORRECT CALCULATIONS
    private void loadClientLoansAsync(int selectedClientId, String selectedClientName, JDialog dialog, JPanel mainContent) {
        showLoadingInDialog(dialog, "Loading client loans...");
        
        AsyncDatabaseService.executeAsync(() -> {
            List<Object[]> loans = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getConnection()) {
                // FIXED: CORRECT CALCULATION - Get principal paid and interest paid from loan_payments
                String sql = "SELECT " +
                           "l.loan_id, l.loan_number, l.amount as principal, " +
                           "l.total_amount, l.interest_amount as total_interest_due, " +
                           "l.outstanding_balance, l.status, " +
                           "l.interest_rate, l.calculation_method, " +
                           "COALESCE(SUM(CASE WHEN lp.status = 'Paid' THEN lp.principal_amount ELSE 0 END), 0) as principal_paid, " +
                           "COALESCE(SUM(CASE WHEN lp.status = 'Paid' THEN lp.interest_amount ELSE 0 END), 0) as interest_paid " +
                           "FROM loans l " +
                           "LEFT JOIN loan_payments lp ON l.loan_id = lp.loan_id " +
                           "WHERE l.client_id = ? AND l.status IN ('Approved', 'Due') " +
                           "GROUP BY l.loan_id, l.loan_number, l.amount, l.total_amount, " +
                           "l.interest_amount, l.outstanding_balance, l.status, " +
                           "l.interest_rate, l.calculation_method " +
                           "ORDER BY l.loan_id DESC";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, selectedClientId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            double principal = rs.getDouble("principal");
                            double totalAmount = rs.getDouble("total_amount");
                            double totalInterestDue = rs.getDouble("total_interest_due");
                            double principalPaid = rs.getDouble("principal_paid");
                            double interestPaid = rs.getDouble("interest_paid");
                            
                            // CORRECT CALCULATION:
                            // 1. Remaining principal = original principal - principal paid
                            double remainingPrincipal = Math.max(0, principal - principalPaid);
                            
                            // 2. Remaining interest = total interest due - interest paid
                            double remainingInterest = Math.max(0, totalInterestDue - interestPaid);
                            
                            // 3. Correct outstanding balance = remaining principal + remaining interest
                            double correctOutstandingBalance = remainingPrincipal + remainingInterest;
                            
                            // Debug output
                            System.out.println("=== Loan Calculation Debug ===");
                            System.out.println("Loan: " + rs.getString("loan_number"));
                            System.out.println("Principal: " + principal);
                            System.out.println("Total Amount: " + totalAmount);
                            System.out.println("Total Interest Due: " + totalInterestDue);
                            System.out.println("Principal Paid: " + principalPaid);
                            System.out.println("Interest Paid: " + interestPaid);
                            System.out.println("Remaining Principal: " + remainingPrincipal);
                            System.out.println("Remaining Interest: " + remainingInterest);
                            System.out.println("Correct Outstanding: " + correctOutstandingBalance);
                            System.out.println("============================");
                            
                            loans.add(new Object[]{
                                rs.getInt("loan_id"),
                                rs.getString("loan_number"),
                                principal,
                                rs.getDouble("interest_rate"),
                                rs.getString("calculation_method"),
                                rs.getString("status"),
                                correctOutstandingBalance,  // Use CALCULATED outstanding balance
                                remainingPrincipal,
                                remainingInterest,
                                principalPaid,
                                interestPaid
                            });
                        }
                    }
                }
            }
            return new Object[]{selectedClientId, selectedClientName, loans};
        }, 
        result -> {
            SwingUtilities.invokeLater(() -> {
                hideLoadingInDialog(dialog);
                int resultClientId = (int) ((Object[])result)[0];
                String resultClientName = (String) ((Object[])result)[1];
                @SuppressWarnings("unchecked")
                List<Object[]> resultLoans = (List<Object[]>) ((Object[])result)[2];
                
                if (resultLoans.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "❌ No active loans found for this client.\n\n" +
                        "Client must have an 'Approved' or 'Due' loan to make payments.", 
                        "No Active Loans", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                showLoanSelectionDialog(resultClientId, resultClientName, resultLoans, dialog);
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoadingInDialog(dialog);
                JOptionPane.showMessageDialog(dialog, 
                    "Error loading loans: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            });
        });
    }
    
    // NEW: Dialog to select which loan to pay for (for clients with multiple loans)
    private void showLoanSelectionDialog(int clientId, String clientName, List<Object[]> loans, JDialog parentDialog) {
        JDialog loanDialog = new JDialog(parentDialog, "Select Loan for Payment", true);
        loanDialog.setSize(900, 550);
        loanDialog.setLocationRelativeTo(parentDialog);
        loanDialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(CARD_BG);
        
        // Header
        JLabel titleLabel = new JLabel("📋 Select Loan for Payment - " + clientName, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(PRIMARY_BLUE);
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Loans table
        String[] columns = {"Select", "Loan #", "Principal", "Interest Rate", "Type", "Status", 
                          "Outstanding Balance", "Remaining Principal", "Remaining Interest", "Principal Paid", "Interest Paid"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Only select column is editable
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 2 || columnIndex == 6 || columnIndex == 7 || columnIndex == 8 || columnIndex == 9 || columnIndex == 10) 
                    return Double.class;
                return String.class;
            }
        };
        
        for (Object[] loan : loans) {
            model.addRow(new Object[]{
                false, // Checkbox
                loan[1], // Loan number
                loan[2], // Principal
                String.format("%.2f%%", (Double)loan[3]),
                loan[4], // Calculation method
                loan[5], // Status
                loan[6], // Outstanding balance
                loan[7], // Remaining principal
                loan[8], // Remaining interest
                loan[9], // Principal paid
                loan[10] // Interest paid
            });
        }
        
        JTable loansTable = new JTable(model);
        loansTable.setRowHeight(30);
        loansTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        loansTable.getTableHeader().setBackground(PRIMARY_BLUE);
        loansTable.getTableHeader().setForeground(Color.WHITE);
        
        // Renderer for amount columns
        DefaultTableCellRenderer amountRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Double) {
                    setText(String.format("ZMW %,.2f", (Double) value));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        };
        
        for (int i = 2; i <= 10; i++) {
            if (i != 3 && i != 4 && i != 5) { // Skip interest rate, type, status columns
                loansTable.getColumnModel().getColumn(i).setCellRenderer(amountRenderer);
            }
        }
        
        // Checkbox column renderer
        loansTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Boolean) {
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.setSelected((Boolean) value);
                    checkBox.setHorizontalAlignment(SwingConstants.CENTER);
                    checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    return checkBox;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        
        loansTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            @Override
            public Object getCellEditorValue() {
                return ((JCheckBox) getComponent()).isSelected();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(loansTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(CARD_BG);
        
        JButton selectButton = new JButton("✅ Select Loan & Proceed");
        styleButton(selectButton, GREEN_SUCCESS);
        selectButton.addActionListener(e -> {
            int selectedRow = -1;
            for (int i = 0; i < model.getRowCount(); i++) {
                if ((Boolean) model.getValueAt(i, 0)) {
                    selectedRow = i;
                    break;
                }
            }
            
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(loanDialog, 
                    "Please select a loan to pay", 
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int selectedLoanId = (int) loans.get(selectedRow)[0];
            String selectedLoanNumber = (String) loans.get(selectedRow)[1];
            double selectedOutstandingBalance = (double) loans.get(selectedRow)[6];
            
            loanDialog.dispose();
            parentDialog.dispose();
            showPaymentEntryDialog(clientId, clientName, selectedLoanId, selectedLoanNumber, selectedOutstandingBalance);
        });
        
        JButton cancelButton = new JButton("❌ Cancel");
        styleButton(cancelButton, TEXT_GRAY);
        cancelButton.addActionListener(e -> loanDialog.dispose());
        
        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        loanDialog.add(contentPanel, BorderLayout.CENTER);
        loanDialog.setVisible(true);
    }
    
    private void showPaymentEntryDialog(int clientId, String clientName, int loanId, 
                                      String loanNumber, double outstandingBalance) {
        JDialog paymentDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                          "💳 Enter Payment Details", true);
        paymentDialog.setSize(500, 600);
        paymentDialog.setLocationRelativeTo(this);
        paymentDialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(CARD_BG);
        
        // Title
        JLabel titleLabel = new JLabel("💳 New Payment Entry", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(PRIMARY_BLUE);
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        int row = 0;
        
        // Client info (readonly)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel clientLabel = new JLabel("Client:");
        clientLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(clientLabel, gbc);
        
        gbc.gridx = 1;
        JTextField clientField = new JTextField(clientName);
        clientField.setEditable(false);
        clientField.setBackground(new Color(240, 240, 240));
        formPanel.add(clientField, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel loanLabel = new JLabel("Loan #:");
        loanLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(loanLabel, gbc);
        
        gbc.gridx = 1;
        JTextField loanField = new JTextField(loanNumber);
        loanField.setEditable(false);
        loanField.setBackground(new Color(240, 240, 240));
        formPanel.add(loanField, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel balanceLabel = new JLabel("Outstanding:");
        balanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(balanceLabel, gbc);
        
        gbc.gridx = 1;
        JTextField balanceField = new JTextField(String.format("ZMW %,.2f", outstandingBalance));
        balanceField.setEditable(false);
        balanceField.setBackground(new Color(240, 240, 240));
        formPanel.add(balanceField, gbc);
        
        // Separator
        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
        JSeparator separator = new JSeparator();
        formPanel.add(separator, gbc);
        gbc.gridwidth = 1;
        
        // Payment amount
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel amountLabel = new JLabel("Amount *:");
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        amountLabel.setForeground(PRIMARY_BLUE);
        formPanel.add(amountLabel, gbc);
        
        gbc.gridx = 1;
        JTextField amountField = new JTextField();
        amountField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(amountField, gbc);
        
        // Payment date
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel dateLabel = new JLabel("Date *:");
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(dateLabel, gbc);
        
        gbc.gridx = 1;
        JTextField dateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(dateField, gbc);
        
        // Payment method
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel methodLabel = new JLabel("Method *:");
        methodLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(methodLabel, gbc);
        
        gbc.gridx = 1;
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Cash", "Mobile", "Bank", "Other"});
        methodCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(methodCombo, gbc);
        
        // Voucher number
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel voucherLabel = new JLabel("Voucher # *:");
        voucherLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(voucherLabel, gbc);
        
        gbc.gridx = 1;
        JTextField voucherField = new JTextField("VOU" + (System.currentTimeMillis() % 100000));
        voucherField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(voucherField, gbc);
        
        // Notes
        gbc.gridx = 0; gbc.gridy = ++row;
        JLabel notesLabel = new JLabel("Notes:");
        notesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(notesLabel, gbc);
        
        gbc.gridx = 1;
        JTextArea notesArea = new JTextArea(3, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        formPanel.add(notesScroll, gbc);
        
        contentPanel.add(formPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(CARD_BG);
        
        JButton submitButton = new JButton("💳 Submit Payment");
        styleButton(submitButton, GREEN_SUCCESS);
        submitButton.addActionListener(e -> {
            String amount = amountField.getText().trim();
            String voucher = voucherField.getText().trim();
            String notes = notesArea.getText().trim();
            
            if (validatePayment(amount, voucher)) {
                submitPaymentAsync(loanId, loanNumber, amount, 
                                 (String) methodCombo.getSelectedItem(),
                                 dateField.getText(), voucher, notes,
                                 paymentDialog);
            }
        });
        
        JButton cancelButton = new JButton("❌ Cancel");
        styleButton(cancelButton, TEXT_GRAY);
        cancelButton.addActionListener(e -> paymentDialog.dispose());
        
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        paymentDialog.add(contentPanel, BorderLayout.CENTER);
        paymentDialog.setVisible(true);
    }
    
    private void submitPaymentAsync(int loanId, String loanNumber, String amount, String mode, 
                                  String date, String voucher, String notes, JDialog dialog) {
        showLoading("Submitting payment...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                double paymentAmount = Double.parseDouble(amount);
                
                // Use sequence for receipt number
                String receiptSql = "SELECT 'RCPT' || LPAD(nextval('receipt_number_seq')::text, 6, '0') as receipt_num";
                String receiptNumber;
                try (PreparedStatement stmt = conn.prepareStatement(receiptSql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        receiptNumber = rs.next() ? rs.getString("receipt_num") : 
                            "RCPT" + System.currentTimeMillis();
                    }
                }
                
                // Insert payment receipt
                String sql = "INSERT INTO payment_receipts (loan_id, receipt_number, amount, " +
                           "payment_date, payment_mode, voucher_number, status, " +
                           "received_by, notes) VALUES (?, ?, ?, ?::date, ?, ?, 'Pending', ?, ?)";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, loanId);
                    stmt.setString(2, receiptNumber);
                    stmt.setDouble(3, paymentAmount);
                    stmt.setString(4, date);
                    stmt.setString(5, mode);
                    stmt.setString(6, voucher);
                    stmt.setInt(7, userId);
                    stmt.setString(8, notes);
                    return stmt.executeUpdate();
                }
            }
        }, 
        result -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, 
                    "✅ Payment submitted successfully!\n\n" +
                    "Receipt Number: " + "RCPT" + (System.currentTimeMillis() % 100000) + "\n" +
                    "Amount: ZMW " + String.format("%,.2f", Double.parseDouble(amount)) + "\n" +
                    "Status: Pending Approval\n\n" +
                    "Waiting for admin approval.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                logActivityAsync("Payment Initiated", 
                    "Initiated payment of ZMW " + amount + " for loan " + loanNumber);
                
                loadPaymentsDataAsync();
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error submitting payment: " + e.getMessage());
            });
        });
    }
    
    // ==================== PAYMENT APPROVAL/REJECTION ====================
    
    private void updateButtonStates() {
        int selectedRow = paymentsTable.getSelectedRow();
        boolean hasSelection = selectedRow != -1;
        
        if ("admin".equals(userRole) && hasSelection) {
            String status = tableModel.getValueAt(selectedRow, 6).toString();
            boolean isPending = "Pending".equals(status);
            approveButton.setEnabled(isPending);
            rejectButton.setEnabled(isPending);
        }
    }
    
    private void approvePaymentAsync() {
        int selectedRow = paymentsTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a payment to approve");
            return;
        }
        
        int modelRow = paymentsTable.convertRowIndexToModel(selectedRow);
        String receiptNumber = (String) tableModel.getValueAt(modelRow, 0);
        String loanNumber = (String) tableModel.getValueAt(modelRow, 1);
        double amount = (Double) tableModel.getValueAt(modelRow, 3);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "💰 APPROVE PAYMENT\n\n" +
            "Receipt: " + receiptNumber + "\n" +
            "Loan: " + loanNumber + "\n" +
            "Amount: ZMW " + String.format("%,.2f", amount) + "\n\n" +
            "Are you sure you want to approve this payment?",
            "Confirm Approval", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            showLoading("Approving payment...");
            
            AsyncDatabaseService.executeAsync(() -> {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    
                    try {
                        // Update payment receipt status
                        String updateReceipt = "UPDATE payment_receipts SET status = 'Approved', " +
                                              "approved_by = ?, approved_date = CURRENT_TIMESTAMP " +
                                              "WHERE receipt_number = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(updateReceipt)) {
                            stmt.setInt(1, userId);
                            stmt.setString(2, receiptNumber);
                            stmt.executeUpdate();
                        }
                        
                        // Get receipt details
                        String getReceipt = "SELECT pr.loan_id, pr.amount, pr.voucher_number, " +
                                          "pr.payment_mode FROM payment_receipts pr " +
                                          "WHERE pr.receipt_number = ?";
                        int selectedLoanId = 0;
                        double paymentAmount = 0;
                        String voucherNumber = "";
                        String paymentMode = "";
                        
                        try (PreparedStatement stmt = conn.prepareStatement(getReceipt)) {
                            stmt.setString(1, receiptNumber);
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    selectedLoanId = rs.getInt("loan_id");
                                    paymentAmount = rs.getDouble("amount");
                                    voucherNumber = rs.getString("voucher_number");
                                    paymentMode = rs.getString("payment_mode");
                                }
                            }
                        }
                        
                        if (selectedLoanId == 0) {
                            throw new SQLException("Loan not found for receipt");
                        }
                        
                        // FIXED: CORRECT CALCULATION - Get current loan status to calculate split
                        String getLoanDetails = "SELECT " +
                                              "l.amount as principal, l.total_amount, l.interest_amount as total_interest_due, " +
                                              "COALESCE(SUM(CASE WHEN lp.status = 'Paid' THEN lp.principal_amount ELSE 0 END), 0) as principal_paid, " +
                                              "COALESCE(SUM(CASE WHEN lp.status = 'Paid' THEN lp.interest_amount ELSE 0 END), 0) as interest_paid " +
                                              "FROM loans l " +
                                              "LEFT JOIN loan_payments lp ON l.loan_id = lp.loan_id " +
                                              "WHERE l.loan_id = ? " +
                                              "GROUP BY l.loan_id, l.amount, l.total_amount, l.interest_amount";
                        
                        double remainingPrincipal = 0;
                        double remainingInterest = 0;
                        
                        try (PreparedStatement stmt = conn.prepareStatement(getLoanDetails)) {
                            stmt.setInt(1, selectedLoanId);
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    double principal = rs.getDouble("principal");
                                    double totalInterestDue = rs.getDouble("total_interest_due");
                                    double principalPaid = rs.getDouble("principal_paid");
                                    double interestPaid = rs.getDouble("interest_paid");
                                    
                                    // Calculate remaining amounts
                                    remainingPrincipal = Math.max(0, principal - principalPaid);
                                    remainingInterest = Math.max(0, totalInterestDue - interestPaid);
                                }
                            }
                        }
                        
                        // FIXED: CORRECT PAYMENT SPLIT - Pay interest first, then principal
                        double interestAmount = 0.0;
                        double principalAmount = 0.0;
                        
                        if (remainingInterest > 0) {
                            // Pay interest first
                            interestAmount = Math.min(paymentAmount, remainingInterest);
                            principalAmount = Math.min(paymentAmount - interestAmount, remainingPrincipal);
                        } else {
                            // All interest paid, apply to principal
                            principalAmount = Math.min(paymentAmount, remainingPrincipal);
                        }
                        
                        // Get next payment number
                        int nextPaymentNumber = 1;
                        String getPaymentNum = "SELECT COALESCE(MAX(payment_number), 0) + 1 as next_payment " +
                                             "FROM loan_payments WHERE loan_id = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(getPaymentNum)) {
                            stmt.setInt(1, selectedLoanId);
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    nextPaymentNumber = rs.getInt("next_payment");
                                }
                            }
                        }
                        
                        // Create loan_payment record
                        String insertPayment = "INSERT INTO loan_payments (loan_id, payment_number, " +
                                             "scheduled_payment_date, payment_amount, principal_amount, " +
                                             "interest_amount, paid_amount, paid_date, status, payment_mode, " +
                                             "voucher_number, received_by, approved_by, approved_date) " +
                                             "VALUES (?, ?, CURRENT_DATE, ?, ?, ?, ?, CURRENT_TIMESTAMP, " +
                                             "'Paid', ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                        try (PreparedStatement stmt = conn.prepareStatement(insertPayment)) {
                            stmt.setInt(1, selectedLoanId);
                            stmt.setInt(2, nextPaymentNumber);
                            stmt.setDouble(3, paymentAmount);
                            stmt.setDouble(4, principalAmount);
                            stmt.setDouble(5, interestAmount);
                            stmt.setDouble(6, paymentAmount);
                            stmt.setString(7, paymentMode);
                            stmt.setString(8, voucherNumber);
                            stmt.setInt(9, userId);
                            stmt.setInt(10, userId);
                            stmt.executeUpdate();
                        }
                        
                        // FIXED: Update loan outstanding balance CORRECTLY
                        double newRemainingPrincipal = Math.max(0, remainingPrincipal - principalAmount);
                        double newRemainingInterest = Math.max(0, remainingInterest - interestAmount);
                        double newTotalOutstanding = newRemainingPrincipal + newRemainingInterest;
                        
                        String updateLoanBalance = "UPDATE loans SET outstanding_balance = ? WHERE loan_id = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(updateLoanBalance)) {
                            stmt.setDouble(1, newTotalOutstanding);
                            stmt.setInt(2, selectedLoanId);
                            stmt.executeUpdate();
                        }
                        
                        // Check if loan is fully paid
                        if (newTotalOutstanding <= 0.01) { // Small tolerance for floating point
                            String closeLoan = "UPDATE loans SET status = 'Closed', closed_date = CURRENT_TIMESTAMP " +
                                             "WHERE loan_id = ?";
                            try (PreparedStatement stmt = conn.prepareStatement(closeLoan)) {
                                stmt.setInt(1, selectedLoanId);
                                stmt.executeUpdate();
                            }
                            
                            System.out.println("✅ Loan " + loanNumber + " fully paid and closed.");
                        }
                        
                        conn.commit();
                        return true;
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }
            }, 
            result -> {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    JOptionPane.showMessageDialog(this, 
                        "✅ Payment approved successfully!\n\n" +
                        "• Payment recorded in loan payments\n" +
                        "• Loan balance updated\n" +
                        "• Receipt status changed to Approved", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                    logActivityAsync("Payment Approved", 
                        "Approved payment " + receiptNumber + " for loan " + loanNumber);
                    
                    loadPaymentsDataAsync();
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    showError("Error approving payment: " + e.getMessage());
                });
            });
        }
    }
    
    private void rejectPaymentDialog() {
        int selectedRow = paymentsTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a payment to reject");
            return;
        }
        
        int modelRow = paymentsTable.convertRowIndexToModel(selectedRow);
        String receiptNumber = (String) tableModel.getValueAt(modelRow, 0);
        String loanNumber = (String) tableModel.getValueAt(modelRow, 1);
        double amount = (Double) tableModel.getValueAt(modelRow, 3);
        
        JTextArea reasonArea = new JTextArea(4, 30);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        Object[] message = {
            "❌ REJECT PAYMENT\n\n" +
            "Receipt: " + receiptNumber + "\n" +
            "Loan: " + loanNumber + "\n" +
            "Amount: ZMW " + String.format("%,.2f", amount) + "\n\n" +
            "Please provide reason for rejection:",
            new JScrollPane(reasonArea)
        };
        
        int option = JOptionPane.showConfirmDialog(this, message, 
            "Reject Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            String rejectionReason = reasonArea.getText().trim();
            if (rejectionReason.isEmpty()) {
                showError("Please provide a rejection reason");
                return;
            }
            
            rejectPaymentAsync(receiptNumber, loanNumber, amount, rejectionReason);
        }
    }
    
    private void rejectPaymentAsync(String receiptNumber, String loanNumber, 
                                  double amount, String rejectionReason) {
        showLoading("Rejecting payment...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE payment_receipts SET status = 'Rejected', " +
                            "approved_by = ?, approved_date = CURRENT_TIMESTAMP, " +
                            "rejection_reason = ? " +
                            "WHERE receipt_number = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    stmt.setString(2, rejectionReason);
                    stmt.setString(3, receiptNumber);
                    return stmt.executeUpdate();
                }
            }
        }, 
        result -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                JOptionPane.showMessageDialog(this, 
                    "❌ Payment rejected successfully!\n\n" +
                    "Reason recorded for employee reference.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                logActivityAsync("Payment Rejected", 
                    "Rejected payment " + receiptNumber + " for loan " + loanNumber + 
                    ". Reason: " + rejectionReason);
                
                loadPaymentsDataAsync();
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                hideLoading();
                showError("Error rejecting payment: " + e.getMessage());
            });
        });
    }
    
    // ==================== HELPER METHODS ====================
    
    private void styleSmallButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(PRIMARY_BLUE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    private boolean validatePayment(String amount, String voucher) {
        try {
            double paymentAmount = Double.parseDouble(amount);
            if (paymentAmount <= 0) {
                showError("Payment amount must be greater than zero");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid payment amount");
            return false;
        }
        if (voucher.isEmpty()) {
            showError("Please enter a voucher number");
            return false;
        }
        return true;
    }
    
    private String formatDate(java.sql.Date date) {
        if (date == null) return "N/A";
        return displayDateFormat.format(date);
    }
    
    private void logActivityAsync(String action, String details) {
        AsyncDatabaseService.logAsync(userId, action, details);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showLoading(String message) {
        // You could implement a loading overlay here
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