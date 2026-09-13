module com.focusflowai {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.net.http;
    requires org.slf4j;

    opens com.focusflowai to javafx.fxml;
    opens com.focusflowai.controller to javafx.fxml;
    opens com.focusflowai.model to com.google.gson;

    exports com.focusflowai;
}
