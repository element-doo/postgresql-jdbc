package org.postgresql.jdbc2;

import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

public abstract class AbstractJdbc2Blob
{
	private int oid;
	private LargeObject lo;

	public AbstractJdbc2Blob(PGConnection conn, int oid) throws SQLException
	{
		this.oid = oid;
		LargeObjectManager lom = conn.getLargeObjectAPI();
		this.lo = lom.open(oid);
	}

	public long length() throws SQLException
	{
		return lo.size();
	}

	public InputStream getBinaryStream() throws SQLException
	{
		return lo.getInputStream();
	}

	public byte[] getBytes(long pos, int length) throws SQLException
	{
		lo.seek((int)pos, LargeObject.SEEK_SET);
		return lo.read(length);
	}

	/*
	 * For now, this is not implemented.
	 */
	public long position(byte[] pattern, long start) throws SQLException
	{
		throw org.postgresql.Driver.notImplemented();
	}

	/*
	 * This should be simply passing the byte value of the pattern Blob
	 */
	public long position(Blob pattern, long start) throws SQLException
	{
		return position(pattern.getBytes(0, (int)pattern.length()), start);
	}

}
