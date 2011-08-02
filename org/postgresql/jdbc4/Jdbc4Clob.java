/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/Jdbc4Clob.java,v 1.5 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;


public class Jdbc4Clob extends AbstractJdbc4Clob implements java.sql.Clob
{

    public Jdbc4Clob(org.postgresql.core.BaseConnection conn, long oid) throws java.sql.SQLException
    {
        super(conn, oid);
    }

}
