import java.sql.Connection;
import java.sql.DriverManager;

/**
 * BD
 */
public abstract class BD {
    private String driver = "com.mysql.jdbc.Driver";
    private String url = "jdbc:mysql://localhost:3306/";
    private String user = "root";
    private String pass = "";

    public BD(String driver, String url, String user, String pass) {
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.pass = pass;
    };


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
    };

    public abstract void ejecutaQuery();

    public abstract void closeConect();

    
}

