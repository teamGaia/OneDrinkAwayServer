package server;

import java.sql.Connection;
import java.sql.Statement;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler for adding user favorite
 * 
 * query: url/OneDrinkAwayServer/addfavorite? + <userid> + & + <drinkid>
 * 
 * Ex: http://54.200.252.24:8080/OneDrinkAwayServer/addfavorite?9774d56d682e549c&123
 * 
 * @author John L. Wilson
 *
 */
public class AddFavorite extends HttpServlet {
    
    private static final long serialVersionUID = 129408999806943504L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        try {
            String query = request.getQueryString();
            if (query.contains(";") || query.length() > 22) // remove favorite shouldn't be longer than 20
                response.sendError(403, "Bad Request");
            String[] tokens = query.split("&");
            if (tokens.length != 2 || tokens[0].length() > 17 || tokens[1].length() > 4) // check lengths
                response.sendError(403, "Bad Request");
            String userid = tokens[0];
            int drinkid = Integer.parseInt(tokens[1]);
            if (addFavorite(userid, drinkid))
                response.setStatus(200);
            else
                response.sendError(500, "Internal Error");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Uploads given favorite to database if it doesn't already exist.
     * @return true if successful, false if failure
     */
    private static boolean addFavorite(String userid, int drinkid) {
        try {
            Connection conn = DbConnection.getConnection();
            Statement stmt = conn.createStatement();
            if (!RemoveFavorite.removeFavorite(userid, drinkid)) // avoid duplicates
                return false;
            String addSQL = "INSERT INTO FAVORITE VALUES (" + drinkid + ", '" +
                            userid + "')";
            stmt.executeUpdate(addSQL);
            conn.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
}