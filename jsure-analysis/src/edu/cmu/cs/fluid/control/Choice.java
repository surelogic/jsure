/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Choice.java,v 1.7 2005/05/19 23:22:21 chance Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** Points where depending on some program condition, control could
 * go in one of two ways.  A constant tag distinguishes various
 * kinds of choice nodes.  The subclass gives a way to attach this
 * choice to a particular syntax node.
 * @author John Tang Boyland
 * @see ComponentChoice
 * @see SubcomponentChoice
 */
public abstract class Choice extends Split {
  private final Object value;
  public Choice(Object info) { value = info; }
  public Object getValue() {
    return value;
  }
  public Object getInfo() {
    return value;
  }
  public abstract IRNode getSyntax();

}
