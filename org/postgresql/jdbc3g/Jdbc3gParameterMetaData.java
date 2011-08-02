/*-------------------------------------------------------------------------
*
* Copyright (c) 2005-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3g/Jdbc3gParameterMetaData.java,v 1.3 2011/08/02 13:50:28 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3g;

import java.sql.ParameterMetaData;
import org.postgresql.core.BaseConnection;

public class Jdbc3gParameterMetaData extends org.postgresql.jdbc3.AbstractJdbc3ParameterMetaData implements ParameterMetaData {

    public Jdbc3gParameterMetaData(BaseConnection connection, int oids[])
    {
        super(connection, oids);
    }

}

