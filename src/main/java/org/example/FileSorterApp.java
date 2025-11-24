package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.SortingRule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class FileSorterApp extends Application {

    // --- МОДЕЛЬ ДАННЫХ ---
    // Пункт 3: ObservableList для динамических данных (Связь с TableView)
    private final ObservableList<SortingRule> rulesData = FXCollections.observableArrayList();

    // UI Элементы
    private TableView<SortingRule> table;
    private TextField filterField;
    private TextArea logArea;
    private ProgressBar progressBar;
    private PieChart statsChart;

    // Пункт 6: Файл для сохранения данных
    private static final String SAVE_FILE = "rules_db.csv";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Smart File Sorter - Курсовой проект");

        // Загружаем данные при старте
        loadData();

        // --- Вкладка 1: Управление правилами (CRUD) ---
        Tab rulesTab = createRulesTab(primaryStage);

        // --- Вкладка 2: Процесс сортировки ---
        Tab processingTab = createProcessingTab(primaryStage);

        // --- Вкладка 3: Статистика ---
        Tab statsTab = createStatsTab();

        // Пункт 4: TabPane для переключения вкладок
        TabPane tabPane = new TabPane(rulesTab, processingTab, statsTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Основной Layout
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 900, 600);

        // Пункт 5: Подключение CSS
        try {
            if (getClass().getResource("styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            }
        } catch (Exception e) {
            System.out.println("Стиль не найден, работаем на стандартном.");
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        // Сохранение данных при выходе
        primaryStage.setOnCloseRequest(e -> saveData());
    }

    // ==================== МОДУЛЬ 1: УПРАВЛЕНИЕ ПРАВИЛАМИ ====================

    private Tab createRulesTab(Stage stage) {
        Tab tab = new Tab("База правил");

        // Пункт 7: Фильтрация
        filterField = new TextField();
        filterField.setPromptText("🔍 Поиск по расширению...");

        FilteredList<SortingRule> filteredData = new FilteredList<>(rulesData, p -> true);

        // Пункт 8: Слушатель изменения текста
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(rule -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return rule.getExtension().toLowerCase().contains(newValue.toLowerCase());
            });
        });

        // Таблица
        table = new TableView<>();
        table.setItems(filteredData);
        table.setPlaceholder(new Label("Правил нет. Добавьте первое!"));

        TableColumn<SortingRule, String> extCol = new TableColumn<>("Расширение");
        extCol.setCellValueFactory(new PropertyValueFactory<>("extension"));

        TableColumn<SortingRule, String> folderCol = new TableColumn<>("Папка назначения");
        folderCol.setCellValueFactory(new PropertyValueFactory<>("targetFolder"));

        TableColumn<SortingRule, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.getColumns().addAll(extCol, folderCol, descCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Форма добавления
        TextField extInput = new TextField();
        extInput.setPromptText("Расширение (без точки)");

        // Пункт 9: ComboBox
        ComboBox<String> folderInput = new ComboBox<>();
        folderInput.getItems().addAll("Images", "Documents", "Music", "Video", "Archives", "Installers", "Code");
        folderInput.setPromptText("Выберите папку");
        folderInput.setEditable(true);

        TextField descInput = new TextField();
        descInput.setPromptText("Описание (опционально)");
        HBox.setHgrow(descInput, Priority.ALWAYS);

        Button addButton = new Button("Добавить");
        addButton.getStyleClass().add("action-btn");
        addButton.setOnAction(e -> {
            // Пункт 10: Валидация
            String ext = extInput.getText().trim();
            if (ext.startsWith(".")) ext = ext.substring(1);

            if (ext.isEmpty() || folderInput.getValue() == null) {
                showAlert("Ошибка", "Заполните расширение и папку!");
                return;
            }

            rulesData.add(new SortingRule(ext, folderInput.getValue(), descInput.getText()));

            extInput.clear();
            folderInput.getSelectionModel().clearSelection();
            descInput.clear();
            updateStats();
        });

        Button deleteButton = new Button("Удалить");
        deleteButton.getStyleClass().add("danger-btn");
        deleteButton.setOnAction(e -> {
            SortingRule selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Пункт 13: Подтверждение
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Подтверждение");
                alert.setHeaderText("Удалить правило для ." + selected.getExtension() + "?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    rulesData.remove(selected);
                    updateStats();
                }
            } else {
                showAlert("Внимание", "Выберите строку для удаления.");
            }
        });

        // Пункт 15: Layouts
        HBox inputArray = new HBox(10, extInput, folderInput, descInput, addButton, deleteButton);
        inputArray.setAlignment(Pos.CENTER_LEFT);
        inputArray.setPadding(new Insets(10));

        VBox layout = new VBox(10, filterField, table, inputArray);
        layout.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);

        tab.setContent(layout);
        return tab;
    }

    // ==================== МОДУЛЬ 2: ПРОЦЕССОР СОРТИРОВКИ ====================

    private Tab createProcessingTab(Stage stage) {
        Tab tab = new Tab("🚀 Сортировка");

        Label titleLabel = new Label("Сортировщик файлов");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button chooseDirBtn = new Button("📂 Выбрать папку...");
        Label pathLabel = new Label("Папка не выбрана");

        Button startBtn = new Button("ЗАПУСТИТЬ");
        startBtn.setDisable(true);
        startBtn.getStyleClass().add("action-btn");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);

        logArea = new TextArea();
        logArea.setEditable(false);

        chooseDirBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File file = dc.showDialog(stage);
            if (file != null) {
                pathLabel.setText(file.getAbsolutePath());
                startBtn.setDisable(false);
                startBtn.setUserData(file);
            }
        });

        startBtn.setOnAction(e -> {
            File dir = (File) startBtn.getUserData();
            runSortingLogic(dir);
        });

        VBox layout = new VBox(20, titleLabel, chooseDirBtn, pathLabel, startBtn, progressBar, logArea);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        tab.setContent(layout);

        return tab;
    }

    private void runSortingLogic(File directory) {
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        logArea.clear();
        logArea.appendText("Старт обработки: " + directory.getName() + "\n");

        Map<String, String> rulesMap = new HashMap<>();
        for (SortingRule rule : rulesData) {
            rulesMap.put(rule.getExtension().toLowerCase(), rule.getTargetFolder());
        }

        new Thread(() -> {
            try (Stream<Path> walk = Files.list(directory.toPath())) {
                var files = walk.filter(Files::isRegularFile).toList();
                int total = files.size();
                int current = 0;

                if (total == 0) {
                    Platform.runLater(() -> logArea.appendText("Папка пуста.\n"));
                    Platform.runLater(() -> progressBar.setProgress(0));
                    return;
                }

                for (Path source : files) {
                    String fileName = source.getFileName().toString();
                    String ext = getExtension(fileName);

                    if (rulesMap.containsKey(ext)) {
                        String targetFolder = rulesMap.get(ext);
                        Path targetDir = directory.toPath().resolve(targetFolder);

                        if (!Files.exists(targetDir)) Files.createDirectories(targetDir);

                        Path targetFile = targetDir.resolve(fileName);
                        targetFile = resolveNameConflict(targetFile);

                        Files.move(source, targetFile, StandardCopyOption.REPLACE_EXISTING);

                        String msg = "✅ " + fileName + " -> " + targetFolder;
                        Platform.runLater(() -> logArea.appendText(msg + "\n"));
                    } else {
                        Platform.runLater(() -> logArea.appendText("⚪ Пропущен: " + fileName + "\n"));
                    }

                    current++;
                    double progress = (double) current / total;
                    Platform.runLater(() -> progressBar.setProgress(progress));
                    Thread.sleep(30);
                }
                Platform.runLater(() -> {
                    logArea.appendText("--- ГОТОВО! ---");
                    progressBar.setProgress(1.0);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> logArea.appendText("Ошибка: " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    // ==================== МОДУЛЬ 3: СТАТИСТИКА ====================

    private Tab createStatsTab() {
        Tab tab = new Tab("📊 Статистика");

        // Пункт 12: Мини-статистика
        statsChart = new PieChart();
        statsChart.setTitle("Распределение правил");
        updateStats();

        Button aboutBtn = new Button("О программе");
        aboutBtn.setOnAction(e -> showAboutWindow());

        VBox layout = new VBox(20, statsChart, aboutBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        tab.setContent(layout);
        return tab;
    }

    private void updateStats() {
        if (statsChart == null) return;

        Map<String, Integer> counts = new HashMap<>();
        for (SortingRule rule : rulesData) {
            counts.put(rule.getTargetFolder(), counts.getOrDefault(rule.getTargetFolder(), 0) + 1);
        }

        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
        counts.forEach((folder, count) -> chartData.add(new PieChart.Data(folder, count)));

        statsChart.setData(chartData);
    }

    // Пункт 11: Отдельное окно
    private void showAboutWindow() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("О программе");

        Label title = new Label("File Sorter Coursework");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label desc = new Label("Курсовой проект по JavaFX.\nВыполнил студент.");
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button close = new Button("Закрыть");
        close.setOnAction(e -> dialog.close());

        VBox vbox = new VBox(15, title, desc, close);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));

        Scene scene = new Scene(vbox, 300, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ==================== УТИЛИТЫ ====================

    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0 && i < fileName.length() - 1) {
            return fileName.substring(i + 1).toLowerCase();
        }
        return "";
    }

    private Path resolveNameConflict(Path target) {
        if (!Files.exists(target)) return target;

        String fileName = target.getFileName().toString();
        String name = fileName;
        String ext = "";

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            name = fileName.substring(0, dotIndex);
            ext = fileName.substring(dotIndex);
        }

        int counter = 1;
        while (Files.exists(target)) {
            String newName = name + " (" + counter + ")" + ext;
            target = target.resolveSibling(newName);
            counter++;
        }
        return target;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void saveData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SAVE_FILE, StandardCharsets.UTF_8))) {
            for (SortingRule rule : rulesData) {
                writer.println(rule.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                SortingRule rule = SortingRule.fromString(line);
                if (rule != null) rulesData.add(rule);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}