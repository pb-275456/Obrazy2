package com.example.obrazy;

import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.util.converter.IntegerStringConverter;
import org.controlsfx.control.Notifications;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.UnaryOperator;


public class HelloController {
    @FXML private ComboBox<String> operations;
    @FXML private Button loadButton;
    @FXML private Button executeButton;
    @FXML private Button saveButton;
    @FXML private Button scaleButton;
    @FXML private ImageView originalImageView;
    @FXML private ImageView changedImageView;
    @FXML private Button rotateLeftBtn;
    @FXML private Button rotateRightBtn;


    private Image originalImage;
    private Image changedImage;
    private Boolean imageChanged = false;


    Filters filters = new Filters();

    @FXML
    public void initialize() {
        //ustawienie przyciskow na disabled
        operations.setDisable(true);
        executeButton.setDisable(true);
        saveButton.setDisable(true);
        scaleButton.setDisable(true);
        rotateLeftBtn.setDisable(true);
        rotateRightBtn.setDisable(true);
    }

    @FXML
    protected void onExecuteButtonClick() {
        String selectedOperation = operations.getValue();

        if (selectedOperation == null) {
            showErrorToast("Nie wybrano operacji do wykonania");

        } else {
            //jakos to potem zmienic

            switch (operations.getValue()) {
                case "Negatyw":
                    negative();
                    break;

                case "Progowanie":
                    thresholdDialog();
                    break;

                case "Kontur":
                    contour();
                    break;
            }
        }
    }

    @FXML
    public void loadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz obrazek");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Pliki JPG", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(loadButton.getScene().getWindow());

