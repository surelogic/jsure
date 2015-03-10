/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeManager.java,v 1.38 2007/07/10 22:16:30 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * An attribute manager is the piece of the model implementation 
 * that manages the
 * attributes of the model.  In particular, it provides the wrappers
 * around the attributes that make them safe to expose to Views, for 
 * example by enforcing immutability, or by always getting values from
 * a particular Version.  The attribute manager is not directly exposed,
 * but is delegated to by the ModelCore of a model implemenation.  The
 * contents of an attribute manager are protected by the structural lock
 * of the model it is associated with, and it is the responsibility of
 * the client of the attribute manager to acquire the appropriate lock.
 *
 * <p>An attribute (?) may be pre-loaded with attributes during its creation.
 * This is particularly important for attribute managers used by stateful
 * views, and for the proper inheritance of {@link Model#IS_ELLIPSIS}
 * {@link Model#ELLIDED_NODES} attributes.
 *
 * <p>An AttributeManager has a set of named properties.  There are no
 * standard default properties.  This feature exists to avoid having
 * to use typecasts in model implementations.  It is is used, for example,
 * to maintain a Version reference in
 * {@link edu.cmu.cs.fluid.mvc.version.FixedVersionAttributeManager}.
 *
 * @author Aaron Greenhouse
 */
public interface AttributeManager
{
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin node-to-attribute methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Get the name of the attribute identified with the given IRNode.
   * Caller must hold the lock of the associated model.
   * @param node The IRNode (representing an attribute) to query on.
   * @return An interned String giving the name of the attribute.
   * @exception UnknownAttributeException Thrown if the IRNode is not
   * identified with an attribute in this manager.
   */
  public String getAttributeName( IRNode node )
  throws UnknownAttributeException;

  /**
   * Get the name-space of the attribute identified with the given IRNode.
   * Caller must hold the lock of the associated model.
   * @param node The IRNode (representing an attribute) to query on.
   * @return <code>true</code> iff the attribute identified with given node
   *   is a node-level attribute.
   * @exception UnknownAttributeException Thrown if the IRNode is not
   * identified with an attribute in this manager.
   */
  public boolean isNodeAttribute( IRNode node )
  throws UnknownAttributeException;


  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End node-to-attribute methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin attribute manager properties
  //-----------------------------------------------------------
  //-----------------------------------------------------------

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
  //-- End attribute manager properties
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Component-level Attribute management methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Membership in component-level attributes
  //===========================================================

  /**
   * Get the names of the managed component-level attributes.
   * Caller must hold the lock of the associated model.
   * @return An Iterator over interned {@link java.lang.String}s.
   */
  public Iterator<String> getComponentAttributes();

  /**
   * Test if a given component-level attribute is recognized
   * by this manager.
   * Caller must hold the lock of the associated model.
   */
  public boolean isComponentAttribute( String attr );

  

  //===========================================================
  //== Primary methods to Add component-level attributes
  //===========================================================
  
  /**
   * Does the work of actually adding an attribute to the set of managed
   * model-level attributes.  Models are not expected to use this method 
   * directly, but rather indirectly through other components that assist 
   * in the addition of attributes, such as the
   * {@link AttributeInheritanceManager}.
   * Caller must hold the lock of the associated model.
   * @param attr The name of the component-level attribute to add; must be 
   * an interned String.
   * @param kind The kind of the attribute to add.
   * @param isMutable Whether the attribute should be treated as being
   * mutable by clients of the model.
   * @param wrapped The ComponentSlot to use, already suitable wrapped, etc.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   */ 
  public void addCompAttributeImpl(
    String attr, int kind, boolean isMutable, 
    ComponentSlot wrapped );

  /**
   * Primary method through which a model adds an attribute to the set of
   * managed model-level attributes.
   * Caller must hold the lock of the associated model.
   * @param attr The name of the component-level attribute.
   * @param isMutable <code>true</code> if clients <em>outside</em> of the
   * model should be allowed to modify the value of the attribute.
   * @param ca The underlying ComponentSlot to use.
   * @param cb The callback to use when the value changes.
   * @return The ComponentSlot, suitably wrapped for clients of the model.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   */ 
  public <T> ComponentSlot<T> addCompAttribute(
    String attr, int kind, boolean isMutable, ComponentSlot<T> ca,
    AttributeChangedCallback cb );



  //===========================================================
  //== Convienence methods to Add component-level attributes
  //===========================================================

  /**
   * Convienence method through which a model may add a new immutable
   * attribute to the set of managed model-level attributes.
   * Caller must hold the lock of the associated model.
   * @param attr The name of the model-level attribute.
   * @param ca The underlying ComponentSlot to use.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @return The ComponentSlot, suitably wrapped for clients of the model.
   */ 
  public <T> ComponentSlot<T> addCompAttribute( String attr, int kind,
                                         ComponentSlot<T> ca );

  /**
   * Convienence method through which a model may add a new mutable
   * attribute to the set of managed model-level attributes.
   * Caller must hold the lock of the associated model.
   * @param attr The name of the model-level attribute.
   * @param ca The underlying ComponentSlot to use.
   * @param cb The callback to use when the value changes.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @return The ComponentSlot, suitably wrapped for clients of the model.
   */ 
  public <T> ComponentSlot<T> addCompAttribute(
    String attr, int kind, ComponentSlot<T> ca, AttributeChangedCallback cb );

  

  //===========================================================
  //== Get info about component-level attributes
  //===========================================================

  /**
   * Get the kind of a component-level attribute.
   * Caller must hold the lock of the associated model.
   */
  public int getCompAttrKind( String attr );

  /**
   * Get the IRNode identified with a component-level attribute.
   * Caller must hold the lock of the associated model.
   */
  public IRNode getCompAttrNode( String attr );

  /**
   * Query if a model attribute is mutable.
   * Caller must hold the lock of the associated model.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public boolean isCompAttrMutable( String attr );

  /**
   * Get the Slot used to access the value of the component-level attribute.
   * Caller must hold the lock of the associated model.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public ComponentSlot getCompAttribute( String attr );

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Component-level Attribute management methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin node-level Attribute management methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Membership in node-level attributes
  //===========================================================

  /**
   * Get the names of the managed node-level attributes.
   * Caller must hold the lock of the associated model.
   * @return An Iterator over interned {@link java.lang.String}s.
   */
  public Iterator<String> getNodeAttributes();

  /**
   * Test if a given node attribute is recognized by this manager.
   * Caller must hold the lock of the associated model.
   */
  public boolean isNodeAttribute( String attr );



  //===========================================================
  //== Primary method to Add node-level attributes
  //===========================================================
  
  /**
   * Does the work of actually adding an attribute to the set of managed
   * node-level attributes.  Models are not expected to use this method 
   * directly, but rather indirectly through other components that assist 
   * in the addition of attributes, such as the
   * {@link AttributeInheritanceManager}.
   * Caller must hold the lock of the associated model.
   * @param attr The name of the attribute to add; must be an interned String.
   * @param kind The kind of the attribute.
   * @param isMutable Whether the attribute should be modifiable by
   * clients of the model.
   * @param srcs The source models whose union forms the domain of the
   *   attribute, or <code>null</code> if the domain is the model itself.
   * @param wrapped The attribute to add, appropriated wrapped, etc.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   */
  public void addNodeAttributeImpl(
    String attr, int kind, boolean isMutable, Model[] srcs, SlotInfo wrapped );

  /**
   * Primary method through which a model adds an attribute (whose domain
   * is the set of nodes in the mdoel) to the set of
   * managed node-level attributes.
   * Caller must hold the lock of the associated model.
   * @param attr The name of the attribute
   * @param kind The kind of the attribute.
   * @param mutable Whether the attribute should be modifiable by
   * clients of the model.
   * @param si The underlying storage for the attribute.
   * @param cb The callback to use when values in the attribute change.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @return The SlotInfo used to hold the attribute, suitable wrapped for
   *         use by clients of the model.
   */
  public <T> SlotInfo<T> addNodeAttribute(
    String attr, int kind, boolean mutable, SlotInfo<T> si,
    AttributeChangedCallback cb );

  /**
   * Primary method through which a model adds an attribute whose domain
   * is a subset of the union of some of the model's source models
   * to the set of managed node-level attributes.
   * Caller must hold the lock of the associated model.
   * @param attr The name of the attribute
   * @param kind The kind of the attribute.
   * @param mutable Whether the attribute should be modifiable by
   * clients of the model.
   * @param si The underlying storage for the attribute.
   * @param srcs The source models whose union forms the domain of the attribute.
   * @param cb The callback to use when values in the attribute change.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @return The SlotInfo used to hold the attribute, suitable wrapped for
   *         use by clients of the model.
   */
  public <T> SlotInfo<T> addNodeAttribute(
    String attr, int kind, boolean mutable, Model[] srcs, SlotInfo<T> si,
    AttributeChangedCallback cb );

                                

  //===========================================================
  //== Convienence methods to Add node-level attributes
  //===========================================================

  /**
   * Convienence method through which a model may add a new
   * mutable node-level attribute.
   * Caller must hold the lock of the associated model.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @return The SlotInfo used to hold the attribute, suitable wrapped for
   *         use by clients of the model.
   */
  public <T> SlotInfo<T> addNodeAttribute( String att, int kind, SlotInfo<T> si,
                                    AttributeChangedCallback callback );

  /**
   * Convienence method through which a model may add a new
   * immutable node-level attribute.
   * Caller must hold the lock of the associated model.
   * @exception AttributeAlreadyExistsException Thrown if a model-level
   *            attribute of the given name already exists in the manager.
   * @return The SlotInfo used to hold the attribute, suitable wrapped for
   *         use by clients of the model.
   */
  public <T> SlotInfo<T> addNodeAttribute( String att, int kind, SlotInfo<T> si );




  //===========================================================
  //== Get info about node-level attributes
  //===========================================================

  /**
   * Get the kind of the node-level attribute
   * Caller must hold the lock of the associated model.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public int getNodeAttrKind( String attr );

  /**
   * Get the IRNode identified with a node-level attribute.
   * Caller must hold the lock of the associated model.
   */
  public IRNode getNodeAttrNode( String attr );

  /**
   * Get whether the attribute should be modifiable by the
   * outside world.  
   * Caller must hold the lock of the associated model.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public boolean isNodeAttrMutable( String attr );

  /**
   * Get the domain identified with a node attribute.
   * Caller must hold the lock of the associated model.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public int getNodeAttrDomain( String att );

  /**
   * Get the source models whose union forms the domain identified with a
   * node attribute whose domain is {@link Model#SRC_DOMAIN}.
   * Caller must hold the lock of the associated model.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   * @exception IllegalArgumentException if the given attribute does not 
   * have a domain of {@link Model#SRC_DOMAIN}.
   */
  public Model[] getNodeAttrDomainSrcs( String att );

  /**
   * Get SlotInfo that should be used by model clients to access the attribute.
   * Caller must hold the lock of the associated model.
   * @exception UnknownAttributeException Thrown if the given attribute 
   * is not recognized.
   */
  public SlotInfo getNodeAttribute( String attr );

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End node-level Attribute management methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin factory interface
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Interface defining factories for creating attribute manager 
   * instances.  
   *
   * @author Aaron Greenhouse
   */
  public static interface Factory
  {
    /**
     * Create a new attribute manager instance.
     * @param model The model the attribute manager is for.
     * @param mutex The lock used to protect that model/attribute manager.
     */
    public AttributeManager create( Model model, Object mutex );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End factory interface
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}

