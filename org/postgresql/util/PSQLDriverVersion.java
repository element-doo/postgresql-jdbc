/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2011, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/util/PSQLDriverVersion.java,v 1.32 2011/09/11 01:39:32 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.util;

import org.postgresql.Driver;

/**
 * This class holds the current build number and a utility program to print
 * it and the file it came from.  The primary purpose of this is to keep
 * from filling the cvs history of Driver.java.in with commits simply
 * changing the build number.  The utility program can also be helpful for
 * people to determine what version they really have and resolve problems
 * with old and unknown versions located somewhere in the classpath.
 */
public class PSQLDriverVersion {

    public final static int buildNumber = 1000;

    public static void main(String args[]) {
        java.net.URL url = Driver.class.getResource("/org/postgresql/Driver.class");
        System.out.println(Driver.getVersion());
        System.out.println("Found in: " + url);
    }

}
