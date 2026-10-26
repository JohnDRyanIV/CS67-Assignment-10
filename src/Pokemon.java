public class Pokemon {

    private int id;
    private String name;
    private String primaryType;
    private String secondaryType;
    private int hp;
    private int attack;
    private int defense;
    private int specialAttack;
    private int specialDefense;
    private int speed;
    private int statTotal;

    

    public Pokemon(int id, String name, String primaryType, String secondaryType, int hp, int atk, int def, int spatk, int spdef, int speed) {
        setId(id);
        setName(name);
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


    /// Getters & Setters ///

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    public String getSecondaryType() {
        return secondaryType;
    }

    public void setSecondaryType(String secondaryType) {
        this.secondaryType = secondaryType;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public int getSpecialAttack() {
        return specialAttack;
    }

    public void setSpecialAttack(int specialAttack) {
        this.specialAttack = specialAttack;
    }

    public int getSpecialDefense() {
        return specialDefense;
    }

    public void setSpecialDefense(int specialDefense) {
        this.specialDefense = specialDefense;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getStatTotal() {
        return statTotal;
    }

    public void setStatTotal() {
        this.statTotal = this.getHp() + this.getAttack() + this.getDefense() + this.getSpecialAttack() + this.getSpecialDefense() + this.getSpeed();
    }


}
