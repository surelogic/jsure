package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.java.JavaNode;

public class EffectSpecificationNode extends AASTNode {
	// Fields
	private final boolean isWrite;
	private final ExpressionNode context;
	private final RegionSpecificationNode region;

	public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
			"EffectSpecification") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			boolean isWrite = JavaNode.getModifier(_mods, JavaNode.WRITE);
			ExpressionNode context = (ExpressionNode) _kids.get(0);
			RegionSpecificationNode region = (RegionSpecificationNode) _kids.get(1);
			return new EffectSpecificationNode(_start, isWrite, context, region);
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be
	 * 
	 * @unique
	 */
	public EffectSpecificationNode(int offset, boolean isWrite,
			ExpressionNode context, RegionSpecificationNode region) {
		super(offset);
		this.isWrite = isWrite;
		if (context == null) {
			throw new IllegalArgumentException("context is null");
		}
		((AASTNode) context).setParent(this);
		this.context = context;
		if (region == null) {
			throw new IllegalArgumentException("region is null");
		}
		((AASTNode) region).setParent(this);
		this.region = region;
	}

	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append("EffectSpecification\n");
			indent(sb, indent + 2);
			sb.append("isWrite=").append(getIsWrite());
			sb.append("\n");
		}
		if (debug) {
		  sb.append(getContext().unparse(true, indent + 2));
		  sb.append(getRegion().unparse(true, indent + 2));
		} else {
		  /* This is super sleazy, but I don't know what else to do. 
		   * ImplicitQualifierNode is really just a place holder to keep the
		   * parse tree happy.  All the informaton I need to interpret that node
		   * is in the region itself: if the region is instance, then we pretend
		   * the qualifier is "this", other wise we pretend the qualifier is
		   * the class name, which we already know from the bound region.
		   */
	    if (!(getContext() instanceof ImplicitQualifierNode)) {
	      sb.append(getContext().unparse(false, indent + 2));
	      sb.append(':');
	    } else {
	      final IRegionBinding boundRegion = getRegion().resolveBinding();
	      if (boundRegion != null) {
	        if (boundRegion.getRegion().isStatic()) {
	          final String s = boundRegion.getModel().regionName;
	          sb.append(s.substring(0, s.lastIndexOf('.')));
	          sb.append(':');
	        } else {          
	          sb.append("this:");
	        }
	      }
	    }
	    sb.append(getRegion().unparse(debug, indent + 2));
		}
		return sb.toString();
	}

	/**
	 * @return A non-null boolean
	 */
	public boolean getIsWrite() {
		return isWrite;
	}

	/**
	 * @return A non-null node
	 */
	public ExpressionNode getContext() {
		return context;
	}

	/**
	 * @return A non-null node
	 */
	public RegionSpecificationNode getRegion() {
		return region;
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {

		return visitor.visit(this);
	}

	@Override
	public IAASTNode cloneTree() {
		return new EffectSpecificationNode(getOffset(), getIsWrite(),
				(ExpressionNode) getContext().cloneTree(),
				(RegionSpecificationNode) getRegion().cloneTree());
	}
}