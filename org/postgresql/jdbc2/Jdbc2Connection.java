package org.postgresql.jdbc2;


import java.sql.*;
import java.util.Vector;
import java.util.Hashtable;
import org.postgresql.Field;

/* $Header: /cvsroot/jdbc/pgjdbc/org/postgresql/jdbc2/Attic/Jdbc2Connection.java,v 1.3 2002/07/25 22:45:28 barry Exp $
 * This class implements the java.sql.Connection interface for JDBC2.
 * However most of the implementation is really done in 
 * org.postgresql.jdbc2.AbstractJdbc2Connection or one of it's parents
 */
public class Jdbc2Connection extends org.postgresql.jdbc2.AbstractJdbc2Connection implements java.sql.Connection
{

        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
        {
                Jdbc2Statement s = new Jdbc2Statement(this);
                s.setResultSetType(resultSetType);
                s.setResultSetConcurrency(resultSetConcurrency);
                return s;
        }


        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
        {
                Jdbc2PreparedStatement s = new Jdbc2PreparedStatement(this, sql);
                s.setResultSetType(resultSetType);
                s.setResultSetConcurrency(resultSetConcurrency);
                return s;
        }

        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
        {
		Jdbc2CallableStatement s = new org.postgresql.jdbc2.Jdbc2CallableStatement(this,sql);
		s.setResultSetType(resultSetType);
	      	s.setResultSetConcurrency(resultSetConcurrency);
	       	return s;
        }

        public java.sql.DatabaseMetaData getMetaData() throws SQLException
        {
                if (metadata == null)
                        metadata = new org.postgresql.jdbc2.DatabaseMetaData(this);
                return metadata;
        }

        public java.sql.ResultSet getResultSet(Statement statement, Field[] fields, Vector tuples, String status, int updateCount, long insertOID, boolean binaryCursor) throws SQLException
        {
                return new Jdbc2ResultSet(this, statement, fields, tuples, status, updateCount, insertOID, binaryCursor);
        }

        public java.sql.ResultSet getResultSet(Statement statement, Field[] fields, Vector tuples, String status, int updateCount) throws SQLException
        {
                return new Jdbc2ResultSet(this, statement, fields, tuples, status, updateCount, 0, false);
        }


}


