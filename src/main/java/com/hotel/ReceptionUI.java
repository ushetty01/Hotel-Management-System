// File: src/main/java/com/hotel/ReceptionUI.java
package com.hotel;

import com.hotel.model.Bill;
import com.hotel.model.Customer;
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
import java.util.Optional;

/**
 * Receptionist interface.
 * Handles: view available rooms, book a room, checkout, bill generation, PDF export.
 */
public class ReceptionUI {

    private final Stage        stage;
    private final HotelService hotelService;
    private final Main         mainApp;
    private Scene              scene;

    /** Holds the last generated bill so the PDF button can access it. */
    private Bill lastBill = null;

    /** Shared rooms table – promoted to field so every view can refresh it. */
    private TableView<Room> roomsTable;

    /** Single ObservableList instance — mutated in-place so JavaFX always sees the change. */
    private final javafx.collections.ObservableList<Room> roomList =
            FXCollections.observableArrayList();

    /** The main content area swapped by nav buttons. */
    private BorderPane contentArea;

    /** Track active nav button for styling. */
    private Button activeNavBtn = null;

    public ReceptionUI(Stage stage, HotelService hotelService, Main mainApp) {
        this.stage        = stage;
        this.hotelService = hotelService;
        this.mainApp      = mainApp;
    }

    // ── Build & Show ──────────────────────────────────────────────────────────────

