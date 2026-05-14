package com.library;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.util.Map;
import java.util.List;

// --- NEW IMPORTS ADDED FOR THE SETUP SCRIPT ---
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class Main {
    public static void main(String[] args) {
        
        // 1. Initialize BOTH DAOs here
        UserDAO userDAO = new UserDAO();
        LibrarianDAO librarianDAO = new LibrarianDAO();
        BookDAO bookDAO = new BookDAO();
        TransactionDAO transactionDAO = new TransactionDAO();

       // ==========================================
        // AUTOMATED ADMIN SETUP SCRIPT (WITH DEBUGGING)
        // ==========================================
        System.out.println("[DEBUG] Step 1: Attempting to connect to database...");
        try (Connection conn = createDBConnection.getConnection()) { 
            System.out.println("[DEBUG] Step 2: Database connection successful!");
            
            String checkSql = "SELECT * FROM librarian_details WHERE email = 'admin@library.com'";
            System.out.println("[DEBUG] Step 3: Executing SELECT query to check for existing admin...");
            
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                 ResultSet rs = checkStmt.executeQuery()) {
                
                System.out.println("[DEBUG] Step 4: SELECT query executed.");
                
                if (!rs.next()) {
                    System.out.println("[DEBUG] Step 5: No admin account found. Proceeding to create one...");
                    
                    String plainTextPassword = "admin123";
                    String newHash = BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
                    System.out.println("[DEBUG] Step 6: BCrypt hash generated successfully. Length: " + newHash.length());
                    
                    // NOTE: Make sure these columns match your Railway Database exactly!
                    String insertSql = "INSERT INTO librarian_details (librarianId, first_name, last_name, email, phone_number, password, address) VALUES (?, ?, ?, ?, ?)";
                    System.out.println("[DEBUG] Step 7: Preparing INSERT statement...");
                    
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, "saanya");
                        insertStmt.setString(2, "Aanya");
                        insertStmt.setString(3, "Singh");
                        insertStmt.setString(4, "saanya@gmail.com");
                        insertStmt.setString(5, "555-1234");
                        insertStmt.setString(6, newHash);
                        insertStmt.setString(7, "Library HQ");
                        
                        System.out.println("[DEBUG] Step 8: Executing INSERT statement now...");
                        int rowsAffected = insertStmt.executeUpdate();
                        
                        System.out.println("[DEBUG] Step 9: INSERT executed. Rows affected: " + rowsAffected);
                        if (rowsAffected > 0) {
                            System.out.println("Admin account created successfully!");
                        } else {
                            System.out.println("[DEBUG] WARNING: Insert ran, but 0 rows were affected.");
                        }
                    } catch (SQLException e) {
                        System.out.println("[DEBUG] FAILED AT STEP 8 (INSERT ERROR): " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("[DEBUG] Step 5 (Alternate): Admin account already exists. Skipping setup.");
                }
            } catch (SQLException e) {
                System.out.println("[DEBUG] FAILED AT STEP 3 (SELECT ERROR): " + e.getMessage());
                e.printStackTrace();
            }
        } catch (SQLException e) {
            System.out.println("[DEBUG] FAILED AT STEP 1 (CONNECTION ERROR): " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[DEBUG] UNEXPECTED NON-SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        // ==========================================


        // 2. Start the Javalin Web Server on port 7070
        int port = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 7070;
        Javalin app = Javalin.create(config -> {
            // Pointing directly to your raw folder so Maven doesn't get confused!
            config.staticFiles.add("/public", Location.CLASSPATH);
            
            // Allow our frontend to talk to our backend
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start("0.0.0.0", port);

        System.out.println("LMS Web Server is running at http://localhost:" + port);
        

        // ==========================================
        // REST API ENDPOINTS
        // ==========================================

        // Unified Login API (Handles both Members and Librarians)
        app.post("/api/login", ctx -> {
            Map<String, String> loginData = ctx.bodyAsClass(Map.class);
            String email = loginData.get("email");
            String password = loginData.get("password");
            String role = loginData.get("role"); // Getting the radio button choice!

            if ("librarian".equals(role)) {
                // Route to Librarian Database
                String libId = librarianDAO.loginLibrarian(email, password);
                if (libId != null) {
                    ctx.json(Map.of("status", "success", "role", "librarian", "userId", libId));
                } else {
                    ctx.status(401).json(Map.of("status", "error", "message", "Invalid Librarian credentials"));
                }
            } else {
                // Route to Member Database
                boolean success = userDAO.loginUser(email, password);
                if (success) {
                    String userId = userDAO.getUserIdByEmail(email);
                    ctx.json(Map.of("status", "success", "role", "member", "userId", userId));
                } else {
                    ctx.status(401).json(Map.of("status", "error", "message", "Invalid Member credentials"));
                }
            }
        });
        
        app.post("/api/books", ctx -> {
            Map<String, String> bookData = ctx.bodyAsClass(Map.class);
            
            String title = bookData.get("title");
            String author = bookData.get("author");
            String isbn = bookData.get("isbn");
            String publisher = bookData.get("publisher");
            String genre = bookData.get("genre");
            int year = Integer.parseInt(bookData.get("year"));
            int copies = Integer.parseInt(bookData.get("copies"));
            int available_copies = Integer.parseInt(bookData.get("available_copies"));

            // Call your EXACT existing BookDAO!
            boolean success = bookDAO.addBook(title, author, isbn, publisher, genre, year, copies, available_copies);

            if (success) {
                ctx.json(Map.of("status", "success", "message", "Book added to catalog!"));
            } else {
                ctx.status(500).json(Map.of("status", "error", "message", "Database error. Could not add book."));
            }
        });
        
        // 3. Get All Books API
        app.get("/api/books", ctx -> {
            // Call your newly modified method!
            List<Map<String, Object>> books = bookDAO.viewAllBooks();
            ctx.json(books);
        });
        
        app.post("/api/borrow", ctx -> {
            Map<String, String> requestData = ctx.bodyAsClass(Map.class);
            String userId = requestData.get("userId");
            String bookId = requestData.get("bookId");

            // CALLING YOUR METHOD HERE!
            boolean success = transactionDAO.issueBook(userId, bookId);

            if (success) {
                ctx.json(Map.of("status", "success", "message", "Book successfully borrowed! Due in 14 days."));
            } else {
                ctx.status(400).json(Map.of("status", "error", "message", "Book is unavailable or an error occurred."));
            }
        });
        
        // 5. Get Borrowed Books API (For the Member Dashboard)
        app.get("/api/borrowed/{userId}", ctx -> {
            // Javalin reads the {userId} directly from the URL!
            String userId = ctx.pathParam("userId"); 
            List<Map<String, Object>> borrowedBooks = transactionDAO.getBorrowedBooks(userId);
            ctx.json(borrowedBooks);
        });

        // 6. Return Book API (Now with Fine Calculation!)
        app.post("/api/return", ctx -> {
            Map<String, String> requestData = ctx.bodyAsClass(Map.class);
            String userId = requestData.get("userId");
            String bookId = requestData.get("bookId");

            // Catch the fine amount returned by our updated DAO
            double fine = transactionDAO.returnBook(userId, bookId);

            if (fine >= 0.0) {
                if (fine > 0.0) {
                    // String.format("%.2f", fine) ensures it looks like money (e.g., $5.00)
                    String msg = "Book returned! Note: You have a late fee of $" + String.format("%.2f", fine);
                    ctx.json(Map.of("status", "success", "message", msg));
                } else {
                    ctx.json(Map.of("status", "success", "message", "Book returned on time. No late fees!"));
                }
            } else {
                ctx.status(400).json(Map.of("status", "error", "message", "Could not process return."));
            }
        });
        
        // 7. Register New Member API
        app.post("/api/members", ctx -> {
            Map<String, String> data = ctx.bodyAsClass(Map.class);
            // Pass all 6 fields to your DAO!
            boolean success = userDAO.registerUser(
                data.get("first_name"), data.get("last_name"), 
                data.get("email"), data.get("phone"), 
                data.get("password"), data.get("address")
            );
            if (success) {
                ctx.json(Map.of("status", "success", "message", "Member successfully registered!"));
            } else {
                ctx.status(400).json(Map.of("status", "error", "message", "Email already exists or error occurred."));
            }
        });

        // 8. Register New Librarian API
        app.post("/api/librarians", ctx -> {
            Map<String, String> data = ctx.bodyAsClass(Map.class);
            boolean success = librarianDAO.registerLibrarian(
                data.get("first_name"), data.get("last_name"), 
                data.get("email"), data.get("phone"), 
                data.get("password"), data.get("address")
            );
            if (success) {
                ctx.json(Map.of("status", "success", "message", "Librarian successfully registered!"));
            } else {
                ctx.status(400).json(Map.of("status", "error", "message", "Email already exists or error occurred."));
            }
        });
    }
}