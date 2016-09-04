package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import model.Expense;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO:
 * - NONE-Eintrag in categorycombobox kann gel�scht werden (nur der default eintrag)
 * 
 * History:
 * V1.0:
 * - Basic implementation: Add/Load/Save/Delete/Edit/Calculate expenses
 * 
 * V1.1:
 * - Errormessages added
 * - FileChoosers customized
 * - Bugs fixed
 * 
 * V1.1.1:
 * - Little changes in details
 * 
 * V1.2.0:
 * - Drag&Drop for Files added
 * - Design update
 * 
 * V1.2.1:
 * - More brightness for the background
 * - New "Calculated-Values" color
 * - Fixed the combobox. It now shows all entries, when you load an existing file
 * - Fixed the edit-functionality, which was a bit irrating
 * - Fixed the possibility of creating a "someFileName.xml.xml" file
 * 
 * V1.2.2:
 * - Fixed the bug with German umlauts. Now it writes a correct UTF-8 XML file
 * - Delete-key now deletes the selected line (same function as the delete button)
 * 
 * V1.3.0
 * - Added an exit-dialog
 * - Modified the decimal digit management. You won't see too many decimal fractions anymore
 * - Added a "taskbar logo" (something like a favicon, webdesigners will know :-) )
 * - Adjusted the bright green color
 * - Optimized background-images
 * - Code improvements
 *
 * V2.0.0
 * - Added resizing features
 * - Partly new Design
 * - New output filetype (JSON): Uses fewer space, more conventient to handle
 * - Better stability + usability
 * - Applied newer code knowledge
 * - Added decorated window
 * - Project is now open source! :)
 * - Update to Java 8
 */

public class ExpCalc extends Application {

	private static final String EMPTY_STRING = "";

	private File path = getInitialDocumentPath();
	private GridPane mainView;
	private ObservableList<Expense> expenseList;

	private boolean hasPendingChanges = false;

	private double xOffset = 0;
	private double yOffset = 0;

	@FXML
	private TableView<Expense> expensesTableView;
	@FXML
	private ImageView logoImageView;
	@FXML
	private TextField expenseTitle;
	@FXML
	private ComboBox<String> expensePeriod;
	@FXML
	private ComboBox<String> expenseCategory;
	@FXML
	private TextField expenseValue;
	@FXML
	private TextField addCategoryTextField;
	@FXML
	private Label expensesPerYearText;
	@FXML
	private Label expensesPerMonthText;
	@FXML
	private Label expensesPerWeekText;
	@FXML
	private Label errorMessage;
	@FXML
	private Label expensesPerDayText;
	@FXML
	private Label expensesPerHourText;

