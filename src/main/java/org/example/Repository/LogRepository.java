package org.example.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class LogRepository {

    // Kolay sürüm (orderId yok)
    public void createLog(String logType, Integer customerId, String details) {
        createLog(logType, customerId, null, details);
    }

    // Tam sürüm
    public void createLog(String logType, Integer customerId, Integer orderId, String details) {
        String sql = "INSERT INTO Logs (logType, customerId, orderId, details) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, logType);

            if (customerId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, customerId);

            if (orderId == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, orderId);

            ps.setString(4, details);

            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


