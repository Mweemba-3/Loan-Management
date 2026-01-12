import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;

public class ApplyLoanScreen extends JPanel {
    private int employeeId;
    private String userRole;
    private JTextField searchField;
    private JTextField clientNameField;
    private JTextField phoneField;
    private JTextField amountField;
    private JTextField emailField;
    private JComboBox<String> productComboBox;
    private JTextField interestRateField;
    private JComboBox<String> calculationMethodComboBox;
    private JTextField loanTermField;
    private JComboBox<String> installmentTypeComboBox;
    private JComboBox<String> loanFeeComboBox;
    private JComboBox<String> category1ComboBox;
    private JComboBox<String> category2ComboBox;
    private JTextArea collateralArea;
    private JTextArea guarantorsArea;
    private JTextArea calculationDetailsArea;
    private JLabel clientStatusLabel;
    
    private Integer selectedClientId = null;
    private Integer selectedProductId = null;
    private Map<String, Integer> clientSearchResults = new HashMap<>();
    private JDialog searchResultsDialog;
    
    private JLabel loanTermLabel;
    private String currentTermUnit = "Months";
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
    
    // UI Components
    private JButton searchBtn, clearBtn, calculateBtn, submitBtn, backBtn;
    private JPanel loadingPanel;
    private JLabel loadingLabel;
    
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

    public ApplyLoanScreen(int employeeId, String userRole) {
        this.employeeId = employeeId;
        this.userRole = userRole;
        initUI();
        loadLoanProductsAsync();
        AsyncDatabaseService.logAsync(employeeId, "Loan Application", "Accessed apply loan screen");
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BG_COLOR);

        JPanel clientSearchPanel = createClientSearchPanel();
        mainPanel.add(clientSearchPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabbedPane.addTab("📝 Loan Details", createLoanDetailsPanel());
        tabbedPane.addTab("🏷️ Collateral", createCollateralPanel());
        tabbedPane.addTab("🤝 Guarantors", createGuarantorsPanel());
        tabbedPane.addTab("🧮 Calculation", createCalculationPanel());
        
        // Loading overlay for tabs
        JPanel tabContainer = new JPanel(new BorderLayout());
        loadingPanel = createLoadingPanel();
        tabContainer.setLayout(new OverlayLayout(tabContainer));
        tabContainer.add(loadingPanel);
        tabContainer.add(tabbedPane);
        
        mainPanel.add(tabContainer, BorderLayout.CENTER);

        JPanel buttonsPanel = createButtonsPanel();
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        
        // Initial calculation
        calculateLoanAsync();
    }
    