    public void show() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildHeader());

        HBox body = new HBox();
        body.getStyleClass().add("body-area");
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox navBar = buildNavBar();

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

        Label roleTag = new Label("Receptionist");
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

        Button btnDashboard  = navBtn("Dashboard");
        Button btnViewRooms  = navBtn("View Rooms");
        Button btnBookRoom   = navBtn("Book Room");
        Button btnCheckout   = navBtn("Checkout & Bill");
        Button btnReadiness  = navBtn("Room Readiness");

        setActive(btnDashboard);

        wireNavBtn(btnDashboard, () -> { setActive(btnDashboard); showDashboard(); });
        wireNavBtn(btnViewRooms, () -> { setActive(btnViewRooms); showRoomsView(); });
        wireNavBtn(btnBookRoom,  () -> { setActive(btnBookRoom);  showContent(buildBookRoomView()); });
        wireNavBtn(btnCheckout,  () -> { setActive(btnCheckout);  showContent(buildCheckoutView()); });
        wireNavBtn(btnReadiness, () -> { setActive(btnReadiness); showContent(buildMarkCleanView()); });

        nav.getChildren().addAll(navHeader, btnDashboard, btnViewRooms, btnBookRoom, btnCheckout, btnReadiness);
        return nav;
    }

    private Button navBtn(String label) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

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
        refreshAllRooms();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox page = new VBox(24);
        page.setPadding(new Insets(28, 28, 28, 28));

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

        HBox row = new HBox(16);
        row.getChildren().addAll(
            dashCard("Available Rooms",  String.valueOf(available)),
            dashCard("Occupied Rooms",   String.valueOf(booked)),
            dashCard("Total Rooms",      String.valueOf(total)),
            dashCard("Guests In House",  String.valueOf(booked))
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

        Label desc = new Label("A unified front-office workflow for reservations, guest services, settlement, and records.");
        desc.getStyleClass().add("subtitle-text");
        desc.setWrapText(true);

        VBox entries = new VBox(10,
            hubEntry("Booking Desk",   "Assign rooms, register guests, and capture advance payments."),
            hubEntry("Guest Services", "Post spa, dining, travel, and other premium services straight to the folio."),
            hubEntry("Checkout Desk",  "Review dates, taxes, and settlement before releasing the room."),
            hubEntry("Invoice Archive","Revisit completed guest invoices and export operational records anytime.")
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
        descLabel.setWrapText(true);

        VBox entry = new VBox(3, nameLabel, descLabel);
        entry.getStyleClass().add("hub-entry");
        entry.setPadding(new Insets(10, 14, 10, 14));
        return entry;
    }

    private VBox buildServicesCard() {
        Label title = new Label("Premium Service Portfolio");
        title.getStyleClass().add("card-title");

        String[] services = {
            "Airport Pickup", "Chef Special Food Service", "City Tour",
            "Conference Hall Slot", "Gaming Lounge", "Gym Personal Trainer",
            "Infinity Pool Access", "Laundry Express", "Luxury Cab",
            "Mini Bar Refill", "Spa Therapy"
        };

        FlowPane tags = new FlowPane(8, 8);
        for (String svc : services) {
            Label tag = new Label(svc);
            tag.getStyleClass().add("service-tag");
            tags.getChildren().add(tag);
        }

        VBox card = new VBox(14, title, tags);
        card.getStyleClass().add("section");
        card.setPadding(new Insets(20));
        return card;
    }

    // ── Rooms View ────────────────────────────────────────────────────────────────

    private void showRoomsView() {
        Label sectionTitle = sectionLabel("Room Overview");

        roomsTable = buildAllRoomsTable();
        roomsTable.setItems(roomList);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> refreshAllRooms());

        Label note = new Label("All Rooms  (Status: Available / Booked / Cleaning)");
        note.getStyleClass().add("subtitle-text");

        VBox inner = new VBox(12, note, refreshBtn, roomsTable);
        VBox tableSection = section(inner);
        VBox.setVgrow(roomsTable, Priority.ALWAYS);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        VBox page = new VBox(16, sectionTitle, tableSection);
        page.setPadding(new Insets(24));
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        refreshAllRooms();
        showContent(new BorderPane(page));
    }

    // ── Book Room View ────────────────────────────────────────────────────────────

    private VBox buildBookRoomView() {
        Label sectionTitle = sectionLabel("New Booking");

        TextField roomField  = styledField("Room number");
        TextField nameField  = styledField("Customer full name");
        TextField phoneField = styledField("10-digit phone number");
        Label     statusLbl  = new Label();

        Button bookBtn = new Button("Confirm Booking");
        bookBtn.getStyleClass().add("primary-button");

        bookBtn.setOnAction(e -> {
            String roomStr = roomField.getText().trim();
            String name    = nameField.getText().trim();
            String phone   = phoneField.getText().trim();

            if (!Validator.isPositiveInt(roomStr)) {
                showError("Invalid Input", "Please enter a valid room number."); return;
            }
            if (Validator.isEmpty(name)) {
                showError("Invalid Input", "Customer name cannot be empty."); return;
            }
            if (!Validator.isValidPhone(phone)) {
                showError("Invalid Input", "Please enter a valid 10-digit phone number."); return;
            }

            Room room = hotelService.findRoom(Integer.parseInt(roomStr));
            if (room == null) {
                showError("Room Not Found", "Room " + roomStr + " does not exist.");
                setMsg(statusLbl, "Room not found.", false); return;
            }
            if (!Room.STATUS_AVAILABLE.equals(room.getStatus())) {
                showError("Room Not Available",
                    "Room " + roomStr + " is currently " + room.getStatus() + ".\n"
                    + "Only Available rooms can be booked.");
                setMsg(statusLbl, "Room is not available (status: " + room.getStatus() + ").", false);
                return;
            }

            boolean confirmed = showConfirm("Confirm Booking",
                "Book Room " + roomStr + " for " + name + "?");
            if (!confirmed) return;

            boolean ok = hotelService.bookRoom(
                Integer.parseInt(roomStr), new Customer(name, phone));

            if (ok) {
                showInfo("Booking Confirmed",
                    "Room " + roomStr + " has been booked for " + name + ".");
                setMsg(statusLbl, "Room " + roomStr + " booked for " + name + ".", true);
                roomField.clear(); nameField.clear(); phoneField.clear();
                refreshAllRooms();
            } else {
                showError("Booking Failed", "Could not book Room " + roomStr + ". Please try again.");
                setMsg(statusLbl, "Booking failed.", false);
            }
        });

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Room Number:"),   roomField);
        grid.addRow(1, new Label("Customer Name:"), nameField);
        grid.addRow(2, new Label("Phone Number:"),  phoneField);

        VBox formSection = section(new VBox(16, grid, bookBtn, statusLbl));

        VBox page = new VBox(16, sectionTitle, formSection);
        page.setPadding(new Insets(24));
        return page;
    }

    // ── Checkout View ─────────────────────────────────────────────────────────────

    private ScrollPane buildCheckoutView() {
        Label sectionTitle  = sectionLabel("Guest Checkout");
        Label billTitle     = sectionLabel("Invoice Preview");

        TextField roomField = styledField("Room number to check out");
        TextField daysField = styledField("Number of days stayed");
        Label     statusLbl = new Label();

        TextArea billArea = new TextArea();
        billArea.setEditable(false);
        billArea.setPrefHeight(210);
        billArea.getStyleClass().add("bill-area");

        Button exportPdfBtn = new Button("Export as PDF");
        exportPdfBtn.getStyleClass().add("secondary-button");
        exportPdfBtn.setDisable(true);

        Button checkoutBtn = new Button("Checkout & Generate Bill");
        checkoutBtn.getStyleClass().add("primary-button");

        checkoutBtn.setOnAction(e -> {
            String roomStr = roomField.getText().trim();
            String daysStr = daysField.getText().trim();

            if (!Validator.isPositiveInt(roomStr)) {
                showError("Invalid Input", "Please enter a valid room number."); return;
            }
            if (!Validator.isPositiveInt(daysStr)) {
                showError("Invalid Input", "Please enter a valid number of days."); return;
            }

            Room room = hotelService.findRoom(Integer.parseInt(roomStr));
            if (room == null) {
                showError("Room Not Found", "Room " + roomStr + " does not exist.");
                setMsg(statusLbl, "Room not found.", false); return;
            }
            if (!Room.STATUS_BOOKED.equals(room.getStatus())) {
                showError("Cannot Checkout",
                    "Room " + roomStr + " is currently " + room.getStatus() + ".\n"
                    + "Only Booked rooms can be checked out.");
                setMsg(statusLbl, "Room is not booked (status: " + room.getStatus() + ").", false);
                return;
            }

            boolean confirmed = showConfirm("Confirm Checkout",
                "Check out Room " + roomStr + " after " + daysStr + " day(s)?");
            if (!confirmed) return;

            Bill bill = hotelService.checkoutRoom(
                Integer.parseInt(roomStr), Integer.parseInt(daysStr));

            if (bill == null) {
                showError("Checkout Failed", "Room " + roomStr + " could not be checked out.");
                setMsg(statusLbl, "Checkout failed. Please try again.", false);
                billArea.clear();
                exportPdfBtn.setDisable(true);
                lastBill = null;
            } else {
                showInfo("Checkout Successful",
                    "Room " + roomStr + " has been checked out.\nTotal: Rs. "
                    + String.format("%.2f", bill.getTotalAmount()));
                setMsg(statusLbl, "Checkout complete. Bill saved to bills.txt.", true);
                billArea.setText(bill.getSummary());
                lastBill = bill;
                exportPdfBtn.setDisable(false);
                roomField.clear(); daysField.clear();
                refreshAllRooms();
            }
        });

        exportPdfBtn.setOnAction(e -> {
            if (lastBill == null) return;
            String path = hotelService.getFileService().generateBillPDF(lastBill);
            if (path != null) {
                showInfo("PDF Exported", "PDF saved to:\n" + path);
                setMsg(statusLbl, "PDF saved: " + path, true);
            } else {
                showError("PDF Error", "PDF generation failed. Check the console for details.");
            }
        });

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Room Number:"),  roomField);
        grid.addRow(1, new Label("Days Stayed:"),  daysField);

        HBox btnRow = new HBox(12, checkoutBtn, exportPdfBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        VBox formSection = section(new VBox(16, grid, btnRow, statusLbl));
        VBox billSection = section(new VBox(10, billArea));

        VBox box = new VBox(16, sectionTitle, formSection, billTitle, billSection);
        box.setPadding(new Insets(24));

        return new ScrollPane(box) {{
            setFitToWidth(true);
            setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        }};
    }

    // ── Room Readiness View ───────────────────────────────────────────────────────

    private VBox buildMarkCleanView() {
        Label sectionTitle = sectionLabel("Housekeeping");

        Label infoLbl = new Label(
            "After housekeeping is complete, mark the room as Available.");
        infoLbl.getStyleClass().add("subtitle-text");
        infoLbl.setWrapText(true);

        TextField roomField = styledField("Room number (status: Cleaning)");
        Label     statusLbl = new Label();

        Button markBtn = new Button("Mark as Available");
        markBtn.getStyleClass().add("primary-button");

        markBtn.setOnAction(e -> {
            String roomStr = roomField.getText().trim();
            if (!Validator.isPositiveInt(roomStr)) {
                showError("Invalid Input", "Please enter a valid room number."); return;
            }

            boolean ok = hotelService.markRoomAvailable(Integer.parseInt(roomStr));
            if (ok) {
                showInfo("Status Updated", "Room " + roomStr + " is now Available.");
                setMsg(statusLbl, "Room " + roomStr + " is now Available.", true);
                roomField.clear();
                refreshAllRooms();
            } else {
                showError("Status Error",
                    "Room " + roomStr + " was not found or is not in Cleaning status.");
                setMsg(statusLbl, "Room not found or not in Cleaning status.", false);
            }
        });

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Room Number:"), roomField);

        VBox sec = section(new VBox(14, infoLbl, grid, markBtn, statusLbl));

        VBox page = new VBox(16, sectionTitle, sec);
        page.setPadding(new Insets(24));
        return page;
    }

    // ── TableView ─────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TableView<Room> buildAllRoomsTable() {
        TableView<Room> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No rooms have been added yet."));
        table.setFixedCellSize(38);

        TableColumn<Room, Integer> colNo    = col("Room No",     "roomNumber");
        TableColumn<Room, String>  colType  = col("Type",        "type");
        TableColumn<Room, Double>  colPrice = col("Price / Day", "price");

        TableColumn<Room, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Available" ->
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "Booked" ->
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    case "Cleaning" ->
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });

        table.getColumns().addAll(colNo, colType, colPrice, colStatus);
        return table;
    }

    private void refreshAllRooms() {
        roomList.setAll(hotelService.getAllRooms());
        if (roomsTable != null) {
            roomsTable.refresh();
            roomsTable.setPlaceholder(new Label(
                roomList.isEmpty() ? "No rooms have been added yet." : ""));
        }
    }

    // ── Layout helpers ────────────────────────────────────────────────────────────

    private VBox section(VBox content) {
        content.getStyleClass().add("section");
        content.setPadding(new Insets(16));
        return content;
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("section-title");
        return lbl;
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────────

    private <T> TableColumn<Room, T> col(String title, String property) {
        TableColumn<Room, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        return c;
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("input-field");
        f.setPrefWidth(280);
        return f;
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(14); g.setVgap(14);
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
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message);
        a.showAndWait();
    }
}
