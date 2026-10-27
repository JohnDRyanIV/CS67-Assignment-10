import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import java.util.ArrayList;
import java.util.Locale;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

/** A JavaFX desktop interface for the Pokemon database, styled with AtlantaFX. */
public class PokemonGUI extends Application {

    private final ObservableList<Pokemon> results = FXCollections.observableArrayList();
    private final TableView<Pokemon> table = new TableView<>(results);
    private final ComboBox<String> searchMode = new ComboBox<>();
    private final ComboBox<String> typeInput = new ComboBox<>();
    private final ComboBox<Integer> generationInput = new ComboBox<>();
    private final ComboBox<String> statInput = new ComboBox<>();
    private final TextField valueInput = new TextField();
    private final VBox form = new VBox(10);
    private final VBox detailContent = new VBox(12);
    private final Label formError = label("", "form-error");
    private final Label status = label("Ready to explore.", "status-label");
    private final Label resultCount = label("0 matches", "count-badge");
    private final Label resultSummary = label("Choose a search to get started.", "muted-label");
    private final Label emptyTitle = label("Your next discovery starts here", "empty-title");
    private final Label emptyMessage = label("Choose a search on the left.", "empty-text");
    private final ProgressIndicator loading = new ProgressIndicator();
    private VBox searchCard;
    private boolean searching;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(22, 26, 16, 26));
        status.setId("searchStatus");
        resultCount.setId("resultCount");
        root.setTop(createHeader(root));

        searchCard = createSearchCard();
        VBox resultsCard = createResultsCard();
        VBox detailCard = createDetailCard();
        HBox body = new HBox(18, searchCard, resultsCard, detailCard);
        HBox.setHgrow(resultsCard, Priority.ALWAYS);
        root.setCenter(body);
        BorderPane.setMargin(body, new Insets(22, 0, 18, 0));

        loading.setPrefSize(16, 16);
        loading.setMaxSize(16, 16);
        loading.setVisible(false);
        loading.managedProperty().bind(loading.visibleProperty());
        HBox footer = new HBox(10, loading, status, spacer(),
                label("LOCAL DATABASE  /  GENERATIONS I - IX", "section-kicker"));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("footer");
        root.setBottom(footer);

        Scene scene = new Scene(root, 1280, 850);
        scene.getStylesheets().add(PokemonGUI.class.getResource("/pokemon-gui.css").toExternalForm());
        stage.setTitle("Pokemon Explorer");
        stage.setMinWidth(1120);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
        search();
    }

    private VBox createHeader(BorderPane root) {
        StackPane mark = new StackPane();
        Circle outer = new Circle(21, Color.web("#127568"));
        Line middle = new Line(-20, 0, 20, 0);
        middle.setStroke(Color.WHITE);
        middle.setStrokeWidth(3);
        mark.getChildren().addAll(outer, middle,
                new Circle(7, Color.WHITE), new Circle(3, Color.web("#127568")));
        mark.getStyleClass().add("brand-mark");

        VBox brand = new VBox(2, label("Pokemon Explorer", "brand-title"),
                label("A field guide for the curious", "brand-subtitle"));
        ToggleButton theme = new ToggleButton("Dark mode");
        theme.setId("themeToggle");
        theme.getStyleClass().add("theme-toggle");
        theme.setOnAction(event -> {
            boolean dark = theme.isSelected();
            Application.setUserAgentStylesheet(dark
                    ? new PrimerDark().getUserAgentStylesheet()
                    : new PrimerLight().getUserAgentStylesheet());
            root.getStyleClass().remove("dark");
            if (dark) {
                root.getStyleClass().add("dark");
            }
            theme.setText(dark ? "Light mode" : "Dark mode");
        });
        HBox topbar = new HBox(12, mark, brand, spacer(), theme);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");

        VBox introduction = new VBox(8,
                label("THE NATIONAL POKEDEX", "eyebrow"),
                label("Big world. Small discoveries.", "hero-title"),
                label("Find a favorite, explore a type, or follow the stats.", "hero-subtitle"));
        Label chip = label("1,025 SPECIES\n9 GENERATIONS", "hero-chip");
        HBox hero = new HBox(20, introduction, spacer(), chip);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("hero-banner");
        return new VBox(18, topbar, hero);
    }

    private VBox createSearchCard() {
        searchMode.getItems().addAll("Type", "Generation", "Name", "Pokedex number",
                "Base stat total", "Individual stat");
        searchMode.setMaxWidth(Double.MAX_VALUE);
        searchMode.setId("searchMode");
        typeInput.getItems().addAll("Normal", "Fire", "Water", "Electric", "Grass", "Ice",
                "Fighting", "Poison", "Ground", "Flying", "Psychic", "Bug", "Rock",
                "Ghost", "Dragon", "Dark", "Steel", "Fairy");
        typeInput.setValue("Fire");
        typeInput.setId("typeInput");
        typeInput.setMaxWidth(Double.MAX_VALUE);
        for (int i = 1; i <= 9; i++) {
            generationInput.getItems().add(i);
        }
        generationInput.setValue(1);
        generationInput.setId("generationInput");
        generationInput.setMaxWidth(Double.MAX_VALUE);
        statInput.getItems().addAll("HP", "Attack", "Defense", "Special Attack", "Special Defense", "Speed");
        statInput.setValue("HP");
        statInput.setId("statInput");
        statInput.setMaxWidth(Double.MAX_VALUE);
        valueInput.setId("searchValue");
        formError.setWrapText(true);
        formError.setVisible(false);
        formError.managedProperty().bind(formError.visibleProperty());

        Button searchButton = new Button("Search Pokemon");
        searchButton.getStyleClass().add("primary-button");
        searchButton.setMaxWidth(Double.MAX_VALUE);
        searchButton.setDefaultButton(true);
        searchButton.setId("searchButton");
        searchButton.setOnAction(event -> search());

        Button resetButton = new Button("Reset search");
        resetButton.setId("resetButton");
        resetButton.getStyleClass().add("secondary-button");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setOnAction(event -> {
            typeInput.setValue("Fire");
            generationInput.setValue(1);
            statInput.setValue("HP");
            searchMode.getSelectionModel().select(0);
            configureForm();
            search();
        });

        Label tip = label("A little tip\nNames ignore case. Stat searches find values strictly above your number.", "helper-text");
        tip.setWrapText(true);
        VBox fields = new VBox(12, fieldLabel("Search by", searchMode), searchMode,
                form, tip);
        fields.setPadding(new Insets(0, 4, 8, 0));
        ScrollPane fieldsScroll = new ScrollPane(fields);
        fieldsScroll.setFitToWidth(true);
        fieldsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fieldsScroll.getStyleClass().add("detail-scroll");
        VBox.setVgrow(fieldsScroll, Priority.ALWAYS);
        VBox card = new VBox(16, label("MAKE A DISCOVERY", "section-kicker"),
                label("Find Pokemon", "card-title"), fieldsScroll, formError, searchButton, resetButton);
        card.getStyleClass().addAll("card", "search-card");
        card.setPrefWidth(240);
        card.setMinWidth(240);
        card.setMaxWidth(240);
        searchMode.valueProperty().addListener((observable, before, after) -> configureForm());
        searchMode.getSelectionModel().selectFirst();
        return card;
    }

    private void configureForm() {
        form.getChildren().clear();
        formError.setVisible(false);
        switch (searchMode.getSelectionModel().getSelectedIndex()) {
            case 0:
                form.getChildren().addAll(fieldLabel("Pokemon type", typeInput), typeInput,
                        hint("Matches either primary or secondary type."));
                break;
            case 1:
                form.getChildren().addAll(fieldLabel("Generation", generationInput), generationInput,
                        hint("Explore species introduced in a generation."));
                break;
            case 2:
                valueInput.setText("Lucario");
                valueInput.setPromptText("e.g. Lucario");
                form.getChildren().addAll(fieldLabel("Full Pokemon name", valueInput), valueInput,
                        hint("Use the full name. Uppercase or lowercase both work."));
                break;
            case 3:
                valueInput.setText("25");
                valueInput.setPromptText("e.g. 25");
                form.getChildren().addAll(fieldLabel("Pokedex number", valueInput), valueInput,
                        hint("Look up a species by its National Pokedex number."));
                break;
            case 4:
                valueInput.setText("500");
                valueInput.setPromptText("e.g. 500");
                form.getChildren().addAll(fieldLabel("Base stat total above", valueInput), valueInput,
                        hint("BST is the sum of all six base stats."));
                break;
            case 5:
                valueInput.setText("100");
                valueInput.setPromptText("e.g. 100");
                form.getChildren().addAll(fieldLabel("Stat", statInput), statInput,
                        fieldLabel("Stat value above", valueInput), valueInput,
                        hint("Find species with a stat greater than this value."));
                break;
            default:
                break;
        }
    }

    private VBox createResultsCard() {
        TableColumn<Pokemon, Number> number = new TableColumn<>("#");
        number.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().getId()));
        number.setPrefWidth(58);
        TableColumn<Pokemon, String> name = new TableColumn<>("Pokemon");
        name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        name.setPrefWidth(148);
        TableColumn<Pokemon, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(cell -> new ReadOnlyStringWrapper(types(cell.getValue())));
        type.setPrefWidth(154);
        type.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : typeBadges(item));
            }
        });
        TableColumn<Pokemon, Number> generation = new TableColumn<>("Gen.");
        generation.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().getGeneration()));
        generation.setPrefWidth(58);
        TableColumn<Pokemon, Number> total = new TableColumn<>("BST");
        total.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().getStatTotal()));
        total.setPrefWidth(66);
        table.getColumns().add(number);
        table.getColumns().add(name);
        table.getColumns().add(type);
        table.getColumns().add(generation);
        table.getColumns().add(total);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(49);
        table.getStyleClass().add("results-table");
        table.setId("resultsTable");
        VBox empty = new VBox(10, emptyTitle, emptyMessage);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(20));
        emptyMessage.setWrapText(true);
        emptyMessage.setMaxWidth(280);
        table.setPlaceholder(empty);
        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, before, after) -> showDetails(after));
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox title = new HBox(10, label("Your discoveries", "result-title"), spacer(), resultCount);
        title.setAlignment(Pos.CENTER_LEFT);
        resultSummary.setWrapText(true);
        VBox card = new VBox(12, title, resultSummary, table,
                label("Click a column to sort. Select a Pokemon to see its stats.", "helper-text"));
        card.getStyleClass().add("card");
        card.setMinWidth(430);
        return card;
    }

    private VBox createDetailCard() {
        ScrollPane scroll = new ScrollPane(detailContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("detail-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox card = new VBox(16, label("IN THE SPOTLIGHT", "section-kicker"), scroll);
        card.getStyleClass().addAll("card", "detail-card");
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setMaxWidth(280);
        showDetails(null);
        return card;
    }

    private void showDetails(Pokemon pokemon) {
        detailContent.getChildren().clear();
        if (pokemon == null) {
            Label message = label("Select a Pokemon to explore its types and base stats.", "empty-text");
            message.setWrapText(true);
            detailContent.getChildren().addAll(label("Get to know a Pokemon", "empty-title"), message);
            return;
        }

        Label name = label(pokemon.getName(), "detail-name");
        name.setId("detailName");
        name.setWrapText(true);
        detailContent.getChildren().addAll(
                label(String.format("POKEDEX #%04d", pokemon.getId()), "dex-label"),
                name, typeBadges(types(pokemon)),
                label("Generation " + pokemon.getGeneration(), "generation-label"),
                label("BASE STATS", "section-kicker"));
        String[] names = {"HP", "Attack", "Defense", "Sp. Attack", "Sp. Defense", "Speed"};
        int[] values = {pokemon.getHp(), pokemon.getAttack(), pokemon.getDefense(),
                pokemon.getSpecialAttack(), pokemon.getSpecialDefense(), pokemon.getSpeed()};
        for (int i = 0; i < names.length; i++) {
            HBox caption = new HBox(label(names[i], "stat-name"), spacer(),
                    label(Integer.toString(values[i]), "stat-value"));
            ProgressBar bar = new ProgressBar(values[i] / 255.0);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("stat-bar");
            bar.setAccessibleText(names[i] + ": " + values[i]);
            detailContent.getChildren().add(new VBox(5, caption, bar));
        }
        HBox total = new HBox(label("Base stat total", "bst-label"), spacer(),
                label(Integer.toString(pokemon.getStatTotal()), "bst-value"));
        total.setAlignment(Pos.CENTER_LEFT);
        total.getStyleClass().add("bst-box");
        detailContent.getChildren().add(total);
    }

    /** Runs searches in the background so the window remains responsive. */
    private void search() {
        if (searching) {
            return;
        }
        int mode = searchMode.getSelectionModel().getSelectedIndex();
        String text = mode == 0 ? typeInput.getValue() : valueInput.getText().trim();
        int value = 0;
        formError.setVisible(false);
        if (mode == 2 && text.isEmpty()) {
            showFormError("Enter a Pokemon's full name.");
            return;
        }
        try {
            if (mode == 1) {
                value = generationInput.getValue();
            } else if (mode >= 3) {
                value = Integer.parseInt(text);
                if (value < 0 || (mode == 3 && value == 0)) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException e) {
            showFormError(mode == 3 ? "Enter a Pokedex number greater than zero."
                    : "Enter a whole number of zero or greater (up to 2147483647).");
            return;
        }

        final int queryValue = value;
        final String stat = statInput.getValue();
        String description = switch (mode) {
            case 0 -> text + " type - either primary or secondary";
            case 1 -> "Species introduced in Generation " + value;
            case 2 -> "Full name: " + text + " (ignoring case)";
            case 3 -> "National Pokedex #" + value;
            case 4 -> "Base stat total greater than " + value;
            default -> stat + " greater than " + value;
        };

        results.clear();
        showDetails(null);
        resultCount.setText("Searching");
        resultSummary.setText(description);
        emptyTitle.setText("Searching the Pokedex...");
        emptyMessage.setText("Your discoveries will appear here.");
        setBusy(true);
        status.getStyleClass().remove("status-error");
        status.setText("Searching the local database...");

        Task<ArrayList<Pokemon>> task = new Task<>() {
            @Override
            protected ArrayList<Pokemon> call() {
                return switch (mode) {
                    case 0 -> DBAPI.getAllPokemonByType(text);
                    case 1 -> DBAPI.getAllPokemonFromGeneration(queryValue);
                    case 2 -> DBAPI.getAllPokemonByName(text);
                    case 3 -> DBAPI.getAllPokemonById(queryValue);
                    case 4 -> DBAPI.getAllPokemonAboveBST(queryValue);
                    default -> DBAPI.getAllPokemonAboveStat(stat, queryValue);
                };
            }
        };
        task.setOnSucceeded(event -> {
            results.setAll(task.getValue());
            table.sort();
            int count = results.size();
            resultCount.setText(count + (count == 1 ? " match" : " matches"));
            emptyTitle.setText("No Pokemon found");
            emptyMessage.setText("Try another name, type, or lower stat threshold.");
            status.setText(count == 0 ? "No matches. Try a different search."
                    : "Search complete. Select a Pokemon to explore its stats.");
            if (count > 0) {
                table.getSelectionModel().selectFirst();
            }
            setBusy(false);
        });
        task.setOnFailed(event -> {
            resultCount.setText("Search failed");
            emptyTitle.setText("Could not open the database");
            emptyMessage.setText("Check the message below, then try again.");
            status.getStyleClass().add("status-error");
            status.setText(task.getException().getMessage());
            setBusy(false);
        });
        Thread worker = new Thread(task, "pokemon-search");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy) {
        searching = busy;
        searchCard.setDisable(busy);
        loading.setVisible(busy);
    }

    private void showFormError(String message) {
        formError.setText(message);
        formError.setVisible(true);
        valueInput.requestFocus();
    }

    private static String types(Pokemon pokemon) {
        String secondary = pokemon.getSecondaryType();
        return pokemon.getPrimaryType()
                + (secondary == null || secondary.isBlank() ? "" : " / " + secondary);
    }

    private static HBox typeBadges(String types) {
        HBox badges = new HBox(5);
        badges.setAlignment(Pos.CENTER_LEFT);
        for (String type : types.split(" / ")) {
            Label badge = label(type, "type-badge");
            badge.getStyleClass().add("type-" + type.toLowerCase(Locale.ROOT));
            badges.getChildren().add(badge);
        }
        return badges;
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private static Label fieldLabel(String text, Node control) {
        Label label = label(text, "field-label");
        label.setLabelFor(control);
        return label;
    }

    private static Label hint(String text) {
        Label label = label(text, "helper-text");
        label.setWrapText(true);
        return label;
    }

    private static Region spacer() {
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        return space;
    }
}
