// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AbstractCore.java,v 1.11 2005/05/20 15:48:02 chance Exp $
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.EnumerationAlreadyDefinedException;
import edu.cmu.cs.fluid.ir.IREnumeratedType;

/**
 * Root class of all Core implementation classes.
 */
public abstract class AbstractCore extends ModelPart {
  /**
   * The attribute manager of the model.
   */
  protected final AttributeManager attrManager;

  /**
   * Initialize the abstract core.
   * @param model The model this instance is a part of.
   * @param lock The lock used to protect the state of the model.
   * @param manager The attribute manager of the model.
   * @exception NullPointerException Thrown if the model, lock, or attribute
   *            manager are <code>null</code>.
   */
  public AbstractCore(
    final Model model,
    final Object lock,
    final AttributeManager manager) {
    super(model, lock);
    if (manager == null) {
      throw new NullPointerException("Attribute manager is null.");
    }
    attrManager = manager;
  }

  /**
   * Return the {@link IREnumeratedType} with the given name and elements.
   */
  public static IREnumeratedType newEnumType(
    final String name,
    final String[] elts) {
    IREnumeratedType enm = null;

    if (IREnumeratedType.typeExists(name)) {
      enm = IREnumeratedType.getIterator(name);
    } else {
      try {
        enm = new IREnumeratedType(name, elts);
      } catch (EnumerationAlreadyDefinedException e) {
        // This shouldn't happed because of the above condtional
        enm = IREnumeratedType.getIterator(name);
        //System.err.println(        
        //    "Error intializing class edu.cmu.cs.fluid.mvc.PredicateModelCore: "
        //  + "received \"EnumerationAlreadyDefinedException\" for "
        //  + PredicateModel.VISIBLE_ENUM );
      }
    }
    return enm;
  }
}
