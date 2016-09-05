package model;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MessageToast extends VBox {

    @FXML
    private Label msgLabel;

    public MessageToast() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/MessageToastView.fxml"));
        fxmlLoader.setController(this);
        try {
//            VBox vBox = fxmlLoader.load();
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        this.getStylesheets().add("/message_toast_stylesheet.css");
    }

    public void showErrorMessage(String message) {
        this.msgLabel.setText(message);
        this.getStyleClass().clear();
        this.getStyleClass().add("success");
    }

    public void showSuccessMessage(String message) {
        this.msgLabel.setText(message);
        this.getStyleClass().clear();
        this.getStyleClass().add("error");
    }

}
