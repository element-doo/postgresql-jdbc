/*-------------------------------------------------------------------------
*
* Copyright (c) 2003-2005, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc2/AbstractJdbc2ResultSet.java,v 1.70 2005/01/14 01:20:19 oliver Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc2;

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.GregorianCalendar;
import org.postgresql.Driver;
import org.postgresql.core.*;
import org.postgresql.largeobject.*;
import org.postgresql.util.PGobject;
import org.postgresql.util.PGbytea;
import org.postgresql.util.PGtokenizer;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.GT;


public abstract class AbstractJdbc2ResultSet implements BaseResultSet, org.postgresql.PGRefCursorResultSet
{

    //needed for updateable result set support
    private boolean updateable = false;
    private boolean doingUpdates = false;
    private HashMap updateValues = null;
    private boolean usingOID = false; // are we using the OID for the primary key?
    private Vector primaryKeys;    // list of primary keys
    private boolean singleTable = false;
    private String tableName = null;
    private PreparedStatement updateStatement = null;
    private PreparedStatement insertStatement = null;
    private PreparedStatement deleteStatement = null;
    private PreparedStatement selectStatement = null;
    private int resultsettype;
    private int resultsetconcurrency;
    private int fetchdirection = ResultSet.FETCH_UNKNOWN;
    protected final BaseConnection connection;  // the connection we belong to
    protected final BaseStatement statement;    // the statement we belong to
    protected final Field fields[];          // Field metadata for this resultset.
    protected final Query originalQuery;        // Query we originated from

    protected final int maxRows;            // Maximum rows in this resultset (might be 0).
    protected final int maxFieldSize;       // Maximum field size in this resultset (might be 0).

    protected Vector rows;           // Current page of results.
    protected int current_row = -1;         // Index into 'rows' of our currrent row (0-based)
    protected int row_offset;               // Offset of row 0 in the actual resultset
    protected byte[][] this_row;      // copy of the current result row
    protected SQLWarning warnings = null; // The warning chain
    protected boolean wasNullFlag = false; // the flag for wasNull()
    protected boolean onInsertRow = false;  // are we on the insert row (for JDBC2 updatable resultsets)?

    private GregorianCalendar calendar = null;
    public byte[][] rowBuffer = null;       // updateable rowbuffer

    protected int fetchSize;       // Current fetch size (might be 0).
    protected ResultCursor cursor; // Cursor for fetching additional data.

    private HashMap columnNameIndexMap; // Speed up findColumn by caching lookups

    public abstract ResultSetMetaData getMetaData() throws SQLException;


    public AbstractJdbc2ResultSet(Query originalQuery, BaseStatement statement, Field[] fields, Vector tuples,
                                  ResultCursor cursor, int maxRows, int maxFieldSize,
                                  int rsType, int rsConcurrency) throws SQLException
    {
        this.originalQuery = originalQuery;
        this.connection = (BaseConnection) statement.getConnection();
        this.statement = statement;
        this.fields = fields;
        this.rows = tuples;
        this.cursor = cursor;
        this.maxRows = maxRows;
        this.maxFieldSize = maxFieldSize;
        this.resultsettype = rsType;
        this.resultsetconcurrency = rsConcurrency;
    }

    public java.net.URL getURL(int columnIndex) throws SQLException
    {
        checkClosed();
        throw org.postgresql.Driver.notImplemented();
    }


    public java.net.URL getURL(String columnName) throws SQLException
    {
        return getURL(findColumn(columnName));
    }

    protected Object internalGetObject(int columnIndex, Field field) throws SQLException
    {
        switch (getSQLType(columnIndex))
        {
        case Types.BIT:
            return getBoolean(columnIndex) ? Boolean.TRUE : Boolean.FALSE;
        case Types.SMALLINT:
            return new Short(getShort(columnIndex));
        case Types.INTEGER:
            return new Integer(getInt(columnIndex));
        case Types.BIGINT:
            return new Long(getLong(columnIndex));
        case Types.NUMERIC:
            return getBigDecimal
                   (columnIndex, (field.getMod() == -1) ? -1 : ((field.getMod() - 4) & 0xffff));
        case Types.REAL:
            return new Float(getFloat(columnIndex));
        case Types.DOUBLE:
            return new Double(getDouble(columnIndex));
        case Types.CHAR:
        case Types.VARCHAR:
            return getString(columnIndex);
        case Types.DATE:
            return getDate(columnIndex);
        case Types.TIME:
            return getTime(columnIndex);
        case Types.TIMESTAMP:
            return getTimestamp(columnIndex);
        case Types.BINARY:
        case Types.VARBINARY:
            return getBytes(columnIndex);
        case Types.ARRAY:
            return getArray(columnIndex);

        default:
            String type = getPGType(columnIndex);

            // if the backend doesn't know the type then coerce to String
            if (type.equals("unknown"))
                return getString(columnIndex);

            // Specialized support for ref cursors is neater.
            if (type.equals("refcursor"))
            {
                // Fetch all results.
                String cursorName = getString(columnIndex);
                String fetchSql = "FETCH ALL IN \"" + cursorName + "\"";

                // nb: no BEGIN triggered here. This is fine. If someone
                // committed, and the cursor was not holdable (closing the
                // cursor), we avoid starting a new xact and promptly causing
                // it to fail. If the cursor *was* holdable, we don't want a
                // new xact anyway since holdable cursor state isn't affected
                // by xact boundaries. If our caller didn't commit at all, or
                // autocommit was on, then we wouldn't issue a BEGIN anyway.
                ResultSet rs = connection.execSQLQuery(fetchSql);
                ((AbstractJdbc2ResultSet)rs).setRefCursor(cursorName);
                return rs;
            }

            // Caller determines what to do (JDBC2 overrides in this case)
            return null;
        }
    }

    private void checkScrollable() throws SQLException
    {
        checkClosed();
        if (resultsettype == ResultSet.TYPE_FORWARD_ONLY)
            throw new PSQLException(GT.tr("Operation requires a scrollable ResultSet, but this ResultSet is FORWARD_ONLY."),
                                    PSQLState.INVALID_CURSOR_STATE);
    }

    public boolean absolute(int index) throws SQLException
    {
        checkScrollable();

        // index is 1-based, but internally we use 0-based indices
        int internalIndex;

        if (index == 0)
        {
            beforeFirst();
            return false;
        }

        final int rows_size = rows.size();

        //if index<0, count from the end of the result set, but check
        //to be sure that it is not beyond the first index
        if (index < 0)
        {
            if (index >= -rows_size)
                internalIndex = rows_size + index;
            else
            {
                beforeFirst();
                return false;
            }
        }
        else
        {
            //must be the case that index>0,
            //find the correct place, assuming that
            //the index is not too large
            if (index <= rows_size)
                internalIndex = index - 1;
            else
            {
                afterLast();
                return false;
            }
        }

        current_row = internalIndex;
        this_row = (byte[][]) rows.elementAt(internalIndex);

        rowBuffer = new byte[this_row.length][];
        System.arraycopy(this_row, 0, rowBuffer, 0, this_row.length);
        onInsertRow = false;

        return true;
    }


    public void afterLast() throws SQLException
    {
        checkScrollable();

        final int rows_size = rows.size();
        if (rows_size > 0)
            current_row = rows_size;

        onInsertRow = false;
        this_row = null;
        rowBuffer = null;
    }


    public void beforeFirst() throws SQLException
    {
        checkScrollable();

        if (rows.size() > 0)
            current_row = -1;

        onInsertRow = false;
        this_row = null;
        rowBuffer = null;
    }


    public boolean first() throws SQLException
    {
        checkScrollable();

        if (rows.size() <= 0)
            return false;

        current_row = 0;
        this_row = (byte[][]) rows.elementAt(current_row);

        rowBuffer = new byte[this_row.length][];
        System.arraycopy(this_row, 0, rowBuffer, 0, this_row.length);
        onInsertRow = false;

        return true;
    }


    public java.sql.Array getArray(String colName) throws SQLException
    {
        return getArray(findColumn(colName));
    }


    public java.sql.Array getArray(int i) throws SQLException
    {
        checkResultSet( i );

        wasNullFlag = (this_row[i - 1] == null);
        if (wasNullFlag)
            return null;

        return createArray(i);
    }


    public java.math.BigDecimal getBigDecimal(int columnIndex) throws SQLException
    {
        return getBigDecimal(columnIndex, -1);
    }


    public java.math.BigDecimal getBigDecimal(String columnName) throws SQLException
    {
        return getBigDecimal(findColumn(columnName));
    }


    public Blob getBlob(String columnName) throws SQLException
    {
        return getBlob(findColumn(columnName));
    }


    public abstract Blob getBlob(int i) throws SQLException;


    public java.io.Reader getCharacterStream(String columnName) throws SQLException
    {
        return getCharacterStream(findColumn(columnName));
    }


    public java.io.Reader getCharacterStream(int i) throws SQLException
    {
        checkResultSet( i );
        wasNullFlag = (this_row[i - 1] == null);
        if (wasNullFlag)
            return null;

        if (((AbstractJdbc2Connection) connection).haveMinimumCompatibleVersion("7.2"))
        {
            //Version 7.2 supports AsciiStream for all the PG text types
            //As the spec/javadoc for this method indicate this is to be used for
            //large text values (i.e. LONGVARCHAR) PG doesn't have a separate
            //long string datatype, but with toast the text datatype is capable of
            //handling very large values.  Thus the implementation ends up calling
            //getString() since there is no current way to stream the value from the server
            return new CharArrayReader(getString(i).toCharArray());
        }
        else
        {
            // In 7.1 Handle as BLOBS so return the LargeObject input stream
            Encoding encoding = connection.getEncoding();
            InputStream input = getBinaryStream(i);

            try
            {
                return encoding.getDecodingReader(input);
            }
            catch (IOException ioe)
            {
                throw new PSQLException(GT.tr("Unexpected error while decoding character data from a large object."), PSQLState.UNEXPECTED_ERROR, ioe);
            }
        }
    }


    public Clob getClob(String columnName) throws SQLException
    {
        return getClob(findColumn(columnName));
    }


    public abstract Clob getClob(int i) throws SQLException;


    public int getConcurrency() throws SQLException
    {
        checkClosed();
        return resultsetconcurrency;
    }


    public java.sql.Date getDate(int i, java.util.Calendar cal) throws SQLException
    {
        // apply available calendar if there is no timezone information
        if (cal == null || getPGType(i).endsWith("tz") )
            return getDate(i);

        java.util.Date tmp = getDate(i);
        if (tmp == null)
            return null;

        cal = org.postgresql.jdbc2.AbstractJdbc2Statement.changeTime(tmp, cal, false);
        return new java.sql.Date(cal.getTime().getTime());
    }


    public Time getTime(int i, java.util.Calendar cal) throws SQLException
    {
        // apply available calendar if there is no timezone information
        if (cal == null || getPGType(i).endsWith("tz") )
            return getTime(i);
        java.util.Date tmp = getTime(i);
        if (tmp == null)
            return null;
        cal = org.postgresql.jdbc2.AbstractJdbc2Statement.changeTime(tmp, cal, false);
        return new java.sql.Time(cal.getTime().getTime());
    }


    public Timestamp getTimestamp(int i, java.util.Calendar cal) throws SQLException
    {
        // apply available calendar if there is no timezone information
        if (cal == null || getPGType(i).endsWith("tz") )
            return getTimestamp(i);
        java.util.Date tmp = getTimestamp(i);
        if (tmp == null)
            return null;
        cal = org.postgresql.jdbc2.AbstractJdbc2Statement.changeTime(tmp, cal, false);
        return new java.sql.Timestamp(cal.getTime().getTime());
    }


    public java.sql.Date getDate(String c, java.util.Calendar cal) throws SQLException
    {
        return getDate(findColumn(c), cal);
    }


    public Time getTime(String c, java.util.Calendar cal) throws SQLException
    {
        return getTime(findColumn(c), cal);
    }


    public Timestamp getTimestamp(String c, java.util.Calendar cal) throws SQLException
    {
        return getTimestamp(findColumn(c), cal);
    }


    public int getFetchDirection() throws SQLException
    {
        checkClosed();
        return fetchdirection;
    }


    public Object getObjectImpl(String columnName, java.util.Map map) throws SQLException
    {
        return getObjectImpl(findColumn(columnName), map);
    }


    /*
     * This checks against map for the type of column i, and if found returns
     * an object based on that mapping. The class must implement the SQLData
     * interface.
     */
    public Object getObjectImpl(int i, java.util.Map map) throws SQLException
    {
        checkClosed();
        throw org.postgresql.Driver.notImplemented();
    }


    public Ref getRef(String columnName) throws SQLException
    {
        return getRef(findColumn(columnName));
    }


    public Ref getRef(int i) throws SQLException
    {
        checkClosed();
        //The backend doesn't yet have SQL3 REF types
        throw org.postgresql.Driver.notImplemented();
    }


    public int getRow() throws SQLException
    {
        checkClosed();

        if (onInsertRow)
            return 0;

        final int rows_size = rows.size();

        if (current_row < 0 || current_row >= rows_size)
            return 0;

        return row_offset + current_row + 1;
    }


    // This one needs some thought, as not all ResultSets come from a statement
    public Statement getStatement() throws SQLException
    {
        checkClosed();
        return (Statement) statement;
    }


    public int getType() throws SQLException
    {
        checkClosed();
        return resultsettype;
    }


    public boolean isAfterLast() throws SQLException
    {
        checkClosed();
        if (onInsertRow)
            return false;

        final int rows_size = rows.size();
        return (current_row >= rows_size && rows_size > 0);
    }


    public boolean isBeforeFirst() throws SQLException
    {
        checkClosed();
        if (onInsertRow)
            return false;

        return ((row_offset + current_row) < 0 && rows.size() > 0);
    }


    public boolean isFirst() throws SQLException
    {
        checkClosed();
        if (onInsertRow)
            return false;

        return ((row_offset + current_row) == 0);
    }


    public boolean isLast() throws SQLException
    {
        checkClosed();
        if (onInsertRow)
            return false;

        final int rows_size = rows.size();

        if (rows_size == 0)
            return false; // No rows.

        if (current_row != (rows_size - 1))
            return false; // Not on the last row of this block.

        // We are on the last row of the current block.

        if (cursor == null)
        {
            // This is the last block and therefore the last row.
            return true;
        }

        if (maxRows > 0 && row_offset + current_row == maxRows)
        {
            // We are implicitly limited by maxRows.
            return true;
        }

        // Now the more painful case begins.
        // We are on the last row of the current block, but we don't know if the
        // current block is the last block; we must try to fetch some more data to
        // find out.

        // We do a fetch of the next block, then prepend the current row to that
        // block (so current_row == 0). This works as the current row
        // must be the last row of the current block if we got this far.

        row_offset += rows_size - 1; // Discarding all but one row.

        // Work out how many rows maxRows will let us fetch.
        int fetchRows = fetchSize;
        if (maxRows != 0)
        {
            if (fetchRows == 0 || row_offset + fetchRows > maxRows) // Fetch would exceed maxRows, limit it.
                fetchRows = maxRows - row_offset;
        }

        // Do the actual fetch.
        connection.getQueryExecutor().fetch(cursor, new CursorResultHandler(), fetchRows);

        // Now prepend our one saved row and move to it.
        rows.insertElementAt(this_row, 0);
        current_row = 0;

        // Finally, now we can tell if we're the last row or not.
        return (rows.size() == 1);
    }

    public boolean last() throws SQLException
    {
        checkScrollable();

        final int rows_size = rows.size();
        if (rows_size <= 0)
            return false;

        current_row = rows_size - 1;
        this_row = (byte[][]) rows.elementAt(current_row);

        rowBuffer = new byte[this_row.length][];
        System.arraycopy(this_row, 0, rowBuffer, 0, this_row.length);
        onInsertRow = false;

        return true;
    }


    public boolean previous() throws SQLException
    {
        checkScrollable();

        if (onInsertRow)
            throw new PSQLException(GT.tr("Can''t use relative move methods while on the insert row."),
                                    PSQLState.INVALID_CURSOR_STATE);

        if (current_row -1 < 0)
        {
            current_row = -1;
            this_row = null;
            rowBuffer = null;
            return false;
        }
        else
        {
            current_row--;
        }
        this_row = (byte[][]) rows.elementAt(current_row);
        rowBuffer = new byte[this_row.length][];
        System.arraycopy(this_row, 0, rowBuffer, 0, this_row.length);
        return true;
    }


    public boolean relative(int rows) throws SQLException
    {
        checkScrollable();

        if (onInsertRow)
            throw new PSQLException(GT.tr("Can''t use relative move methods while on the insert row."),
                                    PSQLState.INVALID_CURSOR_STATE);

        //have to add 1 since absolute expects a 1-based index
        return absolute(current_row + 1 + rows);
    }


    public void setFetchDirection(int direction) throws SQLException
    {
        checkClosed();
        switch (direction)
        {
        case ResultSet.FETCH_FORWARD:
            break;
        case ResultSet.FETCH_REVERSE:
        case ResultSet.FETCH_UNKNOWN:
            checkScrollable();
            break;
        default:
            throw new PSQLException(GT.tr("Invalid fetch direction constant: {0}.", new Integer(direction)),
                                    PSQLState.INVALID_PARAMETER_VALUE);
        }

        this.fetchdirection = direction;
    }


    public synchronized void cancelRowUpdates()
    throws SQLException
    {
        checkClosed();
        if (onInsertRow)
        {
            throw new PSQLException(GT.tr("Cannot call cancelRowUpdates() when on the insert row."),
                                    PSQLState.INVALID_CURSOR_STATE);
        }

        if (doingUpdates)
        {
            doingUpdates = false;

            clearRowBuffer(true);
        }
    }


    public synchronized void deleteRow()
    throws SQLException
    {
        checkUpdateable();

        if (onInsertRow)
        {
            throw new PSQLException(GT.tr("Cannot call deleteRow() when on the insert row."),
                                    PSQLState.INVALID_CURSOR_STATE);
        }

        if (isBeforeFirst())
        {
            throw new PSQLException(GT.tr("Currently positioned before the start of the ResultSet.  You cannot call deleteRow() here."),
                                    PSQLState.INVALID_CURSOR_STATE);
        }
        if (isAfterLast())
        {
            throw new PSQLException(GT.tr("Currently positioned after the end of the ResultSet.  You cannot call deleteRow() here."),
                                    PSQLState.INVALID_CURSOR_STATE);
        }
        if (rows.size() == 0)
        {
            throw new PSQLException(GT.tr("There are no rows in this ResultSet."),
                                    PSQLState.INVALID_CURSOR_STATE);
        }


        int numKeys = primaryKeys.size();
        if ( deleteStatement == null )
        {


            StringBuffer deleteSQL = new StringBuffer("DELETE FROM " ).append(tableName).append(" where " );

            for ( int i = 0; i < numKeys; i++ )
            {
                deleteSQL.append("\"");
                deleteSQL.append( ((PrimaryKey) primaryKeys.get(i)).name );
                deleteSQL.append("\" = ?");
                if ( i < numKeys - 1 )
                {
                    deleteSQL.append( " and " );
                }
            }

            deleteStatement = ((java.sql.Connection) connection).prepareStatement(deleteSQL.toString());
        }
        deleteStatement.clearParameters();

        for ( int i = 0; i < numKeys; i++ )
        {
            deleteStatement.setObject(i + 1, ((PrimaryKey) primaryKeys.get(i)).getValue());
        }


        deleteStatement.executeUpdate();

        rows.removeElementAt(current_row);
        current_row--;
        moveToCurrentRow();
    }


    public synchronized void insertRow()
    throws SQLException
    {
        checkUpdateable();

        if (!onInsertRow)
        {
            throw new PSQLException(GT.tr("Not on the insert row."), PSQLState.INVALID_CURSOR_STATE);
        }
        else if (updateValues.size() == 0)
        {
            throw new PSQLException(GT.tr("You must specify at least one column value to insert a row."),
                                    PSQLState.INVALID_PARAMETER_VALUE);
        }
        else
        {

            // loop through the keys in the insertTable and create the sql statement
            // we have to create the sql every time since the user could insert different
            // columns each time

            StringBuffer insertSQL = new StringBuffer("INSERT INTO ").append(tableName).append(" (");
            StringBuffer paramSQL = new StringBuffer(") values (" );

            Iterator columnNames = updateValues.keySet().iterator();
            int numColumns = updateValues.size();

            for ( int i = 0; columnNames.hasNext(); i++ )
            {
                String columnName = (String) columnNames.next();

                insertSQL.append("\"");
                insertSQL.append( columnName );
                insertSQL.append("\"");
                if ( i < numColumns - 1 )
                {
                    insertSQL.append(", ");
                    paramSQL.append("?,");
                }
                else
                {
                    paramSQL.append("?)");
                }

            }

            insertSQL.append(paramSQL.toString());
            insertStatement = ((java.sql.Connection) connection).prepareStatement(insertSQL.toString());

            Iterator keys = updateValues.keySet().iterator();

            for ( int i = 1; keys.hasNext(); i++)
            {
                String key = (String) keys.next();
                Object o = updateValues.get(key);
                insertStatement.setObject(i, o);
            }

            insertStatement.executeUpdate();

            if ( usingOID )
            {
                // we have to get the last inserted OID and put it in the resultset

                long insertedOID = ((AbstractJdbc2Statement) insertStatement).getLastOID();

                updateValues.put("oid", new Long(insertedOID) );

            }

            // update the underlying row to the new inserted data
            updateRowBuffer();

            rows.addElement(rowBuffer);

            // we should now reflect the current data in this_row
            // that way getXXX will get the newly inserted data
            this_row = rowBuffer;

            // need to clear this in case of another insert
            clearRowBuffer(false);


        }
    }


    public synchronized void moveToCurrentRow()
    throws SQLException
    {
        checkUpdateable();

        if (current_row < 0)
        {
            this_row = null;
            rowBuffer = null;
        }
        else
        {
            this_row = (byte[][]) rows.elementAt(current_row);

            rowBuffer = new byte[this_row.length][];
            System.arraycopy(this_row, 0, rowBuffer, 0, this_row.length);
        }

        onInsertRow = false;
        doingUpdates = false;
    }


    public synchronized void moveToInsertRow()
    throws SQLException
    {
        checkUpdateable();

        if (insertStatement != null)
        {
            insertStatement = null;
        }


        // make sure the underlying data is null
        clearRowBuffer(false);

        onInsertRow = true;
        doingUpdates = false;

    }


    private synchronized void clearRowBuffer(boolean copyCurrentRow)
    throws SQLException
    {
        // rowBuffer is the temporary storage for the row
        rowBuffer = new byte[fields.length][];

        // inserts want an empty array while updates want a copy of the current row
        if (copyCurrentRow)
        {
            System.arraycopy(this_row, 0, rowBuffer, 0, this_row.length);
        }

        // clear the updateValues hashTable for the next set of updates
        updateValues.clear();

    }


    public boolean rowDeleted() throws SQLException
    {
        checkClosed();
        return false;
    }


    public boolean rowInserted() throws SQLException
    {
        checkClosed();
        return false;
    }


    public boolean rowUpdated() throws SQLException
    {
        checkClosed();
        return false;
    }


    public synchronized void updateAsciiStream(int columnIndex,
            java.io.InputStream x,
            int length
                                              )
    throws SQLException
    {
        if (x == null)
        {
            updateNull(columnIndex);
            return ;
        }

        try
        {
            InputStreamReader reader = new InputStreamReader(x, "ASCII");
            char data[] = new char[length];
            int numRead = 0;
            while (true)
            {
                int n = reader.read(data, numRead, length - numRead);
                if (n == -1)
                    break;

                numRead += n;

                if (numRead == length)
                    break;
            }
            updateString(columnIndex, new String(data, 0, numRead));
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new PSQLException(GT.tr("The JVM claims not to support the encoding: {0}","ASCII"), PSQLState.UNEXPECTED_ERROR, uee);
        }
        catch (IOException ie)
        {
            throw new PSQLException(GT.tr("Provided InputStream failed."), null, ie);
        }
    }


    public synchronized void updateBigDecimal(int columnIndex,
            java.math.BigDecimal x )
    throws SQLException
    {
        updateValue(columnIndex, x);
    }


    public synchronized void updateBinaryStream(int columnIndex,
            java.io.InputStream x,
            int length
                                               )
    throws SQLException
    {
        if (x == null)
        {
            updateNull(columnIndex);
            return ;
        }

        byte data[] = new byte[length];
        int numRead = 0;
        try
        {
            while (true)
            {
                int n = x.read(data, numRead, length - numRead);
                if (n == -1)
                    break;

                numRead += n;

                if (numRead == length)
                    break;
            }
        }
        catch (IOException ie)
        {
            throw new PSQLException(GT.tr("Provided InputStream failed."), null, ie);
        }

        if (numRead == length)
        {
            updateBytes(columnIndex, data);
        }
        else
        {
            // the stream contained less data than they said
            // perhaps this is an error?
            byte data2[] = new byte[numRead];
            System.arraycopy(data, 0, data2, 0, numRead);
            updateBytes(columnIndex, data2);
        }
    }


    public synchronized void updateBoolean(int columnIndex, boolean x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("updating boolean " + fields[columnIndex - 1].getColumnName(connection) + "=" + x);
        updateValue(columnIndex, new Boolean(x));
    }


    public synchronized void updateByte(int columnIndex, byte x)
    throws SQLException
    {
        updateValue(columnIndex, String.valueOf(x));
    }


    public synchronized void updateBytes(int columnIndex, byte[] x)
    throws SQLException
    {
        updateValue(columnIndex, x);
    }


    public synchronized void updateCharacterStream(int columnIndex,
            java.io.Reader x,
            int length
                                                  )
    throws SQLException
    {
        if (x == null)
        {
            updateNull(columnIndex);
            return ;
        }

        try
        {
            char data[] = new char[length];
            int numRead = 0;
            while (true)
            {
                int n = x.read(data, numRead, length - numRead);
                if (n == -1)
                    break;

                numRead += n;

                if (numRead == length)
                    break;
            }
            updateString(columnIndex, new String(data, 0, numRead));
        }
        catch (IOException ie)
        {
            throw new PSQLException(GT.tr("Provided Reader failed."), null, ie);
        }
    }


    public synchronized void updateDate(int columnIndex, java.sql.Date x)
    throws SQLException
    {
        updateValue(columnIndex, x);
    }


    public synchronized void updateDouble(int columnIndex, double x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("updating double " + fields[columnIndex - 1].getColumnName(connection) + "=" + x);
        updateValue(columnIndex, new Double(x));
    }


    public synchronized void updateFloat(int columnIndex, float x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("updating float " + fields[columnIndex - 1].getColumnName(connection) + "=" + x);
        updateValue(columnIndex, new Float(x));
    }


    public synchronized void updateInt(int columnIndex, int x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("updating int " + fields[columnIndex - 1].getColumnName(connection) + "=" + x);
        updateValue(columnIndex, new Integer(x));
    }


    public synchronized void updateLong(int columnIndex, long x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("updating long " + fields[columnIndex - 1].getColumnName(connection) + "=" + x);
        updateValue(columnIndex, new Long(x));
    }


    public synchronized void updateNull(int columnIndex)
    throws SQLException
    {
        String columnTypeName = connection.getPGType(fields[columnIndex - 1].getOID());
        updateValue(columnIndex, new NullObject(columnTypeName));
    }


    public synchronized void updateObject(int columnIndex, Object x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("updating object " + fields[columnIndex - 1].getColumnName(connection) + " = " + x);
        updateValue(columnIndex, x);
    }


    public synchronized void updateObject(int columnIndex, Object x, int scale)
    throws SQLException
    {
        this.updateObject(columnIndex, x);

    }


    public void refreshRow() throws SQLException
    {
        checkUpdateable();
        if (onInsertRow)
            throw new PSQLException(GT.tr("Can''t refresh the insert row."),
                                    PSQLState.INVALID_CURSOR_STATE);

        if (isBeforeFirst() || isAfterLast() || rows.size() == 0)
            return ;

        StringBuffer selectSQL = new StringBuffer( "select ");

        final int numColumns = java.lang.reflect.Array.getLength(fields);

        for (int i = 0; i < numColumns; i++ )
        {
            selectSQL.append( fields[i].getColumnName(connection) );

            if ( i < numColumns - 1 )
            {

                selectSQL.append(", ");
            }

        }
        selectSQL.append(" from " ).append(tableName).append(" where ");

        int numKeys = primaryKeys.size();

        for ( int i = 0; i < numKeys; i++ )
        {

            PrimaryKey primaryKey = ((PrimaryKey) primaryKeys.get(i));
            selectSQL.append(primaryKey.name).append("= ?");

            if ( i < numKeys - 1 )
            {
                selectSQL.append(" and ");
            }
        }
        if ( Driver.logDebug )
            Driver.debug("selecting " + selectSQL.toString());
        selectStatement = ((java.sql.Connection) connection).prepareStatement(selectSQL.toString());


        for ( int j = 0, i = 1; j < numKeys; j++, i++)
        {
            selectStatement.setObject( i, ((PrimaryKey) primaryKeys.get(j)).getValue() );
        }

        AbstractJdbc2ResultSet rs = (AbstractJdbc2ResultSet) selectStatement.executeQuery();

        if ( rs.next() )
        {
            rowBuffer = rs.rowBuffer;
        }

        rows.setElementAt( rowBuffer, current_row );
        this_row = rowBuffer;
        if ( Driver.logDebug )
            Driver.debug("done updates");

        rs.close();
        selectStatement.close();
        selectStatement = null;

    }


    public synchronized void updateRow()
    throws SQLException
    {
        checkUpdateable();

        if (onInsertRow)
        {
            throw new PSQLException(GT.tr("Cannot call updateRow() when on the insert row."),
                                    PSQLState.INVALID_CURSOR_STATE);
        }

        if (isBeforeFirst() || isAfterLast() || rows.size() == 0)
        {
            throw new PSQLException(GT.tr("Cannot update the ResultSet because it is either before the start or after the end of the results."),
                                    PSQLState.INVALID_CURSOR_STATE);
        }

        if (doingUpdates)
        {

            try
            {

                StringBuffer updateSQL = new StringBuffer("UPDATE " + tableName + " SET  ");

                int numColumns = updateValues.size();
                Iterator columns = updateValues.keySet().iterator();

                for (int i = 0; columns.hasNext(); i++ )
                {

                    String column = (String) columns.next();
                    updateSQL.append("\"");
                    updateSQL.append( column );
                    updateSQL.append("\" = ?");

                    if ( i < numColumns - 1 )
                    {

                        updateSQL.append(", ");
                    }

                }
                updateSQL.append( " WHERE " );

                int numKeys = primaryKeys.size();

                for ( int i = 0; i < numKeys; i++ )
                {

                    PrimaryKey primaryKey = ((PrimaryKey) primaryKeys.get(i));
                    updateSQL.append("\"");
                    updateSQL.append(primaryKey.name);
                    updateSQL.append("\" = ?");

                    if ( i < numKeys - 1 )
                    {
                        updateSQL.append(" and ");
                    }
                }
                if ( Driver.logDebug )
                    Driver.debug("updating " + updateSQL.toString());
                updateStatement = ((java.sql.Connection) connection).prepareStatement(updateSQL.toString());

                int i = 0;
                Iterator iterator = updateValues.values().iterator();
                for (; iterator.hasNext(); i++)
                {
                    Object o = iterator.next();
                    updateStatement.setObject( i + 1, o );

                }
                for ( int j = 0; j < numKeys; j++, i++)
                {
                    updateStatement.setObject( i + 1, ((PrimaryKey) primaryKeys.get(j)).getValue() );
                }

                updateStatement.executeUpdate();
                updateStatement.close();

                updateStatement = null;
                updateRowBuffer();


                if ( Driver.logDebug )
                    Driver.debug("copying data");
                System.arraycopy(rowBuffer, 0, this_row, 0, rowBuffer.length);

                rows.setElementAt( rowBuffer, current_row );
                if ( Driver.logDebug )
                    Driver.debug("done updates");
                updateValues.clear();
                doingUpdates = false;
            }
            catch (SQLException e)
            {
                if ( Driver.logDebug )
                    Driver.debug(e.getClass().getName() + e);
                throw e;
            }

        }

    }


    public synchronized void updateShort(int columnIndex, short x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("in update Short " + fields[columnIndex - 1].getColumnName(connection) + " = " + x);
        updateValue(columnIndex, new Short(x));
    }


    public synchronized void updateString(int columnIndex, String x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("in update String " + fields[columnIndex - 1].getColumnName(connection) + " = " + x);
        updateValue(columnIndex, x);
    }


    public synchronized void updateTime(int columnIndex, Time x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("in update Time " + fields[columnIndex - 1].getColumnName(connection) + " = " + x);
        updateValue(columnIndex, x);
    }


    public synchronized void updateTimestamp(int columnIndex, Timestamp x)
    throws SQLException
    {
        if ( Driver.logDebug )
            Driver.debug("updating Timestamp " + fields[columnIndex - 1].getColumnName(connection) + " = " + x);
        updateValue(columnIndex, x);

    }


    public synchronized void updateNull(String columnName)
    throws SQLException
    {
        updateNull(findColumn(columnName));
    }


    public synchronized void updateBoolean(String columnName, boolean x)
    throws SQLException
    {
        updateBoolean(findColumn(columnName), x);
    }


    public synchronized void updateByte(String columnName, byte x)
    throws SQLException
    {
        updateByte(findColumn(columnName), x);
    }


    public synchronized void updateShort(String columnName, short x)
    throws SQLException
    {
        updateShort(findColumn(columnName), x);
    }


    public synchronized void updateInt(String columnName, int x)
    throws SQLException
    {
        updateInt(findColumn(columnName), x);
    }


    public synchronized void updateLong(String columnName, long x)
    throws SQLException
    {
        updateLong(findColumn(columnName), x);
    }


    public synchronized void updateFloat(String columnName, float x)
    throws SQLException
    {
        updateFloat(findColumn(columnName), x);
    }


    public synchronized void updateDouble(String columnName, double x)
    throws SQLException
    {
        updateDouble(findColumn(columnName), x);
    }


    public synchronized void updateBigDecimal(String columnName, BigDecimal x)
    throws SQLException
    {
        updateBigDecimal(findColumn(columnName), x);
    }


    public synchronized void updateString(String columnName, String x)
    throws SQLException
    {
        updateString(findColumn(columnName), x);
    }


    public synchronized void updateBytes(String columnName, byte x[])
    throws SQLException
    {
        updateBytes(findColumn(columnName), x);
    }


    public synchronized void updateDate(String columnName, java.sql.Date x)
    throws SQLException
    {
        updateDate(findColumn(columnName), x);
    }


    public synchronized void updateTime(String columnName, java.sql.Time x)
    throws SQLException
    {
        updateTime(findColumn(columnName), x);
    }


    public synchronized void updateTimestamp(String columnName, java.sql.Timestamp x)
    throws SQLException
    {
        updateTimestamp(findColumn(columnName), x);
    }


    public synchronized void updateAsciiStream(
        String columnName,
        java.io.InputStream x,
        int length)
    throws SQLException
    {
        updateAsciiStream(findColumn(columnName), x, length);
    }


    public synchronized void updateBinaryStream(
        String columnName,
        java.io.InputStream x,
        int length)
    throws SQLException
    {
        updateBinaryStream(findColumn(columnName), x, length);
    }


    public synchronized void updateCharacterStream(
        String columnName,
        java.io.Reader reader,
        int length)
    throws SQLException
    {
        updateCharacterStream(findColumn(columnName), reader, length);
    }


    public synchronized void updateObject(String columnName, Object x, int scale)
    throws SQLException
    {
        updateObject(findColumn(columnName), x);
    }


    public synchronized void updateObject(String columnName, Object x)
    throws SQLException
    {
        updateObject(findColumn(columnName), x);
    }


    /**
     * Is this ResultSet updateable?
     */

    boolean isUpdateable() throws SQLException
    {
        checkClosed();

        if (updateable)
            return true;

        if ( Driver.logDebug )
            Driver.debug("checking if rs is updateable");

        parseQuery();

        if ( singleTable == false )
        {
            if ( Driver.logDebug )
                Driver.debug("not a single table");
            return false;
        }

        if ( Driver.logDebug )
            Driver.debug("getting primary keys");

        //
        // Contains the primary key?
        //

        primaryKeys = new Vector();

        // this is not stricty jdbc spec, but it will make things much faster if used
        // the user has to select oid, * from table and then we will just use oid


        usingOID = false;
        int oidIndex = 0;
        try
        {
            oidIndex = findColumn( "oid" );
        }
        catch (SQLException l_se)
        {
            //Ignore if column oid isn't selected
        }
        int i = 0;


        // if we find the oid then just use it

        //oidIndex will be >0 if the oid was in the select list
        if ( oidIndex > 0 )
        {
            i++;
            primaryKeys.add( new PrimaryKey( oidIndex, "oid" ) );
            usingOID = true;
        }
        else
        {
            // otherwise go and get the primary keys and create a hashtable of keys
            String[] s = quotelessTableName(tableName);
            String quotelessTableName = s[0];
            String quotelessSchemaName = s[1];
            java.sql.ResultSet rs = ((java.sql.Connection) connection).getMetaData().getPrimaryKeys("", quotelessSchemaName, quotelessTableName);
            for (; rs.next(); i++ )
            {
                String columnName = rs.getString(4); // get the columnName
                int index = findColumn( columnName );

                if ( index > 0 )
                {
                    primaryKeys.add( new PrimaryKey(index, columnName ) ); // get the primary key information
                }
            }

            rs.close();
        }

        if ( Driver.logDebug )
            Driver.debug( "no of keys=" + i );

        if ( i < 1 )
        {
            throw new PSQLException(GT.tr("No primary key found for table {0}.", tableName),
                                    PSQLState.DATA_ERROR);
        }

        updateable = primaryKeys.size() > 0;

        if ( Driver.logDebug )
            Driver.debug( "checking primary key " + updateable );

        return updateable;
    }

    /** Cracks out the table name and schema (if it exists) from a fully
     * qualified table name.
     * @param fullname string that we are trying to crack. Test cases:<pre>
     * Table: table ()
     * "Table": Table ()
     * Schema.Table: table (schema)
     * "Schema"."Table": Table (Schema)
     * "Schema"."Dot.Table": Dot.Table (Schema)
     * Schema."Dot.Table": Dot.Table (schema)
     * </pre>
     * @return String array with element zero always being the tablename and
     * element 1 the schema name which may be a zero length string.
     */
    public static String[] quotelessTableName(String fullname) {
        StringBuffer buf = new StringBuffer(fullname);
        String[] parts = new String[] {null, ""};
        StringBuffer acc = new StringBuffer();
        boolean betweenQuotes = false;
        for (int i = 0; i < buf.length(); i++)
        {
            char c = buf.charAt(i);
            switch (c)
            {
            case '"':
                if ((i < buf.length() - 1) && (buf.charAt(i + 1) == '"'))
                {
                    // two consecutive quotes - keep one
                    i++;
                    acc.append(c); // keep the quote
                }
                else
                { // Discard it
                    betweenQuotes = !betweenQuotes;
                }
                break;
            case '.':
                if (betweenQuotes)
                {   // Keep it
                    acc.append(c);
                }
                else
                {    // Have schema name
                    parts[1] = acc.toString();
                    acc = new StringBuffer();
                }
                break;
            default:
                acc.append((betweenQuotes) ? c : Character.toLowerCase(c));
                break;
            }
        }
        // Always put table in slot 0
        parts[0] = acc.toString();
        return parts;
    }

    private void parseQuery()
    {
        String l_sql = originalQuery.toString(null);
        StringTokenizer st = new StringTokenizer(l_sql, " \r\t\n");
        boolean tableFound = false, tablesChecked = false;
        String name = "";

        singleTable = true;

        while ( !tableFound && !tablesChecked && st.hasMoreTokens() )
        {
            name = st.nextToken();
            if ( !tableFound )
            {
                if (name.toLowerCase().equals("from"))
                {
                    tableName = st.nextToken();
                    tableFound = true;
                }
            }
            else
            {
                tablesChecked = true;
                // if the very next token is , then there are multiple tables
                singleTable = !name.equalsIgnoreCase(",");
            }
        }
    }


    private void updateRowBuffer() throws SQLException
    {

        Iterator columns = updateValues.keySet().iterator();

        while ( columns.hasNext() )
        {
            String columnName = (String) columns.next();
            int columnIndex = findColumn( columnName ) - 1;

            Object valueObject = updateValues.get(columnName);
            if (valueObject instanceof NullObject)
            {
                rowBuffer[columnIndex] = null;
            }
            else
            {

                switch ( getSQLType(columnIndex + 1) )
                {

                case Types.DECIMAL:
                case Types.BIGINT:
                case Types.DOUBLE:
                case Types.BIT:
                case Types.VARCHAR:
                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                case Types.SMALLINT:
                case Types.FLOAT:
                case Types.INTEGER:
                case Types.CHAR:
                case Types.NUMERIC:
                case Types.REAL:
                case Types.TINYINT:
                case Types.OTHER:
                    rowBuffer[columnIndex] = connection.encodeString(String.valueOf( valueObject));
                    break;

                case Types.NULL:
                    // Should never happen?
                    break;

                default:
                    rowBuffer[columnIndex] = (byte[]) valueObject;
                }

            }
        }
    }

    public class CursorResultHandler implements ResultHandler {
        private SQLException error;

        public void handleResultRows(Query fromQuery, Field[] fields, Vector tuples, ResultCursor cursor) {
            AbstractJdbc2ResultSet.this.rows = tuples;
            AbstractJdbc2ResultSet.this.cursor = cursor;
        }

        public void handleCommandStatus(String status, int updateCount, long insertOID) {
            handleError(new PSQLException(GT.tr("Unexpected command status: {0}.", status),
                                          PSQLState.PROTOCOL_VIOLATION));
        }

        public void handleWarning(SQLWarning warning) {
            AbstractJdbc2ResultSet.this.addWarning(warning);
        }

        public void handleError(SQLException newError) {
            if (error == null)
                error = newError;
            else
                error.setNextException(newError);
        }

        public void handleCompletion() throws SQLException {
            if (error != null)
                throw error;
        }
    };


    public BaseStatement getPGStatement() {
        return statement;
    }

    //
    // Backwards compatibility with PGRefCursorResultSet
    //

    private String refCursorName;

    public String getRefCursor() {
        // Can't check this because the PGRefCursorResultSet
	// interface doesn't allow throwing a SQLException
        //
        // checkClosed();
        return refCursorName;
    }

    private void setRefCursor(String refCursorName) {
        this.refCursorName = refCursorName;
    }

    public void setFetchSize(int rows) throws SQLException
    {
        checkClosed();
        if (rows < 0)
            throw new PSQLException(GT.tr("Fetch size must be a value greater to or equal to 0."),
                                    PSQLState.INVALID_PARAMETER_VALUE);
        fetchSize = rows;
    }

    public int getFetchSize() throws SQLException
    {
        checkClosed();
        return fetchSize;
    }

    public boolean next() throws SQLException
    {
        checkClosed();

        if (onInsertRow)
            throw new PSQLException(GT.tr("Can''t use relative move methods while on the insert row."),
                                    PSQLState.INVALID_CURSOR_STATE);

        if (current_row + 1 >= rows.size())
        {
            if (cursor == null || (maxRows > 0 && row_offset + rows.size() >= maxRows))
            {
                current_row = rows.size();
                this_row = null;
                rowBuffer = null;
                return false;  // End of the resultset.
            }

            // Ask for some more data.
            row_offset += rows.size(); // We are discarding some data.

            int fetchRows = fetchSize;
            if (maxRows != 0)
            {
                if (fetchRows == 0 || row_offset + fetchRows > maxRows) // Fetch would exceed maxRows, limit it.
                    fetchRows = maxRows - row_offset;
            }

            // Execute the fetch and update this resultset.
            connection.getQueryExecutor().fetch(cursor, new CursorResultHandler(), fetchRows);

            current_row = 0;

            // Test the new rows array.
            if (rows.size() == 0)
            {
                this_row = null;
                rowBuffer = null;
                return false;
            }
        }
        else
        {
            current_row++;
        }

        this_row = (byte [][])rows.elementAt(current_row);

        rowBuffer = new byte[this_row.length][];
        System.arraycopy(this_row, 0, rowBuffer, 0, this_row.length);
        return true;
    }

    public void close() throws SQLException
    {
        //release resources held (memory for tuples)
	rows = null;
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    public boolean wasNull() throws SQLException
    {
        checkClosed();
        return wasNullFlag;
    }

    public String getString(int columnIndex) throws SQLException
    {
        checkResultSet( columnIndex );
        wasNullFlag = (this_row[columnIndex - 1] == null);
        if (wasNullFlag)
            return null;

        Encoding encoding = connection.getEncoding();
        try
        {
            return trimString(columnIndex, encoding.decode(this_row[columnIndex - 1]));
        }
        catch (IOException ioe)
        {
            throw new PSQLException(GT.tr("Invalid character data was found.  This is most likely caused by stored data containing characters that are invalid for the character set the database was created in.  The most common example of this is storing 8bit data in a SQL_ASCII database."), PSQLState.DATA_ERROR, ioe);
        }
    }

    public boolean getBoolean(int columnIndex) throws SQLException
    {
        return toBoolean( getString(columnIndex) );
    }

    private static final BigInteger BYTEMAX = new BigInteger(Byte.toString(Byte.MAX_VALUE));
    private static final BigInteger BYTEMIN = new BigInteger(Byte.toString(Byte.MIN_VALUE));

    public byte getByte(int columnIndex) throws SQLException
    {
        String s = getString(columnIndex);

        if (s != null )
        {
            s = s.trim();
            if ( s.length() == 0 )
                return 0;
            try
            {
                // try the optimal parse
                return Byte.parseByte(s);
            }
            catch (NumberFormatException e)
            {
                // didn't work, assume the column is not a byte
                try
                {
                    BigDecimal n = new BigDecimal(s);
                    BigInteger i = n.toBigInteger();

                    int gt = i.compareTo(BYTEMAX);
                    int lt = i.compareTo(BYTEMIN);

                    if ( gt > 0 || lt < 0 )
                    {
                        throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"byte",s}),
                                                PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
                    }
                    return i.byteValue();
                }
                catch ( NumberFormatException ex )
                {
                    throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"byte",s}),
                                            PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
                }
            }
        }
        return 0; // SQL NULL
    }

    private static final BigInteger SHORTMAX = new BigInteger(Short.toString(Short.MAX_VALUE));
    private static final BigInteger SHORTMIN = new BigInteger(Short.toString(Short.MIN_VALUE));

    public short getShort(int columnIndex) throws SQLException
    {
        String s = getFixedString(columnIndex);

        if (s != null)
        {
            s = s.trim();
            try
            {
                return Short.parseShort(s);
            }
            catch (NumberFormatException e)
            {
                try
                {
                    BigDecimal n = new BigDecimal(s);
                    BigInteger i = n.toBigInteger();
                    int gt = i.compareTo(SHORTMAX);
                    int lt = i.compareTo(SHORTMIN);

                    if ( gt > 0 || lt < 0 )
                    {
                        throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"short",s}),
                                                PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
                    }
                    return i.shortValue();

                }
                catch ( NumberFormatException ne )
                {
                    throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"short",s}),
                                            PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
                }
            }
        }
        return 0; // SQL NULL
    }

    public int getInt(int columnIndex) throws SQLException
    {
        return toInt( getFixedString(columnIndex) );
    }

    public long getLong(int columnIndex) throws SQLException
    {
        return toLong( getFixedString(columnIndex) );
    }

    public float getFloat(int columnIndex) throws SQLException
    {
        return toFloat( getFixedString(columnIndex) );
    }

    public double getDouble(int columnIndex) throws SQLException
    {
        return toDouble( getFixedString(columnIndex) );
    }

    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException
    {
        return toBigDecimal( getFixedString(columnIndex), scale );
    }

    /*
     * Get the value of a column in the current row as a Java byte array.
     *
     * <p>In normal use, the bytes represent the raw values returned by the
     * backend. However, if the column is an OID, then it is assumed to
     * refer to a Large Object, and that object is returned as a byte array.
     *
     * <p><b>Be warned</b> If the large object is huge, then you may run out
     * of memory.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result
     * is null
     * @exception SQLException if a database access error occurs
     */
    public byte[] getBytes(int columnIndex) throws SQLException
    {
        checkResultSet( columnIndex );
        wasNullFlag = (this_row[columnIndex - 1] == null);
        if (!wasNullFlag)
        {
            if (fields[columnIndex - 1].getFormat() == Field.BINARY_FORMAT)
            {
                //If the data is already binary then just return it
                return this_row[columnIndex - 1];
            }
            else if (connection.haveMinimumCompatibleVersion("7.2"))
            {
                //Version 7.2 supports the bytea datatype for byte arrays
                if (fields[columnIndex - 1].getOID() == Oid.BYTEA)
                {
                    return trimBytes(columnIndex, PGbytea.toBytes(this_row[columnIndex - 1]));
                }
                else
                {
                    return trimBytes(columnIndex, this_row[columnIndex - 1]);
                }
            }
            else
            {
                //Version 7.1 and earlier supports LargeObjects for byte arrays
                // Handle OID's as BLOBS
                if ( fields[columnIndex - 1].getOID() == 26)
                {
                    LargeObjectManager lom = connection.getLargeObjectAPI();
                    LargeObject lob = lom.open(getInt(columnIndex));
                    byte buf[] = lob.read(lob.size());
                    lob.close();
                    return trimBytes(columnIndex, buf);
                }
                else
                {
                    return trimBytes(columnIndex, this_row[columnIndex - 1]);
                }
            }
        }
        return null;
    }

    public java.sql.Date getDate(int columnIndex) throws SQLException
    {
        checkResultSet(columnIndex);
        if (calendar == null)
            calendar = new GregorianCalendar();
        return TimestampUtils.toDate(calendar, getString(columnIndex));
    }

    public Time getTime(int columnIndex) throws SQLException
    {
        checkResultSet(columnIndex);
        if (calendar == null)
            calendar = new GregorianCalendar();
        return TimestampUtils.toTime(calendar, getString(columnIndex));
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException
    {
        this.checkResultSet(columnIndex);
        if (calendar == null)
            calendar = new GregorianCalendar();
        return TimestampUtils.toTimestamp(calendar, getString(columnIndex));
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException
    {
        checkResultSet( columnIndex );
        wasNullFlag = (this_row[columnIndex - 1] == null);
        if (wasNullFlag)
            return null;

        if (connection.haveMinimumCompatibleVersion("7.2"))
        {
            //Version 7.2 supports AsciiStream for all the PG text types
            //As the spec/javadoc for this method indicate this is to be used for
            //large text values (i.e. LONGVARCHAR) PG doesn't have a separate
            //long string datatype, but with toast the text datatype is capable of
            //handling very large values.  Thus the implementation ends up calling
            //getString() since there is no current way to stream the value from the server
            try
            {
                return new ByteArrayInputStream(getString(columnIndex).getBytes("ASCII"));
            }
            catch (UnsupportedEncodingException l_uee)
            {
                throw new PSQLException(GT.tr("The JVM claims not to support the encoding: {0}","ASCII"), PSQLState.UNEXPECTED_ERROR, l_uee);
            }
        }
        else
        {
            // In 7.1 Handle as BLOBS so return the LargeObject input stream
            return getBinaryStream(columnIndex);
        }
    }

    public InputStream getUnicodeStream(int columnIndex) throws SQLException
    {
        checkResultSet( columnIndex );
        wasNullFlag = (this_row[columnIndex - 1] == null);
        if (wasNullFlag)
            return null;

        if (connection.haveMinimumCompatibleVersion("7.2"))
        {
            //Version 7.2 supports AsciiStream for all the PG text types
            //As the spec/javadoc for this method indicate this is to be used for
            //large text values (i.e. LONGVARCHAR) PG doesn't have a separate
            //long string datatype, but with toast the text datatype is capable of
            //handling very large values.  Thus the implementation ends up calling
            //getString() since there is no current way to stream the value from the server
            try
            {
                return new ByteArrayInputStream(getString(columnIndex).getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException l_uee)
            {
                throw new PSQLException(GT.tr("The JVM claims not to support the encoding: {0}","UTF-8"), PSQLState.UNEXPECTED_ERROR, l_uee);
            }
        }
        else
        {
            // In 7.1 Handle as BLOBS so return the LargeObject input stream
            return getBinaryStream(columnIndex);
        }
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException
    {
        checkResultSet( columnIndex );
        wasNullFlag = (this_row[columnIndex - 1] == null);
        if (wasNullFlag)
            return null;

        if (connection.haveMinimumCompatibleVersion("7.2"))
        {
            //Version 7.2 supports BinaryStream for all PG bytea type
            //As the spec/javadoc for this method indicate this is to be used for
            //large binary values (i.e. LONGVARBINARY) PG doesn't have a separate
            //long binary datatype, but with toast the bytea datatype is capable of
            //handling very large values.  Thus the implementation ends up calling
            //getBytes() since there is no current way to stream the value from the server
            byte b[] = getBytes(columnIndex);
            if (b != null)
                return new ByteArrayInputStream(b);
        }
        else
        {
            // In 7.1 Handle as BLOBS so return the LargeObject input stream
            if ( fields[columnIndex - 1].getOID() == 26)
            {
                LargeObjectManager lom = connection.getLargeObjectAPI();
                LargeObject lob = lom.open(getInt(columnIndex));
                return lob.getInputStream();
            }
        }
        return null;
    }

    public String getString(String columnName) throws SQLException
    {
        return getString(findColumn(columnName));
    }

    public boolean getBoolean(String columnName) throws SQLException
    {
        return getBoolean(findColumn(columnName));
    }

    public byte getByte(String columnName) throws SQLException
    {

        return getByte(findColumn(columnName));
    }

    public short getShort(String columnName) throws SQLException
    {
        return getShort(findColumn(columnName));
    }

    public int getInt(String columnName) throws SQLException
    {
        return getInt(findColumn(columnName));
    }

    public long getLong(String columnName) throws SQLException
    {
        return getLong(findColumn(columnName));
    }

    public float getFloat(String columnName) throws SQLException
    {
        return getFloat(findColumn(columnName));
    }

    public double getDouble(String columnName) throws SQLException
    {
        return getDouble(findColumn(columnName));
    }

    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException
    {
        return getBigDecimal(findColumn(columnName), scale);
    }

    public byte[] getBytes(String columnName) throws SQLException
    {
        return getBytes(findColumn(columnName));
    }

    public java.sql.Date getDate(String columnName) throws SQLException
    {
        return getDate(findColumn(columnName));
    }

    public Time getTime(String columnName) throws SQLException
    {
        return getTime(findColumn(columnName));
    }

    public Timestamp getTimestamp(String columnName) throws SQLException
    {
        return getTimestamp(findColumn(columnName));
    }

    public InputStream getAsciiStream(String columnName) throws SQLException
    {
        return getAsciiStream(findColumn(columnName));
    }

    public InputStream getUnicodeStream(String columnName) throws SQLException
    {
        return getUnicodeStream(findColumn(columnName));
    }

    public InputStream getBinaryStream(String columnName) throws SQLException
    {
        return getBinaryStream(findColumn(columnName));
    }

    public SQLWarning getWarnings() throws SQLException
    {
        checkClosed();
        return warnings;
    }

    public void clearWarnings() throws SQLException
    {
        checkClosed();
        warnings = null;
    }

    protected void addWarning(SQLWarning warnings)
    {
        if (this.warnings != null)
            this.warnings.setNextWarning(warnings);
        else
            this.warnings = warnings;
    }

    public String getCursorName() throws SQLException
    {
        checkClosed();
        return null;
    }

    /*
     * Get the value of a column in the current row as a Java object
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java Object type corresponding to the column's SQL type, following
     * the mapping specified in the JDBC specification.
     *
     * <p>This method may also be used to read database specific abstract
     * data types.
     *
     * @param columnIndex the first column is 1, the second is 2...
     * @return a Object holding the column value
     * @exception SQLException if a database access error occurs
     */
    public Object getObject(int columnIndex) throws SQLException {
        Field field;

        checkResultSet(columnIndex);

        wasNullFlag = (this_row[columnIndex - 1] == null);
        if (wasNullFlag)
            return null;

        field = fields[columnIndex - 1];

        // some fields can be null, mainly from those returned by MetaData methods
        if (field == null)
        {
            wasNullFlag = true;
            return null;
        }

        Object result = internalGetObject(columnIndex, field);
        if (result != null)
            return result;

        return connection.getObject(getPGType(columnIndex), getString(columnIndex));
    }

    public Object getObject(String columnName) throws SQLException
    {
        return getObject(findColumn(columnName));
    }

    /*
     * Map a ResultSet column name to a ResultSet column index
     */
    public int findColumn(String columnName) throws SQLException
    {
        checkClosed();
        if (columnNameIndexMap == null)
        {
            columnNameIndexMap = new HashMap(fields.length * 2);
            for (int i = 0; i < fields.length; i++)
            {
                columnNameIndexMap.put(fields[i].getColumnLabel().toLowerCase(), new Integer(i + 1));
            }
        }

        Integer index = (Integer)columnNameIndexMap.get(columnName);
        if (index != null)
        {
            return index.intValue();
        }

        index = (Integer)columnNameIndexMap.get(columnName.toLowerCase());
        if (index != null)
        {
            columnNameIndexMap.put(columnName, index);
            return index.intValue();
        }

        throw new PSQLException (GT.tr("The column name {0} was not found in this ResultSet.", columnName),
                                 PSQLState.UNDEFINED_COLUMN);
    }

    /*
     * returns the OID of a field.<p>
     * It is used internally by the driver.
     */
    public int getColumnOID(int field)
    {
        return fields[field -1].getOID();
    }

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

        // if we don't have at least 2 characters it can't be money.
        if (s.length() < 2)
            return s;

        // Handle Money
        if (s.charAt(0) == '(')
        {
            s = "-" + PGtokenizer.removePara(s).substring(1);
        }
        if (s.charAt(0) == '$')
        {
            s = s.substring(1);
        }
        else if (s.charAt(0) == '-' && s.charAt(1) == '$')
        {
            s = "-" + s.substring(2);
        }

        return s;
    }

    protected String getPGType( int column ) throws SQLException
    {
        return connection.getPGType(fields[column - 1].getOID());
    }

    protected int getSQLType( int column ) throws SQLException
    {
        return connection.getSQLType(fields[column - 1].getOID());
    }

    private void checkUpdateable() throws SQLException
    {
        checkClosed();

        if (!isUpdateable())
            throw new PSQLException(GT.tr("ResultSet is not updateable.  The query that generated this result set must select only one table, and must select all primary keys from that table. See the JDBC 2.1 API Specification, section 5.6 for more details."),
                                    PSQLState.INVALID_CURSOR_STATE);

        if (updateValues == null)
        {
            // allow every column to be updated without a rehash.
            updateValues = new HashMap((int)(fields.length / 0.75), 0.75f);
        }
    }

    protected void checkClosed() throws SQLException {
        if (rows == null)
            throw new PSQLException(GT.tr("This ResultSet is closed."), PSQLState.CONNECTION_DOES_NOT_EXIST);
    }

    protected void checkResultSet( int column ) throws SQLException
    {
        checkClosed();
        if ( this_row == null )
            throw new PSQLException(GT.tr("ResultSet not positioned properly, perhaps you need to call next."),
                                    PSQLState.INVALID_CURSOR_STATE);
        if ( column < 1 || column > fields.length )
            throw new PSQLException(GT.tr("The column index is out of range: {0}, number of columns: {1}.", new Object[]{new Integer(column), new Integer(fields.length)}), PSQLState.INVALID_PARAMETER_VALUE );
    }

    //----------------- Formatting Methods -------------------

    public static boolean toBoolean(String s)
    {
        if (s != null)
        {
            s = s.trim();

            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t"))
                return true;

            try
            {
                if (Double.valueOf(s).doubleValue() == 1)
                    return true;
            }
            catch (NumberFormatException e)
            {
            }
        }
        return false;  // SQL NULL
    }

    private static final BigInteger INTMAX = new BigInteger(Integer.toString(Integer.MAX_VALUE));
    private static final BigInteger INTMIN = new BigInteger(Integer.toString(Integer.MIN_VALUE));

    public static int toInt(String s) throws SQLException
    {
        if (s != null)
        {
            try
            {
                s = s.trim();
                return Integer.parseInt(s);
            }
            catch (NumberFormatException e)
            {
                try
                {
                    BigDecimal n = new BigDecimal(s);
                    BigInteger i = n.toBigInteger();

                    int gt = i.compareTo(INTMAX);
                    int lt = i.compareTo(INTMIN);

                    if (gt > 0 || lt < 0)
                    {
                        throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"int",s}),
                                                PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
                    }
                    return i.intValue();

                }
                catch ( NumberFormatException ne )
                {
                    throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"int",s}),
                                            PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
                }
            }
        }
        return 0;  // SQL NULL
    }
    private final static BigInteger LONGMAX = new BigInteger(Long.toString(Long.MAX_VALUE));
    private final static BigInteger LONGMIN = new BigInteger(Long.toString(Long.MIN_VALUE));

    public static long toLong(String s) throws SQLException
    {
        if (s != null)
        {
            try
            {
                s = s.trim();
                return Long.parseLong(s);
            }
            catch (NumberFormatException e)
            {
                try
                {
                    BigDecimal n = new BigDecimal(s);
                    BigInteger i = n.toBigInteger();
                    int gt = i.compareTo(LONGMAX);
                    int lt = i.compareTo(LONGMIN);

                    if ( gt > 0 || lt < 0 )
                    {
                        throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"long",s}),
                                                PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
                    }
                    return i.longValue();
                }
                catch ( NumberFormatException ne )
                {
                    throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"long",s}),
                                            PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
                }
            }
        }
        return 0;  // SQL NULL
    }

    public static BigDecimal toBigDecimal(String s, int scale) throws SQLException
    {
        BigDecimal val;
        if (s != null)
        {
            try
            {
                s = s.trim();
                val = new BigDecimal(s);
            }
            catch (NumberFormatException e)
            {
                throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"BigDecimal",s}),
                                        PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
            }
            if (scale == -1)
                return val;
            try
            {
                return val.setScale(scale);
            }
            catch (ArithmeticException e)
            {
                throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"BigDecimal",s}),
                                        PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
            }
        }
        return null;  // SQL NULL
    }

    public static float toFloat(String s) throws SQLException
    {
        if (s != null)
        {
            try
            {
                s = s.trim();
                return Float.valueOf(s).floatValue();
            }
            catch (NumberFormatException e)
            {
                throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"float",s}),
                                        PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
            }
        }
        return 0;  // SQL NULL
    }

    public static double toDouble(String s) throws SQLException
    {
        if (s != null)
        {
            try
            {
                s = s.trim();
                return Double.valueOf(s).doubleValue();
            }
            catch (NumberFormatException e)
            {
                throw new PSQLException(GT.tr("Bad value for type {0} : {1}", new Object[]{"double",s}),
                                        PSQLState.NUMERIC_VALUE_OUT_OF_RANGE);
            }
        }
        return 0;  // SQL NULL
    }

    private boolean isColumnTrimmable(int columnIndex) throws SQLException
    {
        switch (getSQLType(columnIndex))
        {
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
            return true;
        }
        return false;
    }

    private byte[] trimBytes(int p_columnIndex, byte[] p_bytes) throws SQLException
    {
        //we need to trim if maxsize is set and the length is greater than maxsize and the
        //type of this column is a candidate for trimming
        if (maxFieldSize > 0 && p_bytes.length > maxFieldSize && isColumnTrimmable(p_columnIndex))
        {
            byte[] l_bytes = new byte[maxFieldSize];
            System.arraycopy (p_bytes, 0, l_bytes, 0, maxFieldSize);
            return l_bytes;
        }
        else
        {
            return p_bytes;
        }
    }

    private String trimString(int p_columnIndex, String p_string) throws SQLException
    {
        //we need to trim if maxsize is set and the length is greater than maxsize and the
        //type of this column is a candidate for trimming
        if (maxFieldSize > 0 && p_string.length() > maxFieldSize && isColumnTrimmable(p_columnIndex))
        {
            return p_string.substring(0, maxFieldSize);
        }
        else
        {
            return p_string;
        }
    }

    protected void updateValue(int columnIndex, Object value) throws SQLException {
        checkUpdateable();

        if (!onInsertRow && (isBeforeFirst() || isAfterLast() || rows.size() == 0))
        {
            throw new PSQLException(GT.tr("Cannot update the ResultSet because it is either before the start or after the end of the results."),
                                    PSQLState.INVALID_CURSOR_STATE);
        }
        doingUpdates = !onInsertRow;
        if (value == null)
            updateNull(columnIndex);
        else
            updateValues.put(fields[columnIndex - 1].getColumnName(connection), value);
    }

    private class PrimaryKey
    {
        int index;    // where in the result set is this primaryKey
        String name;   // what is the columnName of this primary Key

        PrimaryKey( int index, String name)
        {
            this.index = index;
            this.name = name;
        }
        Object getValue() throws SQLException
        {
            return getObject(index);
        }
    };

    //
    // We need to specify the type of NULL when updating a column to NULL, so
    // NullObject is a simple extension of PGobject that always returns null
    // values but retains column type info.
    //

    class NullObject extends PGobject {
        NullObject(String type) {
            setType(type);
        }

        public String getValue() {
            return null;
        }
    };
}

