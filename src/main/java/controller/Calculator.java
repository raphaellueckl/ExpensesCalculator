package controller;

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
import javafx.util.Callback;
import model.Transaction;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * TODO:
 * - Exit Dialog: Don't close, if user presses [x]
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
 *
 * V2.1.0:
 * - New Feature: Toast-Messages containing information (error and success messages)
 * - Code improvements & bug fixes.
 *
 * V2.2.0
 * - New Feature: You can now add incomes, not just expenses.
 * - Updated logo
 * - Optimized the window-drag-movement for Windows 10.
 * - Code improvements & bug fixes.
 */
public class Calculator extends Application {

	private FileService fileService;
	private File path = getInitialDocumentPath();
	private GridPane mainView;
	private ObservableList<Transaction> transactionList;
	private ResourceBundle currentResourceBundle;

	private boolean hasPendingChanges = false;

	private double xOffset = 0;
	private double yOffset = 0;

	@FXML private TableView<Transaction> expensesTableView;
	@FXML private ImageView logoImageView;
	@FXML private TextField expenseTitle;
	@FXML private ComboBox<String> expensePeriod;
	@FXML private ComboBox<String> expenseCategory;
	@FXML private TextField expenseValue;
	@FXML private TextField addCategoryTextField;
	@FXML private Label expensesPerYearText;
	@FXML private Label expensesPerMonthText;
	@FXML private Label expensesPerWeekText;
	@FXML private Label expensesPerDayText;
	@FXML private Label expensesPerHourText;
	@FXML private MessageToast errorMessage;
	@FXML private CheckBox isIncome;