        if (file != null) {
            if (!file.getName().toLowerCase().endsWith(".jpg") &&
                    !file.getName().toLowerCase().endsWith(".jpeg")) {
                showErrorToast("Niedozwolony format pliku");
                return;
            }

            try {
                // usun poprzednie wersje
                originalImageView.setImage(null);
                changedImageView.setImage(null);
                originalImage = null;

                // wczytaj nowy do image view
                originalImage = new Image(file.toURI().toString());
                originalImageView.setImage(originalImage);
                imageChanged = false;
                changedImage = null;

                //update rozmiaru obrazka
                updateImageViewSize(originalImageView, originalImage);

                // odblokuj opcje
                operations.setDisable(false);
                executeButton.setDisable(false);
                saveButton.setDisable(false);
                scaleButton.setDisable(false);
                rotateLeftBtn.setDisable(false);
                rotateRightBtn.setDisable(false);

                Logs.logInfo("Załadowano pomyślnie plik: " + file.getName());
                showSuccessToast("Pomyślnie załadowano plik");
            } catch (Exception e) {
                Logs.logError("Bład przy wczytywaniu pliku", e);
                showErrorToast("Nie udało się załadować pliku");
            }
        }
    }

    @FXML
    public void onSaveButtonClick() {
        //tworzymy dialog do interakcji z innymi apkami
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Zapisz obraz");
        dialog.initModality(Modality.APPLICATION_MODAL);

        //tworzymy nowy label do pokazania informacji o braku zmiany obrazu
        Label warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: orange;");
        warningLabel.setVisible(false);

        if (!imageChanged) {
            warningLabel.setText("Na pliku nie zostały wykonane żadne operacje!");
            warningLabel.setVisible(true);
        }

        //dajemy textField do podania nazwy pliku
        TextField fileNameField = new TextField();
        fileNameField.setPromptText("Wpisz nazwę pliku (3-100 znaków)");
        fileNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 100) {
                fileNameField.setText(oldVal);
            }
        });

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(warningLabel, 0, 0, 2, 1);
        grid.add(new Label("Nazwa pliku:"), 0, 1);
        grid.add(fileNameField, 1, 1);
        grid.add(errorLabel, 1, 2);

        dialog.getDialogPane().setContent(grid);

        //walidacja nazwy
        fileNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean isValid = newVal.length() >= 3;
            saveButton.setDisable(!isValid);

            if (!newVal.isEmpty() && newVal.length() < 3) {
                errorLabel.setText("Wpisz co najmniej 3 znaki");
                errorLabel.setVisible(true);
            } else {
                errorLabel.setVisible(false);
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return fileNameField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(fileName -> {
            try {
                File picturesDir = new File(System.getProperty("user.home"), "Desktop");

                File outputFile = new File(picturesDir, fileName + ".jpg");

                if (outputFile.exists()) {
                    Logs.logWarning("Plik już istnieje:" + fileName);
                    showErrorToast("Plik " + fileName + ".jpg już istnieje...");
                    return;
                }

                BufferedImage bImage = SwingFXUtils.fromFXImage(changedImageView.getImage(), null);
                BufferedImage rgbImage = new BufferedImage(
                        bImage.getWidth(),
                        bImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                );
                rgbImage.getGraphics().drawImage(bImage, 0, 0, null);

                if (ImageIO.write(rgbImage, "jpg", outputFile)) {
                    Logs.logInfo("Pomyślnie zapisano pliku: " + fileName);
                    showSuccessToast("Zapisano obraz w pliku " + fileName + ".jpg");
                } else {
                    Logs.logWarning("Nie udało zapisać się pliku: " + fileName);
                    showErrorToast("Nie udało się zapisać pliku " + fileName + ".jpg");
                }
            } catch (IOException e) {
                Logs.logError("Nie udało się zapisać pliku: ", e);
                showErrorToast("Nie udało się zapisać pliku " + fileName + ".jpg");
            }
        });
    }

    @FXML
    public void onScaleButtonClick() {
        //tworzymy dialog do interakcji z innymi apkami
        Dialog<Pair<Integer, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Skalowanie Obrazu");
        dialog.initModality(Modality.APPLICATION_MODAL);

        //tworzenie pol
        TextField widthField = createNumericField();
        TextField heightField = createNumericField();
        Label widthError = createErrorLabel();
        Label heightError = createErrorLabel();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.addRow(0, new Label("Szerokość (px):"), widthField);
        grid.add(widthError, 1, 1);
        grid.addRow(2, new Label("Wysokość (px):"), heightField);
        grid.add(heightError, 1, 3);

        ButtonType scaleBtn = new ButtonType("Zmień rozmiar", ButtonBar.ButtonData.OK_DONE);
        ButtonType restoreBtn = new ButtonType("Przywróć oryginał", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(scaleBtn, restoreBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);

        Node scaleBtnNode = dialog.getDialogPane().lookupButton(scaleBtn);
        scaleBtnNode.setDisable(true);

        ChangeListener<String> validationListener = (obs, oldVal, newVal) -> {
            boolean valid = !widthField.getText().isEmpty() && !heightField.getText().isEmpty();
            scaleBtnNode.setDisable(!valid);
            widthError.setVisible(widthField.getText().isEmpty());
            heightError.setVisible(heightField.getText().isEmpty());
        };
        widthField.textProperty().addListener(validationListener);
        heightField.textProperty().addListener(validationListener);

        dialog.setResultConverter(button -> {
            if (button == scaleBtn || button == restoreBtn) {
                int w = button == restoreBtn ? (int) originalImage.getWidth() : Integer.parseInt(widthField.getText());
                int h = button == restoreBtn ? (int) originalImage.getHeight() : Integer.parseInt(heightField.getText());
                return new Pair<>(w, h);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dim -> {
            Image base = (changedImage != null) ? changedImage : originalImage;

            changedImage = filters.scaleImage(base, dim.getKey(), dim.getValue());
            changedImageView.setImage(changedImage);
            imageChanged = true;
            updateImageViewSize(changedImageView, changedImage);

            Logs.logInfo("Udało się przeskalować obraz");
            showSuccessToast("Przeskalowano obraz do " + dim.getKey() + "x" + dim.getValue() + "px");
        });
    }

    @FXML
    public void handleRotateLeft() {
        rotateImage(-90);
    }

    @FXML
    public void handleRotateRight() {
        rotateImage(90);
    }

    public void rotateImage(double angle) {
        Image currentImage = (changedImage != null) ? changedImage : originalImage;

        ImageView view = new ImageView(currentImage);
        view.setRotate(angle);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        WritableImage result = view.snapshot(params, null);
        changedImage = result;
        changedImageView.setImage(result);
        updateImageViewSize(changedImageView, changedImage);
    }

    private void negative() {
        try {
            if (!imageChanged)
                changedImage = originalImage;

            // updatujemy do wyswietlenia
            changedImage = filters.applyNegative(changedImage);
            changedImageView.setImage(changedImage);
            updateImageViewSize(changedImageView, changedImage);
            imageChanged = true;

            Logs.logInfo("Udało się wykonać negatyw");
            showSuccessToast("Negatyw został wygenerowany pomyślnie!");
        } catch (Exception e) {
            Logs.logError("Nie udało się wykonać negatywu: ", e);
            showErrorToast("Nie udało się wykonać negatywu");
        }
    }

    private void thresholdDialog() {
        //dialog
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Progowanie obrazu");
        dialog.initModality(Modality.APPLICATION_MODAL);

        // wejscie numeryczne
        Spinner<Integer> thresholdSpinner = new Spinner<>(0, 255, 100);
        thresholdSpinner.setEditable(true);
        thresholdSpinner.getValueFactory().setValue(128);

        // przyciski
        ButtonType applyButton = new ButtonType("Wykonaj progowanie", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButton, ButtonType.CANCEL);

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(new Label("Wartość progu (0-255):"), 0, 0);
        grid.add(thresholdSpinner, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == applyButton) {
                return thresholdSpinner.getValue();
            }
            return null;
        });

        // zwrot wyniku -> threshold
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(threshold -> {
            try {
                threshold(threshold);
                Logs.logInfo("Udało się wykonać progowanie");
                showSuccessToast("Progowanie zostało przeprowadzone pomyślnie!");
            } catch (Exception e) {
                Logs.logError("Nie udało się wykonać progowania:", e);
                showErrorToast("Nie udało się wykonać progowania");
            }
        });
    }

    private void threshold(int threshold) {
        try {
            changedImage = filters.applyThreshold(originalImage, threshold);
            changedImageView.setImage(changedImage);
            imageChanged = true;
            updateImageViewSize(changedImageView, changedImage);

        } catch (Exception e) {
            throw new RuntimeException("Thresholding failed", e);
        }
    }

    private void contour() {
        try {
            changedImage = filters.applyContour(originalImage);
            changedImageView.setImage(changedImage);
            imageChanged = true;
            updateImageViewSize(changedImageView, changedImage);

            Logs.logInfo("Udało się wykonać kontirowanie");
            showSuccessToast("Konturowanie zostało przeprowadzone pomyślnie!");
        } catch (Exception e) {
            Logs.logError("Nie udało się wykonać konturowania: ", e);
            showErrorToast("Nie udało się wykonać konturowania");
        }
    }

    private void updateImageViewSize(ImageView imageView, Image image) {
        double minSize = 70;
        double maxSize = 300;

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        double fitWidth = Math.max(minSize, Math.min(maxSize, imageWidth));
        double fitHeight = Math.max(minSize, Math.min(maxSize, imageHeight));

        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        imageView.setPreserveRatio(true);
    }

    private void showSuccessToast(String message) {
        Notifications.create()
                .title("Sukces")
                .text(message)
                .hideAfter(Duration.seconds(3))
                .position(Pos.CENTER)
                .show();
    }

    private void showErrorToast(String message) {
        Notifications.create()
                .title("Błąd")
                .text(message)
                .hideAfter(Duration.seconds(3))
                .position(Pos.CENTER)
                .show();
    }

    private TextField createNumericField() {
        TextField field = new TextField();
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.matches("\\d*") && (text.isEmpty() || Integer.parseInt((text))<=0 || Integer.parseInt(text) <= 3000)) {
                return change;
            }
            return null;
        };
        field.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, filter));
        return field;
    }

    private Label createErrorLabel() {
        Label label = new Label("Pole jest wymagane");
        label.setStyle("-fx-text-fill: red;");
        label.setVisible(false);
        return label;
    }
}