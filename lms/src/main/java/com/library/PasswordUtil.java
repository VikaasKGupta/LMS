package com.library;

// ==========================================
// PasswordUtil.java
// Handles hashing and verifying passwords
// ==========================================
import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // 1. Hash the password before saving to the database
    public static String hashPassword(String plainTextPassword) {
        // gensalt() automatically generates a random salt and applies it
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12)); 
    }

    // 2. Verify a typed password against the database hash during login
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        // BCrypt extracts the salt from the hash and checks if they match
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
 /*    public static void main(String[] args) {
        // Generate the hash for your default password
        String myHash = hashPassword("admin123");
        System.out.println("Copy this hash to your SQL script: " + myHash);
    }*/
}