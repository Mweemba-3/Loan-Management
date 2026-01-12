import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class EMICalculatorDialog extends JDialog {
    private JTextField principalField, interestField, termField;
    private JComboBox<String> methodCombo, installmentCombo;
    private JTextArea resultArea;
    
    public EMICalculatorDialog(Frame parent) {
        super(parent, "EMI Calculator", true);
        initUI();
        setLocationRelativeTo(parent);
    }
    
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 600);
        setResizable(false);
        
        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        inputPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        inputPanel.setBackground(Color.WHITE);
        
        // Principal Amount
        JLabel principalLabel = new JLabel("Principal Amount (ZMW):");
        principalLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(principalLabel);
        
        principalField = new JTextField();
        principalField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        principalField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        inputPanel.add(principalField);
        
        // Interest Rate
        JLabel interestLabel = new JLabel("Annual Interest Rate (%):");
        interestLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(interestLabel);
        
        interestField = new JTextField();
        interestField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        interestField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        inputPanel.add(interestField);
        
        // Loan Term
        JLabel termLabel = new JLabel("Loan Term:");
        termLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(termLabel);
        
        termField = new JTextField();
        termField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        termField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        inputPanel.add(termField);
        
        // Calculation Method
        JLabel methodLabel = new JLabel("Calculation Method:");
        methodLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(methodLabel);
        
        methodCombo = new JComboBox<>(new String[]{"FLAT", "REDUCING"});
        methodCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        methodCombo.setBackground(Color.WHITE);
        inputPanel.add(methodCombo);
        
        // Installment Type
        JLabel installmentLabel = new JLabel("Installment Type:");
        installmentLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(installmentLabel);
        
        installmentCombo = new JComboBox<>(new String[]{"Weekly", "Monthly", "Quarterly", "Annually"});
        installmentCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        installmentCombo.setBackground(Color.WHITE);
        inputPanel.add(installmentCombo);
        
        add(inputPanel, BorderLayout.NORTH);
        
        // Calculate Button
        JButton calculateBtn = new JButton("Calculate EMI");
        calculateBtn.setBackground(new Color(0, 150, 150));
        calculateBtn.setForeground(Color.WHITE);
        calculateBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        calculateBtn.setFocusPainted(false);
        calculateBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        calculateBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calculateBtn.addActionListener(e -> calculateEMI());
        
        // Button hover effects
        calculateBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                calculateBtn.setBackground(new Color(0, 170, 170));
            }
            public void mouseExited(MouseEvent e) {
                calculateBtn.setBackground(new Color(0, 150, 150));
            }
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        buttonPanel.add(calculateBtn);
        add(buttonPanel, BorderLayout.CENTER);
        
        // Results Area
        resultArea = new JTextArea(15, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setBackground(new Color(248, 248, 248));
        resultArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(new EmptyBorder(10, 20, 20, 20));
        add(scrollPane, BorderLayout.SOUTH);
    }
    
    private void calculateEMI() {
        try {
            double principal = Double.parseDouble(principalField.getText());
            double annualRate = Double.parseDouble(interestField.getText());
            int term = Integer.parseInt(termField.getText());
            String method = (String) methodCombo.getSelectedItem();
            String installmentType = (String) installmentCombo.getSelectedItem();
            
            if (principal <= 0 || annualRate <= 0 || term <= 0) {
                JOptionPane.showMessageDialog(this, "Please enter positive values for all fields", 
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Call PostgreSQL function for calculation
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement stmt = conn.prepareCall("{ ? = call calculate_emi(?, ?, ?, ?, ?) }")) {
                
                stmt.registerOutParameter(1, Types.OTHER);
                stmt.setDouble(2, principal);
                stmt.setDouble(3, annualRate);
                stmt.setInt(4, term);
                stmt.setString(5, installmentType);
                stmt.setString(6, method);
                
                stmt.execute();
                
                ResultSet rs = (ResultSet) stmt.getObject(1);
                if (rs.next()) {
                    double emi = rs.getDouble("emi");
                    double totalInterest = rs.getDouble("total_interest");
                    double totalAmount = rs.getDouble("total_amount");
                    
                    displayResults(principal, annualRate, term, installmentType, method, 
                                 emi, totalInterest, totalAmount);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values", 
                "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), 
                "Calculation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), 
                "Calculation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void displayResults(double principal, double rate, int term, String installmentType, 
                              String method, double emi, double totalInterest, double totalAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("EMI CALCULATION RESULTS\n");
        sb.append("=======================\n\n");
        sb.append(String.format("Principal Amount:  ZMW %,.2f%n", principal));
        sb.append(String.format("Interest Rate:     %.2f%% %s%n", rate, method));
        sb.append(String.format("Loan Term:         %d %s%n", term, installmentType.toLowerCase()));
        sb.append(String.format("Calculation:       %s Balance%n%n", method));
        
        sb.append("PAYMENT SUMMARY:\n");
        sb.append("----------------\n");
        sb.append(String.format("EMI Amount:        ZMW %,.2f%n", emi));
        sb.append(String.format("Total Interest:    ZMW %,.2f%n", totalInterest));
        sb.append(String.format("Total Payment:     ZMW %,.2f%n", totalAmount));
        sb.append(String.format("Interest %%:        %.2f%%%n%n", (totalInterest/principal)*100));
        
        // Payment schedule preview
        sb.append("PAYMENT SCHEDULE (First 12 installments):\n");
        sb.append("-----------------------------------------\n");
        
        double balance = totalAmount;
        int numPayments = getNumberOfPayments(term, installmentType);
        
        for (int i = 1; i <= Math.min(12, numPayments); i++) {
            double interestPortion = balance * (rate/100) / getPeriodsPerYear(installmentType);
            double principalPortion = emi - interestPortion;
            balance -= emi;
            
            if (balance < 0) balance = 0;
            
            sb.append(String.format("Installment %2d:   EMI: ZMW %,.2f (Principal: ZMW %,.2f, Interest: ZMW %,.2f)%n",
                                  i, emi, principalPortion, interestPortion));
        }
        
        if (numPayments > 12) {
            sb.append(String.format("... and %d more installments%n", numPayments - 12));
        }
        
        resultArea.setText(sb.toString());
    }
    
    private int getNumberOfPayments(int term, String installmentType) {
        switch (installmentType) {
            case "Weekly": return term <= 4 ? 1 : term;
            case "Monthly": return term == 1 ? 1 : term;
            case "Quarterly": return (int) Math.ceil(term / 3.0);
            case "Annually": return (int) Math.ceil(term / 12.0);
            default: return term;
        }
    }
    
    private int getPeriodsPerYear(String installmentType) {
        switch (installmentType) {
            case "Weekly": return 52;
            case "Monthly": return 12;
            case "Quarterly": return 4;
            case "Annually": return 1;
            default: return 12;
        }
    }
}