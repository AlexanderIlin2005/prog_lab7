import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:postgresql://se.ifmo.ru:2222/studs", "s381032", "aIERPRFWna2YH9th");
    }
}
