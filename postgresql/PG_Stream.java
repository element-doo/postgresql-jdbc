package postgresql;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;
import java.sql.*;
import postgresql.*;

/**
 * @version 1.0 15-APR-1997
 *
 * This class is used by Connection & PGlobj for communicating with the
 * backend.
 *
 * @see java.sql.Connection
 */
//  This class handles all the Streamed I/O for a postgresql connection
public class PG_Stream
{
  private Socket connection;
  private InputStream pg_input;
  private BufferedOutputStream pg_output;
  
  // This is the error message returned when an EOF occurs
  private static final String EOF_MSG = "The backend has broken the connection. Possibly the action you have attempted has caused it to close.";
  
  // This is the error message returned when an IOException occurs
  private static final String IOE_MSG = "IOError while reading from backend: ";
  
  // This is the error message returned when flushing the stream.
  private static final String FLUSH_MSG = "Error flushing output: ";
  
  /**
   * Constructor:  Connect to the PostgreSQL back end and return
   * a stream connection.
   *
   * @param host the hostname to connect to
   * @param port the port number that the postmaster is sitting on
   * @exception IOException if an IOException occurs below it.
   */
  public PG_Stream(String host, int port) throws IOException
  {
    connection = new Socket(host, port);
    
    // Submitted by Jason Venner <jason@idiom.com> adds a 10x speed
    // improvement on FreeBSD machines (caused by a bug in their TCP Stack)
    connection.setTcpNoDelay(true);
    
    pg_input = connection.getInputStream();
    pg_output = new BufferedOutputStream(connection.getOutputStream());
  }
  
  /**
   * Sends a single character to the back end
   *
   * @param val the character to be sent
   * @exception IOException if an I/O error occurs
   */
  public void SendChar(int val) throws IOException
  {
    byte b[] = new byte[1];
    b[0] = (byte)val;
    pg_output.write(b);
  }
  
  /**
   * Sends an integer to the back end
   *
   * @param val the integer to be sent
   * @param siz the length of the integer in bytes (size of structure)
   * @exception IOException if an I/O error occurs
   */
  public void SendInteger(int val, int siz) throws IOException
  {
    byte[] buf = new byte[siz];
    
    while (siz-- > 0)
      {
	buf[siz] = (byte)(val & 0xff);
	val >>= 8;
      }
    Send(buf);
  }
  
  /**
   * Sends an integer to the back end in reverse order.
   *
   * This is required when the backend uses the routines in the
   * src/backend/libpq/pqcomprim.c module.
   *
   * As time goes by, this should become obsolete.
   *
   * @param val the integer to be sent
   * @param siz the length of the integer in bytes (size of structure)
   * @exception IOException if an I/O error occurs
   */
  public void SendIntegerReverse(int val, int siz) throws IOException
  {
    byte[] buf = new byte[siz];
    int p=0;
    while (siz-- > 0)
      {
	buf[p++] = (byte)(val & 0xff);
	val >>= 8;
      }
    Send(buf);
  }
  
  /**
   * Send an array of bytes to the backend
   *
   * @param buf The array of bytes to be sent
   * @exception IOException if an I/O error occurs
   */
  public void Send(byte buf[]) throws IOException
  {
    pg_output.write(buf);
  }
  
  /**
   * Send an exact array of bytes to the backend - if the length
   * has not been reached, send nulls until it has.
   *
   * @param buf the array of bytes to be sent
   * @param siz the number of bytes to be sent
   * @exception IOException if an I/O error occurs
   */
  public void Send(byte buf[], int siz) throws IOException
  {
    Send(buf,0,siz);
  }
  
  /**
   * Send an exact array of bytes to the backend - if the length
   * has not been reached, send nulls until it has.
   *
   * @param buf the array of bytes to be sent
   * @param off offset in the array to start sending from
   * @param siz the number of bytes to be sent
   * @exception IOException if an I/O error occurs
   */
  public void Send(byte buf[], int off, int siz) throws IOException
  {
    int i;
    
    pg_output.write(buf, off, ((buf.length-off) < siz ? (buf.length-off) : siz));
    if((buf.length-off) < siz)
      {
	for (i = buf.length-off ; i < siz ; ++i)
	  {
	    pg_output.write(0);
	  }
      }
  }
  
  /**
   * Sends a packet, prefixed with the packet's length
   * @param buf buffer to send
   * @exception SQLException if an I/O Error returns
   */
  public void SendPacket(byte[] buf) throws IOException
  {
    SendInteger(buf.length+4,4);
    Send(buf);
  }
  
  /**
   * Receives a single character from the backend
   *
   * @return the character received
   * @exception SQLException if an I/O Error returns
   */
  public int ReceiveChar() throws SQLException
  {
    int c = 0;
    
    try
      {
	c = pg_input.read();
	if (c < 0) throw new IOException(EOF_MSG);
      } catch (IOException e) {
	throw new SQLException(IOE_MSG + e.toString());
      }
      return c;
  }
  
