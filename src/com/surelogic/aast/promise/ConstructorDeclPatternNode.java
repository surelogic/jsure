package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.java.TypeNode;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.parse.TempListNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class ConstructorDeclPatternNode extends ConcreteTargetNode {
	// Fields
	private final int mods;
	private final List<TypeNode> sig;
	private final InPatternNode inPattern;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"ConstructorDeclPattern") {
		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			int mods = _mods;
			InPatternNode inPattern = (InPatternNode) _kids.get(0);
			List<TypeNode> sig = ((TempListNode) _kids.get(1)).toList();
			return new ConstructorDeclPatternNode(_start, mods, sig, inPattern);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public ConstructorDeclPatternNode(int offset, int mods, 
			List<TypeNode> sig, InPatternNode inPattern) {
		super(offset);
		this.mods = mods;
		if (sig == null) {
			throw new IllegalArgumentException("sig is null");
		}
		for (TypeNode _c : sig) {
			((AASTNode) _c).setParent(this);
		}
		this.sig = Collections.unmodifiableList(sig);

		if (inPattern == null) {
			throw new IllegalArgumentException("inPattern is null");
		}
		this.inPattern = inPattern;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("ConstructorDeclPattern\n");
			indent(sb, indent + 2);
			sb.append("mods=").append(getMods());
			sb.append("\n");	
			sb.append(getInPattern().unparse(debug, indent + 2));
			for (AASTNode _n : getSigList()) {
				sb.append(_n.unparse(debug, indent + 2));
			}
		} else {
			sb.append("new(");
			unparseList(sb, getSigList());
			sb.append(')');
			sb.append(getInPattern().unparse(debug));
		}
		return sb.toString();
	}

	/**
	 * @return A non-null int
	 */
	public int getMods() {
		return mods;
	}

	/**
	 * @return A non-null, but possibly empty list of nodes
	 */
	public List<TypeNode> getSigList() {
		return sig;
	}

	/**
	 * 
	 * @return A non-null, InPatternNode
	 */
	public InPatternNode getInPattern() {
		return inPattern;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Operator appliesTo() {
		return ConstructorDeclaration.prototype;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.promise.PromiseTargetNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
		// match the type
		if (ConstructorDeclaration.prototype.includes(irNode)) {
			return  matchesModifiers(mods, ConstructorDeclaration.getModifiers(irNode))
					// match argument list
					&& matchesArgs(ConstructorDeclaration.getParams(irNode))
					// matches the 'in' pattern
					&& inPattern.matches(irNode);
		}
		return false;
	}

	/**
	 * @param params
	 * @return
	 */
	private boolean matchesArgs(IRNode params) {
		return matchLists(sig, Parameters.getFormalIterator(params));
	}

	/**
	 * Generic method to match a list of {@link TypeNode} to a list of
	 * {@link IRNode}
	 * 
	 * @param patterns
	 *            The TypeNode list representing what should be matched
	 * @param actualsIterator
	 *            An {@link Iterator} over what is actually in the code
	 * @return
	 */
	private boolean matchLists(final List<TypeNode> patterns,
			final Iterator<IRNode> actualsIterator) {
		if (patterns.isEmpty()) {
			// Should be empty
			return !actualsIterator.hasNext();
		} else {

			if ((patterns.get(0) instanceof NamedTypeNode || patterns.get(0) instanceof NamedTypePatternNode)
					&& "**".equals(((NamedTypeNode) patterns.get(0)).getType())) {
				return true;
			}
		}
		int count = 0;
		while (actualsIterator.hasNext()) {
			count++;

			if (patterns.size() < count) {
				return false; // more types than patterns
			}

			IRNode type = actualsIterator.next();

			Operator op = JJNode.tree.getOperator(type);
			if (op instanceof ParameterDeclaration) {
				// take into account the initial increment of count
				if (!patterns.get(count - 1).matches(
						ParameterDeclaration.getType(type))) {
					return false;
				}
			} else if (op instanceof NameType) {
				// take into account the initial increment of count
				if (!patterns.get(count - 1).matches(NameType.getName(type))) {
					return false;
				}
			} else {
				return false;
			}
		}
		if (count != patterns.size()) {
			return false; // more patterns than types
		}
		return true;
	}

	/*
	private boolean matchesType(IRNode decl, boolean useEnclosing) {
		IRNode here = decl;

		if ("".equals(type)) {
			return true;
		}
		if (!"*".equals(type)) {
			final IRNode enclosingT = useEnclosing ? VisitUtil
					.getEnclosingType(decl) : decl;
			final String tName = JJNode.getInfo(enclosingT);
			if (!tName.matches(type)) {
				return false;
			}
			here = enclosingT;
		}

		// otherwise: wildcard matches anything

		if ("".equals(pkg) || "*".equals(pkg)) {
			return true;
		}
		final IRNode compUnit = OpSearch.cuSearch.findEnclosing(here);
		final IRNode packg = CompilationUnit.getPkg(compUnit);
		if (NamedPackageDeclaration.prototype.includes(JJNode.tree
				.getOperator(packg))) {
			final String pkgName = NamedPackageDeclaration.getId(packg);
			final String pattern2 = pkg.replaceAll("\\*", ".*");

			return pkgName.matches(pkg);
		}

		return false;
	}
	*/

	@Override
	public IAASTNode cloneTree() {
		List<TypeNode> sigCopy = new ArrayList<TypeNode>(sig.size());
		// Clone the list
		for (TypeNode typeNode : sig) {
			sigCopy.add((TypeNode) typeNode.cloneTree());
		}

		return new ConstructorDeclPatternNode(getOffset(), getMods(),
				sigCopy, (InPatternNode) getInPattern().cloneTree());
	}

	@Override
	public boolean isFullWildcard() {
		if (mods == JavaNode.ALL_FALSE && sig.size() == 1 && inPattern.isFullWildcard() && 
			sig.get(0) instanceof NamedTypePatternNode) {
			NamedTypePatternNode st = (NamedTypePatternNode) sig.get(0);
			return st.isFullWildcard();
		}
		return false;
	}
}
