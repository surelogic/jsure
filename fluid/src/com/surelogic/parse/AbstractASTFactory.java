/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/AbstractASTFactory.java,v 1.8 2008/07/08 18:53:14 chance Exp $*/
package com.surelogic.parse;

import java.util.*;

/**
 * Delegates to any registered factories
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractASTFactory<T> implements IASTFactory<T> {
	private Map<String, IASTFactory<T>> factories = null;

	@Override
  public T create(String token, int start, int stop, int mods,
			String id, int dims, List<T> kids) {
		checkToken(token);
		if (factories != null) {
			IASTFactory<T> f = factories.get(token);
			if (f == null) {
				if (token.endsWith("s")) {
					if (!token.equals("Throws") && !token.equals("Parameters") &&
						!token.equals("RegionSpecifications")) {
						System.err.println(
								"No factory found for " + token
										+ ", creating TempListNode");
					}
				} else if (!token.equals("Nothing")) {
					/*
					SLLogger.getLogger().log(
							Level.WARNING,
							"No factory found for non-list " + token
									+ ", creating TempListNode");
	                */
				}
				//return new TempListNode(kids);
				return createTempNode(kids);
			}
			return f.create(token, start, stop, mods, id, dims, kids);
		}
		throw new IllegalArgumentException("No factories");
	}

	protected abstract T createTempNode(List<T> kids);
	
	@Override
  public IASTFactory<T> registerFactory(String token, IASTFactory<T> f) {
		checkToken(token);
		if (f == null) {
			throw new IllegalArgumentException("Null factory provided for "
					+ token);
		}
		if (factories == null) {
			factories = new HashMap<String, IASTFactory<T>>();
		}
		return factories.put(token, f);
	}

	protected void checkToken(String token) {
		if (token == null || NIL.equals(token)) {
			throw new IllegalArgumentException("Bad token type: " + token);
		}
	}

	@Override
  public boolean handles(String token) {
		checkToken(token);
		if (factories != null) {
			IASTFactory<T> f = factories.get(token);
			return f != null && f.handles(token);
		}
		return false;
	}

	// Convenience methods
	protected static <T> T ensureOnlyChild(
			List<T> kids) {
		if (kids.size() != 1) {
			throw new IllegalArgumentException("Wrong number of children: "
					+ kids.size());
		}
		return kids.get(0);
	}
}
