package org.postgresql.test.jdbc2;

import org.postgresql.test.TestUtil;
import junit.framework.TestCase;
import java.sql.*;

/*
 * TestCase to test the internal functionality of org.postgresql.jdbc2.DatabaseMetaData
 *
 * PS: Do you know how difficult it is to type on a train? ;-)
 *
 * $PostgreSQL: pgjdbc/org/postgresql/test/jdbc2/DatabaseMetaDataTest.java,v 1.27 2004/08/11 05:41:12 jurka Exp $
 */

public class DatabaseMetaDataTest extends TestCase
{

	private Connection con;
	/*
	 * Constructor
	 */
	public DatabaseMetaDataTest(String name)
	{
		super(name);
	}

	protected void setUp() throws Exception
	{
		con = TestUtil.openDB();
		TestUtil.createTable( con, "testmetadata", "id int4, name text, updated timestamp" );
		Statement stmt = con.createStatement();
		//we add the following comments to ensure the joins to the comments
		//are done correctly. This ensures we correctly test that case.
		stmt.execute("comment on table testmetadata is 'this is a table comment'");
		stmt.execute("comment on column testmetadata.id is 'this is a column comment'");
	}
	protected void tearDown() throws Exception
	{
		TestUtil.dropTable( con, "testmetadata" );

		TestUtil.closeDB( con );
	}

