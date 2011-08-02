/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/test/jdbc2/optional/SimpleDataSourceTest.java,v 1.11 2011/08/02 13:50:29 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.test.jdbc2.optional;

import org.postgresql.test.TestUtil;
import org.postgresql.jdbc2.optional.SimpleDataSource;

/**
 * Performs the basic tests defined in the superclass. Just adds the
 * configuration logic.
 *
 * @author Aaron Mulder (ammulder@chariotsolutions.com)
 */
public class SimpleDataSourceTest extends BaseDataSourceTest
{
    /**
     * Constructor required by JUnit
     */
    public SimpleDataSourceTest(String name)
    {
        super(name);
    }

    /**
     * Creates and configures a new SimpleDataSource.
     */
    protected void initializeDataSource()
    {
        if (bds == null)
        {
            bds = new SimpleDataSource();
            setupDataSource(bds);
        }
    }
}
