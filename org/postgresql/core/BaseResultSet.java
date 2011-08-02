/*-------------------------------------------------------------------------
*
* Copyright (c) 2003-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/core/BaseResultSet.java,v 1.11 2011/08/02 13:40:12 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.core;

import java.sql.*;

/**
 * Driver-internal resultset interface. Application code should not use
 * this interface.
 */
public interface BaseResultSet extends ResultSet
{
    /**
     * Return a sanitized numeric string for a column. This handles
     * "money" representations, stripping $ and () as appropriate.
     *
     * @param col the 1-based column to retrieve
     * @return the sanitized string
     * @throws SQLException if something goes wrong
     */
    public String getFixedString(int col) throws SQLException;

    public Array createArray(int col) throws SQLException;
}
