package org.postgresql.test.jdbc3;

import java.sql.*;
import junit.framework.TestCase;
import org.postgresql.test.TestUtil;

/* $PostgreSQL $ */

public class TypesTest extends TestCase {

	private Connection _conn;

	public TypesTest(String name) {
		super(name);
	}

	protected void setUp() throws SQLException {
		_conn = TestUtil.openDB();
	}

	protected void tearDown() throws SQLException {
		TestUtil.closeDB(_conn);
	}

	public void testBoolean() throws SQLException {
		PreparedStatement pstmt = _conn.prepareStatement("SELECT ?,?,?,?");
		pstmt.setNull(1, Types.BOOLEAN);
		pstmt.setObject(2, null, Types.BOOLEAN);
		pstmt.setBoolean(3, true);
		pstmt.setObject(4, Boolean.FALSE);
		ResultSet rs = pstmt.executeQuery();
		assertTrue(rs.next());
		assertTrue(!rs.getBoolean(1));
		assertTrue(rs.wasNull());
		assertNull(rs.getObject(2));
		assertTrue(rs.getBoolean(3));
		// Only the V3 protocol return will be strongly typed.
		// The V2 path will return a String because it doesn't know
		// any better.
		if (TestUtil.haveMinimumServerVersion(_conn, "7.4")) {
			assertTrue(!((Boolean)rs.getObject(4)).booleanValue());
		}
	}

}
