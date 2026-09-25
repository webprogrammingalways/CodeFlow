module com.example.myapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.myapp to javafx.fxml;
    exports com.example.myapp;
}