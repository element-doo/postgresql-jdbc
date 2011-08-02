/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3/Jdbc3Blob.java,v 1.8 2011/08/02 13:49:01 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3;


import java.sql.*;

public class Jdbc3Blob extends org.postgresql.jdbc3.AbstractJdbc3Blob implements java.sql.Blob
{

    public Jdbc3Blob(org.postgresql.core.BaseConnection conn, long oid) throws SQLException
    {
        super(conn, oid);
    }

}
