/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/FixedVersionInheritedAttributeBuilderFactory.java,v 1.7 2006/03/30 16:20:26 chance Exp $ */
package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;

import edu.cmu.cs.fluid.mvc.version.attributes.*;

/**
 * Factory returning an attribute builder that wraps attributes so they are
 * always presented at a particular version.  The returned builder can handle
 * attributes whose values are sequences, but does not currently handle any
 * other structured values.
 */
public final class FixedVersionInheritedAttributeBuilderFactory
implements BareAttributeInheritanceManager.InheritedAttributeBuilderFactory
{
  /**
   * The VersionTracker the builder should use when wrapping attributes.
   */
  private final VersionTracker tracker;
  
  /**
   * Construct a factory instance that always returns builders that use a 
   * particular VersionTracker when they wrap the attributes.
   */
  public FixedVersionInheritedAttributeBuilderFactory( final VersionTracker vt )
  {
    tracker = vt;
  }
  
  // inherit javadoc.
  @Override
  public BareAttributeInheritanceManager.InheritedAttributeBuilder create()
  {
    return new FixedVersionInheritedAttributeBuilder( tracker );
  }
}



/**
 * Attribute builder that wraps attributes so they are always
 * presented at a particular version.  Can handle attributes whose values are
 * sequences, but does not currently handle any other structured values.
 */
final class FixedVersionInheritedAttributeBuilder
implements BareAttributeInheritanceManager.InheritedAttributeBuilder
{
  /**
   * The VersionTracker shared amongst the model and the attribute wrappers.
   */
  private final VersionTracker vt;
    
  public FixedVersionInheritedAttributeBuilder( final VersionTracker vt )
  {
    this.vt = vt;
  }
    
  /**
   * Generates an attribute wrapped by
   * {@link FixedVersionModelAttribute},
   * or {@link SequentialFixedVersionModelAttribute}
   * if the attribute's type is IRSequence.
   */ 
  @Override
  @SuppressWarnings("unchecked")
  public ComponentSlot buildCompAttribute(
    final Model partOf, final Object mutex, final String attr,
    final Object mode, final ComponentSlot ca, 
    final AttributeChangedCallback cb )
  {
    ComponentSlot wrapped = null;
    if( ca.getType() instanceof IRSequence ) {
      wrapped = new SequentialFixedVersionModelAttribute(
                      attr, ca, vt, partOf.getName() );
    } else {
      wrapped = new FixedVersionModelAttribute( attr, ca, vt, partOf.getName() );
    }
    return wrapped;
  }
  
  /**
   * Generates an attribute wrapped by
   * {@link FixedVersionNodeAttribute},
   * or {@link SequentialFixedVersionNodeAttribute}
   * if the attribute's type is IRSequence.
   */
  @Override
  @SuppressWarnings("unchecked")
  public SlotInfo buildNodeAttribute(
    final Model partOf, final Object mutex, final String attr,
    final Object mode, final SlotInfo si, final AttributeChangedCallback cb )
  {
    SlotInfo wrapped = null;
    if( si.type() instanceof IRSequenceType ) {
      wrapped = new SequentialFixedVersionNodeAttribute( partOf, attr, si, vt );
    } else {
      wrapped = new FixedVersionNodeAttribute( partOf, attr, si, vt );
    }
    return wrapped;
  }
  
  /**
   * Considers no modes to be mutable.
   */
  @Override
  public boolean isModeMutable( final Object mode )
  {
    return false;
  }
}
