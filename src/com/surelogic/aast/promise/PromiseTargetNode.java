
package com.surelogic.aast.promise;


import com.surelogic.aast.AASTNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;

public abstract class PromiseTargetNode extends AASTNode { 
  // Fields

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public PromiseTargetNode(int offset) {
    super(offset);
  }
  
  /**
   * Returns <code>true</code> if the target represented by this node (technically, the specific sub-class)
   * @param irNode
   * @return
   */
  public abstract boolean matches(IRNode irNode);
  
  /**
   * 
   * @param patternMods The modifiers for the pattern
   * @param declMods The modifiers for the declaration
   * @return
   */
	protected boolean matchesModifiers(int patternMods, int declMods) {
    if (patternMods == JavaNode.ALL_FALSE) {
      return true;
    }
    boolean rv = true;
    
    if (JavaNode.isSet(patternMods, JavaNode.STATIC)) {
      // check if static
      rv = rv && JavaNode.isSet(declMods, JavaNode.STATIC);
    }
    if (JavaNode.isSet(patternMods, JavaNode.INSTANCE)) {
      // check if instance
      rv = rv && !JavaNode.isSet(declMods, JavaNode.STATIC);
    }
    if (JavaNode.isSet(patternMods, JavaNode.PUBLIC)) {
      // check if public
      rv = rv && JavaNode.isSet(declMods, JavaNode.PUBLIC);
    }    
    if (JavaNode.isSet(patternMods, JavaNode.PROTECTED)) {
      // check if public
      rv = rv && JavaNode.isSet(declMods, JavaNode.PROTECTED);
    }    
    if (JavaNode.isSet(patternMods, JavaNode.PRIVATE)) {
      // check if public
      rv = rv && JavaNode.isSet(declMods, JavaNode.PRIVATE);
    }   
    return rv;
  }
}

