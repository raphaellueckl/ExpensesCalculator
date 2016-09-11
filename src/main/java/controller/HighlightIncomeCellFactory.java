package controller;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import model.Transaction;

public class HighlightIncomeCellFactory implements Callback<TableColumn<Transaction, String>, TableCell<Transaction, String>> {

    @Override
    public TableCell<Transaction, String> call(TableColumn<Transaction, String> param) {
        return new TableCell<Transaction, String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && item.contains("-")) {
                    getTableRow().setStyle("-fx-background-color: rgb(102, 51, 0)");
//                    getTableRow().getStyleClass().add("positive-value");
//                    getStyleClass().add("positive-value");
                }
                setText(item);
            }
        };
    }

}
