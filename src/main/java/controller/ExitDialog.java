package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ExitDialog extends Stage {

	private final Calculator expCalc;
	private double xOffset = 0;
	private double yOffset = 0;

	@FXML
	private Button saveAndCloseButton;
	@FXML
	private Button discardAndCloseButton;

	public ExitDialog(Calculator expCalc) {
		this.expCalc = expCalc;
		final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ExitDialogView.fxml"));
		fxmlLoader.setController(this);
		VBox vBox = null;
		try {
			 vBox = fxmlLoader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

		vBox.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});

		vBox.setOnMouseDragged(event -> {
			this.setX(event.getScreenX() - xOffset);
			this.setY(event.getScreenY() - yOffset);
		});
		
		final Scene exitDialogScene = new Scene(vBox, 440, 200);
		exitDialogScene.getStylesheets().add(Calculator.class.getResource("/stylesheet.css").toExternalForm());
		exitDialogScene.getStylesheets().add(Calculator.class.getResource("/stylesheet_exitdialog.css").toExternalForm());
		this.initModality(Modality.APPLICATION_MODAL);
		this.getIcons().add(new Image(getClass().getResourceAsStream("/nubage_favicon.png")));
		this.setScene(exitDialogScene);
	}

	@FXML
	private void onSaveAndClose() {
		this.expCalc.onSaveButton();
		this.close();
	}

	@FXML
	private void onDiscardAndClose() {
		this.close();
	}
}
