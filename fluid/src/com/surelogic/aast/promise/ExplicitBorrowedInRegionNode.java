
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

public class ExplicitBorrowedInRegionNode extends AASTRootNode 
{ 
  // Fields
  private final MappedRegionSpecificationNode spec;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ExplicitBorrowedInRegion") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        MappedRegionSpecificationNode spec =  (MappedRegionSpecificationNode) _kids.get(0);
        return new ExplicitBorrowedInRegionNode (_start, spec        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ExplicitBorrowedInRegionNode(int offset,
                     MappedRegionSpecificationNode spec) {
    super(offset);
    if (spec == null) { throw new IllegalArgumentException("spec is null"); }
    ((AASTNode) spec).setParent(this);
    this.spec = spec;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("ExplicitBorrowedInRegionNode\n");
    	indent(sb, indent+2);
    	sb.append(spec.unparse(debug, indent+2));
    } else {
    	sb.append("BorrowedInRegion(\"");
    	sb.append(spec.unparse(debug, indent+2));
    	sb.append("\")");
    }
    return sb.toString();
  }

  public String unparseForPromise() {
	  return unparse(false);
  }
  
  /**
   * @return A non-null node
   */
  public MappedRegionSpecificationNode getMapping() {
    return spec;
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ExplicitBorrowedInRegionNode(offset, (MappedRegionSpecificationNode)spec.cloneTree());
  }
}

