/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc2/AbstractJdbc2Blob.java,v 1.15 2011/08/02 13:48:35 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc2;

import java.sql.SQLException;

import org.postgresql.core.BaseConnection;

public abstract class AbstractJdbc2Blob extends AbstractJdbc2BlobClob
{

    public AbstractJdbc2Blob(BaseConnection conn, long oid) throws SQLException
    {
        super(conn, oid);
    }

}
