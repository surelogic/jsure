package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * An attribute manager factory that produces inheriting attribute managers
 * suitable for stateful views that produce
 * fixed version projections of a <em>single</em> model.
 * Inherited attributes are wrapped to always appear at a fixed
 * version and to be immutable&mdash;mutability doesn't make any
 * sense because the new value wouldn't appear in the projection,
 * and would always cause a new version to bud from the version
 * into which the model is being projected.
 *
 * <p>Generated attribute managesr understand the property {@link FixedVersionSupport#VERSION}
 * for maintaining the version at which attribute values are presented.
 *
 * <p>Handles IRSequence-valued attributes by projecting 
 * IRSequences into the fixed version as well.  <em>Does not 
 * specially handle any other types of values</em>.  Changing
 * the {@link FixedVersionSupport#VERSION} property will change the version at which
 * any such IRSequences are presented.
 */
public class FixedVersionAttributeManagerFactory
implements AttributeInheritanceManager.Factory
{
  private final Version version;

  public FixedVersionAttributeManagerFactory( final Version v )
  { 
    this.version = v;
  }

  /**
   * Create a new attribute inheritance manager for a particular model.
   * @param model The model whose attribute inheritance is to be managed
   *             by the new instance.
   * @param mutex The lock used to protect the state of the model.
   * @param attrManager The attribute manager of the model.
   */
  @Override
  public AttributeInheritanceManager create(
    final Model model, final Object mutex, final AttributeManager attrManager )
  {
    return new FixedVersionAttributeManager( model, mutex, attrManager, version );
  }
  
}

/**
 * An attribute manager suitable for stateful views that produce
 * fixed version projections of a <em>single</em> model.
 * Inherited attributes are wrapped to always appear at a fixed
 * version and to be immutable&mdash;mutability doesn't make any
 * sense because the new value wouldn't appear in the projection,
 * and would always cause a new version to bud from the version
 * into which the model is being projected.
 *
 * <p>Understands the property {@link FixedVersionSupport#VERSION} for maintaining
 * the version at which attribute values are presented.
 *
 * <p>Handles IRSequence-valued attributes by projecting 
 * IRSequences into the fixed version as well.  <em>Does not 
 * specially handle any other types of values</em>.  Changing
 * the {@link FixedVersionSupport#VERSION} property will change the version at which
 * any such IRSequences are presented.
 */
final class FixedVersionAttributeManager
extends BareAttributeInheritanceManager
{
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin fields
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Fields for storing property values
  //===========================================================

  /**
   * Indirect reference to the version at which attribute values are given.
   * This reference is shared with all the wrapped attributes.
   */
  protected final VersionTracker fixedVersion;

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End fields
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Constructors
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Creates a new <code>FixedVersionAttributeManager</code> instance.
   * Does not initialize any attributes.  
   *
   * @param partOf The model this manager is a part of.
   * @param mutex The mutex used to protect this model.
   * @param version The initial version to use.
   */
  protected FixedVersionAttributeManager(
    final Model partOf, final Object mutex, 
    final AttributeManager attrManager, final Version version )
  {
    this( partOf, mutex, attrManager, new VersionMarker( version ) );
  }

  // Hack so that the FixedVersionInheritedAttributeBuilder can share
  // the version tracker referenced by fixedVersion.
  private FixedVersionAttributeManager(
    final Model partOf, final Object mutex,
    final AttributeManager attrManager, final VersionTracker vt )
  {
    super( partOf, mutex, attrManager,
      new FixedVersionInheritedAttributeBuilderFactory( vt )  );
    fixedVersion = vt;
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Constructors
  //-----------------------------------------------------------
  //-----------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Property methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Implemenation understand the {@link FixedVersionSupport#VERSION} property.
   */
  @Override
  public final void setProperty( final String property, final Object version )
  {
    if( property == FixedVersionSupport.VERSION ) {
      try {
        fixedVersion.setVersion( (Version)version );
      } catch( final ClassCastException e ) {
        throw new IllegalArgumentException(
            "Property \"" + FixedVersionSupport.VERSION
          + "\" requires a fluid.version.Version, received a "
          + version.getClass().getName() );
      }
    } else {
      super.setProperty( property, version );
    }
  }

  /**
   * Implemenation understand the {@link FixedVersionSupport#VERSION} property.
   */
  @Override
  public final Object getProperty( final String property )
  {
    if( property == FixedVersionSupport.VERSION ) {
      return fixedVersion.getVersion();
    } else {
      return super.getProperty( property );
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Property methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}


