import java.util.ArrayList;
import java.util.Scanner;

/** Provides an interactive console menu for searching the local Pokémon database. */
public class PokemonDriver {

    /**
     * Repeats searches until the user enters 0 or exit, ignoring case for exit.
     * Prints full details for up to five matches and short entries for larger results.
     * Invalid numeric input and unsupported stat names are reported before retrying.
     */
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        String choice; // user input
        ArrayList<Pokemon> results = new ArrayList<Pokemon>();

        do { // sentinel value is '0' or 'exit'
            System.out.println("\nFind Pokemon by:");
            System.out.println("1. Type");
            System.out.println("2. Generation");
            System.out.println("3. Name");
            System.out.println("4. Pokedex number");
            System.out.println("5. Base stat total above a value");
            System.out.println("6. Individual stat above a value");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            choice = input.nextLine().trim();

            try {
                switch (choice.toLowerCase()) {
                case "1": // Get pokemon by type
                    System.out.print("Enter a type: ");
                    String type = input.nextLine().trim();
                    results = DBAPI.getAllPokemonByType(type);
                    break;
                case "2": // Get pokemon from generation
                    System.out.print("Enter a generation: ");
                    int generation = Integer.parseInt(input.nextLine().trim());
                    results = DBAPI.getAllPokemonFromGeneration(generation);
                    break;
                case "3": // get pokemon by name
                    System.out.print("Enter the full Pokemon name: ");
                    String name = input.nextLine().trim();
                    results = DBAPI.getAllPokemonByName(name);
                    break;
                case "4": // get pokemon by id
                    System.out.print("Enter a Pokedex number: ");
                    int id = Integer.parseInt(input.nextLine().trim());
                    results = DBAPI.getAllPokemonById(id);
                    break;
                case "5": // get pokemon with a base stat total greater than 'bst'
                    System.out.print("Find Pokemon with a base stat total greater than: ");
                    int bst = Integer.parseInt(input.nextLine().trim());
                    results = DBAPI.getAllPokemonAboveBST(bst);
                    break;
                case "6": // get pokemon with a single stat greater than 'stat'
                    System.out.print("Enter a stat (HP, Attack, Defense, Special Attack, Special Defense, Speed): ");
                    String stat = input.nextLine().trim();
                    System.out.print("Find Pokemon with that stat greater than: ");
                    int value = Integer.parseInt(input.nextLine().trim());
                    results = DBAPI.getAllPokemonAboveStat(stat, value);
                    break;
                // if input is either sentinel value
                case "0":
                    continue;
                case "exit":
                    continue;
                default: // if input is invalid
                    System.out.println("Please choose an option from 0 to 6.");
                    continue;
                }
                
                if (results.isEmpty()) {
                    System.out.println("No matching Pokemon found.");
                } else {
                    System.out.println("\nFound " + results.size() + " matching Pokemon:");
                    for (Pokemon pokemon : results) {
                        if (results.size() > 5) { // if there are more than 5 pokemon in the arraylist, print in shorthand
                            System.out.println(pokemon.toPrintShort());
                        } else { // if there are 5 or less pokemon in arraylist, print longhand.
                            System.out.println(pokemon.toPrint());
                            System.out.println();
                        }
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number for numeric searches.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        } while (!choice.equals("0") && !choice.equalsIgnoreCase("exit"));

        System.out.println("Goodbye!");
        input.close();
    }
}

