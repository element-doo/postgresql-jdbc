package org.postgresql.jdbc2;


import java.sql.*;
import java.util.Vector;
import org.postgresql.core.*;

/* $PostgreSQL: pgjdbc/org/postgresql/jdbc2/Jdbc2ResultSet.java,v 1.11 2004/06/29 06:43:27 jurka Exp $
 * This class implements the java.sql.ResultSet interface for JDBC2.
 * However most of the implementation is really done in
 * org.postgresql.jdbc2.AbstractJdbc2ResultSet or one of it's parents
 */
public class Jdbc2ResultSet extends org.postgresql.jdbc2.AbstractJdbc2ResultSet implements java.sql.ResultSet
{
	Jdbc2ResultSet(Query originalQuery, BaseStatement statement, Field[] fields, Vector tuples, ResultCursor cursor,
				   int maxRows, int maxFieldSize, int rsType, int rsConcurrency) throws SQLException
	{
		super(originalQuery, statement, fields, tuples, cursor, maxRows, maxFieldSize, rsType, rsConcurrency);
	}

	public ResultSetMetaData getMetaData() throws SQLException
	{
		return new Jdbc2ResultSetMetaData(connection, fields);
	}

	public java.sql.Clob getClob(int i) throws SQLException
	{
		wasNullFlag = (this_row[i - 1] == null);
		if (wasNullFlag)
			return null;

		return new org.postgresql.jdbc2.Jdbc2Clob(connection, getInt(i));
	}

	public java.sql.Blob getBlob(int i) throws SQLException
	{
		wasNullFlag = (this_row[i - 1] == null);
		if (wasNullFlag)
			return null;

		return new org.postgresql.jdbc2.Jdbc2Blob(connection, getInt(i));
	}

}

