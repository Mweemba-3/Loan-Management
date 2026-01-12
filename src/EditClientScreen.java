import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class EditClientScreen extends JPanel {
    private int clientId;
    private int currentUserId;
    private String currentUserRole;
    
    // Form fields
    private JTextField firstNameField, middleNameField, lastNameField, phoneField, emailField;
    private JTextField physicalAddressField, provinceField, postalAddressField, idNumberField;
    private JTextField employerNameField, employeeNumberField, jobTitleField, monthlyIncomeField;
    private JComboBox<String> titleCombo, idTypeCombo, employmentStatusCombo;
    private JComboBox<String> genderCombo, maritalStatusCombo, branchCombo;
    private JTextField dobField;
    
    // Next of Kin fields
    private JTextField kinNameField, kinPhoneField, kinAddressField, kinIdField;
    private JComboBox<String> kinRelationshipCombo;
    
    // Bank details fields
    private JTextField bankNameField, accountNumberField, accountNameField, branchCodeField, branchNameField;
    
    private JButton saveButton, cancelButton;
    private JPanel mainContentPanel;
    private JLayeredPane layeredPane;
    
    private final Color LIGHT_BG = new Color(245, 245, 245);
    private final Color CARD_BG = Color.WHITE;
    private final Color TEXT_DARK = new Color(50, 50, 50);
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color GREEN_SUCCESS = new Color(39, 174, 96);
    private final Color RED_ALERT = new Color(231, 76, 60);
    
    public EditClientScreen(int clientId, int userId, String userRole) {
        this.clientId = clientId;
        this.currentUserId = userId;
        this.currentUserRole = userRole;
        initUI();
        loadClientDataAsync();
        AsyncDatabaseService.logAsync(userId, "Edit Client Screen", "Editing client ID: " + clientId);
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(LIGHT_BG);
        
        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1200, 700));
        
        // Main content panel
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(LIGHT_BG);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainContentPanel.setBounds(0, 0, 1200, 700);
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(LIGHT_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("EDIT CLIENT - ID: " + clientId);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_DARK);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(LIGHT_BG);
        
        saveButton = new JButton("💾 Save");
        cancelButton = new JButton("❌ Cancel");
        
        styleButton(saveButton, GREEN_SUCCESS, GREEN_SUCCESS.darker());
        styleButton(cancelButton, RED_ALERT, RED_ALERT.darker());
        
        saveButton.addActionListener(e -> saveClientAsync());
        cancelButton.addActionListener(e -> goBack());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        mainContentPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Main form with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        tabbedPane.addTab("Personal Info", createScrollablePanel(createPersonalInfoPanel()));
        tabbedPane.addTab("Next of Kin", createScrollablePanel(createNextOfKinPanel()));
        tabbedPane.addTab("Bank Details", createScrollablePanel(createBankDetailsPanel()));
        
        mainContentPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Add main content to layered pane
        layeredPane.add(mainContentPanel, Integer.valueOf(0));
        
        // Loading overlay (initially hidden)
        JPanel loadingOverlay = createLoadingOverlay();
        loadingOverlay.setBounds(0, 0, 1200, 700);
        loadingOverlay.setVisible(false);
        layeredPane.add(loadingOverlay, Integer.valueOf(1));
        
        // Store reference to loading overlay
        layeredPane.putClientProperty("loadingOverlay", loadingOverlay);
        
        add(layeredPane, BorderLayout.CENTER);
    }
    
    private JScrollPane createScrollablePanel(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }
    
    private JPanel createLoadingOverlay() {
        JPanel overlay = new JPanel(new GridBagLayout());
        overlay.setBackground(new Color(0, 0, 0, 100));
        overlay.setOpaque(true);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(255, 255, 255, 255));
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_BLUE, 2),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));
        
        JLabel loadingLabel = new JLabel("Loading client data...");
        loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loadingLabel.setForeground(TEXT_DARK);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(200, 10));
        progressBar.setMaximumSize(new Dimension(200, 10));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(loadingLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(progressBar);
        
        overlay.add(contentPanel);
        
        return overlay;
    }
    
    private void setLoading(boolean loading) {
        SwingUtilities.invokeLater(() -> {
            JPanel loadingOverlay = (JPanel) layeredPane.getClientProperty("loadingOverlay");
            if (loadingOverlay != null) {
                loadingOverlay.setVisible(loading);
            }
            
            saveButton.setEnabled(!loading);
            cancelButton.setEnabled(!loading);
            setFormEnabled(!loading);
            
            if (loading) {
                saveButton.setText("Loading...");
            } else {
                saveButton.setText("💾 Save");
            }
        });
    }
    
    private void setFormEnabled(boolean enabled) {
        Component[] components = mainContentPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) comp;
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component tab = tabbedPane.getComponentAt(i);
                    if (tab instanceof JScrollPane) {
                        tab = ((JScrollPane) tab).getViewport().getView();
                    }
                    setComponentEnabled(tab, enabled);
                }
            }
        }
    }
    
    private void setComponentEnabled(Component comp, boolean enabled) {
        comp.setEnabled(enabled);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                setComponentEnabled(child, enabled);
            }
        }
    }
    
    private void styleButton(JButton button, Color bgColor, Color hoverColor) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
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
    
    private JPanel createPersonalInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Personal Information Section
        JPanel personalSection = createSectionPanel("Personal Information");
        personalSection.setLayout(new GridLayout(0, 4, 10, 10));
        
        personalSection.add(new JLabel("Branch:"));
        String[] branches = {"Lusaka Main", "Kitwe Copperbelt", "Ndola Central", "Livingstone South", "Chipata East"};
        branchCombo = new JComboBox<>(branches);
        personalSection.add(branchCombo);
        
        personalSection.add(new JLabel("Title:"));
        titleCombo = new JComboBox<>(new String[]{"Mr", "Ms", "Mrs", "Dr", "Prof", "Other"});
        personalSection.add(titleCombo);
        
        personalSection.add(new JLabel("First Name:*"));
        firstNameField = new JTextField();
        personalSection.add(firstNameField);
        
        personalSection.add(new JLabel("Middle Name:"));
        middleNameField = new JTextField();
        personalSection.add(middleNameField);
        
        personalSection.add(new JLabel("Last Name:*"));
        lastNameField = new JTextField();
        personalSection.add(lastNameField);
        
        personalSection.add(new JLabel("Date of Birth:*"));
        dobField = new JTextField();
        dobField.setToolTipText("YYYY-MM-DD");
        personalSection.add(dobField);
        
        personalSection.add(new JLabel("Gender:*"));
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        personalSection.add(genderCombo);
        
        personalSection.add(new JLabel("Marital Status:*"));
        maritalStatusCombo = new JComboBox<>(new String[]{"Single", "Married", "Divorced", "Widowed"});
        personalSection.add(maritalStatusCombo);
        
        panel.add(personalSection);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Contact Information Section
        JPanel contactSection = createSectionPanel("Contact Information");
        contactSection.setLayout(new GridLayout(0, 4, 10, 10));
        
        contactSection.add(new JLabel("Phone:*"));
        phoneField = new JTextField();
        contactSection.add(phoneField);
        
        contactSection.add(new JLabel("Email:"));
        emailField = new JTextField();
        contactSection.add(emailField);
        
        contactSection.add(new JLabel("Physical Address:*"));
        physicalAddressField = new JTextField();
        contactSection.add(physicalAddressField);
        
        contactSection.add(new JLabel("Province:*"));
        provinceField = new JTextField();
        contactSection.add(provinceField);
        
        contactSection.add(new JLabel("Postal Address:"));
        postalAddressField = new JTextField();
        contactSection.add(postalAddressField);
        
        panel.add(contactSection);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // ID Information Section
        JPanel idSection = createSectionPanel("Identification");
        idSection.setLayout(new GridLayout(0, 4, 10, 10));
        
        idSection.add(new JLabel("ID Type:*"));
        idTypeCombo = new JComboBox<>(new String[]{"NRC", "Passport", "Driver's License", "Other"});
        idSection.add(idTypeCombo);
        
        idSection.add(new JLabel("ID Number:*"));
        idNumberField = new JTextField();
        idSection.add(idNumberField);
        
        panel.add(idSection);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Employment Information Section
        JPanel employmentSection = createSectionPanel("Employment Information");
        employmentSection.setLayout(new GridLayout(0, 4, 10, 10));
        
        employmentSection.add(new JLabel("Employment Status:*"));
        employmentStatusCombo = new JComboBox<>(new String[]{"Employed", "Self-Employed", "Unemployed"});
        employmentSection.add(employmentStatusCombo);
        
        employmentSection.add(new JLabel("Employer:"));
        employerNameField = new JTextField();
        employmentSection.add(employerNameField);
        
        employmentSection.add(new JLabel("Employee #:"));
        employeeNumberField = new JTextField();
        employmentSection.add(employeeNumberField);
        
        employmentSection.add(new JLabel("Job Title:"));
        jobTitleField = new JTextField();
        employmentSection.add(jobTitleField);
        
        employmentSection.add(new JLabel("Monthly Income:"));
        monthlyIncomeField = new JTextField();
        employmentSection.add(monthlyIncomeField);
        
        panel.add(employmentSection);
        
        return panel;
    }
    
