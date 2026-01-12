import javax.print.PrintService;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.print.*;

public class AdminDashboard extends JPanel {
    private int employeeId;
    private String employeeName;
    private DoughnutChart doughnutChart;
    private Timer animationTimer;
    private float animationProgress = 0f;
    
    private MetricCard pendingDisbursementsCard;
    private MetricCard dueLoansCard;
    private MetricCard activeClientsCard;
    private MetricCard totalGivenOutCard;
    private MetricCard totalExpectedCard;
    private MetricCard activeLoansCard;
    
    private JPanel recentActivitiesPanel;
    private JPanel pendingDisbursementsListPanel;
    private JPanel dueLoansListPanel;
    
    private final DecimalFormat currencyFormat = new DecimalFormat("K #,##0.00");
    private final DecimalFormat compactCurrencyFormat = new DecimalFormat("K #,##0");
    private final DecimalFormat numberFormat = new DecimalFormat("#,##0");
    
    private final Color PRIMARY_BLUE = new Color(0, 100, 200);
    private final Color DARK_BG = new Color(25, 30, 40);
    private final Color CARD_BG = new Color(35, 40, 50);
    private final Color SIDEBAR_BG = new Color(30, 35, 45);
    private final Color TEXT_WHITE = Color.WHITE;
    private final Color TEXT_GRAY = new Color(180, 180, 180);
    private final Color RED_ALERT = new Color(220, 80, 80);
    private final Color GREEN_SUCCESS = new Color(80, 180, 80);
    private final Color ORANGE_WARNING = new Color(220, 140, 60);
    private final Color PURPLE = new Color(148, 85, 211);
    
    private JButton backupBtn;
    private JButton refreshBtn;
    private Timer timeoutTimer;
    
    private int completedLoads = 0;
    private final int TOTAL_LOADS = 9;

