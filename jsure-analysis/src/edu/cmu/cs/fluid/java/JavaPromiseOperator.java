/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaPromiseOperator.java,v 1.9 2007/07/10 22:16:32 aarong Exp $ */
package edu.cmu.cs.fluid.java;

public abstract class JavaPromiseOperator extends JavaOperator implements JavaPromiseOpInterface {
	static {
    /* Promises have to be derived anyway.  Eventually they shouldn't be IRNodes.
		JavaPromise.treeChanged.addRootObserver(new Observer() {
			public void update(Observable obs, Object x) {
				IRNode node = (IRNode) x;
				try {
          if (JavaPromise.tree.opExists(node)) {          
  					Operator op = JavaPromise.tree.getOperator(node);
  					if (op instanceof JavaPromiseOperator) {
	  					IRNode promisedFor = JavaPromise.getPromisedFor(node);
		  				JavaPromise.treeChanged.noteChange(promisedFor);
			  		}
          }
				}
				catch (SlotUndefinedException e) {}
			}
		});*/
	}
  
  public JavaNode createPromise() {
    return JavaPromise.makeJavaPromise(this);
  }
}
