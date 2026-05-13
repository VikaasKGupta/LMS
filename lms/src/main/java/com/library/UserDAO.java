package com.library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class UserDAO {
    // Method to insert a new user into the database
    public boolean registerUser(String firstName, String lastName, String email, String phone, String password, String address) {
        // The ? are placeholders that protect against SQL Injection
        String sql = "INSERT INTO user_details (userId, first_name, last_name, email, phone_number, password, address) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String userId = "USR-" + java.util.UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        
        // Try-with-resources automatically closes the database connection when done
        try (Connection conn = createDBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Bind the Java variables to the ? placeholders
            pstmt.setString(1, userId);
            pstmt.setString(2, firstName);
            pstmt.setString(3, lastName);
            pstmt.setString(4, email);
            pstmt.setString(5, phone);
            pstmt.setString(6, password);
            pstmt.setString(7, address);

            // Execute the query
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Returns true if the insert succeeded

        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            return false;
        }
    }
    public boolean loginUser(String email, String rawPassword) {
        // We only need to fetch the password hash based on the email provided
        String sql = "SELECT password FROM user_details WHERE email = ?";

        try (Connection conn = createDBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            
            // For SELECT queries, we use executeQuery(), which returns a ResultSet
            try (ResultSet rs = pstmt.executeQuery()) {
                
                // rs.next() moves the cursor to the first row of results. 
                // If it's false, the email doesn't exist in the database.
                if (rs.next()) {
                    // Grab the hashed password from the database row
                    String storedHash = rs.getString("password");
                    
                    // Use our PasswordUtil to see if the raw password matches the hash
                    return PasswordUtil.checkPassword(rawPassword, storedHash);
                } else {
                    System.out.println("Error: Invalid email or password.");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            return false;
        }
    }
    public String getUserIdByEmail(String email) {
        String sql = "SELECT userId FROM user_details WHERE email = ?";
        try (Connection conn = createDBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("userId");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user ID: " + e.getMessage());
        }
        return "Error: Something went wrong"; // Returns  if something went wrong
    }
}
