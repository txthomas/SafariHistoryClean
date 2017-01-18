package com.clean.safariHistory;

import com.dd.plist.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static List<String> idList = new ArrayList<String>();
    static boolean verboseMode = false;

    public static void main(String[] args) {

        String searchFileName = "searchExpressions.txt";
        try {
            searchFileName = getOwnDirectoryPath() + searchFileName;
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unable to get program location! Use -s parameter to fix problem. (" + e.toString() + ")");
        }
        String dbFileName = System.getProperty("user.home") + "/Library/Safari/History.db";
        String plistFileName = System.getProperty("user.home") + "/Library/Safari/RecentlyClosedTabs.plist";

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
                case "-v":
                    verboseMode = true;
                    break;
                case "-h" :
                case "--help":
                    printHelp();
                    return;
            }
        }

        if(dbFileName.equals("")) {
            System.out.println("File path to database file required. Use -h to see help text.");
            return;
        }

        if(plistFileName.equals("")) {
            System.out.println("File path to plist file required. Use -h to see help text.");
            return;
        }

        // output to check file locations
        System.out.println("Database: " + dbFileName);
        System.out.println("PList:    " + plistFileName);
        System.out.println("Search:   " + searchFileName);

//        try {
//            Files.copy(Paths.get(dbFileName), Paths.get(dbFileName+"_bakup"), StandardCopyOption.REPLACE_EXISTING);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // open search expressions file
        File searchFile = new File(searchFileName);
        FileReader fr = null;
        try {
            fr = new FileReader(searchFile);
        } catch (FileNotFoundException e) {
            System.out.println("Search expressions file not found! (" + e.toString() + ")");
            return;
        }
        BufferedReader br = new BufferedReader(fr);

        // open database file and connect to db
        DBController dbc = null;
        try {
            dbc = new DBController(dbFileName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        dbc.initDBConnection();

        try {
            Statement stmt = dbc.getDBConnection().createStatement();

            // backup db
            stmt.executeUpdate("backup to " + dbFileName.substring(0, dbFileName.lastIndexOf('.')) + "_backup.db");
            stmt.close();

        } catch (SQLException e) {
            System.out.println("Couldn't handle DB-Query! (" + e.toString() + ")");
        }

        // open plist file for recent closed tabs and parse it
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
            Statement stmt = dbc.getDBConnection().createStatement();

            // search for history_visits which no history_item exists for
            stmt.executeUpdate("DELETE FROM history_visits WHERE history_item NOT IN (SELECT id FROM history_items);");
            stmt.close();

        } catch (SQLException e) {
            System.out.println("Couldn't handle DB-Query! (" + e.toString() + ")");
        }

        try {
            dbc.closeDBConnection();
        } catch (SQLException e) {
            System.out.println("DB connection could not be closed! (" + e.toString() + ")");
        }

        if(verboseMode) {

            System.out.println("\nUse following SQL statements to clean your database manually:\n");
            System.out.println("DELETE FROM history_visits WHERE history_item IN (" + idList.toString().replace("[", "").replace("]", "") + ");");
            System.out.println("DELETE FROM history_items WHERE id IN (" + idList.toString().replace("[", "").replace("]", "") + ");");
        } else {

            System.out.println("\nAll cleaned successfully!");
        }
    }

    // print help output
    private static void printHelp() {

        System.out.println();
        System.out.println("-d:\tFile path to database file (default: /Users/.../Library/Safari/History.db)");
        System.out.println("-p:\tFile path to plist file (default: /Users/.../Library/Safari/RecentlyClosedTabs.plist");
        System.out.println("-s:\tFile path to search expressions file (default 'searchExpressions.txt')");
        System.out.println("-v:\tPerform verbose db scan and show sql clean query (manuall clean required)");
    }

    private static void cleanHistory(Connection connection, String expression) {
        try {
            Statement stmt = connection.createStatement();
            Statement stmtDel = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, url FROM history_items ORDER BY id ASC;");

            while (rs.next()) {

                if(rs.getString("url").matches(expression)) {
                    System.out.printf("ID = %4d", Integer.parseInt(rs.getString("id")));
                    System.out.print("  URL = " + rs.getString("url"));

                    if(verboseMode) {

                        System.out.println();
                        idList.add(rs.getString("id"));
                    } else {

                        System.out.println("...   deleted from history!");

                        stmtDel.executeUpdate("DELETE FROM history_visits WHERE history_item = " + rs.getString("id") + ";");
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stmtDel.executeUpdate("DELETE FROM history_items WHERE id = " + rs.getString("id") + ";");
                    }
                }
            }

            connection.commit();
            rs.close();
            stmt.close();
            stmtDel.close();

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

    private static String getOwnDirectoryPath() throws UnsupportedEncodingException {

        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        decodedPath = decodedPath.substring(0, decodedPath.lastIndexOf('/'));

        return decodedPath + "/";
    }
}