private JPanel createNextOfKinPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    
    JPanel kinSection = createSectionPanel("Next of Kin Information");
    kinSection.setLayout(new GridBagLayout());
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    kinSection.add(new JLabel("Name:*"), gbc);
    
    gbc.gridx = 1;
    kinNameField = new JTextField(20); // Regular size
    kinSection.add(kinNameField, gbc);
    
    gbc.gridx = 2;
    kinSection.add(new JLabel("Relationship:*"), gbc);
    
    gbc.gridx = 3;
    kinRelationshipCombo = new JComboBox<>(new String[]{"Spouse", "Parent", "Sibling", "Other"});
    kinSection.add(kinRelationshipCombo, gbc);
    
    gbc.gridx = 0;
    gbc.gridy = 1;
    kinSection.add(new JLabel("Phone:*"), gbc);
    
    gbc.gridx = 1;
    kinPhoneField = new JTextField(20); // Regular size
    kinSection.add(kinPhoneField, gbc);
    
    gbc.gridx = 2;
    kinSection.add(new JLabel("Address:"), gbc);
    
    gbc.gridx = 3;
    kinAddressField = new JTextField(20); // Regular size
    kinSection.add(kinAddressField, gbc);
    
    gbc.gridx = 0;
    gbc.gridy = 2;
    kinSection.add(new JLabel("ID Number:"), gbc);
    
    gbc.gridx = 1;
    kinIdField = new JTextField(20); // Regular size
    kinSection.add(kinIdField, gbc);
    
    panel.add(kinSection);
    
    return panel;
}

