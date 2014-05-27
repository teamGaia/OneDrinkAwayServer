package server;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Statement;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Test extends HttpServlet {
    
    private static final long serialVersionUID = 129408999806943504L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        try {
            PrintWriter out = response.getWriter();
            out.println("Got PrintWriter");
            try {
                Connection conn = DbConnection.getConnection();
                if (conn == null)
                    out.println("failed to get connection");
                else {
                    out.println("Got Connection");
                    Statement stmt = conn.createStatement();
                    String sql = "SELECT drinkid, AVG(rating) FROM RATING GROUP BY drinkid";
                    stmt.executeQuery(sql);
                    out.println("executed query");
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                out.println(e2.getMessage());
            }
            out.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}