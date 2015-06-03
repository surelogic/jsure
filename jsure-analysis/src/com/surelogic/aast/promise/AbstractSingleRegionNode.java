
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;

public abstract class AbstractSingleRegionNode<T extends FieldRegionSpecificationNode> extends AASTRootNode 
{ 
	public static abstract class Factory<T extends FieldRegionSpecificationNode> 
	extends AbstractAASTNodeFactory {
		Factory(String id) {
			super(id);
		}
		
		@Override
		public final AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {
			@SuppressWarnings("unchecked")
			T spec =  (T) _kids.get(0);
			return create(_start, spec, _mods);
		}
		
		protected abstract AASTRootNode create(int offset, T spec, int mods);
	}
	
  // Fields
  private final T spec;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public AbstractSingleRegionNode(int offset, T spec) {
    super(offset);
    if (spec == null) { throw new IllegalArgumentException("spec is null"); }
    ((AASTNode) spec).setParent(this);
    this.spec = spec;
  }

  protected String unparse(boolean debug, int indent, String token) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append(token).append("Node\n");
    	indent(sb, indent+2);
    	sb.append(getSpec().unparse(debug, indent+2));
    } else {
    	sb.append(token).append("(\"");
    	sb.append(getSpec());
    	sb.append("\")");
    }
    return sb.toString();
  }
  
  @Override
  public final String unparseForPromise() {
	  return unparse(false);
  }

  /**
   * @return A non-null node
   */
  public final T getSpec() {
    return spec;
  }
  
  protected IAASTRootNode cloneTree(Factory<T> f, int mods) {
	@SuppressWarnings("unchecked")
    T s = (T)spec.cloneTree();
    return f.create(offset, s, mods);
  }
}

