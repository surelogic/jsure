package edu.cmu.cs.fluid.mvc.version;

/**
 * Interface for objects that coordinate modifications to versioned IR.
 * Modifications are made by passing {@link Runnable}s
 * to {@link #executeAtomically}. All modifications made using the same
 * manager are serialized.  The manager mantains a reference to a 
 * {@link VersionMarkerModel} that refers to the version at which the
 * modification will begin.  After a runnable has been executed, the 
 * version is updated.  
 * 
 * <p>The version marker given to the ModificationManager should not have
 * its version set except by the manager.  Hopefully the implementation will
 * assist in enforcing this by insisting on an unique marker when it is 
 * created and only providing access to a wrapped marker that is not mutable.
 * 
 * <p><em>Need to say more here about the purpose of this, why we
 * use a VersionMarker and not a VersionTracker,
 * etc.</em>
 */
public interface ModificationManager
{
  public boolean isExecuting();
  
  public void executeAtomically( Runnable r );

  public void executeAtMarker( Runnable r );
  
  public VersionTrackerModel getMarker();
  
  public VersionSpaceModel getVersionSpace();
  
  public static interface Factory
  {
  	public ModificationManager create(
  	  VersionSpaceModel vs, VersionTrackerModel marker );
  }
}
