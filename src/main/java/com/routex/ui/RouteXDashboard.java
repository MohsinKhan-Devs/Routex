package com.routex.ui;

import com.routex.enums.Priority;
import com.routex.enums.ShipmentStatus;
import com.routex.enums.UserRole;
import com.routex.enums.UserStatus;
import com.routex.model.InventoryItem;
import com.routex.model.Shipment;
import com.routex.model.ShipmentOrder;
import com.routex.model.User;
import com.routex.model.Vehicle;
import com.routex.service.AuthService;
import com.routex.service.InventoryService;
import com.routex.service.ShipmentOrderService;
import com.routex.service.ShipmentService;
import com.routex.service.UserManagementService;
import com.routex.service.VehicleService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RouteXDashboard {

    private final Stage stage;
    private final BorderPane root = new BorderPane();

    private final AuthService authService = new AuthService();
    private final InventoryService inventoryService = new InventoryService();
    private final ShipmentOrderService shipmentOrderService = new ShipmentOrderService();
    private final ShipmentService shipmentService = new ShipmentService();
    private final VehicleService vehicleService = new VehicleService();
    private final UserManagementService userManagementService = new UserManagementService();

    private User currentUser;
    private Scene scene;

    public RouteXDashboard(Stage stage) {
        this.stage = stage;
        root.getStyleClass().add("app-root");
    }

    public Scene createScene() {
        if (scene == null) {
            scene = new Scene(root, 1280, 840);
            scene.getStylesheets().add(getClass().getResource("/ui/theme.css").toExternalForm());
            showLoginView();
        }
        return scene;
    }

    private void showLoginView() {
        root.setTop(buildPublicHeader());
        root.setLeft(null);
        root.setBottom(buildFooter("RouteX Control Center - Secure Access"));
        root.setCenter(buildLoginCard());
    }

    private Node buildPublicHeader() {
        VBox header = new VBox(6);
        header.getStyleClass().add("hero-header");
        header.setPadding(new Insets(28, 36, 22, 36));

        Label title = new Label("RouteX");
        title.getStyleClass().add("hero-title");
        Label subtitle = new Label("Inventory, shipment, fleet, and user operations in one system");
        subtitle.getStyleClass().add("hero-subtitle");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private Node buildLoginCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(420);
        card.setPadding(new Insets(30));

        Label title = new Label("Sign in");
        title.getStyleClass().add("section-title");

        Label info = new Label("Please enter your credentials to continue.");
        info.getStyleClass().add("muted-text");
        info.setWrapText(true);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> attemptLogin(emailField.getText(), passwordField.getText()));

        VBox.setVgrow(loginButton, javafx.scene.layout.Priority.NEVER);
        card.getChildren().addAll(title, info, emailField, passwordField, loginButton);
        return new StackPane(card);
    }

    private void attemptLogin(String email, String password) {
        try {
            currentUser = authService.login(email, password);
            showDashboard();
        } catch (AuthService.AuthException ex) {
            showAlert(Alert.AlertType.ERROR, "Login failed", ex.getMessage());
        }
    }

    private void showDashboard() {
        root.setTop(buildDashboardHeader());
        root.setLeft(buildSideInfo());
        root.setCenter(buildRoleTabs());
        root.setBottom(buildFooter("Session Active: " + currentUser.getRole().getDisplayName() + " Access Level"));
    }

    private Node buildDashboardHeader() {
        HBox header = new HBox(16);
        header.getStyleClass().add("dashboard-header");
        header.setPadding(new Insets(20, 28, 20, 28));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox brand = new VBox(2);
        Label title = new Label("RouteX Control Center");
        title.getStyleClass().add("dashboard-title");
        Label subtitle = new Label("Signed in as " + currentUser.getName() + " • " + currentUser.getRole().getDisplayName());
        subtitle.getStyleClass().add("dashboard-subtitle");
        brand.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button logout = new Button("Logout");
        logout.getStyleClass().add("secondary-button");
        logout.setOnAction(e -> {
            currentUser = null;
            showLoginView();
        });

        header.getChildren().addAll(brand, spacer, logout);
        return header;
    }

    private Node buildSideInfo() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("info-panel");
        panel.setPadding(new Insets(24));
        panel.setPrefWidth(300);

        Label title = new Label("Session Summary");
        title.getStyleClass().add("section-title");

        Label role = new Label("Role: " + currentUser.getRole().getDisplayName());
        Label status = new Label("Account status: " + currentUser.getStatus());
        Label note = new Label("Use the dashboard workspace to manage your assigned operational tasks.");
        note.setWrapText(true);
        note.getStyleClass().add("muted-text");

        panel.getChildren().addAll(title, role, status, new Separator(), note);
        return panel;
    }

    private Node buildRoleTabs() {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("routex-tabs");

        tabs.getTabs().add(tab("Home", buildHomeTab()));

        if (currentUser.getRole() == UserRole.SYSTEM_ADMIN) {
            tabs.getTabs().add(tab("Users", buildUsersTab()));
        }
        if (currentUser.getRole() == UserRole.INVENTORY_MANAGER) {
            tabs.getTabs().add(tab("Inventory", buildInventoryTab()));
            tabs.getTabs().add(tab("Orders", buildOrdersTab()));
        }
        if (currentUser.getRole() == UserRole.FLEET_DISPATCHER) {
            tabs.getTabs().add(tab("Dispatch", buildDispatchTab()));
        }
        if (currentUser.getRole() == UserRole.FIELD_DRIVER) {
            tabs.getTabs().add(tab("Driver", buildDriverTab()));
        }

        return tabs;
    }

    private Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private Node buildHomeTab() {
        VBox box = new VBox(14);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("Welcome to RouteX");
        title.getStyleClass().add("section-title");

        Label body = new Label("Navigate through the tabs to access your specific module functions and daily operations.");
        body.setWrapText(true);
        body.getStyleClass().add("muted-text");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(40);
        statsGrid.setVgap(15);
        statsGrid.setPadding(new Insets(20));
        statsGrid.getStyleClass().add("stat-card");

        refreshHomeStats(statsGrid);

        Button refresh = new Button("Refresh Summary");
        refresh.getStyleClass().add("primary-button");
        refresh.setOnAction(e -> refreshHomeStats(statsGrid));

        box.getChildren().addAll(title, body, refresh, statsGrid);
        return box;
    }

    private void refreshHomeStats(GridPane grid) {
        grid.getChildren().clear();

        addStatRow(grid, 0, "Current User", currentUser.getName());
        addStatRow(grid, 1, "Role", currentUser.getRole().getDisplayName());

        addStatRow(grid, 3, "Total Users", String.valueOf(userManagementService.getAllUsers().size()));
        addStatRow(grid, 4, "Inventory Items", String.valueOf(inventoryService.getAllItems().size()));
        addStatRow(grid, 5, "Shipment Orders", String.valueOf(shipmentOrderService.getAllOrders().size()));
        addStatRow(grid, 6, "Total Vehicles", String.valueOf(vehicleService.getAllVehicles().size()));
        addStatRow(grid, 7, "Total Shipments", String.valueOf(shipmentService.getAllShipments().size()));
    }

    private void addStatRow(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("muted-text");
        Label value = new Label(valueText);
        value.getStyleClass().add("value-text");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private Node buildUsersTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("Manage User Accounts");
        title.getStyleClass().add("section-title");

        TableView<User> table = createUserTable();
        javafx.scene.layout.VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        Runnable refreshTable = () -> table.setItems(FXCollections.observableArrayList(userManagementService.getAllUsers()));

        HBox actions = new HBox(10,
            actionButton("Create", e -> { createUser(); refreshTable.run(); }),
            actionButton("Update", e -> { updateUser(); refreshTable.run(); }),
            actionButton("Deactivate", e -> { deactivateUser(); refreshTable.run(); }),
            actionButton("Delete", e -> { deleteUser(); refreshTable.run(); })
        );

        Button refresh = new Button("Refresh Data");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> refreshTable.run());
        actions.getChildren().add(refresh);

        refreshTable.run();
        box.getChildren().addAll(title, actions, table);
        return box;
    }

    private Node buildInventoryTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("Manage Inventory Stock");
        title.getStyleClass().add("section-title");

        TableView<InventoryItem> table = createInventoryTable();
        javafx.scene.layout.VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        javafx.beans.property.BooleanProperty showingAll = new javafx.beans.property.SimpleBooleanProperty(true);

        Runnable refreshTable = () -> {
            if (showingAll.get()) {
                table.setItems(FXCollections.observableArrayList(inventoryService.getAllItems()));
            } else {
                table.setItems(FXCollections.observableArrayList(inventoryService.getItemsBelowThreshold()));
            }
        };

        HBox actions = new HBox(10,
            actionButton("List All", e -> { showingAll.set(true); refreshTable.run(); }),
            actionButton("Add", e -> { addInventoryItem(); refreshTable.run(); }),
            actionButton("Update", e -> { updateInventoryItem(); refreshTable.run(); }),
            actionButton("Remove", e -> { removeInventoryItem(); refreshTable.run(); }),
            actionButton("Below Threshold", e -> { showingAll.set(false); refreshTable.run(); })
        );

        Button refresh = new Button("Refresh Data");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> refreshTable.run());
        actions.getChildren().add(refresh);

        refreshTable.run();
        box.getChildren().addAll(title, actions, table);
        return box;
    }

    private Node buildOrdersTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("Shipment Orders");
        title.getStyleClass().add("section-title");

        TableView<ShipmentOrder> table = createOrderTable();
        javafx.scene.layout.VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        javafx.beans.property.BooleanProperty showingPending = new javafx.beans.property.SimpleBooleanProperty(true);

        Runnable refreshTable = () -> {
            if (showingPending.get()) {
                table.setItems(FXCollections.observableArrayList(shipmentOrderService.getPendingOrders()));
            } else {
                table.setItems(FXCollections.observableArrayList(shipmentOrderService.getApprovedOrders()));
            }
        };

        HBox actions = new HBox(10,
            actionButton("List Pending", e -> { showingPending.set(true); refreshTable.run(); }),
            actionButton("List Approved", e -> { showingPending.set(false); refreshTable.run(); }),
            actionButton("Generate Manual", e -> { generateManualOrder(); refreshTable.run(); }),
            actionButton("Auto Generate", e -> { autoGenerateOrders(); refreshTable.run(); }),
            actionButton("Approve", e -> { approveOrder(); refreshTable.run(); }),
            actionButton("Reject", e -> { rejectOrder(); refreshTable.run(); })
        );

        Button refresh = new Button("Refresh Data");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> refreshTable.run());
        actions.getChildren().add(refresh);

        refreshTable.run();
        box.getChildren().addAll(title, actions, table);
        return box;
    }

    private Node buildDispatchTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("Dispatch & Fleet Management");
        title.getStyleClass().add("section-title");

        StackPane tableContainer = new StackPane();
        javafx.scene.layout.VBox.setVgrow(tableContainer, javafx.scene.layout.Priority.ALWAYS);

        TableView<ShipmentOrder> orderTable = createOrderTable();
        TableView<Vehicle> vehicleTable = createVehicleTable();
        TableView<Shipment> shipmentTable = createShipmentTable();

        javafx.beans.property.ObjectProperty<TableView<?>> currentView = new javafx.beans.property.SimpleObjectProperty<>(orderTable);

        Runnable refreshCurrent = () -> {
            if (currentView.get() == orderTable) {
                orderTable.setItems(FXCollections.observableArrayList(shipmentOrderService.getApprovedOrders()));
            } else if (currentView.get() == vehicleTable) {
                vehicleTable.setItems(FXCollections.observableArrayList(vehicleService.getAllVehicles()));
            } else if (currentView.get() == shipmentTable) {
                shipmentTable.setItems(FXCollections.observableArrayList(shipmentService.getAllShipments()));
            }
        };

        HBox actions = new HBox(10,
            actionButton("Approved Orders", e -> {
                currentView.set(orderTable);
                refreshCurrent.run();
                tableContainer.getChildren().setAll(orderTable);
            }),
            actionButton("Vehicles", e -> {
                currentView.set(vehicleTable);
                refreshCurrent.run();
                tableContainer.getChildren().setAll(vehicleTable);
            }),
            actionButton("Assign Vehicle", e -> { assignVehicle(); refreshCurrent.run(); }),
            actionButton("Shipments", e -> {
                currentView.set(shipmentTable);
                refreshCurrent.run();
                tableContainer.getChildren().setAll(shipmentTable);
            })
        );

        Button refresh = new Button("Refresh View");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> refreshCurrent.run());
        actions.getChildren().add(refresh);

        refreshCurrent.run();
        tableContainer.getChildren().setAll(currentView.get());

        box.getChildren().addAll(title, actions, tableContainer);
        return box;
    }

    private Node buildDriverTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("Driver Operations");
        title.getStyleClass().add("section-title");

        TableView<Shipment> table = createShipmentTable();
        javafx.scene.layout.VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        Runnable refreshTable = () -> table.setItems(FXCollections.observableArrayList(shipmentService.getShipmentsForDriver(currentUser.getUserId())));

        HBox actions = new HBox(10,
            actionButton("Update Status", e -> { updateDeliveryStatus(); refreshTable.run(); }),
            actionButton("Report Issue", e -> { reportVehicleIssue(); refreshTable.run(); })
        );

        Button refresh = new Button("Refresh Assignments");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> refreshTable.run());
        actions.getChildren().add(refresh);

        refreshTable.run();
        box.getChildren().addAll(title, actions, table);
        return box;
    }

    // --- TABLE GENERATION METHODS ---

    private TableView<User> createUserTable() {
        TableView<User> table = new TableView<>();
        table.getStyleClass().add("inventory-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> idCol = new TableColumn<>("User ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserId()));
        idCol.getStyleClass().add("uuid-cell");

        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.getStyleClass().add("name-cell");

        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().toString()));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().toString()));

        table.getColumns().addAll(idCol, nameCol, emailCol, roleCol, statusCol);
        installCopyPasteHandler(table);
        return table;
    }

    private TableView<InventoryItem> createInventoryTable() {
        TableView<InventoryItem> table = new TableView<>();
        table.getStyleClass().add("inventory-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<InventoryItem, String> idCol = new TableColumn<>("Item ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemId()));
        idCol.getStyleClass().add("uuid-cell");

        TableColumn<InventoryItem, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSku()));
        skuCol.getStyleClass().add("sku-cell");

        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.getStyleClass().add("name-cell");

        TableColumn<InventoryItem, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantity())));
        qtyCol.getStyleClass().add("qty-cell");

        TableColumn<InventoryItem, String> threshCol = new TableColumn<>("Threshold");
        threshCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getReorderThreshold())));
        threshCol.getStyleClass().add("threshold-cell");

        TableColumn<InventoryItem, String> whCol = new TableColumn<>("Warehouse");
        whCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getWarehouseName()));
        whCol.getStyleClass().add("warehouse-cell");

        table.getColumns().addAll(idCol, skuCol, nameCol, qtyCol, threshCol, whCol);
        installCopyPasteHandler(table);
        return table;
    }

    private TableView<ShipmentOrder> createOrderTable() {
        TableView<ShipmentOrder> table = new TableView<>();
        table.getStyleClass().add("inventory-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ShipmentOrder, String> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderId()));
        idCol.getStyleClass().add("uuid-cell");

        TableColumn<ShipmentOrder, String> itemCol = new TableColumn<>("Item Name");
        itemCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        itemCol.getStyleClass().add("name-cell");

        TableColumn<ShipmentOrder, String> qtyCol = new TableColumn<>("Req Qty");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getRequiredQty())));
        qtyCol.getStyleClass().add("qty-cell");

        TableColumn<ShipmentOrder, String> prioCol = new TableColumn<>("Priority");
        prioCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPriority().toString()));

        TableColumn<ShipmentOrder, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().toString()));

        table.getColumns().addAll(idCol, itemCol, qtyCol, prioCol, statusCol);
        installCopyPasteHandler(table);
        return table;
    }

    private TableView<Vehicle> createVehicleTable() {
        TableView<Vehicle> table = new TableView<>();
        table.getStyleClass().add("inventory-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Vehicle, String> idCol = new TableColumn<>("Vehicle ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleId()));
        idCol.getStyleClass().add("uuid-cell");

        TableColumn<Vehicle, String> plateCol = new TableColumn<>("License Plate");
        plateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLicensePlate()));
        plateCol.getStyleClass().add("sku-cell");

        TableColumn<Vehicle, String> capCol = new TableColumn<>("Capacity");
        capCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCapacity())));
        capCol.getStyleClass().add("qty-cell");

        TableColumn<Vehicle, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().toString()));

        table.getColumns().addAll(idCol, plateCol, capCol, statusCol);
        installCopyPasteHandler(table);
        return table;
    }

    private TableView<Shipment> createShipmentTable() {
        TableView<Shipment> table = new TableView<>();
        table.getStyleClass().add("inventory-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Shipment, String> idCol = new TableColumn<>("Shipment ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getShipmentId()));
        idCol.getStyleClass().add("uuid-cell");

        TableColumn<Shipment, String> vehicleCol = new TableColumn<>("Vehicle Plate");
        vehicleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleLicensePlate()));

        TableColumn<Shipment, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDriverName()));

        TableColumn<Shipment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().toString()));

        table.getColumns().addAll(idCol, vehicleCol, driverCol, statusCol);
        installCopyPasteHandler(table);
        return table;
    }

    // --- REUSABLE UTILITIES ---

    private void installCopyPasteHandler(TableView<?> table) {
        // Enable cell-level selection instead of row-level
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Add keyboard shortcut listener for Copy (Ctrl+C / Cmd+C)
        table.setOnKeyPressed(event -> {
            KeyCodeCombination copyShortcut = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
            if (copyShortcut.match(event)) {
                // By using a wildcard list and explicit casts inside the loop, we avoid
                // strict Maven type-checking compilation failures.
                List<?> selectedCells = table.getSelectionModel().getSelectedCells();
                if (selectedCells.isEmpty()) return;

                StringBuilder clipboardString = new StringBuilder();
                int prevRow = -1;
                
                for (Object cellObj : selectedCells) {
                    TablePosition<?, ?> pos = (TablePosition<?, ?>) cellObj;
                    
                    // Add newline if we moved to a new row, else add tab if moving columns
                    if (prevRow != -1 && prevRow != pos.getRow()) {
                        clipboardString.append("\n");
                    } else if (prevRow != -1) {
                        clipboardString.append("\t");
                    }
                    
                    Object value = pos.getTableColumn().getCellData(pos.getRow());
                    clipboardString.append(value == null ? "" : value.toString());
                    prevRow = pos.getRow();
                }

                // Push to system clipboard
                ClipboardContent content = new ClipboardContent();
                content.putString(clipboardString.toString());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
    }

    private Button actionButton(String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(label);
        button.getStyleClass().add("primary-button");
        button.setOnAction(handler);
        return button;
    }

    private Node buildFooter(String message) {
        Label footer = new Label(message);
        footer.getStyleClass().add("footer-text");
        footer.setPadding(new Insets(10, 18, 16, 18));
        footer.setTextAlignment(TextAlignment.CENTER);
        return footer;
    }

    private void createUser() {
        Optional<String> name = promptText("Create User", "Name", "Enter user name");
        Optional<String> email = promptText("Create User", "Email", "Enter email address");
        Optional<String> password = promptText("Create User", "Password", "Enter password");
        Optional<UserRole> role = promptChoice("Create User", "Role", List.of(UserRole.values()));
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            return;
        }
        if (execute(() -> userManagementService.createUser(name.get(), email.get(), password.get(), role.get(), currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "User created successfully.");
        }
    }

    private void updateUser() {
        Optional<String> id = promptText("Update User", "UserId", "Enter user id");
        if (id.isEmpty()) return;
        User target = userManagementService.getAllUsers().stream()
            .filter(u -> u.getUserId().equalsIgnoreCase(id.get()))
            .findFirst().orElse(null);
        if (target == null) {
            showAlert(Alert.AlertType.ERROR, "Update User", "User not found.");
            return;
        }
        Optional<String> name = promptText("Update User", "Name", "Current: " + target.getName());
        Optional<String> email = promptText("Update User", "Email", "Current: " + target.getEmail());
        Optional<UserRole> role = promptChoice("Update User", "Role", List.of(UserRole.values()));
        Optional<UserStatus> status = promptChoice("Update User", "Status", List.of(UserStatus.values()));
        if (name.isEmpty() || email.isEmpty() || role.isEmpty() || status.isEmpty()) return;
        target.setName(name.get());
        target.setEmail(email.get());
        target.setRole(role.get());
        target.setStatus(status.get());
        if (execute(() -> userManagementService.updateUser(target, currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully.");
        }
    }

    private void deactivateUser() {
        Optional<String> id = promptText("Deactivate User", "UserId", "Enter user id");
        id.ifPresent(value -> {
            if (execute(() -> userManagementService.deactivateUser(value, currentUser.getUserId()))) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "User deactivated successfully.");
            }
        });
    }

    private void deleteUser() {
        Optional<String> id = promptText("Delete User", "UserId", "Enter user id");
        id.ifPresent(value -> {
            if (execute(() -> userManagementService.deleteUser(value, currentUser.getUserId()))) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully.");
            }
        });
    }

    private void addInventoryItem() {
        Optional<String> sku = promptText("Add Inventory Item", "SKU", "Enter SKU");
        Optional<String> name = promptText("Add Inventory Item", "Name", "Enter item name");
        Optional<String> qty = promptText("Add Inventory Item", "Quantity", "Enter quantity");
        Optional<String> expiry = promptText("Add Inventory Item", "Expiry Date", "yyyy-mm-dd");
        Optional<String> warehouseId = promptText("Add Inventory Item", "WarehouseId", "Enter warehouse id");
        Optional<String> threshold = promptText("Add Inventory Item", "Reorder Threshold", "Enter threshold");
        if (sku.isEmpty() || name.isEmpty() || qty.isEmpty() || expiry.isEmpty() || warehouseId.isEmpty() || threshold.isEmpty()) return;

        InventoryItem item = new InventoryItem();
        item.setSku(sku.get());
        item.setName(name.get());
        item.setQuantity(Integer.parseInt(qty.get()));
        item.setExpiryDate(LocalDate.parse(expiry.get()));
        item.setWarehouseId(warehouseId.get());
        item.setReorderThreshold(Integer.parseInt(threshold.get()));

        if (execute(() -> inventoryService.addItem(item, currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Inventory item added.");
        }
    }

    private void updateInventoryItem() {
        Optional<String> itemId = promptText("Update Inventory Item", "ItemId", "Enter item id");
        Optional<String> name = promptText("Update Inventory Item", "Name", "Enter item name");
        Optional<String> qty = promptText("Update Inventory Item", "Quantity", "Enter quantity");
        Optional<String> expiry = promptText("Update Inventory Item", "Expiry Date", "yyyy-mm-dd");
        Optional<String> warehouseId = promptText("Update Inventory Item", "WarehouseId", "Enter warehouse id");
        Optional<String> threshold = promptText("Update Inventory Item", "Reorder Threshold", "Enter threshold");
        if (itemId.isEmpty() || name.isEmpty() || qty.isEmpty() || expiry.isEmpty() || warehouseId.isEmpty() || threshold.isEmpty()) return;

        InventoryItem item = new InventoryItem();
        item.setItemId(itemId.get());
        item.setName(name.get());
        item.setQuantity(Integer.parseInt(qty.get()));
        item.setExpiryDate(LocalDate.parse(expiry.get()));
        item.setWarehouseId(warehouseId.get());
        item.setReorderThreshold(Integer.parseInt(threshold.get()));
        if (execute(() -> inventoryService.updateItem(item, currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Inventory item updated.");
        }
    }

    private void removeInventoryItem() {
        Optional<String> itemId = promptText("Remove Inventory Item", "ItemId", "Enter item id");
        itemId.ifPresent(value -> {
            if (execute(() -> inventoryService.removeItem(value, currentUser.getUserId()))) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Inventory item removed.");
            }
        });
    }

    private void generateManualOrder() {
        Optional<String> itemId = promptText("Generate Order", "ItemId", "Enter item id");
        Optional<String> qty = promptText("Generate Order", "Required Quantity", "Enter quantity");
        Optional<String> destination = promptText("Generate Order", "Destination Address", "Enter delivery address");
        Optional<Priority> priority = promptChoice("Generate Order", "Priority", List.of(Priority.values()));
        Optional<String> date = promptText("Generate Order", "Expected Delivery Date", "yyyy-mm-dd (optional)");
        if (itemId.isEmpty() || qty.isEmpty() || destination.isEmpty() || priority.isEmpty()) return;
        LocalDate delivery = (date.isPresent() && !date.get().isBlank()) ? LocalDate.parse(date.get()) : null;
        if (execute(() -> shipmentOrderService.generateOrder(itemId.get(), Integer.parseInt(qty.get()), destination.get(), priority.get(), delivery, currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Shipment order created.");
        }
    }

    private void autoGenerateOrders() {
        int count = shipmentOrderService.generateOrdersForLowStock(currentUser.getUserId());
        showAlert(Alert.AlertType.INFORMATION, "Auto Generate", count + " order(s) created from low stock items.");
    }

    private void approveOrder() {
        Optional<String> orderId = promptText("Approve Order", "OrderId", "Enter order id");
        Optional<String> qty = promptText("Approve Order", "Adjusted Quantity", "Enter approved quantity");
        if (orderId.isEmpty() || qty.isEmpty()) return;
        if (execute(() -> shipmentOrderService.approveOrder(orderId.get(), Integer.parseInt(qty.get()), currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Order approved.");
        }
    }

    private void rejectOrder() {
        Optional<String> orderId = promptText("Reject Order", "OrderId", "Enter order id");
        Optional<String> reason = promptText("Reject Order", "Reason", "Enter rejection reason");
        if (orderId.isEmpty() || reason.isEmpty()) return;
        if (execute(() -> shipmentOrderService.rejectOrder(orderId.get(), reason.get(), currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Order rejected.");
        }
    }

    private void assignVehicle() {
        Optional<String> orderId = promptText("Assign Vehicle", "Approved OrderId", "Enter approved order id");
        Optional<String> vehicleId = promptText("Assign Vehicle", "VehicleId", "Enter vehicle id");
        Optional<String> driverId = promptText("Assign Vehicle", "DriverId", "Enter driver id");
        if (orderId.isEmpty() || vehicleId.isEmpty() || driverId.isEmpty()) return;
        if (execute(() -> vehicleService.assignVehicle(vehicleId.get(), orderId.get(), driverId.get(), currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Vehicle assigned to shipment.");
        }
    }

    private void updateDeliveryStatus() {
        Optional<String> shipmentId = promptText("Update Delivery Status", "ShipmentId", "Enter shipment id");
        Optional<ShipmentStatus> status = promptChoice("Update Delivery Status", "Next Status", List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED, ShipmentStatus.CANCELLED));
        if (shipmentId.isEmpty() || status.isEmpty()) return;
        if (execute(() -> shipmentService.updateDeliveryStatus(shipmentId.get(), status.get(), currentUser.getUserId(), currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Shipment status updated.");
        }
    }

    private void reportVehicleIssue() {
        Optional<String> vehicleId = promptText("Report Vehicle Issue", "VehicleId", "Enter vehicle id");
        Optional<String> description = promptText("Report Vehicle Issue", "Description", "Describe the issue");
        Optional<String> category = promptText("Report Vehicle Issue", "Category", "ENGINE, TYRE, ELECTRICAL, etc.");
        Optional<String> gps = promptText("Report Vehicle Issue", "GPS Location", "Enter GPS location");
        if (vehicleId.isEmpty() || description.isEmpty() || category.isEmpty() || gps.isEmpty()) return;
        if (execute(() -> vehicleService.reportIssue(vehicleId.get(), currentUser.getUserId(), description.get(), category.get(), gps.get(), currentUser.getUserId()))) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Vehicle issue reported.");
        }
    }

    private Optional<String> promptText(String title, String header, String content) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        dialog.initOwner(stage);
        return dialog.showAndWait().map(String::trim).filter(value -> !value.isBlank());
    }

    private <T> Optional<T> promptChoice(String title, String header, List<T> choices) {
        ChoiceDialog<T> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.initOwner(stage);
        return dialog.showAndWait();
    }

    private boolean execute(CheckedAction action) {
        try {
            action.run();
            return true;
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Operation failed", ex.getMessage());
            return false;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private interface CheckedAction {
        void run() throws Exception;
    }
}