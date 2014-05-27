package server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Scanner;

public class DbConnection {

    /**
     * Attempts connection to database
     * @return true if successful, false if not
     */
    public static Connection getConnection() {
        try {
            Scanner sc = new Scanner(new File("/usr/pwd.txt"));
            String password = sc.next();
            sc.close();
            String dbName = "onedrinkaway"; 
            String userName = "teamgaia"; 
            String hostname = "onedrinkaway.ctfs3q1wopmj.us-west-2.rds.amazonaws.com";
            String port = "3306";
            
            String jdbcUrl = "jdbc:mysql://" + hostname + ":"
            + port + "/" + dbName + "?user=" + userName + "&password=" + password;
            
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(jdbcUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
}
