package postgresql.util;

import java.sql.*;
import java.text.*;
import java.util.*;

/**
 * This class extends SQLException, and provides our internationalisation handling
 */
public class PSQLException extends SQLException
{
    private String message;
    
    // Cache for future errors
    static ResourceBundle bundle;
    
    /**
     * This provides the same functionality to SQLException
     * @param error Error string
     */
    public PSQLException(String error) {
	super();
	translate(error,null);
    }
    
    /**
     * A more generic entry point.
     * @param error Error string or standard message id
     * @param args Array of arguments
     */
    public PSQLException(String error,Object[] args)
    {
	super();
	translate(error,args);
    }
    
    /**
     * Helper version for 1 arg
     */
    public PSQLException(String error,Object arg)
    {
	super();
	Object[] argv = new Object[1];
	argv[0] = arg;
	translate(error,argv);
    }
    
    /**
     * This does the actual translation
     */
    private void translate(String id,Object[] args)
    {
	if(bundle == null) {
	    try {
		bundle = ResourceBundle.getBundle("postgresql.errors");
	    } catch(MissingResourceException e) {
	    }
	}
	
	// Now look up a localized message. If one is not found, then use
	// the supplied message instead.
	message = null;
	try {
	    message = bundle.getString(id);
	} catch(MissingResourceException e) {
	    message = id;
	}
	
	// Expand any arguments
	if(args!=null)
	    message = MessageFormat.format(message,args);
	
    }
    
    /**
     * Overides Throwable
     */
    public String getLocalizedMessage()
    {
	return message;
    }
    
    /**
     * Overides Throwable
     */
    public String getMessage()
    {
	return message;
    }
    
    /**
     * Overides Object
     */
    public String toString()
    {
	return message;
    }
    
}
