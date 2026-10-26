import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/traffix";
    private static final String USER = "root";

    // Replace this value with your local MySQL password. Do not publish it.
    private static final String PASSWORD = "YOUR_MYSQL_PASSWORD";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver could not be loaded.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
