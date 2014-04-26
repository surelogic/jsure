/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/VariableComponent.java,v 1.4 2005/05/23 18:32:39 chance Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Components with a variable number of children to be executed.
 * We construct a control-flow graph with a special
 * <tt>VariableSubcomponent</tt> around each child.
 * There is another Variable Subcomponent that logically sits
 * before the first and after the last component.
 * The variable components have special variable edges which are
 * connected up in parallel with the ones before and after.
 * These edges are then used to connect the actual subcomponent
 * for the child with the variable subcomponent around it.
 * @see SequenceComponent
 * @see VariableSubcomponent
 */
public abstract class VariableComponent extends Component {
  /** The variable subcomponent serves as both first and last
   * subcomponent around the elements in the sequence.
   * In effect it is "inside out", because the parts of the edges
   * technically outside the subcomponent are
   * connected with the first and last true
   * subcomponents in the sequence, whereas the inside is connected
   * to the outer context edges.  Perhaps it would be cleaner to have
   * two separate variable subcomponents, one without only output
   * edges and one with only input edges, but that would have complicated
   * matters.  As it stands when there are no children, the exit edges
   * connect immediately and in parallel with the entry edges.
   */
  protected final VariableSubcomponent variable;

  public VariableComponent(IRNode node, int numEdges) {
    super(node);
    variable = new VariableSubcomponent(this,null,numEdges);
  }
  public VariableComponent(IRNode node, int numEdges,
			   int numEntry, int numNormal, int numAbrupt) {
    super(node,numEntry,numNormal,numAbrupt);
    variable = new VariableSubcomponent(this,null,numEdges);
  }
  @Override
  public VariableSubcomponent getVariableSubcomponent() {
    return variable;
  }
  @Override
  public ISubcomponent getSubcomponent(IRLocation loc) {
    ISubcomponent sub = super.getSubcomponent(loc);
    if (sub == null)
      sub = createVariableSubcomponent(loc);
    return sub;
  }

  abstract protected VariableSubcomponent
       createVariableSubcomponent(IRLocation loc);
}
