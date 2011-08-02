/*-------------------------------------------------------------------------
*
* Copyright (c) 2005-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/AbstractJdbc4ParameterMetaData.java,v 1.5 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;

import java.sql.SQLException;
import org.postgresql.core.BaseConnection;

public abstract class AbstractJdbc4ParameterMetaData extends org.postgresql.jdbc3.AbstractJdbc3ParameterMetaData
{

    public AbstractJdbc4ParameterMetaData(BaseConnection connection, int oids[])
    {
        super(connection, oids);
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

