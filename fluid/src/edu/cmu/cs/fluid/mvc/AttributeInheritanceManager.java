/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeInheritanceManager.java,v 1.8 2007/06/04 16:55:01 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;


/**
 * Interface for model helper that managers the inheritance of attributes
 * from source models.  While not obvious from the interface, every
 * AttributeInheritanceManager must have a reference to the model's
 * attribute manager (see {@link edu.cmu.cs.fluid.mvc.AttributeInheritanceManager.Factory}).
 *
 * @author Aaron Greenhouse
 */
public interface AttributeInheritanceManager
{
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Constants for Inheritance Modes
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Constant meaning an attribute should be inherited as immutable.
   */
  public static final Object IMMUTABLE = "immutable";
  
  /**
   * Constant meaning an attribute should be inherited as
   * mutable, with the new values stored locally.  That is
   * changes to the attribute are not propogated to the
   * model from which the attribute is inherited.
   */
  public static final Object MUTABLE_LOCAL = "mutableLocal";
  
  /**
   * Constant meaning an attribute should be inherited as
   * mutable, with the new values stored in the original
   * attribute.  This should not be used if values are 
   * going to be set for nodes that are not part of
   * the original model.
   */
  public static final Object MUTABLE_SOURCE = "mutableSource";

    
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- New Model-level attribute Methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  
  /**
   * Inherit a model-level attribute from another model.  It is up to the
   * implemenation of the method to determine whether the attribute should
   * be inherited at all, and how the attribute is to be inherited.  It 
   * is expected that different implemenations will implement different ways
   * in which attributes may be inherited.
   *
   * @param model The model from which the attribute is be inherited.
   * @param srcAttr The name of the attribute to inherit as an interned String.
   * @param attr The name by which the attribute is to be known in this manager  as an interned String.
   * @param mode The mode in which to inherit that attribute.
   * @param kind The kind the inherited attribute should have.
   * @param cb The callback to use when the attribute is locally altered.
   *
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @exception IllegalArgumentException Thrown if the inheritance mode is
   *            not supported by the inheritance manager.
   *
   * @return <code>true</code> if the attribute was successfully inherited,
   *         <code>false</code> if the manager determined the attribute should
   *         not be inherited.
   */ 
  public boolean inheritCompAttribute(
    Model model, String srcAttr, String attr, 
    Object mode, int kind, AttributeChangedCallback cb );

  /**
   * Inherit a node-level attribute from another model.  It is up to the
   * implemenation of the method to determine whether the attribute should
   * be inherited at all, and how the attribute is to be inherited.  It 
   * is expected the different implemenations will implement different ways
   * in which attributes may be inherited.
   *
   * @param model The model from which the attribute is be inherited.
   * @param srcAttr The name of the attribute to inherit as an interned String.
   * @param attr The name by which the attribute is to be known in this manager  as an interned String.
   * @param mode The mode in which to inherit that attribute.
   * @param kind The kind the inherited attribute should have.
   * @param cb The callback to use when the attribute is locally altered.
   *
   * @exception AttributeAlreadyExistsException Thrown if a node-level
   *            attribute of the given name already exists in the manager.
   * @exception IllegalArgumentException Thrown if the inheritance mode is
   *            not supported by the inheritance manager.
   *
   * @return <code>true</code> if the attribute was successfully inherited,
   *         <code>false</code> if the manager determined the attribute should
   *         not be inherited.
   */ 
  public boolean inheritNodeAttribute(
    Model model, String srcAttr, String attr, 
    Object mode, int kind, AttributeChangedCallback cb );

  /**
   * Inherit attributes from a particular model.  The given
   * inheritance policy determines which attributes are actually
   * inherited.
   * @param srcModel The model from which attributes are inherited.
   * @param policy The policy describing which attributes to inherit, and how.
   * @param cb The callback to use when the attributes are locally mutated.
   *
   * @exception AttributeAlreadyExistsException Thrown if the policy
   *            causes an attribute to be inherited with the same name as
   *            an already existing attribute.
   * @exception IllegalArgumentException Thrown if the policy tries to use 
   *            an inheritance mode not understood by the manager.
   */
  public void inheritAttributesFromModel(
    Model srcModel, AttributeInheritancePolicy policy,
    AttributeChangedCallback cb );



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin attribute manager properties
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Get the value of a property.
   * @param property The name of the property as in interned String 
   *                 whose value is to be returned.
   * @return The value of the property.
   * @exception IllegalArgumentException Thrown if the property is not
   *            understood.  This seems like a better idea than returning null
   *            because properties are intended to deal with special cases 
   *            of attribute manager implementations, and therefore trying to
   *            get the wrong property would indicate an error in the
   *            implementation of the Model that is using the manager.
   */
  public Object getProperty( String property );
  
  /**
   * Set the value of a property.
   * @param property The name of the property as in interned String 
   *                 whose value is to be set.
   * @param value The new value.
   * @exception IllegalArgumentException Thrown if the property is not
   *            understood.  This seems like a better idea than silently
   *            failing because properties are intended to deal with special
   *            cases of attribute manager implementations, and therefore
   *            trying to set the wrong property would indicate an error in the
   *            implementation of the Model that is using the manager.
   *
   *            <p>May also be thrown if the value is unacceptable.
   */
  public void setProperty( String property, Object value );



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Factory
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Interface for factories that create attribute inheritance managers.
   * (Here the reliance on an underlying attribute manager is exposed in
   * the create method.)
   *
   * @author Aaron Greenhouse
   */
  public interface Factory
  {
    /**
     * Create a new attribute inheritance manager for a particular model.
     * @param model The model whose attribute inheritance is to be managed
     *              by the new instance.
     * @param mutex The lock used to protect the state of the model.
     * @param attrManager The attribute manager of the model.
     */
    public AttributeInheritanceManager create( 
      Model model, Object mutex, AttributeManager attrManager );
  }
}
