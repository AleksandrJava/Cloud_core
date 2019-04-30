import java.sql.*;

public class AuthService {

    private static Connection connection;
    private static Statement stmt;

    public static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect(){
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

    public static boolean checkPassword(String login, String password){
        try {
            String sql = String.format("SELECT password FROM main\n" + "where login = '%s'", login);
            ResultSet rs = stmt.executeQuery(sql);
            System.out.println("login = " + login + " password = " + password);

            int passwordFromDB = rs.getInt(1);
            System.out.println("Password from DB = " + passwordFromDB);
            System.out.println("Hashcode = " + password.hashCode());
            if (passwordFromDB == password.hashCode()) {
                System.out.println("YEEEEEEESSS");
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addUser(String login, String pass) {
        String sql = String.format("INSERT INTO main (login, password)" +
                "VALUES ('%s', '%s')", login, pass.hashCode());
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public static boolean checkHaveThisUser(String login) {
        try {
            String sql = String.format("SELECT login FROM main\n" + "where login = '%s'", login);
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return true;
            } else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


}
