package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.bind.IType;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.java.TypeExpressionNode;
import com.surelogic.aast.java.VariableUseExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.AnnotationRules.ParameterMap;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;

public class EffectSpecificationNode extends AASTNode {
	// Fields
	private final boolean isWrite;
	private ExpressionNode context;
	private final RegionSpecificationNode region;

	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
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
      sb.append(getContext().unparse(true, indent + 2));
      sb.append(getRegion().unparse(true, indent + 2));
		} else {
		  /* This is super sleazy, but I don't know what else to do. 
		   * ImplicitQualifierNode is really just a place holder to keep the
		   * parse tree happy.  All the information I need to interpret that node
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
	 * Unparse the node for use stand alone, that is, not being nested 
	 * inside an unparsing of its ancestors.
	 */
	public String standAloneUnparse() {
	  return (isWrite ? "writes " : "reads ") + unparse(false);
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

	public void checkForRewriting() {
		if (context instanceof VariableUseExpressionNode) {
			VariableUseExpressionNode v = (VariableUseExpressionNode) context;
			if (!v.bindingExists()) {
				NamedTypeNode t = new NamedTypeNode(v.getOffset(), v.getId());
				context = new TypeExpressionNode(v.getOffset(), t);
				context.setParent(this);
			}
		}
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
	
	
	
  /**
   * Compare two effect specifications from two declarations of the same method to
   * see if they are consistent. This is complicated by the fact that the
   * formal arguments of the two declarations, while the same in number, can
   * have different names. The <code>positionMap</code> is used to map formal
   * arguments of both declarations to their position in the argument list. Both
   * methods use the same map because the keys, the
   * <code>VariableUseExpressionNode</code> objects, are globally unique.
   * 
   * <p>
   * The receiver is the specification from the <em>overriding</em> method.
   * 
   * @param ancestor
   *          The specification from the <em>ancestor</em> method
   * @param positionMap
   *          The map of <code>VariableUseExpressionNode</code>s from both
   *          methods to their position in the argument lists.
   * @return Whether the specification from the overriding method satisfies the
   *         specification of the ancestor method.
   */
  public final boolean satisfiesSpecfication(
      final EffectSpecificationNode ancestor, 
      final ParameterMap parameterMap,
//      final Map<IRNode, Integer> positionMap, 
      final ITypeEnvironment typeEnv) {
    // Must be that the ancestor declares a write, or that both are reads
    if (ancestor.isWrite || !this.isWrite) {
      // Ancestor method region must be an an ancestor of the overriding method region
      final IRegion ancestorRegion = ancestor.region.resolveBinding().getRegion();
      final IRegion overridingRegion = this.region.resolveBinding().getRegion();
      if (ancestorRegion.ancestorOf(overridingRegion)) {
        // Based on cases from Effects.getEffectsFromSpecificationNode()
        final ExpressionNode ancestorContext = ancestor.context;
        final ExpressionNode overridingContext = this.context;
        if (ancestorContext instanceof ImplicitQualifierNode) {
          if (ancestorRegion.isStatic()) { // Affects class region: done
            return true;
          } else { // Affects region of the receiver
            return overridingSpecNamesTheReciever(overridingContext, overridingRegion);
          }
        } else if (ancestorContext instanceof AnyInstanceExpressionNode) {
          final IJavaType ancestorType = ((AnyInstanceExpressionNode) ancestorContext).getType().resolveType().getJavaType();

          /* Affects region of any instance.  Overriding declaration may
           * make the type more specific, or be an instance target whose
           * type is a descendant of the original type.
           */
          if (overridingContext instanceof AnyInstanceExpressionNode) {
            final IJavaType overridingType = ((AnyInstanceExpressionNode) overridingContext).getType().resolveType().getJavaType();
            return typeEnv.isRawSubType(overridingType, ancestorType);
          } else {
            // Cannot be a class target.  
            if (overridingContext instanceof TypeExpressionNode ||
                (overridingContext instanceof ImplicitQualifierNode &&
                    overridingRegion.isStatic())) {
              return false;
            } else { // Instance targets must be of a compatible type
              final IJavaType overridingType = 
                overridingContext.resolveType().getJavaType();
              return typeEnv.isRawSubType(overridingType, ancestorType);
            }
          }
        } else if (ancestorContext instanceof QualifiedThisExpressionNode) {
          // XXX: FIX this and then fix the LockNameNode methods...
          
          /* Affects region of a qualified receiver.  Overriding method must
           * use the same qualified receiver, unless one of the qualified
           * receivers is actually the regular receiver being named by the
           * 0th-outer class.
           */
          
          // First check if the qualified receiver is really the regular receiver
          if (((QualifiedThisExpressionNode) ancestorContext).namesEnclosingTypeOfAnnotatedMethod()) {
            /* Affects region of the receiver.  Overriding method must name
             * the receiver implicitly, explicitly, or via the 0th-outer class.
             */
            return overridingSpecNamesTheReciever(overridingContext, overridingRegion);
          } else { // We have a true receiver of an outer class
            if (overridingContext instanceof QualifiedThisExpressionNode) {
              final IType ancestorType = ((QualifiedThisExpressionNode) ancestorContext).getType().resolveType();
              final IType overridingType = ((QualifiedThisExpressionNode) overridingContext).getType().resolveType();
              return ancestorType.getJavaType().equals(overridingType.getJavaType());
            }
          }
        } else if (ancestorContext instanceof TypeExpressionNode) {
          // Affects class region: done
          return true;
        } else if (ancestorContext instanceof ThisExpressionNode) {
          /* Affects region of the receiver.  Overriding method must name
           * the receiver implicitly, explicitly, or via the 0th-outer class.
           */
          return overridingSpecNamesTheReciever(overridingContext, overridingRegion);
        } else if (ancestorContext instanceof VariableUseExpressionNode) {
          /* Affects region of a formal argument; Overriding declaration must
           * affect the same argument.
           */
          if (overridingContext instanceof VariableUseExpressionNode) {
            final IRNode ancestorFormal = ((VariableUseExpressionNode) ancestorContext).resolveBinding().getNode();
            final IRNode overridingFormal = ((VariableUseExpressionNode) overridingContext).resolveBinding().getNode();
            final int ancestorPos = parameterMap.getPositionOf(ancestorFormal); //positionMap.get(ancestorFormal);
            final int overridingPos = parameterMap.getPositionOf(overridingFormal); //positionMap.get(overridingFormal);
            return (ancestorPos == overridingPos);
          }
        }
      }
    }
    return false;
  }

  private static boolean overridingSpecNamesTheReciever(
      final ExpressionNode overridingContext, 
      final IRegion overridingRegion) {
    if (overridingContext instanceof ImplicitQualifierNode) {
      return !overridingRegion.isStatic();
    } else if (overridingContext instanceof ThisExpressionNode) {
      // Two "this" expressions
      return true;
    } else if (overridingContext instanceof QualifiedThisExpressionNode) {
      // One "this" expression, and one "C.this".  Equal if C is the 
      // class that contains the annotated method.
      return ((QualifiedThisExpressionNode) overridingContext).namesEnclosingTypeOfAnnotatedMethod();
    }
    return false;
  }
  
  public final EffectSpecificationNode cloneForProposal(final ParameterMap pm) {
    final RegionSpecificationNode regionNode = 
        (RegionSpecificationNode) region.cloneTree();
    final ExpressionNode contextNode;
    if (context instanceof VariableUseExpressionNode) {
      contextNode = new VariableUseExpressionNode(
          context.getOffset(),
          ParameterDeclaration.getId(pm.getCorrespondingChildArg(
              ((VariableUseExpressionNode) context).resolveBinding().getNode())));
    } else {
      contextNode = (ExpressionNode) context.cloneTree();
    }
    return new EffectSpecificationNode(
        offset, isWrite, contextNode, regionNode);
  }
  
  /**
   * Returns whether the effect specification node is satisfied by the 
   * effect "writes All".
   */
  public final boolean satisfiedByWritesAll(final IRegion regionAll) {
    /* Ancestor must be writes All (ImplicitQualifierNode) or 
     * writes java.lang.Object:All (TypeExpressionNode)
     */
    if (this.isWrite) {
      final IRegion ancestorRegion = this.region.resolveBinding().getRegion();
      if (ancestorRegion.equals(regionAll)) {
        return (this.context instanceof ImplicitQualifierNode) ||
            (this.context instanceof TypeExpressionNode);
      }
    }
    return false;
  }
}
