/**
 * Stores a Pokémon's National Pokédex identity, types, debut generation, and six base stats.
 */
public class Pokemon {

    private int id;
    private String name;
    private int generation;
    private String primaryType;
    private String secondaryType;
    private int hp;
    private int attack;
    private int defense;
    private int specialAttack;
    private int specialDefense;
    private int speed;
    private int statTotal;

    /**
     * Creates a Pokémon and calculates its base stat total.
     * @param id the National Pokédex number
     * @param name the species name
     * @param generation the debut generation
     * @param primaryType the primary type
     * @param secondaryType the secondary type, or null for a single-type Pokémon
     * @param hp the base hit points
     * @param atk the base attack
     * @param def the base defense
     * @param spatk the base special attack
     * @param spdef the base special defense
     * @param speed the base speed
     */
    public Pokemon(int id, String name, int generation, String primaryType, String secondaryType, int hp, int atk, int def, int spatk, int spdef, int speed) {
        setId(id);
        setName(name);
        setGeneration(generation);
        setPrimaryType(primaryType);
        setSecondaryType(secondaryType);
        setHp(hp);
        setAttack(atk);
        setDefense(def);
        setSpecialAttack(spatk);
        setSpecialDefense(spdef);
        setSpeed(speed);

        setStatTotal();
    }

    /**
     * This function returns a human readable message detailing the pokemon's information
     * @return - A formatted String containing the pokemon's identity, types, and stats
     */
    public String toPrint() {
        String types = getPrimaryType();
        if (getSecondaryType() != null && !getSecondaryType().trim().isEmpty()) {
            types += " / " + getSecondaryType();
        }

        return String.format(
                "%s (Pokedex #%d)%n"
                + "Generation: %d%n"
                + "Type: %s%n"
                + "Stats:%n"
                + "  HP:              %d%n"
                + "  Attack:          %d%n"
                + "  Defense:         %d%n"
                + "  Special Attack:  %d%n"
                + "  Special Defense: %d%n"
                + "  Speed:           %d%n"
                + "  Total:           %d",
                getName(), getId(), getGeneration(), types,
                getHp(), getAttack(), getDefense(), getSpecialAttack(),
                getSpecialDefense(), getSpeed(), getStatTotal());
    }

    /**
     * This function returns the pokemon's Pokedex number and name in a short format
     * @return - A formatted String containing the pokemon's Pokedex number and name
     */
    public String toPrintShort() {
        return "#" + getId() + " " + getName();
    }

    /**
     * Returns a diagnostic representation containing all stored values.
     * @return the Pokémon's field values labeled by their getter names
     */
    @Override
    public String toString() {
        return "Pokemon [getId()=" + getId() + ", getName()=" + getName() + ", getGeneration()=" + getGeneration()
                + ", getPrimaryType()=" + getPrimaryType() + ", getSecondaryType()=" + getSecondaryType() + ", getHp()="
                + getHp() + ", getAttack()=" + getAttack() + ", getDefense()=" + getDefense() + ", getSpecialAttack()="
                + getSpecialAttack() + ", getSpecialDefense()=" + getSpecialDefense() + ", getSpeed()=" + getSpeed()
                + ", getStatTotal()=" + getStatTotal() + "]";
    }

    /// Getters & Setters ///

    /**
     * Returns the National Pokédex number.
     * @return the National Pokédex number
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the National Pokédex number.
     * @param id the National Pokédex number
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the species name.
     * @return the species name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the species name.
     * @param name the species name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the debut generation.
     * @return the debut generation
     */
    public int getGeneration() {
        return generation;
    }

    /**
     * Sets the debut generation.
     * @param generation the debut generation
     */
    public void setGeneration(int generation) {
        this.generation = generation;
    }

    /**
     * Returns the primary type.
     * @return the primary type
     */
    public String getPrimaryType() {
        return primaryType;
    }

    /**
     * Sets the primary type.
     * @param primaryType the primary type
     */
    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    /**
     * Returns the secondary type, which may be null.
     * @return the secondary type, which may be null
     */
    public String getSecondaryType() {
        return secondaryType;
    }

    /**
     * Sets the secondary type, which may be null.
     * @param secondaryType the secondary type, which may be null
     */
    public void setSecondaryType(String secondaryType) {
        this.secondaryType = secondaryType;
    }

    /**
     * Returns the base hit points.
     * @return the base hit points
     */
    public int getHp() {
        return hp;
    }

    /**
     * Sets the base hit points.
     * @param hp the base hit points
     */
    public void setHp(int hp) {
        this.hp = hp;
    }

    /**
     * Returns the base attack.
     * @return the base attack
     */
    public int getAttack() {
        return attack;
    }

    /**
     * Sets the base attack.
     * @param attack the base attack
     */
    public void setAttack(int attack) {
        this.attack = attack;
    }

    /**
     * Returns the base defense.
     * @return the base defense
     */
    public int getDefense() {
        return defense;
    }

    /**
     * Sets the base defense.
     * @param defense the base defense
     */
    public void setDefense(int defense) {
        this.defense = defense;
    }

    /**
     * Returns the base special attack.
     * @return the base special attack
     */
    public int getSpecialAttack() {
        return specialAttack;
    }

    /**
     * Sets the base special attack.
     * @param specialAttack the base special attack
     */
    public void setSpecialAttack(int specialAttack) {
        this.specialAttack = specialAttack;
    }

    /**
     * Returns the base special defense.
     * @return the base special defense
     */
    public int getSpecialDefense() {
        return specialDefense;
    }

    /**
     * Sets the base special defense.
     * @param specialDefense the base special defense
     */
    public void setSpecialDefense(int specialDefense) {
        this.specialDefense = specialDefense;
    }

    /**
     * Returns the base speed.
     * @return the base speed
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Sets the base speed.
     * @param speed the base speed
     */
    public void setSpeed(int speed) {
        this.speed = speed;
    }

    /**
     * Returns the stored sum of the six base stats.
     * @return stored sum of the six base stats.
     */
    public int getStatTotal() {
        return statTotal;
    }

    /** Recalculates the stored total from the current six base stats. */
    public void setStatTotal() {
        this.statTotal = this.getHp() + this.getAttack() + this.getDefense() + this.getSpecialAttack() + this.getSpecialDefense() + this.getSpeed();
    }


}
