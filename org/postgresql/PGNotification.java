/*-------------------------------------------------------------------------
 *
 * Copyright (c) 2003, PostgreSQL Global Development Group
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgjdbc/org/postgresql/PGNotification.java,v 1.6 2004/06/08 07:41:56 jurka Exp $
 *
 *-------------------------------------------------------------------------
 */
package org.postgresql;

/**
 *    This interface defines public PostgreSQL extention for Notifications
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
	 * @since 7.5
	 */
	public String getParameter();

}

