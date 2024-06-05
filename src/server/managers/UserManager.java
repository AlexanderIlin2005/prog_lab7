package server.managers;

import java.sql.*;

public class UserManager {
    private static final String url = "jdbc:postgresql://localhost:5432/studs";
    static String user = "s381032";
    static String password = "aIERPRFWna2YH9th";

    private Connection connection = null;

    public UserManager(Connection connection){
        this.connection = connection;
    }

    public boolean registerUser(String login, String hashedPassword) {
        try {
            if (authenticateUser(login, hashedPassword)){
                return false;
            }
            String query = "INSERT INTO users (login, hashed_password) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, login);
            statement.setString(2, hashedPassword);
            statement.executeUpdate();
            connection.commit();
            System.out.println("добавлен");
            return true;
        } catch (SQLException e) {
            System.out.println("Error registering user: " + e.getMessage());
            return  false;
        }
    }

    public boolean authenticateUser(String login, String hashedPassword) {
        try {
            String query = "SELECT * FROM Users WHERE login = ? AND hashed_password = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, login);
            statement.setString(2, hashedPassword);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            return false;
        }
    }
}
