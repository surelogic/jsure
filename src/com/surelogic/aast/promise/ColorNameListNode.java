/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorNameListNode.java,v 1.1 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;

public abstract class ColorNameListNode extends ColoringAnnotationNode {
//Fields
  protected final List<ColorNameNode> color;
  private final String kind;

  public ColorNameListNode(int offset,
                        List<ColorNameNode> color,
                        String kind) {
    super(offset);
    if (color == null) { throw new IllegalArgumentException("color is null"); }
    for (ColorNameNode _c : color) {
      ((AASTNode) _c).setParent(this);
    }
    this.color = Collections.unmodifiableList(color);
    this.kind = kind;
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<ColorNameNode> getColorList() {
    return color;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append(kind);
    sb.append('\n');
    for(AASTNode _n : color) {
      sb.append(_n.unparse(debug, indent+2));
    }
    return sb.toString();
  }

  protected List<ColorNameNode> cloneColorList() {
    List<ColorNameNode> colorCopy = new ArrayList<ColorNameNode>(color.size());
    for (ColorNameNode colorNameNode : color) {
      colorCopy.add((ColorNameNode)colorNameNode.cloneTree());
    } 
    return colorCopy;
  }
}
