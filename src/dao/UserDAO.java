package dao;

import model.*;
import security.PasswordHasher;

import java.sql.*;

public class UserDAO {
    
    // Authenticate user
    @SuppressWarnings("CallToPrintStackTrace")
    public User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        User user = null;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);

            String storedPassword = null;
            int userId = 0;
            String role = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    storedPassword = rs.getString("password");
                    if (PasswordHasher.matches(password, storedPassword)) {
                        userId = rs.getInt("user_id");
                        role = rs.getString("role");
                    }
                }
            }
            if (role != null) {
                user = new User();
                user.setUserId(userId);
                user.setUsername(username);
                user.setPassword(password);
                user.setRole(role);
                if (PasswordHasher.needsUpgrade(storedPassword)) {
                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE users SET password = ? WHERE user_id = ?")) {
                        update.setString(1, PasswordHasher.hash(password));
                        update.setInt(2, userId);
                        update.executeUpdate();
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return user;
    }
    @SuppressWarnings("CallToPrintStackTrace")
    // Check if username exists
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    /** Role value used for student accounts. Anything else is treated as an admin, as before. */
    public static final String ROLE_STUDENT = "STUDENT";

    /** Initial password for a new student login: date of birth as ddMMyyyy (e.g. 26122004). */
    public static String defaultStudentPassword(java.time.LocalDate dob) {
        return dob.format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public boolean addUser(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, PasswordHasher.hash(user.getPassword()));
            pstmt.setString(3, user.getRole());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Changes a password only if the current one is correct. */
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        String sql = "SELECT user_id, password FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            int userId;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next() || !PasswordHasher.matches(oldPassword, rs.getString("password"))) {
                    return false;
                }
                userId = rs.getInt("user_id");
            }
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE users SET password = ? WHERE user_id = ?")) {
                update.setString(1, PasswordHasher.hash(newPassword));
                update.setInt(2, userId);
                return update.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Admin action: set a student's password without knowing the old one. */
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean resetStudentPassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ? AND role = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, PasswordHasher.hash(newPassword));
            pstmt.setString(2, username);
            pstmt.setString(3, ROLE_STUDENT);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