    public AdminDashboard(int employeeId, String employeeName) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        
        initUI();
        loadDashboardStats();
        startAnimation();
        AsyncDatabaseService.logAsync(employeeId, "Dashboard Access", "Accessed admin dashboard");
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(DARK_BG);
        
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);
        
        JPanel mainContentPanel = createMainContent();
        add(mainContentPanel, BorderLayout.CENTER);
        
        revalidate();
        repaint();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(220, getHeight()));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        
        JLabel logoLabel = new JLabel("MS CODEFORGE LMS");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoLabel.setForeground(PRIMARY_BLUE);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        sidebar.add(logoLabel);
        
        JLabel userLabel = new JLabel(employeeName);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLabel.setForeground(TEXT_WHITE);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        sidebar.add(userLabel);
        
        JLabel roleLabel = new JLabel("Administrator");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roleLabel.setForeground(TEXT_GRAY);
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));
        sidebar.add(roleLabel);
        
        String[] menuIcons = {"👥", "💰", "💳", "📋", "👨‍💼", "🏷️"};
        String[] menuTexts = {"Clients", "Loans", "Payments", "Activities", "Employees", "Logo"};
        
        for (int i = 0; i < menuIcons.length; i++) {
            JLabel menuItem = createMenuItem(menuIcons[i], menuTexts[i]);
            sidebar.add(menuItem);
            sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        
        sidebar.add(Box.createVerticalGlue());
        
        JButton logoutBtn = createLogoutButton();
        sidebar.add(logoutBtn);
        
        return sidebar;
    }

    private JLabel createMenuItem(String icon, String text) {
        JLabel label = new JLabel("<html><table cellpadding='5'><tr>" +
            "<td width='30'>" + icon + "</td>" +
            "<td>" + text + "</td>" +
            "</tr></table></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_GRAY);
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setOpaque(true);
        label.setBackground(SIDEBAR_BG);
        
        label.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                label.setBackground(new Color(40, 45, 55));
                label.setForeground(PRIMARY_BLUE);
            }

            public void mouseExited(MouseEvent evt) {
                label.setBackground(SIDEBAR_BG);
                label.setForeground(TEXT_GRAY);
            }

            public void mouseClicked(MouseEvent evt) {
                handleMenuClick(text);
            }
        });

        return label;
    }

    private JButton createLogoutButton() {
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutBtn.setBackground(new Color(60, 65, 75));
        logoutBtn.setForeground(TEXT_WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 85, 95), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return logoutBtn;
    }

    private void handleMenuClick(String menuText) {
        setLoadingState(true);
        
        AsyncDatabaseService.executeAsync(() -> {
            Thread.sleep(50);
            return menuText;
        },
        result -> {
            setLoadingState(false);
            navigateToScreen(menuText);
        },
        e -> {
            setLoadingState(false);
            showError("Navigation failed: " + e.getMessage());
        });
    }

    private void navigateToScreen(String screenName) {
        SwingUtilities.invokeLater(() -> {
            try {
                JPanel screen = null;
                
                switch (screenName) {
                    case "Clients":
                        screen = new ClientsScreen(employeeId, "admin");
                        break;
                    case "Loans":
                        screen = new LoansScreen(employeeId, "admin");
                        break;
                    case "Payments":
                        screen = new PaymentsScreen(employeeId, "admin");
                        break;
                    case "Activities":
                        screen = new ActivitiesScreen(employeeId, "admin");
                        break;
                    case "Employees":
                        screen = new EmployeesScreen(employeeId, "admin");
                        break;
                    case "Logo":
                        screen = new LogoManagementScreen(employeeId, "admin");
                        break;
                    default:
                        return;
                }
                
                if (screen != null) {
                    ScreenManager.getInstance().showScreen(screen);
                }
            } catch (Exception e) {
                showError("Cannot open " + screenName + ": " + e.getMessage());
            }
        });
    }
    
    private void logout() {
        AsyncDatabaseService.logAsync(employeeId, "Logout", "User logged out from system");
        ScreenManager.getInstance().showScreen(new LoginScreen());
    }

    private JPanel createMainContent() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(DARK_BG);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel headerPanel = createHeaderPanel();
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        
        JPanel dashboardContent = createDashboardContent();
        contentPanel.add(dashboardContent, BorderLayout.CENTER);
        
        return contentPanel;
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DARK_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel titleLabel = new JLabel("Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_WHITE);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(DARK_BG);
        
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        JLabel timeLabel = new JLabel(sdf.format(new Date()));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        timeLabel.setForeground(TEXT_GRAY);
        
        backupBtn = new JButton("📁 Backup PDF");
        styleButton(backupBtn, ORANGE_WARNING);
        backupBtn.addActionListener(e -> createBackupPDF());
        
        refreshBtn = new JButton("⟳ Refresh");
        styleButton(refreshBtn, PRIMARY_BLUE);
        refreshBtn.addActionListener(e -> refreshDashboard());
        
        rightPanel.add(timeLabel);
        rightPanel.add(backupBtn);
        rightPanel.add(refreshBtn);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
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
                button.setBackground(adjustColor(bgColor, 20));
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

    private JPanel createDashboardContent() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(DARK_BG);
        
        JPanel welcomePanel = createWelcomePanel();
        mainPanel.add(welcomePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JPanel row1 = createMetricCardsRow1();
        mainPanel.add(row1);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        JPanel row2 = createMetricCardsRow2();
        mainPanel.add(row2);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPanel middleSection = createMiddleSection();
        mainPanel.add(middleSection);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPanel bottomSection = createBottomSection();
        mainPanel.add(bottomSection);
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(DARK_BG);
        container.add(scrollPane, BorderLayout.CENTER);
        
        return container;
    }

    private JPanel createWelcomePanel() {
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(DARK_BG);
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel welcomeLabel = new JLabel("Welcome back, " + employeeName);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        welcomeLabel.setForeground(TEXT_GRAY);
        
        JLabel dateLabel = new JLabel(new SimpleDateFormat("EEEE, MMMM d, yyyy").format(new Date()));
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateLabel.setForeground(TEXT_GRAY);
        
        welcomePanel.add(welcomeLabel, BorderLayout.WEST);
        welcomePanel.add(dateLabel, BorderLayout.EAST);
        
        return welcomePanel;
    }

    private JPanel createMetricCardsRow1() {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setBackground(DARK_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        pendingDisbursementsCard = new MetricCard("Pending Approvals", "0\nK 0.00", PRIMARY_BLUE);
        dueLoansCard = new MetricCard("Due Loans", "0\nK 0.00", RED_ALERT);
        activeLoansCard = new MetricCard("Active Loans", "0", GREEN_SUCCESS);
        activeClientsCard = new MetricCard("Active Clients", "0", PURPLE);
        
        row.add(pendingDisbursementsCard);
        row.add(dueLoansCard);
        row.add(activeLoansCard);
        row.add(activeClientsCard);
        
        return row;
    }

    private JPanel createMetricCardsRow2() {
        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setBackground(DARK_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        totalGivenOutCard = new MetricCard("Total Disbursed", "K 0.00", PRIMARY_BLUE);
        totalExpectedCard = new MetricCard("Total Expected", "K 0.00", ORANGE_WARNING);
        
        JPanel blankcard = new JPanel();
        blankcard.setBackground(DARK_BG);
        blankcard.setBorder(null);
        
        row.add(totalGivenOutCard);
        row.add(totalExpectedCard);
        row.add(blankcard);
        
        return row;
    }

    private JPanel createMiddleSection() {
        JPanel middlePanel = new JPanel(new GridLayout(1, 2, 15, 0));
        middlePanel.setBackground(DARK_BG);
        middlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        
        middlePanel.add(createChartPanel());
        middlePanel.add(createActivitiesPanel());
        
        return middlePanel;
    }

    private JPanel createChartPanel() {
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBackground(CARD_BG);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 55, 65), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel chartTitle = new JLabel("Portfolio Distribution");
        chartTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chartTitle.setForeground(TEXT_WHITE);
        chartTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        doughnutChart = new DoughnutChart();
        doughnutChart.setPreferredSize(new Dimension(300, 250));
        
        chartPanel.add(chartTitle, BorderLayout.NORTH);
        chartPanel.add(doughnutChart, BorderLayout.CENTER);
        
        return chartPanel;
    }

    private JPanel createActivitiesPanel() {
        JPanel activitiesPanel = new JPanel(new BorderLayout());
        activitiesPanel.setBackground(CARD_BG);
        activitiesPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 55, 65), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel activitiesTitle = new JLabel("Recent Activities");
        activitiesTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        activitiesTitle.setForeground(TEXT_WHITE);
        activitiesTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        recentActivitiesPanel = new JPanel();
        recentActivitiesPanel.setLayout(new BoxLayout(recentActivitiesPanel, BoxLayout.Y_AXIS));
        recentActivitiesPanel.setBackground(CARD_BG);
        
        JLabel loadingLabel = new JLabel("Loading activities...");
        loadingLabel.setForeground(TEXT_GRAY);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        recentActivitiesPanel.add(loadingLabel);
        
        JScrollPane activitiesScroll = new JScrollPane(recentActivitiesPanel);
        activitiesScroll.setBorder(null);
        activitiesScroll.getViewport().setBackground(CARD_BG);
        activitiesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        activitiesPanel.add(activitiesTitle, BorderLayout.NORTH);
        activitiesPanel.add(activitiesScroll, BorderLayout.CENTER);
        
        return activitiesPanel;
    }

    private JPanel createBottomSection() {
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        bottomPanel.setBackground(DARK_BG);
        bottomPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        
        pendingDisbursementsListPanel = createListPanel("Pending Loan Approvals", PRIMARY_BLUE);
        dueLoansListPanel = createListPanel("Due Loans", RED_ALERT);
        
        bottomPanel.add(pendingDisbursementsListPanel);
        bottomPanel.add(dueLoansListPanel);
        
        return bottomPanel;
    }

    private JPanel createListPanel(String title, Color titleColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 55, 65), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(titleColor);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(CARD_BG);

        JLabel loadingLabel = new JLabel("Loading...");
        loadingLabel.setForeground(TEXT_GRAY);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPanel.add(loadingLabel);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(CARD_BG);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadDashboardStats() {
        completedLoads = 0;
        setLoadingState(true);
        
        startTimeoutTimer();
        
        loadPendingDisbursements();
        loadDueLoans();
        loadActiveLoans();
        loadActiveClients();
        loadTotalGivenOut();
        loadTotalExpected();
        loadRecentActivities();
        loadPendingApprovalsList();
        loadDueLoansList();
    }
    
    private void startTimeoutTimer() {
        if (timeoutTimer != null) {
            timeoutTimer.stop();
        }
        
        timeoutTimer = new Timer(10000, e -> {
            setLoadingState(false);
            System.out.println("Dashboard: Loading timeout");
        });
        timeoutTimer.setRepeats(false);
        timeoutTimer.start();
    }
    
    private synchronized void incrementLoadCount() {
        completedLoads++;
        if (completedLoads >= TOTAL_LOADS) {
            setLoadingState(false);
            if (timeoutTimer != null) {
                timeoutTimer.stop();
            }
        }
    }
    
    private void loadPendingDisbursements() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COUNT(*) as count, COALESCE(SUM(amount), 0) as total " +
                              "FROM loans WHERE status = 'Pending'";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return new Object[]{rs.getInt("count"), rs.getDouble("total")};
                        }
                    }
                }
            }
            return new Object[]{0, 0.0};
        }, 
        result -> {
            int count = (int) ((Object[]) result)[0];
            double total = (double) ((Object[]) result)[1];
            pendingDisbursementsCard.setValue(String.format("%s\n%s", 
                numberFormat.format(count), 
                formatCurrency(total)));
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading pending disbursements: " + e.getMessage());
            pendingDisbursementsCard.setValue("0\nK 0.00");
            incrementLoadCount();
        });
    }
    
    private void loadDueLoans() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COUNT(*) as count, COALESCE(SUM(outstanding_balance), 0) as total " +
                              "FROM loans WHERE status = 'Due'";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return new Object[]{rs.getInt("count"), rs.getDouble("total")};
                        }
                    }
                }
            }
            return new Object[]{0, 0.0};
        },
        result -> {
            int count = (int) ((Object[]) result)[0];
            double total = (double) ((Object[]) result)[1];
            dueLoansCard.setValue(String.format("%s\n%s", 
                numberFormat.format(count), 
                formatCurrency(total)));
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading due loans: " + e.getMessage());
            dueLoansCard.setValue("0\nK 0.00");
            incrementLoadCount();
        });
    }
    
    private void loadActiveLoans() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COUNT(*) FROM loans WHERE status IN ('Approved', 'Due')";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getInt(1) : 0;
                    }
                }
            }
        },
        count -> {
            activeLoansCard.setValue(numberFormat.format(count));
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading active loans: " + e.getMessage());
            activeLoansCard.setValue("0");
            incrementLoadCount();
        });
    }
    
    private void loadActiveClients() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COUNT(DISTINCT c.client_id) as count " +
                              "FROM clients c JOIN loans l ON c.client_id = l.client_id " +
                              "WHERE l.status IN ('Approved', 'Due') AND c.is_active = true";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getInt("count") : 0;
                    }
                }
            }
        },
        count -> {
            activeClientsCard.setValue(numberFormat.format(count));
            incrementLoadCount();
            updateDoughnutChart();
        },
        e -> {
            System.err.println("Error loading active clients: " + e.getMessage());
            activeClientsCard.setValue("0");
            incrementLoadCount();
            updateDoughnutChart();
        });
    }
    
    private void loadTotalGivenOut() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COALESCE(SUM(amount), 0) as total " +
                              "FROM loans WHERE status IN ('Approved', 'Due')";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getDouble("total") : 0.0;
                    }
                }
            }
        },
        total -> {
            totalGivenOutCard.setValue(formatCurrency(total));
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading total given out: " + e.getMessage());
            totalGivenOutCard.setValue("K 0.00");
            incrementLoadCount();
        });
    }
    
    private void loadTotalExpected() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COALESCE(SUM(total_amount), 0) as total " +
                              "FROM loans WHERE status IN ('Approved', 'Due')";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getDouble("total") : 0.0;
                    }
                }
            }
        },
        total -> {
            totalExpectedCard.setValue(formatCurrency(total));
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading total expected: " + e.getMessage());
            totalExpectedCard.setValue("K 0.00");
            incrementLoadCount();
        });
    }
    
    private void updateDoughnutChart() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                int totalClients = 0;
                int activeLoans = 0;
                int pendingLoans = 0;
                int dueLoans = 0;
                
                String query = "SELECT COUNT(*) FROM clients WHERE is_active = true";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) totalClients = rs.getInt(1);
                    }
                }
                
                query = "SELECT COUNT(*) FROM loans WHERE status IN ('Approved', 'Due')";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) activeLoans = rs.getInt(1);
                    }
                }
                
                query = "SELECT COUNT(*) FROM loans WHERE status = 'Pending'";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) pendingLoans = rs.getInt(1);
                    }
                }
                
                query = "SELECT COUNT(*) FROM loans WHERE status = 'Due'";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) dueLoans = rs.getInt(1);
                    }
                }
                
                return new int[]{totalClients, activeLoans, pendingLoans, dueLoans};
            }
        },
        data -> {
            if (doughnutChart != null) {
                doughnutChart.setData(data);
            }
        },
        e -> System.err.println("Error updating doughnut chart: " + e.getMessage()));
    }
    
    private void loadRecentActivities() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT employee_name, action, action_date, details " +
                              "FROM audit_logs ORDER BY action_date DESC LIMIT 6";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<String> activities = new ArrayList<>();
                        while (rs.next()) {
                            String time = new SimpleDateFormat("HH:mm").format(rs.getTimestamp("action_date"));
                            String action = rs.getString("action");
                            String employee = rs.getString("employee_name");
                            String details = rs.getString("details");
                            
                            String activity = String.format("%s - %s: %s", time, employee, action);
                            if (details != null && !details.trim().isEmpty()) {
                                if (details.length() > 30) details = details.substring(0, 27) + "...";
                                activity += " - " + details;
                            }
                            activities.add(activity);
                        }
                        return activities.toArray(new String[0]);
                    }
                }
            }
        },
        activities -> {
            updateRecentActivitiesList(activities);
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading recent activities: " + e.getMessage());
            updateRecentActivitiesList(new String[0]);
            incrementLoadCount();
        });
    }
    
    private void loadPendingApprovalsList() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT l.loan_number, c.first_name, c.last_name, l.amount " +
                              "FROM loans l JOIN clients c ON l.client_id = c.client_id " +
                              "WHERE l.status = 'Pending' " +
                              "ORDER BY l.application_date DESC LIMIT 5";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<String> approvals = new ArrayList<>();
                        while (rs.next()) {
                            String name = rs.getString("first_name") + " " + rs.getString("last_name");
                            if (name.length() > 20) name = name.substring(0, 17) + "...";
                            String approval = String.format("%s: %s - %s",
                                rs.getString("loan_number"),
                                name,
                                formatCompactCurrency(rs.getDouble("amount")));
                            approvals.add(approval);
                        }
                        return approvals.toArray(new String[0]);
                    }
                }
            }
        },
        approvals -> {
            updateListContent(pendingDisbursementsListPanel, approvals, "No pending loan approvals");
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading pending approvals: " + e.getMessage());
            updateListContent(pendingDisbursementsListPanel, new String[0], "No pending loan approvals");
            incrementLoadCount();
        });
    }
    
    private void loadDueLoansList() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT l.loan_number, c.first_name, c.last_name, " +
                              "l.outstanding_balance, l.due_date " +
                              "FROM loans l " +
                              "JOIN clients c ON l.client_id = c.client_id " +
                              "WHERE l.status = 'Due' " +
                              "ORDER BY l.due_date ASC LIMIT 5";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<String> loans = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
                        while (rs.next()) {
                            String name = rs.getString("first_name") + " " + rs.getString("last_name");
                            if (name.length() > 20) name = name.substring(0, 17) + "...";
                            String dueDate = "N/A";
                            if (rs.getDate("due_date") != null) {
                                dueDate = sdf.format(rs.getDate("due_date"));
                            }
                            String loan = String.format("%s: %s - %s (%s)",
                                rs.getString("loan_number"),
                                name,
                                formatCompactCurrency(rs.getDouble("outstanding_balance")),
                                dueDate);
                            loans.add(loan);
                        }
                        return loans.toArray(new String[0]);
                    }
                }
            }
        },
        loans -> {
            updateListContent(dueLoansListPanel, loans, "No due loans");
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading due loans: " + e.getMessage());
            updateListContent(dueLoansListPanel, new String[0], "No due loans");
            incrementLoadCount();
        });
    }
    
    private void updateListContent(JPanel container, String[] items, String emptyMessage) {
        if (container == null) return;
        
        for (Component comp : container.getComponents()) {
            if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                Component view = scrollPane.getViewport().getView();
                if (view instanceof JPanel) {
                    JPanel listPanel = (JPanel) view;
                    listPanel.removeAll();
                    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
                    
                    if (items == null || items.length == 0) {
                        JLabel noData = new JLabel(emptyMessage);
                        noData.setForeground(TEXT_GRAY);
                        noData.setAlignmentX(Component.CENTER_ALIGNMENT);
                        listPanel.add(noData);
                    } else {
                        for (String item : items) {
                            JLabel label = new JLabel("• " + item);
                            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                            label.setForeground(TEXT_WHITE);
                            label.setAlignmentX(Component.LEFT_ALIGNMENT);
                            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
                            listPanel.add(label);
                        }
                    }
                    
                    listPanel.revalidate();
                    listPanel.repaint();
                    break;
                }
            }
        }
    }
    
    private String formatCurrency(double amount) {
        if (amount == 0) {
            return "K 0.00";
        } else if (amount < 1000) {
            return currencyFormat.format(amount);
        } else if (amount < 1_000_000) {
            return currencyFormat.format(amount);
        } else if (amount < 1_000_000_000) {
            double millions = amount / 1_000_000;
            if (millions >= 100) {
                return currencyFormat.format(amount);
            } else {
                return String.format("K %.2fM", millions);
            }
        } else {
            double billions = amount / 1_000_000_000;
            return String.format("K %.2fB", billions);
        }
    }
    
    private String formatCompactCurrency(double amount) {
        if (amount == 0) {
            return "K 0";
        } else if (amount < 1000) {
            return compactCurrencyFormat.format(amount);
        } else if (amount < 1_000_000) {
            return compactCurrencyFormat.format(amount);
        } else if (amount < 1_000_000_000) {
            double millions = amount / 1_000_000;
            if (millions >= 100) {
                return compactCurrencyFormat.format(amount);
            } else {
                return String.format("K %.1fM", millions);
            }
        } else {
            double billions = amount / 1_000_000_000;
            return String.format("K %.1fB", billions);
        }
    }
    
    private void updateRecentActivitiesList(String[] activities) {
        if (recentActivitiesPanel == null) return;
        
        recentActivitiesPanel.removeAll();
        recentActivitiesPanel.setLayout(new BoxLayout(recentActivitiesPanel, BoxLayout.Y_AXIS));
        
        if (activities == null || activities.length == 0) {
            JLabel noData = new JLabel("No recent activities");
            noData.setForeground(TEXT_GRAY);
            noData.setAlignmentX(Component.CENTER_ALIGNMENT);
            recentActivitiesPanel.add(noData);
        } else {
            for (String activity : activities) {
                JLabel item = new JLabel("• " + activity);
                item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                item.setForeground(TEXT_WHITE);
                item.setAlignmentX(Component.LEFT_ALIGNMENT);
                item.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
                recentActivitiesPanel.add(item);
            }
        }
        
        recentActivitiesPanel.revalidate();
        recentActivitiesPanel.repaint();
    }

    private void startAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        
        animationTimer = new Timer(16, e -> {
            animationProgress += 0.05f;
            if (animationProgress >= 1f) {
                animationProgress = 1f;
                animationTimer.stop();
            }
            if (doughnutChart != null) {
                doughnutChart.setAnimationProgress(animationProgress);
                doughnutChart.repaint();
            }
        });
        animationTimer.start();
    }

    private void setLoadingState(boolean isLoading) {
        SwingUtilities.invokeLater(() -> {
            if (refreshBtn != null) {
                refreshBtn.setEnabled(!isLoading);
                refreshBtn.setText(isLoading ? "Loading..." : "⟳ Refresh");
            }
            
            if (backupBtn != null) {
                backupBtn.setEnabled(!isLoading);
            }
        });
    }

    private void refreshDashboard() {
        animationProgress = 0f;
        startAnimation();
        loadDashboardStats();
        AsyncDatabaseService.logAsync(employeeId, "Dashboard Refresh", "Refreshed dashboard");
    }
    
