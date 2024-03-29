/* JJT: 0.2.2 (modified by Bryan Nagy and John Boyland) */

package edu.cmu.cs.fluid.parse;


/* All AST nodes must implement this interface.  It provides basic
   machinery for constructing the parent and child relationships
   between nodes.   Some unused methods are removed here (boyland).*/

public interface Node {

  /** This method is called after the node has been made the current
    node.  It indicates that child nodes can now be added to it. */
  public void jjtOpen();

  /** This method is called after all the child nodes have been
    added. */
  public void jjtClose();

  /** This methods are used to inform the node of its parent. */
  public void jjtSetParent(Node n);
  // public Node jjtGetParent();

  /** This method tells the node to add its argument to the node's
    list of children.  */
  public void jjtAddChild(Node n);
  public void jjtAddChild(Node n, int i);

  // /** This method returns a child node.  The children are numbered
  //   from zero, left to right. */
  // public Node jjtGetChild(int i);

  // /** Return the number of children the node has. */
  // int jjtGetNumChildren();
}
