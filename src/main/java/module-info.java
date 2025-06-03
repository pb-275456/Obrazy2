module com.example.obrazy {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires java.desktop;
    requires javafx.swing;
    requires java.logging;


    opens com.example.obrazy to javafx.fxml;
    exports com.example.obrazy;
}