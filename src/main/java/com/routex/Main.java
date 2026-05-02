package com.routex;

import com.routex.ui.RouteXDashboard;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        RouteXDashboard dashboard = new RouteXDashboard(stage);
        Scene scene = dashboard.createScene();
        stage.setTitle("RouteX - Inventory and Fleet Management");
        stage.setScene(scene);
        stage.setMinWidth(1280);
        stage.setMinHeight(840);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
