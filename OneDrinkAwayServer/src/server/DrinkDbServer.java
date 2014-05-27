package server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * First attempt at web server, no longer in use. Had to reformat this to work on tomcat/ec2
 * @author avalanche
 *
 */

public class DrinkDbServer extends HttpServlet {

    private static final long serialVersionUID = -6065213098037558426L;
    // database connection
    private static Connection conn;
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        try {
            String req = request.getQueryString();
            PrintWriter out = response.getWriter();
            out.println(req);
            out.close();
        }
        catch(Exception e) {
            System.out.println( "cannot get writer: " + e );
        }
    }

    /**
     * Starts server, no args required
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/addrating", new AddUserRatingHandler());
            server.createContext("/getuserrating", new GetUserRatingHandler());
            server.createContext("/getavgrating", new GetAvgRatingHandler());
            server.createContext("/addfavorite", new AddFavoriteHandler());
            server.createContext("/getfavorites", new GetFavoritesHandler());
            server.createContext("/removefavorite", new RemoveFavoriteHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
    }
    
    /**
     * Handler for getting user favorites
     */
    private static class GetFavoritesHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String requestMethod = t.getRequestMethod();
            if (requestMethod.equals("GET")) { // only accept GET
                String userid = t.getRequestURI().getQuery();
                if (userid.contains(";") || userid.length() > 17) // userid shouldn't be longer than 17
                    badRequest(t);
                if (getConnection()) {
                    String result = getUserFavorites(userid);
                    successfulRequest(t, result);
                } else {
                    connectionError(t);
                }
            } else { // not a get Request
                badRequest(t);
            }
        }
    }
    
    /**
     * Handler for getting average ratings
     */
    private static class GetAvgRatingHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String requestMethod = t.getRequestMethod();
            if (requestMethod.equals("GET")) { // only accept GET
                if (getConnection()) {
                    String result = getAvgRatings();
                    successfulRequest(t, result);
                } else {
                    connectionError(t);
                }
            } else { // not a get Request
                badRequest(t);
            }
        }
    }
    
    /**
     * Handler for getting user ratings
     */
    private static class GetUserRatingHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String requestMethod = t.getRequestMethod();
            if (requestMethod.equals("GET")) { // only accept GET
                String userid = t.getRequestURI().getQuery();
                if (userid.contains(";") || userid.length() > 17) // userid shouldn't be longer than 17
                    badRequest(t);
                if (getConnection()) {
                    String result = getUserRatings(userid);
                    successfulRequest(t, result);
                } else {
                    connectionError(t);
                }
            } else { // not a get Request
                badRequest(t);
            }
        }
    }
    
    /**
     * Handler for removing a favorite
     */
    private static class RemoveFavoriteHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String requestMethod = t.getRequestMethod();
            if (requestMethod.equals("GET")) { // only accept GET
                String query = t.getRequestURI().getQuery();
                if (query.contains(";") || query.length() > 22) // remove favorite shouldn't be longer than 20
                    badRequest(t);
                String[] tokens = query.split("&");
                if (tokens.length != 2 || tokens[0].length() > 17 || tokens[1].length() > 4) // check lengths
                    badRequest(t);
                String userid = tokens[0];
                int drinkid = Integer.parseInt(tokens[1]);
                if (getConnection() && removeFavorite(userid, drinkid)) {
                    successfulRequest(t, "success");
                } else {
                    connectionError(t);
                }
            } else { // not a get Request
                badRequest(t);
            }
        }
    }
    
    /**
     * Handler for adding a favorite
     */
    private static class AddFavoriteHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String requestMethod = t.getRequestMethod();
            if (requestMethod.equals("GET")) { // only accept GET
                String query = t.getRequestURI().getQuery();
                if (query.contains(";") || query.length() > 22) // add favorite shouldn't be longer than 20
                    badRequest(t);
                String[] tokens = query.split("&");
                if (tokens.length != 2 || tokens[0].length() > 17 || tokens[1].length() > 4) // check lengths
                    badRequest(t);
                String userid = tokens[0];
                int drinkid = Integer.parseInt(tokens[1]);
                if (getConnection() && uploadFavorite(userid, drinkid)) {
                    successfulRequest(t, "success");
                } else {
                    connectionError(t);
                }
            } else { // not a get Request
                badRequest(t);
            }
        }
    }

    /**
     * Handler for adding a rating
     */
    private static class AddUserRatingHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String requestMethod = t.getRequestMethod();
            if (requestMethod.equals("GET")) { // only accept GET
                String query = t.getRequestURI().getQuery();
                if (query.contains(";") || query.length() > 24) // add ratings shouldn't be longer than 22
                    badRequest(t);
                String[] tokens = query.split("&");
                if (tokens.length != 3 || tokens[0].length() > 17 ||
                    tokens[1].length() > 4 || tokens[2].length() > 1) // check lengths
                    badRequest(t);
                String userid = tokens[0];
                int drinkid = Integer.parseInt(tokens[1]);
                int rating = Integer.parseInt(tokens[2]);
                if (getConnection() && uploadRating(userid, drinkid, rating)) {
                    successfulRequest(t, "success");
                } else {
                    connectionError(t);
                }
            } else { // not a get Request
                badRequest(t);
            }
        }
    }
    
    /**
     * Queries database for user favorites, Requires connection established
     * @return a String representing the ratings, formatted:
     *         "drinkid1,drinkid2,..."
     */
    private static String getUserFavorites(String userid) {
        StringBuilder sb = new StringBuilder();
        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT drinkid FROM FAVORITE WHERE userid = '" + userid + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) { // fence post
                sb.append(rs.getString(1));
            }
            while (rs.next()) {
                sb.append(",");
                sb.append(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    /**
     * Queries database for average user ratings, Requires connection established
     * @return a String representing the ratings, formatted:
     *         "drinkid1:rating1,drinkid2:rating2,..."
     */
    private static String getAvgRatings() {
        StringBuilder sb = new StringBuilder();
        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT drinkid, AVG(rating) FROM RATING GROUP BY drinkid";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) { // fence post
                sb.append(rs.getString(1));
                sb.append(":");
                sb.append(rs.getString(2));
            }
            while (rs.next()) {
                sb.append(",");
                sb.append(rs.getString(1));
                sb.append(":");
                sb.append(rs.getString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    /**
     * Queries database for user ratings, Requires connection established
     * @return a String representing the ratings, formatted:
     *         "drinkid1:rating1,drinkid2:rating2,..."
     */
    private static String getUserRatings(String userid) {
        StringBuilder sb = new StringBuilder();
        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT drinkid, rating FROM RATING WHERE userid = '" + userid + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) { // fence post
                sb.append(rs.getString(1));
                sb.append(":");
                sb.append(rs.getString(2));
            }
            while (rs.next()) {
                sb.append(",");
                sb.append(rs.getString(1));
                sb.append(":");
                sb.append(rs.getString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    /**
     * Uploads given rating to database. Replaces existing rating if exists.
     * Requires connection established.
     * @return true if successful, false if not.
     */
    private static boolean uploadRating(String userid, int drinkid, int rating) {
        try {
            Statement stmt = conn.createStatement();
            String remSQL = "DELETE FROM RATING WHERE drinkid = " + drinkid +
                    " AND userid = '" + userid + "'";
            stmt.executeUpdate(remSQL);
            String addSQL = "INSERT INTO RATING VALUES (" + drinkid + ", " + 
                            rating + ", '" + userid + "')";
            stmt.executeUpdate(addSQL);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Uploads given favorite to database if it doesn't already exist.
     * Requires connection established.
     * @return true if successful, false if failure
     */
    private static boolean uploadFavorite(String userid, int drinkid) {
        try {
            Statement stmt = conn.createStatement();
            if (!removeFavorite(userid, drinkid)) // avoid duplicates
                return false;
            String addSQL = "INSERT INTO FAVORITE VALUES (" + drinkid + ", '" +
                            userid + "')";
            stmt.executeUpdate(addSQL);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Removes given favorite to database if it exists.
     * Requires connection established.
     * @return true if successful, false if failure
     */
    private static boolean removeFavorite(String userid, int drinkid) {
        try {
            Statement stmt = conn.createStatement();
            String remSQL = "DELETE FROM FAVORITE WHERE drinkid = " + drinkid +
                    " AND userid = '" + userid + "'";
            stmt.executeUpdate(remSQL);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Sends "success" as response, ends exchange
     */
    private static void successfulRequest(HttpExchange t, String response) {
        try {
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sends bad request response, ends exchange
     */
    private static void badRequest(HttpExchange t) {
        try {
            String response = "Bad Request";
            t.sendResponseHeaders(403, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sends connection Error response, ends exchange
     */
    private static void connectionError(HttpExchange t) {
        try {
            String response = "Internal Error";
            t.sendResponseHeaders(500, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Attempts connection to database
     * @return true if successful, false if not
     */
    private static boolean getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
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
                conn = DriverManager.getConnection(jdbcUrl);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}