
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

public class StartsSpecificationNode extends AASTRootNode 
{  
  // Fields

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("StartsSpecification") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new StartsSpecificationNode (_start        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public StartsSpecificationNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append("StartsSpecification\n");
    } else {
      return "Starts(\"nothing\")";
    }
    return sb.toString();
  }

  @Override
  public final String unparseForPromise() {
	 return unparse(false);
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new StartsSpecificationNode(getOffset());
  }

  @Override
  public boolean implies(IAASTRootNode other) {
	  return isSameClass(other);
  }
  
  @Override
  public boolean isSameAs(IAASTRootNode other) {
	  return isSameClass(other);
  }
}

