package org.postgresql.jdbc1;


import java.sql.*;
import java.util.Vector;
import org.postgresql.core.BaseStatement;
import org.postgresql.core.Field;

/* $Header: /cvsroot/jdbc/pgjdbc/org/postgresql/jdbc1/Attic/Jdbc1ResultSet.java,v 1.6 2003/03/07 18:39:44 barry Exp $
 * This class implements the java.sql.ResultSet interface for JDBC1.
 * However most of the implementation is really done in
 * org.postgresql.jdbc1.AbstractJdbc1ResultSet
 */
public class Jdbc1ResultSet extends org.postgresql.jdbc1.AbstractJdbc1ResultSet implements java.sql.ResultSet
{

	public Jdbc1ResultSet(BaseStatement statement, Field[] fields, Vector tuples, String status, int updateCount, long insertOID, boolean binaryCursor)
	{
		super(statement, fields, tuples, status, updateCount, insertOID, binaryCursor);
	}

	public java.sql.ResultSetMetaData getMetaData() throws SQLException
	{
		return new Jdbc1ResultSetMetaData(rows, fields);
	}

}

