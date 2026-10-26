I used ChatGPT Codex within Visual Studio IDE to import and create a pokemon dataset. The dataset uses pokedex number as key, and includes the name of the pokemon, the type(s) of the pokemon, and the base stats and stat total of the pokemon for all exsting pokemon through generation IX. ChatGPT Codex also posted what operations it performed to the README.


# Pokémon database

`CreateDB` creates a SQLite database with a `pokemon` table and loads the bundled
`data/pokemon.tsv` file. It makes no API calls and needs no internet connection.

From the project root, with Java 17 or newer:

```sh
javac -encoding UTF-8 -d bin src/CreateDB.java
java -cp 'bin:lib/*' CreateDB
```

The default output is `pokemon.db`. To choose another database or data file:

```sh
java -cp 'bin:lib/*' CreateDB /tmp/pokemon.db data/pokemon.tsv
```

The SQLite JDBC driver is included in `lib/`. On Windows, use `bin;lib/*` for the
classpath. Run from the project root so the default data path resolves correctly.

Each row contains a National Pokédex number (the primary key), English species
name, debut generation, required primary type, optional secondary type (`NULL`
for single-type Pokémon), six base stats, and their sum in `stat_total`. These
are base stats, not calculated stats for a particular level or individual.
Rerunning the program updates matching Pokédex numbers without creating duplicates.
Loading happens in one transaction, so a failed import rolls back its changes.

```sql
SELECT pokedex_number, name, primary_type, secondary_type, stat_total
FROM pokemon
ORDER BY pokedex_number;
```

The bundled snapshot includes 1,025 species, #1 Bulbasaur through #1025 Pecharunt,
across Generations I–IX. It was assembled from the
[PokeAPI source data](https://github.com/PokeAPI/pokeapi/tree/master/data/v2/csv)
on October 22, 2026, using `pokemon_species.csv`, `pokemon_species_names.csv`
(English), `pokemon.csv` (default varieties), `pokemon_types.csv`, `types.csv`,
and `pokemon_stats.csv`. Each species uses its default variety's current types
and base stats. Alternate forms, regional forms, and Mega Evolutions do not get
separate rows because they share a National Pokédex number. The snapshot is
local and does not automatically acquire future species or stat changes.
