/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/attributes/FixedVersionNodeAttribute.java,v 1.8 2007/07/05 18:15:22 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.version.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * SlotInfo wrapper intended for use by AttributeManagers that
 * projects a (versioned) attribute at a particualar version.  That
 * version may change over time, however, as it is refernced through
 * a VersionTracker.  The attribute is by necessity immutable.
 *
 * @author Aaron Greenhouse
 */
public class FixedVersionNodeAttribute<T>
extends SlotInfoWrapper<T>
{
  private final String attr;
  protected final VersionTracker fixedVersion;
  private final Model partOf;
    
  public FixedVersionNodeAttribute(
    final Model partOf, final String name, final SlotInfo<T> wrapped,
    final VersionTracker vt )
  {
    super( wrapped );
    this.attr = name;
    this.fixedVersion = vt;
    this.partOf = partOf;
  }

  @Override
  protected final boolean valueExists( final IRNode node )
  {
    if( partOf.isAttributable( node, attr ) ) {
      try {
        Version.saveVersion( fixedVersion.getVersion() );
        return super.valueExists( node );
      } finally {
	Version.restoreVersion();
      }
    } else {
      return false;
    }
  }

  @Override
  protected final void setSlotValue( final IRNode node, final T newValue )
  throws SlotImmutableException
  {
    throw new SlotImmutableException( 
        "Attribute \"" + attr + "\" in model \""
      + partOf.getName() + "\" is Immutable." );
  }
    
  @Override
  protected final T getSlotValue( final IRNode node )
  throws SlotUndefinedException
  {
    if( partOf.isAttributable( node, attr ) ) {
      try {
	Version.saveVersion( fixedVersion.getVersion() );
	return projectValue( super.getSlotValue( node ) );
      } finally {
	Version.restoreVersion();
      }
    } else {
      throw new SlotUndefinedException(
          "Node " + node + " is not in model \""
        + partOf.getName() + "\"." );
    }
  }
    
  protected T projectValue( final T value )
  {
    return value;
  }
}