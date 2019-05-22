import java.sql.*;

public class AuthService {

    private static Connection connection;
    private static Statement stmt;

    public static synchronized void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void disconnect(){
        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized UserLogin checkPassword(String login, String password){
        UserLogin user = null;
        try {
            connect();
            String sql = String.format("SELECT password FROM main\n" + "where login = '%s'", login);
            ResultSet rs = stmt.executeQuery(sql);
            System.out.println("login = " + login + " password = " + password);

            int passwordFromDB = rs.getInt(1);
            System.out.println("Password from DB = " + passwordFromDB);
            System.out.println("Hashcode = " + password.hashCode());
            if (passwordFromDB == password.hashCode()) {
                user = new UserLogin(login);
                disconnect();
                return user;
            }
            disconnect();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    public static synchronized void addUser(String login, String pass) {

        String sql = String.format("INSERT INTO main (login, password)" +
                "VALUES ('%s', '%s')", login, pass.hashCode());
        try {
            connect();
            stmt.execute(sql);
            disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public static synchronized boolean checkHaveThisUser(String login) {
        try {
            connect();
            String sql = String.format("SELECT login FROM main\n" + "where login = '%s'", login);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                disconnect();
                return true;
            }
            disconnect();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


}
