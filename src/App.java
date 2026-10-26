import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class App {

    final static String DB_URL = "jdbc:sqlite:pokemon.db";
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");

        // try-with-resources automatically closes the connection and statement.
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
                
            String sqlQuery = "SELECT * FROM Pet";
            ResultSet rs = statement.executeQuery(sqlQuery);
                while(rs.next())
                {
                    String type = rs.getString("type");
                    if(type.equals("Cat"))
                        System.out.println(rs.getString("name"));
                }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }



    }

    public static ArrayList<Pokemon> getAllPokemonByType(String type) {
        // try-with-resources automatically closes the connection and statement.
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
                
            String sqlQuery = "SELECT * FROM Pet";
            ResultSet rs = statement.executeQuery(sqlQuery);
                while(rs.next())
                {
                    String type = rs.getString("primaryType");
                    if(type.equals("Cat"))
                        System.out.println(rs.getString("name"));
                }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        
    }
}
