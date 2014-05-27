package server;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler for getting user favorites, query must be a single parameter, the userid.
 * 
 * query: url/OneDrinkAwayServer/getuserfavorites? + <userid>
 * 
 * Ex: http://54.200.252.24:8080/OneDrinkAwayServer/getuserfavorites?9774d56d682e549c
 * 
 * Response is a String representing the ratings, formatted:
     *         "drinkid1,drinkid2,..."
 * 
 * @author John L. Wilson
 *
 */
public class GetUserFavorites extends HttpServlet {
    
    private static final long serialVersionUID = 129408999806943504L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        try {
            String userid = request.getQueryString();
            if (userid.contains(";") || userid.length() > 17)
                response.sendError(403, "Bad Request");
            String result = getUserFavorites(userid);
            response.setStatus(200);
            PrintWriter out = response.getWriter();
            out.println(result);
            out.close();
        }
        catch(Exception e) {
            e.printStackTrace();
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
            Connection conn = DbConnection.getConnection();
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
    
}