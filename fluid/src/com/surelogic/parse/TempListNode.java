/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/TempListNode.java,v 1.9 2007/09/24 21:09:56 ethan Exp $*/
package com.surelogic.parse;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.aast.*;

/**
 * A temporary node for holding children

 * @author Edwin.Chan
 */
public class TempListNode extends AASTNode {
  final List<AASTNode> children;
  
  public TempListNode(List<AASTNode> kids) {
    super(-1);
    children = kids;
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AASTNode getParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    for(AASTNode c : children) {
      sb.append(c.unparse(debug));
    }
    return sb.toString();
  }
  
  @Override
  public int getOffset() {
    return -1;
  }
  
  @SuppressWarnings("unchecked")
  public List toList() {
    return children;
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		List<AASTNode> childrenCopy = new ArrayList<AASTNode>(children.size());
		for (AASTNode node : children) {
			childrenCopy.add((AASTNode)node.cloneTree());
		}
		return new TempListNode(childrenCopy);
	}
}
