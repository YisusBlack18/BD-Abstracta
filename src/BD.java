package src;
import java.sql.Connection;

/**
 * BD
 */
public abstract class BD {
    protected static String driver;
    protected String url;
    protected String user;
    protected String pass;
    

    public BD(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    };
    
    public abstract Connection conectBD();

    public abstract void ejecutaUpdate(Connection conex, String query);

    public abstract void ejecutaConsulta(Connection conex, String query);

    public abstract void closeConect(Connection conex);

    
}

