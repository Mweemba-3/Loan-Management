import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class LoansScreen extends JPanel {
    private int employeeId;
    private String userRole;
    private JTable loansTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> filterComboBox;
    private JTextField searchField;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    // UI Components
    private JButton applyLoanBtn, viewBtn, refreshBtn, createProductBtn;
    private JButton approveBtn, rejectBtn, deleteBtn, backBtn;
    private JPanel loadingPanel;
    private JLabel loadingLabel;
    private Timer loadingTimer;
    private int loadingDots = 0;
    
    // Color scheme
    private final Color BG_COLOR = new Color(245, 247, 250);
    private final Color CARD_COLOR = Color.WHITE;
    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color SUCCESS_COLOR = new Color(39, 174, 96);
    private final Color WARNING_COLOR = new Color(241, 196, 15);
    private final Color DANGER_COLOR = new Color(231, 76, 60);
    private final Color INFO_COLOR = new Color(52, 152, 219);
    private final Color DARK_TEXT = new Color(52, 73, 94);
    private final Color LIGHT_TEXT = new Color(127, 140, 141);
    
    public LoansScreen(int employeeId, String userRole) {
        this.employeeId = employeeId;
        this.userRole = userRole;
        initUI();
        loadLoansDataAsync("All Loans");
        AsyncDatabaseService.logAsync(employeeId, "Loans Access", "Accessed loans management");
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Search and Filter Panel
        JPanel searchFilterPanel = createSearchFilterPanel();
        add(searchFilterPanel, BorderLayout.NORTH);
        
        // Table Panel with loading overlay
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);
        
        // Buttons Panel
        JPanel buttonsPanel = createButtonsPanel();
        add(buttonsPanel, BorderLayout.SOUTH);
        
        // Initialize button states
        updateButtonStates();
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("LOANS MANAGEMENT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(DARK_TEXT);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(BG_COLOR);
        
        JButton emiCalculatorBtn = new JButton("🧮 EMI Calculator");
        styleButton(emiCalculatorBtn, INFO_COLOR, true);
        emiCalculatorBtn.addActionListener(e -> showEMICalculator());
        
        rightPanel.add(emiCalculatorBtn);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createSearchFilterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(BG_COLOR);
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchLabel.setForeground(DARK_TEXT);
        
        searchField = new JTextField(25);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Client name, loan number, phone...");
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchLoansAsync();
                }
            }
        });
        
        JButton searchBtn = new JButton("Search");
        styleButton(searchBtn, PRIMARY_COLOR, true);
        searchBtn.addActionListener(e -> searchLoansAsync());
        
        backBtn = new JButton("Back to Dashboard");
        styleButton(backBtn, LIGHT_TEXT, false);
        backBtn.addActionListener(e -> navigateToDashboard());
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(Box.createHorizontalStrut(20));
        searchPanel.add(backBtn);
        
        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(BG_COLOR);
        
        JLabel filterLabel = new JLabel("Filter by:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        filterLabel.setForeground(DARK_TEXT);
        
        // FIXED: Updated filter options to match database statuses
        String[] filters = {"All Loans", "Pending", "Approved", "Due", "Closed", "Rejected"};
        filterComboBox = new JComboBox<>(filters);
        styleComboBox(filterComboBox);
        filterComboBox.addActionListener(e -> {
            if (!((String)filterComboBox.getSelectedItem()).equals("All Loans")) {
                filterLoansAsync();
            }
        });
        
        filterPanel.add(filterLabel);
        filterPanel.add(filterComboBox);
        
        panel.add(searchPanel);
        panel.add(filterPanel);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        tablePanel.setBackground(BG_COLOR);
        
        // Create table
        String[] columns = {"Loan ID", "Loan Number", "Client Name", "Amount", "Status", "Application Date", "Due Date", "Issued By"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Integer.class;
                if (columnIndex == 3) return Double.class;
                return String.class;
            }
        };
        
        loansTable = new JTable(tableModel);
        styleLoansTable(loansTable);
        
        JScrollPane scrollPane = new JScrollPane(loansTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        // Loading overlay
        loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setBackground(new Color(255, 255, 255, 200));
        loadingPanel.setVisible(false);
        
        loadingLabel = new JLabel("Loading loans data", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loadingLabel.setForeground(DARK_TEXT);
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
        
        tablePanel.setLayout(new OverlayLayout(tablePanel));
        tablePanel.add(loadingPanel);
        tablePanel.add(scrollPane);
        
        return tablePanel;
    }
    
    private void styleLoansTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(40);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(new Color(240, 240, 240));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        
        // Custom header
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(52, 73, 94));
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        
        // Set column widths and renderers
        TableColumnModel columnModel = table.getColumnModel();
        
        columnModel.getColumn(0).setPreferredWidth(60);
        columnModel.getColumn(1).setPreferredWidth(120);
        columnModel.getColumn(2).setPreferredWidth(180);
        columnModel.getColumn(3).setPreferredWidth(120);
        columnModel.getColumn(4).setPreferredWidth(100);
        columnModel.getColumn(5).setPreferredWidth(120);
        columnModel.getColumn(6).setPreferredWidth(120);
        columnModel.getColumn(7).setPreferredWidth(150);
        
        // Center align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // FIXED: Custom renderer for amount column - CENTERED with K symbol
        columnModel.getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Number) {
                    // FIXED: Changed from ZMW to K and made it CENTERED
                    setText(String.format("K %,.2f", ((Number) value).doubleValue()));
                }
                setHorizontalAlignment(SwingConstants.CENTER); // FIXED: Changed from RIGHT to CENTER
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                return this;
            }
        });
        
        // FIXED: Custom renderer for status column - updated statuses to match database
        columnModel.getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel();
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                
                if (value != null) {
                    String status = value.toString();
                    label.setText(status);
                    
                    // FIXED: Updated status colors to match database schema
                    switch (status) {
                        case "Pending":
                            label.setBackground(new Color(254, 249, 231));
                            label.setForeground(new Color(148, 108, 0));
                            break;
                        case "Approved":
                            label.setBackground(new Color(232, 245, 233));
                            label.setForeground(new Color(19, 115, 51));
                            break;
                        case "Due":
                            label.setBackground(new Color(254, 226, 226));
                            label.setForeground(new Color(220, 38, 38));
                            break;
                        case "Rejected":
                            label.setBackground(new Color(254, 235, 235));
                            label.setForeground(new Color(185, 28, 28));
                            break;
                        case "Closed":
                            label.setBackground(new Color(243, 244, 246));
                            label.setForeground(new Color(75, 85, 99));
                            break;
                        default:
                            label.setBackground(Color.WHITE);
                            label.setForeground(Color.BLACK);
                    }
                    
                    label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(label.getForeground(), 1),
                        BorderFactory.createEmptyBorder(3, 10, 3, 10)
                    ));
                }
                
                if (isSelected) {
                    label.setBackground(table.getSelectionBackground());
                    label.setForeground(table.getSelectionForeground());
                }
                
                return label;
            }
        });
        
        // Add selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
    }
    
    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonsPanel.setBackground(BG_COLOR);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        applyLoanBtn = new JButton("📝 Apply Loan");
        styleButton(applyLoanBtn, SUCCESS_COLOR, true);
        applyLoanBtn.addActionListener(e -> applyLoan());
        
        viewBtn = new JButton("👁️ View Details");
        styleButton(viewBtn, PRIMARY_COLOR, true);
        viewBtn.addActionListener(e -> viewLoan());
        
        refreshBtn = new JButton("🔄 Refresh");
        styleButton(refreshBtn, WARNING_COLOR, true);
        refreshBtn.addActionListener(e -> refreshAllLoans());
        
        buttonsPanel.add(applyLoanBtn);
        buttonsPanel.add(viewBtn);
        buttonsPanel.add(refreshBtn);
        
        if ("admin".equals(userRole)) {
            createProductBtn = new JButton("📋 Create Product");
            styleButton(createProductBtn, INFO_COLOR, true);
            createProductBtn.addActionListener(e -> createLoanProduct());
            
            approveBtn = new JButton("✅ Approve");
            styleButton(approveBtn, SUCCESS_COLOR, true);
            approveBtn.addActionListener(e -> approveLoanAsync());
            
            rejectBtn = new JButton("❌ Reject");
            styleButton(rejectBtn, DANGER_COLOR, true);
            rejectBtn.addActionListener(e -> rejectLoan());
            
            deleteBtn = new JButton("🗑️ Delete");
            styleButton(deleteBtn, DANGER_COLOR, false);
            deleteBtn.addActionListener(e -> deleteLoanAsync());
            
            buttonsPanel.add(createProductBtn);
            buttonsPanel.add(approveBtn);
            buttonsPanel.add(rejectBtn);
            buttonsPanel.add(deleteBtn);
        } else {
            deleteBtn = new JButton("🗑️ Delete");
            styleButton(deleteBtn, DANGER_COLOR, false);
            deleteBtn.addActionListener(e -> deleteLoanAsync());
            buttonsPanel.add(deleteBtn);
        }
        
        return buttonsPanel;
    }
    
    private void updateButtonStates() {
        int selectedRow = loansTable.getSelectedRow();
        boolean rowSelected = selectedRow != -1;
        
        viewBtn.setEnabled(rowSelected);
        deleteBtn.setEnabled(rowSelected);
        
        if (rowSelected && "admin".equals(userRole)) {
            int modelRow = loansTable.convertRowIndexToModel(selectedRow);
            String status = (String) tableModel.getValueAt(modelRow, 4);
            approveBtn.setEnabled("Pending".equals(status));
            rejectBtn.setEnabled("Pending".equals(status));
        } else if (rowSelected && !"admin".equals(userRole)) {
            int modelRow = loansTable.convertRowIndexToModel(selectedRow);
            String status = (String) tableModel.getValueAt(modelRow, 4);
            deleteBtn.setEnabled("Pending".equals(status));
        }
    }
    
    private void styleButton(JButton button, Color bgColor, boolean isPrimary) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        if (isPrimary) {
            button.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(bgColor.brighter());
                }
                public void mouseExited(MouseEvent e) {
                    button.setBackground(bgColor);
                }
            });
        }
    }
    
    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    ((JLabel) c).setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }
                return c;
            }
        });
    }
    
    private void showLoading(boolean show) {
        SwingUtilities.invokeLater(() -> {
            loadingPanel.setVisible(show);
            setComponentsEnabled(!show);
            
            if (show) {
                loadingTimer = new Timer(500, e -> {
                    loadingDots = (loadingDots + 1) % 4;
                    loadingLabel.setText("Loading loans data" + ".".repeat(loadingDots));
                });
                loadingTimer.start();
            } else if (loadingTimer != null) {
                loadingTimer.stop();
                loadingDots = 0;
                loadingLabel.setText("Loading loans data");
            }
        });
    }
    
    private void setComponentsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            Component[] components = getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    setPanelComponentsEnabled((JPanel) comp, enabled);
                }
            }
        });
    }
    
    private void setPanelComponentsEnabled(JPanel panel, boolean enabled) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JButton || comp instanceof JComboBox || 
                comp instanceof JTextField) {
                comp.setEnabled(enabled);
            } else if (comp instanceof JPanel) {
                setPanelComponentsEnabled((JPanel) comp, enabled);
            }
        }
    }
    
    // FIXED: Updated query to match database schema
    private void loadLoansDataAsync(String filter) {
        showLoading(true);
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return new Object[0][0];
                }
                
                String sql = buildLoanQuery(filter);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        java.util.List<Object[]> rows = new java.util.ArrayList<>();
                        
                        while (rs.next()) {
                            Object[] row = new Object[8];
                            row[0] = rs.getInt("loan_id");
                            row[1] = rs.getString("loan_number");
                            row[2] = rs.getString("client_name");
                            row[3] = rs.getDouble("amount");
                            row[4] = rs.getString("status");
                            row[5] = formatDate(rs.getDate("application_date")); // FIXED: Removed TO_CHAR
                            row[6] = formatDate(rs.getDate("due_date")); // FIXED: Removed TO_CHAR
                            row[7] = rs.getString("issued_by");
                            rows.add(row);
                        }
                        return rows.toArray(new Object[0][0]);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error loading loans: " + ex.getMessage());
                return new Object[0][0];
            }
        }, 
        result -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (Object[] row : (Object[][]) result) {
                    tableModel.addRow(row);
                }
                showLoading(false);
                updateButtonStates();
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false);
                showError("Failed to load loans: " + e.getMessage());
            });
        });
    }
    
    // FIXED: Updated query builder to match database schema
    private String buildLoanQuery(String filter) {
        String baseQuery = "SELECT l.loan_id, l.loan_number, " +
                         "CONCAT(c.first_name, ' ', c.last_name) as client_name, " +
                         "l.amount, l.status, " +
                         "l.application_date, " + // FIXED: Removed TO_CHAR
                         "l.due_date, " + // FIXED: Removed TO_CHAR
                         "e.name as issued_by " +
                         "FROM loans l " +
                         "JOIN clients c ON l.client_id = c.client_id " +
                         "JOIN employees e ON l.created_by = e.employee_id " +
                         "WHERE 1=1 ";
        
        // FIXED: Updated status filters to match database
        switch (filter) {
            case "Pending":
                baseQuery += "AND l.status = 'Pending' ";
                break;
            case "Approved":
                baseQuery += "AND l.status = 'Approved' ";
                break;
            case "Due":
                baseQuery += "AND l.status = 'Due' ";
                break;
            case "Closed":
                baseQuery += "AND l.status = 'Closed' ";
                break;
            case "Rejected":
                baseQuery += "AND l.status = 'Rejected' ";
                break;
        }
        
        baseQuery += "ORDER BY l.application_date DESC";
        return baseQuery;
    }
    
    // FIXED: Updated search query to match database schema
    private void searchLoansAsync() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            refreshAllLoans();
            return;
        }
        
        showLoading(true);
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return new Object[0][0];
                }
                
                String sql = "SELECT l.loan_id, l.loan_number, " +
                            "CONCAT(c.first_name, ' ', c.last_name) as client_name, " +
                            "l.amount, l.status, " +
                            "l.application_date, " + // FIXED: Removed TO_CHAR
                            "l.due_date, " + // FIXED: Removed TO_CHAR
                            "e.name as issued_by " +
                            "FROM loans l " +
                            "JOIN clients c ON l.client_id = c.client_id " +
                            "JOIN employees e ON l.created_by = e.employee_id " +
                            "WHERE (c.first_name ILIKE ? OR c.last_name ILIKE ? OR " +
                            "l.loan_number ILIKE ? OR c.phone_number ILIKE ?) " +
                            "ORDER BY l.application_date DESC";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setQueryTimeout(5);
                    String likeTerm = "%" + searchTerm + "%";
                    stmt.setString(1, likeTerm);
                    stmt.setString(2, likeTerm);
                    stmt.setString(3, likeTerm);
                    stmt.setString(4, likeTerm);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        java.util.List<Object[]> rows = new java.util.ArrayList<>();
                        while (rs.next()) {
                            rows.add(new Object[]{
                                rs.getInt("loan_id"),
                                rs.getString("loan_number"),
                                rs.getString("client_name"),
                                rs.getDouble("amount"),
                                rs.getString("status"),
                                formatDate(rs.getDate("application_date")), // FIXED: Removed TO_CHAR
                                formatDate(rs.getDate("due_date")), // FIXED: Removed TO_CHAR
                                rs.getString("issued_by")
                            });
                        }
                        return rows.toArray(new Object[0][0]);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error searching loans: " + ex.getMessage());
                return new Object[0][0];
            }
        },
        result -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (Object[] row : (Object[][]) result) {
                    tableModel.addRow(row);
                }
                showLoading(false);
                updateButtonStates();
                
                if (((Object[][]) result).length == 0) {
                    JOptionPane.showMessageDialog(this, 
                        "No loans found for: " + searchTerm, 
                        "No Results", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            });
            
            AsyncDatabaseService.logAsync(employeeId, "Loan Search", "Searched for: " + searchTerm);
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false);
                showError("Search failed: " + e.getMessage());
            });
        });
    }
    
    private void filterLoansAsync() {
        String filter = (String) filterComboBox.getSelectedItem();
        if (filter != null) {
            loadLoansDataAsync(filter);
        }
    }
    
    private void refreshAllLoans() {
        filterComboBox.setSelectedItem("All Loans");
        loadLoansDataAsync("All Loans");
    }
    
    private void applyLoan() {
        SwingUtilities.invokeLater(() -> {
            ScreenManager.getInstance().showScreen(new ApplyLoanScreen(employeeId, userRole));
        });
    }
    
    private void createLoanProduct() {
        SwingUtilities.invokeLater(() -> {
            ScreenManager.getInstance().showScreen(new CreateLoanProductScreen(employeeId, userRole));
        });
    }
    
    private void showEMICalculator() {
        SwingUtilities.invokeLater(() -> {
            Window parent = SwingUtilities.getWindowAncestor(this);
            Frame frame = null;
            if (parent instanceof Frame) {
                frame = (Frame) parent;
            } else if (parent != null) {
                frame = new Frame();
            }
            new EMICalculatorDialog(frame).setVisible(true);
        });
    }
    
    private void viewLoan() {
        int selectedRow = loansTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a loan to view");
            return;
        }
        
        int modelRow = loansTable.convertRowIndexToModel(selectedRow);
        int loanId = (Integer) tableModel.getValueAt(modelRow, 0);
        showLoanDetailsAsync(loanId);
    }
    
    private void showLoanDetailsAsync(int loanId) {
        showLoading(true);
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return null;
                }
                
                String sql = "SELECT l.*, CONCAT(c.first_name, ' ', c.last_name) as client_name, " +
                            "c.phone_number, c.id_number, e.name as employee_name, " +
                            "e2.name as approved_by_name, lp.product_name " +
                            "FROM loans l " +
                            "JOIN clients c ON l.client_id = c.client_id " +
                            "JOIN employees e ON l.created_by = e.employee_id " +
                            "LEFT JOIN employees e2 ON l.approved_by = e2.employee_id " +
                            "LEFT JOIN loan_products lp ON l.product_id = lp.product_id " +
                            "WHERE l.loan_id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, loanId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Map<String, Object> loanDetails = new HashMap<>();
                            loanDetails.put("loan_number", rs.getString("loan_number"));
                            loanDetails.put("client_name", rs.getString("client_name"));
                            loanDetails.put("id_number", rs.getString("id_number"));
                            loanDetails.put("phone_number", rs.getString("phone_number"));
                            loanDetails.put("amount", rs.getDouble("amount"));
                            loanDetails.put("interest_rate", rs.getDouble("interest_rate"));
                            loanDetails.put("loan_term", rs.getInt("loan_term"));
                            loanDetails.put("installment_type", rs.getString("installment_type"));
                            loanDetails.put("status", rs.getString("status"));
                            loanDetails.put("employee_name", rs.getString("employee_name"));
                            loanDetails.put("application_date", rs.getDate("application_date"));
                            loanDetails.put("approved_by_name", rs.getString("approved_by_name"));
                            loanDetails.put("approved_date", rs.getDate("approved_date"));
                            loanDetails.put("due_date", rs.getDate("due_date"));
                            loanDetails.put("rejection_reason", rs.getString("rejection_reason"));
                            loanDetails.put("collateral_details", rs.getString("collateral_details"));
                            loanDetails.put("guarantors_details", rs.getString("guarantors_details"));
                            loanDetails.put("total_amount", rs.getDouble("total_amount"));
                            loanDetails.put("installment_amount", rs.getDouble("installment_amount"));
                            //loanDetails.put("outstanding_balance", rs.getDouble("outstanding_balance"));
                            loanDetails.put("product_name", rs.getString("product_name"));
                            return loanDetails;
                        }
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error loading loan details: " + ex.getMessage());
            }
            return null;
        },
        loanDetails -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false);
                if (loanDetails != null) {
                    showLoanDetailsDialog((Map<String, Object>) loanDetails);
                } else {
                    showError("Could not load loan details");
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false);
                showError("Error loading loan details: " + e.getMessage());
            });
        });
    }
    
    // FIXED: Updated currency display from ZMW to K
    private void showLoanDetailsDialog(Map<String, Object> loan) {
        StringBuilder details = new StringBuilder();
        details.append("📋 LOAN DETAILS\n");
        details.append("═══════════════════════════════════════════════════════\n\n");
        
        details.append(String.format("🔢 Loan Number:     %s%n", loan.get("loan_number")));
        details.append(String.format("👤 Client:          %s%n", loan.get("client_name")));
        details.append(String.format("📞 Phone:           %s%n", loan.get("phone_number")));
        details.append(String.format("🆔 ID Number:       %s%n", loan.get("id_number")));
        details.append(String.format("💰 Amount:          K %,.2f%n", loan.get("amount"))); // FIXED: ZMW to K
        details.append(String.format("📈 Interest Rate:   %.2f%%%n", loan.get("interest_rate")));
        details.append(String.format("⏱️ Term:            %d %s%n", loan.get("loan_term"), loan.get("installment_type")));
        
        if (loan.get("product_name") != null) {
            details.append(String.format("📦 Product:         %s%n", loan.get("product_name")));
        }
        
        details.append(String.format("📊 Status:          %s%n", loan.get("status")));
        details.append(String.format("👨‍💼 Created By:      %s%n", loan.get("employee_name")));
        details.append(String.format("📅 Application:     %s%n", formatDate((java.sql.Date) loan.get("application_date"))));
        
        if (loan.get("approved_by_name") != null) {
            details.append(String.format("✅ Approved By:     %s%n", loan.get("approved_by_name")));
            details.append(String.format("📅 Approved Date:   %s%n", formatDate((java.sql.Date) loan.get("approved_date"))));
        }
        
        details.append(String.format("📅 Due Date:        %s%n", formatDate((java.sql.Date) loan.get("due_date"))));
        
        // Add financial summary
        details.append("\n💵 FINANCIAL SUMMARY\n");
        details.append("═══════════════════════════════════════════════════════\n");
        details.append(String.format("Total Payable:     K %,.2f%n", loan.get("total_amount"))); // FIXED: ZMW to K
        details.append(String.format("Installment:       K %,.2f%n", loan.get("installment_amount"))); // FIXED: ZMW to K
        //details.append(String.format("Outstanding:       K %,.2f%n", loan.get("outstanding_balance"))); // FIXED: ZMW to K
        
        if ("Rejected".equals(loan.get("status")) && loan.get("rejection_reason") != null) {
            details.append("\n❌ REJECTION REASON\n");
            details.append("═══════════════════════════════════════════════════════\n");
            details.append(String.format("%s%n", loan.get("rejection_reason")));
        }
        
        if (loan.get("collateral_details") != null && !((String) loan.get("collateral_details")).trim().isEmpty()) {
            details.append("\n🏷️ COLLATERAL DETAILS\n");
            details.append("═══════════════════════════════════════════════════════\n");
            details.append(String.format("%s%n", loan.get("collateral_details")));
        }
        
        if (loan.get("guarantors_details") != null && !((String) loan.get("guarantors_details")).trim().isEmpty()) {
            details.append("\n🤝 GUARANTORS\n");
            details.append("═══════════════════════════════════════════════════════\n");
            details.append(String.format("%s%n", loan.get("guarantors_details")));
        }
        
        JTextArea textArea = new JTextArea(details.toString(), 30, 70);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setBackground(new Color(250, 250, 250));
        textArea.setCaretPosition(0);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        
        JOptionPane.showMessageDialog(this, scrollPane, 
            "Loan Details - " + loan.get("loan_number"), 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void approveLoanAsync() {
        int selectedRow = loansTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a loan to approve");
            return;
        }
        
        int modelRow = loansTable.convertRowIndexToModel(selectedRow);
        int loanId = (Integer) tableModel.getValueAt(modelRow, 0);
        String loanNumber = (String) tableModel.getValueAt(modelRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "✅ APPROVE LOAN\n\n" +
            "Are you sure you want to approve loan: " + loanNumber + "?\n\n" +
            "This action cannot be undone.",
            "Confirm Approval", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            showLoading(true);
            
            AsyncDatabaseService.executeAsync(() -> {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    if (conn == null || conn.isClosed()) {
                        return false;
                    }
                    
                    String sql = "UPDATE loans SET status = 'Approved', approved_by = ?, " +
                                "approved_date = CURRENT_TIMESTAMP, disbursement_date = CURRENT_TIMESTAMP " +
                                "WHERE loan_id = ? AND status = 'Pending'";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, employeeId);
                        stmt.setInt(2, loanId);
                        int rows = stmt.executeUpdate();
                        return rows > 0;
                    }
                } catch (SQLException ex) {
                    System.err.println("Error approving loan: " + ex.getMessage());
                    return false;
                }
            },
            success -> {
                SwingUtilities.invokeLater(() -> {
                    showLoading(false);
                    if ((Boolean) success) {
                        JOptionPane.showMessageDialog(this,
                            "✅ Loan approved successfully!\n\n" +
                            "Loan Number: " + loanNumber + "\n" +
                            "Status updated to: Approved",
                            "Approval Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        AsyncDatabaseService.logAsync(employeeId, "Loan Approved", 
                            "Approved loan: " + loanNumber);
                        refreshAllLoans();
                    } else {
                        showError("Loan approval failed. Loan may have already been processed.");
                    }
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    showLoading(false);
                    showError("Approval failed: " + e.getMessage());
                });
            });
        }
    }
    
    private void rejectLoan() {
        int selectedRow = loansTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a loan to reject");
            return;
        }
        
        int modelRow = loansTable.convertRowIndexToModel(selectedRow);
        int loanId = (Integer) tableModel.getValueAt(modelRow, 0);
        String loanNumber = (String) tableModel.getValueAt(modelRow, 1);
        
        // Create rejection reason dialog
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel header = new JLabel("❌ REJECT LOAN APPLICATION");
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.setForeground(DANGER_COLOR);
        
        JLabel infoLabel = new JLabel("<html><b>Loan Number:</b> " + loanNumber + "<br><br>" +
                                     "Please provide detailed reason for rejection:</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JTextArea reasonArea = new JTextArea(5, 40);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        JScrollPane scrollPane = new JScrollPane(reasonArea);
        scrollPane.setPreferredSize(new Dimension(400, 120));
        
        panel.add(header, BorderLayout.NORTH);
        panel.add(infoLabel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);
        
        int option = JOptionPane.showConfirmDialog(this, panel, 
            "Reject Loan - " + loanNumber, 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            String rejectionReason = reasonArea.getText().trim();
            if (rejectionReason.isEmpty()) {
                showError("Please provide a rejection reason");
                return;
            }
            
            if (rejectionReason.length() < 10) {
                showError("Please provide a more detailed rejection reason (minimum 10 characters)");
                return;
            }
            
            showLoading(true);
            
            AsyncDatabaseService.executeAsync(() -> {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    if (conn == null || conn.isClosed()) {
                        return false;
                    }
                    
                    // FIXED: Using reviewed_by instead of approved_by for consistency with database
                    String sql = "UPDATE loans SET status = 'Rejected', reviewed_by = ?, " +
                                "reviewed_date = CURRENT_TIMESTAMP, rejection_reason = ? " +
                                "WHERE loan_id = ? AND status = 'Pending'";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, employeeId);
                        stmt.setString(2, rejectionReason);
                        stmt.setInt(3, loanId);
                        int rows = stmt.executeUpdate();
                        return rows > 0;
                    }
                } catch (SQLException ex) {
                    System.err.println("Error rejecting loan: " + ex.getMessage());
                    return false;
                }
            },
            success -> {
                SwingUtilities.invokeLater(() -> {
                    showLoading(false);
                    if ((Boolean) success) {
                        JOptionPane.showMessageDialog(this,
                            "✅ Loan rejected successfully!\n\n" +
                            "Rejection reason has been recorded and will be visible to all users.",
                            "Rejection Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        AsyncDatabaseService.logAsync(employeeId, "Loan Rejected", 
                            "Rejected loan: " + loanNumber + " - Reason: " + rejectionReason.substring(0, Math.min(100, rejectionReason.length())));
                        refreshAllLoans();
                    } else {
                        showError("Loan rejection failed. Loan may have already been processed.");
                    }
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    showLoading(false);
                    showError("Rejection failed: " + e.getMessage());
                });
            });
        }
    }
    
    private void deleteLoanAsync() {
        int selectedRow = loansTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a loan to delete");
            return;
        }
        
        int modelRow = loansTable.convertRowIndexToModel(selectedRow);
        int loanId = (Integer) tableModel.getValueAt(modelRow, 0);
        String status = (String) tableModel.getValueAt(modelRow, 4);
        String loanNumber = (String) tableModel.getValueAt(modelRow, 1);
        
        // Check permissions
        if (!"admin".equals(userRole) && !"Pending".equals(status)) {
            showError("Employees can only delete pending loans");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "🗑️ DELETE LOAN\n\n" +
            "Are you sure you want to delete loan: " + loanNumber + "?\n\n" +
            "⚠️  Warning: This action is permanent and cannot be undone!",
            "Confirm Deletion", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            showLoading(true);
            
            AsyncDatabaseService.executeAsync(() -> {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    if (conn == null || conn.isClosed()) {
                        return false;
                    }
                    
                    conn.setAutoCommit(false);
                    try {
                        // Delete in correct order to maintain referential integrity
                        String[] deleteQueries = {
                            "DELETE FROM loan_payments WHERE loan_id = ?",
                            "DELETE FROM payment_receipts WHERE loan_id = ?",
                            "DELETE FROM loan_schedules WHERE loan_id = ?",
                            "DELETE FROM loans WHERE loan_id = ?"
                        };
                        
                        for (String query : deleteQueries) {
                            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                                stmt.setInt(1, loanId);
                                stmt.executeUpdate();
                            }
                        }
                        conn.commit();
                        return true;
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException ex) {
                    System.err.println("Error deleting loan: " + ex.getMessage());
                    return false;
                }
            },
            success -> {
                SwingUtilities.invokeLater(() -> {
                    showLoading(false);
                    if ((Boolean) success) {
                        JOptionPane.showMessageDialog(this,
                            "✅ Loan deleted successfully!",
                            "Deletion Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        AsyncDatabaseService.logAsync(employeeId, "Loan Deleted", 
                            "Deleted loan: " + loanNumber);
                        refreshAllLoans();
                    } else {
                        showError("Loan deletion failed. Please try again.");
                    }
                });
            },
            e -> {
                SwingUtilities.invokeLater(() -> {
                    showLoading(false);
                    showError("Deletion failed: " + e.getMessage());
                });
            });
        }
    }
    
    private void navigateToDashboard() {
        SwingUtilities.invokeLater(() -> {
            if ("admin".equals(userRole)) {
                ScreenManager.getInstance().showScreen(new AdminDashboard(employeeId, "Admin"));
            } else {
                ScreenManager.getInstance().showScreen(new EmployeeDashboard(employeeId, "Employee"));
            }
        });
    }
    
    private String formatDate(java.sql.Date date) {
        if (date == null) return "N/A";
        return dateFormat.format(date);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}