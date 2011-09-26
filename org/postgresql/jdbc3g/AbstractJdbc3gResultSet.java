/*-------------------------------------------------------------------------
*
* Copyright (c) 2008-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3g/AbstractJdbc3gResultSet.java,v 1.4 2011/09/26 12:52:30 davecramer Exp $
*
*-------------------------------------------------------------------------
*/

package org.postgresql.jdbc3g;

import java.sql.*;
import java.util.UUID;
import java.util.Vector;

import org.postgresql.core.*;
import org.postgresql.util.ByteConverter;
import org.postgresql.util.GT;
import org.postgresql.util.PSQLState;
import org.postgresql.util.PSQLException;

public abstract class AbstractJdbc3gResultSet extends org.postgresql.jdbc3.AbstractJdbc3ResultSet
{

    public AbstractJdbc3gResultSet(Query originalQuery, BaseStatement statement, Field[] fields, Vector tuples,
                                  ResultCursor cursor, int maxRows, int maxFieldSize, int rsType, int rsConcurrency, int rsHoldability) throws SQLException
    {
        super (originalQuery, statement, fields, tuples, cursor, maxRows, maxFieldSize, rsType, rsConcurrency, rsHoldability);
    }


    protected Object getUUID(String data) throws SQLException
    {
        UUID uuid;
        try {
            uuid = UUID.fromString(data);
        } catch (java.lang.IllegalArgumentException iae) {
            throw new PSQLException(GT.tr("Invalid UUID data."), PSQLState.INVALID_PARAMETER_VALUE, iae);
        }

        return uuid;
    }

    protected Object getUUID(byte[] data) throws SQLException
    {
        return new UUID(ByteConverter.int8(data, 0), ByteConverter.int8(data, 8));
    }
}

