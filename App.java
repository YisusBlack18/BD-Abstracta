import java.sql.Connection;

import src.MySQLBD;
import src.PostgreSQL;

public class App {
    public static void main(String[] args) {
        MySQLBD mySQLBD = new MySQLBD("com.mysql.jdbc.Driver", "jdbc:mysql://192.168.0.50/", "root", "");
        Connection conex = mySQLBD.conectBD();
        String query = "USE Veterinaria";
        mySQLBD.ejecutaQuery(conex, query);
        mySQLBD.closeConect(conex);

        PostgreSQL postgreSQLBD = new PostgreSQL("org.postgresql.Driver", "jdbc:postgresql://192.168.0.50/", "root", "");
        conex = mySQLBD.conectBD();
        query = "USE Veterinaria";
        postgreSQLBD.ejecutaQuery(conex, query);
        postgreSQLBD.closeConect(conex);

    }
}
