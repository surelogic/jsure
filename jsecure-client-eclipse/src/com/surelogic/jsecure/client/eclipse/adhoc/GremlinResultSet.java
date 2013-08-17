package com.surelogic.jsecure.client.eclipse.adhoc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

import com.tinkerpop.blueprints.*;
import com.tinkerpop.pipes.Pipe;

public class GremlinResultSet implements ResultSet {
	final Property[] props;
	final Pipe<?,?> pipe;
	Iterator<?> iterator;
	Object current;
	// Should be the same as current
	List<?> currentList = null;
	int currentIndex = -1;
	
	public GremlinResultSet(Pipe<?,?> result, Property[] props) {
		this.props = props;
		pipe = result;
		iterator = pipe.iterator();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean next() throws SQLException {
		if (currentList != null) {
			currentIndex++;
			if (currentIndex < currentList.size()) {
				//System.out.println("\tWorking on #"+currentIndex+" from "+currentList);
				return true;
			}
			// otherwise, we're done with this list
			//System.out.println("\tDone with "+currentList);
			currentList = null;
			currentIndex = -1;
		}
		if (iterator.hasNext()) {
			current = iterator.next();
			
			if (current instanceof List) {
				//System.out.println("Starting on "+current);
				currentList = (List<?>) current;
				currentIndex = 0;
				if (currentList.isEmpty()) {
					throw new SQLException("Not expecting an empty list");
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void close() throws SQLException {
	}

	@Override
	public boolean wasNull() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getString(final int columnIndex) throws SQLException {
		final int i = columnIndex-1;
		final Object o; 
		if (i != 0 && currentList != null) {
			o = currentList.get(currentIndex);
		} else {
			o = current;
		}
		return getString(o, props[i].expr);
	}
	
	private static String getString(final Object o, final String prop) throws SQLException {
		if (o instanceof Element) {
			Element currentElement = (Element) o;
			//return currentElement.toString();
			if ("id".equalsIgnoreCase(prop)) {
				return currentElement.getId().toString();
			}
			if (currentElement instanceof Edge) {
				Edge v = (Edge) currentElement;
				return getFromEdge(v, prop);				
			}
			/*
			if (currentElement instanceof Vertex) {
				Vertex v = (Vertex) currentElement;
				String rv = getFromVertex(v, props[i]);
				if (rv != null) {
					return rv;
				}
			}
			*/
			return getNonnullProperty(currentElement, prop);
		}
		else if (o instanceof List) {
			final List<?> l = (List<?>) o;
			final StringBuilder sb = new StringBuilder();
			sb.append('{');
			boolean first = true;
			for(Object o2 : l) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(getString(o2, prop));
			}
			sb.append('}');
			return sb.toString();
		}
		else if (o instanceof Map) {
			
		}
		return null;
	}
	
	private static String getFromVertex(Vertex v, String prop) throws SQLException {
		return getNonnullProperty(v, prop);
	}

	private static String getFromEdge(Edge e, String prop) throws SQLException {
		if ("label".equals(prop)) {
			return e.getLabel();
		}
		if (prop.startsWith("head.")) {
			return getFromVertex(e.getVertex(Direction.IN), prop.substring(5));
		}
		if (prop.startsWith("tail.")) {
			return getFromVertex(e.getVertex(Direction.OUT), prop.substring(5));
		}
		return getNonnullProperty(e, prop);
	}

	private static String getNonnullProperty(Element e, String prop) throws SQLException {
		String rv = e.getProperty(prop);
		if (rv == null && !"icon".equals(prop)) {
			throw new SQLException("No such property '"+prop+"'on "+e.getClass().getSimpleName());
		}
		return rv;
	}
	
	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public String getCursorName() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return new GremlinResultSetMetadata(props);
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isFirst() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isLast() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void beforeFirst() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void afterLast() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean first() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean last() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getRow() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean previous() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public int getFetchDirection() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public int getFetchSize() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getType() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getConcurrency() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean rowInserted() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void updateNull(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBoolean(String columnLabel, boolean x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader,
			int length) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void insertRow() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateRow() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void deleteRow() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void refreshRow() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void moveToInsertRow() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public Statement getStatement() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public int getHoldability() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isClosed() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void updateNString(int columnIndex, String nString)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNString(String columnLabel, String nString)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x,
			long length) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream,
			long length) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateClob(String columnLabel, Reader reader)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void updateNClob(String columnLabel, Reader reader)
			throws SQLException {
		throw new UnsupportedOperationException();

	}

}