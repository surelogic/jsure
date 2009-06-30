/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeMergingManager.java,v 1.6 2003/07/15 21:47:18 aarong Exp $
 *
 * MergingInheritingAttributeManager.java
 * Created on January 22, 2002, 3:30 PM
 */

package edu.cmu.cs.fluid.mvc;



/**
 * Interface defining components that manage the merging of attributes.
 * An attribute that is merge-inherited is inherited from more
 * than one source model, but is presented as a single attribute.  How the
 * multiple source attributes are combined into the single attribute is 
 * dependent on the implementation of the interface.  For example, an
 * implementation could assume (with the client models enforcing) that the
 * source models have disjoint sets of nodes, in which case a merge could
 * simply be a union of the two attributes.  Another possible implementation 
 * is to provide a mechanism for dynamically switching between the source
 * attributes.
 *
 * <p>While not obvious from the interface, every
 * AttributeMergingManager must have a reference to the model's
 * attribute manager (see {@link edu.cmu.cs.fluid.mvc.AttributeMergingManager.Factory}).
 *
 * @author  Aaron Greenhouse
 */
public interface AttributeMergingManager
{
  //---------------------------------------------------------------------
  //-- Constants for Inheritance Modes
  //---------------------------------------------------------------------

  /**
   * Inheritance mode constant
   * meaning an attribute should be merge-inherited as immutable;
   * the attribute should not be inherited normally.
   */
  public static final Object IMMUTABLE_MERGED = new Object();
  
  /**
   * Inheritance mode constant
   * meaning an attribute should be inherited as
   * mutable, with the new values stored locally.
   * That is changes to the attribute are not propogated to the
   * model from which the attribute is inherited.
   * The attribute should not be inherited normally.  
   */
  public static final Object MUTABLE_LOCAL_MERGED = new Object();
  
  
  
  //=====================================================================
  //== New Model-level attribute Methods
  //=====================================================================

  /**
   * Merge-Inherit a model-level attribute from the given models.  It is up to
   * the implemenation of the method to determine whether the attribute should
   * be merge-inherited at all, and how the attribute is to be inherited.  It 
   * is expected that different implemenations will implement different ways
   * in which attributes may be inherited.
   *
   * @param models The models from which the attribute is be merge-inherited.
   * @param srcAttr The name of the attributes to inherit as an interned
   *   Strings.  The attributes correspond to the Models in the
   *   <code>models</code> attribute.
   * @param attr The name by which the attribute is to be known in this manager.
   * @param mode The mode in which to inherit that attribute.
   * @param kind The kind the inherited attribute should have.
   * @param cb The callback to use when the attribute is locally altered.
   *
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @throws IllegalArgumentException If the length of <code>models</code>
   *   and the length of <code>srcAttrs</code> are not the same.
   * @throws AttributeUnknownException Thrown if one of the source
   *    attributes is unknown.
   *
   * @return <code>true</code> if the attribute was successfully inherited,
   *         <code>false</code> if the manager determined the attribute should
   *         not be inherited.
   */ 
  public boolean mergeCompAttributes(
    Model[] models, String[] srcAttr, String attr,
    Object mode, int kind, AttributeChangedCallback cb );

  
  
  //=====================================================================
  //== New node-level attribute Methods
  //=====================================================================

  /**
   * Merge-Inherit a node-level attribute from the given models.  It is up to
   * the implemenation of the method to determine whether the attribute should
   * be merge-inherited at all, and how the attribute is to be inherited.  It 
   * is expected the different implemenations will implement different ways
   * in which attributes may be inherited.
   *
   * @param models The models from which the attribute is be inherited.
   * @param srcAttr The name of the attributes to inherit as an interned
   *   Strings.  The attributes correspond to the Models in the
   *   <code>models</code> attribute.
   * @param attr The name by which the attribute is to be known in this manager.
   * @param mode The mode in which to inherit that attribute.
   * @param kind The kind the inherited attribute should have.
   * @param cb The callback to use when the attribute is locally altered.
   *
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @throws IllegalArgumentException If the length of <code>models</code>
   *   and the length of <code>srcAttrs</code> are not the same.
   * @throws AttributeUnknownException Thrown if one of the source
   *    attributes is unknown.
   *
   * @return <code>true</code> if the attribute was successfully inherited,
   *         <code>false</code> if the manager determined the attribute should
   *         not be inherited.
   */ 
  public boolean mergeNodeAttributes(
    Model[] models, String[] srcAttr, String attr,
    Object mode, int kind, AttributeChangedCallback cb );

  
  
  //=====================================================================
  //== Property methods
  //=====================================================================

  /**
   * Get the value of a property.  This method must be implemented to be
   * interference free with the {@link #setProperty} method.
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
   * Set the value of a property.  This method must be implemented to be
   * interference free with the {@link #getProperty} method.
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
   * Interface for factories that create attribute merging managers.
   * (Here the reliance on an underlying attribute manager is exposed in
   * the create method.)
   *
   * @author Aaron Greenhouse
   */
  public interface Factory
  {
    /**
     * Create a new attribute merging manager for a particular model.
     * @param model The model whose attribute merging is to be managed
     *              by the new instance.
     * @param mutex The lock used to protect the state of the model.
     * @param attrManager The attribute manager of the model.
     */
    public AttributeMergingManager create( 
      Model model, Object mutex, AttributeManager attrManager );
  }
}

