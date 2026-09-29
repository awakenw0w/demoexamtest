package ru.demoexam.algorithms.auth;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Показывает пазл из четырёх фрагментов и проверяет их порядок. */
public class PuzzleCaptcha extends VBox {
    private static final int PIECE_COUNT = 4;
    private static final double TILE_SIZE = 140;
    private static final String IMAGE_FOLDER = "/ru/demoexam/algorithms/auth/puzzle/";

    // Границы видимой части фрагментов в PNG-файлах.
    private static final Rectangle2D[] IMAGE_AREAS = {
            new Rectangle2D(281, 239, 421, 463),
            new Rectangle2D(0, 275, 626, 427),
            new Rectangle2D(314, 0, 388, 463),
            new Rectangle2D(0, 0, 403, 461)
    };

    private final Image[] images = new Image[PIECE_COUNT];
    private final int[] pieceAtCell = new int[PIECE_COUNT];
    private final GridPane puzzleGrid = new GridPane();
    private int selectedCell = -1;

    /** Загружает картинки и показывает перемешанный пазл. */
    public PuzzleCaptcha() {
        setSpacing(8);
        setAlignment(Pos.CENTER);
        loadImages();

        Label instructions = new Label(
                "Нажмите на два фрагмента, чтобы поменять их местами. "
                        + "Затем нажмите «Войти»."
        );
        instructions.setWrapText(true);
        puzzleGrid.setHgap(4);
        puzzleGrid.setVgap(4);
        puzzleGrid.setAlignment(Pos.CENTER);

        getChildren().addAll(new Label("Проверка пазлом"), instructions, puzzleGrid);
        shuffle();
    }

    /** Загружает четыре фрагмента из ресурсов приложения. */
    private void loadImages() {
        for (int pieceNumber = 0; pieceNumber < PIECE_COUNT; pieceNumber++) {
            String imageName = IMAGE_FOLDER + (pieceNumber + 1) + ".png";
            URL imageUrl = getClass().getResource(imageName);
            if (imageUrl == null) {
                throw new IllegalStateException("Не найден файл пазла: " + imageName);
            }
            images[pieceNumber] = new Image(imageUrl.toExternalForm());
        }
    }

    /** Перемешивает фрагменты и снимает прежнее выделение. */
    public void shuffle() {
        List<Integer> shuffledPieces = new ArrayList<>();
        for (int pieceNumber = 0; pieceNumber < PIECE_COUNT; pieceNumber++) {
            shuffledPieces.add(pieceNumber);
        }
        Collections.shuffle(shuffledPieces);

        // Если случайно выпал готовый ответ, сдвигаем фрагменты на одну позицию.
        boolean alreadySolved = true;
        for (int cell = 0; cell < PIECE_COUNT; cell++) {
            if (shuffledPieces.get(cell) != cell) {
                alreadySolved = false;
                break;
            }
        }
        if (alreadySolved) {
            Collections.rotate(shuffledPieces, 1);
        }

        for (int cell = 0; cell < PIECE_COUNT; cell++) {
            pieceAtCell[cell] = shuffledPieces.get(cell);
        }
        selectedCell = -1;
        drawPuzzle();
    }

    /** Возвращает true, если все четыре фрагмента стоят на своих местах. */
    public boolean isSolved() {
        for (int cell = 0; cell < PIECE_COUNT; cell++) {
            if (pieceAtCell[cell] != cell) {
                return false;
            }
        }
        return true;
    }

    /** Рисует четыре кнопки с изображениями в текущем порядке. */
    private void drawPuzzle() {
        puzzleGrid.getChildren().clear();

        for (int cell = 0; cell < PIECE_COUNT; cell++) {
            Button tileButton = createTileButton(cell);
            puzzleGrid.add(tileButton, cell % 2, cell / 2);
        }
    }

    /** Создаёт кнопку с фрагментом и назначает ей обработчик нажатия. */
    private Button createTileButton(int cell) {
        int pieceNumber = pieceAtCell[cell];
        ImageView imageView = new ImageView(images[pieceNumber]);
        imageView.setViewport(IMAGE_AREAS[pieceNumber]);
        imageView.setFitWidth(TILE_SIZE);
        imageView.setFitHeight(TILE_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Button tileButton = new Button();
        tileButton.setGraphic(imageView);
        tileButton.setPrefSize(TILE_SIZE + 8, TILE_SIZE + 8);
        tileButton.getStyleClass().add("puzzle-tile");
        if (cell == selectedCell) {
            tileButton.getStyleClass().add("selected");
        }
        tileButton.setOnAction(event -> selectTile(cell));
        return tileButton;
    }

    /** Выбирает фрагмент или меняет его местами с выбранным ранее. */
    private void selectTile(int cell) {
        if (selectedCell == -1) {
            selectedCell = cell;
        } else if (selectedCell == cell) {
            selectedCell = -1;
        } else {
            int savedPiece = pieceAtCell[selectedCell];
            pieceAtCell[selectedCell] = pieceAtCell[cell];
            pieceAtCell[cell] = savedPiece;
            selectedCell = -1;
        }
        drawPuzzle();
    }
}
