package com.example.obrazy;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
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
import java.util.function.BiConsumer;
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
    private double totalRotation = 0; //do obracania addytywnego

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
                    imageChanged = true;
                    break;

                case "Progowanie":
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

                // wczytaj nowy
                originalImage = new Image(file.toURI().toString());
                originalImageView.setImage(originalImage);

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
                if (ImageIO.write(bImage, "jpg", outputFile)) {
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

        //tworzymy nowy label do pokazania informacji o braku zmiany obrazu
        Label widthError = new Label();
        Label heightError = new Label();
        widthError.setStyle("-fx-text-fill: red;");
        heightError.setStyle("-fx-text-fill: red;");
        widthError.setVisible(false);
        heightError.setVisible(false);

        TextField widthField = new TextField();
        TextField heightField = new TextField();

        //walidacja wpisywania liczb
        UnaryOperator<TextFormatter.Change> numericFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { //tylko liczby
                if (!newText.isEmpty()) {
                    int value = Integer.parseInt(newText);
                    if (value > 3000) {
                        return null; // do 3000
                    }
                }
                return change;
            }
            return null;
        };

        widthField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, numericFilter));
        heightField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, numericFilter));

        // przyciski
        ButtonType scaleButtonType = new ButtonType("Zmień rozmiar", ButtonBar.ButtonData.OK_DONE);
        ButtonType restoreButtonType = new ButtonType("Przywróć oryginał", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(scaleButtonType, restoreButtonType, ButtonType.CANCEL);

        // layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(new Label("Szerokość (px):"), 0, 0);
        grid.add(widthField, 1, 0);
        grid.add(widthError, 1, 1);

        grid.add(new Label("Wysokość (px):"), 0, 2);
        grid.add(heightField, 1, 2);
        grid.add(heightError, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node scaleBtn = dialog.getDialogPane().lookupButton(scaleButtonType);
        scaleBtn.setDisable(true);

        BiConsumer<TextField, Label> validation = (field, errorLabel) -> {
            field.textProperty().addListener((obs, oldVal, newVal) -> {
                boolean bothValid = !widthField.getText().isEmpty() && !heightField.getText().isEmpty();
                scaleBtn.setDisable(!bothValid);

                if (field == widthField) {
                    widthError.setVisible(newVal.isEmpty());
                } else {
                    heightError.setVisible(newVal.isEmpty());
                }
            });
        };

        validation.accept(widthField, widthError);
        validation.accept(heightField, heightError);

        // Set result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == scaleButtonType) {
                if (widthField.getText().isEmpty()) {
                    widthError.setText("Pole jest wymagane");
                    widthError.setVisible(true);
                    return null;
                }
                if (heightField.getText().isEmpty()) {
                    heightError.setText("Pole jest wymagane");
                    heightError.setVisible(true);
                    return null;
                }

                try {
                    return new Pair<>(
                            Integer.parseInt(widthField.getText()),
                            Integer.parseInt(heightField.getText())
                    );
                } catch (NumberFormatException e) {
                    return null;
                }
            } else if (dialogButton == restoreButtonType) {
                return new Pair<>(
                        (int)originalImage.getWidth(),
                        (int)originalImage.getHeight()
                );
            }
            return null;
        });

        // Handle the result properly
        Optional<Pair<Integer, Integer>> result = dialog.showAndWait();
        result.ifPresent(dimensions -> {
            int newWidth = dimensions.getKey();
            int newHeight = dimensions.getValue();

            System.out.println("originalImage = " + originalImage);
            System.out.println("changedImage = " + changedImage);

            Image scaledImage;
            // Scaling logic here
            if (!imageChanged)
            {   scaledImage = scaleImage(originalImage, newWidth, newHeight);}
            else {
                scaledImage = scaleImage(changedImage, newWidth, newHeight);
            }
            changedImageView.setImage(scaledImage);
            changedImage = scaledImage;
            imageChanged = true;

            showSuccessToast("Przeskalowano obraz do " + newWidth + "x" + newHeight + "px");
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
            // Preserve aspect ratio if one dimension is 0
            double ratio = source.getWidth() / source.getHeight();
            if (width == 0) {
                width = (int)(height * ratio);
            } else {
                height = (int)(width / ratio);
            }
        }

        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            System.err.println("PixelReader is null. Cannot scale.");
            return null;
        }

        WritableImage output = new WritableImage(width, height);
        PixelWriter writer = output.getPixelWriter();

        double scaleX = source.getWidth() / width;
        double scaleY = source.getHeight() / height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcX = (int) (x * scaleX);
                int srcY = (int) (y * scaleY);
                writer.setArgb(x, y, reader.getArgb(srcX, srcY));
            }
        }

        return output;
    }


    private void rotateImage(double angle) {
        if(!imageChanged)
            changedImage = originalImage;

        totalRotation = (totalRotation + angle) % 360;

        double pivotX = changedImage.getWidth() / 2;
        double pivotY = changedImage.getHeight() / 2;

        ImageView imageView = new ImageView(changedImage);
        imageView.getTransforms().clear();
        // Create rotation transformation
        Rotate rotate = new Rotate(totalRotation, pivotX, pivotY);


        imageView.getTransforms().add(rotate);

        // Create snapshot of rotated image
        WritableImage rotatedImage = imageView.snapshot(null, null);

        // Update processed image
        changedImage = rotatedImage;
        changedImageView.setImage(rotatedImage);
    }

    private void negative() {
        try {
            // tworzymy kopie orginalu
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(originalImage, null);

            // Apply negative effect
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    int rgb = bufferedImage.getRGB(x, y);
                    int r = 255 - ((rgb >> 16) & 0xFF);
                    int g = 255 - ((rgb >> 8) & 0xFF);
                    int b = 255 - (rgb & 0xFF);
                    int newRgb = (r << 16) | (g << 8) | b;
                    bufferedImage.setRGB(x, y, newRgb);
                }
            }

            // Update processed image
            changedImage = SwingFXUtils.toFXImage(bufferedImage, null);
            changedImageView.setImage(changedImage);
            imageChanged = true;

            showSuccessToast("Negatyw został wygenerowany pomyślnie!");
        } catch (Exception e) {
            showErrorToast("Nie udało się wykonać negatywu");
            e.printStackTrace();
        }
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
}