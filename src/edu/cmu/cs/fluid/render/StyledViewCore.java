package edu.cmu.cs.fluid.render;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * A View that adds a style attribute to an existing Model
 * 
 * @author Edwin Chan
 */
public class StyledViewCore extends AbstractCore {
  static void debug(String message) {
    Model.MV.fine(message);
  }

  //===========================================================
  //== Fields
  //===========================================================

  /** Maps IRNodes from a model to IRNodes in a palette */
  final SlotInfo style;

  /**
	 * The palette that IRNodes are being mapped to. (potentially shared between
	 * Styled models)
	 */
  final StyleSetModel palette;

  //===========================================================
  //== Constructors
  //===========================================================

  protected StyledViewCore(
    final String name,
    final Model model,
    final Object lock,
    final AttributeManager manager,
    final AttributeInheritanceManager inheritManager,
    final AttributeChangedCallback cb,
    final SlotFactory sf,
    final StyleSetModel p)
    throws SlotAlreadyRegisteredException {
    super(model, lock, manager);

    style =
      sf.newAttribute(name + "-" + StyledView.STYLE, IRNodeType.prototype);
    attrManager.addNodeAttribute(
      StyledView.STYLE,
      Model.INFORMATIONAL,
      true,
      style,
      cb);

    palette = p;

    ensureStyling(model); // FIX what if the attribute view breaks?
  }

  //===========================================================
  //== Attribute Convenience Methods
  //===========================================================

  /**
	 * Sets the style if valid; otherwise ignored
	 * 
	 * @param node
	 *          The node being styled
	 * @param o
	 *          The IRNode representing the style in the palette
	 */
  public void setStyle(final IRNode node, final IRNode o) {
    // FIX needs sync?
    if (palette.isPresent(o)) {
      synchronized (structLock) {
        node.setSlotValue(style, o);
      }
    }
  }

  /**
	 * Returns the assigned style from the associated palette, or null otherwise.
	 */
  public IRNode getStyle(final IRNode node) {
    if (node == null) {
      return null;
    }
    synchronized (structLock) {
      IRNode n = (IRNode) node.getSlotValue(style);
      if (!palette.isPresent(n)) {
        // FIX set it to null?
        return null;
      }
      return n;
    }
  }

  /** Return true if the node has a valid style */
  public boolean hasStyling(final IRNode node) {
    if (node == null) {
      return false;
    }
    synchronized (structLock) {
      if (node.valueExists(style)) {
        IRNode n = (IRNode) node.getSlotValue(style);
        // FIX set it to null if not present?
        return palette.isPresent(n);
      }
      return false;
    }
  }

  /**
	 * Ensures that every node in the Styled model has a style defined for it in
	 * the palette. If not, it creates one.
	 */
  public void ensureStyling(Model model) {
    synchronized (structLock) {
      // Assign default styles to attributes (if needed)
      Iterator it = model.getNodes();
      while (it.hasNext()) {
        IRNode attr = (IRNode) it.next();
        if (!this.hasStyling(attr)) {
          IRNode style = StylePalette.getNewStyle(palette);
          debug("Assigning a new style " + style + " to " + attr);
          this.setStyle(attr, style); // FIX
        }
      }
    }
  }
}
