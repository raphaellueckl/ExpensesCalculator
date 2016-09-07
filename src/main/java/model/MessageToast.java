package model;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MessageToast extends VBox {

    @FXML private VBox messageToastBox;
    @FXML private Label msgLabel;

    private Thread thread = null;

    public MessageToast() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/MessageToastView.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        msgLabel.managedProperty().bind(this.visibleProperty());
        msgLabel.opacityProperty().bind(this.opacityProperty());
        this.getStylesheets().add("/message_toast_stylesheet.css");
    }

    public void showErrorMessage(String message) {
        this.setVisible(true);
        this.msgLabel.setText(message);
        this.getStyleClass().clear();
        this.getStyleClass().add("success");
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                for (int i=0; i<10; ++i) {
                    Thread.sleep(100);
                    Platform.runLater(() -> {
                        this.setOpacity(this.getOpacity()-0.1);
                    });
                }
                MessageToast.this.setVisible(false);
                MessageToast.this.setOpacity(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void showSuccessMessage(String message) {
        messageToastBox.setVisible(true);
        this.msgLabel.setText(message);
        this.getStyleClass().clear();
        this.getStyleClass().add("success");
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> messageToastBox.setVisible(false));
        }).start();
    }

}