private JPanel createBankDetailsPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    
    JPanel bankSection = createSectionPanel("Bank Account Details");
    bankSection.setLayout(new GridBagLayout());
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    bankSection.add(new JLabel("Bank Name:*"), gbc);
    
    gbc.gridx = 1;
    bankNameField = new JTextField(20); // Regular size
    bankSection.add(bankNameField, gbc);
    
    gbc.gridx = 2;
    bankSection.add(new JLabel("Account Number:*"), gbc);
    
    gbc.gridx = 3;
    accountNumberField = new JTextField(20); // Regular size
    bankSection.add(accountNumberField, gbc);
    
    gbc.gridx = 0;
    gbc.gridy = 1;
    bankSection.add(new JLabel("Account Name:*"), gbc);
    
    gbc.gridx = 1;
    accountNameField = new JTextField(20); // Regular size
    bankSection.add(accountNameField, gbc);
    
    gbc.gridx = 2;
    bankSection.add(new JLabel("Branch Code:"), gbc);
    
    gbc.gridx = 3;
    branchCodeField = new JTextField(20); // Regular size
    bankSection.add(branchCodeField, gbc);
    
    gbc.gridx = 0;
    gbc.gridy = 2;
    bankSection.add(new JLabel("Branch Name:"), gbc);
    
    gbc.gridx = 1;
    branchNameField = new JTextField(20); // Regular size
    bankSection.add(branchNameField, gbc);
    
    panel.add(bankSection);
    
    return panel;
}
    
    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            title
        );
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        border.setTitleColor(new Color(70, 70, 70));
        panel.setBorder(BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        return panel;
    }
    
    private void loadClientDataAsync() {
        setLoading(true);
        
        AsyncDatabaseService.executeAsync(() -> {
            Connection conn = null;
            try {
                conn = DatabaseConnection.getConnection();
                if (conn == null || conn.isClosed()) {
                    return new LoadResult(false, "Database connection failed", null, null, null);
                }
                
                // Load all data
                ClientData clientData = loadClientDetails(conn);
                KinData kinData = loadNextOfKinDetails(conn);
                BankData bankData = loadBankDetails(conn);
                
                return new LoadResult(true, "Success", clientData, kinData, bankData);
                
            } catch (SQLException e) {
                return new LoadResult(false, "Database error: " + e.getMessage(), null, null, null);
            } finally {
                if (conn != null) {
                    try { conn.close(); } catch (SQLException e) {}
                }
            }
        },
        result -> {
            SwingUtilities.invokeLater(() -> {
                setLoading(false);
                
                LoadResult loadResult = (LoadResult) result;
                if (loadResult.success) {
                    // Update UI with loaded data
                    updateUIWithClientData(loadResult.clientData);
                    updateUIWithKinData(loadResult.kinData);
                    updateUIWithBankData(loadResult.bankData);
                } else {
                    showError("Error loading client: " + loadResult.message);
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                setLoading(false);
                showError("Error loading client: " + e.getMessage());
            });
        });
    }
    
    private ClientData loadClientDetails(Connection conn) throws SQLException {
        String sql = "SELECT c.*, b.branch_name FROM clients c LEFT JOIN branches b ON c.branch_id = b.branch_id WHERE c.client_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ClientData(
                        rs.getString("branch_name"),
                        rs.getString("title"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name"),
                        rs.getDate("date_of_birth"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getString("physical_address"),
                        rs.getString("province"),
                        rs.getString("postal_address"),
                        rs.getString("id_type"),
                        rs.getString("id_number"),
                        rs.getString("employer_name"),
                        rs.getString("employee_number"),
                        rs.getString("job_title"),
                        rs.getObject("monthly_income"),
                        rs.getString("employment_status"),
                        rs.getString("gender"),
                        rs.getString("marital_status")
                    );
                }
            }
        }
        return null;
    }
    
    private void updateUIWithClientData(ClientData data) {
        if (data == null) return;
        
        setComboBoxValue(branchCombo, data.branchName);
        setComboBoxValue(titleCombo, data.title);
        firstNameField.setText(data.firstName);
        middleNameField.setText(data.middleName);
        lastNameField.setText(data.lastName);
        
        if (data.dateOfBirth != null) {
            dobField.setText(data.dateOfBirth.toString());
        }
        
        phoneField.setText(data.phoneNumber);
        emailField.setText(data.email);
        physicalAddressField.setText(data.physicalAddress);
        provinceField.setText(data.province);
        postalAddressField.setText(data.postalAddress);
        setComboBoxValue(idTypeCombo, data.idType);
        idNumberField.setText(data.idNumber);
        employerNameField.setText(data.employerName);
        employeeNumberField.setText(data.employeeNumber);
        jobTitleField.setText(data.jobTitle);
        
        if (data.monthlyIncome != null) {
            monthlyIncomeField.setText(String.valueOf(data.monthlyIncome));
        }
        
        setComboBoxValue(employmentStatusCombo, data.employmentStatus);
        setComboBoxValue(genderCombo, data.gender);
        setComboBoxValue(maritalStatusCombo, data.maritalStatus);
    }
    
    private KinData loadNextOfKinDetails(Connection conn) throws SQLException {
        String sql = "SELECT * FROM next_of_kin WHERE client_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new KinData(
                        rs.getString("name"),
                        rs.getString("relationship"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("id_number")
                    );
                }
            }
        }
        return null;
    }
    
    private void updateUIWithKinData(KinData data) {
        if (data == null) return;
        
        kinNameField.setText(data.name);
        setComboBoxValue(kinRelationshipCombo, data.relationship);
        kinPhoneField.setText(data.phone);
        kinAddressField.setText(data.address);
        kinIdField.setText(data.idNumber);
    }
    
    private BankData loadBankDetails(Connection conn) throws SQLException {
        String sql = "SELECT * FROM bank_details WHERE client_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new BankData(
                        rs.getString("bank_name"),
                        rs.getString("account_number"),
                        rs.getString("account_name"),
                        rs.getString("branch_code"),
                        rs.getString("branch_name")
                    );
                }
            }
        }
        return null;
    }
    
    private void updateUIWithBankData(BankData data) {
        if (data == null) return;
        
        bankNameField.setText(data.bankName);
        accountNumberField.setText(data.accountNumber);
        accountNameField.setText(data.accountName);
        branchCodeField.setText(data.branchCode);
        branchNameField.setText(data.branchName);
    }
    
    private void setComboBoxValue(JComboBox<String> combo, String value) {
        if (value != null) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (combo.getItemAt(i).equalsIgnoreCase(value)) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
        }
    }
    
    private void saveClientAsync() {
        if (!validateForm()) return;
        
        setLoading(true);
        
        AsyncDatabaseService.executeAsync(() -> {
            Connection conn = null;
            try {
                conn = DatabaseConnection.getConnection();
                conn.setAutoCommit(false);
                
                // Update client details
                updateClientDetails(conn);
                
                // Update next of kin
                updateNextOfKinDetails(conn);
                
                // Update bank details
                updateBankDetails(conn);
                
                // Log the action
                logAuditAction(conn, "Updated client ID: " + clientId);
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) {}
                }
                throw new RuntimeException("Failed to save client data: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try { 
                        conn.setAutoCommit(true);
                        conn.close(); 
                    } catch (SQLException e) {}
                }
            }
        },
        success -> {
            SwingUtilities.invokeLater(() -> {
                setLoading(false);
                if ((Boolean) success) {
                    JOptionPane.showMessageDialog(EditClientScreen.this,
                        "Client updated successfully!", "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    goBack();
                } else {
                    showError("Save Error", "Failed to save client data");
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                setLoading(false);
                showError("Save Error", "Failed to complete save operation: " + e.getMessage());
            });
        });
    }
    
    private void updateClientDetails(Connection conn) throws SQLException {
        String sql = "UPDATE clients SET branch_id=?, title=?, first_name=?, middle_name=?, " +
                   "last_name=?, date_of_birth=?, phone_number=?, email=?, physical_address=?, " +
                   "province=?, postal_address=?, id_type=?, id_number=?, " +
                   "employer_name=?, employee_number=?, job_title=?, monthly_income=?, " +
                   "employment_status=?, gender=?, marital_status=?, updated_at=CURRENT_TIMESTAMP " +
                   "WHERE client_id=?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            
            // Get branch ID from branch name
            int branchId = getBranchId(conn, (String) branchCombo.getSelectedItem());
            stmt.setInt(paramIndex++, branchId);
            
            stmt.setString(paramIndex++, (String) titleCombo.getSelectedItem());
            stmt.setString(paramIndex++, firstNameField.getText().trim());
            stmt.setString(paramIndex++, middleNameField.getText().trim());
            stmt.setString(paramIndex++, lastNameField.getText().trim());
            stmt.setDate(paramIndex++, java.sql.Date.valueOf(dobField.getText().trim()));
            stmt.setString(paramIndex++, phoneField.getText().trim());
            stmt.setString(paramIndex++, emailField.getText().trim());
            stmt.setString(paramIndex++, physicalAddressField.getText().trim());
            stmt.setString(paramIndex++, provinceField.getText().trim());
            stmt.setString(paramIndex++, postalAddressField.getText().trim());
            stmt.setString(paramIndex++, (String) idTypeCombo.getSelectedItem());
            stmt.setString(paramIndex++, idNumberField.getText().trim());
            stmt.setString(paramIndex++, employerNameField.getText().trim());
            stmt.setString(paramIndex++, employeeNumberField.getText().trim());
            stmt.setString(paramIndex++, jobTitleField.getText().trim());
            
            if (monthlyIncomeField.getText().trim().isEmpty()) {
                stmt.setNull(paramIndex++, Types.DECIMAL);
            } else {
                stmt.setDouble(paramIndex++, Double.parseDouble(monthlyIncomeField.getText().trim()));
            }
            
            stmt.setString(paramIndex++, (String) employmentStatusCombo.getSelectedItem());
            stmt.setString(paramIndex++, (String) genderCombo.getSelectedItem());
            stmt.setString(paramIndex++, (String) maritalStatusCombo.getSelectedItem());
            stmt.setInt(paramIndex, clientId);
            
            stmt.executeUpdate();
        }
    }
    
    private int getBranchId(Connection conn, String branchName) throws SQLException {
        String sql = "SELECT branch_id FROM branches WHERE branch_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, branchName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("branch_id");
                }
            }
        }
        return 1; // Default to first branch
    }
    
    private void updateNextOfKinDetails(Connection conn) throws SQLException {
        // First delete existing records
        String deleteSql = "DELETE FROM next_of_kin WHERE client_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, clientId);
            stmt.executeUpdate();
        }
        
        // Then insert new record if we have data
        if (!kinNameField.getText().trim().isEmpty()) {
            String insertSql = "INSERT INTO next_of_kin (client_id, name, relationship, phone, address, id_number) " +
                             "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, clientId);
                stmt.setString(2, kinNameField.getText().trim());
                stmt.setString(3, (String) kinRelationshipCombo.getSelectedItem());
                stmt.setString(4, kinPhoneField.getText().trim());
                stmt.setString(5, kinAddressField.getText().trim());
                stmt.setString(6, kinIdField.getText().trim());
                
                stmt.executeUpdate();
            }
        }
    }
    
    private void updateBankDetails(Connection conn) throws SQLException {
        // First delete existing records
        String deleteSql = "DELETE FROM bank_details WHERE client_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, clientId);
            stmt.executeUpdate();
        }
        
        // Then insert new record if we have data
        if (!bankNameField.getText().trim().isEmpty() && !accountNumberField.getText().trim().isEmpty()) {
            String insertSql = "INSERT INTO bank_details (client_id, bank_name, account_number, account_name, branch_code, branch_name) " +
                             "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, clientId);
                stmt.setString(2, bankNameField.getText().trim());
                stmt.setString(3, accountNumberField.getText().trim());
                stmt.setString(4, accountNameField.getText().trim());
                stmt.setString(5, branchCodeField.getText().trim());
                stmt.setString(6, branchNameField.getText().trim());
                
                stmt.executeUpdate();
            }
        }
    }
    
    private void logAuditAction(Connection conn, String action) throws SQLException {
        String sql = "INSERT INTO audit_logs (employee_id, employee_name, action, details) " +
                    "SELECT ?, name, 'Client Update', ? FROM employees WHERE employee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            stmt.setString(2, action);
            stmt.setInt(3, currentUserId);
            stmt.executeUpdate();
        }
    }
    
    private boolean validateForm() {
        // Check required fields
        if (firstNameField.getText().trim().isEmpty()) {
            showValidationError("First Name is required");
            return false;
        }
        
        if (lastNameField.getText().trim().isEmpty()) {
            showValidationError("Last Name is required");
            return false;
        }
        
        if (phoneField.getText().trim().isEmpty()) {
            showValidationError("Phone Number is required");
            return false;
        }
        
        if (physicalAddressField.getText().trim().isEmpty()) {
            showValidationError("Physical Address is required");
            return false;
        }
        
        if (provinceField.getText().trim().isEmpty()) {
            showValidationError("Province is required");
            return false;
        }
        
        if (idNumberField.getText().trim().isEmpty()) {
            showValidationError("ID Number is required");
            return false;
        }
        
        // Validate date format
        if (!dobField.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            showValidationError("Invalid date format. Use YYYY-MM-DD");
            return false;
        }
        
        return true;
    }
    
    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }
    
    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void goBack() {
        SwingUtilities.invokeLater(() -> {
            ScreenManager.getInstance().showScreen(new ClientsScreen(currentUserId, currentUserRole));
        });
    }
    
    // Data classes to store loaded data
    private static class LoadResult {
        boolean success;
        String message;
        ClientData clientData;
        KinData kinData;
        BankData bankData;
        
        LoadResult(boolean success, String message, ClientData clientData, KinData kinData, BankData bankData) {
            this.success = success;
            this.message = message;
            this.clientData = clientData;
            this.kinData = kinData;
            this.bankData = bankData;
        }
    }
    
    private static class ClientData {
        String branchName;
        String title;
        String firstName;
        String middleName;
        String lastName;
        Date dateOfBirth;
        String phoneNumber;
        String email;
        String physicalAddress;
        String province;
        String postalAddress;
        String idType;
        String idNumber;
        String employerName;
        String employeeNumber;
        String jobTitle;
        Object monthlyIncome;
        String employmentStatus;
        String gender;
        String maritalStatus;
        
        ClientData(String branchName, String title, String firstName, String middleName, String lastName, 
                  Date dateOfBirth, String phoneNumber, String email, String physicalAddress, 
                  String province, String postalAddress, String idType, String idNumber, 
                  String employerName, String employeeNumber, String jobTitle, Object monthlyIncome,
                  String employmentStatus, String gender, String maritalStatus) {
            this.branchName = branchName;
            this.title = title;
            this.firstName = firstName;
            this.middleName = middleName;
            this.lastName = lastName;
            this.dateOfBirth = dateOfBirth;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.physicalAddress = physicalAddress;
            this.province = province;
            this.postalAddress = postalAddress;
            this.idType = idType;
            this.idNumber = idNumber;
            this.employerName = employerName;
            this.employeeNumber = employeeNumber;
            this.jobTitle = jobTitle;
            this.monthlyIncome = monthlyIncome;
            this.employmentStatus = employmentStatus;
            this.gender = gender;
            this.maritalStatus = maritalStatus;
        }
    }
    
    private static class KinData {
        String name;
        String relationship;
        String phone;
        String address;
        String idNumber;
        
        KinData(String name, String relationship, String phone, String address, String idNumber) {
            this.name = name;
            this.relationship = relationship;
            this.phone = phone;
            this.address = address;
            this.idNumber = idNumber;
        }
    }
    
    private static class BankData {
        String bankName;
        String accountNumber;
        String accountName;
        String branchCode;
        String branchName;
        
        BankData(String bankName, String accountNumber, String accountName, String branchCode, String branchName) {
            this.bankName = bankName;
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.branchCode = branchCode;
            this.branchName = branchName;
        }
    }
}