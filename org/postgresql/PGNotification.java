/*-------------------------------------------------------------------------
 *
 * Copyright (c) 2003-2004, PostgreSQL Global Development Group
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgjdbc/org/postgresql/PGNotification.java,v 1.9 2004/11/07 22:15:29 jurka Exp $
 *
 *-------------------------------------------------------------------------
 */
package org.postgresql;

/**
 *    This interface defines the public PostgreSQL extension for Notifications
 */
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
	 *
	 * @since 8.0
	 */
	public String getParameter();

}

