package org.postgresql.jdbc1;


import java.sql.*;
import java.util.Vector;
import org.postgresql.core.BaseResultSet;
import org.postgresql.core.Field;

/* $Header: /cvsroot/jdbc/pgjdbc/org/postgresql/jdbc1/Attic/Jdbc1Statement.java,v 1.5 2003/03/07 18:39:44 barry Exp $
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

	public BaseResultSet createResultSet (Field[] fields, Vector tuples, String status, int updateCount, long insertOID, boolean binaryCursor) throws SQLException
	{
		return new Jdbc1ResultSet(this, fields, tuples, status, updateCount, insertOID, binaryCursor);
	}
}
