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
import com.routex.model.VehicleIssue;
import com.routex.model.Warehouse;
import com.routex.service.AuthService;
import com.routex.service.InventoryService;
import com.routex.service.ShipmentOrderService;
import com.routex.service.ShipmentService;
import com.routex.service.UserManagementService;
import com.routex.service.VehicleService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
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
        root.setBottom(buildFooter("Please authenticate to access the RouteX control center."));
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

        Label info = new Label("Use a seeded account to enter the system.");
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
        card.getChildren().addAll(title, info, emailField, passwordField, loginButton, seedHintBox());
        return new StackPane(card);
    }

    private Node seedHintBox() {
        VBox box = new VBox(4);
        box.getStyleClass().add("hint-box");
        Label heading = new Label("Seed accounts");
        heading.getStyleClass().add("hint-title");
        Label body = new Label("Admin: s.jenkins@routex.com / Admin@123\nManager: m.thorne@routex.com / Manager@123\nDispatcher: e.rodriguez@routex.com / Dispatcher@123\nDriver: j.howlett@routex.com / Driver1@123");
        body.getStyleClass().add("hint-text");
        box.getChildren().addAll(heading, body);
        return box;
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
        root.setBottom(buildFooter("Role-based access is active for " + currentUser.getRole().getDisplayName() + "."));
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
        Label note = new Label("This single dashboard contains every use case assigned to your group, with the service and DAO layers handling all persistence.");
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

        Label body = new Label("Use the tabs below to work with the business functions mapped to your role. The application follows a 3-tier structure: JavaFX presentation, Java service logic, and SQL Server DAOs.");
        body.setWrapText(true);
        body.getStyleClass().add("muted-text");

        TextArea output = new TextArea();
        output.setEditable(false);
        output.getStyleClass().add("output-area");
        output.setPrefRowCount(14);
        refreshHomeSummary(output);

        Button refresh = new Button("Refresh Summary");
        refresh.getStyleClass().add("primary-button");
        refresh.setOnAction(e -> refreshHomeSummary(output));

        box.getChildren().addAll(title, body, refresh, output);
        return box;
    }

    private void refreshHomeSummary(TextArea output) {
        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(currentUser.getName()).append('\n');
        sb.append("Role: ").append(currentUser.getRole().getDisplayName()).append('\n');
        sb.append("\nCounts\n");
        sb.append("Users: ").append(userManagementService.getAllUsers().size()).append('\n');
        sb.append("Inventory items: ").append(inventoryService.getAllItems().size()).append('\n');
        sb.append("Shipment orders: ").append(shipmentOrderService.getAllOrders().size()).append('\n');
        sb.append("Vehicles: ").append(vehicleService.getAllVehicles().size()).append('\n');
        sb.append("Shipments: ").append(shipmentService.getAllShipments().size()).append('\n');
        output.setText(sb.toString());
    }

    private Node buildUsersTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("UC11 - Manage User Accounts");
        title.getStyleClass().add("section-title");

        HBox actions = new HBox(10,
            actionButton("List", e -> showUsers()),
            actionButton("Create", e -> createUser()),
            actionButton("Update", e -> updateUser()),
            actionButton("Deactivate", e -> deactivateUser()),
            actionButton("Delete", e -> deleteUser())
        );
        TextArea output = new TextArea();
        output.setEditable(false);
        output.getStyleClass().add("output-area");
        output.setPrefRowCount(18);

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> output.setText(formatUsers(userManagementService.getAllUsers())));

        actions.getChildren().add(refresh);
        box.getChildren().addAll(title, actions, output);
        output.setText(formatUsers(userManagementService.getAllUsers()));
        bindOutputContext(actions, output);
        return box;
    }

    private Node buildInventoryTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("UC02 - Manage Inventory Stock");
        title.getStyleClass().add("section-title");

        HBox actions = new HBox(10,
            actionButton("List", e -> {}),
            actionButton("Add", e -> addInventoryItem()),
            actionButton("Update", e -> updateInventoryItem()),
            actionButton("Remove", e -> removeInventoryItem()),
            actionButton("Below Threshold", e -> {})
        );
        TextArea output = new TextArea();
        output.setEditable(false);
        output.getStyleClass().add("output-area");
        output.setPrefRowCount(18);

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> output.setText(formatInventory(inventoryService.getAllItems())));
        actions.getChildren().add(refresh);
        box.getChildren().addAll(title, actions, output);
        output.setText(formatInventory(inventoryService.getAllItems()));
        bindOutputContext(actions, output);
        return box;
    }

    private Node buildOrdersTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("UC04 / UC05 - Shipment Orders");
        title.getStyleClass().add("section-title");

        HBox actions = new HBox(10,
            actionButton("List Pending", e -> {}),
            actionButton("List Approved", e -> {}),
            actionButton("Generate Manual", e -> generateManualOrder()),
            actionButton("Auto Generate", e -> autoGenerateOrders()),
            actionButton("Approve", e -> approveOrder()),
            actionButton("Reject", e -> rejectOrder())
        );
        TextArea output = new TextArea();
        output.setEditable(false);
        output.getStyleClass().add("output-area");
        output.setPrefRowCount(18);

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> output.setText(formatOrders(shipmentOrderService.getAllOrders())));
        actions.getChildren().add(refresh);
        box.getChildren().addAll(title, actions, output);
        output.setText(formatOrders(shipmentOrderService.getAllOrders()));
        bindOutputContext(actions, output);
        return box;
    }

    private Node buildDispatchTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("UC06 - Assign Vehicle to Shipment");
        title.getStyleClass().add("section-title");

        HBox actions = new HBox(10,
            actionButton("Approved Orders", e -> {}),
            actionButton("Vehicles", e -> {}),
            actionButton("Assign Vehicle", e -> assignVehicle()),
            actionButton("Shipments", e -> {})
        );
        TextArea output = new TextArea();
        output.setEditable(false);
        output.getStyleClass().add("output-area");
        output.setPrefRowCount(18);

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> output.setText(formatDispatchSnapshot()));
        actions.getChildren().add(refresh);
        box.getChildren().addAll(title, actions, output);
        output.setText(formatDispatchSnapshot());
        bindOutputContext(actions, output);
        return box;
    }

    private Node buildDriverTab() {
        VBox box = new VBox(12);
        box.getStyleClass().add("tab-body");
        box.setPadding(new Insets(22));

        Label title = new Label("UC09 / UC10 - Driver Operations");
        title.getStyleClass().add("section-title");

        HBox actions = new HBox(10,
            actionButton("My Shipments", e -> {}),
            actionButton("Update Status", e -> updateDeliveryStatus()),
            actionButton("Report Issue", e -> reportVehicleIssue())
        );
        TextArea output = new TextArea();
        output.setEditable(false);
        output.getStyleClass().add("output-area");
        output.setPrefRowCount(18);

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> output.setText(formatDriverSnapshot()));
        actions.getChildren().add(refresh);
        box.getChildren().addAll(title, actions, output);
        output.setText(formatDriverSnapshot());
        bindOutputContext(actions, output);
        return box;
    }

    private Button actionButton(String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(label);
        button.getStyleClass().add("primary-button");
        button.setOnAction(handler);
        return button;
    }

    private void bindOutputContext(HBox actions, TextArea output) {
        actions.getChildren().filtered(node -> node instanceof Button).forEach(node -> {
            Button button = (Button) node;
            switch (button.getText()) {
                case "List" -> button.setOnAction(e -> output.setText(formatInventory(inventoryService.getAllItems())));
                case "Below Threshold" -> button.setOnAction(e -> output.setText(formatInventory(inventoryService.getItemsBelowThreshold())));
                case "List Pending" -> button.setOnAction(e -> output.setText(formatOrders(shipmentOrderService.getPendingOrders())));
                case "List Approved" -> button.setOnAction(e -> output.setText(formatOrders(shipmentOrderService.getApprovedOrders())));
                case "Approved Orders" -> button.setOnAction(e -> output.setText(formatOrders(shipmentOrderService.getApprovedOrders())));
                case "Vehicles" -> button.setOnAction(e -> output.setText(formatVehicles(vehicleService.getAllVehicles())));
                case "Shipments" -> button.setOnAction(e -> output.setText(formatShipments(shipmentService.getAllShipments())));
                case "My Shipments" -> button.setOnAction(e -> output.setText(formatShipments(shipmentService.getShipmentsForDriver(currentUser.getUserId()))));
            }
        });
    }

    private Node buildFooter(String message) {
        Label footer = new Label(message);
        footer.getStyleClass().add("footer-text");
        footer.setPadding(new Insets(10, 18, 16, 18));
        footer.setTextAlignment(TextAlignment.CENTER);
        return footer;
    }

    private void showUsers() {
        showAlert(Alert.AlertType.INFORMATION, "Users", formatUsers(userManagementService.getAllUsers()));
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

    private String formatUsers(List<User> users) {
        StringBuilder sb = new StringBuilder();
        for (User user : users) {
            sb.append(user.getUserId()).append(" | ").append(user.getName())
              .append(" | ").append(user.getEmail())
              .append(" | ").append(user.getRole())
              .append(" | ").append(user.getStatus())
              .append('\n');
        }
        return sb.toString();
    }

    private String formatInventory(List<InventoryItem> items) {
        StringBuilder sb = new StringBuilder();
        for (InventoryItem item : items) {
            sb.append(item.getItemId()).append(" | ").append(item.getSku())
              .append(" | ").append(item.getName())
              .append(" | qty=").append(item.getQuantity())
              .append(" | threshold=").append(item.getReorderThreshold())
              .append(" | warehouse=").append(item.getWarehouseName())
              .append('\n');
        }
        return sb.toString();
    }

    private String formatOrders(List<ShipmentOrder> orders) {
        StringBuilder sb = new StringBuilder();
        for (ShipmentOrder order : orders) {
            sb.append(order.getOrderId()).append(" | ").append(order.getItemName())
              .append(" | qty=").append(order.getRequiredQty())
              .append(" | priority=").append(order.getPriority())
              .append(" | status=").append(order.getStatus())
              .append('\n');
        }
        return sb.toString();
    }

    private String formatVehicles(List<Vehicle> vehicles) {
        StringBuilder sb = new StringBuilder();
        for (Vehicle vehicle : vehicles) {
            sb.append(vehicle.getVehicleId()).append(" | ").append(vehicle.getLicensePlate())
              .append(" | capacity=").append(vehicle.getCapacity())
              .append(" | status=").append(vehicle.getStatus())
              .append('\n');
        }
        return sb.toString();
    }

    private String formatShipments(List<Shipment> shipments) {
        StringBuilder sb = new StringBuilder();
        for (Shipment shipment : shipments) {
            sb.append(shipment.getShipmentId()).append(" | vehicle=").append(shipment.getVehicleLicensePlate())
              .append(" | driver=").append(shipment.getDriverName())
              .append(" | status=").append(shipment.getStatus())
              .append('\n');
        }
        return sb.toString();
    }

    private String formatDriverSnapshot() {
        return formatShipments(shipmentService.getShipmentsForDriver(currentUser.getUserId())) + "\n\nReport vehicle issues from this screen after selecting one of your assigned vehicles.";
    }

    private String formatDispatchSnapshot() {
        return "Approved Orders\n\n" + formatOrders(shipmentOrderService.getApprovedOrders()) +
               "\nAvailable Vehicles\n\n" + formatVehicles(vehicleService.getEligibleVehicles()) +
               "\nAll Shipments\n\n" + formatShipments(shipmentService.getAllShipments());
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
