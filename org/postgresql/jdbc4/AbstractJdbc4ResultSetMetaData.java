/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/AbstractJdbc4ResultSetMetaData.java,v 1.4 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;

import org.postgresql.core.*;

import java.sql.SQLException;

abstract class AbstractJdbc4ResultSetMetaData extends org.postgresql.jdbc2.AbstractJdbc2ResultSetMetaData
{

    public AbstractJdbc4ResultSetMetaData(BaseConnection connection, Field[] fields)
    {
        super(connection, fields);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        throw org.postgresql.Driver.notImplemented(this.getClass(), "isWrapperFor(Class<?>)");
    }

    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        throw org.postgresql.Driver.notImplemented(this.getClass(), "unwrap(Class<T>)");
    }

}


