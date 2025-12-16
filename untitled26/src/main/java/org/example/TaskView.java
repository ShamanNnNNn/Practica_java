package org.example;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskView {
    private final TaskService taskService;
    private final BorderPane root;
    private final TableView<Task> taskTable;
    private final ObservableList<Task> taskData;
    private final TextArea detailsArea;
    private final Label statsLabel;

    private List<Task> allTasksCache = new ArrayList<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TIMEOUT_MS = 30000; // 30 секунд

    private String currentFilter = "all";

    private boolean isLoading = false;

    public TaskView(TaskService taskService) {
        this.taskService = taskService;
        this.taskData = FXCollections.observableArrayList();
        this.root = new BorderPane();
        this.taskTable = createTaskTable();
        this.detailsArea = new TextArea();
        this.statsLabel = new Label();

        initializeUI();
        refreshAllData();
    }

    private void initializeUI() {
        root.setTop(createHeader());
        root.setCenter(createCenterPane());
        root.setBottom(createStatusBar());
        root.setLeft(createFilterPanel());
        root.setRight(createDetailsPanel());
    }

    private HBox createHeader() {
        Label titleLabel = new Label("Менеджер Задач");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.DARKSLATEBLUE);

        HBox header = new HBox(titleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #E3F2FD, #BBDEFB); " +
                "-fx-border-color: #2196F3; -fx-border-width: 0 0 2 0;");

        return header;
    }

    private VBox createFilterPanel() {
        VBox filterPanel = new VBox(15);
        filterPanel.setPadding(new Insets(20, 15, 20, 15));
        filterPanel.setPrefWidth(200);
        filterPanel.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #E0E0E0; -fx-border-width: 0 1 0 0;");

        Label filterLabel = new Label("Фильтры");
        filterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Button btnAll = createFilterButton("Все задачи", "all", () -> applyFilter("all"));
        Button btnToday = createFilterButton("На сегодня", "today", () -> applyFilter("today"));
        Button btnOverdue = createFilterButton("Просроченные", "overdue", () -> applyFilter("overdue"));
        Button btnCompleted = createFilterButton("Выполненные", "completed", () -> applyFilter("completed"));
        Button btnPending = createFilterButton("Активные", "pending", () -> applyFilter("pending"));

        Label statsTitleLabel = new Label("Статистика:");
        statsTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        statsLabel.setFont(Font.font("Arial", 12));
        statsLabel.setWrapText(true);

        filterPanel.getChildren().addAll(filterLabel, btnAll, btnToday, btnOverdue,
                btnCompleted, btnPending, new Separator(), statsTitleLabel, statsLabel);

        return filterPanel;
    }

    private Button createFilterButton(String text, String filterType, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(e -> {
            if (!isLoading) {
                currentFilter = filterType;
                action.run();
                updateStatistics();
                highlightActiveFilter(button);
            }
        });
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: #E8EAF6; -fx-text-fill: #283593; " +
                "-fx-font-size: 14px; -fx-padding: 8px; -fx-cursor: hand;");

        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: #C5CAE9; -fx-text-fill: #283593; " +
                        "-fx-font-size: 14px; -fx-padding: 8px; -fx-cursor: hand;"));
        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains("-fx-background-color: #4CAF50")) {
                button.setStyle("-fx-background-color: #E8EAF6; -fx-text-fill: #283593; " +
                        "-fx-font-size: 14px; -fx-padding: 8px; -fx-cursor: hand;");
            }
        });

        return button;
    }

    private void highlightActiveFilter(Button activeButton) {
        for (var node : ((VBox) taskTable.getParent().getParent().getChildrenUnmodifiable().get(2)).getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn != activeButton && !btn.getText().contains("🔄")) {
                    btn.setStyle("-fx-background-color: #E8EAF6; -fx-text-fill: #283593; " +
                            "-fx-font-size: 14px; -fx-padding: 8px; -fx-cursor: hand;");
                }
            }
        }
        activeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 8px; -fx-cursor: hand;");
    }

    private BorderPane createCenterPane() {
        BorderPane centerPane = new BorderPane();

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnAdd = createStyledButton("➕ Добавить", "primary", this::showAddTaskDialog);
        Button btnEdit = createStyledButton("✏️ Редактировать", "secondary", this::showEditTaskDialog);
        Button btnDelete = createStyledButton("🗑️ Удалить", "danger", this::deleteSelectedTask);
        Button btnComplete = createStyledButton("✅ Выполнить", "success", this::markAsCompleted);

        toolbar.getChildren().addAll(btnAdd, btnEdit, btnDelete, btnComplete);

        VBox tableContainer = new VBox(10);
        tableContainer.setPadding(new Insets(10));

        Label tableTitle = new Label("Список задач");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        taskTable.setPrefHeight(400);
        taskTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> showTaskDetails(newSelection));

        tableContainer.getChildren().addAll(tableTitle, taskTable);

        centerPane.setTop(toolbar);
        centerPane.setCenter(tableContainer);

        return centerPane;
    }


    private VBox createDetailsPanel() {
        VBox detailsPanel = new VBox(15);
        detailsPanel.setPadding(new Insets(20));
        detailsPanel.setPrefWidth(300);
        detailsPanel.setStyle("-fx-background-color: #FFFDE7; -fx-border-color: #FFD54F; -fx-border-width: 0 0 0 2;");

        Label detailsLabel = new Label("Детали задачи");
        detailsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setPrefHeight(300);
        detailsArea.setStyle("-fx-control-inner-background: #FFFDE7; -fx-font-size: 14px;");

        detailsPanel.getChildren().addAll(detailsLabel, detailsArea);

        return detailsPanel;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #212121;");

        Label statusLabel = new Label("Готово");
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setFont(Font.font("Arial", 12));

        Label filterInfoLabel = new Label("Фильтр: Все задачи");
        filterInfoLabel.setTextFill(Color.LIGHTGRAY);
        filterInfoLabel.setFont(Font.font("Arial", 12));

        statusBar.getChildren().addAll(statusLabel, new Separator(), filterInfoLabel);

        return statusBar;
    }

    private TableView<Task> createTaskTable() {
        TableView<Task> table = new TableView<>();
        table.setPlaceholder(new Label("Нет задач для отображения"));

        TableColumn<Task, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Task, String> titleCol = new TableColumn<>("Название");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(200);

        TableColumn<Task, LocalDate> dateCol = new TableColumn<>("Срок");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        dateCol.setPrefWidth(100);
        dateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Task, String> priorityCol = new TableColumn<>("Приоритет");
        priorityCol.setCellValueFactory(cellData -> {
            Task.Priority priority = cellData.getValue().getPriority();
            String displayName = (priority != null) ? priority.getDisplayName() : "Не указан";
            return new SimpleStringProperty(displayName);
        });
        priorityCol.setPrefWidth(100);
        priorityCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Task, String> categoryCol = new TableColumn<>("Категория");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(120);

        TableColumn<Task, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            if (task == null) return new SimpleStringProperty("");
            boolean completed = task.isCompleted();
            LocalDate dueDate = task.getDueDate();

            String status;
            if (completed) {
                status = "Выполнена";
            } else if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
                status = "Просрочена";
            } else {
                status = "В работе";
            }
            return new SimpleStringProperty(status);
        });
        statusCol.setPrefWidth(100);
        statusCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(idCol, titleCol, dateCol, priorityCol, categoryCol, statusCol);
        table.setItems(taskData);

        return table;
    }

    private Button createStyledButton(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(e -> {
            if (!isLoading) {
                action.run();
            }
        });

        String baseStyle = "-fx-padding: 8px 15px; -fx-font-size: 14px; -fx-cursor: hand;";

        switch(styleClass) {
            case "primary":
                button.setStyle(baseStyle + "-fx-background-color: #2196F3; -fx-text-fill: white;");
                break;
            case "secondary":
                button.setStyle(baseStyle + "-fx-background-color: #757575; -fx-text-fill: white;");
                break;
            case "danger":
                button.setStyle(baseStyle + "-fx-background-color: #F44336; -fx-text-fill: white;");
                break;
            case "success":
                button.setStyle(baseStyle + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
                break;
        }

        button.setOnMouseEntered(e -> button.setStyle(button.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);", "")));

        return button;
    }


    private void refreshAllData() {
        if (isLoading) return;

        try {
            isLoading = true;
            System.out.println("Полное обновление данных...");

            allTasksCache = taskService.findAll();
            lastCacheUpdate = System.currentTimeMillis();

            System.out.println("Загружено задач: " + allTasksCache.size());

            applyFilter(currentFilter);

            updateStatistics();

            showAlert("Обновлено", "Данные успешно обновлены из базы");

        } catch (Exception e) {
            System.err.println("Ошибка при обновлении данных: " + e.getMessage());
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось обновить данные");
        } finally {
            isLoading = false;
        }
    }

    private void refreshFromDatabase() {
        if (isLoading) return;

        try {
            isLoading = true;
            System.out.println("Принудительная синхронизация с базой...");

            allTasksCache = taskService.findAll();
            lastCacheUpdate = System.currentTimeMillis();

            applyFilter(currentFilter);

            updateStatistics();

            showAlert("Синхронизировано", "Данные синхронизированы с базой");

        } catch (Exception e) {
            System.err.println("Ошибка при синхронизации: " + e.getMessage());
            showAlert("Ошибка", "Не удалось синхронизировать данные");
        } finally {
            isLoading = false;
        }
    }

    private void applyFilter(String filterType) {
        if (isLoading) return;

        try {
            isLoading = true;
            currentFilter = filterType;


            List<Task> filteredTasks = new ArrayList<>();

            if (allTasksCache.isEmpty() ||
                    System.currentTimeMillis() - lastCacheUpdate > CACHE_TIMEOUT_MS) {
                allTasksCache = taskService.findAll();
                lastCacheUpdate = System.currentTimeMillis();
            }

            LocalDate today = LocalDate.now();

            switch (filterType) {
                case "all":
                    filteredTasks.addAll(allTasksCache);
                    break;

                case "today":
                    for (Task task : allTasksCache) {
                        if (task.getDueDate() != null &&
                                task.getDueDate().equals(today) &&
                                !task.isCompleted()) {
                            filteredTasks.add(task);
                        }
                    }
                    break;

                case "overdue":
                    for (Task task : allTasksCache) {
                        if (task.getDueDate() != null &&
                                task.getDueDate().isBefore(today) &&
                                !task.isCompleted()) {
                            filteredTasks.add(task);
                        }
                    }
                    break;

                case "completed":
                    for (Task task : allTasksCache) {
                        if (task.isCompleted()) {
                            filteredTasks.add(task);
                        }
                    }
                    break;

                case "pending":
                    for (Task task : allTasksCache) {
                        if (!task.isCompleted()) {
                            filteredTasks.add(task);
                        }
                    }
                    break;
            }

            taskTable.getSelectionModel().clearSelection();

            javafx.application.Platform.runLater(() -> {
                try {
                    taskData.setAll(filteredTasks);
                    System.out.println("Отображается задач в таблице: " + taskData.size());
                } catch (Exception e) {
                    System.err.println("Ошибка при обновлении таблицы: " + e.getMessage());
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isLoading = false;
        }
    }

    private void updateStatistics() {
        if (allTasksCache.isEmpty()) {
            return;
        }

        try {
            int total = allTasksCache.size();
            int completed = 0;
            int overdue = 0;

            for (Task task : allTasksCache) {
                if (task.isCompleted()) {
                    completed++;
                } else if (task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDate.now())) {
                    overdue++;
                }
            }

            int active = total - completed;

            String statsText = String.format(
                    "СТАТИСТИКА:\n" +
                            "Всего задач: %d\n" +
                            "Выполнено: %d (%.0f%%)\n" +
                            "Просрочено: %d\n" +
                            "Активных: %d",
                    total,
                    completed,
                    total > 0 ? (completed * 100.0 / total) : 0,
                    overdue,
                    active
            );

            statsLabel.setText(statsText);

        } catch (Exception e) {
            System.err.println("Ошибка при обновлении статистики: " + e.getMessage());
        }
    }

    private void showAddTaskDialog() {
        if (isLoading) return;

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Добавить задачу");
        dialog.setHeaderText("Введите данные новой задачи");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Название");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Описание");
        descArea.setPrefRowCount(3);
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField categoryField = new TextField();
        categoryField.setPromptText("Категория");

        ComboBox<Task.Priority> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(Task.Priority.values());
        priorityCombo.setValue(Task.Priority.MEDIUM);

        grid.add(new Label("Название:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Описание:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Срок:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Категория:"), 0, 3);
        grid.add(categoryField, 1, 3);
        grid.add(new Label("Приоритет:"), 0, 4);
        grid.add(priorityCombo, 1, 4);

        dialog.getDialogPane().setContent(grid);

        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                if (titleField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Название задачи не может быть пустым!");
                    return null;
                }

                if (datePicker.getValue() == null) {
                    showAlert("Ошибка", "Необходимо указать срок выполнения!");
                    return null;
                }

                Task task = new Task(
                        titleField.getText().trim(),
                        descArea.getText().trim(),
                        datePicker.getValue(),
                        priorityCombo.getValue(),
                        categoryField.getText().trim()
                );

                Long id = taskService.save(task);
                if (id != null) {
                    task.setId(id);

                    allTasksCache.add(task);
                    lastCacheUpdate = System.currentTimeMillis();

                    applyFilter(currentFilter);

                    updateStatistics();

                    for (int i = 0; i < taskData.size(); i++) {
                        Task t = taskData.get(i);
                        if (t.getId() != null && t.getId().equals(task.getId())) {
                            taskTable.getSelectionModel().select(i);
                            taskTable.scrollTo(i);
                            break;
                        }
                    }

                    return task;
                } else {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(task -> {
        });
    }

    private void showEditTaskDialog() {
        if (isLoading) return;

        Task selected = taskTable.getSelectionModel().getSelectedItem();

        if (selected != null && selected.getId() != null) {
            final Long taskId = selected.getId();

            Dialog<Task> dialog = new Dialog<>();

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField titleField = new TextField(selected.getTitle());
            TextArea descArea = new TextArea(selected.getDescription());
            descArea.setPrefRowCount(3);
            DatePicker datePicker = new DatePicker(selected.getDueDate());
            TextField categoryField = new TextField(selected.getCategory());
            ComboBox<Task.Priority> priorityCombo = new ComboBox<>();
            priorityCombo.getItems().addAll(Task.Priority.values());
            priorityCombo.setValue(selected.getPriority());
            CheckBox completedCheck = new CheckBox("Выполнена");
            completedCheck.setSelected(selected.isCompleted());

            grid.add(new Label("Название:"), 0, 0);
            grid.add(titleField, 1, 0);
            grid.add(new Label("Описание:"), 0, 1);
            grid.add(descArea, 1, 1);
            grid.add(new Label("Срок:"), 0, 2);
            grid.add(datePicker, 1, 2);
            grid.add(new Label("Категория:"), 0, 3);
            grid.add(categoryField, 1, 3);
            grid.add(new Label("Приоритет:"), 0, 4);
            grid.add(priorityCombo, 1, 4);
            grid.add(new Label("Статус:"), 0, 5);
            grid.add(completedCheck, 1, 5);

            dialog.getDialogPane().setContent(grid);

            ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    if (titleField.getText().trim().isEmpty()) {
                        showAlert("Ошибка", "Название задачи не может быть пустым!");
                        return null;
                    }

                    if (datePicker.getValue() == null) {
                        showAlert("Ошибка", "Необходимо указать срок выполнения!");
                        return null;
                    }

                    Long oldId = selected.getId();

                    selected.setTitle(titleField.getText().trim());
                    selected.setDescription(descArea.getText().trim());
                    selected.setDueDate(datePicker.getValue());
                    selected.setCategory(categoryField.getText().trim());
                    selected.setPriority(priorityCombo.getValue());
                    selected.setCompleted(completedCheck.isSelected());

                    try {
                        boolean updated = taskService.update(selected);
                        if (updated) {
                            for (int i = 0; i < allTasksCache.size(); i++) {
                                Task cachedTask = allTasksCache.get(i);
                                if (cachedTask.getId() != null && cachedTask.getId().equals(oldId)) {
                                    allTasksCache.set(i, selected);
                                    break;
                                }
                            }
                            lastCacheUpdate = System.currentTimeMillis();

                            return selected;
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            });

            dialog.showAndWait().ifPresent(updatedTask -> {
                taskTable.getSelectionModel().clearSelection();

                applyFilter(currentFilter);

                updateStatistics();

            });
        } else {
            showAlert("Предупреждение", "Выберите задачу для редактирования!");
        }
    }

    private void deleteSelectedTask() {
        if (isLoading) return;

        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getId() != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Подтверждение удаления");
            confirm.setHeaderText("Удалить задачу?");
            confirm.setContentText("Задача '" + selected.getTitle() + "' будет удалена безвозвратно.");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    System.out.println("Удаляем задачу ID=" + selected.getId());

                    boolean deleted = taskService.delete(selected.getId());
                    if (deleted) {
                        System.out.println("Задача удалена из БД, удаляем из кэша");

                        boolean foundInCache = false;
                        for (int i = allTasksCache.size() - 1; i >= 0; i--) {
                            Task task = allTasksCache.get(i);
                            if (task.getId() != null && task.getId().equals(selected.getId())) {
                                System.out.println("Удаляем из кэша задачу с ID=" + task.getId());
                                allTasksCache.remove(i);
                                foundInCache = true;
                                break;
                            }
                        }

                        if (!foundInCache) {
                            System.out.println("Задача не найдена в кэше, перезагружаем из БД");
                            allTasksCache = taskService.findAll();
                        }

                        lastCacheUpdate = System.currentTimeMillis();

                        taskTable.getSelectionModel().clearSelection();

                        applyFilter(currentFilter);

                        updateStatistics();

                        detailsArea.clear();

                        showAlert("Успех", "Задача удалена!");

                    } else {
                        showAlert("Ошибка", "Не удалось удалить задачу из базы данных");
                    }

                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            showAlert("Предупреждение", "Выберите задачу для удаления!");
        }
    }

    private void markAsCompleted() {
        if (isLoading) return;

        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (!selected.isCompleted()) {
                try {
                    boolean marked = taskService.markAsCompleted(selected.getId());
                    if (marked) {
                        boolean foundInCache = false;
                        for (Task task : allTasksCache) {
                            if (task.getId() != null && task.getId().equals(selected.getId())) {
                                System.out.println("Найдена задача в кэше, ID=" + task.getId() +
                                        ", было completed=" + task.isCompleted());
                                task.setCompleted(true);
                                foundInCache = true;
                                break;
                            }
                        }

                        if (!foundInCache) {
                            allTasksCache = taskService.findAll();
                            lastCacheUpdate = System.currentTimeMillis();
                        } else {
                            lastCacheUpdate = System.currentTimeMillis();
                        }

                        selected.setCompleted(true);

                        applyFilter(currentFilter);

                        updateStatistics();

                        showTaskDetails(selected);

                        showAlert("Успех", "Задача отмечена как выполненная!");

                    } else {
                        showAlert("Ошибка", "Не удалось отметить задачу как выполненную в БД");
                    }

                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            } else {
            }
        } else {
            showAlert("Предупреждение", "Выберите задачу для отметки о выполнении!");
        }
    }

    private void showTaskDetails(Task task) {
        if (task != null) {
            try {
                String details = String.format(
                        "Название:\n%s\n\n" +
                                "Описание:\n%s\n\n" +
                                "Срок:\n%s\n\n" +
                                "Приоритет:\n%s\n\n" +
                                "Категория:\n%s\n\n" +
                                "Статус:\n%s\n\n" +
                                "ID: %d",
                        task.getTitle(),
                        task.getDescription() != null && !task.getDescription().isEmpty()
                                ? task.getDescription() : "Нет описания",
                        task.getDueDate() != null ? task.getDueDate() : "Не указан",
                        task.getPriority() != null ? task.getPriority().getDisplayName() : "Не указан",
                        task.getCategory() != null && !task.getCategory().isEmpty()
                                ? task.getCategory() : "Без категории",
                        task.isCompleted() ? "ВЫПОЛНЕНА" : "В РАБОТЕ",
                        task.getId()
                );
                detailsArea.setText(details);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        } else {
            detailsArea.setText("");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public BorderPane getView() {
        return root;
    }
    private void safelyClearSelectionAndUpdate() {
        javafx.application.Platform.runLater(() -> {
            try {
                taskTable.getSelectionModel().clearSelection();
                detailsArea.clear();
            } catch (Exception e) {
                System.err.println("Ошибка при очистке выделения: " + e.getMessage());
            }
        });
    }
}