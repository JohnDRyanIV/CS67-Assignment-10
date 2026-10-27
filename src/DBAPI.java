import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

public class DBAPI {

    final static String DB_URL = "jdbc:sqlite:pokemon.db";
    
    /**
     * This function accepts a ResultSet and returns a Pokemon from data within the ResultSet
     * @param rs - a ResultSet that contains all the information needed to generate a pokemon object.
     * @return - A Pokemon object generated from the ResultSet
     * @throws SQLException
     */
    public static Pokemon getPokemonInfo(ResultSet rs) throws SQLException {
        Pokemon pokemon = new Pokemon(
                rs.getInt("pokedex_number"), rs.getString("name"),
                rs.getInt("generation"), rs.getString("primary_type"),
                rs.getString("secondary_type"), rs.getInt("hp"),
                rs.getInt("attack"), rs.getInt("defense"),
                rs.getInt("special_attack"), rs.getInt("special_defense"),
                rs.getInt("speed"));
        return pokemon;
    }

    /**
     * This function returns an ArrayList of all pokemon that match a String 'type', ignoring case
     * @param type - the type of pokemon being looked for
     * @return - An ArrayList<Pokemon> containing all pokemon of that type
     */
    public static ArrayList<Pokemon> getAllPokemonByType(String type) {
        return queryPokemon(
                "SELECT * FROM pokemon WHERE primary_type = ? COLLATE NOCASE OR secondary_type = ? COLLATE NOCASE",
                type, type);
    }

    /**
     * This function returns an ArrayList of all pokemon from the specified generation
     * @param generation - the generation of pokemon being looked for
     * @return - An ArrayList<Pokemon> containing all pokemon from that generation
     */
    public static ArrayList<Pokemon> getAllPokemonFromGeneration(int generation) {
        return queryPokemon("SELECT * FROM pokemon WHERE generation = ?", generation);
    }

    /**
     * This function returns an ArrayList of pokemon with the specified name, ignoring case
     * @param name - the full name of the pokemon being looked for
     * @return - An ArrayList<Pokemon> containing all pokemon with that name
     */
    public static ArrayList<Pokemon> getAllPokemonByName(String name) {
        return queryPokemon("SELECT * FROM pokemon WHERE name = ? COLLATE NOCASE", name);
    }

    /**
     * This function returns an ArrayList of pokemon with the specified Pokedex number
     * @param id - the Pokedex number of the pokemon being looked for
     * @return - An ArrayList<Pokemon> containing the matching pokemon, or an empty list if none match
     */
    public static ArrayList<Pokemon> getAllPokemonById(int id) {
        return queryPokemon("SELECT * FROM pokemon WHERE pokedex_number = ?", id);
    }

    /**
     * This function returns an ArrayList of pokemon with a base stat total above the specified value
     * @param bst - the exclusive lower limit for the base stat total
     * @return - An ArrayList<Pokemon> containing all pokemon above that base stat total
     */
    public static ArrayList<Pokemon> getAllPokemonAboveBST(int bst) {
        return queryPokemon("SELECT * FROM pokemon WHERE stat_total > ?", bst);
    }

    /**
     * This function returns an ArrayList of pokemon with the given stat above the specified value
     * @param stat - hp, attack, defense, special_attack, special_defense, or speed, ignoring case;
     *               spaces may be used instead of underscores
     * @param value - the exclusive lower limit for the stat
     * @return - An ArrayList<Pokemon> containing all pokemon above that stat value
     * @throws IllegalArgumentException - if the stat name is null or unsupported
     */
    public static ArrayList<Pokemon> getAllPokemonAboveStat(String stat, int value) {
        if (stat == null) {
            throw new IllegalArgumentException("Stat name must not be null");
        }
        String column = stat.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        switch (column) {
            case "hp":
            case "attack":
            case "defense":
            case "special_attack":
            case "special_defense":
            case "speed":
                return queryPokemon("SELECT * FROM pokemon WHERE " + column + " > ?", value);
            default:
                throw new IllegalArgumentException("Unsupported stat: " + stat);
        }
    }

    /**
     * This function executes a SQL query and returns an ArrayList of pokemon from the results
     * @param sql - the SQL query containing placeholders for the parameters
     * @param parameters - the values to bind to the placeholders in order
     * @return - An ArrayList<Pokemon> containing the pokemon read from the query results
     * @throws IllegalStateException - if the database is missing or the query fails
     */
    private static ArrayList<Pokemon> queryPokemon(String sql, Object... parameters) {
        if (!Files.isRegularFile(Path.of("pokemon.db"))) {
            throw new IllegalStateException("pokemon.db was not found. Run CreateDB from the project root first.");
        }
        ArrayList<Pokemon> response = new ArrayList<Pokemon>();

        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    response.add(getPokemonInfo(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read pokemon.db: " + e.getMessage(), e);
        }

        return response;
    }
}
