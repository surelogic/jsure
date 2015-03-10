package edu.cmu.cs.fluid.mvc.version;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.version.Version;

/**
 * Minimal implementation of {@link VersionCursorModel}.
 */
final class VersionCursorImpl
  extends AbstractVersionSpaceToVersionTrackerStatefulView
  implements VersionCursorModel {
  protected VersionCursorImpl(
    final String name,
    final VersionSpaceModel src,
    final ModelCore.Factory mf,
    final ViewCore.Factory vcf,
    final VersionTrackerModelCore.Factory vtf,
    final VersionSpaceToVersionTrackerStatefulViewCore.Factory cf,
    final boolean isFollowing)
    throws SlotAlreadyRegisteredException {
    super(name, src, mf, vcf, vtf, cf, LocalAttributeManagerFactory.prototype);
    vsVcCore.setFollowing(isFollowing);

    // init the VERSION attribute to be the root of the VERSION SPACE
    final Iterator roots = src.getRoots();
    if (!roots.hasNext()) {
      throw new IllegalArgumentException("Source VersionSpaceModel does not have a root!");
    }
    final IRNode root = (IRNode) roots.next();
    curModCore.setVersion(src.getVersion(root));

    // init the SRC_MODELS attribute
    final IRSequence<Model> srcModels = ConstantSlotFactory.prototype.newSequence( 1 );
    srcModels.setElementAt(src, 0);
    viewCore.setSourceModels(srcModels);

    src.addModelListener(srcModelBreakageHandler);
    //src.associateVersionCursor(this);
    finalizeInitialization();
  }

  @Override
  protected void rebuildModel(final List events) {
    /*
		 * Need this outside of critical section so that we can use them to send an
		 * event if the model breaks.
		 */
    boolean broken = false;
    Version current = null;

    synchronized (structLock) {
      ModelEvent e;

      if (vsVcCore.isFollowing()) {
        // get the last event in the list
        Iterator it = events.iterator();
        final Version orig = curModCore.getVersion();
        current = orig;

        while (it.hasNext()) {
          e = (ModelEvent) it.next();
          if (e instanceof VersionSpaceEvent) {
            final VersionSpaceEvent le = (VersionSpaceEvent) e;

            /*
						 * Check to see if a new node was added as a child to the Version
						 * we are currently referencing.
						 */
            final Version base = le.getBaseVersion();
            final Version bud = le.getChildVersion();
            if (current.equals(base)) {
              if (MV.isLoggable(Level.FINE)) {
                MV.fine(
                  getName() + ": cursor following from " + base + " to " + bud);
              }
              current = bud;
            }
          } else {
            // nothing that we can do
            // infomration less event are not of our interest
          }
        }
        if (current != orig) {
          curModCore.setVersion(current);
          broken = true;
        }
      }
    }

    if (broken) {
      modelCore.fireModelEvent(
        new AttributeValuesChangedEvent(this, VERSION, current));
    }
  }

}