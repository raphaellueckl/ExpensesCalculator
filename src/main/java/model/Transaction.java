package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Transaction {

	private StringProperty title = new SimpleStringProperty();
	private StringProperty category = new SimpleStringProperty();
	private StringProperty period = new SimpleStringProperty();
	private StringProperty value = new SimpleStringProperty();

	public Transaction() {}	//Default constructor is needed for JSON-handling

	public Transaction(String title, String value, String period, String category) {
		this.title = new SimpleStringProperty(title);
		this.value = new SimpleStringProperty(value);
		this.period = new SimpleStringProperty(period);
		this.category = new SimpleStringProperty(category);
	}


	//Property methods are needed for FXML.
	public StringProperty titleProperty() { return this.title; }

	public StringProperty categoryProperty() { return this.category; }

	public StringProperty periodProperty() { return this.period; }

	public StringProperty valueProperty() { return this.value; }

	//Getters and Setters are needed for JSON-handling.
	public void setTitle(String title) { this.title.set(title); }

	public void setCategory(String category) { this.category.set(category); }

	public void setPeriod(String period) { this.period.set(period); }

	public void setValue(String value) { this.value.set(value); }

	public String getTitle() { return this.title.get(); }

	public String getCategory() { return this.category.get(); }

	public String getPeriod() { return this.period.get(); }

	public String getValue() { return this.value.get(); }

}
