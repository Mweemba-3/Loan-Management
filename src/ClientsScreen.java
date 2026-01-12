import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ClientsScreen extends JPanel {
    private JTable clientsTable;
    private ClientTableModel tableModel;
    private JTextField searchField;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    private int currentUserId;
    private String currentUserRole;
    
    private final Color LIGHT_BG = new Color(245, 245, 245);
    private final Color CARD_BG = Color.WHITE;
    private final Color TEXT_DARK = new Color(50, 50, 50);
    private final Color TEXT_GRAY = new Color(120, 120, 120);
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color GREEN_SUCCESS = new Color(39, 174, 96);
    private final Color RED_ALERT = new Color(231, 76, 60);
    private final Color ORANGE_WARNING = new Color(220, 140, 60);
    
    private JButton addBtn, editBtn, deleteBtn, viewBtn, refreshBtn, backBtn;
    
    public ClientsScreen(int userId, String userRole) {
        this.currentUserId = userId;
        this.currentUserRole = userRole;
        initUI();
        loadClientsDataAsync();
        AsyncDatabaseService.logAsync(userId, "Clients Access", "Accessed clients management");
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(LIGHT_BG);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        JPanel searchFilterPanel = createSearchFilterPanel();
        add(searchFilterPanel, BorderLayout.NORTH);
        
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);
        
        JPanel buttonsPanel = createButtonsPanel();
        add(buttonsPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(LIGHT_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("CLIENTS MANAGEMENT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_DARK);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        return headerPanel;
    }
    
    private JPanel createSearchFilterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(LIGHT_BG);
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchLabel.setForeground(TEXT_DARK);
        
        searchField = new JTextField(25);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBackground(CARD_BG);
        searchField.setForeground(TEXT_DARK);
        searchField.setCaretColor(TEXT_DARK);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Client name, ID number, phone...");
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchClientsAsync();
                }
            }
        });
        
        JButton searchBtn = new JButton("Search");
        styleButton(searchBtn, PRIMARY_BLUE);
        searchBtn.addActionListener(e -> searchClientsAsync());
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        
        panel.add(searchPanel);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(LIGHT_BG);
        
        tableModel = new ClientTableModel();
        clientsTable = new JTable(tableModel);
        customizeTable(clientsTable);
        
        clientsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showClientDetails();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(clientsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setBackground(CARD_BG);
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        return tablePanel;
    }
    
    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonsPanel.setBackground(LIGHT_BG);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        addBtn = new JButton("➕ Add Client");
        styleButton(addBtn, GREEN_SUCCESS);
        addBtn.addActionListener(e -> showAddClientScreen());
        
        editBtn = new JButton("✏️ Edit");
        styleButton(editBtn, ORANGE_WARNING);
        editBtn.addActionListener(e -> showEditClientScreen());
        
        deleteBtn = new JButton("🗑️ Delete");
        styleButton(deleteBtn, RED_ALERT);
        deleteBtn.addActionListener(e -> deleteSelectedClient());
        
        viewBtn = new JButton("👁️ View");
        styleButton(viewBtn, PRIMARY_BLUE);
        viewBtn.addActionListener(e -> showClientDetails());
        
        refreshBtn = new JButton("🔄 Refresh");
        styleButton(refreshBtn, PRIMARY_BLUE);
        refreshBtn.addActionListener(e -> loadClientsDataAsync());
        
        backBtn = new JButton("🏠 Dashboard");
        styleButton(backBtn, TEXT_GRAY);
        backBtn.addActionListener(e -> goBackHome());
        
        buttonsPanel.add(addBtn);
        buttonsPanel.add(editBtn);
        buttonsPanel.add(deleteBtn);
        buttonsPanel.add(viewBtn);
        buttonsPanel.add(refreshBtn);
        buttonsPanel.add(backBtn);
        
        return buttonsPanel;
    }
    
    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
    }
    
    private void customizeTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(40);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(52, 73, 94));
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        
        TableColumnModel columnModel = table.getColumnModel();
        
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(120);
        columnModel.getColumn(2).setPreferredWidth(120);
        columnModel.getColumn(3).setPreferredWidth(120);
        columnModel.getColumn(4).setPreferredWidth(100);
        columnModel.getColumn(5).setPreferredWidth(100);
        columnModel.getColumn(6).setPreferredWidth(100);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setCellRenderer(centerRenderer);
        }
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(SwingConstants.CENTER);
                
                if (isSelected) {
                    setBackground(PRIMARY_BLUE);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(row % 2 == 0 ? CARD_BG : new Color(250, 250, 250));
                    setForeground(TEXT_DARK);
                }
                
                return this;
            }
        });
    }
    
    private void loadClientsDataAsync() {
        setComponentsEnabled(false);
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return new ArrayList<Client>();
                }
                
                String sql = "SELECT client_id, first_name, last_name, phone_number, id_number, date_of_birth, created_at FROM clients WHERE is_active = true ORDER BY created_at DESC";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Client> clients = new ArrayList<>();
                        while (rs.next()) {
                            Client client = new Client(
                                rs.getInt("client_id"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("id_number"),
                                rs.getString("phone_number"),
                                rs.getDate("date_of_birth"),
                                rs.getTimestamp("created_at")
                            );
                            clients.add(client);
                        }
                        return clients;
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error loading clients: " + ex.getMessage());
                return new ArrayList<Client>();
            }
        }, 
        clients -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setClients((List<Client>) clients);
                setComponentsEnabled(true);
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                setComponentsEnabled(true);
                showError("Failed to load clients: " + e.getMessage());
            });
        });
    }
    
    private void searchClientsAsync() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadClientsDataAsync();
            return;
        }
        
        setComponentsEnabled(false);
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return new ArrayList<Client>();
                }
                
                String sql = "SELECT client_id, first_name, last_name, phone_number, id_number, date_of_birth, created_at FROM clients WHERE is_active = true AND (first_name ILIKE ? OR last_name ILIKE ? OR phone_number ILIKE ? OR id_number ILIKE ?) ORDER BY created_at DESC";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setQueryTimeout(5);
                    String likeTerm = "%" + searchTerm + "%";
                    stmt.setString(1, likeTerm);
                    stmt.setString(2, likeTerm);
                    stmt.setString(3, likeTerm);
                    stmt.setString(4, likeTerm);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Client> clients = new ArrayList<>();
                        while (rs.next()) {
                            Client client = new Client(
                                rs.getInt("client_id"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("id_number"),
                                rs.getString("phone_number"),
                                rs.getDate("date_of_birth"),
                                rs.getTimestamp("created_at")
                            );
                            clients.add(client);
                        }
                        return clients;
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error searching clients: " + ex.getMessage());
                return new ArrayList<Client>();
            }
        },
        clients -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.setClients((List<Client>) clients);
                setComponentsEnabled(true);
                
                if (((List<Client>) clients).isEmpty()) {
                    showInfo("No clients found for: " + searchTerm);
                }
            });
            
            AsyncDatabaseService.logAsync(currentUserId, "Client Search", "Searched for: " + searchTerm);
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                setComponentsEnabled(true);
                showError("Search failed: " + e.getMessage());
            });
        });
    }
    
    private void deleteSelectedClient() {
        int selectedRow = clientsTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a client to delete");
            return;
        }
        
        int modelRow = clientsTable.convertRowIndexToModel(selectedRow);
        Client client = tableModel.getClientAt(modelRow);
        String clientName = client.getFirstName() + " " + client.getLastName();
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return false;
                }
                
                String sql = "SELECT COUNT(*) FROM loans WHERE client_id = ? AND status IN ('Approved', 'Due', 'Pending')";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, client.getClientId());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
                return false;
            } catch (SQLException ex) {
                System.err.println("Error checking client loans: " + ex.getMessage());
                return false;
            }
        },
        hasLoans -> {
            if (!currentUserRole.equalsIgnoreCase("admin") && (Boolean) hasLoans) {
                showError("Access Denied: Only administrators can delete clients with active loans.\n\n" +
                         "Client '" + clientName + "' has active loan records.\n" +
                         "Please contact your administrator for client deletion.");
                return;
            }
            
            SwingUtilities.invokeLater(() -> {
                if (currentUserRole.equalsIgnoreCase("admin")) {
                    if ((Boolean) hasLoans) {
                        int confirm = JOptionPane.showConfirmDialog(this,
                            "ADMIN DELETE - CLIENT WITH LOANS\n\n" +
                            "Client: " + clientName + "\n" +
                            "Status: Has active loans\n\n" +
                            "This will permanently delete:\n" +
                            "• All client loans and payment records\n" +
                            "• Next of kin information\n" +
                            "• Bank details\n" +
                            "• All related data\n\n" +
                            "This action cannot be undone!",
                            "Admin - Confirm Delete Client with Loans",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                            
                        if (confirm == JOptionPane.YES_OPTION) {
                            softDeleteClientAsync(client.getClientId(), clientName);
                        }
                    } else {
                        int confirm = JOptionPane.showConfirmDialog(this,
                            "Delete client: " + clientName + "?",
                            "Admin - Confirm Delete", 
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                            
                        if (confirm == JOptionPane.YES_OPTION) {
                            softDeleteClientAsync(client.getClientId(), clientName);
                        }
                    }
                } else {
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "EMPLOYEE DELETE CONFIRMATION\n\n" +
                        "You are about to delete client: " + clientName + "\n\n" +
                        "Note: Employees can only delete clients without active loans.\n" +
                        "This action will remove all client data permanently.\n\n" +
                        "Are you sure you want to proceed?",
                        "Employee - Confirm Delete", 
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                        
                    if (confirm == JOptionPane.YES_OPTION) {
                        softDeleteClientAsync(client.getClientId(), clientName);
                    }
                }
            });
        },
        e -> {
            showError("Error checking client loans: " + e.getMessage());
        });
    }
    
    private void softDeleteClientAsync(int clientId, String clientName) {
        setComponentsEnabled(false);
        
        AsyncDatabaseService.executeAsync(() -> {
            Connection conn = null;
            try {
                conn = DatabaseConnection.getConnection();
                conn.setAutoCommit(false);
                
                // First, soft delete the client (mark as inactive)
                String updateClient = "UPDATE clients SET is_active = false, updated_at = CURRENT_TIMESTAMP WHERE client_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateClient)) {
                    stmt.setInt(1, clientId);
                    int affectedRows = stmt.executeUpdate();
                    
                    if (affectedRows > 0) {
                        // If admin deleting with loans, also update loan status
                        if (currentUserRole.equalsIgnoreCase("admin")) {
                            String updateLoans = "UPDATE loans SET status = 'Closed', closing_reason = 'Client deleted by admin', updated_at = CURRENT_TIMESTAMP WHERE client_id = ? AND status IN ('Approved', 'Due', 'Pending')";
                            try (PreparedStatement loanStmt = conn.prepareStatement(updateLoans)) {
                                loanStmt.setInt(1, clientId);
                                loanStmt.executeUpdate();
                            }
                        }
                        
                        conn.commit();
                        return true;
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("Rollback failed: " + ex.getMessage());
                    }
                }
                
                // Check if it's a sequence issue
                if (e.getMessage().contains("S_1") || e.getMessage().contains("already exists")) {
                    // Fix the sequence issue
                    try {
                        if (conn != null) {
                            // Reset client_id sequence
                            String fixSequence = "SELECT setval('clients_client_id_seq', COALESCE((SELECT MAX(client_id) FROM clients), 0) + 1, false)";
                            try (PreparedStatement fixStmt = conn.prepareStatement(fixSequence)) {
                                fixStmt.executeQuery();
                            }
                        }
                    } catch (SQLException seqEx) {
                        System.err.println("Failed to fix sequence: " + seqEx.getMessage());
                    }
                }
                
                throw new RuntimeException("Database error: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException ex) {
                        System.err.println("Error closing connection: " + ex.getMessage());
                    }
                }
            }
        },
        success -> {
            SwingUtilities.invokeLater(() -> {
                setComponentsEnabled(true);
                if ((Boolean) success) {
                    showSuccess("Client '" + clientName + "' deleted successfully");
                    AsyncDatabaseService.logAsync(currentUserId, "Client Deleted", "Deleted client: " + clientName);
                    loadClientsDataAsync();
                } else {
                    showError("Failed to delete client. Client not found.");
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                setComponentsEnabled(true);
                
                String errorMsg = e.getMessage();
                if (errorMsg.contains("S_1") || errorMsg.contains("already exists")) {
                    showError("Database sequence error. Please try again or contact administrator.\nError: " + errorMsg);
                } else {
                    showError("Error deleting client: " + errorMsg);
                }
            });
        });
    }
    
    private void showAddClientScreen() {
        SwingUtilities.invokeLater(() -> {
            ScreenManager.getInstance().showScreen(new AddClientScreen(currentUserId, currentUserRole));
        });
    }
    
    private void showEditClientScreen() {
        int selectedRow = clientsTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a client to edit");
            return;
        }
        int modelRow = clientsTable.convertRowIndexToModel(selectedRow);
        Client client = tableModel.getClientAt(modelRow);

        SwingUtilities.invokeLater(() -> {
            ScreenManager.getInstance().showScreen(new EditClientScreen(client.getClientId(), currentUserId, currentUserRole));
        });
    }
    
    private void showClientDetails() {
        int selectedRow = clientsTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a client to view");
            return;
        }
        
        int modelRow = clientsTable.convertRowIndexToModel(selectedRow);
        Client client = tableModel.getClientAt(modelRow);
        
        SwingUtilities.invokeLater(() -> {
            ScreenManager.getInstance().showScreen(new ClientDetailsScreen(client, currentUserId, currentUserRole));
        });
    }
    
    private void goBackHome() {
        SwingUtilities.invokeLater(() -> {
            if (currentUserRole.equalsIgnoreCase("admin")) {
                ScreenManager.getInstance().showScreen(new AdminDashboard(currentUserId, getEmployeeName(currentUserId)));
            } else {
                ScreenManager.getInstance().showScreen(new EmployeeDashboard(currentUserId, getEmployeeName(currentUserId)));
            }
        });
    }
    
    private String getEmployeeName(int employeeId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT name FROM employees WHERE employee_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException ex) {
            System.err.println("Error getting employee name: " + ex.getMessage());
        }
        return "User";
    }
    
    private void setComponentsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            addBtn.setEnabled(enabled);
            editBtn.setEnabled(enabled);
            deleteBtn.setEnabled(enabled);
            viewBtn.setEnabled(enabled);
            refreshBtn.setEnabled(enabled);
            backBtn.setEnabled(enabled);
            searchField.setEnabled(enabled);
            clientsTable.setEnabled(enabled);
        });
    }
    
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private class ClientTableModel extends AbstractTableModel {
        private List<Client> clients = new ArrayList<>();
        private String[] columnNames = {"ID", "First Name", "Last Name", "ID Number", "Phone", "Date of Birth", "Member Since"};
        
        public void setClients(List<Client> clients) {
            this.clients = clients;
            fireTableDataChanged();
        }
        
        public Client getClientAt(int row) {
            return clients.get(row);
        }
        
        @Override public int getRowCount() { return clients.size(); }
        @Override public int getColumnCount() { return columnNames.length; }
        @Override public String getColumnName(int col) { return columnNames[col]; }
        
        @Override
        public Object getValueAt(int row, int col) {
            Client client = clients.get(row);
            switch (col) {
                case 0: return client.getClientId();
                case 1: return client.getFirstName();
                case 2: return client.getLastName();
                case 3: return client.getIdNumber();
                case 4: return client.getPhoneNumber();
                case 5: return client.getDateOfBirth() != null ? dateFormat.format(client.getDateOfBirth()) : "N/A";
                case 6: return client.getCreatedAt() != null ? dateFormat.format(client.getCreatedAt()) : "N/A";
                default: return null;
            }
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }
    
    public static class Client {
        private int clientId;
        private String firstName;
        private String lastName;
        private String idNumber;
        private String phoneNumber;
        private Date dateOfBirth;
        private Timestamp createdAt;
        
        public Client(int clientId, String firstName, String lastName, String idNumber, 
                     String phoneNumber, Date dateOfBirth, Timestamp createdAt) {
            this.clientId = clientId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.idNumber = idNumber;
            this.phoneNumber = phoneNumber;
            this.dateOfBirth = dateOfBirth;
            this.createdAt = createdAt;
        }
        
        public int getClientId() { return clientId; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getIdNumber() { return idNumber; }
        public String getPhoneNumber() { return phoneNumber; }
        public Date getDateOfBirth() { return dateOfBirth; }
        public Timestamp getCreatedAt() { return createdAt; }
    }
}