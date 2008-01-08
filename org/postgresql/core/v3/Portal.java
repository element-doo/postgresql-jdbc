/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
* Copyright (c) 2004, Open Cloud Limited.
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/core/v3/Portal.java,v 1.6 2008/01/08 06:56:27 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.core.v3;

import java.lang.ref.PhantomReference;
import org.postgresql.core.*;

/**
 * V3 ResultCursor implementation in terms of backend Portals.
 * This holds the state of a single Portal. We use a PhantomReference
 * managed by our caller to handle resource cleanup.
 *
 * @author Oliver Jowett (oliver@opencloud.com)
 */
class Portal implements ResultCursor {
    Portal(SimpleQuery query, String portalName) {
        this.query = query;
        this.portalName = portalName;
        this.encodedName = Utils.encodeUTF8(portalName);
    }

    public void close() {
        if (cleanupRef != null)
        {
            cleanupRef.clear();
            cleanupRef.enqueue();
            cleanupRef = null;
        }
    }

    String getPortalName() {
        return portalName;
    }

    byte[] getEncodedPortalName() {
        return encodedName;
    }

    SimpleQuery getQuery() {
        return query;
    }

    void setCleanupRef(PhantomReference cleanupRef) {
        this.cleanupRef = cleanupRef;
    }

    public String toString() {
        return portalName;
    }

    // Holding on to a reference to the generating query has
    // the nice side-effect that while this Portal is referenced,
    // so is the SimpleQuery, so the underlying statement won't
    // be closed while the portal is open (the backend closes
    // all open portals when the statement is closed)

    private final SimpleQuery query;
    private final String portalName;
    private final byte[] encodedName;
    private PhantomReference cleanupRef;
}
