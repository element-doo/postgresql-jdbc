/*-------------------------------------------------------------------------
*
* Copyright (c) 2004-2008, PostgreSQL Global Development Group
*
* IDENTIFICATION
*   $PostgreSQL: pgjdbc/org/postgresql/ssl/MakeSSL.java,v 1.7 2008/01/08 06:56:30 jurka Exp $
*
*-------------------------------------------------------------------------
*/
package org.postgresql.ssl;

import java.util.Properties;
import java.io.IOException;
import java.net.Socket;
import java.lang.reflect.Constructor;
import javax.net.ssl.SSLSocketFactory;

import org.postgresql.core.PGStream;
import org.postgresql.core.Logger;
import org.postgresql.util.GT;
import org.postgresql.util.PSQLState;
import org.postgresql.util.PSQLException;

public class MakeSSL {
    public static void convert(PGStream stream, Properties info, Logger logger) throws IOException, PSQLException {
        logger.debug("converting regular socket connection to ssl");

        SSLSocketFactory factory;

        // Use the default factory if no specific factory is requested
        //
        String classname = info.getProperty("sslfactory");
        if (classname == null)
        {
            factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
        }
        else
        {
            Object[] args = {info.getProperty("sslfactoryarg")};
            Constructor ctor;
            Class factoryClass;

            try
            {
                factoryClass = Class.forName(classname);
                try
                {
                    ctor = factoryClass.getConstructor(new Class[]{String.class});
                }
                catch (NoSuchMethodException nsme)
                {
                    ctor = factoryClass.getConstructor((Class[])null);
                    args = null;
                }
                factory = (SSLSocketFactory)ctor.newInstance(args);
            }
            catch (Exception e)
            {
                throw new PSQLException(GT.tr("The SSLSocketFactory class provided {0} could not be instantiated.", classname), PSQLState.CONNECTION_FAILURE, e);
            }
        }

        Socket newConnection = factory.createSocket(stream.getSocket(), stream.getHost(), stream.getPort(), true);
        stream.changeSocket(newConnection);
    }

}


