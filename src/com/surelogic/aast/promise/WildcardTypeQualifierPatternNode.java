/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/WildcardTypeQualifierPatternNode.java,v 1.8 2008/10/01 20:56:15 chance Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * Represents a type qualifier string that can contain wildcard characters
 * 
 * @author ethan
 */
public class WildcardTypeQualifierPatternNode extends InTypePatternNode {
	// Can't be final b/c we may modify it with package info
	private final String typePattern;

	// Set only via the private constructor which is called via the {@link
	// #cloneAndModifyTree() method
	private String packagePattern;

	public static final AbstractSingleNodeFactory factory = new com.surelogic.parse.AbstractSingleNodeFactory(
			"WildcardTypeQualifierPattern") {

		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			return new WildcardTypeQualifierPatternNode(_start, _id);
		}
	};

	public WildcardTypeQualifierPatternNode(int offset, String typePattern) {
		super(offset);
		this.typePattern = typePattern;
		this.packagePattern = "";
	}

	/**
	 * Only used when cloning and modifying a tree via the
	 * {@link #cloneAndModifyTree(String)}
	 * 
	 * @param offset
	 * @param typePattern
	 * @param packagePattern
	 */
	private WildcardTypeQualifierPatternNode(int offset, String typePattern,
			String packagePattern) {
		super(offset);
		this.typePattern = typePattern;
		this.packagePattern = packagePattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.promise.InTypePatternNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
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

	private boolean matches(String stringToMatch, String nonRegexPattern) {
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
	private String replaceWilds(String string) {
		String ret = string.replaceAll("\\*\\*", ".*");
		//Seems that looking for 0+ (i.e., using *) for non-'.' chars doesn't work if the '*' is the first char
		ret = ret.replaceAll("([^\\.]?)\\*", "$1[^\\\\.]*");
		return ret;
	}

	/**
	 * Returns the qualified type pattern
	 * 
	 * @return
	 */
	public String getTypePattern() {
		return typePattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#accept(com.surelogic.aast.INodeVisitor)
	 */
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#unparse(boolean, int)
	 */
	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("WildcardTypeQualifierPattern\n");
			indent(sb, indent + 2);
			sb.append("type=").append(getTypePattern());
			sb.append("\n");
		} else {
			sb.append(typePattern);
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.promise.InTypePatternNode#combineAASTs(com.surelogic.aast.promise.InTypePatternNode)
	 */
	@Override
	InTypePatternNode combineAASTs(InTypePatternNode typeNode) {
		return typeNode.cloneAndModifyTree(typePattern);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.IAASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new WildcardTypeQualifierPatternNode(offset, new String(typePattern));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.promise.InTypePatternNode#cloneTree(java.lang.String)
	 */
	@Override
	InTypePatternNode cloneAndModifyTree(String packagePattern) {
		StringBuilder sb = new StringBuilder(packagePattern);
		// add a '.' if the package pattern doesn't end with one - in order to
		// separate it
		
		if (!packagePattern.endsWith(".") && !"*".equals(packagePattern)) {
			sb.append(".");
		}

		/*
		 * StringBuilder sb2 = new StringBuilder(this.typePattern); if
		 * (!typePattern.endsWith(".")) { sb2.append("."); }
		 */
		return new WildcardTypeQualifierPatternNode(offset, this.typePattern, sb
				.toString());
	}
}