	/**
	 * Build the main part of the GUI.
	 */
	@Override
	public void start(Stage stage) {
		currentResourceBundle = ResourceBundle.getBundle("bundles/language_en", new Locale("en", "EN"));
		final FXMLLoader loader = new FXMLLoader();
		loader.setController(this);
		loader.setResources(currentResourceBundle);
		loader.setLocation(getClass().getResource("/view/AppView.fxml"));
		try {
			mainView = (GridPane) loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

		fileService = new FileService(errorMessage);

		transactionList = FXCollections.observableArrayList();
		expensePeriod.getSelectionModel().selectFirst();

		setupTableView(transactionList);
		setupCategoryComboBox();
		setupPeriodComboBox();
		buildListeners(stage);

		//Adding the logo...
		logoImageView.setImage(new Image(getClass().getResourceAsStream("/nubage_logo.png")));

		final Scene scene = new Scene(mainView, 1200, 800);
		scene.setOnKeyReleased(new KeyHandler());
		scene.setOnDragOver(new DragOverHandler());
		scene.setOnDragDropped(new FileDragHandler());
        scene.getStylesheets().add(Calculator.class.getResource("/stylesheet.css").toExternalForm());
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

	private void setupTableView(ObservableList<Transaction> transactionList) {
		expensesTableView.sortPolicyProperty().set(cb -> {
			Comparator<Transaction> c = (a, b) -> {
				if (a.getValue().contains("-") ^ b.getValue().contains("-")) {
					return a.getValue().contains("-") ? 1 : -1;
				}
				return 0;
			};
			FXCollections.sort(expensesTableView.getItems(), c);
			return true;
		});

		expensesTableView.setRowFactory(new Callback<TableView<Transaction>, TableRow<Transaction>>() {
			@Override
			public TableRow<Transaction> call(TableView<Transaction> tableView) {
				return new TableRow<Transaction>() {
					@Override
					protected void updateItem(Transaction person, boolean empty){
						super.updateItem(person, empty);
						if (person == null || person.getValue().contains("-")) {
							getStyleClass().remove("income-row");
						} else if ( !person.getValue().contains("-")) {
							getStyleClass().add("income-row");
						} else {
							getStyleClass().remove("income-row");
						}
					}
				};
			}
		});

		expensesTableView.setItems(transactionList);
	}

	@FXML
	public void onSaveButton() {
		if (transactionList.isEmpty()) {
			errorMessage.showErrorMessage("Nothing to save!");
			return;
		}
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

		if (path != null) fileService.writeListToJson(path, transactionList);
	}

	@FXML
	public void onLoadButton() {
		if (path != null && !path.isDirectory()) {
			path = path.getParentFile();
		}
		try {
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Load JSON-File");
			fileChooser.getExtensionFilters().add(new ExtensionFilter("JSON-Document", "*.json"));
			if (path != null && path.isDirectory()) fileChooser.setInitialDirectory(path);
			path = fileChooser.showOpenDialog(new Stage());
			if (path == null) return;

			loadFile(path.toString());

		} catch (Exception e) {
			if (path != null)
				errorMessage.showErrorMessage("Error!");
		}
	}

	@FXML
	public void onEditButton() {
		TableView.TableViewSelectionModel<Transaction> selectionModel = expensesTableView.getSelectionModel();
		ObservableList selectedCells = selectionModel.getSelectedCells();
		TablePosition tablePosition = (TablePosition) selectedCells.get(0);

		int row = tablePosition.getRow();
		expenseTitle.setText(transactionList.get(row).getTitle());
		for (int i = 0; i< expensePeriod.getItems().size(); ++i) {
			if (expensePeriod.getItems().get(i).equals(transactionList.get(row).getPeriod())) {
				expensePeriod.setValue(transactionList.get(row).getPeriod());
				break;
			}
		}

		for (int i = 0; i< expenseCategory.getItems().size(); ++i) {
			if (expenseCategory.getItems().get(i).equals(transactionList.get(row).getCategory())) {
				expenseCategory.setValue(transactionList.get(row).getCategory());
				break;
			}
		}

		expenseValue.setText(transactionList.get(row).getValue());
		transactionList.remove(row);
	}

	@FXML
	public void onDeleteButton() {
		deleteSelectedRow();
		hasPendingChanges = true;
	}

	@FXML
	public void onNewSheetButton() {
		transactionList.clear();
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
	public void setupCategoryComboBox() {
		expenseCategory.getItems().addAll(currentResourceBundle.getString("combobox.none"), currentResourceBundle.getString("combobox.add_a_category"));
        for (int i = 0; i< transactionList.size(); ++i) {
        	if (expenseCategory.getItems().contains(transactionList.get(i).getCategory()) == false) {
        		expenseCategory.getItems().add(transactionList.get(i).getCategory());
        	}
        }
        expenseCategory.getSelectionModel().selectFirst();
        expenseCategory.setOnAction(new CategoryListener());
	}

	/**
	 * Builds the "Period"-cobobox
	 */
	public void setupPeriodComboBox() {
		expensePeriod.getItems().addAll(
			currentResourceBundle.getString("combobox.year"),
			currentResourceBundle.getString("combobox.six_months"),
			currentResourceBundle.getString("combobox.quarter"),
			currentResourceBundle.getString("combobox.month"),
			currentResourceBundle.getString("combobox.week"),
			currentResourceBundle.getString("combobox.day"));
		expensePeriod.getSelectionModel().selectFirst();
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
			stage.setX(event.getScreenX() - xOffset - 9);
			stage.setY(event.getScreenY() - yOffset - 37);
		});

		transactionList.addListener(new ListListener());
	}

	/**
	 * Adds an "Transaction" to the list.
	 */
	public void addExpense() {
		//Checks if the "Category"-combobox already has the String from the "Category-Textfield". If not, it will be added to the combobox.
		if (addCategoryTextField.getText() != null && !addCategoryTextField.getText().isEmpty()) {
			if (!expenseCategory.getItems().contains(addCategoryTextField.getText())) {
				expenseCategory.getItems().add(addCategoryTextField.getText());
			}
		}
		
		//Everything is ok, if the value of the "Value"-Field could be parsed to a double. The "Category"-Combobox is will be set to the newest entry.
		//The filled fields will be resetted.
		double expValue = 0d;
		try {
			expValue = Double.parseDouble(expenseValue.getText());
			if (!isIncome.isSelected()) {
				expValue -= expValue * 2;
			}
		} catch (Exception e) {
			errorMessage.showErrorMessage("No valid value!");
			return;
		}
		
		//Check the fields and create a new "Transaction".
		Transaction exp = null;

		if (!expenseTitle.getText().isEmpty() && !expenseValue.getText().isEmpty()) {
			if (addCategoryTextField.getText() != null && !addCategoryTextField.getText().isEmpty()) {
				exp = new Transaction(expenseTitle.getText(), String.valueOf(expValue), expensePeriod.getValue(), addCategoryTextField.getText());

			} else if (expenseCategory.getValue().equals(currentResourceBundle.getString("combobox.add_a_category"))) {
				expenseCategory.getSelectionModel().selectFirst();
				exp = new Transaction(expenseTitle.getText(), String.valueOf(expValue), expensePeriod.getValue(), expenseCategory.getValue());

			} else {
				exp = new Transaction(expenseTitle.getText(), String.valueOf(expValue), expensePeriod.getValue(), expenseCategory.getValue());

			}
			transactionList.add(exp);
			hasPendingChanges = true;
		} else {
			errorMessage.showErrorMessage("Some fields aren't filled correctly!");
		}

		addCategoryTextField.setText("");
		addCategoryTextField.setVisible(false);

		expenseTitle.setText("");
		expenseCategory.getSelectionModel().selectLast();
		expensePeriod.getSelectionModel().selectFirst();
		expenseValue.setText("");
		expenseTitle.requestFocus();

		expensesTableView.sort();

		hasPendingChanges = true;
	}

	/**
	 * Deletes the selected row in the list.
	 */
	boolean deleteSelectedRow() {
		try {
		    TableView.TableViewSelectionModel<Transaction> selectionModel = expensesTableView.getSelectionModel();
		    ObservableList selectedCells = selectionModel.getSelectedCells();
		    TablePosition tablePosition = (TablePosition) selectedCells.get(0);
		    int row = tablePosition.getRow();
		    transactionList.remove(row);
			return true;
		} catch(Exception e) {}
	    return false;
	}

	/**
	 * Everything will be calculated to a year and from there back to the other values.
	 */
	public void calculateValues() {
		Double sum = new Double(0);
		for (Transaction e : transactionList) {
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
				errorMessage.showErrorMessage("Calculating error!");
			}
		}

		DecimalFormat  hourFormat = new DecimalFormat("#.####");
		DecimalFormat  dayFormat = new DecimalFormat("#.##");
		DecimalFormat  weekFormat = new DecimalFormat("#.##");
		DecimalFormat  monthFormat = new DecimalFormat("#");
		DecimalFormat  yearFormat = new DecimalFormat("#");

		setStyleClassForNegativeOrPositiveValue(Arrays.asList(expensesPerHourText, expensesPerDayText, expensesPerWeekText, expensesPerMonthText, expensesPerYearText), sum);

		expensesPerHourText.setText(hourFormat.format(sum / 8760).toString());
		expensesPerDayText.setText(dayFormat.format(sum / 365).toString());
		expensesPerWeekText.setText(weekFormat.format(sum / 52).toString());
		expensesPerMonthText.setText(monthFormat.format(sum / 12).toString());
		expensesPerYearText.setText(yearFormat.format(sum).toString());
	}

