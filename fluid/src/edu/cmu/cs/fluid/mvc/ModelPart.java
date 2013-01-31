// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ModelPart.java,v 1.5 2004/06/25 17:16:06 boyland Exp $
/*
 * ModelPart.java
 *
 * Created on November 28, 2001, 10:39 AM
 */

package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.IRState;

/**
 * Abstract super class of all classes whose instances are used as mixins
 * in the implementation of a model.
 *
 * @author  Aaron Greenhouse
 */
public abstract class ModelPart extends edu.cmu.cs.fluid.ir.DefaultDescribe implements IRState {
  /** The model the core is a part of. */
  protected final Model partOf;

  /**
   * The object used the model to protect its structure.
   */
  protected final Object structLock;

  /**
   * Initialize the part.
   * @param model The model this instance is a part of.
   * @param lock The lock used to protect the state of the model.
   * @exception NullPointerException Thrown if the model or the lock are
   *            <code>null</code>.
   */
  protected ModelPart(final Model model, final Object lock) {
    if (model == null)
      throw new NullPointerException("Model is null.");
    if (lock == null)
      throw new NullPointerException("Lock is null.");
    partOf = model;
    structLock = lock;
  }
  
  /* (non-Javadoc)
   * Return the model this is part of.
   * @see edu.cmu.cs.fluid.ir.IRState#getParent()
   */
  @Override
  public IRState getParent() {
    return partOf;
  }
}
