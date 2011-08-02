/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/Jdbc4Blob.java,v 1.5 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;


import java.sql.*;

public class Jdbc4Blob extends AbstractJdbc4Blob implements java.sql.Blob
{

    public Jdbc4Blob(org.postgresql.core.BaseConnection conn, long oid) throws SQLException
    {
        super(conn, oid);
    }

}
