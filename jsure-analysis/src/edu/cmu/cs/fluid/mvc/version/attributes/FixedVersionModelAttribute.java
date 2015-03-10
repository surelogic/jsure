/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/attributes/FixedVersionModelAttribute.java,v 1.8 2007/07/05 18:15:22 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.version.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * ComponentSlot wrapper intended for use by AttributeManagers that
 * projects a (versioned) attribute at a particualar version.  That
 * version may change over time, however, as it is refernced through
 * a VersionTracker.  The attribute is by necessity immutable.
 *
 * @author Aaron Greenhouse
 */
public class FixedVersionModelAttribute<T>
extends ComponentSlotWrapper<T>
{
  private final String errMsg;
  protected final VersionTracker fixedVersion;

  public FixedVersionModelAttribute(
    final String name, final ComponentSlot<T> wrapped,
    final VersionTracker vt, final String modelName )
  {
    super( wrapped );
    fixedVersion = vt;
    errMsg =   "Component-level attribute \"" + name + "\" of model \""
             + modelName + "\" is immutable.";
  }

  /**
   * Always returns <code>false</code>.
   */
  @Override
  public final boolean isChanged()
  {
    return false;
  }

  @Override
  public final boolean isValid()
  {
    try {
      Version.saveVersion();
      Version.setVersion( fixedVersion.getVersion() );
      return super.isValid();
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public final T getValue()
  {
    try {
      Version.saveVersion();
      Version.setVersion( fixedVersion.getVersion() );
      return projectValue( super.getValue() );
    } finally {
      Version.restoreVersion();
    }
  }

  protected T projectValue( final T value )
  {
    return value;
  }
    
  @Override
  public final Slot<T> setValue( final T value )
  {
    throw new SlotImmutableException( errMsg );
  }
}



