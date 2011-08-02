/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3g/Jdbc3gResultSetMetaData.java,v 1.6 2011/08/02 13:50:28 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3g;

import org.postgresql.core.*;

public class Jdbc3gResultSetMetaData extends org.postgresql.jdbc2.AbstractJdbc2ResultSetMetaData implements java.sql.ResultSetMetaData
{

    public Jdbc3gResultSetMetaData(BaseConnection connection, Field[] fields)
    {
        super(connection, fields);
    }

}