private void createBackupPDF() {
    backupBtn.setEnabled(false);
    backupBtn.setText("Generating Report...");
    
    AsyncDatabaseService.executeAsync(() -> {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                StringBuilder reportContent = new StringBuilder();
                
                int totalClients = 0;
                int activeClients = 0;
                int approvedLoans = 0;
                int dueLoans = 0;
                int pendingLoans = 0;
                double portfolio = 0;
                double disbursed = 0;
                double expected = 0;
                double collected = 0;
                
                String query = "SELECT COUNT(*) FROM clients WHERE is_active = true";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) totalClients = rs.getInt(1);
                }
                
                query = "SELECT COUNT(DISTINCT c.client_id) FROM clients c JOIN loans l ON c.client_id = l.client_id WHERE l.status IN ('Approved', 'Due') AND c.is_active = true";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) activeClients = rs.getInt(1);
                }
                
                query = "SELECT COUNT(*) FROM loans WHERE status = 'Approved'";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) approvedLoans = rs.getInt(1);
                }
                
                query = "SELECT COUNT(*) FROM loans WHERE status = 'Due'";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) dueLoans = rs.getInt(1);
                }
                
                query = "SELECT COUNT(*) FROM loans WHERE status = 'Pending'";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) pendingLoans = rs.getInt(1);
                }
                
                query = "SELECT COALESCE(SUM(outstanding_balance), 0), COALESCE(SUM(amount), 0), COALESCE(SUM(total_amount), 0) FROM loans WHERE status IN ('Approved', 'Due')";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        portfolio = rs.getDouble(1);
                        disbursed = rs.getDouble(2);
                        expected = rs.getDouble(3);
                    }
                }
                
                query = "SELECT COALESCE(SUM(paid_amount), 0) FROM loan_payments WHERE status = 'Paid'";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) collected = rs.getDouble(1);
                }
                
                // Get company name from system settings
                String companyName = "MS CODEFORGE";
                query = "SELECT setting_value FROM system_settings WHERE setting_key = 'COMPANY_NAME'";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getString(1) != null) {
                        companyName = rs.getString(1);
                    }
                }
                
                // Get company address/contact if available
                String companyAddress = "";
                String companyPhone = "";
                String companyEmail = "";
                
                query = "SELECT setting_value FROM system_settings WHERE setting_key = 'COMPANY_ADDRESS'";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getString(1) != null) {
                        companyAddress = rs.getString(1);
                    }
                }
                
                query = "SELECT setting_value FROM system_settings WHERE setting_key = 'COMPANY_PHONE'";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getString(1) != null) {
                        companyPhone = rs.getString(1);
                    }
                }
                
                query = "SELECT setting_value FROM system_settings WHERE setting_key = 'COMPANY_EMAIL'";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getString(1) != null) {
                        companyEmail = rs.getString(1);
                    }
                }
                
                // Professional report format with sections
                reportContent.append("================================================================================\n\n");
                
                reportContent.append("Report Details:\n");
                reportContent.append("────────────────────────────────────────────────────────────────────────────────\n");
                reportContent.append("Generated on: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
                reportContent.append("Generated by: ").append(employeeName).append("\n");
                reportContent.append("Report ID: MS-").append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())).append("\n");
                reportContent.append("Period: ").append(new SimpleDateFormat("MMMM yyyy").format(new Date())).append("\n\n");
                
                reportContent.append("PORTFOLIO SUMMARY\n");
                reportContent.append("────────────────────────────────────────────────────────────────────────────────\n");
                reportContent.append("Total Clients: ").append(numberFormat.format(totalClients)).append("\n");
                reportContent.append("Active Clients with Loans: ").append(numberFormat.format(activeClients)).append("\n");
                reportContent.append("Approved Loans: ").append(numberFormat.format(approvedLoans)).append("\n");
                reportContent.append("Due Loans: ").append(numberFormat.format(dueLoans)).append("\n");
                reportContent.append("Pending Loan Approvals: ").append(numberFormat.format(pendingLoans)).append("\n");
                reportContent.append("Total Portfolio Value: ").append(formatCurrency(portfolio)).append("\n");
                reportContent.append("Total Amount Disbursed: ").append(formatCurrency(disbursed)).append("\n");
                reportContent.append("Total Expected Returns: ").append(formatCurrency(expected)).append("\n");
                reportContent.append("Total Amount Collected: ").append(formatCurrency(collected)).append("\n\n");
                
                double profit = expected - disbursed;
                double profitPercentage = disbursed > 0 ? (profit / disbursed) * 100 : 0;
                double collectionRate = disbursed > 0 ? (collected / disbursed) * 100 : 0;
                double defaultRate = dueLoans > 0 ? (dueLoans / (double)(approvedLoans + dueLoans)) * 100 : 0;
                
                reportContent.append("FINANCIAL ANALYSIS\n");
                reportContent.append("────────────────────────────────────────────────────────────────────────────────\n");
                reportContent.append("Projected Profit: ").append(formatCurrency(profit)).append(" (").append(String.format("%.1f", profitPercentage)).append("%)\n");
                reportContent.append("Collection Rate: ").append(String.format("%.1f%%", collectionRate)).append("\n");
                reportContent.append("Default Rate: ").append(String.format("%.1f%%", defaultRate)).append("\n");
                reportContent.append("Collection Efficiency: ").append(collectionRate >= 80 ? "Excellent" : collectionRate >= 60 ? "Good" : collectionRate >= 40 ? "Fair" : "Poor").append("\n\n");
                
                reportContent.append("TOP 5 DUE LOANS\n");
                reportContent.append("────────────────────────────────────────────────────────────────────────────────\n");
                
                query = "SELECT l.loan_number, c.first_name, c.last_name, " +
                      "l.outstanding_balance, l.due_date, b.branch_name " +
                      "FROM loans l " +
                      "JOIN clients c ON l.client_id = c.client_id " +
                      "LEFT JOIN branches b ON c.branch_id = b.branch_id " +
                      "WHERE l.status = 'Due' " +
                      "ORDER BY l.outstanding_balance DESC LIMIT 5";
                
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (rs.next()) {
                        reportContent.append(String.format("%-15s %-25s %-20s %-15s %-12s%n", 
                            "Loan No", "Client", "Branch", "Balance", "Due Date"));
                        reportContent.append("────────────────────────────────────────────────────────────────────────────────\n");
                        
                        do {
                            String dueDate = "N/A";
                            if (rs.getDate("due_date") != null) {
                                dueDate = new SimpleDateFormat("dd/MM/yyyy").format(rs.getDate("due_date"));
                            }
                            
                            reportContent.append(String.format("%-15s %-25s %-20s %-15s %-12s%n",
                                rs.getString("loan_number"),
                                rs.getString("first_name") + " " + rs.getString("last_name"),
                                rs.getString("branch_name") != null ? rs.getString("branch_name") : "N/A",
                                formatCompactCurrency(rs.getDouble("outstanding_balance")),
                                dueDate));
                        } while (rs.next());
                    } else {
                        reportContent.append("No due loans at this time.\n");
                    }
                }
                
                reportContent.append("\n");
                reportContent.append("END OF REPORT\n");
                reportContent.append("Generated by: ").append(companyName).append(" Loan Management System\n");
                reportContent.append("Report Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
                reportContent.append("System Version: 2.0 | Report Type: Portfolio Backup");
                
                return new ReportData(
                    reportContent.toString(), 
                    companyName,
                    companyAddress,
                    companyPhone,
                    companyEmail
                );
            }
            return new ReportData("", "MS CODEFORGE", "", "", "");
        }
    },
    reportData -> {
        backupBtn.setEnabled(true);
        backupBtn.setText("📁 Backup PDF");
        
        Printable printable = createPrintableReport((ReportData) reportData);
        
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(reportData.companyName + " Backup Report");
        
        PrintService[] services = PrinterJob.lookupPrintServices();
        PrintService pdfService = null;
        
        for (PrintService service : services) {
            if (service.getName().toLowerCase().contains("pdf") || 
                service.getName().toLowerCase().contains("print to file")) {
                pdfService = service;
                break;
            }
        }
        
        if (pdfService == null) {
            job.setPrintable(printable);
            if (job.printDialog()) {
                try {
                    job.print();
                } catch(Exception e) {
                    System.out.println("Print job error: " + e.getMessage());
                }
                
                AsyncDatabaseService.logAsync(employeeId, "Report Printed", "Printed backup report");
            }
        } else {
            try {
                job.setPrintService(pdfService);
                job.setPrintable(printable);
                
                if (job.printDialog()) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Save PDF Report");
                    String reportId = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    fileChooser.setSelectedFile(new File(
                        System.getProperty("user.home") + "/Desktop/" + 
                        reportData.companyName.replace(" ", "_") + 
                        "_Portfolio_Report_" + reportId + ".pdf"
                    ));
                    fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
                    
                    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        
                        if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                            selectedFile = new File(selectedFile.getAbsolutePath() + ".pdf");
                        }
                        
                        final File finalFile = selectedFile;
                        
                        job.print();
                        
                        JOptionPane.showMessageDialog(this,
                            "✅ PDF report created successfully!\n\n" +
                            "File: " + finalFile.getName() + "\n" +
                            "Size: " + String.format("%.1f", finalFile.length() / 1024.0) + " KB\n" +
                            "Location: " + finalFile.getParent(),
                            "PDF Backup Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        AsyncDatabaseService.logAsync(employeeId, "PDF Backup Created", 
                            "Created PDF backup file: " + finalFile.getName());
                    }
                }
            } catch (Exception e) {
                showError("PDF Creation Error: " + e.getMessage());
            }
        }
    },
    e -> {
        backupBtn.setEnabled(true);
        backupBtn.setText("📁 Backup PDF");
        showError("Report Generation Error: " + e.getMessage());
    });
}

