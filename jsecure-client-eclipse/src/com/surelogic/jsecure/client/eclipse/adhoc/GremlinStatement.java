package com.surelogic.jsecure.client.eclipse.adhoc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.*;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.pipes.Pipe;

public class GremlinStatement implements Statement {
	final ScriptEngine engine;
	ResultSet resultSet;
	
	GremlinStatement(final Graph graphDb) {
		ScriptEngineManager manager = new ScriptEngineManager();		
		engine = manager.getEngineByName("gremlin-groovy");
		engine.getBindings(ScriptContext.ENGINE_SCOPE).put("g", graphDb);
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
	public ResultSet executeQuery(String sql) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void close() throws SQLException {
		// TODO what to do?
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public int getMaxRows() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public int getQueryTimeout() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void cancel() throws SQLException {
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
	public void setCursorName(String name) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean execute(String sql) throws SQLException {
		// Extract the embedded Gremlin query
		final StringBuilder sb = new StringBuilder();
		Property[] props = null;
		for(String line : sql.split("\\n")) {
			if (line.startsWith("--")) {
				sb.append(line.substring(2)).append(' ');
			} else {
				int start = sql.indexOf(line);
				// Parse the SQL
				props = parseProperties(sql.substring(start));
				break;
			}
		}
		String cypher = sb.toString();
		try {
			Object result = engine.eval(cypher);
			if (result instanceof Pipe) {
				@SuppressWarnings("unchecked")
				Pipe<?,? extends Element> p = (Pipe<?,? extends Element>) result;
				resultSet = new GremlinResultSet(p, props);
				return true;
			}
			else if (result != null) {
				throw new SQLException("Unexpected result type: "+result.getClass());
			}
		} catch (ScriptException e) {
			throw new SQLException(e);
		}
		return false;
	}

	/**
	 * Extract the properties to display
	 */
	private Property[] parseProperties(String sql) throws SQLException {
		final String[] parts = sql.split("[\n ,]");
		final int len = parts.length;
		if (len > 3 && "select".equalsIgnoreCase(parts[0]) &&
			"from".equalsIgnoreCase(parts[len-2]) &&
			"gremlin".equalsIgnoreCase(parts[len-1])) {
			// Preprocess to group expressions and labels
			List<Property> props = new ArrayList<Property>(len-3); 
			Property lastProp = null;
			for(int i=1; i<len-2; i++) {
				if (parts[i] != null && parts[i].length() > 0) {
					if (parts[i].startsWith("\"") && parts[i].endsWith("\"")) {
						if (lastProp == null) {
							throw new SQLException("Missing Gremlin expression before "+parts[i]);
						}
						lastProp.setLabel(parts[i].substring(1, parts[i].length()-1));
					} else {
						lastProp = new Property(parts[i]);
						props.add(lastProp);
					} 
				}
			}
			return props.toArray(new Property[props.size()]);
		}
		throw new SQLException("Not following the pattern SELECT prop1, prop2, ... FROM GREMLIN: "+sql);
	}
	
	@Override
	public ResultSet getResultSet() throws SQLException {
		return resultSet;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean getMoreResults() throws SQLException {
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
	public int getResultSetConcurrency() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getResultSetType() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void clearBatch() throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public int[] executeBatch() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Connection getConnection() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean execute(String sql, String[] columnNames)
			throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean isClosed() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean isPoolable() throws SQLException {
		throw new UnsupportedOperationException();
		
	}

}
