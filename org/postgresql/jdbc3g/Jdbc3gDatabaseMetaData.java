/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3g/Jdbc3gDatabaseMetaData.java,v 1.6 2011/08/02 13:50:28 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3g;


public class Jdbc3gDatabaseMetaData extends org.postgresql.jdbc3.AbstractJdbc3DatabaseMetaData implements java.sql.DatabaseMetaData
{

    public Jdbc3gDatabaseMetaData(Jdbc3gConnection conn)
    {
        super(conn);
    }

}
