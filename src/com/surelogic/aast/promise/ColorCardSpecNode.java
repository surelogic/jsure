package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ColorCardSpecNode extends ColoringAnnotationNode {
	// Fields
	private final ColorNameNode color;
	private final ColorCardChoiceNode card;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"ColorCardinality") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			ColorNameNode color = (ColorNameNode) _kids.get(0);
			ColorCardChoiceNode card = (ColorCardChoiceNode) _kids.get(1);
			return new ColorCardSpecNode(_start, color, card);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public ColorCardSpecNode(int offset, ColorNameNode color,
			ColorCardChoiceNode card) {
		super(offset);
		if (color == null) {
			throw new IllegalArgumentException("color is null");
		}
		((AASTNode) color).setParent(this);
		this.color = color;
		if (card == null) {
			throw new IllegalArgumentException("card is null");
		}
		((AASTNode) card).setParent(this);
		this.card = card;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
		}
		sb.append("ColorCardinality\n");
		sb.append(getColor().unparse(debug, indent + 2));
		sb.append(getCard().unparse(debug, indent + 2));
		return sb.toString();
	}

	/**
	 * @return A non-null node
	 */
	public ColorNameNode getColor() {
		return color;
	}

	/**
	 * @return A non-null node
	 */
	public ColorCardChoiceNode getCard() {
		return card;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new ColorCardSpecNode(getOffset(), (ColorNameNode) getColor()
				.cloneTree(), (ColorCardChoiceNode) getCard().cloneTree());
	}
}
