package com.clean.safariHistory;

import java.io.*;
import java.sql.*;

public class Main {

    public static void main(String[] args) {

        String searchFileName = "searchExpressions.txt";
        String dbFileName = null;

        for(int i = 0; i < args.length; i++) {

            switch(args[i]) {

                case "-d":
                    dbFileName = args[i+1];
                    break;
                case "-s":
                    searchFileName = args[i+1];
                    break;
                case "-h" :
                case "--help":
                    printHelp();
                    break;
            }
        }

        if(dbFileName == null || dbFileName.equals("")) {
            System.out.println("File path to database file required. Use -h to see help text.");
            return;
        }

        DBController dbc = new DBController(dbFileName);
        dbc.initDBConnection();

        File file = new File(searchFileName);
        FileReader fr = null;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            System.out.println("Search expressions file not found! (" + e.toString() + ")");
            return;
        }
        BufferedReader br = new BufferedReader(fr);

        String expression;

        try {
            while((expression = br.readLine()) != null){

                if(!expression.equals("")) {
                    deleteHistory(dbc.getDBConnection(), expression);
                }
            }
        } catch (IOException e) {
            System.out.println("Search expressions file read failed! (" + e.toString() + ")");
            return;
        }

        try {
            dbc.closeDBConnection();
        } catch (SQLException e) {
            System.out.println("DB connection could not be closed! (" + e.toString() + ")");
        }

        System.out.println("\nAll cleaned successfully!");
    }

    // print help output
    private static void printHelp() {

        System.out.println();
        System.out.println("-d:\tFile path to database file !required!");
        System.out.println("-s:\tFile path to search expressions file (default 'searchExpressions.txt')");
    }

    private static void deleteHistory(Connection connection, String expression) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM history_items;");

            PreparedStatement psVisits = connection
                    .prepareStatement("DELETE FROM history_visits WHERE history_item = ?;");

            PreparedStatement psItems = connection
                    .prepareStatement("DELETE FROM history_items WHERE id = ?;");

            psVisits.setInt(1, 432);

            while (rs.next()) {

                if(rs.getString("url").matches(expression)) {
                    System.out.print("ID = " + rs.getString("id"));
                    System.out.print("\tURL = " + rs.getString("url"));
                    System.out.println("...   deleted from history!");

                    psVisits.setInt(1, rs.getInt("id"));
                    psVisits.addBatch();

                    psItems.setInt(1, rs.getInt("id"));
                    psItems.addBatch();
                }
            }

            connection.setAutoCommit(false);
            psVisits.executeBatch();
            psItems.executeBatch();
            connection.setAutoCommit(true);

            rs.close();
        } catch (SQLException e) {
            System.out.println("Couldn't handle DB-Query! (" + e.toString() + ")");
        }
    }
}
