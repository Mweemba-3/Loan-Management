import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

public class LogoManager {
    public static final String LOGO_PRIMARY = "LOGO_PRIMARY";
    public static final String LOGO_REPORT = "LOGO_REPORT";
	public static String DEFAULT_LOGO_BASE64;
    
    // Get logo from database - ORIGINAL IMAGE, no modifications
    public static ImageIcon getLogo(String logoType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, logoType);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String base64 = rs.getString("setting_value");
                    if (base64 != null && !base64.isEmpty()) {
                        // Decode base64 to get original image
                        byte[] imageBytes = Base64.getDecoder().decode(base64);
                        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                        
                        // Read image exactly as stored - NO modifications
                        BufferedImage originalImage = ImageIO.read(bis);
                        
                        if (originalImage != null) {
                            // Return ImageIcon with the original BufferedImage
                            return new ImageIcon(originalImage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
        }
        
        // Fallback: Return null for no logo
        return null;
    }
    
    // Get logo with specific dimensions - returns SCALED copy, not modified original
    public static ImageIcon getLogo(String logoType, int width, int height) {
        ImageIcon originalIcon = getLogo(logoType);
        
        if (originalIcon == null) {
            return null;
        }
        
        // Get original image
        Image originalImage = originalIcon.getImage();
        
        // Create a HIGH-QUALITY scaled version using BufferedImage
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        
        // Set rendering hints for maximum quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw original image scaled to new dimensions
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();
        
        return new ImageIcon(scaledImage);
    }
    
    // Store original image to database - NO MODIFICATIONS, store EXACTLY as uploaded
    public static boolean saveLogo(String logoType, ImageIcon icon) {
        if (icon == null) {
            return false;
        }
        
        try {
            String base64Image = convertImageToBase64(icon);
            if (base64Image != null) {
                return updateLogoInDatabase(logoType, base64Image);
            }
        } catch (Exception e) {
            System.err.println("Error saving logo: " + e.getMessage());
        }
        
        return false;
    }
    
    // Convert image to base64 - PRESERVE ORIGINAL EXACTLY
    static String convertImageToBase64(ImageIcon icon) {
        try {
            // Get original image from icon
            Image image = icon.getImage();
            
            // Create buffered image from original WITHOUT modifications
            BufferedImage bufferedImage;
            if (image instanceof BufferedImage) {
                bufferedImage = (BufferedImage) image;
            } else {
                // Create buffered image with original dimensions and type
                bufferedImage = new BufferedImage(
                    icon.getIconWidth(), 
                    icon.getIconHeight(), 
                    BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2d = bufferedImage.createGraphics();
                // Draw original image AS IS
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
            }
            
            // Convert to PNG for lossless quality
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Use PNG with maximum quality
            ImageIO.write(bufferedImage, "PNG", baos);
            
            // Encode to base64
            return Base64.getEncoder().encodeToString(baos.toByteArray());
            
        } catch (Exception e) {
            System.err.println("Error converting image to base64: " + e.getMessage());
            return null;
        }
    }
    
    // Update logo in database
    private static boolean updateLogoInDatabase(String logoType, String base64Image) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if setting exists
            String checkSql = "SELECT COUNT(*) FROM system_settings WHERE setting_key = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, logoType);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next() && rs.getInt(1) > 0) {
                    // Update existing
                    String updateSql = "UPDATE system_settings SET setting_value = ?, " +
                                     "last_updated = CURRENT_TIMESTAMP, " +
                                     "updated_by = NULL " +  // You might want to add user tracking
                                     "WHERE setting_key = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, base64Image);
                        updateStmt.setString(2, logoType);
                        return updateStmt.executeUpdate() > 0;
                    }
                } else {
                    // Insert new
                    String insertSql = "INSERT INTO system_settings (setting_key, setting_name, " +
                                     "setting_value, setting_type, category) " +
                                     "VALUES (?, ?, ?, 'IMAGE', 'Branding')";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, logoType);
                        insertStmt.setString(2, getSettingName(logoType));
                        insertStmt.setString(3, base64Image);
                        return insertStmt.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating logo in database: " + e.getMessage());
            return false;
        }
    }
    
    private static String getSettingName(String logoType) {
        switch (logoType) {
            case LOGO_PRIMARY:
                return "Primary Logo";
            case LOGO_REPORT:
                return "Report Logo";
            default:
                return "Logo";
        }
    }
    
    // Delete logo from database
    public static boolean deleteLogo(String logoType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE system_settings SET setting_value = NULL WHERE setting_key = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, logoType);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting logo: " + e.getMessage());
            return false;
        }
    }
    
    // Check if logo exists
    public static boolean logoExists(String logoType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT setting_value FROM system_settings " +
                        "WHERE setting_key = ? AND setting_value IS NOT NULL";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, logoType);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking logo existence: " + e.getMessage());
            return false;
        }
    }
    
    // Get logo as BufferedImage (useful for printing) - returns ORIGINAL
    public static BufferedImage getLogoAsBufferedImage(String logoType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT setting_value FROM system_settings WHERE setting_key = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, logoType);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String base64 = rs.getString("setting_value");
                    if (base64 != null && !base64.isEmpty()) {
                        byte[] imageBytes = Base64.getDecoder().decode(base64);
                        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                        // Read EXACT image as stored
                        return ImageIO.read(bis);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting logo as BufferedImage: " + e.getMessage());
        }
        return null;
    }
    
    // Load logo from file and save to database
    public static boolean loadLogoFromFile(String logoType, String filePath) {
        try {
            // Load image from file - no modifications
            ImageIcon icon = new ImageIcon(filePath);
            if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                return saveLogo(logoType, icon);
            }
        } catch (Exception e) {
            System.err.println("Error loading logo from file: " + e.getMessage());
        }
        return false;
    }
    
    // NEW: Get logo with proportional scaling for display
    public static ImageIcon getLogoForDisplay(String logoType, int maxWidth, int maxHeight) {
        ImageIcon originalIcon = getLogo(logoType);
        
        if (originalIcon == null) {
            return null;
        }
        
        int originalWidth = originalIcon.getIconWidth();
        int originalHeight = originalIcon.getIconHeight();
        
        // Calculate proportional scaling
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double scale = Math.min(widthRatio, heightRatio);
        
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);
        
        // Use high-quality scaling
        return getLogo(logoType, newWidth, newHeight);
    }
    
    // NEW: Validate image dimensions
    public static boolean validateImageDimensions(String logoType, int minWidth, int minHeight, int maxWidth, int maxHeight) {
        ImageIcon icon = getLogo(logoType);
        if (icon == null) return false;
        
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        
        return width >= minWidth && height >= minHeight && 
               width <= maxWidth && height <= maxHeight;
    }

	public static void updateLogo(String logoType, String base64Image, int userId) {
		// TODO: Implement if needed
	}
}