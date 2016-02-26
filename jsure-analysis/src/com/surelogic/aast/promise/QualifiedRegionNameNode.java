
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class QualifiedRegionNameNode extends RegionSpecificationNode { 
  // Fields
  private final NamedTypeNode type;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("QualifiedRegionName") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        NamedTypeNode type =  (NamedTypeNode) _kids.get(0);
        String id = _id;
        return new QualifiedRegionNameNode (_start,
          type,
          id        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public QualifiedRegionNameNode(int offset,
                                 NamedTypeNode type,
                                 String id) {
    super(offset, id);
    if (type == null) { throw new IllegalArgumentException("type is null"); }
    ((AASTNode) type).setParent(this);
    this.type = type;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    if (!debug) {
      return getType().unparse(false)+'.'+getId();
    }
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("QualifiedRegionName\n");
    sb.append(getType().unparse(debug, indent+2));
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    return sb.toString();
  }

  @Override
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  @Override
  public IRegionBinding resolveBinding() {
    return AASTBinder.getInstance().resolve(this);
  }

  /**
   * @return A non-null node
   */
  public NamedTypeNode getType() {
    return type;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new QualifiedRegionNameNode(getOffset(), (NamedTypeNode)getType().cloneOrModifyTree(mod), getId());
  }
}

