package com.library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID; // Needed to generate unique text IDs

public class LibrarianDAO {

    public boolean registerLibrarian(String firstName, String lastName, String email, String phone, String password, String address) {
        
        // Generate a random, unique ID (e.g., "LIB-4F9A2")
        String uniqueId = "LIB-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        
        String sql = "INSERT INTO librarian_details (librarianId, first_name, last_name, email, phone_number, password, address) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = createDBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uniqueId);
            pstmt.setString(2, firstName);
            pstmt.setString(3, lastName);
            pstmt.setString(4, email);
            pstmt.setString(5, phone);
            pstmt.setString(6, password);
            pstmt.setString(7, address);

            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Librarian Badge ID Generated: " + uniqueId);
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            return false;
        }
    }
    
    public String loginLibrarian(String email, String rawPassword) {
        String sql = "SELECT librarianId, password FROM librarian_details WHERE email = ?";

        try (Connection conn = createDBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    
                    // Verify the password
                    if (PasswordUtil.checkPassword(rawPassword, storedHash)) {
                        return rs.getString("librarianId"); // Return their badge ID!
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error during login: " + e.getMessage());
        }
        return null; // Return null if login fails
    }
}