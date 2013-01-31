package edu.cmu.cs.fluid.mvc.version;

/**
 * Factory for creating instances of default {@link ModificationManager}
 * implementations.  The returned implementions do not yet wrap the
 * VersionMarkerModel to prevent external modification of the marker's
 * version.
 */
public final class ModificationManagerFactory
implements ModificationManager.Factory
{
  public static final ModificationManager.Factory prototype =
    new ModificationManagerFactory();
    
  private ModificationManagerFactory() {}
  
  @Override
  public ModificationManager create(
    final VersionSpaceModel versionSpace, final VersionTrackerModel marker )
  {
  	return new ModificationManagerImpl( versionSpace, marker );
  }
}
