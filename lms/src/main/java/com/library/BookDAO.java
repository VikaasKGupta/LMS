package com.library;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookDAO {

    // 1. Method to add a new book to the database
    public boolean addBook(String title, String author, String isbn, String publisher, String genre, int published_year, int total_copies, int available_copies) {
        String sql = "INSERT INTO book_details (book_id, title, author, isbn, publisher, genre, published_year, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String book_Id = "BOOK-" + java.util.UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        try (Connection conn = createDBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book_Id);
            pstmt.setString(2, title);
            pstmt.setString(3, author);
            pstmt.setString(4, isbn);
            pstmt.setString(5, publisher);
            pstmt.setString(6, genre);
            pstmt.setInt(7, published_year);
            pstmt.setInt(8, total_copies);
            pstmt.setInt(9, available_copies);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            return false;
        }
    }

    // 2. Method to fetch and print all books in the library
    public List<Map<String, Object>> viewAllBooks() {
        // 1. Create an empty list to hold the books
        List<Map<String, Object>> bookList = new ArrayList<>();
        String sql = "SELECT * FROM book_details";

        try (Connection conn = createDBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            // rs.next() loops through every single row returned by the database
            while (rs.next()) {
                // 2. Instead of printing, create a "dictionary" (Map) for the current book
                Map<String, Object> book = new HashMap<>();
                
                // 3. Put all the data from the ResultSet into the map
                book.put("id", rs.getString("book_id"));
                book.put("title", rs.getString("title"));
                book.put("author", rs.getString("author"));
                book.put("isbn", rs.getString("isbn"));
                book.put("publisher", rs.getString("publisher"));
                book.put("genre", rs.getString("genre"));
                book.put("year", rs.getInt("published_year"));
                book.put("total", rs.getInt("total_copies"));
                book.put("available", rs.getInt("available_copies"));
                
                // 4. Add this completed book to our master list
                bookList.add(book);
            }

        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
        }
        
        // 5. Return the full list to the Web Server!
        return bookList;
    }
}