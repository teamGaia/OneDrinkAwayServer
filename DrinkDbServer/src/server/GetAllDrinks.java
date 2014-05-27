package server;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.onedrinkaway.model.Drink;
import com.onedrinkaway.model.DrinkInfo;

/**
 * Handler for getting all Drinks and related DrinkInfo.
 * 
 * query: url/OneDrinkAwayServer/getalldrinks
 * 
 * Ex: http://54.200.252.24:8080/OneDrinkAwayServer/getalldrinks
 * 
 * Response is a String representing a map from all Drinks to their DrinkInfo
 * 
 * @author John L. Wilson
 *
 */
public class GetAllDrinks extends HttpServlet {

    private static final long serialVersionUID = 3726699317820925241L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        try {
            PrintWriter out = response.getWriter();
            String result = getAllDrinks();
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
    private static String getAllDrinks() {
        Gson gson = new Gson();
        HashMap<String, String> drinks = new HashMap<String, String>();
        try {
            Connection conn = DbConnection.getConnection();
            Statement stmt = conn.createStatement();
            String drinkSQL = "SELECT * FROM DRINK";
            ResultSet drinkRS = stmt.executeQuery(drinkSQL);
            // map id->avgrating
            Map<Integer, Double> avgRatings = getAvgRatings();
            // map id->list of ingredients
            Map<Integer, List<String>> drinkIngredients = getDrinkIngredients();
            // map id->list of categories
            Map<Integer, List<String>> drinkCategories = getDrinkCategories();
            while (drinkRS.next()) {
                // get basic info from Drinks
                int id = drinkRS.getInt(1);
                String name = drinkRS.getString(2);
                String glass = drinkRS.getString(3);
                String garnish = drinkRS.getString(4);
                String description = drinkRS.getString(5);
                String instructions = drinkRS.getString(6);
                String source = drinkRS.getString(7);
                int[] att = new int[11];
                for (int i = 0; i < 11; i++) {
                    att[i] = drinkRS.getInt(i + 8);
                }
                double avg = avgRatings.get(id);
                List<String> cats = drinkCategories.get(id);
                List<String> ings = drinkIngredients.get(id);
                Drink d = new Drink(name, id, avg, att, cats, glass);
                DrinkInfo di = new DrinkInfo(ings, description, garnish, instructions, source, id);
                drinks.put(gson.toJson(d), gson.toJson(di));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gson.toJson(drinks);
    }
    
    /**
     * Gets average user ratings for all drinks
     */
    private static Map<Integer, Double> getAvgRatings() throws Exception {
        Map<Integer, Double> avgRatings = new HashMap<Integer, Double>();
        Connection conn = DbConnection.getConnection();
        String ratingSQL = "SELECT drinkid, AVG(rating) FROM RATING GROUP BY drinkid";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(ratingSQL);
        while (rs.next()) {
            avgRatings.put(rs.getInt(1), rs.getDouble(2));
        }
        conn.close();
        return avgRatings;
    }
    
    /**
     * Gets ingredient lists for all drinks
     */
    private static Map<Integer, List<String>> getDrinkIngredients() throws Exception {
        Map<Integer, List<String>> drinkIngredients = new HashMap<Integer, List<String>>();
        Connection conn = DbConnection.getConnection();
        String ratingSQL = "SELECT id FROM DRINK";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(ratingSQL);
        while (rs.next()) {
            int id = rs.getInt(1);
            List<String> ingr = new ArrayList<String>(); // ingredients list for drinkInfo
            String isql = "SELECT ingredientWithPortions FROM INGREDIENT WHERE drinkid = " + id;
            Statement istmt = conn.createStatement();
            ResultSet irs = istmt.executeQuery(isql);
            while (irs.next()) {
                ingr.add(irs.getString(1));
            }
            drinkIngredients.put(id, ingr);
        }
        return drinkIngredients;
    }
    
    /**
     * Gets category lists for all drinks
     */
    private static Map<Integer, List<String>> getDrinkCategories() throws Exception {
        Map<Integer, List<String>> drinkCategories = new HashMap<Integer, List<String>>();
        Connection conn = DbConnection.getConnection();
        String ratingSQL = "SELECT id FROM DRINK";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(ratingSQL);
        while (rs.next()) {
            int id = rs.getInt(1);
            List<String> ingr = new ArrayList<String>(); // ingredients list for drinkInfo
            String isql = "SELECT category FROM CATEGORY WHERE drinkid = " + id;
            Statement istmt = conn.createStatement();
            ResultSet irs = istmt.executeQuery(isql);
            while (irs.next()) {
                ingr.add(irs.getString(1));
            }
            drinkCategories.put(id, ingr);
        }
        return drinkCategories;
    }
    
}