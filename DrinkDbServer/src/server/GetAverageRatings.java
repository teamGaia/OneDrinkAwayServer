package server;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler for getting average ratings.
 * 
 * query: url/OneDrinkAwayServer/getavgratings
 * 
 * Ex: http://54.200.252.24:8080/OneDrinkAwayServer/getavgratings
 * 
 * Response is a String representing the ratings, formatted:
 *          "drinkid1:rating1,drinkid2:rating2,..."
 * 
 * @author John L. Wilson
 *
 */
public class GetAverageRatings extends HttpServlet {
    
    private static final long serialVersionUID = 129408999806943504L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        try {
            PrintWriter out = response.getWriter();
            String result = getAvgRatings();
            response.setStatus(200);
            out.println(result);
            out.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Queries database for average user ratings, Requires connection established
     * @return a String representing the ratings, formatted:
     *         "drinkid1:rating1,drinkid2:rating2,..."
     */
    private static String getAvgRatings() {
        StringBuilder sb = new StringBuilder();
        try {
            Connection conn = DbConnection.getConnection();
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
    
}
