import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDashboard extends JPanel {
    private int employeeId;
    private String employeeName;
    private DoughnutChart doughnutChart;
    private Timer animationTimer;
    private float animationProgress = 0f;
    
    private MetricCard totalClientsCard;
    private MetricCard activeLoansCard;
    private MetricCard duePaymentsCard;
    private MetricCard pendingLoansCard;
    
    private JPanel recentActivitiesPanel;
    
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
    
    private JButton refreshBtn;
    private Timer timeoutTimer;
    
    private int completedLoads = 0;
    private final int TOTAL_LOADS = 5;

    public EmployeeDashboard(int employeeId, String employeeName) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        
        initUI();
        loadDashboardStats();
        startAnimation();
        AsyncDatabaseService.logAsync(employeeId, "Dashboard Access", "Accessed employee dashboard");
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
        
        JLabel roleLabel = new JLabel("Employee");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roleLabel.setForeground(TEXT_GRAY);
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));
        sidebar.add(roleLabel);
        
        String[] menuIcons = {"👥", "💰", "💳", "🔒"};
        String[] menuTexts = {"Clients", "Loans", "Payments", "Change Password"};
        
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
                switch (screenName) {
                    case "Clients":
                        ScreenManager.getInstance().showScreen(new ClientsScreen(employeeId, "employee"));
                        break;
                    case "Loans":
                        ScreenManager.getInstance().showScreen(new LoansScreen(employeeId, "employee"));
                        break;
                    case "Payments":
                        ScreenManager.getInstance().showScreen(new PaymentsScreen(employeeId, "employee"));
                        break;
                    case "Change Password":
                        ScreenManager.getInstance().showScreen(new ChangePasswordScreen(employeeId, "employee", employeeName));
                        break;
                    default:
                        return;
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
        
        refreshBtn = new JButton("⟳ Refresh");
        styleButton(refreshBtn, PRIMARY_BLUE);
        refreshBtn.addActionListener(e -> refreshDashboard());
        
        rightPanel.add(timeLabel);
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
    }

    private JPanel createDashboardContent() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(DARK_BG);
        
        JPanel welcomePanel = createWelcomePanel();
        mainPanel.add(welcomePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JPanel metricsPanel = createMetricsPanel();
        mainPanel.add(metricsPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPanel chartAndActivitiesPanel = createChartAndActivitiesPanel();
        mainPanel.add(chartAndActivitiesPanel);
        
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
        
        JLabel welcomeLabel = new JLabel("Welcome, " + employeeName);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        welcomeLabel.setForeground(TEXT_GRAY);
        
        JLabel dateLabel = new JLabel(new SimpleDateFormat("EEEE, MMMM d, yyyy").format(new Date()));
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateLabel.setForeground(TEXT_GRAY);
        
        welcomePanel.add(welcomeLabel, BorderLayout.WEST);
        welcomePanel.add(dateLabel, BorderLayout.EAST);
        
        return welcomePanel;
    }

    private JPanel createMetricsPanel() {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setBackground(DARK_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        totalClientsCard = new MetricCard("Total Clients", "0", PRIMARY_BLUE);
        activeLoansCard = new MetricCard("Active Loans", "0", GREEN_SUCCESS);
        duePaymentsCard = new MetricCard("Due Payments", "0", RED_ALERT);
        pendingLoansCard = new MetricCard("Pending Loans", "0", ORANGE_WARNING);
        
        row.add(totalClientsCard);
        row.add(activeLoansCard);
        row.add(duePaymentsCard);
        row.add(pendingLoansCard);
        
        return row;
    }

    private JPanel createChartAndActivitiesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
        panel.setBackground(DARK_BG);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        
        panel.add(createChartPanel());
        panel.add(createActivitiesPanel());
        
        return panel;
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

    private void loadDashboardStats() {
        completedLoads = 0;
        setLoadingState(true);
        startTimeoutTimer();
        
        loadTotalClients();
        loadActiveLoans();
        loadDuePayments();
        loadPendingLoans();
        loadRecentActivities();
    }
    
    private void startTimeoutTimer() {
        if (timeoutTimer != null) {
            timeoutTimer.stop();
        }
        
        timeoutTimer = new Timer(10000, e -> {
            setLoadingState(false);
            System.out.println("Employee Dashboard: Loading timeout");
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
    
    private void loadTotalClients() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COUNT(*) FROM clients WHERE is_active = true";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getInt(1) : 0;
                    }
                }
            }
        },
        count -> {
            totalClientsCard.setValue(numberFormat.format(count));
            incrementLoadCount();
        },
        e -> {
            System.err.println("Error loading total clients: " + e.getMessage());
            totalClientsCard.setValue("0");
            incrementLoadCount();
        });
    }
    
    // FIXED: Active loans should include both Approved and Due status loans
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
            updateDoughnutChart();
        },
        e -> {
            System.err.println("Error loading active loans: " + e.getMessage());
            activeLoansCard.setValue("0");
            incrementLoadCount();
            updateDoughnutChart();
        });
    }
    
    // FIXED: Due payments should only show "Due" status loans
    private void loadDuePayments() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COUNT(*) FROM loans WHERE status = 'Due'";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getInt(1) : 0;
                    }
                }
            }
        },
        count -> {
            duePaymentsCard.setValue(numberFormat.format(count));
            incrementLoadCount();
            updateDoughnutChart();
        },
        e -> {
            System.err.println("Error loading due payments: " + e.getMessage());
            duePaymentsCard.setValue("0");
            incrementLoadCount();
            updateDoughnutChart();
        });
    }
    
    private void loadPendingLoans() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT COUNT(*) FROM loans WHERE status = 'Pending'";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setQueryTimeout(5);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() ? rs.getInt(1) : 0;
                    }
                }
            }
        },
        count -> {
            pendingLoansCard.setValue(numberFormat.format(count));
            incrementLoadCount();
            updateDoughnutChart();
        },
        e -> {
            System.err.println("Error loading pending loans: " + e.getMessage());
            pendingLoansCard.setValue("0");
            incrementLoadCount();
            updateDoughnutChart();
        });
    }
    
    // FIXED: Optimized SQL query to get all data in one call
    private void updateDoughnutChart() {
        AsyncDatabaseService.executeAsync(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT " +
                             "(SELECT COUNT(*) FROM clients WHERE is_active = true) as total_clients, " +
                             "(SELECT COUNT(*) FROM loans WHERE status IN ('Approved', 'Due')) as active_loans, " +
                             "(SELECT COUNT(*) FROM loans WHERE status = 'Pending') as pending_loans, " +
                             "(SELECT COUNT(*) FROM loans WHERE status = 'Due') as due_loans";
                
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int totalClients = rs.getInt("total_clients");
                            int activeLoans = rs.getInt("active_loans");
                            int pendingLoans = rs.getInt("pending_loans");
                            int dueLoans = rs.getInt("due_loans");
                            
                            return new int[]{totalClients, activeLoans, pendingLoans, dueLoans};
                        }
                    }
                }
                return new int[]{0, 0, 0, 0};
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
        });
    }

    private void refreshDashboard() {
        animationProgress = 0f;
        startAnimation();
        loadDashboardStats();
        AsyncDatabaseService.logAsync(employeeId, "Dashboard Refresh", "Refreshed dashboard");
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
                valueLabel.setText("<html><center>" + value + "</center></html>");
            }
            repaint();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}