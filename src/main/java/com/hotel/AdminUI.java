// File: src/main/java/com/hotel/AdminUI.java
package com.hotel;

import com.hotel.model.Room;
import com.hotel.service.HotelService;
import com.hotel.util.Validator;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminUI {

    private final Stage        stage;
    private final HotelService hotelService;
    private final Main         mainApp;
    private Scene              scene;

    // Shared rooms table — refreshed from anywhere
    private TableView<Room> roomsTable;

    // The main content area swapped by nav buttons
    private BorderPane contentArea;

    // Track active nav button
    private Button activeNavBtn = null;

    public AdminUI(Stage stage, HotelService hotelService, Main mainApp) {
        this.stage        = stage;
        this.hotelService = hotelService;
        this.mainApp      = mainApp;
    }

    // ── Build & Show ──────────────────────────────────────────────────────────────

    public void show() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // Header
        root.setTop(buildHeader());

        // Left nav + main content side by side
        HBox body = new HBox();
        body.getStyleClass().add("body-area");
        VBox.setVgrow(body, Priority.ALWAYS);

        // Navigation sidebar
        VBox navBar = buildNavBar();

        // Content area
        contentArea = new BorderPane();
        contentArea.getStyleClass().add("content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        body.getChildren().addAll(navBar, contentArea);
        root.setCenter(body);

        // Show dashboard by default
        showDashboard();

        scene = new Scene(root);
        mainApp.applyCSS(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
    }

    // ── Header ────────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.getStyleClass().add("top-bar");

        Text heading = new Text("Hotel Management System");
        heading.getStyleClass().add("heading-text");

        Label roleTag = new Label("Admin");
        roleTag.getStyleClass().add("role-tag");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button themeBtn = new Button(mainApp.isDarkMode() ? "Light Mode" : "Dark Mode");
        themeBtn.getStyleClass().add("secondary-button");
        themeBtn.setOnAction(e -> {
            mainApp.toggleTheme(scene);
            themeBtn.setText(mainApp.isDarkMode() ? "Light Mode" : "Dark Mode");
        });

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("danger-button");
        logoutBtn.setOnAction(e -> mainApp.showLoginScreen());

        bar.getChildren().addAll(heading, roleTag, spacer, themeBtn, logoutBtn);
        return bar;
    }

    // ── Navigation Sidebar ────────────────────────────────────────────────────────

    private VBox buildNavBar() {
        VBox nav = new VBox(4);
        nav.getStyleClass().add("nav-sidebar");
        nav.setPadding(new Insets(20, 12, 20, 12));
        nav.setPrefWidth(190);

        Label navHeader = new Label("NAVIGATION");
        navHeader.getStyleClass().add("nav-header-label");

        Button btnDashboard = navBtn("Dashboard",   () -> showDashboard());
        Button btnAddRoom   = navBtn("Add Room",     () -> showContent(buildAddRoomView()));
        Button btnRooms     = navBtn("Rooms",        () -> showRoomsView());
        Button btnCheckout  = navBtn("Edit Room",    () -> showContent(buildEditRoomView()));
        Button btnStats     = navBtn("Statistics",   () -> showStatsView());

        // Activate dashboard button by default
        setActive(btnDashboard);

        // Wire up to set active state on click
        wireNavBtn(btnDashboard, () -> { setActive(btnDashboard); showDashboard(); });
        wireNavBtn(btnAddRoom,   () -> { setActive(btnAddRoom);   showContent(buildAddRoomView()); });
        wireNavBtn(btnRooms,     () -> { setActive(btnRooms);     showRoomsView(); });
        wireNavBtn(btnCheckout,  () -> { setActive(btnCheckout);  showContent(buildEditRoomView()); });
        wireNavBtn(btnStats,     () -> { setActive(btnStats);     showStatsView(); });

        nav.getChildren().addAll(navHeader, btnDashboard, btnAddRoom, btnRooms, btnCheckout, btnStats);
        return nav;
    }

    private Button navBtn(String label, Runnable action) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    /** Re-wires a nav button so it sets itself active and then runs the action. */
    private void wireNavBtn(Button btn, Runnable action) {
        btn.setOnAction(e -> action.run());
    }

    private void setActive(Button btn) {
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().remove("nav-button-active");
        }
        activeNavBtn = btn;
        if (!btn.getStyleClass().contains("nav-button-active")) {
            btn.getStyleClass().add("nav-button-active");
        }
    }

    private void showContent(Node node) {
        contentArea.setCenter(node);
    }

    // ── Dashboard View ────────────────────────────────────────────────────────────

    private void showDashboard() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox page = new VBox(24);
        page.setPadding(new Insets(28, 28, 28, 28));

        // Page title
        Label pageTitle = new Label("Dashboard");
        pageTitle.getStyleClass().add("page-title");

        // Stats row
        HBox statsRow = buildStatsRow();

        // Operations Hub card
        VBox opsCard = buildOpsHubCard();

        // Premium services card
        VBox servicesCard = buildServicesCard();

        HBox bottomRow = new HBox(20, opsCard, servicesCard);
        HBox.setHgrow(opsCard, Priority.ALWAYS);
        HBox.setHgrow(servicesCard, Priority.ALWAYS);

        page.getChildren().addAll(pageTitle, statsRow, bottomRow);
        scroll.setContent(page);
        showContent(scroll);
    }

    private HBox buildStatsRow() {
        List<Room> rooms = hotelService.getAllRooms();
        long total     = rooms.size();
        long available = rooms.stream().filter(r -> "Available".equals(r.getStatus())).count();
        long booked    = rooms.stream().filter(r -> "Booked".equals(r.getStatus())).count();
        long cleaning  = rooms.stream().filter(r -> "Cleaning".equals(r.getStatus())).count();

        Map<String, String> stats = hotelService.getEnhancedStats();
        String revenue = stats.getOrDefault("Total Revenue", "Rs. 0.00");

        HBox row = new HBox(16);
        row.getChildren().addAll(
            dashCard("Total Rooms",     String.valueOf(total)),
            dashCard("Available Rooms", String.valueOf(available)),
            dashCard("Occupied Rooms",  String.valueOf(booked)),
            dashCard("Revenue Outlook", revenue)
        );

        for (Node card : row.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        return row;
    }

    private VBox dashCard(String label, String value) {
        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("dash-card-value");

        Label keyLabel = new Label(label);
        keyLabel.getStyleClass().add("dash-card-label");

        VBox card = new VBox(6, keyLabel, valLabel);
        card.getStyleClass().add("dash-card");
        card.setPadding(new Insets(18, 20, 18, 20));
        return card;
    }

    private VBox buildOpsHubCard() {
        Label title = new Label("Operations Hub");
        title.getStyleClass().add("card-title");

        Label desc = new Label("Front-office workflow for room management and administration.");
        desc.getStyleClass().add("subtitle-text");
        desc.setWrapText(true);

        VBox entries = new VBox(10,
            hubEntry("Add Room",    "Add new rooms to the hotel inventory."),
            hubEntry("Rooms",       "View and search all rooms with status filters."),
            hubEntry("Edit Room",   "Update room type and pricing."),
            hubEntry("Statistics",  "View revenue and occupancy statistics.")
        );

        VBox card = new VBox(14, title, desc, entries);
        card.getStyleClass().add("section");
        card.setPadding(new Insets(20));
        return card;
    }

    private VBox hubEntry(String name, String desc) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("hub-entry-title");

        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("subtitle-text");

        VBox entry = new VBox(3, nameLabel, descLabel);
        entry.getStyleClass().add("hub-entry");
        entry.setPadding(new Insets(10, 14, 10, 14));
        return entry;
    }

    private VBox buildServicesCard() {
        Label title = new Label("Room Status Overview");
        title.getStyleClass().add("card-title");

        List<Room> rooms = hotelService.getAllRooms();

        FlowPane tags = new FlowPane(8, 8);
        if (rooms.isEmpty()) {
            tags.getChildren().add(new Label("No rooms added yet."));
        } else {
            for (Room r : rooms) {
                Label tag = new Label("Room " + r.getRoomNumber() + " - " + r.getStatus());
                tag.getStyleClass().add("room-status-tag");
                String statusStyle = switch (r.getStatus()) {
                    case "Available" -> "tag-available";
                    case "Booked"    -> "tag-booked";
                    case "Cleaning"  -> "tag-cleaning";
                    default -> "";
                };
                if (!statusStyle.isEmpty()) tag.getStyleClass().add(statusStyle);
                tags.getChildren().add(tag);
            }
        }

        VBox card = new VBox(14, title, tags);
        card.getStyleClass().add("section");
        card.setPadding(new Insets(20));
        return card;
    }

    // ── Rooms View ────────────────────────────────────────────────────────────────

    private void showRoomsView() {
        roomsTable = buildRoomTable();

        Label sectionTitle = sectionLabel("Room List");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by room number...");
        searchField.getStyleClass().add("input-field");
        searchField.setPrefWidth(200);

        ComboBox<String> filterBox = new ComboBox<>(
            FXCollections.observableArrayList("All", "Available", "Booked", "Cleaning"));
        filterBox.setValue("All");
        filterBox.getStyleClass().add("input-field");

        Button applyBtn   = new Button("Apply");
        applyBtn.getStyleClass().add("secondary-button");

        Button refreshBtn = new Button("Reset");
        refreshBtn.getStyleClass().add("secondary-button");

        Runnable applyFilter = () -> {
            String query  = searchField.getText().trim();
            String filter = filterBox.getValue();
            List<Room> result = hotelService.getAllRooms().stream()
                .filter(r -> (query.isEmpty() || String.valueOf(r.getRoomNumber()).contains(query))
                          && ("All".equals(filter) || r.getStatus().equals(filter)))
                .collect(Collectors.toList());
            roomsTable.setItems(FXCollections.observableArrayList(result));
        };

        applyBtn.setOnAction(e -> applyFilter.run());
        searchField.setOnAction(e -> applyFilter.run());
        refreshBtn.setOnAction(e -> {
            searchField.clear();
            filterBox.setValue("All");
            refreshRoomsTable();
        });

        HBox toolbar = new HBox(10, searchField, filterBox, applyBtn, refreshBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox tableInner = new VBox(12, toolbar, roomsTable);
        VBox.setVgrow(roomsTable, Priority.ALWAYS);
        VBox tableSection = section(tableInner);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        VBox page = new VBox(16, sectionTitle, tableSection);
        page.setPadding(new Insets(24));
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        BorderPane wrapper = new BorderPane(page);
        VBox.setVgrow(page, Priority.ALWAYS);

        refreshRoomsTable();
        showContent(wrapper);
    }

    // ── Add Room View ─────────────────────────────────────────────────────────────

    private VBox buildAddRoomView() {
        Label sectionTitle = sectionLabel("Add New Room");

        TextField roomNoField = styledField("e.g. 201");
        ComboBox<String> typeCombo = new ComboBox<>(
            FXCollections.observableArrayList("Single", "Double", "Suite", "Deluxe"));
        typeCombo.setValue("Single");
        typeCombo.getStyleClass().add("input-field");
        typeCombo.setPrefWidth(244);

        TextField priceField = styledField("e.g. 1500");
        Label statusLbl = new Label();

        Button addBtn = new Button("Add Room");
        addBtn.getStyleClass().add("primary-button");

        addBtn.setOnAction(e -> {
            String roomStr  = roomNoField.getText().trim();
            String priceStr = priceField.getText().trim();

            if (!Validator.isPositiveInt(roomStr)) {
                showError("Invalid Input", "Please enter a valid room number."); return;
            }
            if (!Validator.isPositiveDouble(priceStr)) {
                showError("Invalid Input", "Please enter a valid price."); return;
            }

            boolean ok = hotelService.addRoom(
                Integer.parseInt(roomStr), typeCombo.getValue(),
                Double.parseDouble(priceStr));

            if (ok) {
                setMsg(statusLbl, "Room " + roomStr + " added successfully.", true);
                roomNoField.clear();
                priceField.clear();
                refreshRoomsTable();
            } else {
                showError("Duplicate Room", "Room number " + roomStr + " already exists.");
                setMsg(statusLbl, "Room number already exists.", false);
            }
        });

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Room Number:"),    roomNoField);
        grid.addRow(1, new Label("Room Type:"),      typeCombo);
        grid.addRow(2, new Label("Price/Day (Rs):"), priceField);

        VBox formSection = section(new VBox(18, grid, addBtn, statusLbl));

        VBox page = new VBox(16, sectionTitle, formSection);
        page.setPadding(new Insets(24));
        return page;
    }

    // ── Edit Room View ────────────────────────────────────────────────────────────

    private VBox buildEditRoomView() {
        TextField roomNoField = styledField("Room number to edit");
        ComboBox<String> typeCombo = new ComboBox<>(
            FXCollections.observableArrayList("Single", "Double", "Suite", "Deluxe"));
        typeCombo.setValue("Single");
        typeCombo.getStyleClass().add("input-field");
        typeCombo.setPrefWidth(244);

        TextField priceField = styledField("New price");
        Label     infoLbl    = new Label();
        Label     statusLbl  = new Label();

        Button loadBtn = new Button("Load Room");
        loadBtn.getStyleClass().add("secondary-button");
        loadBtn.setOnAction(e -> {
            String roomStr = roomNoField.getText().trim();
            if (!Validator.isPositiveInt(roomStr)) {
                showError("Invalid Input", "Enter a valid room number."); return;
            }
            Room room = hotelService.findRoom(Integer.parseInt(roomStr));
            if (room == null) {
                setMsg(infoLbl, "Room not found.", false);
            } else {
                typeCombo.setValue(room.getType());
                priceField.setText(String.valueOf(room.getPrice()));
                setMsg(infoLbl, "Loaded: Room " + room.getRoomNumber()
                    + " | " + room.getType() + " | Rs." + room.getPrice(), true);
            }
        });

        Button saveBtn = new Button("Save Changes");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> {
            String roomStr  = roomNoField.getText().trim();
            String priceStr = priceField.getText().trim();

            if (!Validator.isPositiveInt(roomStr)) {
                showError("Invalid Input", "Enter a valid room number."); return;
            }
            if (!Validator.isPositiveDouble(priceStr)) {
                showError("Invalid Input", "Enter a valid price."); return;
            }

            boolean ok = hotelService.editRoom(
                Integer.parseInt(roomStr), typeCombo.getValue(),
                Double.parseDouble(priceStr));

            if (ok) {
                setMsg(statusLbl, "Room updated successfully.", true);
                refreshRoomsTable();
            } else {
                showError("Not Found", "Room " + roomStr + " was not found.");
                setMsg(statusLbl, "Room not found.", false);
            }
        });

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Room Number:"), roomNoField);
        grid.addRow(1, new Label("New Type:"),    typeCombo);
        grid.addRow(2, new Label("New Price:"),   priceField);

        Label sectionTitle = sectionLabel("Edit Existing Room");

        HBox actionRow = new HBox(10, loadBtn, saveBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox formSection = section(new VBox(16, grid, actionRow, infoLbl, statusLbl));

        VBox page = new VBox(16, sectionTitle, formSection);
        page.setPadding(new Insets(24));
        return page;
    }

    // ── Statistics View ───────────────────────────────────────────────────────────

    private void showStatsView() {
        Label sectionTitle = sectionLabel("Overview");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary-button");

        Button resetBtn = new Button("Reset Stats");
        resetBtn.getStyleClass().add("danger-button");

        FlowPane cardsPane = new FlowPane(16, 16);
        cardsPane.setPadding(new Insets(4, 0, 0, 0));

        Runnable fillStats = () -> {
            cardsPane.getChildren().clear();
            Map<String, String> stats = hotelService.getEnhancedStats();

            if (stats.isEmpty()) {
                cardsPane.getChildren().add(new Label("No statistics available."));
                return;
            }
            for (Map.Entry<String, String> entry : stats.entrySet()) {
                cardsPane.getChildren().add(statCard(entry.getKey(), entry.getValue()));
            }
        };

        refreshBtn.setOnAction(e -> fillStats.run());

        resetBtn.setOnAction(e -> {
            boolean confirmed = showConfirm(
                "Reset Statistics",
                "This will clear all billing and booking history.\n"
                + "Room data will NOT be affected.\n\nAre you sure?"
            );
            if (confirmed) {
                hotelService.resetStatistics();
                fillStats.run();
                showInfo("Reset Complete", "Statistics have been reset successfully.");
            }
        });

        fillStats.run();

        HBox btnRow = new HBox(10, refreshBtn, resetBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        VBox statsSection = section(new VBox(14, btnRow, cardsPane));

        VBox page = new VBox(16, sectionTitle, statsSection);
        page.setPadding(new Insets(24));

        showContent(page);
    }

    // ── Layout helpers ────────────────────────────────────────────────────────────

    private VBox section(VBox content) {
        content.getStyleClass().add("section");
        if (content.getPadding().equals(Insets.EMPTY)) {
            content.setPadding(new Insets(16));
        }
        return content;
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("section-title");
        return lbl;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private void refreshRoomsTable() {
        if (roomsTable == null) return;
        List<Room> all = hotelService.getAllRooms();
        roomsTable.setItems(FXCollections.observableArrayList(all));
        roomsTable.setPlaceholder(new Label(
            all.isEmpty() ? "No rooms have been added yet." : "No rooms available."));
    }

    private HBox statCard(String label, String value) {
        Label lblNode = new Label(label);
        lblNode.setMinWidth(165);

        Label valNode = new Label(value);
        valNode.getStyleClass().add("stat-value");

        HBox card = new HBox(20, lblNode, valNode);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 22, 14, 22));
        card.setPrefWidth(340);
        return card;
    }

    @SuppressWarnings("unchecked")
    private TableView<Room> buildRoomTable() {
        TableView<Room> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No rooms have been added yet."));
        table.setFixedCellSize(38);

        TableColumn<Room, Integer> colNo    = col("Room No",   "roomNumber");
        TableColumn<Room, String>  colType  = col("Type",      "type");
        TableColumn<Room, Double>  colPrice = col("Price/Day", "price");
        TableColumn<Room, String>  colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Available" -> setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;");
                    case "Booked"    -> setStyle("-fx-text-fill:#e74c3c;-fx-font-weight:bold;");
                    case "Cleaning"  -> setStyle("-fx-text-fill:#e67e22;-fx-font-weight:bold;");
                    default          -> setStyle("");
                }
            }
        });

        table.getColumns().addAll(colNo, colType, colPrice, colStatus);
        return table;
    }

    private <T> TableColumn<Room, T> col(String title, String property) {
        TableColumn<Room, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        return c;
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("input-field");
        f.setPrefWidth(244);
        return f;
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(14);
        return g;
    }

    private void setMsg(Label lbl, String msg, boolean success) {
        lbl.getStyleClass().setAll(success ? "success-label" : "error-label");
        lbl.setText(msg);
    }

    // ── Alert helpers ─────────────────────────────────────────────────────────────

    private boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
