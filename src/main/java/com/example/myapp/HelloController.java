package com.example.myapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HelloController {

    // Ezek a mezők az FXML fx:id attribútumaihoz kapcsolódnak.
    // JavaFX automatikusan beinjektálja őket, amikor betölti az FXML-t.
    @FXML
    private BorderPane rootPane;

    @FXML
    private ListView<String> fileList;

    @FXML
    private TextArea editText;

    // Eltároljuk, melyik fájlt szerkesztjük éppen,
    // hogy a "Save" gomb tudja, hova mentsen.
    private File currentFile;

    // Eltároljuk az aktuálisan megnyitott mappát is,
    // hogy a fileList-ben rákattintva meg tudjuk nyitni a fájlokat.
    private File currentFolder;

    // --- FILE MENÜ ---

    @FXML
    private void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Fájl megnyitása");
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                editText.setText(content);
                currentFile = file; // ezt szerkesztjük most
            } catch (IOException e) {
                showError("Nem sikerült megnyitni a fájlt: " + e.getMessage());
            }
        }
    }

    @FXML
    private void openFolder(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Mappa megnyitása");
        File folder = dirChooser.showDialog(rootPane.getScene().getWindow());

        if (folder != null) {
            currentFolder = folder;
            fileList.getItems().clear();

            // Csak a fájlneveket listázzuk ki (nem a teljes elérési utat)
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    fileList.getItems().add(f.getName());
                }
            }
        }
    }

    // Amikor a felhasználó rákattint egy fájlnévre a listában,
    // betöltjük annak tartalmát a TextArea-ba.
    @FXML
    private void fileListClicked() {
        String selectedName = fileList.getSelectionModel().getSelectedItem();
        if (selectedName != null && currentFolder != null) {
            File file = new File(currentFolder, selectedName);
            try {
                String content = Files.readString(file.toPath());
                editText.setText(content);
                currentFile = file;
            } catch (IOException e) {
                showError("Nem sikerült beolvasni a fájlt: " + e.getMessage());
            }
        }
    }

    @FXML
    private void save(ActionEvent event) {
        if (currentFile == null) {
            // Ha még nincs fájl kiválasztva, viselkedjen úgy, mint a "Save as"
            saveAs(event);
            return;
        }
        writeToFile(currentFile);
    }

    @FXML
    private void saveAs(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Mentés másként");
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            writeToFile(file);
            currentFile = file; // mostantól ezt a fájlt szerkesztjük
        }
    }

    // Közös metódus, amit a save() és saveAs() is használ,
    // hogy ne kelljen kétszer megírni ugyanazt a logikát.
    private void writeToFile(File file) {
        try {
            Files.writeString(file.toPath(), editText.getText());
        } catch (IOException e) {
            showError("Nem sikerült menteni a fájlt: " + e.getMessage());
        }
    }

    // --- EDIT MENÜ ---

    @FXML
    private void deleteItem(ActionEvent event) {
        String selectedName = fileList.getSelectionModel().getSelectedItem();
        if (selectedName != null && currentFolder != null) {
            File file = new File(currentFolder, selectedName);
            if (file.delete()) {
                fileList.getItems().remove(selectedName);
                if (file.equals(currentFile)) {
                    currentFile = null;
                    editText.clear();
                }
            } else {
                showError("Nem sikerült törölni a fájlt.");
            }
        }
    }

    // --- HELP MENÜ ---

    @FXML
    private void aboutItem(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Névjegy");
        alert.setHeaderText(null);
        alert.setContentText("Egyszerű szövegszerkesztő JavaFX-ben.");
        alert.showAndWait();
    }

    // --- SEGÉD METÓDUS ---

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Hiba");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
