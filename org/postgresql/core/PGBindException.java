/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/core/PGBindException.java,v 1.6 2011/08/02 13:40:12 davecramer Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.core;

import java.io.IOException;

public class PGBindException extends IOException {

    private IOException _ioe;

    public PGBindException(IOException ioe) {
        _ioe = ioe;
    }

    public IOException getIOException() {
        return _ioe;
    }
}
