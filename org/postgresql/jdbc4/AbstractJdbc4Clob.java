/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/AbstractJdbc4Clob.java,v 1.7 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;

import java.io.Reader;
import java.sql.SQLException;

public abstract class AbstractJdbc4Clob extends org.postgresql.jdbc3.AbstractJdbc3Clob
{

    public AbstractJdbc4Clob(org.postgresql.core.BaseConnection conn, long oid) throws SQLException
    {
        super(conn, oid);
    }

    public synchronized Reader getCharacterStream(long pos, long length) throws SQLException
    {
        checkFreed();
        throw org.postgresql.Driver.notImplemented(this.getClass(), "getCharacterStream(long, long)");
    }

}
