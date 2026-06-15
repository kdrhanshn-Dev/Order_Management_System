// src/main/java/org/example/Model/Customer.java
package org.example.Model;

import javafx.beans.property.*;

public class Customer {
    private final IntegerProperty id;
    private final StringProperty  name;
    private final DoubleProperty  budget;
    private final StringProperty  type;

    private final DoubleProperty  totalSpent;
    private final DoubleProperty  priorityScore;
    private final IntegerProperty waitingTime;
    private final StringProperty  orderStatus;

    public Customer(int id, String name, double budget, String type) {
        this.id          = new SimpleIntegerProperty(id);
        this.name        = new SimpleStringProperty(name);
        this.budget      = new SimpleDoubleProperty(budget);
        this.type        = new SimpleStringProperty(type);

        this.totalSpent   = new SimpleDoubleProperty(0);
        this.priorityScore= new SimpleDoubleProperty(0);
        this.waitingTime  = new SimpleIntegerProperty(0);
        this.orderStatus  = new SimpleStringProperty("BEKLEMEDE");
    }

    public void calculatePriorityScore() {
        double base = type.get().equalsIgnoreCase("Premium") ? 15 : 10;
        this.priorityScore.set(base + waitingTime.get() * 0.5);
    }

    public void incrementWaitingTime() {
        this.waitingTime.set(this.waitingTime.get() + 1);
        calculatePriorityScore();
    }

    // --- Getter & Setter ---
    public int getId()             { return id.get(); }
    public String getName()        { return name.get(); }
    public double getBudget()      { return budget.get(); }
    public String getType()        { return type.get(); }

    public void setBudget(double newBudget) { this.budget.set(newBudget); }

    public double getTotalSpent()  { return totalSpent.get(); }
    public void setTotalSpent(double value) { this.totalSpent.set(value); }

    public double getPriorityScore(){ return priorityScore.get(); }

    public int getWaitingTime()    { return waitingTime.get(); }
    public void setWaitingTime(int value) { this.waitingTime.set(value); }

    public String getOrderStatus() { return orderStatus.get(); }
    public void setOrderStatus(String status) { this.orderStatus.set(status); }

    // --- Property Getter'lar ---
    public IntegerProperty idProperty()        { return id; }
    public StringProperty  nameProperty()      { return name; }
    public DoubleProperty  budgetProperty()    { return budget; }
    public StringProperty  typeProperty()      { return type; }
    public DoubleProperty  totalSpentProperty(){ return totalSpent; }
    public DoubleProperty  priorityScoreProperty(){ return priorityScore; }
    public IntegerProperty waitingTimeProperty(){ return waitingTime; }
    public StringProperty  orderStatusProperty(){ return orderStatus; }

    // === DÜZELTİLEN SETTER ===
    public void setType(String value) {
        this.type.set(value);
        // Tür değişince öncelik tabanı değişir → skoru güncelle
        calculatePriorityScore();
    }

    // (Opsiyonel) İleride lazım olursa:
    public void setName(String value) {
        this.name.set(value);
    }
}