private Printable createPrintableReport(ReportData reportData) {
    return new Printable() {
        @Override
        public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
            if (page > 0) {
                return Printable.NO_SUCH_PAGE;
            }
                
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pf.getImageableX(), pf.getImageableY());
                
            int y = 40;
            int width = (int) pf.getImageableWidth();
            int margin = 20;
            
            // Get logo from database
            try {
                BufferedImage logo = LogoManager.getLogoAsBufferedImage(LogoManager.LOGO_REPORT);
                if (logo != null) {
                    // Smaller logo (40px height) positioned on left
                    int logoHeight = 40;
                    int logoWidth = (int) ((double) logo.getWidth() / logo.getHeight() * logoHeight);
                    
                    // Draw logo on left
                    g2d.drawImage(logo, margin, y, logoWidth, logoHeight, null);
                    
                    // Company info on right of logo
                    int textStartX = margin + logoWidth + 15;
                    
                    Font companyFont = new Font("Arial", Font.BOLD, 12);
                    Font detailsFont = new Font("Arial", Font.PLAIN, 9);
                    
                    g2d.setFont(companyFont);
                    g2d.drawString(reportData.companyName, textStartX, y + 15);
                    
                    g2d.setFont(detailsFont);
                    if (!reportData.companyAddress.isEmpty()) {
                        g2d.drawString(reportData.companyAddress, textStartX, y + 30);
                        y += 15;
                    }
                    if (!reportData.companyPhone.isEmpty()) {
                        g2d.drawString("Tel: " + reportData.companyPhone, textStartX, y + 45);
                        y += 15;
                    }
                    if (!reportData.companyEmail.isEmpty()) {
                        g2d.drawString("Email: " + reportData.companyEmail, textStartX, y + 60);
                    }
                    
                    y = 40 + logoHeight + 25;
                }
            } catch (Exception e) {
                // Logo not available, just draw company name
                Font companyFont = new Font("Arial", Font.BOLD, 14);
                g2d.setFont(companyFont);
                g2d.drawString(reportData.companyName, margin, y);
                y += 25;
            }
            
            // Draw line separator
            g2d.drawLine(margin, y, width - margin, y);
            y += 15;
            
            Font titleFont = new Font("Arial", Font.BOLD, 16);
            Font subtitleFont = new Font("Arial", Font.BOLD, 12);
            Font headerFont = new Font("Arial", Font.BOLD, 11);
            Font normalFont = new Font("Arial", Font.PLAIN, 10);
            Font smallFont = new Font("Arial", Font.PLAIN, 8);
            
            // Report title (centered)
            g2d.setFont(titleFont);
            FontMetrics fm = g2d.getFontMetrics();
            String title = "PORTFOLIO BACKUP REPORT";
            int titleX = margin + ((width - (2 * margin) - fm.stringWidth(title)) / 2);
            g2d.drawString(title, titleX, y);
            y += 25;
            
            String[] lines = reportData.content.split("\n");
            boolean inSection = false;
            
            for (String line : lines) {
                if (line.contains("────────────────")) {
                    // Draw a line separator
                    g2d.drawLine(margin, y - 5, width - margin, y - 5);
                    y += 5;
                } else if (line.startsWith("PORTFOLIO SUMMARY") || 
                          line.startsWith("FINANCIAL ANALYSIS") || 
                          line.startsWith("TOP 5 DUE LOANS") ||
                          line.startsWith("Report Details")) {
                    g2d.setFont(headerFont);
                    g2d.drawString(line, margin, y);
                    y += 15;
                    inSection = true;
                } else if (line.startsWith("Generated by:") || 
                          line.startsWith("Report Generated:") || 
                          line.startsWith("System Version:") ||
                          line.startsWith("END OF REPORT")) {
                    g2d.setFont(smallFont);
                    fm = g2d.getFontMetrics();
                    int lineX = margin + ((width - (2 * margin) - fm.stringWidth(line)) / 2);
                    g2d.drawString(line, lineX, y);
                    y += 10;
                } else if (line.startsWith("========================================================================")) {
                    // Draw thick line
                    g2d.drawLine(margin, y, width - margin, y);
                    y += 5;
                } else {
                    g2d.setFont(normalFont);
                    g2d.drawString(line, margin + (inSection ? 10 : 0), y);
                    y += 12;
                }
                    
                if (y > pf.getImageableHeight() - 50) {
                    break;
                }
            }
                
            // Footer
            g2d.setFont(smallFont);
            String footer = "Page 1 of 1 - " + reportData.companyName + " Loan Management System";
            fm = g2d.getFontMetrics();
            int footerX = margin + ((width - (2 * margin) - fm.stringWidth(footer)) / 2);
            g2d.drawString(footer, footerX, (int) pf.getImageableHeight() - 20);
                
            return Printable.PAGE_EXISTS;
        }
    };
}

