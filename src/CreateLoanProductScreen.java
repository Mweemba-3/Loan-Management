import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class CreateLoanProductScreen extends JPanel {
    private int employeeId;
    private String userRole;
    
    private JTextField productNameField;
    private JTextField interestRateField;
    private JComboBox<String> calculationMethodComboBox;
    private JTextField gracePeriodField;
    private JComboBox<String> installmentTypeComboBox;
    private JComboBox<String> loanFeeTypeComboBox;
    private JComboBox<String> category1ComboBox;
    private JComboBox<String> category2ComboBox;
    private JComboBox<String> refinanceComboBox;
    
    // UI Components
    private JButton clearBtn, createBtn, backBtn;
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

    public CreateLoanProductScreen(int employeeId, String userRole) {
        this.employeeId = employeeId;
        this.userRole = userRole;
        initUI();
        AsyncDatabaseService.logAsync(employeeId, "Loan Product", "Accessed create loan product screen");
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main Form Panel with loading overlay
        JPanel formPanel = createFormPanel();
        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setBackground(BG_COLOR);
        formContainer.add(formPanel, BorderLayout.CENTER);
        
        // Loading overlay
        loadingPanel = createLoadingPanel();
        formContainer.setLayout(new OverlayLayout(formContainer));
        formContainer.add(loadingPanel);
        formContainer.add(formPanel);
        
        add(formContainer, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonsPanel = createButtonsPanel();
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        headerPanel.setBackground(BG_COLOR);

        JLabel titleLabel = new JLabel("CREATE LOAN PRODUCT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(DARK_TEXT);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        backBtn = new JButton("⬅️ Back to Loans");
        styleButton(backBtn, LIGHT_TEXT, true);
        backBtn.addActionListener(e -> goBackToLoans());
        headerPanel.add(backBtn, BorderLayout.EAST);

        return headerPanel;
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

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        formPanel.setBackground(CARD_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;
        
        // Product Name
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createFormLabel("Product Name *:"), gbc);
        
        gbc.gridx = 1;
        productNameField = createFormTextField();
        formPanel.add(productNameField, gbc);
        
        // Interest Rate
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(createFormLabel("Interest Rate (%) *:"), gbc);
        
        gbc.gridx = 1;
        interestRateField = createFormTextField();
        interestRateField.putClientProperty("JTextField.placeholderText", "e.g., 15.00");
        formPanel.add(interestRateField, gbc);
        
        // Calculation Method
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(createFormLabel("Calculation Method *:"), gbc);
        
        gbc.gridx = 1;
        calculationMethodComboBox = new JComboBox<>(new String[]{"FLAT", "REDUCING"});
        styleComboBox(calculationMethodComboBox);
        formPanel.add(calculationMethodComboBox, gbc);
        
        // Installment Type
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(createFormLabel("Installment Type *:"), gbc);
        
        gbc.gridx = 1;
        installmentTypeComboBox = new JComboBox<>(new String[]{"Weekly", "Monthly", "Quarterly", "Annually"});
        styleComboBox(installmentTypeComboBox);
        formPanel.add(installmentTypeComboBox, gbc);
        
        // Grace Period
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(createFormLabel("Grace Period (Months):"), gbc);
        
        gbc.gridx = 1;
        gracePeriodField = createFormTextField();
        gracePeriodField.setText("0");
        gracePeriodField.putClientProperty("JTextField.placeholderText", "e.g., 0");
        formPanel.add(gracePeriodField, gbc);
        
        // Loan Fee Type
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(createFormLabel("Loan Fee Type *:"), gbc);
        
        gbc.gridx = 1;
        loanFeeTypeComboBox = new JComboBox<>(new String[]{"Cash", "Mobile", "Bank"});
        styleComboBox(loanFeeTypeComboBox);
        formPanel.add(loanFeeTypeComboBox, gbc);
        
        // Category 1
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(createFormLabel("Category 1 *:"), gbc);
        
        gbc.gridx = 1;
        category1ComboBox = new JComboBox<>(new String[]{"Personal", "Business", "Education", "Agricultural"});
        styleComboBox(category1ComboBox);
        formPanel.add(category1ComboBox, gbc);
        
        // Category 2
        gbc.gridx = 0; gbc.gridy = 7;
        formPanel.add(createFormLabel("Category 2 *:"), gbc);
        
        gbc.gridx = 1;
        category2ComboBox = new JComboBox<>(new String[]{"Short-Term", "Long-Term", "Microloan"});
        styleComboBox(category2ComboBox);
        formPanel.add(category2ComboBox, gbc);
        
        // Refinance
        gbc.gridx = 0; gbc.gridy = 8;
        formPanel.add(createFormLabel("Refinance Allowed:"), gbc);
        
        gbc.gridx = 1;
        refinanceComboBox = new JComboBox<>(new String[]{"No", "Yes"});
        styleComboBox(refinanceComboBox);
        formPanel.add(refinanceComboBox, gbc);

        return formPanel;
    }
    
    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(DARK_TEXT);
        return label;
    }
    
    private JTextField createFormTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonsPanel.setBackground(BG_COLOR);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        clearBtn = new JButton("🗑️ Clear Form");
        styleButton(clearBtn, WARNING_COLOR, true);
        clearBtn.addActionListener(e -> clearForm());

        createBtn = new JButton("✅ Create Product");
        styleButton(createBtn, SUCCESS_COLOR, true);
        createBtn.addActionListener(e -> createLoanProductAsync());

        buttonsPanel.add(clearBtn);
        buttonsPanel.add(createBtn);

        return buttonsPanel;
    }
    
    private void styleButton(JButton button, Color bgColor, boolean isPrimary) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
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
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void createLoanProductAsync() {
        if (!validateForm()) {
            return;
        }

        String productName = productNameField.getText().trim();
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "<html><b>CREATE LOAN PRODUCT</b><br><br>" +
            "Are you sure you want to create this loan product?<br>" +
            "Product: <b>" + productName + "</b></html>", 
            "Confirm Creation", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        showLoading(true);
        
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return false;
                }
                
                String sql = "INSERT INTO loan_products (product_name, product_code, interest_rate, calculation_method, " +
                           "installment_type, grace_period, loan_fee_type, category1, category2, refinance, created_by) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String productCode = generateProductCode(productName);
                    
                    stmt.setString(1, productName);
                    stmt.setString(2, productCode);
                    stmt.setDouble(3, Double.parseDouble(interestRateField.getText()));
                    stmt.setString(4, (String) calculationMethodComboBox.getSelectedItem());
                    stmt.setString(5, (String) installmentTypeComboBox.getSelectedItem());
                    stmt.setInt(6, Integer.parseInt(gracePeriodField.getText()));
                    stmt.setString(7, (String) loanFeeTypeComboBox.getSelectedItem());
                    stmt.setString(8, (String) category1ComboBox.getSelectedItem());
                    stmt.setString(9, (String) category2ComboBox.getSelectedItem());
                    stmt.setBoolean(10, "Yes".equals(refinanceComboBox.getSelectedItem()));
                    stmt.setInt(11, employeeId);
                    
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            } catch (SQLException ex) {
                if (ex.getMessage().contains("duplicate key") || ex.getMessage().contains("unique constraint")) {
                    throw new RuntimeException("A loan product with this name already exists");
                } else {
                    throw new RuntimeException("Database error: " + ex.getMessage());
                }
            }
        },
        success -> {
            showLoading(false);
            if ((Boolean) success) {
                JOptionPane.showMessageDialog(this, 
                    "<html><b>✅ Loan Product Created Successfully!</b><br><br>" +
                    "Product: <b>" + productNameField.getText().trim() + "</b><br>" +
                    "You can now use this product for new loan applications.</html>", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                AsyncDatabaseService.logAsync(employeeId, "Loan Product Created", 
                    "Created loan product: " + productNameField.getText().trim());
                clearForm();
            } else {
                showError("Failed to create loan product");
            }
        },
        e -> {
            showLoading(false);
            showError(e.getMessage());
        });
    }
    
    private String generateProductCode(String productName) {
        String prefix = "";
        String category = (String) category1ComboBox.getSelectedItem();
        
        switch (category) {
            case "Personal": prefix = "PERS"; break;
            case "Business": prefix = "BUS"; break;
            case "Education": prefix = "EDU"; break;
            case "Agricultural": prefix = "AGR"; break;
            default: prefix = "PROD";
        }
        
        String initials = "";
        String[] words = productName.split(" ");
        for (String word : words) {
            if (!word.isEmpty()) {
                initials += Character.toUpperCase(word.charAt(0));
            }
        }
        
        if (initials.length() > 3) {
            initials = initials.substring(0, 3);
        }
        
        return prefix + "-" + initials + "-" + System.currentTimeMillis() % 1000;
    }

    private boolean validateForm() {
        try {
            // Check required fields
            if (productNameField.getText().trim().isEmpty()) {
                showError("Please enter product name");
                productNameField.requestFocus();
                return false;
            }
            
            if (interestRateField.getText().trim().isEmpty()) {
                showError("Please enter interest rate");
                interestRateField.requestFocus();
                return false;
            }
            
            if (gracePeriodField.getText().trim().isEmpty()) {
                showError("Please enter grace period");
                gracePeriodField.requestFocus();
                return false;
            }

            // Validate numeric values
            double interestRate = Double.parseDouble(interestRateField.getText());
            int gracePeriod = Integer.parseInt(gracePeriodField.getText());

            if (interestRate <= 0 || interestRate > 100) {
                showError("Interest rate must be between 0.01% and 100%");
                interestRateField.requestFocus();
                return false;
            }
            
            if (gracePeriod < 0) {
                showError("Grace period cannot be negative");
                gracePeriodField.requestFocus();
                return false;
            }
            
            if (gracePeriod > 36) {
                showError("Grace period cannot exceed 36 months");
                gracePeriodField.requestFocus();
                return false;
            }
            
            // Validate product name length
            String productName = productNameField.getText().trim();
            if (productName.length() > 100) {
                showError("Product name is too long (max 100 characters)");
                productNameField.requestFocus();
                return false;
            }

            return true;

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for interest rate and grace period");
            return false;
        }
    }
    
    private void showLoading(boolean show) {
        SwingUtilities.invokeLater(() -> {
            loadingPanel.setVisible(show);
            setComponentsEnabled(!show);
            
            if (show) {
                loadingLabel.setText("Creating product...");
            }
        });
    }
    
    private void setComponentsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            clearBtn.setEnabled(enabled);
            createBtn.setEnabled(enabled);
            backBtn.setEnabled(enabled);
            productNameField.setEnabled(enabled);
            interestRateField.setEnabled(enabled);
            gracePeriodField.setEnabled(enabled);
            calculationMethodComboBox.setEnabled(enabled);
            installmentTypeComboBox.setEnabled(enabled);
            loanFeeTypeComboBox.setEnabled(enabled);
            category1ComboBox.setEnabled(enabled);
            category2ComboBox.setEnabled(enabled);
            refinanceComboBox.setEnabled(enabled);
        });
    }

    private void clearForm() {
        SwingUtilities.invokeLater(() -> {
            productNameField.setText("");
            interestRateField.setText("");
            gracePeriodField.setText("0");
            calculationMethodComboBox.setSelectedIndex(0);
            installmentTypeComboBox.setSelectedIndex(0);
            loanFeeTypeComboBox.setSelectedIndex(0);
            category1ComboBox.setSelectedIndex(0);
            category2ComboBox.setSelectedIndex(0);
            refinanceComboBox.setSelectedIndex(0);
            productNameField.requestFocus();
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
}