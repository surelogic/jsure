
package com.surelogic.aast.promise;


import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class SubtypedBySpecificationNode extends AASTNode { 
  // Fields
  private final List<NamedTypeNode> types;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("SubtypedBySpecification") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<NamedTypeNode> types = ((List) _kids);
        return new SubtypedBySpecificationNode (_start,
          types        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public SubtypedBySpecificationNode(int offset,
                                     List<NamedTypeNode> types) {
    super(offset);
    if (types == null) { throw new IllegalArgumentException("types is null"); }
    for (NamedTypeNode _c : types) {
      ((AASTNode) _c).setParent(this);
    }
    this.types = Collections.unmodifiableList(types);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("SubtypedBySpecification\n");
    for(AASTNode _n : getTypesList()) {
      sb.append(_n.unparse(debug, indent+2));
    }
    return sb.toString();
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<NamedTypeNode> getTypesList() {
    return types;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	List<NamedTypeNode> typesCopy = new ArrayList<NamedTypeNode>(types.size());
  	for (NamedTypeNode namedTypeNode : types) {
			typesCopy.add((NamedTypeNode)namedTypeNode.cloneOrModifyTree(mod));
		}
  	return new SubtypedBySpecificationNode(getOffset(), typesCopy);
  }
}

