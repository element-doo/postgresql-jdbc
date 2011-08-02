/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3g/Jdbc3gBlob.java,v 1.8 2011/08/02 13:50:28 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3g;


import java.sql.*;

public class Jdbc3gBlob extends org.postgresql.jdbc3.AbstractJdbc3Blob implements java.sql.Blob
{

    public Jdbc3gBlob(org.postgresql.core.BaseConnection conn, long oid) throws SQLException
    {
        super(conn, oid);
    }

}
