package ru.demoexam.algorithms;

import javafx.application.Application;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Desktop version of the original web exercise. */
public class AlgorithmsApp extends Application {
    private static final int MAX_UNDO_STEPS = 15;
    private static final String NAME_PATTERN = "[a-zA-Zа-яА-ЯёЁ\\s-]+";

    private final ObservableList<String> people = FXCollections.observableArrayList(
            "Алексей", "Андрей", "Варвара");
    private final Deque<List<String>> snapshots = new ArrayDeque<>();

    private final TextField leftNumber = new TextField();
    private final TextField rightNumber = new TextField();
    private final Label mathOutput = new Label("Готов к работе");
    private final TextField personName = new TextField();
    private final ListView<String> roster = new ListView<>(people);
    private final Label notice = new Label();
    private final PauseTransition noticeDelay = new PauseTransition(javafx.util.Duration.seconds(1.7));

    @Override
    public void start(Stage stage) {
        configureControls();

        VBox content = new VBox(14,
                new Label("Алгоритмы — практика"),
                new Label("Базовые операции, ввод и вывод данных, работа со списком."),
                calculatorPane(),
                namesPane());
        content.setPadding(new Insets(24));

        BorderPane root = new BorderPane(content);
        root.setBottom(notice);
        BorderPane.setMargin(notice, new Insets(0, 24, 18, 24));
        notice.setManaged(false);
        notice.setVisible(false);

        Scene scene = new Scene(root, 720, 620);
        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalKeys);

