/*-------------------------------------------------------------------------
*
* Copyright (c) 2005-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc4/Jdbc4ParameterMetaData.java,v 1.3 2011/08/02 13:49:23 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc4;

import java.sql.ParameterMetaData;
import org.postgresql.core.BaseConnection;

public class Jdbc4ParameterMetaData extends AbstractJdbc4ParameterMetaData implements ParameterMetaData {

    public Jdbc4ParameterMetaData(BaseConnection connection, int oids[])
    {
        super(connection, oids);
    }

}