    private JPanel createLoadingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(255, 255, 255, 200));
        panel.setVisible(false);
        
        loadingLabel = new JLabel("Processing...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loadingLabel.setForeground(DARK_TEXT);
        panel.add(loadingLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void showLoading(boolean show, String message) {
        SwingUtilities.invokeLater(() -> {
            loadingPanel.setVisible(show);
            loadingLabel.setText(message);
            setComponentsEnabled(!show);
        });
    }
    
    private void setComponentsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            searchBtn.setEnabled(enabled);
            clearBtn.setEnabled(enabled);
            calculateBtn.setEnabled(enabled);
            submitBtn.setEnabled(enabled);
            backBtn.setEnabled(enabled);
            searchField.setEnabled(enabled);
            amountField.setEnabled(enabled);
            interestRateField.setEnabled(enabled);
            loanTermField.setEnabled(enabled);
            productComboBox.setEnabled(enabled);
            calculationMethodComboBox.setEnabled(enabled);
            installmentTypeComboBox.setEnabled(enabled);
            loanFeeComboBox.setEnabled(enabled);
            category1ComboBox.setEnabled(enabled);
            category2ComboBox.setEnabled(enabled);
            collateralArea.setEnabled(enabled);
            guarantorsArea.setEnabled(enabled);
        });
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        headerPanel.setBackground(BG_COLOR);

        JLabel titleLabel = new JLabel("APPLY FOR LOAN");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(DARK_TEXT);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        backBtn = new JButton("⬅️ Back to Loans");
        styleButton(backBtn, LIGHT_TEXT, true);
        backBtn.addActionListener(e -> goBackToLoans());
        headerPanel.add(backBtn, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createClientSearchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("🔍 Client Search"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(CARD_COLOR);
        panel.setPreferredSize(new Dimension(800, 160));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;
        
        // Search row
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createFormLabel("Search Client (Name/Phone/ID):"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        searchField = createFormTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Enter name, phone number, or ID...");
        panel.add(searchField, gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        searchBtn = new JButton("Search");
        styleButton(searchBtn, PRIMARY_COLOR, true);
        searchBtn.addActionListener(e -> searchClientsAsync());
        panel.add(searchBtn, gbc);
        
        // Client details row
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        panel.add(createFormLabel("Client Name:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        clientNameField = createFormTextField();
        clientNameField.setEditable(false);
        panel.add(clientNameField, gbc);
        
        // Phone and email row
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0.3;
        panel.add(createFormLabel("Phone:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 0.7;
        phoneField = createFormTextField();
        phoneField.setEditable(false);
        panel.add(phoneField, gbc);
        
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0.3;
        panel.add(createFormLabel("Email:"), gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1; gbc.weightx = 0.7;
        emailField = createFormTextField();
        emailField.setEditable(false);
        panel.add(emailField, gbc);
        
        // Client status row (NEW)
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.3;
        panel.add(createFormLabel("Loan Status:"), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        clientStatusLabel = new JLabel("No client selected");
        clientStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clientStatusLabel.setForeground(LIGHT_TEXT);
        panel.add(clientStatusLabel, gbc);
        
        return panel;
    }
    
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(DARK_TEXT);
        return label;
    }
    
    private JTextField createFormTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }

    private JPanel createLoanDetailsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(CARD_COLOR);
        
        // Create a scrollable panel for the form
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(CARD_COLOR);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Loan Product - This should be visible now
        JPanel productPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        productPanel.setBackground(CARD_COLOR);
        productPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        JLabel productLabel = new JLabel("Loan Product (Optional):");
        productLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        productLabel.setForeground(DARK_TEXT);
        productLabel.setPreferredSize(new Dimension(180, 30));
        
        productComboBox = new JComboBox<>();
        productComboBox.addItem("Select Product");
        productComboBox.setPreferredSize(new Dimension(300, 35));
        productComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productComboBox.setBackground(Color.WHITE);
        productComboBox.addActionListener(e -> autoFillFromProductAsync());
        
        productPanel.add(productLabel);
        productPanel.add(productComboBox);
        formPanel.add(productPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Create a 2-column grid for the rest of the fields
        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setBackground(CARD_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;
        
        int row = 0;
        
        // Interest Rate
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        gridPanel.add(createFormLabel("Interest Rate (%):"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        interestRateField = createFormTextField();
        interestRateField.setText("15.00");
        interestRateField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { calculateLoanAsync(); }
            public void removeUpdate(DocumentEvent e) { calculateLoanAsync(); }
            public void insertUpdate(DocumentEvent e) { calculateLoanAsync(); }
        });
        gridPanel.add(interestRateField, gbc);
        
        row++;
        
        // Calculation Method
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        gridPanel.add(createFormLabel("Calculation Method:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        calculationMethodComboBox = new JComboBox<>(new String[]{"FLAT", "REDUCING"});
        calculationMethodComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        calculationMethodComboBox.setBackground(Color.WHITE);
        calculationMethodComboBox.addActionListener(e -> calculateLoanAsync());
        gridPanel.add(calculationMethodComboBox, gbc);
        
        row++;
        
        // Loan Term
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        loanTermLabel = createFormLabel("Loan Term (Months):");
        gridPanel.add(loanTermLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        loanTermField = createFormTextField();
        loanTermField.setText("12");
        loanTermField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { calculateLoanAsync(); }
            public void removeUpdate(DocumentEvent e) { calculateLoanAsync(); }
            public void insertUpdate(DocumentEvent e) { calculateLoanAsync(); }
        });
        gridPanel.add(loanTermField, gbc);
        
        row++;
        
        // Amount
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        gridPanel.add(createFormLabel("Amount (ZMW):"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        amountField = createFormTextField();
        amountField.setText("10000.00");
        amountField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { calculateLoanAsync(); }
            public void removeUpdate(DocumentEvent e) { calculateLoanAsync(); }
            public void insertUpdate(DocumentEvent e) { calculateLoanAsync(); }
        });
        gridPanel.add(amountField, gbc);
        
        row++;
        
        // Installment Type
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        gridPanel.add(createFormLabel("Installment Type:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        installmentTypeComboBox = new JComboBox<>(new String[]{"Weekly", "Monthly", "Quarterly", "Annually"});
        installmentTypeComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        installmentTypeComboBox.setBackground(Color.WHITE);
        installmentTypeComboBox.addActionListener(e -> updateTermLabel());
        installmentTypeComboBox.addActionListener(e -> calculateLoanAsync());
        gridPanel.add(installmentTypeComboBox, gbc);
        
        row++;
        
        // Loan Fee Type
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        gridPanel.add(createFormLabel("Loan Fee Type:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        loanFeeComboBox = new JComboBox<>(new String[]{"Cash", "Mobile", "Bank"});
        loanFeeComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loanFeeComboBox.setBackground(Color.WHITE);
        gridPanel.add(loanFeeComboBox, gbc);
        
        row++;
        
        // Category 1
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        gridPanel.add(createFormLabel("Category 1:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        category1ComboBox = new JComboBox<>(new String[]{"Personal", "Business", "Education", "Agricultural"});
        category1ComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        category1ComboBox.setBackground(Color.WHITE);
        gridPanel.add(category1ComboBox, gbc);
        
        row++;
        
        // Category 2
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.4;
        gridPanel.add(createFormLabel("Category 2:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.6;
        category2ComboBox = new JComboBox<>(new String[]{"Short-Term", "Long-Term", "Microloan"});
        category2ComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        category2ComboBox.setBackground(Color.WHITE);
        gridPanel.add(category2ComboBox, gbc);
        
        // Add the grid to the form
        formPanel.add(gridPanel);
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        return mainPanel;
    }

    private JPanel createCollateralPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(CARD_COLOR);

        JLabel titleLabel = new JLabel("🏷️ Collateral Details (Optional)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(DARK_TEXT);
        panel.add(titleLabel, BorderLayout.NORTH);

        collateralArea = new JTextArea(8, 50);
        collateralArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        collateralArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        collateralArea.setText("Example format:\n• Car - ZMW 50,000.00\n• House - ZMW 150,000.00\n• Equipment - ZMW 25,000.00");

        JScrollPane scrollPane = new JScrollPane(collateralArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGuarantorsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(CARD_COLOR);

        JLabel titleLabel = new JLabel("🤝 Guarantors (Optional)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(DARK_TEXT);
        panel.add(titleLabel, BorderLayout.NORTH);

        guarantorsArea = new JTextArea(8, 50);
        guarantorsArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        guarantorsArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        guarantorsArea.setText("Example format:\n• Mweemba Obvious - 0971234567 - Friend - ZMW 10,000.00\n• Orleans Mayaya - 0967654321 - Relative - ZMW 15,000.00");

        JScrollPane scrollPane = new JScrollPane(guarantorsArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCalculationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(CARD_COLOR);

        JLabel titleLabel = new JLabel("🧮 Loan Calculation Details");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(DARK_TEXT);
        panel.add(titleLabel, BorderLayout.NORTH);

        calculationDetailsArea = new JTextArea(12, 50);
        calculationDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        calculationDetailsArea.setEditable(false);
        calculationDetailsArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        calculationDetailsArea.setText("Enter loan details to see calculation results...");

        JScrollPane scrollPane = new JScrollPane(calculationDetailsArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        clearBtn = new JButton("🗑️ Clear Form");
        styleButton(clearBtn, WARNING_COLOR, true);
        clearBtn.addActionListener(e -> clearForm());

        calculateBtn = new JButton("🧮 Calculate");
        styleButton(calculateBtn, INFO_COLOR, true);
        calculateBtn.addActionListener(e -> calculateLoanAsync());

        submitBtn = new JButton("✅ Submit Application");
        styleButton(submitBtn, SUCCESS_COLOR, true);
        submitBtn.addActionListener(e -> submitApplicationAsync());

        panel.add(clearBtn);
        panel.add(calculateBtn);
        panel.add(submitBtn);

        return panel;
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
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void loadLoanProductsAsync() {
        showLoading(true, "Loading loan products...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT product_id, product_name FROM loan_products WHERE is_active = true ORDER BY product_name";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        Map<Integer, String> products = new HashMap<>();
                        while (rs.next()) {
                            products.put(rs.getInt("product_id"), rs.getString("product_name"));
                        }
                        return products;
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Error loading loan products: " + ex.getMessage());
            }
        },
        products -> {
            SwingUtilities.invokeLater(() -> {
                productComboBox.removeAllItems();
                productComboBox.addItem("Select Product");
                
                for (String productName : ((Map<Integer, String>) products).values()) {
                    productComboBox.addItem(productName);
                }
                showLoading(false, "");
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false, "");
                showError("Failed to load loan products: " + e.getMessage());
            });
        });
    }

    private void searchClientsAsync() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            showError("Please enter a search term");
            return;
        }

        showLoading(true, "Searching clients...");
        clientSearchResults.clear();
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT client_id, title, first_name, middle_name, last_name, phone_number, email " +
                           "FROM clients WHERE is_active = true AND " +
                           "(first_name ILIKE ? OR last_name ILIKE ? OR phone_number ILIKE ? OR id_number ILIKE ? OR email ILIKE ?) " +
                           "ORDER BY first_name, last_name LIMIT 20";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String likeTerm = "%" + searchTerm + "%";
                    for (int i = 1; i <= 5; i++) {
                        stmt.setString(i, likeTerm);
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        java.util.List<Object[]> results = new java.util.ArrayList<>();
                        while (rs.next()) {
                            int clientId = rs.getInt("client_id");
                            String title = rs.getString("title");
                            String firstName = rs.getString("first_name");
                            String middleName = rs.getString("middle_name");
                            String lastName = rs.getString("last_name");
                            String phone = rs.getString("phone_number");
                            String email = rs.getString("email");
                            
                            String fullName = title + " " + firstName + 
                                (middleName != null ? " " + middleName : "") + 
                                " " + lastName;
                            
                            // Check client loan status
                            String loanStatus = checkClientLoanStatus(clientId);
                            
                            results.add(new Object[]{clientId, fullName, phone, email, loanStatus});
                            clientSearchResults.put(fullName + " - " + phone, clientId);
                        }
                        return results.toArray(new Object[0][0]);
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Error searching clients: " + ex.getMessage());
            }
        },
        results -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false, "");
                if (((Object[][]) results).length == 0) {
                    showError("No clients found matching: " + searchTerm);
                } else {
                    showSearchResultsDialog((Object[][]) results);
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false, "");
                showError("Search failed: " + e.getMessage());
            });
        });
    }

    private String checkClientLoanStatus(int clientId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT status FROM loans WHERE client_id = ? AND status IN ('Approved', 'Pending', 'Due') ORDER BY loan_id DESC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, clientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("status");
                    }
                }
            }
            return "No Active Loan";
        } catch (SQLException ex) {
            return "Error";
        }
    }

    private void showSearchResultsDialog(Object[][] results) {
        searchResultsDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Select Client", true);
        searchResultsDialog.setLayout(new BorderLayout());
        searchResultsDialog.setSize(750, 400);
        searchResultsDialog.setLocationRelativeTo(this);

        String[] columnNames = {"ID", "Full Name", "Phone", "Email", "Loan Status"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable resultsTable = new JTable(model);
        resultsTable.setRowHeight(30);
        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Custom cell renderer for loan status
        resultsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else {
                    switch (status) {
                        case "Approved":
                            c.setForeground(new Color(39, 174, 96)); // Green
                            break;
                        case "Pending":
                            c.setForeground(new Color(241, 196, 15)); // Yellow
                            break;
                        case "Due":
                            c.setForeground(new Color(231, 76, 60)); // Red
                            break;
                        case "No Active Loan":
                            c.setForeground(new Color(52, 152, 219)); // Blue
                            break;
                        default:
                            c.setForeground(Color.BLACK);
                    }
                    c.setBackground(table.getBackground());
                }
                
                return c;
            }
        });

        for (Object[] row : results) {
            model.addRow(row);
        }

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && resultsTable.getSelectedRow() != -1) {
                int selectedRow = resultsTable.getSelectedRow();
                int clientId = (Integer) resultsTable.getValueAt(selectedRow, 0);
                String fullName = (String) resultsTable.getValueAt(selectedRow, 1);
                String phone = (String) resultsTable.getValueAt(selectedRow, 2);
                String email = (String) resultsTable.getValueAt(selectedRow, 3);
                String loanStatus = (String) resultsTable.getValueAt(selectedRow, 4);
                
                // Check if client can apply for new loan
                if (loanStatus.equals("Approved") || loanStatus.equals("Pending") || loanStatus.equals("Due")) {
                    JOptionPane.showMessageDialog(searchResultsDialog,
                        "<html><b>Cannot Select Client</b><br><br>" +
                        "Client <b>" + fullName + "</b> has an <b>" + loanStatus + "</b> loan.<br>" +
                        "Only clients with closed loans or no active loans can apply for new loans.</html>",
                        "Client Has Active Loan",
                        JOptionPane.WARNING_MESSAGE);
                    resultsTable.clearSelection();
                } else {
                    loadClientDetails(clientId, fullName, phone, email, loanStatus);
                    searchResultsDialog.dispose();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        searchResultsDialog.add(scrollPane, BorderLayout.CENTER);

        JButton cancelBtn = new JButton("Cancel");
        styleButton(cancelBtn, LIGHT_TEXT, true);
        cancelBtn.addActionListener(e -> searchResultsDialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(BG_COLOR);
        buttonPanel.add(cancelBtn);
        searchResultsDialog.add(buttonPanel, BorderLayout.SOUTH);

        searchResultsDialog.setVisible(true);
    }

    private void loadClientDetails(int clientId, String fullName, String phone, String email, String loanStatus) {
        selectedClientId = clientId;
        clientNameField.setText(fullName);
        phoneField.setText(phone);
        emailField.setText(email != null ? email : "Not provided");
        
        // Update client status label
        SwingUtilities.invokeLater(() -> {
            if (loanStatus.equals("No Active Loan") || loanStatus.equals("Closed") || loanStatus.equals("Error")) {
                clientStatusLabel.setText("✅ Eligible for new loan");
                clientStatusLabel.setForeground(SUCCESS_COLOR);
            } else {
                clientStatusLabel.setText("❌ Has active loan: " + loanStatus);
                clientStatusLabel.setForeground(DANGER_COLOR);
            }
        });
    }

    private void updateTermLabel() {
        String installmentType = (String) installmentTypeComboBox.getSelectedItem();
        switch (installmentType) {
            case "Weekly":
                loanTermLabel.setText("Loan Term (Weeks):");
                currentTermUnit = "Weeks";
                break;
            case "Monthly":
                loanTermLabel.setText("Loan Term (Months):");
                currentTermUnit = "Months";
                break;
            case "Quarterly":
                loanTermLabel.setText("Loan Term (Quarters):");
                currentTermUnit = "Quarters";
                break;
            case "Annually":
                loanTermLabel.setText("Loan Term (Years):");
                currentTermUnit = "Years";
                break;
        }
    }

    private void autoFillFromProductAsync() {
        String selectedProduct = (String) productComboBox.getSelectedItem();
        if (selectedProduct == null || selectedProduct.equals("Select Product")) {
            selectedProductId = null;
            setProductFieldsEnabled(true);
            return;
        }
        
        showLoading(true, "Loading product details...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT product_id, interest_rate, calculation_method, installment_type, " +
                           "loan_fee_type, category1, category2 FROM loan_products WHERE product_name = ? AND is_active = true";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, selectedProduct);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return new Object[]{
                                rs.getInt("product_id"),
                                rs.getDouble("interest_rate"),
                                rs.getString("calculation_method"),
                                rs.getString("installment_type"),
                                rs.getString("loan_fee_type"),
                                rs.getString("category1"),
                                rs.getString("category2")
                            };
                        }
                        return null;
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Error loading product details: " + ex.getMessage());
            }
        },
        productDetails -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false, "");
                if (productDetails != null) {
                    Object[] details = (Object[]) productDetails;
                    selectedProductId = (Integer) details[0];
                    setProductFieldsEnabled(false);
                    
                    interestRateField.setText(String.valueOf((Double) details[1]));
                    calculationMethodComboBox.setSelectedItem((String) details[2]);
                    installmentTypeComboBox.setSelectedItem((String) details[3]);
                    loanFeeComboBox.setSelectedItem((String) details[4]);
                    category1ComboBox.setSelectedItem((String) details[5]);
                    category2ComboBox.setSelectedItem((String) details[6]);
                    
                    calculateLoanAsync();
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false, "");
                showError("Failed to load product details: " + e.getMessage());
            });
        });
    }

    private void setProductFieldsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            interestRateField.setEnabled(enabled);
            calculationMethodComboBox.setEnabled(enabled);
            installmentTypeComboBox.setEnabled(enabled);
            loanFeeComboBox.setEnabled(enabled);
            category1ComboBox.setEnabled(enabled);
            category2ComboBox.setEnabled(enabled);
            
            Color bgColor = enabled ? Color.WHITE : new Color(240, 240, 240);
            interestRateField.setBackground(bgColor);
        });
    }

    private void calculateLoanAsync() {
        AsyncDatabaseService.executeAsync(() -> {
            try {
                return calculateLoanDetails();
            } catch (Exception e) {
                throw new RuntimeException("Calculation error: " + e.getMessage());
            }
        },
        result -> {
            if (result != null) {
                displayCalculationResults((Object[]) result);
            }
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                calculationDetailsArea.setText("Error in calculation: " + e.getMessage());
            });
        });
    }

    private Object[] calculateLoanDetails() {
        try {
            if (amountField.getText().trim().isEmpty() || interestRateField.getText().trim().isEmpty() || 
                loanTermField.getText().trim().isEmpty()) {
                return null;
            }

            double principal = Double.parseDouble(amountField.getText());
            double annualInterestRate = Double.parseDouble(interestRateField.getText());
            int loanTerm = Integer.parseInt(loanTermField.getText());
            String calculationMethod = (String) calculationMethodComboBox.getSelectedItem();
            String installmentType = (String) installmentTypeComboBox.getSelectedItem();

            boolean isSinglePayment = isSinglePaymentLoan(installmentType, loanTerm);

            double totalInterest = 0;
            double totalAmount = 0;
            double installmentAmount = 0;
            int numberOfInstallments = 1;

            if ("FLAT".equals(calculationMethod)) {
                totalInterest = principal * (annualInterestRate / 100);
                totalAmount = principal + totalInterest;
                
                if (isSinglePayment) {
                    installmentAmount = totalAmount;
                    numberOfInstallments = 1;
                } else {
                    numberOfInstallments = getNumberOfInstallments(installmentType, loanTerm);
                    installmentAmount = totalAmount / numberOfInstallments;
                }
                
            } else {
                if (isSinglePayment) {
                    double periodicRate = getPeriodicInterestRate(installmentType, annualInterestRate);
                    totalInterest = principal * periodicRate * loanTerm;
                    totalAmount = principal + totalInterest;
                    installmentAmount = totalAmount;
                    numberOfInstallments = 1;
                } else {
                    double periodicRate = getPeriodicInterestRate(installmentType, annualInterestRate);
                    numberOfInstallments = getNumberOfInstallments(installmentType, loanTerm);
                    
                    double power = Math.pow(1 + periodicRate, numberOfInstallments);
                    installmentAmount = principal * periodicRate * power / (power - 1);
                    totalAmount = installmentAmount * numberOfInstallments;
                    totalInterest = totalAmount - principal;
                }
            }

            installmentAmount = Math.round(installmentAmount * 100.0) / 100.0;
            totalAmount = Math.round(totalAmount * 100.0) / 100.0;
            totalInterest = Math.round(totalInterest * 100.0) / 100.0;

            String dueDate = calculateDueDate(installmentType, loanTerm);

            return new Object[]{
                principal, annualInterestRate, loanTerm, calculationMethod, 
                installmentType, installmentAmount, totalInterest, totalAmount, 
                numberOfInstallments, isSinglePayment, dueDate
            };
            
        } catch (NumberFormatException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Calculation error: " + e.getMessage());
        }
    }

    private boolean isSinglePaymentLoan(String installmentType, int loanTerm) {
        return (installmentType.equals("Weekly") && loanTerm <= 3) || 
               (installmentType.equals("Monthly") && loanTerm == 1);
    }

    private int getNumberOfInstallments(String installmentType, int loanTerm) {
        if (isSinglePaymentLoan(installmentType, loanTerm)) {
            return 1;
        }
        return loanTerm;
    }

    private double getPeriodicInterestRate(String installmentType, double annualRate) {
        switch (installmentType) {
            case "Weekly": return (annualRate / 100) / 52;
            case "Monthly": return (annualRate / 100) / 12;
            case "Quarterly": return (annualRate / 100) / 4;
            case "Annually": return (annualRate / 100);
            default: return (annualRate / 100) / 12;
        }
    }

    private String calculateDueDate(String installmentType, int loanTerm) {
        try {
            Calendar cal = Calendar.getInstance();
            
            switch (installmentType) {
                case "Weekly":
                    cal.add(Calendar.WEEK_OF_YEAR, loanTerm);
                    break;
                case "Monthly":
                    cal.add(Calendar.MONTH, loanTerm);
                    break;
                case "Quarterly":
                    cal.add(Calendar.MONTH, loanTerm * 3);
                    break;
                case "Annually":
                    cal.add(Calendar.YEAR, loanTerm);
                    break;
                default:
                    cal.add(Calendar.MONTH, loanTerm);
            }
            
            return dateFormat.format(cal.getTime());
        } catch (Exception e) {
            return "Error calculating due date";
        }
    }

    private void displayCalculationResults(Object[] results) {
        if (results == null) {
            SwingUtilities.invokeLater(() -> {
                calculationDetailsArea.setText("Please fill in amount, interest rate, and loan term to calculate.");
            });
            return;
        }
        
        double principal = (Double) results[0];
        double annualRate = (Double) results[1];
        int loanTerm = (Integer) results[2];
        String method = (String) results[3];
        String installmentType = (String) results[4];
        double installment = (Double) results[5];
        double totalInterest = (Double) results[6];
        double totalAmount = (Double) results[7];
        int numInstallments = (Integer) results[8];
        boolean isSinglePayment = (Boolean) results[9];
        String dueDate = (String) results[10];
        
        StringBuilder details = new StringBuilder();
        details.append("LOAN CALCULATION DETAILS\n");
        details.append("═══════════════════════════════════════════════════════\n");
        details.append(String.format("Principal Amount:    ZMW %s%n", currencyFormat.format(principal)));
        details.append(String.format("Interest Rate:       %.2f%% %s%n", annualRate, method));
        details.append(String.format("Term:                %d %s%n", loanTerm, currentTermUnit.toLowerCase()));
        details.append(String.format("Installment Type:    %s%n", installmentType));
        details.append(String.format("Due Date:            %s%n", dueDate));
        details.append("───────────────────────────────────────────────────────\n");
        
        if (isSinglePayment) {
            details.append("PAYMENT TYPE: SINGLE PAYMENT\n");
            details.append("───────────────────────────────────────────────────────\n");
        }
        
        if ("FLAT".equals(method)) {
            details.append("INTEREST METHOD: FLAT RATE\n");
            details.append("(Full annual interest charged regardless of term)\n");
            details.append("───────────────────────────────────────────────────────\n");
        } else {
            details.append("INTEREST METHOD: REDUCING BALANCE\n");
            details.append("(Interest pro-rated based on actual time period)\n");
            details.append("───────────────────────────────────────────────────────\n");
        }
        
        details.append(String.format("Installment Amount:  ZMW %s%n", currencyFormat.format(installment)));
        details.append(String.format("Total Interest:      ZMW %s%n", currencyFormat.format(totalInterest)));
        details.append(String.format("Total Repayment:     ZMW %s%n", currencyFormat.format(totalAmount)));
        details.append(String.format("Number of Payments:  %d%n", numInstallments));
        
        if (!isSinglePayment) {
            details.append(String.format("Payment Frequency:   %s%n", installmentType.toLowerCase()));
        }
        
        details.append("═══════════════════════════════════════════════════════\n");

        SwingUtilities.invokeLater(() -> {
            calculationDetailsArea.setText(details.toString());
        });
    }

    private void submitApplicationAsync() {
        if (selectedClientId == null) {
            showError("Please select a client first");
            return;
        }

        if (!validateForm()) {
            return;
        }

        // Check client loan status before submission
        String clientLoanStatus = checkClientLoanStatus(selectedClientId);
        if (clientLoanStatus.equals("Approved") || clientLoanStatus.equals("Pending") || clientLoanStatus.equals("Due")) {
            showError("Client has an " + clientLoanStatus + " loan. Only clients with closed loans or no active loans can apply.");
            return;
        }

        // Get the selected product name
        final String selectedProductName = (String) productComboBox.getSelectedItem();
        String clientName = clientNameField.getText();
        double amount = Double.parseDouble(amountField.getText());
        double interestRate = Double.parseDouble(interestRateField.getText());
        int term = Integer.parseInt(loanTermField.getText());
        String installmentType = (String) installmentTypeComboBox.getSelectedItem();
        
        String productInfo = selectedProductName != null && !selectedProductName.equals("Select Product") ? 
            "Product: <b>" + selectedProductName + "</b><br>" : "";
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "<html><b>SUBMIT LOAN APPLICATION</b><br><br>" +
            "Client: <b>" + clientName + "</b><br>" +
            productInfo +
            "Amount: <b>ZMW " + currencyFormat.format(amount) + "</b><br>" +
            "Interest Rate: <b>" + interestRate + "%</b><br>" +
            "Term: <b>" + term + " " + currentTermUnit.toLowerCase() + "</b><br>" +
            "Installment Type: <b>" + installmentType + "</b><br><br>" +
            "Are you sure you want to submit this application?</html>",
            "Confirm Submission",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        showLoading(true, "Submitting loan application...");
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                
                try {
                    // Double-check client loan status before inserting
                    String checkStatusSQL = "SELECT COUNT(*) FROM loans WHERE client_id = ? AND status IN ('Approved', 'Pending', 'Due')";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkStatusSQL)) {
                        checkStmt.setInt(1, selectedClientId);
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                throw new RuntimeException("Client already has an active loan. Cannot apply for new loan.");
                            }
                        }
                    }
                    
                    // Generate loan number
                    String loanNumber = generateLoanNumber(conn);
                    
                    // Get loan calculation details
                    Object[] calcDetails = calculateLoanDetails();
                    if (calcDetails == null) {
                        throw new RuntimeException("Could not calculate loan details");
                    }
                    
                    double principal = (Double) calcDetails[0];
                    double annualRate = (Double) calcDetails[1];
                    int loanTerm = (Integer) calcDetails[2];
                    String method = (String) calcDetails[3];
                    String installmentTypeCalc = (String) calcDetails[4];
                    double installmentAmount = (Double) calcDetails[5];
                    double totalInterest = (Double) calcDetails[6];
                    double totalAmount = (Double) calcDetails[7];
                    
                    // Calculate due date
                    Calendar cal = Calendar.getInstance();
                    switch (installmentTypeCalc) {
                        case "Weekly": cal.add(Calendar.WEEK_OF_YEAR, loanTerm); break;
                        case "Monthly": cal.add(Calendar.MONTH, loanTerm); break;
                        case "Quarterly": cal.add(Calendar.MONTH, loanTerm * 3); break;
                        case "Annually": cal.add(Calendar.YEAR, loanTerm); break;
                    }
                    Date dueDate = cal.getTime();
                    
                    // Get branch ID for the client
                    int branchId = getClientBranchId(conn, selectedClientId);
                    
                    // Insert loan record
                    String insertLoanSQL = "INSERT INTO loans (" +
                        "loan_number, client_id, product_id, branch_id, " +
                        "amount, interest_rate, calculation_method, loan_term, " +
                        "installment_type, loan_fee_type, category1, category2, " +
                        "total_amount, interest_amount, installment_amount, " +
                        "outstanding_balance, status, application_date, due_date, " +
                        "collateral_details, guarantors_details, created_by" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(insertLoanSQL)) {
                        stmt.setString(1, loanNumber);
                        stmt.setInt(2, selectedClientId);
                        
                        if (selectedProductId != null) {
                            stmt.setInt(3, selectedProductId);
                        } else {
                            stmt.setNull(3, Types.INTEGER);
                        }
                        
                        stmt.setInt(4, branchId);
                        stmt.setDouble(5, principal);
                        stmt.setDouble(6, annualRate);
                        stmt.setString(7, method);
                        stmt.setInt(8, loanTerm);
                        stmt.setString(9, installmentTypeCalc);
                        stmt.setString(10, (String) loanFeeComboBox.getSelectedItem());
                        stmt.setString(11, (String) category1ComboBox.getSelectedItem());
                        stmt.setString(12, (String) category2ComboBox.getSelectedItem());
                        stmt.setDouble(13, totalAmount);
                        stmt.setDouble(14, totalInterest);
                        stmt.setDouble(15, installmentAmount);
                        stmt.setDouble(16, totalAmount);
                        stmt.setString(17, "Pending");
                        stmt.setTimestamp(18, new Timestamp(System.currentTimeMillis()));
                        stmt.setDate(19, new java.sql.Date(dueDate.getTime()));
                        stmt.setString(20, collateralArea.getText());
                        stmt.setString(21, guarantorsArea.getText());
                        stmt.setInt(22, employeeId);
                        
                        int affectedRows = stmt.executeUpdate();
                        
                        if (affectedRows > 0) {
                            conn.commit();
                            
                            AsyncDatabaseService.logAsync(employeeId, "Loan Application", 
                                "Applied loan " + loanNumber + " for client " + selectedClientId + 
                                " - Amount: ZMW " + currencyFormat.format(principal));
                            
                            return new Object[]{loanNumber, dueDate, totalAmount, installmentAmount};
                        } else {
                            conn.rollback();
                            throw new RuntimeException("Failed to insert loan record");
                        }
                    }
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException ex) {
                if (ex.getMessage().contains("unique constraint") || ex.getMessage().contains("duplicate key")) {
                    throw new RuntimeException("Loan number already exists. Please try again.");
                }
                throw new RuntimeException("Database error: " + ex.getMessage());
            }
        },
        result -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false, "");
                if (result != null) {
                    Object[] details = (Object[]) result;
                    String loanNumber = (String) details[0];
                    Date dueDate = (Date) details[1];
                    double totalAmount = (Double) details[2];
                    double installmentAmount = (Double) details[3];
                    
                    String productInfoDisplay = selectedProductName != null && !selectedProductName.equals("Select Product") ? 
                        "Product: <b>" + selectedProductName + "</b><br>" : "";
                    
                    showSuccess(
                        "<html><b>✅ Loan Application Submitted Successfully!</b><br><br>" +
                        "Loan Number: <b>" + loanNumber + "</b><br>" +
                        "Client: <b>" + clientNameField.getText() + "</b><br>" +
                        productInfoDisplay +
                        "Amount: <b>ZMW " + currencyFormat.format(Double.parseDouble(amountField.getText())) + "</b><br>" +
                        "Total Repayable: <b>ZMW " + currencyFormat.format(totalAmount) + "</b><br>" +
                        "Installment: <b>ZMW " + currencyFormat.format(installmentAmount) + "</b><br>" +
                        "Due Date: <b>" + dateFormat.format(dueDate) + "</b><br><br>" +
                        "Status: <b style='color:orange'>Pending Approval</b></html>"
                    );
                    clearForm();
                }
            });
        },
        e -> {
            SwingUtilities.invokeLater(() -> {
                showLoading(false, "");
                showError("Submission failed: " + e.getMessage());
            });
        });
    }
    
    private String generateLoanNumber(Connection conn) throws SQLException {
        String sql = "SELECT nextval('loan_number_seq')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long nextNum = rs.getLong(1);
                    return "LN" + String.format("%06d", nextNum);
                }
            }
        }
        throw new SQLException("Could not generate loan number");
    }
    
    private int getClientBranchId(Connection conn, int clientId) throws SQLException {
        String sql = "SELECT branch_id FROM clients WHERE client_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("branch_id");
                }
                throw new SQLException("Client not found");
            }
        }
    }

    private boolean validateForm() {
        try {
            if (amountField.getText().trim().isEmpty()) {
                showError("Please enter loan amount");
                amountField.requestFocus();
                return false;
            }
            
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                showError("Loan amount must be greater than 0");
                amountField.requestFocus();
                return false;
            }
            
            if (interestRateField.getText().trim().isEmpty()) {
                showError("Please enter interest rate");
                interestRateField.requestFocus();
                return false;
            }
            
            double interestRate = Double.parseDouble(interestRateField.getText());
            if (interestRate <= 0 || interestRate > 100) {
                showError("Interest rate must be between 0.01% and 100%");
                interestRateField.requestFocus();
                return false;
            }
            
            if (loanTermField.getText().trim().isEmpty()) {
                showError("Please enter loan term");
                loanTermField.requestFocus();
                return false;
            }
            
            int term = Integer.parseInt(loanTermField.getText());
            if (term <= 0) {
                showError("Loan term must be greater than 0");
                loanTermField.requestFocus();
                return false;
            }
            
            return true;

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for amount, interest rate, and loan term");
            return false;
        }
    }

    private void clearForm() {
        SwingUtilities.invokeLater(() -> {
            searchField.setText("");
            clientNameField.setText("");
            phoneField.setText("");
            emailField.setText("");
            clientStatusLabel.setText("No client selected");
            clientStatusLabel.setForeground(LIGHT_TEXT);
            amountField.setText("10000.00");
            interestRateField.setText("15.00");
            loanTermField.setText("12");
            productComboBox.setSelectedIndex(0);
            calculationMethodComboBox.setSelectedIndex(0);
            installmentTypeComboBox.setSelectedIndex(1);
            loanFeeComboBox.setSelectedIndex(0);
            category1ComboBox.setSelectedIndex(0);
            category2ComboBox.setSelectedIndex(0);
            collateralArea.setText("Example format:\n• Car - ZMW 50,000.00\n• House - ZMW 150,000.00\n• Equipment - ZMW 25,000.00");
            guarantorsArea.setText("Example format:\n• John Doe - 0971234567 - Friend - ZMW 10,000.00\n• Jane Smith - 0967654321 - Relative - ZMW 15,000.00");
            calculationDetailsArea.setText("Enter loan details to see calculation results...");
            selectedClientId = null;
            selectedProductId = null;
            setProductFieldsEnabled(true);
            searchField.requestFocus();
        });
    }

    private void goBackToLoans() {
        SwingUtilities.invokeLater(() -> {
            ScreenManager.getInstance().showScreen(new LoansScreen(employeeId, userRole));
        });
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, 
                "<html><b>Error</b><br>" + message + "</html>", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        });
    }

    private void showSuccess(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, 
                message, 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        });
    }
}