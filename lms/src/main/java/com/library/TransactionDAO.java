package com.library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
//import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionDAO {

    public boolean issueBook(String userId, String bookId) { // Changed to String bookId
        // 1. Check availability
        String checkAvailability = "SELECT available_copies FROM book_details WHERE book_id = ?";
        // 2. Decrease book count
        String updateBook = "UPDATE book_details SET available_copies = available_copies - 1 WHERE book_id = ?";
        // 3. Insert the new transaction matching your exact SQL schema
        String insertTransaction = "INSERT INTO transaction_details (userId, book_id, issue_date, due_date) VALUES (?, ?, ?, ?)";

        Connection conn = null;

        try {
            conn = createDBConnection.getConnection(); // Make sure this matches your DB config class!
            
            // Turn off auto-commit for safe transactions
            conn.setAutoCommit(false); 

            // Step 1: Check if the book is in stock
            try (PreparedStatement pstmtCheck = conn.prepareStatement(checkAvailability)) {
                pstmtCheck.setString(1, bookId); // Changed to setString
                ResultSet rs = pstmtCheck.executeQuery();
                if (rs.next()) {
                    int copies = rs.getInt("available_copies");
                    if (copies <= 0) {
                        System.out.println("❌ Sorry, this book is currently out of stock.");
                        return false; 
                    }
                } else {
                    System.out.println("❌ Error: Book ID not found.");
                    return false;
                }
            }

            // Step 2: Update the book inventory (-1 copy)
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateBook)) {
                pstmtUpdate.setString(1, bookId); // Changed to setString
                pstmtUpdate.executeUpdate();
            }

            // Step 3: Record the transaction
            try (PreparedStatement pstmtInsert = conn.prepareStatement(insertTransaction)) {
                
                // Calculate our dates
                LocalDate today = LocalDate.now();
                LocalDate dueDate = today.plusDays(14); // Books are due in 2 weeks
                
                pstmtInsert.setString(1, userId);
                pstmtInsert.setString(2, bookId); // Changed to setString
                
                // Convert Java LocalDate to SQL Date
                pstmtInsert.setDate(3, java.sql.Date.valueOf(today)); 
                pstmtInsert.setDate(4, java.sql.Date.valueOf(dueDate)); 
                
                pstmtInsert.executeUpdate();
            }

            // If we didn't crash, save EVERYTHING
            conn.commit(); 
            return true;

        } catch (SQLException e) {
            System.err.println("Database Error during transaction: " + e.getMessage());
            // If anything failed, ROLLBACK to protect the database!
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { 
                    conn.setAutoCommit(true); 
                    conn.close(); 
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    // Add this method to TransactionDAO.java
    public boolean returnBook(String userId, int bookId) {
        // Query 1: Get the exact transaction ID and due date
        String checkTx = "SELECT transaction_id, due_date FROM transaction_details WHERE userId = ? AND book_id = ? AND return_date IS NULL";
        String updateTx = "UPDATE transaction_details SET return_date = ?, fine = ? WHERE transaction_id = ?";
        String updateBook = "UPDATE book_details SET available_copies = available_copies + 1 WHERE book_id = ?";
        // NEW: Query to insert into fine_details
        String insertFine = "INSERT INTO fine_details (transaction_id, userId, fine_collection_date, amount, status) VALUES (?, ?, NOW(), ?, 'Unpaid')";

        Connection conn = null;

        try {
            conn = createDBConnection.getConnection();
            conn.setAutoCommit(false);

            LocalDate today = LocalDate.now();
            double fineAmount = 0.00;
            int transactionId = -1;

            try (PreparedStatement pstmtCheck = conn.prepareStatement(checkTx)) {
                pstmtCheck.setString(1, userId); // Now a String!
                pstmtCheck.setInt(2, bookId);
                ResultSet rs = pstmtCheck.executeQuery();

                if (rs.next()) {
                    transactionId = rs.getInt("transaction_id");
                    LocalDate dueDate = rs.getDate("due_date").toLocalDate();

                    if (today.isAfter(dueDate)) {
                        long daysLate = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
                        fineAmount = daysLate * 2.00; 
                        System.out.printf("⚠️ Book is %d days late. Fine assessed: $%.2f\n", daysLate, fineAmount);
                    } else {
                        System.out.println("✅ Book returned on time. No fines assessed.");
                    }
                } else {
                    System.out.println("❌ Error: No active borrowing record found.");
                    return false; 
                }
            }

            // Update Transaction
            try (PreparedStatement pstmtUpdateTx = conn.prepareStatement(updateTx)) {
                pstmtUpdateTx.setDate(1, java.sql.Date.valueOf(today));
                pstmtUpdateTx.setDouble(2, fineAmount);
                pstmtUpdateTx.setInt(3, transactionId);
                pstmtUpdateTx.executeUpdate();
            }

            // Restock Book
            try (PreparedStatement pstmtUpdateBook = conn.prepareStatement(updateBook)) {
                pstmtUpdateBook.setInt(1, bookId);
                pstmtUpdateBook.executeUpdate();
            }

            // NEW: Record the Fine if applicable
            if (fineAmount > 0) {
                try (PreparedStatement pstmtFine = conn.prepareStatement(insertFine)) {
                    pstmtFine.setInt(1, transactionId);
                    pstmtFine.setString(2, userId);
                    pstmtFine.setDouble(3, fineAmount);
                    pstmtFine.executeUpdate();
                    System.out.println("📋 Fine record added to database as 'Unpaid'.");
                }
            }

            conn.commit(); 
            return true;

        } catch (SQLException e) {
            System.err.println("Database Error during return: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } 
                catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }
    // 1. Fetch all books currently borrowed by a specific user
    // 1. Fetch borrowed books WITH live fine calculation for the UI
    public List<Map<String, Object>> getBorrowedBooks(String userId) {
        List<Map<String, Object>> borrowedList = new ArrayList<>();
        String sql = "SELECT b.book_id, b.title, b.author, t.due_date " +
                     "FROM book_details b " +
                     "JOIN transaction_details t ON b.book_id = t.book_id " +
                     "WHERE t.userId = ? AND t.return_date IS NULL";

        try (Connection conn = createDBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> book = new java.util.HashMap<>();
                book.put("bookId", rs.getString("book_id"));
                book.put("title", rs.getString("title"));
                book.put("author", rs.getString("author"));
                
                // Calculate the LIVE fine preview
                java.sql.Date sqlDueDate = rs.getDate("due_date");
                LocalDate dueDate = sqlDueDate.toLocalDate();
                LocalDate today = LocalDate.now();
                long daysLate = ChronoUnit.DAYS.between(dueDate, today);
                
                double fineDue = daysLate > 0 ? daysLate * 1.0 : 0.0;
                
                book.put("dueDate", rs.getString("due_date"));
                book.put("fineDue", fineDue); // Send the fine to the browser!
                
                borrowedList.add(book);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching borrowed books: " + e.getMessage());
        }
        return borrowedList;
    }

    // 2. Process a book return
    public double returnBook(String userId, String bookId) {
        // Step 1: Find the ACTIVE transaction (where return_date IS NULL)
        String getTransInfo = "SELECT transaction_id, due_date FROM transaction_details WHERE userId = ? AND book_id = ? AND return_date IS NULL LIMIT 1";
        
        // Step 2: NEW! We update the transaction instead of deleting it!
        String markAsReturned = "UPDATE transaction_details SET return_date = CURDATE() WHERE transaction_id = ?";
        
        // Step 3: Update Inventory
        String updateBook = "UPDATE book_details SET available_copies = available_copies + 1 WHERE book_id = ?";
        
        // Step 4: Insert Fine
        String insertFine = "INSERT INTO fine_details (userId, transaction_id, amount, fine_collection_date, status) VALUES (?, ?, ?, CURDATE(), 'UNPAID')";

        double fineAmount = 0.0;
        int transactionId = -1; 
        Connection conn = null;

        try {
            conn = createDBConnection.getConnection();
            conn.setAutoCommit(false); 

            // Step 1: Get Transaction ID and Calculate Fine
            try (PreparedStatement pstmtGet = conn.prepareStatement(getTransInfo)) {
                pstmtGet.setString(1, userId);
                pstmtGet.setString(2, bookId);
                ResultSet rs = pstmtGet.executeQuery();

                if (rs.next()) {
                    transactionId = rs.getInt("transaction_id"); 
                    LocalDate dueDate = rs.getDate("due_date").toLocalDate();
                    long daysLate = java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                    
                    if (daysLate > 0) {
                        fineAmount = daysLate * 1.0; 
                    }
                } else {
                    return -1.0; // Active transaction not found
                }
            }

            // Step 2: Mark as Returned (Using the PK we just found)
            try (PreparedStatement pstmtUpdateTrans = conn.prepareStatement(markAsReturned)) {
                pstmtUpdateTrans.setInt(1, transactionId);
                pstmtUpdateTrans.executeUpdate();
            }

            // Step 3: Update inventory
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateBook)) {
                pstmtUpdate.setString(1, bookId);
                pstmtUpdate.executeUpdate();
            }

            // Step 4: Insert fine (No foreign key errors now, because the transaction still exists!)
            if (fineAmount > 0) {
                try (PreparedStatement pstmtFine = conn.prepareStatement(insertFine)) {
                    pstmtFine.setString(1, userId);
                    pstmtFine.setInt(2, transactionId); 
                    pstmtFine.setDouble(3, fineAmount);
                    pstmtFine.executeUpdate();
                }
            }

            conn.commit(); 
            return fineAmount; 

        } catch (SQLException e) {
            System.err.println("Return error: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return -1.0;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }
}