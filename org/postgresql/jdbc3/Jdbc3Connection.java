/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3/Jdbc3Connection.java,v 1.13 2009/11/18 11:19:31 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3;

import java.util.Map;
import java.util.Properties;
import java.sql.SQLException;

/**
 * This class implements the java.sql.Connection interface for JDBC3.
 * However most of the implementation is really done in
 * org.postgresql.jdbc3.AbstractJdbc3Connection or one of it's parents
 */
public class Jdbc3Connection extends org.postgresql.jdbc3.AbstractJdbc3Connection implements java.sql.Connection
{
    public Jdbc3Connection(String host, int port, String user, String database, Properties info, String url) throws SQLException {
        super(host, port, user, database, info, url);
    }

    public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        Jdbc3Statement s = new Jdbc3Statement(this, resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setPrepareThreshold(getPrepareThreshold());
        return s;
    }


    public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        Jdbc3PreparedStatement s = new Jdbc3PreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setPrepareThreshold(getPrepareThreshold());
        return s;
    }

    public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
    {
        checkClosed();
        Jdbc3CallableStatement s = new Jdbc3CallableStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setPrepareThreshold(getPrepareThreshold());
        return s;
    }

    public java.sql.DatabaseMetaData getMetaData() throws SQLException
    {
        checkClosed();
        if (metadata == null)
            metadata = new Jdbc3DatabaseMetaData(this);
        return metadata;
    }

    public void setTypeMap(Map map) throws SQLException
    {
        setTypeMapImpl(map);
    }

}
