/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3g/Jdbc3gStatement.java,v 1.7 2008/09/30 04:34:51 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3g;

import java.sql.*;
import java.util.Vector;
import org.postgresql.core.*;

/**
 * This class implements the java.sql.Statement interface for JDBC3.
 * However most of the implementation is really done in
 * org.postgresql.jdbc3.AbstractJdbc3Statement or one of it's parents
 */
class Jdbc3gStatement extends org.postgresql.jdbc3g.AbstractJdbc3gStatement implements Statement
{
    Jdbc3gStatement (Jdbc3gConnection c, int rsType, int rsConcurrency, int rsHoldability) throws SQLException
    {
        super(c, rsType, rsConcurrency, rsHoldability);
    }

    protected Jdbc3gStatement(Jdbc3gConnection connection, String sql, boolean isCallable, int rsType, int rsConcurrency, int rsHoldability) throws SQLException
    {
        super(connection, sql, isCallable, rsType, rsConcurrency, rsHoldability);
    }

    public ResultSet createResultSet (Query originalQuery, Field[] fields, Vector tuples, ResultCursor cursor)
    throws SQLException
    {
        Jdbc3gResultSet newResult = new Jdbc3gResultSet(originalQuery, this, fields, tuples, cursor,
                                    getMaxRows(), getMaxFieldSize(),
                                    getResultSetType(), getResultSetConcurrency(), getResultSetHoldability());
        newResult.setFetchSize(getFetchSize());
        newResult.setFetchDirection(getFetchDirection());
        return newResult;
    }

    public ParameterMetaData createParameterMetaData(BaseConnection conn, int oids[]) throws SQLException
    {
        return new Jdbc3gParameterMetaData(conn, oids);
    }

}
