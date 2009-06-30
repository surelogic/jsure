package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.java.JavaNode;

public class NewRegionDeclarationNode extends RegionDeclarationNode {
	// Fields
	private final int modifiers;
	private final RegionSpecificationNode parent;

	public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
			"NewRegionDeclaration") {
		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			int modifiers = _mods;
			String id = _id;
			RegionSpecificationNode parent;
			if (_kids.isEmpty()) {
				parent = null;
			} else {
				parent = (RegionSpecificationNode) _kids.get(0);
			}
			return new NewRegionDeclarationNode(_start, modifiers, id, parent);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public NewRegionDeclarationNode(int offset, int modifiers, String id,
			RegionSpecificationNode parent) {
		super(offset, id);
		this.modifiers = modifiers;
		if (parent != null) {
			((AASTNode) parent).setParent(this);
		}
		this.parent = parent;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("NewRegionDeclaration\n");
			indent(sb, indent + 2);
			sb.append("modifiers=").append(getModifiers());
			sb.append("\n");
			indent(sb, indent + 2);
			sb.append("id=").append(getId());
			sb.append("\n");
			if (getRegionParent() != null) {
				sb.append(getRegionParent().unparse(debug, indent + 2));
			}
		} else {
			sb.append("Region ");
			sb.append(getId());
			if (getRegionParent() != null) {
				sb.append(" extends ");
				sb.append(getRegionParent().unparse(false));
			}
		}
		return sb.toString();
	}

	/**
	 * @return A non-null int
	 */
	public int getModifiers() {
		return modifiers;
	}

	/**
	 * @return A non-null node
	 */
	public RegionSpecificationNode getRegionParent() {
		return parent;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	private static int[] legalVisibilities = { JavaNode.PRIVATE,
			JavaNode.PROTECTED, JavaNode.PUBLIC };

	public int getVisibility() {
		for (int viz : legalVisibilities) {
			if (JavaNode.isSet(modifiers, viz)) {
				return viz;
			}
		}
		return 0;
	}

	public boolean isStatic() {
		return isStatic(modifiers);
	}

	@Override
	public IAASTNode cloneTree() {
		return new NewRegionDeclarationNode(getOffset(), getModifiers(),
				new String(getId()), (RegionSpecificationNode) getRegionParent()
						.cloneTree());
	}
}
