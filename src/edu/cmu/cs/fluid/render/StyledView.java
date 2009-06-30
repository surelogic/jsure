package edu.cmu.cs.fluid.render;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A View that adds a style attribute to an existing Model
 *
 * @author Edwin Chan
 */
public interface StyledView { 
  //===========================================================
  //== Attribute names
  //===========================================================
  /** 
   * The IRNode representing the style (from a StyleSetModel)
   */
  public static final String STYLE      = "StyledView.STYLE";

  /** A component attribute representing the associated StyleSetModel
  public static final String PALETTE    = "StyledView.PALETTE";
   */

  /** 
   * Sets the style if valid; otherwise ignored
   * @param node The node being styled
   * @param o The IRNode representing the style in the palette
   */
  public void setStyle( IRNode node, IRNode o );

  /**
   * Returns the assigned style from the associated palette,
   * or null otherwise.
   */
  public IRNode getStyle( IRNode node );

  /** Return true if the node has a valid style */
  public boolean hasStyling( IRNode node );
}