// Update the ReportData class to include contact info
private class ReportData {
    String content;
    String companyName;
    String companyAddress;
    String companyPhone;
    String companyEmail;
    
    ReportData(String content, String companyName, String companyAddress, 
               String companyPhone, String companyEmail) {
        this.content = content;
        this.companyName = companyName;
        this.companyAddress = companyAddress;
        this.companyPhone = companyPhone;
        this.companyEmail = companyEmail;
    }
}

    private class DoughnutChart extends JPanel {
        private int[] data = new int[4];
        private float animationProgress = 0f;
        private final Color[] colors = {PRIMARY_BLUE, GREEN_SUCCESS, ORANGE_WARNING, RED_ALERT};
        private final String[] labels = {"Total Clients", "Active Loans", "Pending Loans", "Due Loans"};

        public DoughnutChart() {
            setBackground(CARD_BG);
        }

        public void setData(int[] newData) {
            this.data = newData;
            repaint();
        }

        public void setAnimationProgress(float progress) {
            this.animationProgress = progress;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int diameter = Math.min(width, height) - 60;
            int x = (width - diameter) / 2;
            int y = (height - diameter) / 2;

            int total = 0;
            for (int value : data) {
                total += value;
            }

            if (total == 0) {
                g2d.setColor(new Color(60, 65, 75));
                g2d.fillOval(x, y, diameter, diameter);
                
                g2d.setColor(CARD_BG);
                g2d.fillOval(x + 20, y + 20, diameter - 40, diameter - 40);
                
                g2d.setColor(TEXT_GRAY);
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                String noData = "No Data";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(noData, (width - fm.stringWidth(noData)) / 2, height / 2);
                return;
            }

            float startAngle = 90;
            for (int i = 0; i < data.length; i++) {
                float extent = (360 * data[i] / total) * animationProgress;
                
                g2d.setColor(colors[i]);
                g2d.fill(new Arc2D.Float(x, y, diameter, diameter, startAngle, extent, Arc2D.PIE));
                
                startAngle += extent;
            }

            g2d.setColor(CARD_BG);
            int innerDiameter = diameter / 2;
            int innerX = (width - innerDiameter) / 2;
            int innerY = (height - innerDiameter) / 2;
            g2d.fillOval(innerX, innerY, innerDiameter, innerDiameter);

            g2d.setColor(TEXT_WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
            String totalText = String.format("%,d", total);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(totalText, (width - fm.stringWidth(totalText)) / 2, height / 2);
            
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            int legendY = y + diameter + 20;
            for (int i = 0; i < data.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillRect(10, legendY, 10, 10);
                g2d.setColor(TEXT_WHITE);
                String legendText = String.format("%s: %,d (%.1f%%)", 
                    labels[i], data[i], (data[i] * 100.0 / total));
                g2d.drawString(legendText, 25, legendY + 10);
                legendY += 20;
            }
        }
    }
    
    private class MetricCard extends JPanel {
        private JLabel valueLabel;

        public MetricCard(String name, String value, Color color) {
            setLayout(new BorderLayout());
            setBackground(CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 55, 65), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            nameLabel.setForeground(TEXT_GRAY);
            nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

            valueLabel = new JLabel("<html><center>" + value + "</center></html>");
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            valueLabel.setForeground(color);

            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.add(nameLabel);
            contentPanel.add(valueLabel);

            add(contentPanel, BorderLayout.CENTER);
        }

        public void setValue(String value) {
            if (valueLabel != null) {
                valueLabel.setText("<html><center>" + value.replace("\n", "<br>") + "</center></html>");
            }
            repaint();
        }
        
        public JLabel getValueLabel() {
            return valueLabel;
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}