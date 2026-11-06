package com.financeapp;

import com.financeapp.ui.DashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        DashboardView dashboard = new DashboardView();

        Scene scene = new Scene(dashboard.createView(), 1200, 750);

        stage.setTitle("FinanceFlow | Personal Finance Tracker");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}