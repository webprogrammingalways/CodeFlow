package com.example.myapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.HTMLEditor;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class HelloController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private ListView<String> fileList;

    @FXML
    private StackPane editorStack;

    @FXML
    private TextArea editText;

    @FXML
    private HTMLEditor htmlEditor;

    private File currentFile;
    private File currentFolder;

    // Melyik szerkesztő van most használatban: docx -> HTMLEditor, minden más -> TextArea
    private boolean docxMode = false;

    // --- FILE MENÜ ---

    @FXML
    private void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Fájl megnyitása");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Minden támogatott", "*.txt", "*.docx", "*.java", "*.md", "*.json", "*.xml"),
                new FileChooser.ExtensionFilter("Szövegfájl / kód", "*.txt", "*.java", "*.md", "*.json", "*.xml"),
                new FileChooser.ExtensionFilter("Word dokumentum", "*.docx")
        );
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            loadFile(file);
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

            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    fileList.getItems().add(f.getName());
                }
            }
        }
    }

    @FXML
    private void fileListClicked() {
        String selectedName = fileList.getSelectionModel().getSelectedItem();
        if (selectedName != null && currentFolder != null) {
            File file = new File(currentFolder, selectedName);
            loadFile(file);
        }
    }

    // Közös betöltő metódus: a kiterjesztés alapján dönti el,
    // hogy .docx (HTMLEditor, formázással) vagy sima szöveg (TextArea) legyen-e.
    private void loadFile(File file) {
        try {
            if (isDocx(file)) {
                String html = docxToHtml(file);
                htmlEditor.setHtmlText(html);
                switchToHtmlEditor();
            } else {
                editText.setText(Files.readString(file.toPath()));
                switchToTextArea();
            }
            currentFile = file;
        } catch (IOException e) {
            showError("Nem sikerült megnyitni a fájlt: " + e.getMessage());
        }
    }

    // Átkapcsol HTMLEditor nézetre (docx-hez), elrejtve a TextArea-t.
    private void switchToHtmlEditor() {
        docxMode = true;
        editText.setVisible(false);
        editText.setManaged(false);
        htmlEditor.setVisible(true);
        htmlEditor.setManaged(true);
    }

    // Átkapcsol sima TextArea nézetre (txt/kód-hoz), elrejtve a HTMLEditor-t.
    private void switchToTextArea() {
        docxMode = false;
        htmlEditor.setVisible(false);
        htmlEditor.setManaged(false);
        editText.setVisible(true);
        editText.setManaged(true);
    }

    @FXML
    private void save(ActionEvent event) {
        if (currentFile == null) {
            saveAs(event);
            return;
        }
        writeToFile(currentFile);
    }

    @FXML
    private void saveAs(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Mentés másként");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Szövegfájl", "*.txt"),
                new FileChooser.ExtensionFilter("Word dokumentum", "*.docx")
        );
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            writeToFile(file);
            currentFile = file;
        }
    }

    // Közös mentő metódus: docx módban a HTMLEditor tartalmát menti
    // vissza valódi .docx-be (formázással), egyébként a TextArea sima szövegét.
    private void writeToFile(File file) {
        try {
            if (isDocx(file) || docxMode) {
                htmlToDocx(htmlEditor.getHtmlText(), file);
            } else {
                Files.writeString(file.toPath(), editText.getText());
            }
        } catch (IOException e) {
            showError("Nem sikerült menteni a fájlt: " + e.getMessage());
        }
    }

    private boolean isDocx(File file) {
        return file.getName().toLowerCase().endsWith(".docx");
    }

    // ==========================================================
    //  .docx  ->  HTML   (megnyitáshoz, HTMLEditor-be)
    // ==========================================================
    // Végigmegyünk a docx bekezdésein és futásain (run), és minden
    // run formázását (félkövér, dőlt, aláhúzott) megfelelő HTML
    // tag-ekbe csomagoljuk, hogy a HTMLEditor helyesen jelenítse meg.
    private String docxToHtml(File file) throws IOException {
        StringBuilder html = new StringBuilder("<html><body>");

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                html.append("<p>");
                for (XWPFRun run : paragraph.getRuns()) {
                    String text = run.getText(0);
                    if (text == null) continue;

                    text = escapeHtml(text);

                    if (run.isBold()) text = "<b>" + text + "</b>";
                    if (run.isItalic()) text = "<i>" + text + "</i>";
                    if (run.getUnderline() != UnderlinePatterns.NONE) text = "<u>" + text + "</u>";

                    html.append(text);
                }
                html.append("</p>");
            }
        }

        html.append("</body></html>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ==========================================================
    //  HTML  ->  .docx   (mentéshez, HTMLEditor tartalmából)
    // ==========================================================
    // A jsoup-pal feldolgozzuk a HTMLEditor kimenetét: minden <p>
    // egy docx bekezdés lesz, a <b>/<strong>, <i>/<em>, <u> tag-ek
    // pedig a run formázását állítják be.
    private void htmlToDocx(String htmlContent, File file) throws IOException {
        Document jsoupDoc = Jsoup.parse(htmlContent);

        try (XWPFDocument docxDocument = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(file)) {

            Elements paragraphs = jsoupDoc.body().select("> p, > div");
            if (paragraphs.isEmpty()) {
                // Ha nincsenek <p> tag-ek, az egész body-t egy bekezdésként kezeljük.
                XWPFParagraph paragraph = docxDocument.createParagraph();
                appendNodeToParagraph(jsoupDoc.body(), paragraph, false, false, false);
            } else {
                for (Element p : paragraphs) {
                    XWPFParagraph paragraph = docxDocument.createParagraph();
                    appendNodeToParagraph(p, paragraph, false, false, false);
                }
            }

            docxDocument.write(fos);
        }
    }

    // Rekurzívan bejárja a HTML csomópontokat, és a szöveges részeket
    // a megfelelő formázással (bold/italic/underline) írja bele a docx bekezdésbe.
    private void appendNodeToParagraph(Node node, XWPFParagraph paragraph,
                                        boolean bold, boolean italic, boolean underline) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.text();
                if (!text.isEmpty()) {
                    XWPFRun run = paragraph.createRun();
                    run.setText(text);
                    run.setBold(bold);
                    run.setItalic(italic);
                    if (underline) run.setUnderline(UnderlinePatterns.SINGLE);
                }
            } else if (child instanceof Element element) {
                String tag = element.tagName().toLowerCase();
                boolean nextBold = bold || tag.equals("b") || tag.equals("strong");
                boolean nextItalic = italic || tag.equals("i") || tag.equals("em");
                boolean nextUnderline = underline || tag.equals("u");

                if (tag.equals("br")) {
                    paragraph.createRun().addBreak();
                } else {
                    appendNodeToParagraph(element, paragraph, nextBold, nextItalic, nextUnderline);
                }
            }
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
                    htmlEditor.setHtmlText("");
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
        alert.setContentText("CodeFlow — szövegszerkesztő JavaFX-ben.\n.docx = formázott (HTMLEditor)\n.txt/kód = sima szöveg (TextArea)");
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
