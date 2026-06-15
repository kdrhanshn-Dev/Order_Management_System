package org.example.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {

    private static final String URL = System.getenv().getOrDefault(
            "DB_URL", "jdbc:mysql://127.0.0.1:3306/OrderManagementDB"
    );

    private static String readUser() {
        // DOĞRU: önce System Property (VM options -DDB_USER=...), sonra ENV (DB_USER)
        String u = System.getProperty("DB_USER", System.getenv("DB_USER"));
        return (u == null || u.isBlank()) ? "root" : u;
    }

    private static String readPassword() {
        // DOĞRU: önce System Property, sonra ENV
        String p = System.getProperty("DB_PASSWORD", System.getenv("DB_PASSWORD"));
        return (p == null) ? "" : p;
    }

    private static final String USER = readUser();
    private static final String PASSWORD = readPassword();

    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver bulunamadı (mysql-connector-j ekli mi?).", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (PASSWORD.isBlank()) {
            throw new SQLException(
                    "DB_PASSWORD boş. Run/Debug Config > Environment variables altına DB_USER ve DB_PASSWORD ekleyin " +
                            "(örnek: app / app_pass)."
            );
        }

        Properties props = new Properties();
        props.put("user", USER);
        props.put("password", PASSWORD);     // null değil!
        props.put("useUnicode", "true");
        props.put("characterEncoding", "UTF-8");
        props.put("useSSL", "false");
        props.put("allowPublicKeyRetrieval", "true");
        props.put("serverTimezone", "UTC");

        System.out.println("[DB] url=" + URL + " user=" + USER + " hasPwd=" + !PASSWORD.isBlank());
        return DriverManager.getConnection(URL, props);
    }
}
