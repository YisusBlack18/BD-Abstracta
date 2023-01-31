import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * An abstract class for java Objects that are to be used as database models.<br>
 * Implements the following public functions: <b>{@link #save(Connection)},
 * {@link #delete(Connection)}, {@link #createFromRS(ResultSet)},
 * {@link #toString()}</b> <br>
 * <br>
 * Every class the extends this one must satisfy the following requirements: <li>
 * The class name must have the same name as the corresponding table in the
 * database (case insensitive).</li> <li>The attributes of the class must also
 * have the same name as the columns of the table.</li><li>The class must have
 * the variable "<code>private static final String[] primaryKey</code>" declared
 * first, which is an array of the primary key attribute names. (e.g primaryKey
 * = {"id", "name"} where "id" and "name" exist as attributes as well)</li><br>
 * <br>
 * 
 * 
 * 
 * @author Georgios Tzourmpakis<br>
 *         kiougar@gmail.com<br>
 *         Date: Apr 16, 2014
 */
public abstract class DBModel {
	/**
	 * Controls whether save and delete functions print status messages.
	 */
	private static boolean showStatus = false;

	/**
	 * Returns the substring of the class name after the last dot. In short, the
	 * Class name as declared without the package prefix.
	 * 
	 * @param name
	 *            String class name as returned by {@link Class#getName()
	 *            Class.getName()}
	 * @return String class name as declared without the package prefix.
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 24, 2014
	 */
	private static String getModelName(String name) {
		int idx = name.lastIndexOf(".");
		return name.substring(idx + 1);
	}

	/**
	 * Function to get the superClass with the given name
	 * 
	 * @param model
	 *            String name of the super class to get
	 * @return Class object
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 22, 2014
	 */
	private Class<?> getClassWithName(String model) {
		Class<?> c = this.getClass();
		while (!c.getName().endsWith(model)) {
			c = c.getSuperclass();
		}
		return c;
	}

	/**
	 * Inner function to recursively collect all fields of a class and the
	 * inherited ones from super classes.<br>
	 * <br>
	 * Used by {@link #getAllFields()}
	 * 
	 * @param fields
	 *            A List&ltField&gt object
	 * @param type
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 24, 2014
	 */
	private static void getAllFieldsInner(ArrayList<Field> fields, Class<?> type) {
		for (Field field : type.getDeclaredFields()) {
			fields.add(field);
		}

		if (!type.getSuperclass().getName().endsWith("DBModel")) {
			getAllFieldsInner(fields, type.getSuperclass());
		}
	}

	/**
	 * Collect all fields of a class and the inherited ones from the super
	 * classes and return them in a Field array.
	 * 
	 * @return Field[] Array of fields of the class
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 24, 2014
	 */
	private Field[] getAllFields() {
		ArrayList<Field> ls = new ArrayList<Field>();
		getAllFieldsInner(ls, this.getClass());
		return ls.toArray(new Field[ls.size()]);
	}

	/**
	 * Getter for String[] primaryKey attribute.
	 * 
	 * @param model
	 *            String name of the model to get primaryKey for
	 * @return String array of primary key names
	 * 
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 22, 2014
	 */
	private String[] getPrimaryKey(String model) throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		Class<?> c = this.getClassWithName(model);

		Field pk = c.getDeclaredField("primaryKey");
		pk.setAccessible(true);
		return (String[]) pk.get(this);
	}

	/**
	 * Simple method to join a String array with a separator
	 * 
	 * @param sArr
	 *            String array to join
	 * @param sSep
	 *            Separator to join array with
	 * @return Result string
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 21, 2014
	 */
	private static String strJoin(String[] sArr, String sSep) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, il = sArr.length; i < il; i++) {
			if (i > 0)
				sb.append(sSep);
			sb.append(sArr[i]);
		}
		return sb.toString();
	}

	/**
	 * Function to be used in {@link #update(Connection)} and
	 * {@link #delete(Connection)} functions. It returns an SQL like string (e.g
	 * ID='1025' AND NAME='George') used after the 'WHERE' keyword.
	 * 
	 * @return SQL like string
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 21, 2014
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 */
	private String getPrimaryKeySQL(String model) throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		String[] pk = this.getPrimaryKey(model).clone();
		Field field;
		Class<?> c = this.getClassWithName(model);
		for (int i = 0; i < pk.length; i++) {
			field = c.getDeclaredField(pk[i]);
			pk[i] = pk[i] + "='" + field.get(this) + "'";
		}
		return strJoin(pk, " AND ");
	}

	/**
	 * Inner function to insert an object in the Database. Properly inserts
	 * super classes first. Adds in the Statement batch the insert SQL. After
	 * finishing you need to .executeBatch in the Statement object. <br>
	 * <br>
	 * Used by {@link #create(Connection)}
	 * 
	 * @param st
	 *            Statement object to execute the SQL batch.
	 * @param c
	 *            Class object of the current object to save.
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 22, 2014
	 */
	private void createInner(Statement st, Class<?> c)
			throws IllegalArgumentException, IllegalAccessException,
			SQLException {
		// first save superclass in database
		if (!c.getSuperclass().getName().endsWith("DBModel")) {
			createInner(st, c.getSuperclass());
		}

		// get model name
		String model = getModelName(c.getName());

		// find fields and values of the class
		String names = "(";
		String values = "(";
		Field[] fields = c.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			if (Modifier.isStatic(fields[i].getModifiers()))
				continue;
			names += fields[i].getName();
			if (fields[i].get(this) == null)
				values += fields[i].get(this);
			else
				values += "'" + fields[i].get(this) + "'";
			if (i < fields.length - 1) {
				names += ",";
				values += ",";
			}
		}
		names += ")";
		values += ")";

		// insert into database
		String sql = "INSERT INTO " + model + names + " VALUES" + values + ";";
		// System.out.println(sql);
		st.addBatch(sql);
	}

	/**
	 * Inserts the current Java object on the open database (accessed through
	 * conn object). Requires an already created table on the database with the
	 * same name of the current class.
	 * 
	 * @param conn
	 *            SQL Connection object
	 * @return True if succeeded. False otherwise.
	 * 
	 * @throws SQLException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 16, 2014
	 */
	private boolean create(Connection conn) throws SQLException,
			IllegalArgumentException, IllegalAccessException {
		Statement st = conn.createStatement();
		createInner(st, this.getClass());
		int[] res;
		try {
			res = st.executeBatch();
			if (st.getWarnings() != null)
				System.out.println(st.getWarnings());
		} catch (BatchUpdateException e) {
			SQLException tmp = e.getNextException();
			while (tmp != null) {
				System.err.println(tmp.getMessage());
				tmp = tmp.getNextException();
			}
			throw e;
		}
		for (int r : res) {
			if (r == 0)
				return false;
		}
		return true;
	}

	/**
	 * Inner function to update an object in the Database. Properly updates
	 * super classes first. Adds in the Statement batch the update SQL. After
	 * finishing you need to .executeBatch in the Statement object.<br>
	 * <br>
	 * Used by {@link #update(Connection)}
	 * 
	 * @param st
	 *            Statement object to execute the SQL batch.
	 * @param c
	 *            Class object of the current object to save.
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws SQLException
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 22, 2014
	 */
	private void updateInner(Statement st, Class<?> c)
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException, SQLException {
		// first update superclass in database
		if (!c.getSuperclass().getName().endsWith("DBModel")) {
			updateInner(st, c.getSuperclass());
		}

		// get model name
		String model = getModelName(c.getName());

		// find fields and values of the class
		String sets = "";
		Field[] fields = c.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			if (Modifier.isStatic(fields[i].getModifiers()))
				continue;
			if (fields[i].get(this) == null)
				sets += fields[i].getName() + "=null";
			else
				sets += fields[i].getName() + "='" + fields[i].get(this) + "'";
			if (i < fields.length - 1) {
				sets += ",";
			}
		}

		// update database
		String sql = "UPDATE " + model + " SET " + sets + " WHERE "
				+ this.getPrimaryKeySQL(model) + ";";
		// System.out.println(sql);
		st.addBatch(sql);
	}

	/**
	 * Updates (saves) the current Java object on the open database (accessed
	 * through conn object). Requires an already created table on the database
	 * with the same name of the current class.
	 * 
	 * @param conn
	 *            SQL Connection object
	 * @return True if succeeded. False otherwise.
	 * 
	 * @throws SQLException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 16, 2014
	 * 
	 */
	private boolean update(Connection conn) throws SQLException,
			NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException {
		Statement st = conn.createStatement();
		updateInner(st, this.getClass());
		int[] res;
		try {
			res = st.executeBatch();
			if (st.getWarnings() != null)
				System.out.println(st.getWarnings());
		} catch (BatchUpdateException e) {
			SQLException tmp = e.getNextException();
			while (tmp != null) {
				System.err.println(tmp.getMessage());
				tmp = tmp.getNextException();
			}
			throw e;
		}
		for (int r : res) {
			if (r == 0)
				return false;
		}
		return true;
	}

	/**
	 * Saves the current Java object in the open database (accessed through conn
	 * object). If the object exists, uses {@link #update(Connection)} else,
	 * uses {@link #create(Connection)}. Requires an already created table on
	 * the database with the same name of the current class.
	 * 
	 * @param conn
	 *            SQL Connection object
	 * @return True if succeeded. False otherwise.
	 * 
	 * @throws SQLException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 21, 2014
	 */
	public boolean save(Connection conn) throws SQLException,
			NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException {
		boolean res;
		// get model name
		String model = getModelName(this.getClass().getName());
		// get primary key
		String pksql = this.getPrimaryKeySQL(model);
		// check if record exists
		String sql = "SELECT 1 FROM " + model + " WHERE " + pksql + ";";
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery(sql);
		// if exists use update
		if (rs.next()) {
			if (showStatus)
				System.out
						.println("\n\tUpdating " + model + " (" + pksql + ")");
			res = this.update(conn);
			if (showStatus)
				if (res)
					System.out.println("\tUpdate succeeded!");
				else
					System.out.println("\tUpdate failed!");
		} else { // else use create
			if (showStatus)
				System.out
						.println("\n\tCreating " + model + " (" + pksql + ")");
			res = this.create(conn);
			if (showStatus)
				if (res)
					System.out.println("\tInsert succeeded!");
				else
					System.out.println("\tInsert failed!");
		}
		rs.close();
		st.close();
		return res;
	}

	/**
	 * Deletes the current Java object from the open database (accessed through
	 * conn object). Requires an already created table on the database with the
	 * same name of the current class. Assumes that the first field (attribute)
	 * of the class is the ID of the corresponding table row in the database.
	 * 
	 * @param conn
	 *            SQL Connection object
	 * @return True if succeeded. False otherwise.
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws SQLException
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 16, 2014
	 */
	public boolean delete(Connection conn) throws NoSuchFieldException,
			SecurityException, IllegalArgumentException,
			IllegalAccessException, SQLException {
		// find super class name
		Class<?> c = this.getClass();
		while (!c.getSuperclass().getName().endsWith("DBModel")) {
			c = c.getSuperclass();
		}
		// get model name
		String model = getModelName(c.getName());
		// get primary key
		String pksql = this.getPrimaryKeySQL(model);
		if (showStatus)
			System.out.println("\n\tDeleting " + model + " (" + pksql + ")");

		// delete from database
		String sql = "DELETE FROM " + model + " WHERE " + pksql + ";";
		// System.out.println(sql);
		Statement st = conn.createStatement();
		int res = st.executeUpdate(sql);
		if (st.getWarnings() != null) {
			System.out.println(st.getWarnings());
		}
		if (showStatus)
			if (res == 1)
				System.out.println("\tDelete succeeded!");
			else
				System.out.println("\tDelete failed!");
		st.close();
		return res == 1;
	}

	/**
	 * Creates the current object from the ResultSet columns. Prints an error if
	 * a specific field was not in the result set but doen't throw an exception.
	 * 
	 * 
	 * @param rs
	 *            ResultSet containing the object values in columns.
	 * @return -1 if ResultSet is null else a float ranging from 0 to 1
	 *         (inclusive) based on the percentage of correctly filled
	 *         attributes.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws SQLException
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 24, 2014
	 */
	public float createFromRS(ResultSet rs) throws IllegalArgumentException,
			IllegalAccessException, SQLException {
		if (rs == null) {
			return -1;
		}
		Field[] fields = this.getAllFields();
		int correct = 0, all = 0;
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			all++;
			try {
				field.set(this, rs.getObject(field.getName()));
				correct++;
			} catch (SQLException e) {
				if (e.getMessage().endsWith("was not found in this ResultSet.")) {
					System.err.println("createFromRS: " + e.getMessage());
				} else {
					throw e;
				}
			}
		}
		return (float) correct / all;
	}

	/**
	 * Creates the SQL table based on the Java Object.
	 * 
	 * @param conn
	 *            SQL Connection object
	 * @return True if succeeded. False otherwise.
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 24, 2014
	 */
	public boolean createTable(Connection conn) throws Exception {
		String model = getModelName(this.getClass().getName());
		if (!DButils.tableExists(conn, model)) { // TODO Create table
			// Statement st = conn.createStatement(); //
			// st.execute("CREATE TABLE ");
		}
		return false;
	}

	/**
	 * Overrides {@link Object#toString()} method.<br>
	 * Returns the first primary key of the object as a string
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 24, 2014
	 */
	public String toString() {
		String model = getModelName(this.getClass().getName());
		String res;
		try {
			String[] pk = this.getPrimaryKey(model).clone();
			Field field;
			field = this.getClass().getDeclaredField(pk[0]);
			res = field.get(this).toString();
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			res = null;
		}

		return res;
	}

	/**
	 * Prints the whole DBModel with its attributes.
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: May 3, 2014
	 */
	public void print() {
		String model = getModelName(this.getClass().getName());
		String res = model + ": ";
		Field[] fields = this.getAllFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			field.setAccessible(true);
			try {
				res += field.getName() + "='"
						+ field.get(this).toString().trim() + "', ";
			} catch (IllegalArgumentException | IllegalAccessException
					| NullPointerException e) {
				res += field.getName() + "=null, ";
			}
		}

		System.out.println(res);
	}

	/**
	 * Prints all records of the class that called this function that are stored
	 * in the connected database.<br>
	 * <b>You should call this function from an empty DBModel object (an object
	 * instantiated by an empty constructor) because this function overwrites
	 * the object.</b>
	 * 
	 * @param conn
	 *            SQL Connection object.
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: Apr 26, 2014
	 */
	public void printAll(Connection conn) {
		try {
			Class<?> c = this.getClass();
			// if it's not a direct descendant of DBModel there is only one pk
			String pk = this.getPrimaryKey(c.getName())[0];
			String pksql = " WHERE ";
			boolean hasSuperClass = false;
			String sql = "SELECT * FROM ";
			String model;
			while (!c.getSuperclass().getName().endsWith("DBModel")) {
				hasSuperClass = true;
				model = getModelName(c.getName());
				sql += model + ",";
				c = c.getSuperclass();
				pksql += pk + "=" + this.getPrimaryKey(c.getName())[0] + " AND";
			}
			model = getModelName(c.getName());
			sql += model;
			if (hasSuperClass)
				sql += pksql.substring(0, pksql.length() - 4) + " ORDER BY "
						+ pk;
			// System.out.println(sql);
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			model = getModelName(this.getClass().getName());
			System.out.println("\n ========= Printing all " + model
					+ "s =========\n");
			while (rs.next()) {
				this.createFromRS(rs);
				this.print();
			}
			System.out.println(" =========  End of all " + model
					+ "s  =========");
			rs.close();
			st.close();
		} catch (Exception e) {
			return;
		}
	}

	/**
	 * Returns an ArrayList of all records (as Objects) of the class that called
	 * this function that are stored in the connected database.<br>
	 * 
	 * @param conn
	 *            SQL Connection object.
	 * @return ArrayList<?> A collection of the records in the database
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: May 3, 2014
	 */
	public ArrayList<? extends Object> getAll(Connection conn) {
		try {
			ArrayList<Object> res = new ArrayList<Object>();
			Object e;
			Class<?> c = this.getClass();
			// if it's not a direct descendant of DBModel there is only one pk
			String pk = this.getPrimaryKey(c.getName())[0];
			String pksql = " WHERE ";
			boolean hasSuperClass = false;
			String sql = "SELECT * FROM ";
			String model;
			while (!c.getSuperclass().getName().endsWith("DBModel")) {
				hasSuperClass = true;
				model = getModelName(c.getName());
				sql += model + ",";
				c = c.getSuperclass();
				pksql += pk + "=" + this.getPrimaryKey(c.getName())[0] + " AND";
			}
			model = getModelName(c.getName());
			sql += model;
			if (hasSuperClass)
				sql += pksql.substring(0, pksql.length() - 4);
			sql += " ORDER BY " + pk;
			// System.out.println(sql);
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);

			while (rs.next()) {
				e = this.getClass().newInstance();
				((DBModel) e).createFromRS(rs);
				// System.out.print("Adding: ");e.print();
				res.add(e);
			}

			return res;
		} catch (Exception e) {
			return new ArrayList<Object>();
		}

	}

	/**
	 * Returns an ArrayList of all records (as Objects) of the class that called
	 * this function that are stored in the connected database.<br>
	 * 
	 * @param conn
	 *            SQL Connection object.
	 * @param filters
	 *            A String[] that holds sql filters (e.g "id = 20")
	 * @return ArrayList<?> A collection of the records in the database
	 * 
	 * @author Georgios Tzourmpakis<br>
	 *         AM: 2007030034<br>
	 *         Date: May 23, 2014
	 */
	public ArrayList<? extends Object> getAll(Connection conn, String[] filters) {
		try {
			ArrayList<Object> res = new ArrayList<Object>();
			Object e;
			Class<?> c = this.getClass();
			// if it's not a direct descendant of DBModel there is only one pk
			String pk = this.getPrimaryKey(c.getName())[0];
			String pksql = " WHERE ";
			boolean hasSuperClass = false;
			String sql = "SELECT * FROM ";
			String model;
			while (!c.getSuperclass().getName().endsWith("DBModel")) {
				hasSuperClass = true;
				model = getModelName(c.getName());
				sql += model + ",";
				c = c.getSuperclass();
				pksql += pk + "=" + this.getPrimaryKey(c.getName())[0] + " AND";
			}
			model = getModelName(c.getName());
			sql += model;
			if (hasSuperClass)
				sql += pksql.substring(0, pksql.length() - 4);
			// apply filters if any
			if (filters.length > 0) {
				sql += " WHERE " + strJoin(filters, " AND ");
				// System.out.println(" WHERE " + strJoin(filters, " AND "));
			}
			sql += " ORDER BY " + pk;
			// System.out.println(sql);
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);

			while (rs.next()) {
				e = this.getClass().newInstance();
				((DBModel) e).createFromRS(rs);
				// System.out.print("Adding: ");e.print();
				res.add(e);
			}

			return res;
		} catch (Exception e) {
			return new ArrayList<Object>();
		}
	}
}