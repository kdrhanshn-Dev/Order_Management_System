package org.example.Repository;

import org.example.Model.Customer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository {

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Customers")) {

            while (rs.next()) {
                customers.add(new Customer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("budget"),
                        rs.getString("type")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customers;
    }

    public void updateCustomer(Customer customer) {
        String sql = "UPDATE Customers SET budget=?, totalSpent=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, customer.getBudget());
            ps.setDouble(2, customer.getTotalSpent());
            ps.setInt(3, customer.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void addCustomer(Customer customer) {
        String sql = "INSERT INTO Customers (name, budget, type, totalSpent) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getName());
            ps.setDouble(2, customer.getBudget());
            ps.setString(3, customer.getType());
            ps.setDouble(4, customer.getTotalSpent());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean debitIfEnough(java.sql.Connection conn, int customerId, double total) throws Exception {
        String sql = "UPDATE Customers SET budget = budget - ?, totalSpent = totalSpent + ? " +
                "WHERE id = ? AND budget >= ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, total);
            ps.setDouble(2, total);
            ps.setInt(3, customerId);
            ps.setDouble(4, total);
            return ps.executeUpdate() == 1;
        }
    }

}

