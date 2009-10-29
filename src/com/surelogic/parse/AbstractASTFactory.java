/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/AbstractASTFactory.java,v 1.8 2008/07/08 18:53:14 chance Exp $*/
package com.surelogic.parse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.aast.AASTNode;
import com.surelogic.ast.java.operator.IJavaOperatorNode;
import com.surelogic.common.logging.SLLogger;

/**
 * Delegates to any registered factories
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractASTFactory implements IASTFactory {
	private Map<String, IASTFactory> factories = null;

	public AASTNode create(String token, int start, int stop, int mods,
			String id, int dims, List<AASTNode> kids) {
		checkToken(token);
		if (factories != null) {
			IASTFactory f = factories.get(token);
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
				return new TempListNode(kids);
			}
			return f.create(token, start, stop, mods, id, dims, kids);
		}
		throw new IllegalArgumentException("No factories");
	}

	public IASTFactory registerFactory(String token, IASTFactory f) {
		checkToken(token);
		if (f == null) {
			throw new IllegalArgumentException("Null factory provided for "
					+ token);
		}
		if (factories == null) {
			factories = new HashMap<String, IASTFactory>();
		}
		return factories.put(token, f);
	}

	protected void checkToken(String token) {
		if (token == null || NIL.equals(token)) {
			throw new IllegalArgumentException("Bad token type: " + token);
		}
	}

	public boolean handles(String token) {
		checkToken(token);
		if (factories != null) {
			IASTFactory f = factories.get(token);
			return f != null && f.handles(token);
		}
		return false;
	}

	// Convenience methods
	protected static IJavaOperatorNode ensureOnlyChild(
			List<IJavaOperatorNode> kids) {
		if (kids.size() != 1) {
			throw new IllegalArgumentException("Wrong number of children: "
					+ kids.size());
		}
		return kids.get(0);
	}
}
