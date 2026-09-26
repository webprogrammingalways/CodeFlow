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
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private TreeView<File> fileList;

    @FXML
    private StackPane editorStack;

    @FXML
    private Label watermarkLabel;

    @FXML
    private TextArea editText;

    @FXML
    private HTMLEditor htmlEditor;

    // A CodeArea-t kódból hozzuk létre és tesszük a StackPane-be,
    // mert FXML-ből macerásabb lenne a VirtualizedScrollPane csomagolás miatt.
    private CodeArea codeArea;
    private VirtualizedScrollPane<CodeArea> codeScrollPane;

    private File currentFile;
    private File currentFolder;

    // Melyik szerkesztő van most használatban.
    private enum EditorMode { NONE, TEXT, HTML, CODE }
    private EditorMode currentMode = EditorMode.NONE;

    // Azok a kiterjesztések, amiket "kódnak" tekintünk, és szintaxis-kiemelést kapnak.
    private static final java.util.Set<String> CODE_EXTENSIONS = java.util.Set.of(
            "java", "js", "ts", "py", "c", "cpp", "cs", "json", "xml", "html", "css", "sql"
    );

    // ==========================================================
    //  INICIALIZÁLÁS
    // ==========================================================
    @FXML
    private void initialize() {
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStylesheets().add(
                getClass().getResource("code-highlighting.css").toExternalForm()
        );

        // Élő szintaxis-kiemelés: minden szövegváltozáskor újraszínezzük.
        codeArea.textProperty().addListener((obs, oldText, newText) ->
                codeArea.setStyleSpans(0, computeHighlighting(newText))
        );

        codeScrollPane = new VirtualizedScrollPane<>(codeArea);
        codeScrollPane.setVisible(false);
        codeScrollPane.setManaged(false);

        editorStack.getChildren().add(codeScrollPane);

        // Induláskor semmilyen fájl nincs megnyitva: minden szerkesztőt
        // elrejtünk, hogy a vízjel valóban látszódjon, ne takarja el semmi.
        switchMode(EditorMode.NONE);

        // A fájllista cellái: ikon + fájlnév. Mappára kattintva csak
        // ki/be nyílik (a beépített TreeView viselkedés), fájlra kattintva
        // pedig betöltjük a tartalmát.
        fileList.setCellFactory(tv -> new TreeCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(file.getName());
                    setGraphic(new Label(iconFor(file)));
                }
            }
        });

        // Csak akkor töltünk be tartalmat, ha a kiválasztott elem valódi fájl
        // (mappára kattintva nem próbál semmit "megnyitni", csak lenyílik/becsukódik).
        fileList.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                File file = newItem.getValue();
                if (file != null && file.isFile()) {
                    loadFile(file);
                }
            }
        });
    }

    // Kis emoji-ikon a fájl típusa alapján.
    private String iconFor(File file) {
        if (file.isDirectory()) {
            return "📁";
        }
        String ext = getExtension(file);
        return switch (ext) {
            case "java" -> "☕";
            case "js", "ts" -> "🟨";
            case "py" -> "🐍";
            case "json" -> "📦";
            case "html" -> "🌐";
            case "css" -> "🎨";
            case "docx" -> "📄";
            case "md" -> "📝";
            case "xml" -> "🧾";
            default -> "📃";
        };
    }

    // --- FILE MENÜ ---

    @FXML
    private void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Fájl megnyitása");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Minden támogatott", "*.txt", "*.docx", "*.java", "*.md", "*.json", "*.xml", "*.js", "*.py", "*.css", "*.html"),
                new FileChooser.ExtensionFilter("Szövegfájl", "*.txt", "*.md"),
                new FileChooser.ExtensionFilter("Kódfájl", "*.java", "*.js", "*.ts", "*.py", "*.c", "*.cpp", "*.cs", "*.json", "*.xml", "*.html", "*.css", "*.sql"),
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
            TreeItem<File> root = buildTree(folder);
            root.setExpanded(true);
            fileList.setRoot(root);
            fileList.setShowRoot(false);
        }
    }

    // Rekurzívan felépíti a mappa fastruktúráját: minden almappa is
    // egy TreeItem lesz a saját gyerekeivel, amit a TreeView magától
    // tud lenyitni/becsukni a nyilacska ikonnal.
    private TreeItem<File> buildTree(File file) {
        TreeItem<File> item = new TreeItem<>(file);

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                // mappák előre, utána ábécésorrend
                Arrays.sort(children, (a, b) -> {
                    if (a.isDirectory() != b.isDirectory()) {
                        return a.isDirectory() ? -1 : 1;
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                });

                for (File child : children) {
                    item.getChildren().add(buildTree(child));
                }
            }
        }

        return item;
    }

    // Közös betöltő metódus: a kiterjesztés alapján dönti el,
    // hogy .docx (HTMLEditor), kód (CodeArea) vagy sima szöveg (TextArea) legyen-e.
    private void loadFile(File file) {
        try {
            String extension = getExtension(file);

            if (extension.equals("docx")) {
                String html = docxToHtml(file);
                htmlEditor.setHtmlText(html);
                switchMode(EditorMode.HTML);
            } else if (CODE_EXTENSIONS.contains(extension)) {
                String content = Files.readString(file.toPath());
                codeArea.replaceText(content);
                switchMode(EditorMode.CODE);
            } else {
                editText.setText(Files.readString(file.toPath()));
                switchMode(EditorMode.TEXT);
            }
            currentFile = file;
        } catch (IOException e) {
            showError("Nem sikerült megnyitni a fájlt: " + e.getMessage());
        }
    }

    private String getExtension(File file) {
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1);
    }

    // A három szerkesztő közül csak az aktuális látszik, a többi el van rejtve.
    // A CodeFlow vízjel csak akkor látszik, amíg nincs megnyitva semmi.
    private void switchMode(EditorMode mode) {
        currentMode = mode;

        watermarkLabel.setVisible(mode == EditorMode.NONE);

        editText.setVisible(mode == EditorMode.TEXT);
        editText.setManaged(mode == EditorMode.TEXT);

        htmlEditor.setVisible(mode == EditorMode.HTML);
        htmlEditor.setManaged(mode == EditorMode.HTML);

        codeScrollPane.setVisible(mode == EditorMode.CODE);
        codeScrollPane.setManaged(mode == EditorMode.CODE);
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
                new FileChooser.ExtensionFilter("Kódfájl", "*.java", "*.js", "*.py", "*.json", "*.xml", "*.html", "*.css"),
                new FileChooser.ExtensionFilter("Word dokumentum", "*.docx")
        );
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            writeToFile(file);
            currentFile = file;
        }
    }

    // Közös mentő metódus: az aktuális szerkesztő módja alapján dönti el, mit hogyan mentsen.
    private void writeToFile(File file) {
        try {
            switch (currentMode) {
                case HTML -> htmlToDocx(htmlEditor.getHtmlText(), file);
                case CODE -> Files.writeString(file.toPath(), codeArea.getText());
                default -> Files.writeString(file.toPath(), editText.getText());
            }
        } catch (IOException e) {
            showError("Nem sikerült menteni a fájlt: " + e.getMessage());
        }
    }

    // ==========================================================
    //  SZINTAXIS-KIEMELÉS (CodeArea-hoz)
    // ==========================================================

    private static final String[] KEYWORDS = {
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "return", "break", "continue", "class", "interface", "extends",
            "implements", "new", "try", "catch", "finally", "throw", "throws",
            "public", "private", "protected", "static", "final", "void",
            "import", "package", "this", "super", "null", "true", "false",
            "def", "function", "let", "const", "var", "elif", "and", "or", "not"
    };

    private static final String[] TYPES = {
            "int", "float", "double", "long", "short", "byte", "char",
            "boolean", "String", "string", "bool", "number", "object", "Object", "List", "Map"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String TYPE_PATTERN = "\\b(" + String.join("|", TYPES) + ")\\b";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?\\b";
    private static final String COMMENT_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/|#[^\n]*";
    private static final String OPERATOR_PATTERN = "[+\\-*/%=<>!&|^~]+";
    private static final String FUNCTION_PATTERN = "\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\()";
    private static final String PAREN_PATTERN = "[(){}\\[\\];,.]";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<TYPE>" + TYPE_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<FUNCTION>" + FUNCTION_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<OPERATOR>" + OPERATOR_PATTERN + ")"
    );

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastEnd = 0;
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("COMMENT") != null ? "comment" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("TYPE") != null ? "type" :
                    matcher.group("NUMBER") != null ? "number" :
                    matcher.group("FUNCTION") != null ? "function" :
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("OPERATOR") != null ? "operator" :
                    null;

            builder.add(Collections.emptyList(), matcher.start() - lastEnd);
            builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        builder.add(Collections.emptyList(), text.length() - lastEnd);
        return builder.create();
    }

    // ==========================================================
    //  .docx  ->  HTML   (megnyitáshoz, HTMLEditor-be)
    // ==========================================================
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
    private void htmlToDocx(String htmlContent, File file) throws IOException {
        Document jsoupDoc = Jsoup.parse(htmlContent);

        try (XWPFDocument docxDocument = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(file)) {

            Elements paragraphs = jsoupDoc.body().select("> p, > div");
            if (paragraphs.isEmpty()) {
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
        TreeItem<File> selected = fileList.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null) {
            File file = selected.getValue();
            if (file.delete()) {
                TreeItem<File> parent = selected.getParent();
                if (parent != null) {
                    parent.getChildren().remove(selected);
                }
                if (file.equals(currentFile)) {
                    currentFile = null;
                    editText.clear();
                    htmlEditor.setHtmlText("");
                    codeArea.clear();
                    switchMode(EditorMode.NONE);
                }
            } else {
                showError("Nem sikerült törölni a fájlt (lehet, hogy nem üres mappa).");
            }
        }
    }

    // --- HELP MENÜ ---

    @FXML
    private void aboutItem(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Névjegy");
        alert.setHeaderText(null);
        alert.setContentText("CodeFlow — .docx = formázott szerkesztő, kódfájl = szintaxis-kiemelés, egyéb = sima szöveg.");
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
