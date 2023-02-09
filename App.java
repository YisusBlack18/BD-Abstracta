import java.sql.Connection;

import src.MySQLBD;
import src.PostgreSQL;

public class App {
    public static void main(String[] args) {
        MySQLBD mySQLBD = new MySQLBD("jdbc:mysql://localhost:3306/prueba", "prueba", "prueba");
        Connection conex = mySQLBD.conectBD();
        String query = "USE prueba";
        mySQLBD.ejecutaQuery(conex, query);
        query = "SELECT * FROM prueba1";
        mySQLBD.ejecutaQuery(conex, query);
        mySQLBD.closeConect(conex);

        // PostgreSQL postgreSQLBD = new PostgreSQL("org.postgresql.Driver", "jdbc:postgresql://10.10.148.229:3306/", "root", "");
        // conex = mySQLBD.conectBD();
        // query = "USE prueba";
        // postgreSQLBD.ejecutaQuery(conex, query);
        // postgreSQLBD.closeConect(conex);

    }
}
