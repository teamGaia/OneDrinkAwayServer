package server;

import java.sql.Connection;
import java.sql.Statement;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler for adding user rating
 * 
 * query: url/OneDrinkAwayServer/adduserrating? + <userid> + & + <drinkid> + & + rating
 * 
 * Ex: http://54.200.252.24:8080/OneDrinkAwayServer/adduserrating?9774d56d682e549c&123&4
 * 
 * @author John L. Wilson
 *
 */
public class AddUserRating extends HttpServlet {
    
    private static final long serialVersionUID = 129408999806943504L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        try {
            String query = request.getQueryString();
            if (query.contains(";") || query.length() > 24) // add ratings shouldn't be longer than 22
                response.sendError(403, "Bad Request");
            String[] tokens = query.split("&");
            if (tokens.length != 3 || tokens[0].length() > 17 ||
                tokens[1].length() > 4 || tokens[2].length() > 1) // check lengths
                response.sendError(403, "Bad Request");
            String userid = tokens[0];
            int drinkid = Integer.parseInt(tokens[1]);
            int rating = Integer.parseInt(tokens[2]);
            if (uploadRating(userid, drinkid, rating))
                response.setStatus(200);
            else
                response.sendError(500, "Internal Error");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Uploads given rating to database. Replaces existing rating if exists.
     * @return true if successful, false if not.
     */
    private static boolean uploadRating(String userid, int drinkid, int rating) {
        try {
            Connection conn = DbConnection.getConnection();
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
    
}