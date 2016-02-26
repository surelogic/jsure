
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.ClassTypeNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class QualifiedReceiverDeclarationNode extends AASTNode { 
  // Fields
  private final ClassTypeNode base;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("QualifiedReceiverDeclaration") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ClassTypeNode base =  (ClassTypeNode) _kids.get(0);
        return new QualifiedReceiverDeclarationNode (_start,
          base        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public QualifiedReceiverDeclarationNode(int offset,
                                          ClassTypeNode base) {
    super(offset);
    if (base == null) { throw new IllegalArgumentException("base is null"); }
    ((AASTNode) base).setParent(this);
    this.base = base;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("QualifiedReceiverDeclaration\n");
    sb.append(getBase().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ClassTypeNode getBase() {
    return base;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new QualifiedReceiverDeclarationNode(getOffset(), (ClassTypeNode)getBase().cloneOrModifyTree(mod));
  }
}

