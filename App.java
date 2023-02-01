import java.sql.Connection;

import src.MySQLBD;
import src.PostgreSQL;

public class App {
    public static void main(String[] args) {
        MySQLBD mySQLBD = new MySQLBD("com.", "localhost", "root", "");
        Connection conex = mySQLBD.conectBD();
        String query = "USE Veterinaria";
        mySQLBD.ejecutaQuery(conex, query);
        mySQLBD.closeConect(conex);

        PostgreSQL postgreSQLBD = new PostgreSQL("com.", "localhost", "root", "");
        conex = mySQLBD.conectBD();
        query = "USE Veterinaria";
        postgreSQLBD.ejecutaQuery(conex, query);
        postgreSQLBD.closeConect(conex);

    }
}
