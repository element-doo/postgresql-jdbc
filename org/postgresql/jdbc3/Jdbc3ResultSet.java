package org.postgresql.jdbc3;


import java.sql.*;
import java.util.Vector;
import org.postgresql.core.*;

/* $PostgreSQL: pgjdbc/org/postgresql/jdbc3/Jdbc3ResultSet.java,v 1.8 2004/06/29 06:43:27 jurka Exp $
 * This class implements the java.sql.ResultSet interface for JDBC3.
 * However most of the implementation is really done in
 * org.postgresql.jdbc3.AbstractJdbc3ResultSet or one of it's parents
 */
public class Jdbc3ResultSet extends org.postgresql.jdbc3.AbstractJdbc3ResultSet implements java.sql.ResultSet
{
	Jdbc3ResultSet(Query originalQuery, BaseStatement statement, Field[] fields, Vector tuples, ResultCursor cursor, 
				   int maxRows, int maxFieldSize, int rsType, int rsConcurrency, int rsHoldability) throws SQLException
	{
		super(originalQuery, statement, fields, tuples, cursor, maxRows, maxFieldSize, rsType, rsConcurrency, rsHoldability);
	}

	public java.sql.ResultSetMetaData getMetaData() throws SQLException
	{
		return new Jdbc3ResultSetMetaData(connection, fields);
	}

	public java.sql.Clob getClob(int i) throws SQLException
	{
		wasNullFlag = (this_row[i - 1] == null);
		if (wasNullFlag)
			return null;

		return new Jdbc3Clob(connection, getInt(i));
	}

	public java.sql.Blob getBlob(int i) throws SQLException
	{
		wasNullFlag = (this_row[i - 1] == null);
		if (wasNullFlag)
			return null;

		return new Jdbc3Blob(connection, getInt(i));
	}

}

