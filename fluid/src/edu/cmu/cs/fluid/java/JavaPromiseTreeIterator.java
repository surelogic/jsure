/* $Header$ */
package edu.cmu.cs.fluid.java;


import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.DepthFirstSearch;

/**
 * TODO only goes one level deep into promises
 * @author Edwin
 */
public class JavaPromiseTreeIterator extends DepthFirstSearch {
  /* more state */
  private int promiseIndex;
  private IRLocation ploc;
  private Object promise;

  private static Integer[] integers =
      new Integer[JavaPromise.getPromiseChildrenInfos().length+1];
      
  static {
    for (int i=0; i <= JavaPromise.getPromiseChildrenInfos().length; ++i) {
      integers[i] = new Integer(i);
    }
  }

  public JavaPromiseTreeIterator(IRNode root, boolean bottomUp) {
    super(JJNode.tree,root,bottomUp);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void pushState() {
    super.pushState();
    stack.push(integers[promiseIndex]);
    stack.push(ploc);
    stack.push(promise);
    //System.out.println("Pushing "+promise);
    /*
    if (promise instanceof IRNode) {
    	IRNode n = (IRNode) promise;
    	if (n.identity() == IRNode.destroyedNode) {
    		System.out.println("Pushing destroyed node");
    	}
    }
    */
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void popState() {
    promise = stack.pop();
    ploc = (IRLocation)stack.pop();
    promiseIndex = ((Integer)stack.pop()).intValue();
    super.popState();
  }

  @Override
  protected void visit(IRNode node) {
    super.visit(node);
    promiseIndex = 0;
    setPromiseLocation(node);
  }

  @SuppressWarnings("unchecked")
  protected final void setPromiseLocation(IRNode node) {
    while (promiseIndex < JavaPromise.getPromiseChildrenInfos().length) {
      SlotInfo si = JavaPromise.getPromiseChildrenInfos()[promiseIndex];
      if (node.valueExists(si)) {
    	  promise = node.getSlotValue(si);
    	  if (promise instanceof IRSequence) {
    		  if (((IRSequence)promise).hasElements()) {
    			  ploc = ((IRSequence)promise).firstLocation();
    			  return;
    		  }
    	  } else if (promise instanceof IRNode) {
    		  IRNode n = (IRNode) promise;
    		  if (n.identity() != IRNode.destroyedNode) {
    			  return;
    		  } else {
    			  //System.out.println("ignore, and continue in loop");
    		  }
    	  } else if (promise != null) {
    		  return;
    	  }
      }
      ++promiseIndex;
    }
  }

  @Override
  protected boolean additionalChildren(IRNode node) {
    while (promiseIndex < JavaPromise.getPromiseChildrenInfos().length) {
      if (promise instanceof IRSequence) {
        if (ploc == null) {
          ++promiseIndex;
          setPromiseLocation(node);
        }
        else {
          IRNode newNode = (IRNode) ((IRSequence) promise).elementAt(ploc);
          ploc = ((IRSequence) promise).nextLocation(ploc);
          if (newNode != null && newNode.identity() != IRNode.destroyedNode) {
            visit(newNode);
            return true;
          }
        }
      }
      else {
        IRNode newNode = (IRNode) promise;
        ++promiseIndex;
        setPromiseLocation(node);
        visit(newNode);
      }
    }
    return false;
  }

  @Override
  protected boolean mark(IRNode node) {
    return node != null;
  }
}
