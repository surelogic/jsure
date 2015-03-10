/*
 * Created on Dec 4, 2003
 *
 */
package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A basic implementation for rules associated with a promise tag and Operators
 * 
 * @author chance
 */
public abstract class AbstractNamedPromiseRule extends AbstractPromiseRule {
  protected final String name;

  protected String ensureCapitalizedTag(String tag) {
    return PromiseFramework.ensureCapitalizedTag(tag);
  }
  
	protected AbstractNamedPromiseRule(String name, Operator op) {
		super(op);
    this.name = ensureCapitalizedTag(name);
	}  
  
  /**
   * @param ops
   */
  protected AbstractNamedPromiseRule(String name, Operator[] ops) {
    super(ops);
    this.name = ensureCapitalizedTag(name);
  }

  /**
   * Returns the promise tag for this rule
   */
  public final String name() {
    return name;
  }
}
