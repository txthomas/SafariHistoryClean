package com.clean.safariHistory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBController {

    private Connection connection = null;
    private String dbPath;
    private boolean driverLoaded = false;

    public DBController(String dbPath) throws ClassNotFoundException {
        this.dbPath = dbPath;

        Class.forName("org.sqlite.JDBC");
        driverLoaded = true;
    }

    public Connection getDBConnection() {
        return connection;
    }

    public void closeDBConnection() throws SQLException {
        connection.commit();
        connection.close();
        System.out.println("...Connection closed");
    }

    public void initDBConnection() {

        try {
            if (connection != null || !driverLoaded)
                return;

            System.out.println("Creating Connection to Database...");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            if (!connection.isClosed()) {
                connection.setAutoCommit(false);
                System.out.println("...Connection established");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    if (connection != null && !connection.isClosed()) {

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