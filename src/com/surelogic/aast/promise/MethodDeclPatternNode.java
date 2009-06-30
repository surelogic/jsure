package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.java.TypeNode;
import com.surelogic.parse.AbstractSingleNodeFactory;
import com.surelogic.parse.TempListNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class MethodDeclPatternNode extends PromiseTargetNode {
	// Fields
	private final int mods;
	private final TypeNode rtype;
	private final TypeQualifierPatternNode type;
	private final String name;
	private final List<TypeNode> sig;
	private final InPatternNode inPattern;

	public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
			"MethodDeclPattern") {
		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			int mods = _mods;
			TypeNode rtype = (TypeNode) _kids.get(0);
			TypeQualifierPatternNode type = (TypeQualifierPatternNode) _kids.get(1);
			InPatternNode inPattern = (InPatternNode) _kids.get(2);
			String name = _id;
			List<TypeNode> sig = ((TempListNode) _kids.get(3)).toList();

			return new MethodDeclPatternNode(_start, mods, rtype, type, name, sig,
					inPattern);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public MethodDeclPatternNode(int offset, int mods, TypeNode rtype,
			TypeQualifierPatternNode type, String name, List<TypeNode> sig,
			InPatternNode inPattern) {
		super(offset);
		this.mods = mods;
		if (rtype == null) {
			throw new IllegalArgumentException("rtype is null");
		}
		((AASTNode) rtype).setParent(this);
		this.rtype = rtype;
		if (type == null) {
			throw new IllegalArgumentException("type is null");
		}
		this.name = name;
		if (sig == null) {
			throw new IllegalArgumentException("sig is null");
		}
		for (TypeNode _c : sig) {
			((AASTNode) _c).setParent(this);
		}
		this.sig = Collections.unmodifiableList(sig);
		((AASTNode) type).setParent(this);
		this.type = type;
		if (name == null) {
			throw new IllegalArgumentException("name is null");
		}

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
			sb.append("MethodDeclPattern\n");
			indent(sb, indent + 2);
			sb.append("mods=").append(getMods());
			sb.append("\n");
			sb.append(getRtype().unparse(debug, indent + 2));
			sb.append(' ');
			sb.append(getType().unparse(debug, indent + 2));
			indent(sb, indent + 2);
			sb.append("name=").append(getName());
			sb.append("\n");
			sb.append(getInPattern().unparse(debug, indent + 2));
			for (AASTNode _n : getSigList()) {
				sb.append(_n.unparse(debug, indent + 2));
			}
		} else {
			String rtype = getRtype().toString();
			if (!rtype.equals("*")) {
				sb.append(getRtype());
				sb.append(' ');
			}
			sb.append(getType());
			sb.append(getName());
			sb.append('(');
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
	 * @return A non-null node
	 */
	public TypeNode getRtype() {
		return rtype;
	}

	/**
	 * @return A non-null node
	 */
	public TypeQualifierPatternNode getType() {
		return type;
	}

	/**
	 * @return A non-null String
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return A non-null, but possibly empty list of nodes
	 */
	public List<TypeNode> getSigList() {
		return sig;
	}

	/**
	 * 
	 * @return A possibly null InPatternNode
	 */
	public InPatternNode getInPattern() {
		return inPattern;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.promise.PromiseTargetNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
		// TODO Auto-generated method stub
		Operator op = JJNode.tree.getOperator(irNode);
		final String namePattern = name.replaceAll("\\*", ".*");

		// Make sure it's a method declaration
		boolean matches = MethodDeclaration.prototype.includes(op);
		if (matches) {
			// match the name
			matches = matches && (name.indexOf("*") < 0) ?
			// No wildcards
			MethodDeclaration.getId(irNode).equals(name)
					: MethodDeclaration.getId(irNode).matches(namePattern);
			// match the type
			matches = matches
					&& type.matches(MethodDeclaration.getTypes(irNode), true);
			// match the modifiers
			matches = matches
					&& matchesModifiers(mods, MethodDeclaration.getModifiers(irNode));
			// match the return type
			matches = matches
					&& matchesReturn(MethodDeclaration.getReturnType(irNode));
			// match argument list
			matches = matches && matchesArgs(MethodDeclaration.getParams(irNode));
			matches = matches && inPattern.matches(irNode);
		}
		return matches;
	}

	private boolean matchesReturn(IRNode returnType) {
		return rtype.matches(returnType);
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
	 *          The TypeNode list representing what should be matched
	 * @param actualsIterator
	 *          An {@link Iterator} over what is actually in the code
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
				//System.out.println("Too many types.");
				return false; // more types than patterns
			}
			IRNode type = actualsIterator.next();

			Operator op = JJNode.tree.getOperator(type);
			if (op instanceof ParameterDeclaration) {
				// take into account the initial increment of count
				if (!patterns.get(count - 1)
						.matches(ParameterDeclaration.getType(type))) {
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
			System.out.println("Too many patterns.");
			return false; // more patterns than types
		}
		return true;
	}

	@Override
	public IAASTNode cloneTree() {
		List<TypeNode> sigCopy = new ArrayList<TypeNode>(sig.size());
		for (TypeNode typeNode : sig) {
			sigCopy.add((TypeNode) typeNode.cloneTree());
		}
		return new MethodDeclPatternNode(getOffset(), getMods(),
				(TypeNode) getRtype().cloneTree(), (TypeQualifierPatternNode) getType()
						.cloneTree(), new String(getName()), sigCopy,
				(InPatternNode) getInPattern().cloneTree());
	}
}
