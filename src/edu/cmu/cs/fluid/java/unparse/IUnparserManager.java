package edu.cmu.cs.fluid.java.unparse;

import java.util.Iterator;

import edu.cmu.cs.fluid.display.TextCoord;
import edu.cmu.cs.fluid.display.TextRegion;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 */
public interface IUnparserManager {
  /**
   * Get the roots of the tree to be unparsed
   * @return The roots
   */
  Iterator<IRNode> getRoots();

   /**
   * Get the unparsed tree.
   * @return An array of Strings of the unparsed text.  <em>Warning</em>:
   * The array is shared, and should not be modified by the caller.
   */
  String[] getUnparsedText();

  /**
   * Get the TextRegion (the extend of the text) belonging to the given
   * IRNode within the parse tree.
   * @exception NullPointerException Thrown if the given node is null.
   * @return TextRegion
   */
  TextRegion getNodeRegion(IRNode n);

  /**
   * Get the TextRegion (the extent of the text) belonging to the IRNode
   * that is associated with the text at the given character location.
   * Returns <code>null</code> if the location doesn't have a node
   * associated with it.
   */
  TextRegion getNodeRegion(TextCoord textCoord);

 /**
   * Get the IRNode associated with the text at the given character location.
   * @param where The location of interest.
   * @return The IRNode associated with the text location, or <code>null</code>
   * if there is not node associated with the position.
   */
  IRNode getNodeAt(TextCoord textCoord);
  
  /**
   * Redo the unparsing using the specified line width.  This should be used
   * when the parse tree is unchanged, but the region in which it is displayed
   * has changed width.  It should not be used, for example, when the current
   * version has changed.
   */
  public String[] unparseWithWidth( final int lineWidth );

  
  /**
   * Update the tokens and then redo the unparsing using the specified line
   * width.  This should be used when the parse tree itself has changed, for
   * example, because the version has changed.
   */
  public String[] updateWithWidth( final int lineWidth );  
}
