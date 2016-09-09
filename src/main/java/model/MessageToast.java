package model;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class MessageToast extends VBox {

    @FXML private VBox messageToastBox;
    @FXML private Label msgLabel;

    private Thread thread = null;
    private Timeline timeline;

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

        timeline = new Timeline();
        KeyFrame key = new KeyFrame(Duration.millis(2000),
                new KeyValue(this.opacityProperty(), 0));
        timeline.getKeyFrames().add(key);
        timeline.setDelay(Duration.seconds(5));
        timeline.setOnFinished((ae) -> {
            this.setVisible(false);
            this.setOpacity(1);
        });
    }

    private void disableMessageToast() {
        this.setVisible(false);
        this.setOpacity(1);
    }

    public void showErrorMessage(String message) {
        this.setVisible(true);
        this.msgLabel.setText(message);
        this.getStyleClass().clear();
        this.getStyleClass().add("error");

        clear();
        this.setVisible(true);
        timeline.play();
    }

    public void showSuccessMessage(String message) {
        messageToastBox.setVisible(true);
        this.msgLabel.setText(message);
        this.getStyleClass().clear();
        this.getStyleClass().add("success");

        clear();
        this.setVisible(true);
        timeline.play();
    }

    public void clear() {
        timeline.stop();
        this.setVisible(false);
        this.setOpacity(1);
    }

}
