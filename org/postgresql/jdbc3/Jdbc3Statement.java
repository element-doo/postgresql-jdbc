package org.postgresql.jdbc3;


import java.sql.*;
import java.util.Vector;
import org.postgresql.PGRefCursorResultSet;
import org.postgresql.core.BaseResultSet;
import org.postgresql.core.Field;

/* $PostgreSQL: pgjdbc/org/postgresql/jdbc3/Jdbc3Statement.java,v 1.8 2004/06/08 00:01:48 jurka Exp $
 * This class implements the java.sql.Statement interface for JDBC3.
 * However most of the implementation is really done in
 * org.postgresql.jdbc3.AbstractJdbc3Statement or one of it's parents
 */
public class Jdbc3Statement extends org.postgresql.jdbc3.AbstractJdbc3Statement implements java.sql.Statement
{

	public Jdbc3Statement (Jdbc3Connection c) throws SQLException
	{
		super(c);
	}

	public BaseResultSet createResultSet (Field[] fields, Vector tuples, String status, int updateCount, long insertOID) throws SQLException
	{
		return new Jdbc3ResultSet(this, fields, tuples, status, updateCount, insertOID);
	}

  	public PGRefCursorResultSet createRefCursorResultSet (String cursorName) throws SQLException
	{
                return new Jdbc3RefCursorResultSet(this, cursorName);
	}
}
