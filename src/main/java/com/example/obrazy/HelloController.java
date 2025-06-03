package com.example.obrazy;

import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
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
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

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

    private static final ForkJoinPool customPool = new ForkJoinPool(4);

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

                showSuccessToast("Pomyślnie załadowano plik");
            } catch (Exception e) {
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

            if (newVal.length() > 0 && newVal.length() < 3) {
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
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs();
                }

                File outputFile = new File(picturesDir, fileName + ".jpg");

                if (outputFile.exists()) {
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
                    showSuccessToast("Zapisano obraz w pliku " + fileName + ".jpg");
                } else {
                    showErrorToast("Nie udało się zapisać pliku " + fileName + ".jpg");
                }
            } catch (IOException e) {
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
            Image scaled = scaleImage(base, dim.getKey(), dim.getValue());
            changedImage = scaled;
            changedImageView.setImage(scaled);
            imageChanged = true;
            updateImageViewSize(changedImageView, scaled);
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

    private Image scaleImage(Image source, int width, int height) {
        if (width == 0 || height == 0) {

            double ratio = source.getWidth() / source.getHeight();
            if (width == 0) {
                width = (int)(height * ratio);
            } else {
                height = (int)(width / ratio);
            }
        }

        return new Image(
                source.getUrl(), // or source.getUrl() if it’s not null
                width,
                height,
                false,  // preserveRatio
                false   // smooth (set true if you want anti-aliasing)
        );
    }

    private void rotateImage(double angle) {
        try {
            if (!imageChanged)
                changedImage = originalImage;

            BufferedImage image = SwingFXUtils.fromFXImage(changedImage, null);

            BufferedImage rotatedImage;
            if(angle%180 == 0) {
                rotatedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            }
            else{
                rotatedImage = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
            }

            java.awt.Graphics2D graphics = rotatedImage.createGraphics();
            graphics.rotate(Math.toRadians(angle), image.getWidth() / 2.0, image.getHeight() / 2.0);
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();

            changedImage = SwingFXUtils.toFXImage(rotatedImage, null);
            changedImageView.setImage(changedImage);
            imageChanged = true;
        }
        catch (Exception e) {showErrorToast("Nie można obrócić obrazka");}
    }

    private void negative() {
        try {
            // tworzymy kopie orginalu
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(originalImage, null);

            // negatyw
            customPool.submit(() ->
                    IntStream.range(0, bufferedImage.getHeight()).parallel().forEach(y -> {
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    int rgb = bufferedImage.getRGB(x, y);
                    int r = 255 - ((rgb >> 16) & 0xFF);
                    int g = 255 - ((rgb >> 8) & 0xFF);
                    int b = 255 - (rgb & 0xFF);
                    int newRgb = (r << 16) | (g << 8) | b;
                    bufferedImage.setRGB(x, y, newRgb);
                }
            })).get();

            // updatujemy do wyswietlenia
            changedImage = SwingFXUtils.toFXImage(bufferedImage, null);
            changedImageView.setImage(changedImage);
            imageChanged = true;

            showSuccessToast("Negatyw został wygenerowany pomyślnie!");
        } catch (Exception e) {
            showErrorToast("Nie udało się wykonać negatywu");
        }
    }

    private void thresholdDialog() {
        // Create modal dialog
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Progowanie obrazu");
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Create numeric input
        Spinner<Integer> thresholdSpinner = new Spinner<>(0, 255, 100);
        thresholdSpinner.setEditable(true);
        thresholdSpinner.getValueFactory().setValue(128);

        // Add buttons
        ButtonType applyButtonType = new ButtonType("Wykonaj progowanie", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(new Label("Wartość progu (0-255):"), 0, 0);
        grid.add(thresholdSpinner, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                return thresholdSpinner.getValue();
            }
            return null;
        });

        // Process result
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(threshold -> {
            try {
                threshold(threshold);
                showSuccessToast("Progowanie zostało przeprowadzone pomyślnie!");
            } catch (Exception e) {
                showErrorToast("Nie udało się wykonać progowania");
            }
        });
    }

    private void threshold(int threshold) {
        try {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(originalImage, null);

            // zamieniamy na czarno - bialy
            BufferedImage grayImage = new BufferedImage(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY
            );
            grayImage.getGraphics().drawImage(bufferedImage, 0, 0, null);

            // progowanie
            customPool.submit(() ->
                    IntStream.range(0, grayImage.getHeight()).parallel().forEach(y -> {
                        for (int x = 0; x < grayImage.getWidth(); x++) {
                            int rgb = grayImage.getRGB(x, y);
                            int gray = rgb & 0xFF;
                            int newValue = gray > threshold ? 255 : 0;
                            grayImage.setRGB(x, y, (newValue << 16) | (newValue << 8) | newValue);
                        }
                    })).get();

            // Update processed image
            changedImage = SwingFXUtils.toFXImage(grayImage, null);
            changedImageView.setImage(changedImage);
            imageChanged = true;

        } catch (Exception e) {
            throw new RuntimeException("Thresholding failed", e);
        }
    }

    private void contour() {
        try {
            int width = (int) originalImage.getWidth();
            int height = (int) originalImage.getHeight();

            WritableImage result = new WritableImage(width, height);
            PixelReader reader = originalImage.getPixelReader();
            PixelWriter writer = result.getPixelWriter();

            // Sobel kernels
            double[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
            double[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

            customPool.submit(() -> IntStream.range(1, height - 1).parallel().forEach(y -> {
                for (int x = 1; x < width - 1; x++) {
                    double pixelX = 0;
                    double pixelY = 0;

                    // Apply Sobel operator
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            Color color = reader.getColor(x + j, y + i);
                            double gray = 0.299 * color.getRed() +
                                    0.587 * color.getGreen() +
                                    0.114 * color.getBlue();
                            pixelX += gray * sobelX[i + 1][j + 1];
                            pixelY += gray * sobelY[i + 1][j + 1];
                        }
                    }

                    double magnitude = Math.sqrt(pixelX * pixelX + pixelY * pixelY);
                    double edgeValue = magnitude > 0.2 ? 1.0 : 0.0; // Threshold
                    writer.setColor(x, y, new Color(edgeValue, edgeValue, edgeValue, 1.0));
                }
            })).get();

            changedImage = result;
            changedImageView.setImage(result);
            imageChanged = true;
            showSuccessToast("Konturowanie zostało przeprowadzone pomyślnie!");
        } catch (Exception e) {
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