	public void testTables()
	{
		try
		{

			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);

			ResultSet rs = dbmd.getTables( null, null, "testmetadat%", new String[] {"TABLE"});
			assertTrue( rs.next() );
			String tableName = rs.getString("TABLE_NAME");
			assertEquals( "testmetadata", tableName );
			String tableType = rs.getString("TABLE_TYPE");
			assertEquals( "TABLE", tableType );
            //There should only be one row returned 
            assertTrue( "getTables() returned too many rows", rs.next() == false);
			rs.close();

			rs = dbmd.getColumns("", "", "test%", "%" );
			assertTrue( rs.next() );
			assertEquals( "testmetadata", rs.getString("TABLE_NAME") );
			assertEquals( "id", rs.getString("COLUMN_NAME") );
			assertEquals( java.sql.Types.INTEGER, rs.getInt("DATA_TYPE") );

			assertTrue( rs.next() );
			assertEquals( "testmetadata", rs.getString("TABLE_NAME") );
			assertEquals( "name", rs.getString("COLUMN_NAME") );
			assertEquals( java.sql.Types.VARCHAR, rs.getInt("DATA_TYPE") );

			assertTrue( rs.next() );
			assertEquals( "testmetadata", rs.getString("TABLE_NAME") );
			assertEquals( "updated", rs.getString("COLUMN_NAME") );
			assertEquals( java.sql.Types.TIMESTAMP, rs.getInt("DATA_TYPE") );

		}
		catch (SQLException ex)
		{
			fail(ex.getMessage());
		}
	}

	public void testCrossReference()
	{
		try
		{
			Connection con1 = TestUtil.openDB();

			TestUtil.createTable( con1, "vv", "a int not null, b int not null, primary key ( a, b )" );

			TestUtil.createTable( con1, "ww", "m int not null, n int not null, primary key ( m, n ), foreign key ( m, n ) references vv ( a, b )" );


			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);

			ResultSet rs = dbmd.getCrossReference(null, "", "vv", null, "", "ww" );

			for (int j = 1; rs.next(); j++ )
			{

				String pkTableName = rs.getString( "PKTABLE_NAME" );
				assertEquals ( "vv", pkTableName );

				String pkColumnName = rs.getString( "PKCOLUMN_NAME" );
				assertTrue( pkColumnName.equals("a") || pkColumnName.equals("b"));

				String fkTableName = rs.getString( "FKTABLE_NAME" );
				assertEquals( "ww", fkTableName );

				String fkColumnName = rs.getString( "FKCOLUMN_NAME" );
				assertTrue( fkColumnName.equals( "m" ) || fkColumnName.equals( "n" ) ) ;

				String fkName = rs.getString( "FK_NAME" );
				if (TestUtil.haveMinimumServerVersion(con1,"8.0")) {
					assertEquals("ww_m_fkey", fkName);
				} else if (TestUtil.haveMinimumServerVersion(con1,"7.3")) {
					assertTrue(fkName.startsWith("$1"));
				} else {
					assertTrue( fkName.startsWith( "<unnamed>") );
				}

				String pkName = rs.getString( "PK_NAME" );
				assertEquals( "vv_pkey", pkName );

				int keySeq = rs.getInt( "KEY_SEQ" );
				assertEquals( j, keySeq );
			}


			TestUtil.dropTable( con1, "vv" );
			TestUtil.dropTable( con1, "ww" );

		}
		catch (SQLException ex)
		{
			fail(ex.getMessage());
		}
	}

	public void testForeignKeyActions()
	{
		try {
			Connection conn = TestUtil.openDB();
			TestUtil.createTable(conn, "pkt", "id int primary key");
			TestUtil.createTable(conn, "fkt1", "id int references pkt on update restrict on delete cascade");
			TestUtil.createTable(conn, "fkt2", "id int references pkt on update set null on delete set default");
			DatabaseMetaData dbmd = conn.getMetaData();

			ResultSet rs = dbmd.getImportedKeys(null,"","fkt1");
			assertTrue(rs.next());
			assertTrue(rs.getInt("UPDATE_RULE") == DatabaseMetaData.importedKeyRestrict);
			assertTrue(rs.getInt("DELETE_RULE") == DatabaseMetaData.importedKeyCascade);
			rs.close();

			rs = dbmd.getImportedKeys(null,"","fkt2");
			assertTrue(rs.next());
			assertTrue(rs.getInt("UPDATE_RULE") == DatabaseMetaData.importedKeySetNull);
			assertTrue(rs.getInt("DELETE_RULE") == DatabaseMetaData.importedKeySetDefault);
			rs.close();

			TestUtil.dropTable(conn,"fkt2");
			TestUtil.dropTable(conn,"fkt1");
			TestUtil.dropTable(conn,"pkt");
		}
		catch (SQLException ex)
		{
			fail(ex.getMessage());
		}
	}

	public void testForeignKeysToUniqueIndexes()
	{
		try
		{
			if (!TestUtil.haveMinimumServerVersion(con,"7.4"))
				return;
		
			Connection con1 = TestUtil.openDB();
			TestUtil.createTable( con1, "pkt", "a int not null, b int not null, CONSTRAINT pkt_pk_a PRIMARY KEY (a), CONSTRAINT pkt_un_b UNIQUE (b)");
			TestUtil.createTable( con1, "fkt", "c int, d int, CONSTRAINT fkt_fk_c FOREIGN KEY (c) REFERENCES pkt(b)");

			DatabaseMetaData dbmd = con.getMetaData();
			ResultSet rs = dbmd.getImportedKeys("","","fkt");
			int j = 0;
			for (; rs.next(); j++)
			{
				assertTrue("pkt".equals(rs.getString("PKTABLE_NAME")));
				assertTrue("fkt".equals(rs.getString("FKTABLE_NAME")));
				assertTrue("pkt_un_b".equals(rs.getString("PK_NAME")));
				assertTrue("b".equals(rs.getString("PKCOLUMN_NAME")));
			}
			assertTrue(j == 1);

			TestUtil.dropTable(con1, "fkt");
			TestUtil.dropTable(con1, "pkt");
			con1.close();
		}
		catch (SQLException ex)
		{
			fail(ex.getMessage());
		}
	}

	public void testMultiColumnForeignKeys()
	{
		try
		{
			Connection con1 = TestUtil.openDB();
			TestUtil.createTable( con1, "pkt", "a int not null, b int not null, CONSTRAINT pkt_pk PRIMARY KEY (a,b)");
			TestUtil.createTable( con1, "fkt", "c int, d int, CONSTRAINT fkt_fk_pkt FOREIGN KEY (c,d) REFERENCES pkt(b,a)");

			DatabaseMetaData dbmd = con.getMetaData();
			ResultSet rs = dbmd.getImportedKeys("","","fkt");
			int j = 0;
			for (; rs.next(); j++)
			{
				assertTrue("pkt".equals(rs.getString("PKTABLE_NAME")));
				assertTrue("fkt".equals(rs.getString("FKTABLE_NAME")));
				assertTrue(j+1 == rs.getInt("KEY_SEQ"));
				if (j == 0) {
					assertTrue("b".equals(rs.getString("PKCOLUMN_NAME")));
					assertTrue("c".equals(rs.getString("FKCOLUMN_NAME")));
				} else {
					assertTrue("a".equals(rs.getString("PKCOLUMN_NAME")));
					assertTrue("d".equals(rs.getString("FKCOLUMN_NAME")));
				}
			}
			assertTrue(j == 2);

			TestUtil.dropTable(con1, "fkt");
			TestUtil.dropTable(con1, "pkt");
			con1.close();
		}
		catch (SQLException ex)
		{
			fail(ex.getMessage());
		}
	}

	public void testForeignKeys()
	{
		try
		{
			Connection con1 = TestUtil.openDB();
			TestUtil.createTable( con1, "people", "id int4 primary key, name text" );
			TestUtil.createTable( con1, "policy", "id int4 primary key, name text" );

			TestUtil.createTable( con1, "users", "id int4 primary key, people_id int4, policy_id int4," +
								  "CONSTRAINT people FOREIGN KEY (people_id) references people(id)," +
								  "constraint policy FOREIGN KEY (policy_id) references policy(id)" );


			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);

			ResultSet rs = dbmd.getImportedKeys(null, "", "users" );
			int j = 0;
			for (; rs.next(); j++ )
			{

				String pkTableName = rs.getString( "PKTABLE_NAME" );
				assertTrue ( pkTableName.equals("people") || pkTableName.equals("policy") );

				String pkColumnName = rs.getString( "PKCOLUMN_NAME" );
				assertEquals( "id", pkColumnName );

				String fkTableName = rs.getString( "FKTABLE_NAME" );
				assertEquals( "users", fkTableName );

				String fkColumnName = rs.getString( "FKCOLUMN_NAME" );
				assertTrue( fkColumnName.equals( "people_id" ) || fkColumnName.equals( "policy_id" ) ) ;

				String fkName = rs.getString( "FK_NAME" );
				assertTrue( fkName.startsWith( "people") || fkName.startsWith( "policy" ) );

				String pkName = rs.getString( "PK_NAME" );
				assertTrue( pkName.equals( "people_pkey") || pkName.equals( "policy_pkey" ) );

			}

			assertTrue ( j == 2 );

			rs = dbmd.getExportedKeys( null, "", "people" );

			// this is hacky, but it will serve the purpose
			assertTrue ( rs.next() );

			assertEquals( "people", rs.getString( "PKTABLE_NAME" ) );
			assertEquals( "id", rs.getString( "PKCOLUMN_NAME" ) );

			assertEquals( "users", rs.getString( "FKTABLE_NAME" ) );
			assertEquals( "people_id", rs.getString( "FKCOLUMN_NAME" ) );

			assertTrue( rs.getString( "FK_NAME" ).startsWith( "people" ) );


			TestUtil.dropTable( con1, "users" );
			TestUtil.dropTable( con1, "people" );
			TestUtil.dropTable( con1, "policy" );

		}
		catch (SQLException ex)
		{
			fail(ex.getMessage());
		}
	}

	public void testColumns()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getColumns(null,null,"pg_class",null);
			rs.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			fail(sqle.getMessage());
		}
	}

	public void testColumnPrivileges()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getColumnPrivileges(null,null,"pg_statistic",null);
			rs.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			fail(sqle.getMessage());
		}
	}

	public void testTablePrivileges()
	{
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getTablePrivileges(null,null,"testmetadata");
			boolean l_foundSelect = false;
			while (rs.next()) {
				if (rs.getString("GRANTEE").equals(TestUtil.getUser()) 
					&& rs.getString("PRIVILEGE").equals("SELECT")) l_foundSelect = true; 
			}
			rs.close();
			//Test that the table owner has select priv
			assertTrue("Couldn't find SELECT priv on table testmetadata for " + TestUtil.getUser(),l_foundSelect);
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			fail(sqle.getMessage());
		}
	}

	public void testPrimaryKeys()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getPrimaryKeys(null,null,"pg_class");
			rs.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			fail(sqle.getMessage());
		}
	}

	public void testIndexInfo()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getIndexInfo(null,null,"pg_class",false,false);
			rs.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			fail(sqle.getMessage());
		}
	}

	public void testTableTypes()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getTableTypes();
			rs.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			fail(sqle.getMessage());
		}
	}

	public void testProcedureColumns()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getProcedureColumns(null,null,null,null);
			rs.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			fail(sqle.getMessage());
		}
	}

	public void testVersionColumns()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getVersionColumns(null,null,"pg_class");
			rs.close();
		} catch (SQLException sqle) {
			fail(sqle.getMessage());
		}
	}

	public void testBestRowIdentifier()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getBestRowIdentifier(null,null,"pg_type",DatabaseMetaData.bestRowSession,false);
			rs.close();
		} catch (SQLException sqle) {
			fail(sqle.getMessage());
		}
	}

	public void testProcedures()
	{
		// At the moment just test that no exceptions are thrown KJ
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getProcedures(null,null,null);
			rs.close();
		} catch (SQLException sqle) {
			fail(sqle.getMessage());
		}
	}

	public void testCatalogs()
	{
		try
		{
			DatabaseMetaData dbmd = con.getMetaData();
			assertNotNull(dbmd);
			ResultSet rs = dbmd.getCatalogs();
			boolean foundTemplate0 = false;
			boolean foundTemplate1 = false;
			while(rs.next()) {
				String database = rs.getString("TABLE_CAT");
				if ("template0".equals(database)) {
					foundTemplate0 = true;
				} else if ("template1".equals(database)) {
					foundTemplate1 = true;
				}
			}
			rs.close();
			assertTrue(foundTemplate0);
			assertTrue(foundTemplate1);
		} catch(SQLException sqle) {
			fail(sqle.getMessage());
		}
	}

	public void testSchemas() throws Exception
	{
		if (!TestUtil.haveMinimumServerVersion(con, "7.3"))
			return;

		DatabaseMetaData dbmd = con.getMetaData();
		assertNotNull(dbmd);

		ResultSet rs = dbmd.getSchemas();
		boolean foundPublic = false;
		boolean foundEmpty = false;
		boolean foundPGCatalog = false;
		int count;
		
		for(count=0; rs.next(); count++) {
			String schema = rs.getString("TABLE_SCHEM");
			if ("public".equals(schema)) {
				foundPublic = true;
			} else if ("".equals(schema)) {
				foundEmpty = true;
			} else if ("pg_catalog".equals(schema)) {
				foundPGCatalog = true;
			}
		}
		rs.close();
		if (TestUtil.haveMinimumServerVersion(con,"7.3")) {
			assertTrue(count >= 2);
			assertTrue(foundPublic);
			assertTrue(foundPGCatalog);
			assertTrue(!foundEmpty);
		} else {
			assertEquals(count,1);
			assertTrue(foundEmpty);
			assertTrue(!foundPublic);
			assertTrue(!foundPGCatalog);
		}
	}

	public void testSearchStringEscape() throws Exception {
		DatabaseMetaData dbmd = con.getMetaData();
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT 'a' LIKE '" + dbmd.getSearchStringEscape() + "_'");
		assertTrue (rs.next());
		assertTrue(!rs.getBoolean(1));
		rs.close();
		stmt.close();
	}

    public void testGetUDTQualified() throws Exception
    {
		if (!TestUtil.haveMinimumServerVersion(con, "7.3"))
			return;

        Statement stmt =  null;
        try
        {
            stmt = con.createStatement();
            stmt.execute("create schema jdbc");
            stmt.execute("create type jdbc.testint8 as (i int8)");
            DatabaseMetaData dbmd = con.getMetaData();
            ResultSet rs = dbmd.getUDTs(null, null ,"jdbc.testint8" , null);
            assertTrue(rs.next());
            String cat, schema, typeName, remarks, className;
            int dataType;
            int baseType;

            cat = rs.getString("type_cat");
            schema = rs.getString("type_schem");
            typeName = rs.getString( "type_name");
            className = rs.getString("class_name");
            dataType = rs.getInt("data_type");
            remarks = rs.getString( "remarks" );
            baseType = rs.getInt("base_type");
            this.assertEquals("type name ", "testint8", typeName);
            this.assertEquals("schema name ", "jdbc",schema);

            // now test to see if the fully qualified stuff works as planned
            rs = dbmd.getUDTs("catalog", "public" ,"catalog.jdbc.testint8" , null);
            assertTrue(rs.next());
            cat = rs.getString("type_cat");
            schema = rs.getString("type_schem");
            typeName = rs.getString( "type_name");
            className = rs.getString("class_name");
            dataType = rs.getInt("data_type");
            remarks = rs.getString( "remarks" );
            baseType = rs.getInt("base_type");
            this.assertEquals("type name ", "testint8", typeName);
            this.assertEquals("schema name ", "jdbc",schema);
        }
        finally
        {
            try
            {
                if (stmt != null )
                    stmt.close();
                stmt = con.createStatement();
                stmt.execute("drop type jdbc.testint8");
                stmt.execute("drop schema jdbc");
            }
            catch( Exception ex ){}
        }

    }

    public void testGetUDT1() throws Exception
    {
		if (!TestUtil.haveMinimumServerVersion(con, "7.3"))
			return;

        try
         {
             Statement stmt = con.createStatement();
             stmt.execute("create domain testint8 as int8");
             stmt.execute("comment on domain testint8 is 'jdbc123'" );
             DatabaseMetaData dbmd = con.getMetaData();
             ResultSet rs = dbmd.getUDTs(null, null ,"testint8" , null);
             assertTrue(rs.next());
             String cat, schema, typeName, remarks, className;
             int dataType;
             int baseType;

             cat = rs.getString("type_cat");
             schema = rs.getString("type_schem");
             typeName = rs.getString( "type_name");
             className = rs.getString("class_name");
             dataType = rs.getInt("data_type");
             remarks = rs.getString( "remarks" );

             baseType = rs.getInt("base_type");
             this.assertTrue("base type", !rs.wasNull() );
             this.assertEquals("data type", Types.DISTINCT, dataType );
             this.assertEquals("type name ", "testint8", typeName);
             this.assertEquals("remarks", "jdbc123", remarks);

         }
         finally
         {
             try
             {
                 Statement stmt = con.createStatement();
                 stmt.execute("drop domain testint8");
             }
             catch( Exception ex ){}
         }
     }


     public void testGetUDT2() throws Exception
     {
		 if (!TestUtil.haveMinimumServerVersion(con, "7.3"))
			return;

         try
         {
             Statement stmt = con.createStatement();
             stmt.execute("create domain testint8 as int8");
             stmt.execute("comment on domain testint8 is 'jdbc123'");
             DatabaseMetaData dbmd = con.getMetaData();
             ResultSet rs = dbmd.getUDTs(null, null, "testint8", new int[]
                                         {Types.DISTINCT, Types.STRUCT});
             assertTrue(rs.next());
             String cat, schema, typeName, remarks, className;
             int dataType;
             int baseType;

             cat = rs.getString("type_cat");
             schema = rs.getString("type_schem");
             typeName = rs.getString("type_name");
             className = rs.getString("class_name");
             dataType = rs.getInt("data_type");
             remarks = rs.getString("remarks");

             baseType = rs.getInt("base_type");
             this.assertTrue("base type", !rs.wasNull());
             this.assertEquals("data type", Types.DISTINCT, dataType);
             this.assertEquals("type name ", "testint8", typeName);
             this.assertEquals("remarks", "jdbc123", remarks);

         }
         finally
         {
             try
             {
                 Statement stmt = con.createStatement();
                 stmt.execute("drop domain testint8");
             }
             catch (Exception ex)
             {}
         }
     }

     public void testGetUDT3() throws Exception
      {
		  if (!TestUtil.haveMinimumServerVersion(con, "7.3"))
			return;

          try
          {
              Statement stmt = con.createStatement();
              stmt.execute("create domain testint8 as int8");
              stmt.execute("comment on domain testint8 is 'jdbc123'");
              DatabaseMetaData dbmd = con.getMetaData();
              ResultSet rs = dbmd.getUDTs(null, null, "testint8", new int[]
                                          {Types.DISTINCT});
              assertTrue(rs.next());
              String cat, schema, typeName, remarks, className;
              int dataType;
              int baseType;

              cat = rs.getString("type_cat");
              schema = rs.getString("type_schem");
              typeName = rs.getString("type_name");
              className = rs.getString("class_name");
              dataType = rs.getInt("data_type");
              remarks = rs.getString("remarks");

              baseType = rs.getInt("base_type");
              this.assertTrue("base type", !rs.wasNull());
              this.assertEquals("data type", Types.DISTINCT, dataType);
              this.assertEquals("type name ", "testint8", typeName);
              this.assertEquals("remarks", "jdbc123", remarks);

          }
          finally
          {
              try
              {
                  Statement stmt = con.createStatement();
                  stmt.execute("drop domain testint8");
              }
              catch (Exception ex)
              {}
          }
      }

    public void testGetUDT4() throws Exception
    {
		if (!TestUtil.haveMinimumServerVersion(con, "7.3"))
			return;

        try
        {
            Statement stmt = con.createStatement();
            stmt.execute("create type testint8 as (i int8)");
            DatabaseMetaData dbmd = con.getMetaData();
            ResultSet rs = dbmd.getUDTs(null, null ,"testint8" , null);
            assertTrue(rs.next());
            String cat, schema, typeName, remarks, className;
            int dataType;
            int baseType;

            cat = rs.getString("type_cat");
            schema = rs.getString("type_schem");
            typeName = rs.getString( "type_name");
            className = rs.getString("class_name");
            dataType = rs.getInt("data_type");
            remarks = rs.getString( "remarks" );

            baseType = rs.getInt("base_type");
            this.assertTrue("base type", rs.wasNull() );
            this.assertEquals("data type", Types.STRUCT, dataType );
            this.assertEquals("type name ", "testint8", typeName);

        }
        finally
        {
            try
            {
                Statement stmt = con.createStatement();
                stmt.execute("drop type testint8");
            }
            catch( Exception ex ){}
        }
    }
}
