/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2005, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/test/jdbc2/StatementTest.java,v 1.13 2005/01/11 08:25:48 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.test.jdbc2;

import org.postgresql.jdbc2.AbstractJdbc2Connection;
import org.postgresql.test.TestUtil;
import junit.framework.*;
import java.sql.*;
/*
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
        TestUtil.createTempTable(con, "escapetest",
                                 "ts timestamp, d date, t time, \")\" varchar(5), \"\"\"){a}'\" text ");
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        TestUtil.dropTable( con, "test_statement" );
        TestUtil.dropTable( con, "escapetest" );
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

    public void testMultiExecute() throws SQLException
    {
        Statement stmt = con.createStatement();
        stmt.execute("SELECT 1; SELECT 2");

        ResultSet rs = stmt.getResultSet();
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        rs.close();

        assertTrue(stmt.getMoreResults());
        rs = stmt.getResultSet();
        assertTrue(rs.next());
        assertEquals(2, rs.getInt(1));
        rs.close();
        stmt.close();
    }

    public void testUpdateCount() throws SQLException
    {
        Statement stmt = con.createStatement();
        int count;

        count = stmt.executeUpdate("INSERT INTO test_statement VALUES (3)");
        assertEquals(1, count);
        count = stmt.executeUpdate("INSERT INTO test_statement VALUES (3)");
        assertEquals(1, count);

        count = stmt.executeUpdate("UPDATE test_statement SET i=4");
        assertEquals(2, count);

        count = stmt.executeUpdate("CREATE TEMP TABLE another_table (a int)");
        assertEquals(0, count);
    }

    public void testEscapeProcessing() throws SQLException
    {
        Statement stmt = con.createStatement();
        int count;

        count = stmt.executeUpdate("insert into escapetest (ts) values ({ts '1900-01-01 00:00:00'})");
        assertEquals(1, count);

        count = stmt.executeUpdate("insert into escapetest (d) values ({d '1900-01-01'})");
        assertEquals(1, count);

        count = stmt.executeUpdate("insert into escapetest (t) values ({t '00:00:00'})");
        assertEquals(1, count);

        ResultSet rs = stmt.executeQuery( "select {fn version()} as version" );
        assertTrue(rs.next());
        
        // check nested and multiple escaped functions
        rs = stmt.executeQuery( "select {fn version()} as version, {fn log({fn log(3.0)})} as log" );
        assertTrue(rs.next());
        assertEquals(Math.log(Math.log(3)), rs.getDouble(2), 0.00001);

        stmt.executeUpdate("UPDATE escapetest SET \")\" = 'a', \"\"\"){a}'\" = 'b'");
        
        // check "difficult" values
        rs = stmt.executeQuery("select {fn concat(')',escapetest.\")\")} as concat" +
                ", {fn concat('{','}')} " +
                ", {fn concat('\\'','\"')} " +
                ", {fn concat(\"\"\"){a}'\", '''}''')} " +
                " FROM escapetest");
        assertTrue(rs.next());
        assertEquals(")a", rs.getString(1));
        assertEquals("{}", rs.getString(2));
        assertEquals("'\"", rs.getString(3));
        assertEquals("b'}'", rs.getString(4));
        
        count = stmt.executeUpdate( "create temp table b (i int)" );
        assertEquals(0, count);

        rs = stmt.executeQuery( "select * from test_statement as a {oj left outer join b on (a.i=b.i)} ");
        assertTrue(!rs.next());
    }


    public void testPreparedFunction() throws SQLException
    {
        PreparedStatement pstmt = con.prepareStatement("SELECT {fn concat('a', ?)}");
        pstmt.setInt(1, 5);
        ResultSet rs = pstmt.executeQuery();
        assertTrue(rs.next());
        assertEquals("a5", rs.getString(1));
    }
    
    public void testNumericFunctions() throws SQLException
    {
        Statement stmt = con.createStatement();

        ResultSet rs = stmt.executeQuery("select {fn abs(-2.3)} as abs ");
        assertTrue(rs.next());
        assertEquals(2.3f, rs.getFloat(1), 0.00001);

        rs = stmt.executeQuery("select {fn acos(-0.6)} as acos ");
        assertTrue(rs.next());
        assertEquals(Math.acos(-0.6), rs.getDouble(1), 0.00001);

        rs = stmt.executeQuery("select {fn asin(-0.6)} as asin ");
        assertTrue(rs.next());
        assertEquals(Math.asin(-0.6), rs.getDouble(1), 0.00001);

        rs = stmt.executeQuery("select {fn atan(-0.6)} as atan ");
        assertTrue(rs.next());
        assertEquals(Math.atan(-0.6), rs.getDouble(1), 0.00001);

        rs = stmt.executeQuery("select {fn atan2(-2.3,7)} as atan2 ");
        assertTrue(rs.next());
        assertEquals(Math.atan2(-2.3,7), rs.getDouble(1), 0.00001);

        rs = stmt.executeQuery("select {fn ceiling(-2.3)} as ceiling ");
        assertTrue(rs.next());
        assertEquals(-2, rs.getDouble(1), 0.00001);

        rs = stmt.executeQuery("select {fn cos(-2.3)} as cos, {fn cot(-2.3)} as cot ");
        assertTrue(rs.next());
        assertEquals(Math.cos(-2.3), rs.getDouble(1), 0.00001);
        assertEquals(1/Math.tan(-2.3), rs.getDouble(2), 0.00001);

        rs = stmt.executeQuery("select {fn degrees({fn pi()})} as degrees ");
        assertTrue(rs.next());
        assertEquals(180, rs.getDouble(1), 0.00001);

        rs = stmt.executeQuery("select {fn exp(-2.3)}, {fn floor(-2.3)}," +
                " {fn log(2.3)},{fn log10(2.3)},{fn mod(3,2)}");
        assertTrue(rs.next());
        assertEquals(Math.exp(-2.3), rs.getDouble(1), 0.00001);
        assertEquals(-3, rs.getDouble(2), 0.00001);
        assertEquals(Math.log(2.3), rs.getDouble(3), 0.00001);
        assertEquals(Math.log(2.3)/Math.log(10), rs.getDouble(4), 0.00001);
        assertEquals(1, rs.getDouble(5), 0.00001);

        rs = stmt.executeQuery("select {fn pi()}, {fn power(7,-2.3)}," +
            " {fn radians(-180)},{fn rand(-2.3)},{fn round(3.1294,2)}");
        assertTrue(rs.next());
        assertEquals(Math.PI, rs.getDouble(1), 0.00001);
        assertEquals(Math.pow(7,-2.3), rs.getDouble(2), 0.00001);
        assertEquals(-Math.PI, rs.getDouble(3), 0.00001);
        rs.getDouble(4);  // for random all we can test is that it returns something
        assertEquals(3.13, rs.getDouble(5), 0.00001);

        rs = stmt.executeQuery("select {fn sign(-2.3)}, {fn sin(-2.3)}," +
            " {fn sqrt(2.3)},{fn tan(-2.3)},{fn truncate(3.1294,2)}");
        assertTrue(rs.next());
        assertEquals(-1, rs.getInt(1));
        assertEquals(Math.sin(-2.3), rs.getDouble(2), 0.00001);
        assertEquals(Math.sqrt(2.3), rs.getDouble(3), 0.00001);
        assertEquals(Math.tan(-2.3), rs.getDouble(4), 0.00001);
        assertEquals(3.12, rs.getDouble(5), 0.00001);        
    }

    public void testStringFunctions() throws SQLException
    {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select {fn ascii(' test')},{fn char(32)}" +
                ",{fn concat('ab','cd')}" +
                ",{fn lcase('aBcD')},{fn left('1234',2)},{fn length('123 ')}" +
                ",{fn locate('bc','abc')},{fn locate('bc','abc',3)}");
        assertTrue(rs.next());
        assertEquals(32,rs.getInt(1));
        assertEquals(" ",rs.getString(2));
        assertEquals("abcd",rs.getString(3));
        assertEquals("abcd",rs.getString(4));
        assertEquals("12",rs.getString(5));
        assertEquals(3,rs.getInt(6));
        assertEquals(2,rs.getInt(7));
        assertEquals(0,rs.getInt(8));

        if (TestUtil.haveMinimumServerVersion(con, "7.3")) {
            rs = stmt.executeQuery("SELECT {fn insert('abcdef',3,2,'xxxx')}" +
                ",{fn replace('abcdbc','bc','x')}");
            assertTrue(rs.next());
            assertEquals("abxxxxef",rs.getString(1));
            assertEquals("axdx",rs.getString(2));
        }

        rs = stmt.executeQuery("select {fn ltrim(' ab')},{fn repeat('ab',2)}" +
                ",{fn right('abcde',2)},{fn rtrim('ab ')}" +
                ",{fn space(3)},{fn substring('abcd',2,2)}" +
                ",{fn ucase('aBcD')}");
        assertTrue(rs.next());
        assertEquals("ab",rs.getString(1));
        assertEquals("abab",rs.getString(2));
        assertEquals("de",rs.getString(3));
        assertEquals("ab",rs.getString(4));
        assertEquals("   ",rs.getString(5));
        assertEquals("bc",rs.getString(6));
        assertEquals("ABCD",rs.getString(7));
    }
    
    public void testDateFunctions() throws SQLException
    {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select {fn curdate()},{fn curtime()}" +
                ",{fn dayname({fn now()})}, {fn dayofmonth({fn now()})}" +
                ",{fn dayofweek({fn now()})},{fn dayofyear({fn now()})}" +
                ",{fn hour({fn now()})},{fn minute({fn now()})}" +
                ",{fn month({fn now()})}" +
                ",{fn monthname({fn now()})},{fn quarter({fn now()})}" +
                ",{fn second({fn now()})},{fn week({fn now()})}" +
                ",{fn year({fn now()})} ");
        assertTrue(rs.next());
    }
    
    public void testSystemFunctions() throws SQLException
    {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select {fn ifnull(null,'2')}" +
                ",{fn user()} ");
        assertTrue(rs.next());
        assertEquals("2",rs.getString(1));
        assertEquals(TestUtil.getUser(),rs.getString(2));

        if (TestUtil.haveMinimumServerVersion(con, "7.3")) {
            rs = stmt.executeQuery("select {fn database()} ");
            assertTrue(rs.next());
            assertEquals(TestUtil.getDatabase(),rs.getString(1));
        }
    }
    
}
