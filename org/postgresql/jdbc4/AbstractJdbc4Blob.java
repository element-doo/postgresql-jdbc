/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/AbstractJdbc4Blob.java,v 1.6 2008/01/08 06:56:30 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;


import java.sql.*;

public abstract class AbstractJdbc4Blob extends org.postgresql.jdbc3.AbstractJdbc3Blob
{

    public AbstractJdbc4Blob(org.postgresql.core.BaseConnection conn, long oid) throws SQLException
    {
        super(conn, oid);
    }

    public synchronized java.io.InputStream getBinaryStream(long pos, long length) throws SQLException
    {
        checkFreed();
        throw org.postgresql.Driver.notImplemented(this.getClass(), "getBinaryStream(long, long)");
    }

}

