package org.postgresql.jdbc1;


import java.sql.*;
import java.util.Vector;
import org.postgresql.PGRefCursorResultSet;
import org.postgresql.core.BaseResultSet;
import org.postgresql.core.Field;

/* $PostgreSQL: pgjdbc/org/postgresql/jdbc1/Jdbc1Statement.java,v 1.8 2004/03/29 19:17:11 blind Exp $
 * This class implements the java.sql.Statement interface for JDBC1.
 * However most of the implementation is really done in
 * org.postgresql.jdbc1.AbstractJdbc1Statement
 */
public class Jdbc1Statement extends org.postgresql.jdbc1.AbstractJdbc1Statement implements java.sql.Statement
{

	public Jdbc1Statement (Jdbc1Connection c)
	{
		super(c);
	}

	public BaseResultSet createResultSet (Field[] fields, Vector tuples, String status, int updateCount, long insertOID) throws SQLException
	{
		return new Jdbc1ResultSet(this, fields, tuples, status, updateCount, insertOID);
	}

 	public PGRefCursorResultSet createRefCursorResultSet (String cursorName) throws SQLException
	{
                return new Jdbc1RefCursorResultSet(this, cursorName);
	}
}
