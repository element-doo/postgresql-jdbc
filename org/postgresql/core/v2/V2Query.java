/*-------------------------------------------------------------------------
*
* Copyright (c) 2003-2005, PostgreSQL Global Development Group
* Copyright (c) 2004, Open Cloud Limited.
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/core/v2/V2Query.java,v 1.5 2005/01/11 08:25:43 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.core.v2;

import java.util.Vector;
import org.postgresql.core.*;

/**
 * Query implementation for all queries via the V2 protocol.
 */
class V2Query implements Query {
    V2Query(String query, boolean withParameters) {
        if (!withParameters)
        {
            fragments = new String[] { query };
            return ;
        }

        // Parse query and find parameter placeholders.

        Vector v = new Vector();
        int lastParmEnd = 0;

        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < query.length(); ++i)
        {
            char c = query.charAt(i);

            switch (c)
            {
            case '\\':
                if (inSingleQuotes)
                    ++i; // Skip one character.
                break;

            case '\'':
                inSingleQuotes = !inDoubleQuotes && !inSingleQuotes;
                break;

            case '"':
                inDoubleQuotes = !inSingleQuotes && !inDoubleQuotes;
                break;

            case '?':
                if (!inSingleQuotes && !inDoubleQuotes)
                {
                    v.addElement(query.substring (lastParmEnd, i));
                    lastParmEnd = i + 1;
                }
                break;

            default:
                break;
            }
        }

        v.addElement(query.substring (lastParmEnd, query.length()));

        fragments = new String[v.size()];
        for (int i = 0 ; i < fragments.length; ++i)
            fragments[i] = (String)v.elementAt(i);
    }

    public ParameterList createParameterList() {
        if (fragments.length == 1)
            return NO_PARAMETERS;

        return new SimpleParameterList(fragments.length - 1);
    }

    public String toString(ParameterList parameters) {
        StringBuffer sbuf = new StringBuffer(fragments[0]);
        for (int i = 1; i < fragments.length; ++i)
        {
            if (parameters == null)
                sbuf.append("?");
            else
                sbuf.append(parameters.toString(i));
            sbuf.append(fragments[i]);
        }
        return sbuf.toString();
    }

    public void close() {
    }

    String[] getFragments() {
        return fragments;
    }

    private static final ParameterList NO_PARAMETERS = new SimpleParameterList(0);

    private final String[] fragments;      // Query fragments, length == # of parameters + 1
}

