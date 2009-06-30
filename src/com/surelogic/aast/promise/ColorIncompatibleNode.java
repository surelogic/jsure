
package com.surelogic.aast.promise;


import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class ColorIncompatibleNode extends ColorNameListNode { 
  // Fields
  private final List<ColorNameNode> color;

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("ColorIncompatible") {
      @Override
      @SuppressWarnings("unchecked")      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        List<ColorNameNode> color = ((List) _kids);
        return new ColorIncompatibleNode (_start,
          color        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorIncompatibleNode(int offset,
                               List<ColorNameNode> color) {
    super(offset, color, "ColorIncompatible");
    if (color == null) { throw new IllegalArgumentException("color is null"); }
    for (ColorNameNode _c : color) {
      ((AASTNode) _c).setParent(this);
    }
    this.color = Collections.unmodifiableList(color);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ColorIncompatible\n");
    for(AASTNode _n : getColorList()) {
      sb.append(_n.unparse(debug, indent+2));
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
		List<ColorNameNode> colorCopy = new ArrayList<ColorNameNode>(color.size());
		for (ColorNameNode colorNameNode : color) {
			colorCopy.add((ColorNameNode)colorNameNode.cloneTree());
		}	
		return new ColorIncompatibleNode(getOffset(), colorCopy);
	}
}

