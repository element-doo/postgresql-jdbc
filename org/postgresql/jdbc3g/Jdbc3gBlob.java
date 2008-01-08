/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3g/Jdbc3gBlob.java,v 1.7 2008/01/08 06:56:30 jurka Exp $
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
