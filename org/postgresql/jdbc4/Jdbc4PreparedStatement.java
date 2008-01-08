/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/Jdbc4PreparedStatement.java,v 1.2 2008/01/08 06:56:30 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;

import java.sql.*;

class Jdbc4PreparedStatement extends Jdbc4Statement implements PreparedStatement
{
    Jdbc4PreparedStatement(Jdbc4Connection connection, String sql, int rsType, int rsConcurrency, int rsHoldability) throws SQLException
    {
        this(connection, sql, false, rsType, rsConcurrency, rsHoldability);
    }

    protected Jdbc4PreparedStatement(Jdbc4Connection connection, String sql, boolean isCallable, int rsType, int rsConcurrency, int rsHoldability) throws SQLException
    {
        super(connection, sql, isCallable, rsType, rsConcurrency, rsHoldability);
    }
}
