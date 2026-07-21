// File: src/main/java/com/hotel/Main.java
package com.hotel;

import com.hotel.service.AuthService;
import com.hotel.service.HotelService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage        primaryStage;
    private HotelService hotelService = new HotelService();
    private AuthService  authService  = new AuthService();

    /** Tracks current theme; shared with all UI screens. */
    private boolean darkMode = false;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Hotel Management System");
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        showLoginScreen();
        primaryStage.show();
    }

    // ── Login Screen ──────────────────────────────────────────────────────────────

    public void showLoginScreen() {
        // Full-screen container
        BorderPane outerRoot = new BorderPane();
        outerRoot.getStyleClass().add("root-pane");

        // Header bar
        HBox header = new HBox();
        header.getStyleClass().add("top-bar");
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Text appTitle = new Text("Hotel Management System");
        appTitle.getStyleClass().add("heading-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Dark mode toggle — functional before scene is fully set
        Button darkToggleBtn = new Button(darkMode ? "Light Mode" : "Dark Mode");
        darkToggleBtn.getStyleClass().add("secondary-button");
        darkToggleBtn.setOnAction(e -> {
            darkMode = !darkMode;
            darkToggleBtn.setText(darkMode ? "Light Mode" : "Dark Mode");
            applyCSS(outerRoot.getScene());
        });

        header.getChildren().addAll(appTitle, spacer, darkToggleBtn);
        outerRoot.setTop(header);

        // Login card centered
        VBox card = new VBox(14);
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("login-card");
        card.setPadding(new Insets(36, 44, 36, 44));
        card.setMaxWidth(360);

        Text cardTitle = new Text("Sign In");
        cardTitle.getStyleClass().add("login-title");

        Text subtitle = new Text("Enter your credentials to continue");
        subtitle.getStyleClass().add("subtitle-text");

        Label userLbl = new Label("Username");
        userLbl.getStyleClass().add("form-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("input-field");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("form-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("input-field");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);

        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("primary-button");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Runnable doLogin = () -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Please enter username and password.");
                return;
            }

            String role = authService.authenticate(user, pass);
            if (role == null) {
                errorLabel.setText("Invalid credentials. Please try again.");
            } else if (role.equals("admin")) {
                new AdminUI(primaryStage, hotelService, this).show();
            } else {
                new ReceptionUI(primaryStage, hotelService, this).show();
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passwordField.setOnAction(e -> doLogin.run());

        VBox hintBox = new VBox(5);
        hintBox.setAlignment(Pos.CENTER_LEFT);
        hintBox.getStyleClass().add("hint-box");
        hintBox.setPadding(new Insets(12, 16, 12, 16));
        Label hintTitle = new Label("Demo Credentials");
        hintTitle.setStyle("-fx-font-weight: bold;");
        hintBox.getChildren().addAll(
            hintTitle,
            new Label("Admin:        admin / 1234"),
            new Label("Receptionist: user  / 1111")
        );

        card.getChildren().addAll(
            cardTitle, subtitle,
            userLbl, usernameField,
            passLbl, passwordField,
            errorLabel, loginBtn,
            hintBox
        );

        StackPane center = new StackPane(card);
        center.setAlignment(Pos.CENTER);
        outerRoot.setCenter(center);

        Scene scene = new Scene(outerRoot, 1024, 700);
        applyCSS(scene);
        primaryStage.setScene(scene);
    }

    // ── CSS / Theme Helpers ───────────────────────────────────────────────────────

    /** Apply current theme to a scene. */
    public void applyCSS(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String sheet = darkMode ? "/dark.css" : "/style.css";
        try {
            scene.getStylesheets().add(
                getClass().getResource(sheet).toExternalForm());
        } catch (Exception e) {
            System.err.println("Warning: " + sheet + " not found.");
        }
    }

    /** Toggle between light and dark theme and re-apply to the given scene. */
    public void toggleTheme(Scene scene) {
        darkMode = !darkMode;
        applyCSS(scene);
    }

    public boolean isDarkMode() { return darkMode; }

    public static void main(String[] args) { launch(args); }
}
