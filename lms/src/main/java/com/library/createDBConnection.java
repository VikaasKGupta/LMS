package com.library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class createDBConnection {
    // 1. Your local desktop credentials (Fallback)
    private static final String LOCAL_URL = "jdbc:mysql://localhost:3306/LMSAdmin";
    private static final String LOCAL_USER = "LMSAdmin";
    private static final String LOCAL_PASSWORD = "Lm$Adm!n@2026";

    public static Connection getConnection() throws SQLException {
        // 2. Check if Railway (the cloud) has provided a Database Host
        String cloudHost = System.getenv("MYSQLHOST");

        if (cloudHost != null) {
            // We are in the cloud! Build the Railway connection string
            String cloudPort = System.getenv("MYSQLPORT");
            String cloudDb = System.getenv("MYSQLDATABASE");
            String cloudUser = System.getenv("MYSQLUSER");
            String cloudPass = System.getenv("MYSQLPASSWORD");

            String cloudUrl = "jdbc:mysql://" + cloudHost + ":" + cloudPort + "/" + cloudDb;
            
            return DriverManager.getConnection(cloudUrl, cloudUser, cloudPass);
        } else {
            // We are on your local desktop! Use your standard local credentials
            return DriverManager.getConnection(LOCAL_URL, LOCAL_USER, LOCAL_PASSWORD);
        }
    }
}
