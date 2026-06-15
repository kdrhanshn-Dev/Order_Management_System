package org.example.Model;

import javafx.beans.property.*;

public class Product {
    private final IntegerProperty id;
    private final StringProperty name;
    private final IntegerProperty stock;
    private final DoubleProperty price;

    public Product(int id, String name, int stock, double price) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.stock = new SimpleIntegerProperty(stock);
        this.price = new SimpleDoubleProperty(price);
    }

    // Getters
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public int getStock() { return stock.get(); }
    public double getPrice() { return price.get(); }

    // Setters
    public void setStock(int value) { this.stock.set(value); }
    public void setPrice(double value) { this.price.set(value); }

    // Properties
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public IntegerProperty stockProperty() { return stock; }
    public DoubleProperty priceProperty() { return price; }

    @Override
    public String toString() {
        return name.get() + " - " + stock.get() + " units";
    }
}

