package org.postgresql.jdbc1;


import java.sql.*;

/* $Header: /cvsroot/jdbc/pgjdbc/org/postgresql/jdbc1/Attic/Jdbc1Statement.java,v 1.3 2002/09/06 21:23:06 momjian Exp $
 * This class implements the java.sql.Statement interface for JDBC1.
 * However most of the implementation is really done in
 * org.postgresql.jdbc1.AbstractJdbc1Statement
 */
public class Jdbc1Statement extends org.postgresql.jdbc1.AbstractJdbc1Statement implements java.sql.Statement
{

	public Jdbc1Statement (Jdbc1Connection c)
	{
		super(c);
	}

}
