/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/Jdbc4ResultSetMetaData.java,v 1.3 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;

import org.postgresql.core.*;

public class Jdbc4ResultSetMetaData extends AbstractJdbc4ResultSetMetaData implements java.sql.ResultSetMetaData
{

    public Jdbc4ResultSetMetaData(BaseConnection connection, Field[] fields)
    {
        super(connection, fields);
    }

}

