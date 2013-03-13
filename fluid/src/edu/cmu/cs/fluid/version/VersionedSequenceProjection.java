// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedSequenceProjection.java,v 1.9 2007/07/10 22:16:32 aarong Exp $
package edu.cmu.cs.fluid.version;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.IRSequenceWrapper;
import edu.cmu.cs.fluid.ir.InsertionPoint;

/**
 * Projects the contents of a (versioned) sequence into a particular version
 * version.  The version at which the sequence is presented is managing using
 * a {@link VersionTracker}, enabling the version to be altered outside of
 * the control of the projection itself.  If you want the version to be fixed
 * for all time, use the
 * {@link #VersionedSequenceProjection(IRSequence,Version)}
 * constructor, which causes an unique VersionTracker to be used.
 *
 * @author Aaron Greenhouse
 */
public class VersionedSequenceProjection<T>
extends IRSequenceWrapper<T>
{
  /**
   * Indirect reference to the version at which the sequence should be 
   * presented.
   */
  private final VersionTracker fixedVersion;
  
  public VersionedSequenceProjection(
    final IRSequence<T> seq, final VersionTracker vt )
  {
    super( seq );
    fixedVersion = vt;
  }
  
  public VersionedSequenceProjection( final IRSequence<T> seq, final Version v )
  {
    this( seq, new VersionMarker( v ) );
  }
   
  @Override public int size()
  {
    try {
      Version.saveVersion( fixedVersion.getVersion() );
      return super.size();
    } finally {
      Version.restoreVersion();
    }
  }
    
  @Override public boolean hasElements()
  {
    try {
      Version.saveVersion( fixedVersion.getVersion() );
      return super.hasElements();
    } finally {
      Version.restoreVersion();
    }
  }
    
  @Override public Iteratable<T> elements()
  {
    try {
      Version.saveVersion( fixedVersion.getVersion() );
      return super.elements();
    } finally {
      Version.restoreVersion();
    }
  }
    
  @Override public boolean validAt( final IRLocation loc )
  {
    try {
      Version.saveVersion( fixedVersion.getVersion() );
      return super.validAt( loc );
    } finally {
      Version.restoreVersion();
    }
  }
    
  @Override public T elementAt( final IRLocation loc )
  {
    try {
      Version.saveVersion( fixedVersion.getVersion() );
      return super.elementAt( loc );
    } finally {
      Version.restoreVersion();
    }
  }
    
  // This is stupid...
  @Override public void setElementAt( final T element, final IRLocation loc )
  {
    try {
      Version.saveVersion( fixedVersion.getVersion() );
      super.setElementAt( element, loc );
    } finally {
      Version.restoreVersion();
    }
  }
    
  // This is stupid...
  @Override public IRLocation insertElementAt( final T element, final InsertionPoint ip)
  {
    try {
      Version.saveVersion( fixedVersion.getVersion() );
      return ip.insert( sequence, element );
    } finally {
      Version.restoreVersion();
    }
  }
    
  // This is stupid...
  @Override public void removeElementAt( final IRLocation i )
  {
    try {
      Version.saveVersion( fixedVersion.getVersion() );
      super.removeElementAt( i );
    } finally {
      Version.restoreVersion();
    }
  }
}

