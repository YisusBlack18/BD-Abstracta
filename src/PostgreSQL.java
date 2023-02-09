package src;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgreSQL extends BD {
    

    public PostgreSQL(String url, String user, String pass) {
        super(url, user, pass);
    }

    public void main(String[] args) {
        Connection conex = conectBD();
        String query = "SELECT * FROM prueba1";
        ejecutaConsulta(conex, query);
        
    }

    @Override
    public Connection conectBD() {
        Connection conex = null;
        try {
            conex = DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            System.out.println("Error al conectar con la base de datos.\n"
                    + e.getMessage().toString());
        }
        return conex;
    }

    @Override
    public void ejecutaUpdate(Connection conex, String query) {
        Statement sentencias = null;
        try {
            sentencias = conex.createStatement();
            sentencias.executeUpdate(query);
            sentencias.close();
        } catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void closeConect(Connection conex) {
        try {
            conex.close();
        } catch(SQLException e) {
            System.out.println(e.getMessage().toString());
        }
    }

    @Override
    public void ejecutaConsulta(Connection conex, String query) {
        Statement s;
        try {
            s = conex.createStatement();
            ResultSet rs = s.executeQuery (query);
            while (rs.next()) {
                System.out.println (rs.getInt (1) + " " + rs.getString (2));
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        
    }
}