	/**
	 * Build the main part of the GUI.
	 */
	@Override
	public void start(Stage stage) {
		final FXMLLoader loader = new FXMLLoader();
		loader.setController(this);
		loader.setLocation(getClass().getResource("/view/AppView.fxml"));
		try {
			mainView = (GridPane) loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

		expenseList = FXCollections.observableArrayList();

		expensePeriod.getSelectionModel().selectFirst();

		expensesTableView.setItems(expenseList);

		setUpCategoryComboBox();
		buildListeners(stage);

		//Adding the logo...
		logoImageView.setImage(new Image(getClass().getResourceAsStream("/nubage_logo.png")));

		Scene scene = new Scene(mainView, 1200, 800);
		scene.setOnKeyReleased(new KeyHandler());
		scene.setOnDragOver(new DragOverHandler());
		scene.setOnDragDropped(new FileDragHandler());
        scene.getStylesheets().add(ExpCalc.class.getResource("/stylesheet.css").toExternalForm());
		expenseTitle.requestFocus();

		stage.setTitle("Nubage - Expenses Calculator");
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/nubage_favicon.png")));
		stage.setScene(scene);
		stage.setOnCloseRequest(event -> {
			if (hasPendingChanges) {
				ExitDialog exitDialog = new ExitDialog(this);
				exitDialog.showAndWait();
			}
		});
		stage.show();
	}

	@FXML
	public void onSaveButton() {
		if (path != null && !path.isFile() && !path.toString().endsWith(".json")) {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save JSON-File");
			if (path.isDirectory()) fileChooser.setInitialDirectory(path);
			fileChooser.getExtensionFilters().add(new ExtensionFilter("JSOM-Document", "*.json"));

			path = fileChooser.showSaveDialog(new Stage());
			if (path == null) return;
		}
		if (path.toString().endsWith(".json") == false) {
			path = new File(path.toString() + ".json");
		}

		try (
			 OutputStreamWriter outputFile = new OutputStreamWriter(new FileOutputStream(path), Charset.forName("UTF8"));){
			final ObjectMapper mapper = new ObjectMapper();
			final String jsonToSave = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(expenseList);
			outputFile.write(jsonToSave);
			hasPendingChanges = false;
		} catch (Exception e) {
			e.printStackTrace();
			if (path != null) {
				errorMessage.setText("Random\nError!");
			}
		}
	}

	@FXML
	public void onLoadButton() {
		if (path != null && !path.isDirectory()) {
			path = path.getParentFile();
		}
		try {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Load JSON-File");
			fileChooser.getExtensionFilters().add(new ExtensionFilter("JSON-Document", "*.json"));
			if (path != null && path.isDirectory()) fileChooser.setInitialDirectory(path);
			path = fileChooser.showOpenDialog(new Stage());
			if (path == null) return;

			loadFile(path.toString());
		} catch (Exception e) {
			if (path != null)
				errorMessage.setText("Error!");
		}
	}

	@FXML
	public void onEditButton() {
		TableView.TableViewSelectionModel<Expense> selectionModel = expensesTableView.getSelectionModel();
		ObservableList selectedCells = selectionModel.getSelectedCells();
		TablePosition tablePosition = (TablePosition) selectedCells.get(0);

		int row = tablePosition.getRow();
		expenseTitle.setText(expenseList.get(row).getTitle());
		for (int i = 0; i< expensePeriod.getItems().size(); ++i) {
			if (expensePeriod.getItems().get(i).equals(expenseList.get(row).getPeriod())) {
				expensePeriod.setValue(expenseList.get(row).getPeriod());
				break;
			}
		}

		for (int i = 0; i< expenseCategory.getItems().size(); ++i) {
			if (expenseCategory.getItems().get(i).equals(expenseList.get(row).getCategory())) {
				expenseCategory.setValue(expenseList.get(row).getCategory());
				break;
			}
		}

		expenseValue.setText(expenseList.get(row).getValue());
		expenseList.remove(row);
	}

	@FXML
	public void onDeleteButton() {
		deleteSelectedRow();
		hasPendingChanges = true;
	}

	@FXML
	public void onNewSheetButton() {
		expenseList.clear();
		path = getInitialDocumentPath();
		hasPendingChanges = false;
	}

	@FXML
	public void onAddExpense() {
		addExpense();
	}

    /**
     * Starts the application.
     */
	public static void main(String[]args) {
		launch(args);
	}

	/**
	 * Builds the "Category"-cobobox
	 */
	public void setUpCategoryComboBox() {
        for (int i = 0; i< expenseList.size(); ++i) {
        	if (expenseCategory.getItems().contains(expenseList.get(i).getCategory()) == false) {
        		expenseCategory.getItems().add(expenseList.get(i).getCategory());
        	}
        }
        expenseCategory.getSelectionModel().selectFirst();
        expenseCategory.setOnAction(new CategoryListener());
	}

	/**
	 * Builds the Eventlisteners.
	 */
	public void buildListeners(Stage stage) {
		mainView.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});

		mainView.setOnMouseDragged(event -> {
			stage.setX(event.getScreenX() - xOffset);
			stage.setY(event.getScreenY() - yOffset);
		});

