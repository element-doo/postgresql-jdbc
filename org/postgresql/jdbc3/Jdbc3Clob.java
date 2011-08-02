/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3/Jdbc3Clob.java,v 1.8 2011/08/02 13:49:01 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3;


public class Jdbc3Clob extends org.postgresql.jdbc3.AbstractJdbc3Clob implements java.sql.Clob
{

    public Jdbc3Clob(org.postgresql.core.BaseConnection conn, long oid) throws java.sql.SQLException
    {
        super(conn, oid);
    }

}