        stage.setTitle("Алгоритмы — практика");
        stage.setMinWidth(580);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();
    }

    private void configureControls() {
        leftNumber.setPromptText("5");
        rightNumber.setPromptText("6");
        personName.setPromptText("Например: инноКЕнТиЙ");

        roster.setCellFactory(view -> new ListCell<>() {
            private final Label number = new Label();
            private final Label name = new Label();
            private final Button remove = new Button("X");
            private final HBox row = new HBox(10, number, name, new Region(), remove);

            {
                name.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(name, Priority.ALWAYS);
                HBox.setHgrow(row.getChildren().get(2), Priority.ALWAYS);
                remove.getStyleClass().add("remove-row");
                remove.setOnAction(event -> removeAt(getIndex()));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    number.setText(String.format("%02d", getIndex() + 1));
                    name.setText(item);
                    setGraphic(row);
                }
            }
        });
    }

    private VBox calculatorPane() {
        Label heading = new Label("1. Калькулятор");
        heading.getStyleClass().add("section-title");
        Button sum = new Button("Сложить +");
        sum.setOnAction(event -> calculate(true));
        Button difference = new Button("Вычесть −");
        difference.setOnAction(event -> calculate(false));

        HBox arguments = new HBox(8, new Label("Аргумент А:"), leftNumber,
                new Label("Аргумент Б:"), rightNumber);
        arguments.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, heading, arguments, new HBox(8, sum, difference), mathOutput);
    }

    private VBox namesPane() {
        Label heading = new Label("2. Список имён");
        heading.getStyleClass().add("section-title");
        Button add = new Button("Добавить в список");
        add.setOnAction(event -> insertCurrentName());
        Button scan = new Button("Проверить символы");
        scan.setOnAction(event -> scanName());
        Button undo = new Button("Отменить");
        undo.setOnAction(event -> rollbackRoster());
        personName.setOnAction(event -> insertCurrentName());
        roster.setPrefHeight(230);

        return new VBox(8, heading,
                new HBox(8, new Label("Новое имя:"), personName),
                new HBox(8, add, scan, undo),
                new Label("Клик — выбрать | ↑ ↓ — навигация | Ctrl+Z — отмена"), roster);
    }

    private void calculate(boolean sum) {
        Optional<double[]> pair = readPair();
        if (pair.isEmpty()) return;
        double a = pair.get()[0];
        double b = pair.get()[1];
        double value = sum ? a + b : a - b;
        mathOutput.setText(number(a) + (sum ? " + " : " − ") + number(b) + " = " + number(value));
    }

    private Optional<double[]> readPair() {
        if (leftNumber.getText().isBlank() || rightNumber.getText().isBlank()) {
            notify("Нужно заполнить оба аргумента");
            return Optional.empty();
        }
        try {
            return Optional.of(new double[]{Double.parseDouble(leftNumber.getText()), Double.parseDouble(rightNumber.getText())});
        } catch (NumberFormatException exception) {
            notify("Аргументы должны быть числами");
            return Optional.empty();
        }
    }

    private String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void insertCurrentName() {
        String raw = personName.getText();
        if (raw.isBlank()) {
            notify("Введите имя");
            return;
        }
        if (containsGarbage(raw)) {
            offerCleanup();
            return;
        }
        String prepared = formatName(raw);
        if (people.stream().anyMatch(person -> person.equalsIgnoreCase(prepared))) {
            showInfo("Уже есть в списке");
            return;
        }
        rememberRoster();
        people.add(prepared);
        roster.getSelectionModel().selectLast();
        personName.clear();
        notify("Добавлено: " + prepared);
    }

    private void scanName() {
        if (personName.getText().isBlank()) {
            notify("Сначала введите текст");
        } else if (containsGarbage(personName.getText())) {
            offerCleanup();
        } else {
            notify("Лишних символов нет");
        }
    }

    private boolean containsGarbage(String source) {
        return !source.matches(NAME_PATTERN);
    }

    private String formatName(String source) {
        String compact = source.replaceAll("\\s+", "");
        if (compact.isEmpty()) return "";
        String lowered = compact.toLowerCase(Locale.forLanguageTag("ru-RU"));
        return lowered.substring(0, 1).toUpperCase(Locale.forLanguageTag("ru-RU")) + lowered.substring(1);
    }

    private void offerCleanup() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                "В строке есть цифры или посторонние знаки. Очистить их?",
                ButtonType.YES, ButtonType.NO);
        dialog.setTitle("Лишние символы");
        dialog.setHeaderText("Лишние символы");
        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.orElse(ButtonType.NO) == ButtonType.YES) {
            personName.setText(personName.getText().replaceAll("[^a-zA-Zа-яА-ЯёЁ\\s-]", ""));
            personName.requestFocus();
            notify("Лишние символы удалены");
        }
    }

    private void removeAt(int index) {
        if (index < 0 || index >= people.size()) return;
        rememberRoster();
        people.remove(index);
        if (!people.isEmpty()) roster.getSelectionModel().select(Math.min(index, people.size() - 1));
        notify("Строка удалена");
    }

    private void rememberRoster() {
        snapshots.push(List.copyOf(people));
        if (snapshots.size() > MAX_UNDO_STEPS) snapshots.removeLast();
    }

    private void rollbackRoster() {
        if (snapshots.isEmpty()) {
            notify("Нет действий для отмены");
            return;
        }
        people.setAll(snapshots.pop());
        roster.getSelectionModel().clearSelection();
        notify("Последнее изменение отменено");
    }

    private void handleGlobalKeys(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.Z) {
            event.consume();
            rollbackRoster();
            return;
        }
        if (event.getCode() == KeyCode.DOWN) {
            event.consume();
            moveSelection(1);
        } else if (event.getCode() == KeyCode.UP) {
            event.consume();
            moveSelection(-1);
        }
    }

    private void moveSelection(int direction) {
        if (people.isEmpty()) return;
        int selected = roster.getSelectionModel().getSelectedIndex();
        int next = selected < 0
                ? (direction > 0 ? 0 : people.size() - 1)
                : Math.floorMod(selected + direction, people.size());
        roster.getSelectionModel().select(next);
        roster.scrollTo(next);
    }

    private void showInfo(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void notify(String text) {
        notice.setText(text);
        notice.setManaged(true);
        notice.setVisible(true);
        noticeDelay.stop();
        noticeDelay.setOnFinished(event -> {
            notice.setManaged(false);
            notice.setVisible(false);
        });
        noticeDelay.playFromStart();
    }
}
