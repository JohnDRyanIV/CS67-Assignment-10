# Pokemon Search

## 1. Project Description

This project uses a SQLite database of Pokemon through Generation IX. Each Pokemon
has a unique Pokedex number, name, generation, one or two types, six base stats,
and a base stat total (BST).

The program lets users explore the database through a repeating console menu.
PokemonDriver reads the user's choices, DBAPI queries the database, and
Pokemon formats the results for display. Users can search repeatedly before
choosing to exit.

The API can answer the following questions:

- Which Pokemon have a particular primary or secondary type?
- Which Pokemon come from a specified generation?
- Which Pokemon matches a full name?
- Which Pokemon has a specified Pokedex number?
- Which Pokemon have a BST greater than a given value?
- Which Pokemon have HP, Attack, Defense, Special Attack, Special Defense, or
  Speed greater than a given value?

Name, type, and stat-name searches ignore case. Threshold searches use a
greater-than comparison, so Pokemon equal to the supplied value are excluded.

## 2. Example Test Run

```text
Find Pokemon by:
1. Type
2. Generation
3. Name
4. Pokedex number
5. Base stat total above a value
6. Individual stat above a value
0. Exit
Choose an option: 6
Enter a stat (HP, Attack, Defense, Special Attack, Special Defense, Speed): hp
Find Pokemon with that stat greater than: 200

Found 3 matching Pokemon:
Chansey (Pokedex #113)
Generation: 1
Type: Normal
Stats:
  HP:              250
  Attack:          5
  Defense:         5
  Special Attack:  35
  Special Defense: 105
  Speed:           50
  Total:           450

Blissey (Pokedex #242)
Generation: 2
Type: Normal
Stats:
  HP:              255
  Attack:          10
  Defense:         10
  Special Attack:  75
  Special Defense: 135
  Speed:           55
  Total:           540

Guzzlord (Pokedex #799)
Generation: 7
Type: Dark / Dragon
Stats:
  HP:              223
  Attack:          101
  Defense:         53
  Special Attack:  97
  Special Defense: 53
  Speed:           43
  Total:           570


Find Pokemon by:
1. Type
2. Generation
3. Name
4. Pokedex number
5. Base stat total above a value
6. Individual stat above a value
0. Exit
Choose an option: 3
Enter the full Pokemon name: lucario

Found 1 matching Pokemon:
Lucario (Pokedex #448)
Generation: 4
Type: Fighting / Steel
Stats:
  HP:              70
  Attack:          110
  Defense:         70
  Special Attack:  115
  Special Defense: 70
  Speed:           90
  Total:           525


Find Pokemon by:
1. Type
2. Generation
3. Name
4. Pokedex number
5. Base stat total above a value
6. Individual stat above a value
0. Exit
Choose an option: 5
Find Pokemon with a base stat total greater than: 700

Found 1 matching Pokemon:
Arceus (Pokedex #493)
Generation: 4
Type: Normal
Stats:
  HP:              120
  Attack:          120
  Defense:         120
  Special Attack:  120
  Special Defense: 120
  Speed:           120
  Total:           720


Find Pokemon by:
1. Type
2. Generation
3. Name
4. Pokedex number
5. Base stat total above a value
6. Individual stat above a value
0. Exit
Choose an option: 0
Goodbye!
```

## 3. AI Usage Statement

I used ChatGPT Codex to help write SQL queries and refactor repeated database query code into a single reusable function. I reviewed the generated code and tested the queries through the program’s menu to check that they returned the expected Pokémon.

I used ChatGPT to import and create a pokemon dataset. The dataset uses pokedex number as key, and includes the name of the pokemon, the type(s) of the pokemon, and the base stats and stat total of the pokemon for all exsting pokemon through generation IX. ChatGPT Codex also posted what operations it performed to the README.

The below portion of this markdown file was ere generated with AI, as it was creating the pokemon database.


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
