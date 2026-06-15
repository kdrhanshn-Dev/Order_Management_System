package org.example.Repository;

import org.example.Model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class OrderRepository {

    /** Var olan bağlantı içinde (aynı transaction’da) sipariş oluşturur. */
    public void createOrder(Connection conn, Order order) throws Exception {
        final String sql =
                "INSERT INTO Orders (customerId, productId, quantity, totalPrice, status, orderDate) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getCustomerId());
            ps.setInt(2, order.getProductId());
            ps.setInt(3, order.getQuantity());
            ps.setDouble(4, order.getTotalPrice());
            ps.setString(5, order.getStatus()); // "APPROVED/REJECTED/TIMEOUT/..."
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    // İstersen Order objesine id set edebilirsin:
                    // order.setId(keys.getInt(1));
                }
            }
        }
    }

    /** Bağımsız bağlantı ile tek başına sipariş oluşturur (ör: TIMEOUT kaydı). */
    public void createOrderStandalone(Order order) {
        final String sql =
                "INSERT INTO Orders (customerId, productId, quantity, totalPrice, status, orderDate) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, order.getCustomerId());
            ps.setInt(2, order.getProductId());
            ps.setInt(3, order.getQuantity());
            ps.setDouble(4, order.getTotalPrice());
            ps.setString(5, order.getStatus());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** (Opsiyonel) Eski çağrılar için geriye dönük uyumluluk. */
    public void createOrder(Order order) {
        createOrderStandalone(order);
    }
}
