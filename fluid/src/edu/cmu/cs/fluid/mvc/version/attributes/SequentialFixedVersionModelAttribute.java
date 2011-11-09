/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/attributes/SequentialFixedVersionModelAttribute.java,v 1.8 2007/07/05 18:15:22 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.version.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * Wrapper for versioned model attributes that return (versioned) sequences.
 * Meant to be used by AttributeManagers to produce attributes that return 
 * sequences at "fixed" versions.  The version can change because it is
 * referred to through a VersionTracker.
 *
 * @author Aaron Greenhouse
 */
public class SequentialFixedVersionModelAttribute<T extends IRSequence>
extends FixedVersionModelAttribute<T>
{
  public SequentialFixedVersionModelAttribute(
    final String name, final ComponentSlot<T> wrapped,
    final VersionTracker vt, final String modelName )
  {
    super( name, wrapped, vt, modelName );
  }

  @Override
  @SuppressWarnings("unchecked")
  protected T projectValue( final T value )
  {
    return   (value == null)
           ? null
           : (T) new VersionedSequenceProjection( value,
                                              this.fixedVersion );
  }
}
