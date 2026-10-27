import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/** Creates and populates a SQLite Pokémon database using the bundled offline data. */
public class CreateDB {
    private static final String HEADER = "pokedex_number\tname\tgeneration\tprimary_type\t"
            + "secondary_type\thp\tattack\tdefense\tspecial_attack\tspecial_defense\tspeed\tstat_total";

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS pokemon (
                pokedex_number INTEGER PRIMARY KEY CHECK (pokedex_number > 0),
                name TEXT NOT NULL CHECK (length(trim(name)) > 0),
                generation INTEGER NOT NULL CHECK (generation > 0),
                primary_type TEXT NOT NULL CHECK (length(trim(primary_type)) > 0),
                secondary_type TEXT CHECK (secondary_type IS NULL OR length(trim(secondary_type)) > 0),
                hp INTEGER NOT NULL CHECK (hp > 0),
                attack INTEGER NOT NULL CHECK (attack > 0),
                defense INTEGER NOT NULL CHECK (defense > 0),
                special_attack INTEGER NOT NULL CHECK (special_attack > 0),
                special_defense INTEGER NOT NULL CHECK (special_defense > 0),
                speed INTEGER NOT NULL CHECK (speed > 0),
                stat_total INTEGER NOT NULL CHECK (
                    stat_total = hp + attack + defense + special_attack + special_defense + speed
                )
            )
            """;

    private static final String UPSERT = """
            INSERT INTO pokemon (
                pokedex_number, name, generation, primary_type, secondary_type,
                hp, attack, defense, special_attack, special_defense, speed, stat_total
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(pokedex_number) DO UPDATE SET
                name = excluded.name,
                generation = excluded.generation,
                primary_type = excluded.primary_type,
                secondary_type = excluded.secondary_type,
                hp = excluded.hp,
                attack = excluded.attack,
                defense = excluded.defense,
                special_attack = excluded.special_attack,
                special_defense = excluded.special_defense,
                speed = excluded.speed,
                stat_total = excluded.stat_total
            """;

    /**
     * Runs the offline import; run from the project root to use the default paths.
     * @param args optional database path (default pokemon.db), followed by the TSV
     *             data path (default data/pokemon.tsv)
     * @throws IOException if the data cannot be read or has an invalid format
     * @throws SQLException if the database cannot be opened or updated
     * @throws IllegalArgumentException if more than two arguments are supplied
     */
    public static void main(String[] args) throws IOException, SQLException {
        if (args.length > 2) {
            throw new IllegalArgumentException("Usage: CreateDB [database-file] [pokemon-tsv-file]");
        }
        Path database = Path.of(args.length > 0 ? args[0] : "pokemon.db");
        Path data = Path.of(args.length > 1 ? args[1] : "data/pokemon.tsv");
        int count = createDatabase(database, data);
        System.out.println("Loaded " + count + " Pokémon into " + database.toAbsolutePath());
    }

    /**
     * Inserts or updates species from a UTF-8 TSV file in one transaction.
     * Creates the table if needed and retains existing species absent from the file.
     * An empty secondary-type field is stored as SQL NULL. Import failures roll back
     * the transaction, including table creation, without committing partial data.
     * @param database the destination SQLite database path
     * @param data the TSV file with the expected header and twelve fields per row
     * @return the number of data rows processed, including updates to existing species
     * @throws IOException if reading fails, the header or field count is invalid,
     *                     a numeric field cannot be parsed, or no data rows exist
     * @throws SQLException if a database operation or constraint check fails
     */
    public static int createDatabase(Path database, Path data) throws IOException, SQLException {
        try (BufferedReader reader = Files.newBufferedReader(data, StandardCharsets.UTF_8)) {
            if (!HEADER.equals(reader.readLine())) {
                throw new IOException("Unexpected Pokémon data header in " + data);
            }
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath())) {
                connection.setAutoCommit(false);
                try {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(CREATE_TABLE);
                    }
                    int count = 0;
                    try (PreparedStatement insert = connection.prepareStatement(UPSERT)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] fields = line.split("\t", -1);
                            int lineNumber = count + 2;
                            if (fields.length != 12) {
                                throw new IOException("Expected 12 fields at " + data + ":" + lineNumber);
                            }
                            try {
                                insert.setInt(1, Integer.parseInt(fields[0]));
                                insert.setString(2, fields[1]);
                                insert.setInt(3, Integer.parseInt(fields[2]));
                                insert.setString(4, fields[3]);
                                if (fields[4].isEmpty()) {
                                    insert.setNull(5, Types.VARCHAR);
                                } else {
                                    insert.setString(5, fields[4]);
                                }
                                for (int i = 5; i < fields.length; i++) {
                                    insert.setInt(i + 1, Integer.parseInt(fields[i]));
                                }
                            } catch (NumberFormatException exception) {
                                throw new IOException("Invalid number at " + data + ":" + lineNumber, exception);
                            }
                            insert.addBatch();
                            count++;
                        }
                        if (count == 0) {
                            throw new IOException("No Pokémon found in " + data);
                        }
                        insert.executeBatch();
                    }
                    connection.commit();
                    return count;
                } catch (IOException | SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        }
    }
}
