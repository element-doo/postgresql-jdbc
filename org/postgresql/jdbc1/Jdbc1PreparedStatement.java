package org.postgresql.jdbc1;


import java.sql.*;
import org.postgresql.PGRefCursorResultSet;
import org.postgresql.core.BaseResultSet;
import org.postgresql.core.Field;

public class Jdbc1PreparedStatement extends AbstractJdbc1Statement implements PreparedStatement
{

	public Jdbc1PreparedStatement(Jdbc1Connection connection, String sql) throws SQLException
	{
		super(connection, sql);
	}

	public BaseResultSet createResultSet (Field[] fields, java.util.Vector tuples, String status, int updateCount, long insertOID) throws SQLException
	{
		return new Jdbc1ResultSet(this, fields, tuples, status, updateCount, insertOID);
	}

 	public PGRefCursorResultSet createRefCursorResultSet (String cursorName) throws SQLException
	{
                return new Jdbc1RefCursorResultSet(this, cursorName);
	}
}
