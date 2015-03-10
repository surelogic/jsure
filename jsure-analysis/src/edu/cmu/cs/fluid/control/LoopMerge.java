/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/LoopMerge.java,v 1.2 2007/07/10 22:16:36 aarong Exp $*/
package edu.cmu.cs.fluid.control;

/**
 * A merge node that handles control coming around in a loop.
 * The second input should be from the loop itself.
 * The first input should be from the initial state.
 * @author boyland
 */
public class LoopMerge extends Merge implements ComponentNode, MutableComponentNode {

  private Component component;
  
  /**
   * Create a loop merge node for the given loop.
   * @param comp CFG component for the loop
   */
  public LoopMerge(Component comp) {
    super();
    component = comp;
  }
  
  @Override
  public void setComponent(Component c) {
	component = c;
  }

  @Override
  public Component getComponent() {
    return component;
  }
}
