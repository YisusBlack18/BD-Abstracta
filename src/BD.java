package src;
import java.sql.Connection;

/**
 * BD
 */
public abstract class BD {
    protected String driver;
    protected String url;
    protected String user;
    protected String pass;

    public BD(String driver, String url, String user, String pass) {
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.pass = pass;
    };


    public abstract Connection conectBD();

    public abstract void ejecutaQuery(Connection conex, String query);

    public abstract void closeConect(Connection conex);

    
}

