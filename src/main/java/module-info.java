module com.example.myapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.kordamp.bootstrapfx.core;

    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.jsoup;

    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires reactfx;

    opens com.example.myapp to javafx.fxml;
    exports com.example.myapp;
}
