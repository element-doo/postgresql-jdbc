/*-------------------------------------------------------------------------
 *
 * PGNotification.java
 *    This interface defines public PostgreSQL extention for Notifications
 *
 * Copyright (c) 2003, PostgreSQL Global Development Group
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgjdbc/org/postgresql/PGNotification.java,v 1.5 2004/01/28 12:16:09 jurka Exp $
 *
 *-------------------------------------------------------------------------
 */
package org.postgresql;


public interface PGNotification
{
	/**
	 * Returns name of this notification
	 * @since 7.3
	 */
	public String getName();

	/**
	 * Returns the process id of the backend process making this notification
	 * @since 7.3
	 */
	public int getPID();

	/**
	 * Returns additional information from the notifying process.
	 * Currently, this feature is unimplemented and always returns
	 * an empty String.
	 */
	public String getParameter();

}

