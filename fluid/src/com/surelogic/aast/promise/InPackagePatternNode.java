/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/InPackagePatternNode.java,v 1.3 2007/09/25 15:59:10 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;

import com.surelogic.aast.*;

/**
 * Extends InTypePatternNode, only for convenience in reusing And, Or, Not nodes
 * 
 * @author ethan
 */
public class InPackagePatternNode extends InTypePatternNode {
	private final String pattern;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"InPackagePattern") {

		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			return new InPackagePatternNode(_start, _id);
		}
	};

	public InPackagePatternNode(int offset, String pkg) {
		super(offset);
		this.pattern = pkg;
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

	public String getPackagePattern() {
		return pattern;
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
			sb.append("InPackagePattern\n");
			indent(sb, indent + 2);
			sb.append("pattern=").append(getPackagePattern());
			sb.append("\n");
		} else {
			sb.append(getPackagePattern());
		}
		return sb.toString();
	}

	/**
	 * @return True if this tree matches the IRNode-based AST
	 */
	@Override
	public boolean matches(IRNode irNode) {
		if (pattern != null) {
			return matches(JavaNames.getPackageName(irNode), pattern);
		}
		// match all
		return true;
	}
	
	@Override
	public IAASTNode cloneTree(){
		return new InPackagePatternNode(getOffset(), pattern);		
	}
	
	@Override
	public boolean isFullWildcard() {
		return pattern.length() == 0 || "*".equals(pattern) || "**".equals(pattern);
	}
}
