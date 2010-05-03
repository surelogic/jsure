package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ColorGrantNode extends ColorNameListNode {

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ColorGrant") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      List<ColorNameNode> color = ((List) _kids);
      return new ColorGrantNode(_start, color);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorGrantNode(int offset, List<ColorNameNode> color) {
    super(offset, color, "ColorGrant");
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
    }
    sb.append("ColorGrant\n");
    for (AASTNode _n : getColorList()) {
      sb.append(_n.unparse(debug, indent + 2));
    }
    return sb.toString();
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<ColorNameNode> getColorList() {
    return color;
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {

    return visitor.visit(this);
  }

  /* (non-Javadoc)
   * @see com.surelogic.aast.AASTNode#cloneTree()
   */
  @Override
  public IAASTNode cloneTree() {
    return new ColorGrantNode(getOffset(), cloneColorList());
  }
}