  /**
   * Receives an integer from the backend
   *
   * @param siz length of the integer in bytes
   * @return the integer received from the backend
   * @exception SQLException if an I/O error occurs
   */
  public int ReceiveInteger(int siz) throws SQLException
  {
    int n = 0;
    
    try
      {
	for (int i = 0 ; i < siz ; i++)
	  {
	    int b = pg_input.read();
	    
	    if (b < 0)
	      throw new IOException(EOF_MSG);
	    n = n | (b << (8 * i)) ;
	  }
      } catch (IOException e) {
	throw new SQLException(IOE_MSG + e.toString());
      }
      return n;
  }
  
  /**
   * Receives an integer from the backend
   *
   * @param siz length of the integer in bytes
   * @return the integer received from the backend
   * @exception SQLException if an I/O error occurs
   */
  public int ReceiveIntegerR(int siz) throws SQLException
  {
    int n = 0;
    
    try
      {
	for (int i = 0 ; i < siz ; i++)
	  {
	    int b = pg_input.read();
	    
	    if (b < 0)
	      throw new IOException(EOF_MSG);
	    n = b | (n << 8);
	  }
      } catch (IOException e) {
	throw new SQLException(IOE_MSG + e.toString());
      }
      return n;
  }
  
  /**
   * Receives a null-terminated string from the backend.  Maximum of
   * maxsiz bytes - if we don't see a null, then we assume something
   * has gone wrong.
   *
   * @param maxsiz maximum length of string
   * @return string from back end
   * @exception SQLException if an I/O error occurs
   */
  public String ReceiveString(int maxsiz) throws SQLException
  {
    byte[] rst = new byte[maxsiz];
    int s = 0;
    
    try
      {
	while (s < maxsiz)
	  {
	    int c = pg_input.read();
	    if (c < 0)
	      throw new IOException(EOF_MSG);
	    else if (c == 0)
	      break;
	    else
	      rst[s++] = (byte)c;
	  }
	if (s >= maxsiz)
	  throw new IOException("Too Much Data");
      } catch (IOException e) {
	throw new SQLException(IOE_MSG + e.toString());
      }
      String v = new String(rst, 0, s);
      return v;
  }
  
  /**
   * Read a tuple from the back end.  A tuple is a two dimensional
   * array of bytes
   *
   * @param nf the number of fields expected
   * @param bin true if the tuple is a binary tuple
   * @return null if the current response has no more tuples, otherwise
   *	an array of strings
   * @exception SQLException if a data I/O error occurs
   */
  public byte[][] ReceiveTuple(int nf, boolean bin) throws SQLException
  {
    int i, bim = (nf + 7)/8;
    byte[] bitmask = Receive(bim);
    byte[][] answer = new byte[nf][0];
    
    int whichbit = 0x80;
    int whichbyte = 0;
    
    for (i = 0 ; i < nf ; ++i)
      {
	boolean isNull = ((bitmask[whichbyte] & whichbit) == 0);
	whichbit >>= 1;
	if (whichbit == 0)
	  {
	    ++whichbyte;
	    whichbit = 0x80;
	  }
	if (isNull) 
	  answer[i] = null;
	else
	  {
	    int len = ReceiveIntegerR(4);
	    if (!bin) 
	      len -= 4;
	    if (len < 0) 
	      len = 0;
	    answer[i] = Receive(len);
	  }
      }
    return answer;
  }
  
  /**
   * Reads in a given number of bytes from the backend
   *
   * @param siz number of bytes to read
   * @return array of bytes received
   * @exception SQLException if a data I/O error occurs
   */
  private byte[] Receive(int siz) throws SQLException
  {
    byte[] answer = new byte[siz];
    Receive(answer,0,siz);
    return answer;
  }
  
  /**
   * Reads in a given number of bytes from the backend
   *
   * @param buf buffer to store result
   * @param off offset in buffer
   * @param siz number of bytes to read
   * @exception SQLException if a data I/O error occurs
   */
  public void Receive(byte[] b,int off,int siz) throws SQLException
  {
    int s = 0;
    
    try 
      {
	while (s < siz)
	  {
	    int w = pg_input.read(b, off+s, siz - s);
	    if (w < 0)
	      throw new IOException(EOF_MSG);
	    s += w;
	  }
      } catch (IOException e) {
	throw new SQLException(IOE_MSG + e.toString());
      }
  }
  
  /**
   * This flushes any pending output to the backend. It is used primarily
   * by the Fastpath code.
   * @exception SQLException if an I/O error occurs
   */
  public void flush() throws SQLException
  {
    try {
      pg_output.flush();
    } catch (IOException e) {
      throw new SQLException(FLUSH_MSG + e.toString());
    }
  }
  
  /**
   * Closes the connection
   *
   * @exception IOException if a IO Error occurs
   */
  public void close() throws IOException
  {
    pg_output.write("X\0".getBytes());
    pg_output.flush();
    pg_output.close();
    pg_input.close();
    connection.close();
  }
}
