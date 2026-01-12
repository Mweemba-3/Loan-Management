import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AddClientScreen extends JPanel {
    private int userId;
    private String userRole;
    
    // Form fields
    private JTextField firstNameField, middleNameField, lastNameField, phoneField, emailField;
    private JTextField physicalAddressField, postalAddressField, idNumberField, idPlaceField;
    private JTextField employerNameField, employeeNumberField, jobTitleField, monthlyIncomeField;
    private JTextField dobField, idTypeField;
    private JComboBox<String> titleCombo, genderCombo, maritalStatusCombo, employmentStatusCombo;
    private JComboBox<String> provinceCombo, branchCombo;
    
    // Next of Kin fields
    private JTextField kinNameField, kinPhoneField, kinAddressField, kinIdField;
    private JComboBox<String> kinRelationshipCombo;
    
    // Bank details fields
    private JTextField bankNameField, accountNumberField, accountNameField, branchCodeField, branchNameField;
    private JComboBox<String> accountTypeCombo;
    
    private JButton saveButton, cancelButton;
    
    public AddClientScreen(int userId, String userRole) {
        this.userId = userId;
        this.userRole = userRole;
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(245, 245, 245));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JLabel titleLabel = new JLabel("ADD NEW CLIENT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(50, 50, 50));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Main form with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        tabbedPane.addTab("Personal Info", createScrollablePanel(createPersonalInfoPanel()));
        tabbedPane.addTab("Next of Kin", createScrollablePanel(createNextOfKinPanel()));
        tabbedPane.addTab("Bank Details", createScrollablePanel(createBankDetailsPanel()));
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));
        
        saveButton = new JButton("💾 Save Client");
        cancelButton = new JButton("❌ Cancel");
        
        styleButton(saveButton, new Color(39, 174, 96));
        styleButton(cancelButton, new Color(231, 76, 60));
        
        saveButton.addActionListener(e -> saveClient());
        cancelButton.addActionListener(e -> goBack());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JScrollPane createScrollablePanel(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
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
    
    private JPanel createPersonalInfoPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // 1. Personal Information Section
        JPanel personalSection = createSectionPanel("Personal Information");
        personalSection.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        personalSection.add(new JLabel("Branch:*"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        String[] branchNames = {"Lusaka Main", "Kitwe Copperbelt", "Ndola Central", "Livingstone South", "Chipata East"};
        branchCombo = new JComboBox<>(branchNames);
        personalSection.add(branchCombo, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        personalSection.add(new JLabel("Title:*"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        titleCombo = new JComboBox<>(new String[]{"Mr", "Ms", "Mrs", "Dr", "Prof", "Other"});
        personalSection.add(titleCombo, gbc);
        
        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        personalSection.add(new JLabel("First Name:*"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        firstNameField = createTextField(15);
        personalSection.add(firstNameField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        personalSection.add(new JLabel("Middle Name:"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        middleNameField = createTextField(15);
        personalSection.add(middleNameField, gbc);
        
        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        personalSection.add(new JLabel("Last Name:*"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        lastNameField = createTextField(15);
        personalSection.add(lastNameField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        personalSection.add(new JLabel("Date of Birth:*"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        dobField = createTextField(10);
        dobField.setToolTipText("YYYY-MM-DD");
        personalSection.add(dobField, gbc);
        
        // Row 3
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        personalSection.add(new JLabel("Gender:*"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        personalSection.add(genderCombo, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        personalSection.add(new JLabel("Marital Status:*"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        maritalStatusCombo = new JComboBox<>(new String[]{"Single", "Married", "Divorced", "Widowed"});
        personalSection.add(maritalStatusCombo, gbc);
        
        mainPanel.add(personalSection);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // 2. Contact Information Section
        JPanel contactSection = createSectionPanel("Contact Information");
        contactSection.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        contactSection.add(new JLabel("Phone:*"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.7;
        phoneField = createTextField(20);
        contactSection.add(phoneField, gbc);
        
        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        contactSection.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.7;
        emailField = createTextField(20);
        contactSection.add(emailField, gbc);
        
        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0.3;
        contactSection.add(new JLabel("Physical Address:*"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.7;
        physicalAddressField = createTextField(25);
        contactSection.add(physicalAddressField, gbc);
        
        // Row 3
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.3;
        contactSection.add(new JLabel("Province:*"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        String[] zambianProvinces = {
            "Lusaka Province", "Copperbelt Province", "Southern Province", 
            "Northern Province", "Eastern Province", "Western Province", 
            "Luapula Province", "North-Western Province", "Muchinga Province"
        };
        provinceCombo = new JComboBox<>(zambianProvinces);
        contactSection.add(provinceCombo, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        contactSection.add(new JLabel("Postal Address:"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        postalAddressField = createTextField(15);
        contactSection.add(postalAddressField, gbc);
        
        mainPanel.add(contactSection);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // 3. Identification Section
        JPanel idSection = createSectionPanel("Identification");
        idSection.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        idSection.add(new JLabel("ID Type:*"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        idTypeField = createTextField(15);
        idSection.add(idTypeField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        idSection.add(new JLabel("ID Number:*"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        idNumberField = createTextField(15);
        idSection.add(idNumberField, gbc);
        
        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        idSection.add(new JLabel("ID Place:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        idPlaceField = createTextField(10);
        idPlaceField.setText("GRZ");
        idSection.add(idPlaceField, gbc);
        
        mainPanel.add(idSection);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // 4. Employment Information Section
        JPanel employmentSection = createSectionPanel("Employment Information");
        employmentSection.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        employmentSection.add(new JLabel("Employment Status:*"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        employmentStatusCombo = new JComboBox<>(new String[]{"Employed", "Self-Employed", "Unemployed"});
        employmentSection.add(employmentStatusCombo, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        employmentSection.add(new JLabel("Employer Name:"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        employerNameField = createTextField(15);
        employmentSection.add(employerNameField, gbc);
        
        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        employmentSection.add(new JLabel("Employee Number:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        employeeNumberField = createTextField(12);
        employmentSection.add(employeeNumberField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        employmentSection.add(new JLabel("Job Title:"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        jobTitleField = createTextField(15);
        employmentSection.add(jobTitleField, gbc);
        
        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        employmentSection.add(new JLabel("Monthly Income:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        monthlyIncomeField = createTextField(12);
        employmentSection.add(monthlyIncomeField, gbc);
        
        mainPanel.add(employmentSection);
        
        // Add padding at bottom
        mainPanel.add(Box.createVerticalGlue());
        
        return mainPanel;
    }
    
    private JPanel createNextOfKinPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel kinSection = createSectionPanel("Next of Kin Information");
        kinSection.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        kinSection.add(new JLabel("Name:*"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.7;
        kinNameField = createTextField(25);
        kinSection.add(kinNameField, gbc);
        
        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        kinSection.add(new JLabel("Relationship:*"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        kinRelationshipCombo = new JComboBox<>(new String[]{"Spouse", "Parent", "Sibling", "Child", "Other"});
        kinSection.add(kinRelationshipCombo, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        kinSection.add(new JLabel("Phone:*"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        kinPhoneField = createTextField(15);
        kinSection.add(kinPhoneField, gbc);
        
        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        kinSection.add(new JLabel("Address:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.7;
        kinAddressField = createTextField(25);
        kinSection.add(kinAddressField, gbc);
        
        // Row 3
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.3;
        kinSection.add(new JLabel("ID Number:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        kinIdField = createTextField(15);
        kinSection.add(kinIdField, gbc);
        
        mainPanel.add(kinSection);
        mainPanel.add(Box.createVerticalGlue());
        
        return mainPanel;
    }
    
    private JPanel createBankDetailsPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel bankSection = createSectionPanel("Bank Account Details");
        bankSection.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        bankSection.add(new JLabel("Bank Name:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.7;
        bankNameField = createTextField(25);
        bankSection.add(bankNameField, gbc);
        
        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        bankSection.add(new JLabel("Account Number:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        accountNumberField = createTextField(15);
        bankSection.add(accountNumberField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        bankSection.add(new JLabel("Account Type:"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        accountTypeCombo = new JComboBox<>(new String[]{"Savings", "Current", "Mobile Money", "Other"});
        bankSection.add(accountTypeCombo, gbc);
        
        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0.3;
        bankSection.add(new JLabel("Account Name:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.7;
        accountNameField = createTextField(25);
        bankSection.add(accountNameField, gbc);
        
        // Row 3
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.3;
        bankSection.add(new JLabel("Branch Code:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        branchCodeField = createTextField(10);
        bankSection.add(branchCodeField, gbc);
        
        gbc.gridx = 2; gbc.weightx = 0.3;
        bankSection.add(new JLabel("Branch Name:"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.7;
        branchNameField = createTextField(15);
        bankSection.add(branchNameField, gbc);
        
        mainPanel.add(bankSection);
        mainPanel.add(Box.createVerticalGlue());
        
        return mainPanel;
    }
    
    private JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setMaximumSize(new Dimension(200, 30));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return field;
    }
    
    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            title
        );
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
        border.setTitleColor(new Color(70, 70, 70));
        panel.setBorder(BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        return panel;
    }
    
    private boolean validateForm() {
        if (firstNameField.getText().trim().isEmpty()) {
            showError("First Name is required", firstNameField);
            return false;
        }
        
        if (lastNameField.getText().trim().isEmpty()) {
            showError("Last Name is required", lastNameField);
            return false;
        }
        
        if (dobField.getText().trim().isEmpty()) {
            showError("Date of Birth is required (Format: YYYY-MM-DD)", dobField);
            return false;
        }
        
        if (!dobField.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            showError("Invalid date format. Use YYYY-MM-DD", dobField);
            return false;
        }
        
        if (phoneField.getText().trim().isEmpty()) {
            showError("Phone Number is required", phoneField);
            return false;
        }
        
        if (!phoneField.getText().trim().matches("\\d{10,15}")) {
            showError("Phone must be 10-15 digits", phoneField);
            return false;
        }
        
        if (physicalAddressField.getText().trim().isEmpty()) {
            showError("Physical Address is required", physicalAddressField);
            return false;
        }
        
        if (idTypeField.getText().trim().isEmpty()) {
            showError("ID Type is required", idTypeField);
            return false;
        }
        
        if (idNumberField.getText().trim().isEmpty()) {
            showError("ID Number is required", idNumberField);
            return false;
        }
        
        if (kinNameField.getText().trim().isEmpty()) {
            showError("Next of Kin Name is required", kinNameField);
            return false;
        }
        
        if (kinPhoneField.getText().trim().isEmpty()) {
            showError("Next of Kin Phone is required", kinPhoneField);
            return false;
        }
        
        return true;
    }
    
    private void showError(String message, JComponent field) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);
        field.requestFocus();
    }
    
    private void saveClient() {
        if (!validateForm()) {
            return;
        }
        
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        saveButton.setText("Saving...");
        
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = DatabaseConnection.getConnection();
                conn.setAutoCommit(false);
                
                int branchId = getBranchId(conn, (String) branchCombo.getSelectedItem());
                int clientId = insertClient(conn, branchId);
                if (clientId == -1) {
                    conn.rollback();
                    SwingUtilities.invokeLater(() -> {
                        saveButton.setEnabled(true);
                        cancelButton.setEnabled(true);
                        saveButton.setText("💾 Save Client");
                        JOptionPane.showMessageDialog(this, "Failed to save client.", "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return;
                }
                
                if (!insertNextOfKin(conn, clientId)) {
                    conn.rollback();
                    SwingUtilities.invokeLater(() -> {
                        saveButton.setEnabled(true);
                        cancelButton.setEnabled(true);
                        saveButton.setText("💾 Save Client");
                        JOptionPane.showMessageDialog(this, "Failed to save next of kin.", "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return;
                }
                
                insertBankDetails(conn, clientId);
                
                conn.commit();
                
                SwingUtilities.invokeLater(() -> {
                    saveButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    saveButton.setText("💾 Save Client");
                    
                    String clientName = firstNameField.getText() + " " + lastNameField.getText();
                    JOptionPane.showMessageDialog(this, "Client saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearForm();
                });
                
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        rollbackEx.printStackTrace();
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    saveButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    saveButton.setText("💾 Save Client");
                    JOptionPane.showMessageDialog(AddClientScreen.this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException closeEx) {
                        closeEx.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private int insertClient(Connection conn, int branchId) throws SQLException {
        String sql = "INSERT INTO clients (branch_id, title, first_name, middle_name, last_name, " +
                     "date_of_birth, gender, marital_status, phone_number, email, physical_address, " +
                     "province, postal_address, id_type, id_number, id_place, employment_status, " +
                     "employer_name, employee_number, job_title, monthly_income, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?::date, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;
            stmt.setInt(paramIndex++, branchId);
            stmt.setString(paramIndex++, (String) titleCombo.getSelectedItem());
            stmt.setString(paramIndex++, firstNameField.getText().trim());
            stmt.setString(paramIndex++, middleNameField.getText().trim());
            stmt.setString(paramIndex++, lastNameField.getText().trim());
            stmt.setDate(paramIndex++, java.sql.Date.valueOf(dobField.getText().trim()));
            stmt.setString(paramIndex++, (String) genderCombo.getSelectedItem());
            stmt.setString(paramIndex++, (String) maritalStatusCombo.getSelectedItem());
            stmt.setString(paramIndex++, phoneField.getText().trim());
            stmt.setString(paramIndex++, emailField.getText().trim());
            stmt.setString(paramIndex++, physicalAddressField.getText().trim());
            stmt.setString(paramIndex++, (String) provinceCombo.getSelectedItem());
            stmt.setString(paramIndex++, postalAddressField.getText().trim());
            stmt.setString(paramIndex++, idTypeField.getText().trim());
            stmt.setString(paramIndex++, idNumberField.getText().trim());
            stmt.setString(paramIndex++, idPlaceField.getText().trim());
            stmt.setString(paramIndex++, (String) employmentStatusCombo.getSelectedItem());
            stmt.setString(paramIndex++, employerNameField.getText().trim());
            stmt.setString(paramIndex++, employeeNumberField.getText().trim());
            stmt.setString(paramIndex++, jobTitleField.getText().trim());
            
            String incomeStr = monthlyIncomeField.getText().trim();
            if (incomeStr.isEmpty()) {
                stmt.setDouble(paramIndex++, 0.00);
            } else {
                try {
                    stmt.setDouble(paramIndex++, Double.parseDouble(incomeStr));
                } catch (NumberFormatException e) {
                    stmt.setDouble(paramIndex++, 0.00);
                }
            }
            
            stmt.setInt(paramIndex++, userId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating client failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating client failed, no ID obtained.");
                }
            }
        }
    }

    private boolean insertNextOfKin(Connection conn, int clientId) throws SQLException {
        String sql = "INSERT INTO next_of_kin (client_id, name, relationship, phone, address, id_number) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            stmt.setString(2, kinNameField.getText().trim());
            stmt.setString(3, (String) kinRelationshipCombo.getSelectedItem());
            stmt.setString(4, kinPhoneField.getText().trim());
            stmt.setString(5, kinAddressField.getText().trim());
            stmt.setString(6, kinIdField.getText().trim());
            
            return stmt.executeUpdate() > 0;
        }
    }

    private void insertBankDetails(Connection conn, int clientId) throws SQLException {
        String bankName = bankNameField.getText().trim();
        String accountNumber = accountNumberField.getText().trim();
        String accountName = accountNameField.getText().trim();
        
        if (bankName.isEmpty() || accountNumber.isEmpty() || accountName.isEmpty()) {
            return;
        }
        
        String sql = "INSERT INTO bank_details (client_id, bank_name, account_number, account_name, " +
                     "branch_code, branch_name, account_type) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            stmt.setString(2, bankName);
            stmt.setString(3, accountNumber);
            stmt.setString(4, accountName);
            stmt.setString(5, branchCodeField.getText().trim());
            stmt.setString(6, branchNameField.getText().trim());
            stmt.setString(7, (String) accountTypeCombo.getSelectedItem());
            
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
        return 1;
    }

    private void clearForm() {
        branchCombo.setSelectedIndex(0);
        titleCombo.setSelectedIndex(0);
        firstNameField.setText("");
        middleNameField.setText("");
        lastNameField.setText("");
        dobField.setText("");
        genderCombo.setSelectedIndex(0);
        maritalStatusCombo.setSelectedIndex(0);
        phoneField.setText("");
        emailField.setText("");
        physicalAddressField.setText("");
        provinceCombo.setSelectedIndex(0);
        postalAddressField.setText("");
        idTypeField.setText("");
        idNumberField.setText("");
        idPlaceField.setText("GRZ");
        employmentStatusCombo.setSelectedIndex(0);
        employerNameField.setText("");
        employeeNumberField.setText("");
        jobTitleField.setText("");
        monthlyIncomeField.setText("");
        kinNameField.setText("");
        kinRelationshipCombo.setSelectedIndex(0);
        kinPhoneField.setText("");
        kinAddressField.setText("");
        kinIdField.setText("");
        bankNameField.setText("");
        accountNumberField.setText("");
        accountNameField.setText("");
        branchCodeField.setText("");
        branchNameField.setText("");
        accountTypeCombo.setSelectedIndex(0);
    }

    private void goBack() {
        SwingUtilities.invokeLater(() -> {
            ScreenManager.getInstance().showScreen(new ClientsScreen(userId, userRole));
        });
    }
}