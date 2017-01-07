package com.clean.safariHistory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBController {

    private Connection connection = null;
    private String dbPath;
    private boolean driverLoaded = false;

    public DBController(String dbPath){
        this.dbPath = dbPath;

        try {

            Class.forName("org.sqlite.JDBC");
            driverLoaded = true;

        } catch (ClassNotFoundException e) {

            System.err.println("Failed to load JDBC driver");
            e.printStackTrace();
        }
    }

    public Connection getDBConnection() {
        return connection;
    }

    public void closeDBConnection() throws SQLException {
        connection.close();
        System.out.println("...Connection closed");
    }

    public void initDBConnection() {

        try {
            if (connection != null || !driverLoaded)
                return;

            System.out.println("Creating Connection to Database...");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            if (!connection.isClosed())
                System.out.println("...Connection established");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    if (!connection.isClosed() && connection != null) {

                        connection.close();

                        if (connection.isClosed())

                            System.out.println("Connection to Database closed");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}