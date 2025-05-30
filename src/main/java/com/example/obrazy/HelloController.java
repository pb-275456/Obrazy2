package com.example.obrazy;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class HelloController {
    @FXML private ComboBox<String> operations;
    @FXML private Button loadButton;
    @FXML private Button executeButton;
    @FXML private Button saveButton;
    @FXML private ImageView originalImageView;
    @FXML private ImageView changedImageView;

    private Image currentImage;
    private Boolean imageChanged = false;

    @FXML
    public void initialize() {
        //ustawienie przyciskow na disabled
        operations.setDisable(true);
        executeButton.setDisable(true);
        saveButton.setDisable(true);
    }

    @FXML
    protected void onExecuteButtonClick() {
        String selectedOperation = operations.getValue();

        if (selectedOperation == null) {
            showErrorToast("Nie wybrano operacji do wykonania");

        } else {
            //jakos to potem zmienic
            changedImageView.setImage(currentImage);
            imageChanged = true;
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
                currentImage = null;

                // wczytaj nowy
                currentImage = new Image(file.toURI().toString());
                originalImageView.setImage(currentImage);

                // odblokuj opcje
                operations.setDisable(false);
                executeButton.setDisable(false);
                saveButton.setDisable(false);

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