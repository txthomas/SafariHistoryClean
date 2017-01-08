package com.clean.safariHistory;

import com.dd.plist.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.sql.*;
import java.text.ParseException;

public class Main {

    public static void main(String[] args) {

        String searchFileName = "searchExpressions.txt";
        String dbFileName = null;
        String plistFileName = null;

        for(int i = 0; i < args.length; i++) {

            switch(args[i]) {

                case "-d":
                    dbFileName = args[i+1];
                    break;
                case "-s":
                    searchFileName = args[i+1];
                    break;
                case "-p":
                    plistFileName = args[i+1];
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

        if(plistFileName == null || plistFileName.equals("")) {
            System.out.println("File path to plist file required. Use -h to see help text.");
            return;
        }

        File plistFile = new File(plistFileName);

        NSDictionary rootDict;
        try {

            rootDict = (NSDictionary) PropertyListParser.parse(plistFile);
        } catch (IOException e) {

            System.out.println("Plist file read failed! (" + e.toString() + ")");
            return;
        } catch (PropertyListFormatException e) {

            System.out.println("Unknown format of plist file! (" + e.toString() + ")");
            return;
        } catch (ParseException e) {

            System.out.println("Parsing of plist file failed! (" + e.toString() + ")");
            return;
        } catch (ParserConfigurationException e) {

            System.out.println("Plist file parser config error! (" + e.toString() + ")");
            return;
        } catch (SAXException e) {

            System.out.println("SAX Exception! (" + e.toString() + ")");
            return;
        }

        DBController dbc = new DBController(dbFileName);
        dbc.initDBConnection();

        File searchFile = new File(searchFileName);
        FileReader fr = null;
        try {
            fr = new FileReader(searchFile);
        } catch (FileNotFoundException e) {
            System.out.println("Search expressions file not found! (" + e.toString() + ")");
            return;
        }
        BufferedReader br = new BufferedReader(fr);

        String expression;

        try {
            while((expression = br.readLine()) != null){

                if(!expression.equals("")) {
                    cleanHistory(dbc.getDBConnection(), expression);
                    rootDict = cleanTabs(rootDict, expression);
                }
            }
        } catch (IOException e) {
            System.out.println("Search expressions file read failed! (" + e.toString() + ")");
            return;
        }

        try {
            BinaryPropertyListWriter.write(plistFile, rootDict);
        } catch (IOException e) {
            System.out.println("Write new plist file failed! (" + e.toString() + ")");
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
        System.out.println("-p:\tFile path to plist file !required!");
        System.out.println("-s:\tFile path to search expressions file (default 'searchExpressions.txt')");
    }

    private static void cleanHistory(Connection connection, String expression) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, url FROM history_items;");

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

    private static NSDictionary cleanTabs(NSDictionary rootDict, String expression) {

        boolean delete = false;
        String url = "";


        NSObject[] persistentStates = ((NSArray)rootDict.objectForKey("ClosedTabOrWindowPersistentStates")).getArray();

        for(int i = 0, j = 0; i < persistentStates.length; i++) {

            NSObject tabState;
            NSObject[] tabStateParameters;
            NSObject persistentState = ((NSDictionary)persistentStates[i]).objectForKey("PersistentState");

            if(((NSDictionary)persistentState).containsKey("TabStates")) {

                tabState = ((NSDictionary) persistentState).objectForKey("TabStates");
                tabStateParameters = ((NSArray)tabState).getArray();

            } else {

                tabStateParameters = new NSObject[1];
                tabStateParameters[0] = persistentState;
            }

            for(NSObject tabStateParameter: tabStateParameters) {

                tabStateParameter = ((NSDictionary) tabStateParameter).objectForKey("TabURL");

                if (tabStateParameter == null) {
                    delete = true;
                    continue;
                }

                url = tabStateParameter.toString();

                if(url.matches(expression)) {
                    delete = true;
                }
            }

            if(delete) {
                System.out.println(url + " deleted from recent closed tabs!");
                ((NSArray)rootDict.objectForKey("ClosedTabOrWindowPersistentStates")).remove(i-j);
                j++;
            }
        }

        return rootDict;
    }
}
