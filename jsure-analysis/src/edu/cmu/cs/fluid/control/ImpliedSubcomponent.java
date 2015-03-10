package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.*;

/**
 * A subcomponent for a parse node that doesn't really exist in the syntax tree.
 * Children of this subcomponent are really children of a different node.
 * Used for {@link edu.cmu.cs.fluid.java.operator.ImpliedEnumConstantInitialization},
 * where we are really redirecting back to children of the EnumConstantDeclaration
 * that points to us.  The Component associated with the EnumContantDeclaration
 * is modified to forward requests for its subcomponents to the component 
 * associated wit hthe ImpliedEnumConstantInitialization.
 */
public class ImpliedSubcomponent extends Subcomponent {
  /**
   * Redirect attempts to get children to use this node as the parent
   * instead of the node in the parent component. 
   */
  private final IRNode syntax;
  
  /** The syntax tree for control-flow graph subcomponent nodes.
   * <b>This should be a parameter, not a constant.</b>
   */
  protected static final SyntaxTreeInterface tree = edu.cmu.cs.fluid.parse.JJNode.tree;

  public ImpliedSubcomponent(
    Component comp,
    IRNode syntax,
    IRLocation loc,
    int inputs,
    int outputs,
    int abrupts) {
    super(comp, loc, inputs, outputs, abrupts);
    this.syntax = syntax;
  }

  /** Return the node this subcomponent wraps. */
  @Override
  public IRNode getSyntax() {
    return tree.getChild(syntax, location);
  }
}
