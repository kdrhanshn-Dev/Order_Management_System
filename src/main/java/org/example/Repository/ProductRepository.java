package org.example.Repository;

import org.example.Model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, name, stock, price FROM Products";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("stock"),
                        rs.getDouble("price")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return products;
    }

    public Product getProductById(int id) {
        String sql = "SELECT id, name, stock, price FROM Products WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("stock"),
                            rs.getDouble("price")
                    );
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public void updateProductStock(int productId, int newStock) {
        String sql = "UPDATE Products SET stock=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void addProduct(Product product) {
        String sql = "INSERT INTO Products (name, stock, price) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setInt(2, product.getStock());
            ps.setDouble(3, product.getPrice());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deleteProduct(int productId) {
        String sql = "DELETE FROM Products WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public boolean existsByName(String name) {
        String sql = "SELECT 1 FROM Products WHERE name = ? LIMIT 1";
        try (var conn = DatabaseConnection.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    /** Stok yeterliyse azaltır (aynı transaction). */
    public boolean decStockIfEnough(Connection conn, int productId, int qty) throws Exception {
        String sql = "UPDATE Products SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            return ps.executeUpdate() == 1;
        }
    }

    /** Telafi durumları için stoğu geri artır (aynı transaction). */
    public void incStock(Connection conn, int productId, int qty) throws Exception {
        String sql = "UPDATE Products SET stock = stock + ? WHERE id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }
}
