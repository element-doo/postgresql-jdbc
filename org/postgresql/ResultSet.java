package org.postgresql;

import java.lang.*;
import java.io.*;
import java.math.*;
import java.text.*;
import java.util.*;
import java.sql.*;
import org.postgresql.largeobject.*;
import org.postgresql.util.*;

/*
 * This class implements the common internal methods used by both JDBC 1 and
 * JDBC 2 specifications.
 */
public abstract class ResultSet
{
	protected Vector rows;			// The results
	protected Field fields[];		// The field descriptions
	protected String status;		// Status of the result
	protected boolean binaryCursor = false; // is the data binary or Strings
	protected int updateCount;		// How many rows did we get back?
	protected long insertOID;		// The oid of an inserted row
	protected int current_row;		// Our pointer to where we are at
	protected byte[][] this_row;		// the current row result
	protected Connection connection;	// the connection which we returned from
	protected SQLWarning warnings = null;	// The warning chain
	protected boolean wasNullFlag = false;	// the flag for wasNull()

	//	We can chain multiple resultSets together - this points to
	// next resultSet in the chain.
	protected ResultSet next = null;

	/*
	 * Create a new ResultSet - Note that we create ResultSets to
	 * represent the results of everything.
	 *
	 * @param fields an array of Field objects (basically, the
	 *	ResultSet MetaData)
	 * @param tuples Vector of the actual data
	 * @param status the status string returned from the back end
	 * @param updateCount the number of rows affected by the operation
	 * @param cursor the positioned update/delete cursor name
	 */
	public ResultSet(Connection conn, Field[] fields, Vector tuples, String status, int updateCount, long insertOID, boolean binaryCursor)
	{
		this.connection = conn;
		this.fields = fields;
		this.rows = tuples;
		this.status = status;
		this.updateCount = updateCount;
		this.insertOID = insertOID;
		this.this_row = null;
		this.current_row = -1;
		this.binaryCursor = binaryCursor;
	}


	/*
	 * Create a new ResultSet - Note that we create ResultSets to
	 * represent the results of everything.
	 *
	 * @param fields an array of Field objects (basically, the
	 *	ResultSet MetaData)
	 * @param tuples Vector of the actual data
	 * @param status the status string returned from the back end
	 * @param updateCount the number of rows affected by the operation
	 * @param cursor the positioned update/delete cursor name
	 */
	public ResultSet(Connection conn, Field[] fields, Vector tuples, String status, int updateCount)
	{
		this(conn, fields, tuples, status, updateCount, 0, false);
	}

	/*
	 * We at times need to know if the resultSet we are working
	 * with is the result of an UPDATE, DELETE or INSERT (in which
	 * case, we only have a row count), or of a SELECT operation
	 * (in which case, we have multiple fields) - this routine
	 * tells us.
	 *
	 * @return true if we have tuples available
	 */
	public boolean reallyResultSet()
	{
		return (fields != null);
	}

	/*
	 * Since ResultSets can be chained, we need some method of
	 * finding the next one in the chain.  The method getNext()
	 * returns the next one in the chain.
	 *
	 * @return the next ResultSet, or null if there are none
	 */
	public java.sql.ResultSet getNext()
	{
		return (java.sql.ResultSet)next;
	}

	/*
	 * This following method allows us to add a ResultSet object
	 * to the end of the current chain.
	 *
	 * @param r the resultset to add to the end of the chain.
	 */
	public void append(ResultSet r)
	{
		if (next == null)
			next = r;
		else
			next.append(r);
	}

	/*
	 * If we are just a place holder for results, we still need
	 * to get an updateCount.  This method returns it.
	 *
	 * @return the updateCount
	 */
	public int getResultCount()
	{
		return updateCount;
	}

	/*
	 * We also need to provide a couple of auxiliary functions for
	 * the implementation of the ResultMetaData functions.	In
	 * particular, we need to know the number of rows and the
	 * number of columns.  Rows are also known as Tuples
	 *
	 * @return the number of rows
	 */
	public int getTupleCount()
	{
		return rows.size();
	}

	/*
	 * getColumnCount returns the number of columns
	 *
	 * @return the number of columns
	 */
	public int getColumnCount()
	{
		return fields.length;
	}

	/*
	 * Returns the status message from the backend.<p>
	 * It is used internally by the driver.
	 *
	 * @return the status string from the backend
	 */
	public String getStatusString()
	{
		return status;
	}

	/*
	 * returns the OID of a field.<p>
	 * It is used internally by the driver.
	 *
	 * @param field field id
	 * @return the oid of that field's type
	 */
	public int getColumnOID(int field)
	{
		return fields[field -1].getOID();
	}

	/*
	 * returns the OID of the last inserted row.  Deprecated in 7.2 because
         * range for OID values is greater than java signed int.
	 * @deprecated Replaced by getLastOID() in 7.2
	 */
	public int getInsertedOID()
	{
	    return (int) getLastOID();
	}


	/*
	 * returns the OID of the last inserted row
         * @since 7.2
	 */
	public long getLastOID()
	{
		return insertOID;
	}

	/*
	 * This is part of the JDBC API, but is required by org.postgresql.Field
	 */
	public abstract void close() throws SQLException;
	public abstract boolean next() throws SQLException;
	public abstract String getString(int i) throws SQLException;

	/*
	 * This is used to fix get*() methods on Money fields. It should only be
	 * used by those methods!
	 *
	 * It converts ($##.##) to -##.## and $##.## to ##.##
	 */
	public String getFixedString(int col) throws SQLException
	{
		String s = getString(col);

		// Handle SQL Null
		wasNullFlag = (this_row[col - 1] == null);
		if (wasNullFlag)
			return null;

		// Handle Money
		if (s.charAt(0) == '(')
		{
			s = "-" + org.postgresql.util.PGtokenizer.removePara(s).substring(1);
		}
		if (s.charAt(0) == '$')
		{
			s = s.substring(1);
		}

		return s;
	}
}

