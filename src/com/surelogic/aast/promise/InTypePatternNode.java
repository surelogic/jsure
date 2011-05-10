/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/InTypePatternNode.java,v 1.1 2007/09/17 17:28:58 ethan Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.aast.AASTNode;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents a scoped promise's 'in' pattern for the type that a given element
 * is in
 * 
 * @author ethan
 */
public abstract class InTypePatternNode extends AASTNode {
	/**
	 * Constructor
	 * 
	 * @param offset
	 */
	public InTypePatternNode(int offset){
		super(offset);
	}
	
	/**
	 * Returns <code>true</code> if the target represented by the given IRNode
	 * matches this AAST
	 * 
	 * @param irNode
	 *          The IRNode to match against
	 * @return True if this AAST matches the given IRNode
	 */
	public abstract boolean matches(IRNode irNode);
	
	/*
	protected boolean matches(IRNode irNode, String pattern) {
		boolean useEnclosing = true;

		if (typePattern == null) {
			// Don't screw up the old way of doing type qualifying matches
			return true;
		}

		// FIXME Should blank package and type patterns match the current scope?

		final IRNode enclosingT = useEnclosing ? VisitUtil.getEnclosingType(irNode)
				: irNode;

		String tName;
		String pkgPattern = this.packagePattern;

		if ("".equals(packagePattern)) {
			tName = JavaNames.genFullQualifier(irNode);
			//prune any extra '.'s
			if(tName.endsWith("..")){
				//Do this b/c Type declarations end up having 2 '.' at the end
				tName = tName.substring(0, tName.length() - 1);
			}
			pkgPattern = "*";
		} else {
			tName = JJNode.getInfo(enclosingT);
		}

		final String pkg = JavaNames.genPackageQualifier(irNode);

		boolean matches = matches(pkg, pkgPattern);
		//System.err.println("package matches? " + matches);
		matches = matches && matches(tName, this.typePattern);
		//System.err.println("type matches? " + matches);

		return matches;
	}
	*/
	
	protected boolean matches(String stringToMatch, String nonRegexPattern) {
		//System.err.println("simple pattern: " + nonRegexPattern);
		//System.err.println("string to match: " + stringToMatch);

		if ("".equals(nonRegexPattern) || "*".equals(nonRegexPattern)
				|| "**".equals(nonRegexPattern)) {
			return true;
		}

		StringBuilder regex = new StringBuilder();
		String[] parts = nonRegexPattern.split("\\.");
		boolean hadTrailingDot = nonRegexPattern.endsWith(".");
		//Loop over each part of the .-delineated qualified name and replace each
		// * with [^\\.] to make sure it doesn't span package levels.
		// ** with .* to gobble everything
		// Any word with 1 or more * or ** in it with a word where each * or ** is
		// replaced as above.
		for (int i = 0, len = parts.length; i < len; i++) {
			if("*".equals(parts[i])){
				//reluctant regex
				regex.append("[^\\.]*"); 
			} else if( "**".equals(parts[i])){
				//greedy
				regex.append(".*");
			} else {
				regex.append(replaceWilds(parts[i]));
			}
			if(hadTrailingDot || i < len - 1){
  			regex.append("\\.");
			}
		}

		//System.err.println("pattern: " + regex.toString());

		return stringToMatch.matches(regex.toString());
	}

	/**
	 * @param a string with wildcards, eithe single '*' or double '**'
	 * @return
	 */
	protected String replaceWilds(String string) {
		String ret = string.replaceAll("\\*\\*", ".*");
		//Seems that looking for 0+ (i.e., using *) for non-'.' chars doesn't work if the '*' is the first char
		ret = ret.replaceAll("([^\\.]?)\\*", "$1[^\\\\.]*");
		return ret;
	}

	public abstract boolean isFullWildcard();
}
