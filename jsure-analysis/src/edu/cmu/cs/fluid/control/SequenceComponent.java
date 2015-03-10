package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** A component in which all children are executed in sequence.
 * This class only handles variable number of children.
 * @see SimpleComponent
 */
@SuppressWarnings("unused")
public class SequenceComponent extends VariableComponent {
  /** Execute the children in order. */
  public SequenceComponent(IRNode node) {
    super(node,2,1,1,1);
    ControlNode never = new Never();

    ControlEdge startExec =
      new VariableSubcomponentControlEdge(variable,0,false,entryPort);
    ControlEdge startAbrupt =
      new VariableSubcomponentControlEdge(variable,1,false,never);
    
    ControlEdge endExec =
      new VariableSubcomponentControlEdge(variable,0,true,normalExitPort);
    ControlEdge endAbrupt =
      new VariableSubcomponentControlEdge(variable,1,true,abruptExitPort);
  }

  /** Execute the children in order and then
   * execute some language-specific action.
   */
  public SequenceComponent(IRNode node, ComponentFlow cf) {
    super(node,2,1,1,1);
    cf = new ComponentFlow(this,cf.getInfo());
    ControlNode never = new Never();

    ControlEdge startExec =
      new VariableSubcomponentControlEdge(variable,0,false,entryPort);
    ControlEdge startAbrupt =
      new VariableSubcomponentControlEdge(variable,1,false,never);
    
    ControlEdge endExec =
      new VariableSubcomponentControlEdge(variable,0,true,cf);
    ControlEdge endAbrupt =
      new VariableSubcomponentControlEdge(variable,1,true,abruptExitPort);

    ControlEdge.connect(cf,normalExitPort);
  }

  /** Execute the children in order and then
   * execute some language-specific action.
   * @param cf1 flow in the event of normal termination
   * @param cf2 flow in the event of abrupt termination
   */
  public SequenceComponent(IRNode node, ComponentFlow cf1, ComponentFlow cf2) {
    super(node,2,1,1,1);
    cf1 = new ComponentFlow(this,cf1.getInfo());
    cf2 = new ComponentFlow(this,cf2.getInfo());
    ControlNode never = new Never();

    ControlEdge startExec =
      new VariableSubcomponentControlEdge(variable,0,false,entryPort);
    ControlEdge startAbrupt =
      new VariableSubcomponentControlEdge(variable,1,false,never);
    
    ControlEdge endExec =
      new VariableSubcomponentControlEdge(variable,0,true,cf1);
    ControlEdge endAbrupt =
      new VariableSubcomponentControlEdge(variable,1,true,cf2);

    ControlEdge.connect(cf1,normalExitPort);
    ControlEdge.connect(cf2,abruptExitPort);
  }

  // useless as it stands: choice nodes cannot feed directly into
  // abrupt exit ports.
  private SequenceComponent(IRNode node, ComponentChoice cc) {
    super(node,2,1,1,1);
    cc = new ComponentChoice(this,cc.getInfo());
    ControlNode never = new Never();
    ControlNode merge = new Merge();

    ControlEdge startExec =
      new VariableSubcomponentControlEdge(variable,0,false,entryPort);
    ControlEdge startAbrupt =
      new VariableSubcomponentControlEdge(variable,1,false,never);
    
    ControlEdge endExec =
      new VariableSubcomponentControlEdge(variable,0,true,cc);
    ControlEdge endAbrupt =
      new VariableSubcomponentControlEdge(variable,1,true,merge);

    ControlEdge.connect(cc,normalExitPort);
    ControlEdge.connect(cc,merge);
    ControlEdge.connect(merge,abruptExitPort);
  }
  @Override
  protected VariableSubcomponent createVariableSubcomponent(IRLocation loc) {
    return new SequenceSubcomponent(this,loc);
  }
}
