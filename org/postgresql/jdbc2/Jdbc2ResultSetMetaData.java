/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc2/Jdbc2ResultSetMetaData.java,v 1.8 2008/01/08 06:56:28 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc2;

import org.postgresql.core.*;

public class Jdbc2ResultSetMetaData extends AbstractJdbc2ResultSetMetaData implements java.sql.ResultSetMetaData
{
    public Jdbc2ResultSetMetaData(BaseConnection connection, Field[] fields)
    {
        super(connection, fields);
    }
}

