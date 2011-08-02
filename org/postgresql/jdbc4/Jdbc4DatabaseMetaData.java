/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/Jdbc4DatabaseMetaData.java,v 1.3 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;


public class Jdbc4DatabaseMetaData extends AbstractJdbc4DatabaseMetaData implements java.sql.DatabaseMetaData
{

    public Jdbc4DatabaseMetaData(Jdbc4Connection conn)
    {
        super(conn);
    }

}
