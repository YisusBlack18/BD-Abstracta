import java.sql.Connection;
import src.MySQLBD;
import src.PostgreSQL;

public class App {
    public static void main(String[] args) {
        MySQLBD mySQLBD = new MySQLBD("jdbc:mysql://localhost:3306/prueba", "prueba", "prueba");
        Connection conex = mySQLBD.conectBD();
        
        String query = "SELECT * FROM prueba1";
        System.out.println("ID \tNombre");
        mySQLBD.ejecutaConsulta(conex, query);

        mySQLBD.closeConect(conex);


        // PostgreSQL posgresSQL = new PostgreSQL("jdbc:mysql://localhost:3306/prueba", "prueba", "prueba");
        // Connection conex2 = posgresSQL.conectBD();
        
        // String query2 = "SELECT * FROM prueba1";
        // posgresSQL.ejecutaConsulta(conex2, query2);

        // posgresSQL.closeConect(conex2);

    }
}
