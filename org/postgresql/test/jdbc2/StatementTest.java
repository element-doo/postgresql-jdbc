package org.postgresql.test.jdbc2;

import org.postgresql.test.TestUtil;
import java.util.Calendar;
import junit.framework.*;
import java.sql.*;
import java.util.HashMap;
/*
 * $PostgreSQL: pgjdbc/org/postgresql/test/jdbc2/StatementTest.java,v 1.2 2004/02/24 12:33:04 jurka Exp $
 *
 * Test for getObject
 */

public class StatementTest extends TestCase
{
  Connection con = null;

  public StatementTest(String name )
  {
    super(name);
  }
  protected void setUp() throws Exception
  {
    super.setUp();

    con = TestUtil.openDB();
    TestUtil.createTempTable(con, "test_statement",
        "i int");
  }

  protected void tearDown() throws Exception
  {
    super.tearDown();
    TestUtil.dropTable( con, "test_statement" );
    con.close();
  }

  public void testClose() throws SQLException
  {
      Statement stmt = null;
      stmt = con.createStatement();
      stmt.close();

      try
      {
          stmt.getResultSet();
          this.fail( "statements should not be re-used after close" );
      }
      catch (SQLException ex)
      {

      }
  }

  /**
   * Closing a Statement twice is not an error.
   */
  public void testDoubleClose() throws SQLException
  {
     Statement stmt = con.createStatement();
     stmt.close();
     stmt.close();
  }

}
