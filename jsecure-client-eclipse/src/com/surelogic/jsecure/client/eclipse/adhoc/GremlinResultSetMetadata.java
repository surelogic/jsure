package com.surelogic.jsecure.client.eclipse.adhoc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class GremlinResultSetMetadata implements ResultSetMetaData {
	final String[] props;
	
	public GremlinResultSetMetadata(String[] props) {
		this.props = props;
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
	public int getColumnCount() throws SQLException {
		return props.length;
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isSearchable(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int isNullable(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isSigned(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getColumnDisplaySize(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		return props[column-1];
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getSchemaName(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getScale(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getTableName(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getCatalogName(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isWritable(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

}
