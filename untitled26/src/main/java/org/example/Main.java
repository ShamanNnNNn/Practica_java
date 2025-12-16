package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private ConnectionPool connectionPool;
    private TaskService taskService;
    private TaskDao taskDao;

    @Override
    public void init() {

        try {
            connectionPool = ConnectionPool.getInstance();

            DatabaseInitializer dbInitializer = new DatabaseInitializer(connectionPool);
            dbInitializer.initializeDatabase();

            taskDao = new TaskDaoImpl(connectionPool);
            taskService = new TaskService(taskDao);

            if (taskDao instanceof TaskDaoImpl) {
                ((TaskDaoImpl) taskDao).checkDatabaseFormat();
            }

            int taskCount = taskService.findAll().size();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public void start(Stage primaryStage) {

        try {
            if (taskService == null) {
            }

            TaskView taskView = new TaskView(taskService);

            Scene scene = new Scene(taskView.getView(), 1200, 800);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            primaryStage.setOnCloseRequest(event -> {
                if (connectionPool != null) {
                    connectionPool.closeAllConnections();
                }
            });

            primaryStage.show();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();

        }
    }

    @Override
    public void stop() {

        if (connectionPool != null) {
            connectionPool.closeAllConnections();
        }

    }

    public static void main(String[] args) {

        try {
            launch(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }


}