	private void setStyleClassForNegativeOrPositiveValue(List<Label> labels, Double sum) {
		for (Label label : labels) {
			label.getStyleClass().remove("positive-value");
			label.getStyleClass().remove("negative-value");
			if (sum < 0) {
				label.getStyleClass().add("negative-value");
			} else {
				label.getStyleClass().add("positive-value");
			}
		}
	}

	/**
	 * Loads a file by from the path parameter. If the file isn't conform, an errormessage will be displayed.
	 */
	public void loadFile(String path) {
		final List<Transaction> loadedEntities = fileService.loadFile(path.toString());
		transactionList.clear();
		transactionList.addAll(loadedEntities);
		calculateValues();
		setupCategoryComboBox();
		expensesTableView.sort();
		hasPendingChanges = false;
	}

	private File getInitialDocumentPath() {
		return new File(Calculator.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
	}

	/**
	 * The Category-Label and TextField only get visible if the CategoryComboBox is set to "Add a category".
	 */
	public class CategoryListener implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent arg0) {
			if (expenseCategory.getValue().equals(currentResourceBundle.getString("combobox.add_a_category"))) {
		        addCategoryTextField.setVisible(true);
			} else {
				addCategoryTextField.setText("");
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
	public class ListListener implements ListChangeListener<Transaction> {

		@Override
		public void onChanged(Change<? extends Transaction> arg0) {
			calculateValues();
			errorMessage.clear();
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

				hasPendingChanges = false;
			}
			event.setDropCompleted(success);
            event.consume();
        }
	}

}