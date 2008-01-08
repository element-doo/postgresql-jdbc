/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/Jdbc4Statement.java,v 1.2 2008/01/08 06:56:30 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;

import java.sql.*;
import java.util.Vector;
import org.postgresql.core.*;

/**
 * This class implements the java.sql.Statement interface for JDBC4.
 * However most of the implementation is really done in
 * org.postgresql.jdbc4.AbstractJdbc4Statement or one of it's parents
 */
class Jdbc4Statement extends AbstractJdbc4Statement implements Statement
{
    Jdbc4Statement (Jdbc4Connection c, int rsType, int rsConcurrency, int rsHoldability) throws SQLException
    {
        super(c, rsType, rsConcurrency, rsHoldability);
    }

    protected Jdbc4Statement(Jdbc4Connection connection, String sql, boolean isCallable, int rsType, int rsConcurrency, int rsHoldability) throws SQLException
    {
        super(connection, sql, isCallable, rsType, rsConcurrency, rsHoldability);
    }

    public ResultSet createResultSet (Query originalQuery, Field[] fields, Vector tuples, ResultCursor cursor)
    throws SQLException
    {
        Jdbc4ResultSet newResult = new Jdbc4ResultSet(originalQuery, this, fields, tuples, cursor,
                                    getMaxRows(), getMaxFieldSize(),
                                    getResultSetType(), getResultSetConcurrency(), getResultSetHoldability());
        newResult.setFetchSize(getFetchSize());
        newResult.setFetchDirection(getFetchDirection());
        return newResult;
    }

    public ParameterMetaData createParameterMetaData(BaseConnection conn, int oids[]) throws SQLException
    {
        return new Jdbc4ParameterMetaData(conn, oids);
    }

}
