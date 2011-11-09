/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/AbstractSequenceToSequenceStatefulView.java,v 1.14 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.AttributeInheritanceManager;
import edu.cmu.cs.fluid.mvc.AttributeManager;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Default implementation of a SequenceStatefulView.  
 * Implements the methods to delegate to the 
 * provided core implementations. 
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractSequenceToSequenceStatefulView
extends AbstractModelToSequenceStatefulView
implements SequenceToSequenceStatefulView
{
  /* The sequence model being viewed */
  protected final SequenceModel srcModel;



  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a new SeqeunceStatefulView.
   * @param name The name of the stateful view
   * @param mf The factory to use to create the model core
   * @param vf The factory to use to create the view core
   * @param smf The factory to use to create the sequence model core.
   */
  // Does not init source models attribute
  public AbstractSequenceToSequenceStatefulView(
    final String name, final SequenceModel src,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final SequenceModelCore.Factory smf, 
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, smf, attrFactory, inheritFactory );
    srcModel = src;
  }
}

