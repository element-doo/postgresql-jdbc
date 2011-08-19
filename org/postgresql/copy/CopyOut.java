/*-------------------------------------------------------------------------
*
* Copyright (c) 2009-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/copy/CopyOut.java,v 1.3 2011/08/19 21:50:39 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.copy;

import java.sql.SQLException;

public interface CopyOut extends CopyOperation {
    byte[] readFromCopy() throws SQLException;
}
