/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3/Jdbc3ResultSetMetaData.java,v 1.9 2011/08/02 13:49:01 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3;

import org.postgresql.core.*;

public class Jdbc3ResultSetMetaData extends org.postgresql.jdbc2.AbstractJdbc2ResultSetMetaData implements java.sql.ResultSetMetaData
{

    public Jdbc3ResultSetMetaData(BaseConnection connection, Field[] fields)
    {
        super(connection, fields);
    }

}

