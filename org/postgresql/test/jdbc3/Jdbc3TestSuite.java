/*-------------------------------------------------------------------------
 *
 * Copyright (c) 2004, PostgreSQL Global Development Group
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgjdbc/org/postgresql/test/jdbc3/Jdbc3TestSuite.java,v 1.8 2004/11/07 22:17:07 jurka Exp $
 *
 *-------------------------------------------------------------------------
 */
package org.postgresql.test.jdbc3;

import junit.framework.TestSuite;

/*
 * Executes all known tests for JDBC3
 */
public class Jdbc3TestSuite extends TestSuite
{

	/*
	 * The main entry point for JUnit
	 */
	public static TestSuite suite()
	{
        TestSuite suite = new TestSuite();
	suite.addTestSuite(Jdbc3SavepointTest.class);
	suite.addTestSuite(TypesTest.class);
        return suite;
	}
}
