/*-------------------------------------------------------------------------
*
* Copyright (c) 2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/jdbc3g/AbstractJdbc3gConnection.java,v 1.1 2008/10/08 18:24:05 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.jdbc3g;

import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.core.Oid;
import org.postgresql.core.TypeInfo;

public abstract class AbstractJdbc3gConnection extends org.postgresql.jdbc3.AbstractJdbc3Connection
{

    public AbstractJdbc3gConnection(String host, int port, String user, String database, Properties info, String url) throws SQLException {
        super(host, port, user, database, info, url);

        TypeInfo types = getTypeInfo();
        if (haveMinimumServerVersion("8.3")) {
            types.addCoreType("uuid", Oid.UUID, java.sql.Types.OTHER, "java.util.UUID", Oid.UUID_ARRAY);
        }
    }

}

