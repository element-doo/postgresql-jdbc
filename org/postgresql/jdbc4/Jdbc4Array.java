/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/Jdbc4Array.java,v 1.5 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;

import java.util.Map;
import org.postgresql.core.*;
import java.sql.SQLException;
import java.sql.ResultSet;

public class Jdbc4Array extends org.postgresql.jdbc2.AbstractJdbc2Array implements java.sql.Array
{
    public Jdbc4Array(BaseConnection conn, int oid, String fieldString) throws SQLException
    {
        super(conn, oid, fieldString);
    }

    public Object getArray(Map < String, Class < ? >> map) throws SQLException
    {
        return getArrayImpl(map);
    }

    public Object getArray(long index, int count, Map < String, Class < ? >> map) throws SQLException
    {
        return getArrayImpl(index, count, map);
    }

    public ResultSet getResultSet(Map < String, Class < ? >> map) throws SQLException
    {
        return getResultSetImpl(map);
    }

    public ResultSet getResultSet(long index, int count, Map < String, Class < ? >> map) throws SQLException
    {
        return getResultSetImpl(index, count, map);
    }

    public void free() throws SQLException
    {
        throw org.postgresql.Driver.notImplemented(this.getClass(), "free()");
    }

}
