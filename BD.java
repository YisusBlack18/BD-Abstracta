import java.sql.Connection;
// import java.sql.Driver;

/**
 * BD
 */
public abstract class BD {
    // private Driver controlador;
    private Connection conexion;

    public Connection conecBD() {
        this.conexion = null;
        

        return conexion;
    }

    public abstract void ejecutaQuery();



    
}

