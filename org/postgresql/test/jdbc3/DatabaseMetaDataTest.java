/*-------------------------------------------------------------------------
*
* Copyright (c) 2008-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/test/jdbc3/DatabaseMetaDataTest.java,v 1.2 2008/01/08 06:56:31 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.test.jdbc3;

import org.postgresql.test.TestUtil;
import junit.framework.TestCase;
import java.sql.*;

public class DatabaseMetaDataTest extends TestCase
{

    private Connection _conn;

    public DatabaseMetaDataTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        _conn = TestUtil.openDB();
        if (TestUtil.haveMinimumServerVersion(_conn, "7.3")) {
            Statement stmt = _conn.createStatement();
            stmt.execute("CREATE DOMAIN mydom AS int");
            stmt.execute("CREATE TABLE domtab (a mydom)");
        }
    }

    protected void tearDown() throws Exception
    {
        if (TestUtil.haveMinimumServerVersion(_conn, "7.3")) {
            Statement stmt = _conn.createStatement();
            stmt.execute("DROP TABLE domtab");
            stmt.execute("DROP DOMAIN mydom");
        }
    }

    public void testGetColumnsForDomain() throws Exception
    {
        if (!TestUtil.haveMinimumServerVersion(_conn, "7.3"))
            return;

        DatabaseMetaData dbmd = _conn.getMetaData();

        ResultSet rs = dbmd.getColumns("%","%","domtab", "%");
        assertTrue( rs.next() );
        assertEquals("a", rs.getString("COLUMN_NAME"));
        assertEquals(Types.DISTINCT, rs.getInt("DATA_TYPE"));
        assertEquals("mydom", rs.getString("TYPE_NAME"));
        assertEquals(Types.INTEGER, rs.getInt("SOURCE_DATA_TYPE"));
        assertTrue( !rs.next() );
    }

}