		expenseList.addListener(new ListListener());
	}

	/**
	 * Adds an "Expense" to the list.
	 */
	public void addExpense() {
		//Checks if the "Caregory"-combobox already has the String from the "Category-Textfield". If not, it will be added to the combobox.
		if (addCategoryTextField.getText() != null && !addCategoryTextField.getText().equals(EMPTY_STRING)) {
			boolean contained = false;

			for (int i = 0; i< expenseCategory.getItems().size(); ++i) {
				if (expenseCategory.getItems().get(i).equals(addCategoryTextField.getText())) {
					contained = true;
					break;
				}
			}
			if (!contained) {
				expenseCategory.getItems().addAll(addCategoryTextField.getText());
			}
		}

		//Check the fields and create a new "Expense".

		Expense exp = null;

		if (!expenseTitle.getText().equals(EMPTY_STRING) && !expenseValue.getText().equals(EMPTY_STRING)) {
			if (!addCategoryTextField.getText().equals(EMPTY_STRING)) {
				exp = new Expense(expenseTitle.getText(), expenseValue.getText(), expensePeriod.getValue(), addCategoryTextField.getText());

			} else if (expenseCategory.getValue().equals("[Add a category...]")) {
				expenseCategory.getSelectionModel().selectFirst();
				exp = new Expense(expenseTitle.getText(), expenseValue.getText(), expensePeriod.getValue(), expenseCategory.getValue());

			} else {
				exp = new Expense(expenseTitle.getText(), expenseValue.getText(), expensePeriod.getValue(), expenseCategory.getValue());

			}
			hasPendingChanges = true;
		} else {
			errorMessage.setText("Some fields\naren't filled\ncorrectly!");
		}


		//Everything is ok, if the value of the "Value"-Field could be parsed to a double. The "Category"-Combobox is will be set to the newest entry.
		//The filled fields will be resetted.
		try {
			Double.parseDouble(expenseValue.getText());
			if (exp != null) {
				expenseList.add(exp);
			} else {
				errorMessage.setText("Failure!");
			}
		} catch (Exception e) {
			errorMessage.setText("No valid\nvalue!");
		}

		addCategoryTextField.setText(EMPTY_STRING);
		addCategoryTextField.setVisible(false);

		expenseTitle.setText(EMPTY_STRING);
		expenseCategory.getSelectionModel().selectLast();
		expensePeriod.getSelectionModel().selectFirst();
		expenseValue.setText(EMPTY_STRING);
		expenseTitle.requestFocus();

		hasPendingChanges = true;
	}

	/**
	 * Deletes the selected row in the list.
	 */
	boolean deleteSelectedRow() {
		try {
		    TableView.TableViewSelectionModel<Expense> selectionModel = expensesTableView.getSelectionModel();
		    ObservableList selectedCells = selectionModel.getSelectedCells();
		    TablePosition tablePosition = (TablePosition) selectedCells.get(0);
		    int row = tablePosition.getRow();
		    expenseList.remove(row);
			return true;
		} catch(Exception e) {}
	    return false;
	}

	/**
	 * Everything will be calculated to a year and from there back to the other values.
	 */
	public void calculateValues() {
		Double sum = new Double(0);
		for (Expense e : expenseList) {
			if (e.getPeriod().equals("Year")) {
				sum += Double.parseDouble(e.getValue());
			} else if (e.getPeriod().equals("6 Months")) {
				sum += Double.parseDouble(e.getValue()) * 2;
			} else if (e.getPeriod().equals("Quarter")) {
				sum += Double.parseDouble(e.getValue()) * 4;
			} else if (e.getPeriod().equals("Month")) {
				sum += Double.parseDouble(e.getValue()) * 12;
			} else if (e.getPeriod().equals("Week")) {
				sum += Double.parseDouble(e.getValue()) * 52;
			} else if (e.getPeriod().equals("Day")) {
				sum += Double.parseDouble(e.getValue()) * 365;
			} else {
				errorMessage.setText("Calculating\nerror!");
			}
		}

		DecimalFormat  dfHour = new DecimalFormat("#.####");
		DecimalFormat  dfDay = new DecimalFormat("#.##");
		DecimalFormat  dfWeek = new DecimalFormat("#.##");
		DecimalFormat  dfMonth = new DecimalFormat("#");
		DecimalFormat  dfYear = new DecimalFormat("#");
		expensesPerHourText.setText(dfHour.format(sum / 8760).toString());
		expensesPerDayText.setText(dfDay.format(sum / 365).toString());
		expensesPerWeekText.setText(dfWeek.format(sum / 52).toString());
		expensesPerMonthText.setText(dfMonth.format(sum / 12).toString());
		expensesPerYearText.setText(dfYear.format(sum).toString());
	}

	/**
	 * Loads a file by from the path parameter. If the file isn't conform, an errormessage will be displayed.
	 */
	public void loadFile(String path) {
		try {
			if (path.endsWith(".json")) {
				List<String> loadedJsonAsList = Files.readAllLines(Paths.get(path));

				final String loadedJsonFile = loadedJsonAsList.stream().collect(Collectors.joining());

				ObjectMapper mapper = new ObjectMapper();

				List<Expense> loadedExpenses = Arrays.asList(mapper.readValue(loadedJsonFile, Expense[].class));
				expenseList.clear();
				expenseList.addAll(loadedExpenses);
		        errorMessage.setText(EMPTY_STRING);
		        calculateValues();
		        setUpCategoryComboBox();
				hasPendingChanges = false;
			} else {
				errorMessage.setText("Invalid\nFile!");
			}
		} catch (Exception e) {
			if (path.endsWith(".json"))
				errorMessage.setText("JSON file\ncorrupted!");
			else if (path != null)
				errorMessage.setText("Invalid\nFile!");
		}
	}

	private File getInitialDocumentPath() {
		return new File(ExpCalc.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
	}

	/**
	 * The Category-Label and TextField only get visible if the CategoryComboBox is set to "Add a category".
	 */
	public class CategoryListener implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent arg0) {
			if (expenseCategory.getValue().equals("[Add a category...]")) {
		        addCategoryTextField.setVisible(true);
			} else {
				addCategoryTextField.setText(EMPTY_STRING);
				addCategoryTextField.setVisible(false);
			}
		}
	}

	/**
	 * Press ENTER to add a value instead of pressing the Add-Button.
	 */
	public class KeyHandler implements EventHandler<KeyEvent> {

		@Override
		public void handle(KeyEvent event) {
			if (event.getCode() == KeyCode.ENTER) {
				addExpense();
			} else if (event.getCode() == KeyCode.DELETE) {
				deleteSelectedRow();
			}
		}
	}

	/**
	 * If the list changes its values (loading, adding, deleting, editing,... Expenses), the table refreshes.
	 */
	public class ListListener implements ListChangeListener<Expense> {

		@Override
		public void onChanged(Change<? extends Expense> arg0) {
			calculateValues();
			errorMessage.setText(EMPTY_STRING);
		}
	}
	
	/**
	 * Accept the dragging of a file over the GUI.
	 */
    public class DragOverHandler implements EventHandler<DragEvent> {
        
    	@Override
        public void handle(DragEvent event) {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        }
    }
	
	/**
	 * Load files which were dragged into the window.
	 */
	public class FileDragHandler implements EventHandler<DragEvent> {

		@Override
        public void handle(DragEvent event) {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
            	success = true;
                String filePath = null;
                for (File file:db.getFiles()) {
                    filePath = file.getAbsolutePath();
                }
                loadFile(filePath);
            }
            event.setDropCompleted(success);
            event.consume();
        }
	}

}