package src;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgreSQL extends BD {
    

    public PostgreSQL(String url, String user, String pass) {
        super(url, user, pass);
    }

    public void main(String[] args) {
        Connection conex = conectBD();
        String query = "USE Veterinaria";
        ejecutaQuery(conex, query);
        
    }

    @Override
    public Connection conectBD() {
        Connection conex = null;
        
        try {
            Class.forName(driver);
            conex = DriverManager.getConnection(url, user, pass);
            
        } catch (Exception e) {
            System.out.println("Error al conectar con la base de datos.\n"
                    + e.getMessage().toString());
        }
        return conex;
    }

    @Override
    public void ejecutaQuery(Connection conex, String query) {
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
    public void cargaDriver() {
        // TODO Auto-generated method stub
        
    }
}