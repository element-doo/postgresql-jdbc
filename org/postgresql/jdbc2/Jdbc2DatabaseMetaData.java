/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc2/Jdbc2DatabaseMetaData.java,v 1.6 2008/01/08 06:56:28 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc2;


public class Jdbc2DatabaseMetaData extends org.postgresql.jdbc2.AbstractJdbc2DatabaseMetaData implements java.sql.DatabaseMetaData
{
    public Jdbc2DatabaseMetaData(Jdbc2Connection conn)
    {
        super(conn);
    }

}
