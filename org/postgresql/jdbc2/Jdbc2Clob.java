/*-------------------------------------------------------------------------
 *
 * Copyright (c) 2004, PostgreSQL Global Development Group
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgjdbc/org/postgresql/jdbc2/Jdbc2Clob.java,v 1.2 2004/11/07 22:16:14 jurka Exp $
 *
 *-------------------------------------------------------------------------
 */
package org.postgresql.jdbc2;


public class Jdbc2Clob extends AbstractJdbc2Clob implements java.sql.Clob
{

	public Jdbc2Clob(org.postgresql.PGConnection conn, int oid) throws java.sql.SQLException
	{
		super(conn, oid);
	}

}
