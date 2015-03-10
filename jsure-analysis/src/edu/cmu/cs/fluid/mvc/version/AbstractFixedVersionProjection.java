package edu.cmu.cs.fluid.mvc.version;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedIterator;

@SuppressWarnings("deprecation")
public abstract class AbstractFixedVersionProjection
  extends AbstractModelAndVersionTrackerToModelStatefulView {
  /**
	 * Logger for this class
	 */
  static final Logger LOG = SLLogger.getLogger("MV.version");

  /**
	 * A ready made event for sending to views.
	 */
  protected final ModelEvent modelEvent;

  //===========================================================
  //== Constructor
  //===========================================================

  /**
	 * Create a new model projection that projects a versioned model into an
	 * unversioned model by fixing a version. The version at which the model is
	 * projected may be changed at any time via the VersionTrackerModel source
	 * model. The attribute values of the model are also fixed at the same
	 * version, as well as the structure of any sequences in an attribute; <em>no other structured values are currently supported</em>.
	 * 
	 * @param name
	 *          The name of the model.
	 * @param srcModel
	 *          The "Model" source model.
	 * @param vc
	 *          The "VersionTracker" source model.
	 * @param mf
	 *          The factory that creates the ModelCore object to use.
	 * @param vf
	 *          The factory that creates the ViewCore object to use.
	 * @param initVersion
	 *          The initial version at which to project the model.
	 */
  protected AbstractFixedVersionProjection(
    final String name,
    final Model srcModel,
    final VersionTrackerModel vc,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final Version initVersion)
    throws SlotAlreadyRegisteredException {
    super(
      name,
      srcModel,
      vc,
      mf,
      vf,
      LocalAttributeManagerFactory.prototype,
      new FixedVersionAttributeManagerFactory(initVersion));
    modelEvent = new ModelEvent(this);

    /*
		 * Inherit attributes from the source model to be projected. (There is no
		 * need to inherit attributes from the version tracker model. This model
		 * passes-through the IS_ELLIPSIS and ELLIDED_NODES attributes, so we need
		 * to inherit them before we can set them in the ModelCore (see below).
		 */
    inheritManager.inheritAttributesFromModel(
      srcModel,
      FVPAttributeInheritancePolicy.prototype,
      AttributeChangedCallback.nullCallback);

    /*
		 * ModelCore does not provide IS_ELLIPSIS or ELLIDED_NODES In this
		 * (unusual) case they are inherited from the source model.
		 */
    modelCore.setIsEllipsisAttribute(
      attrManager.getNodeAttribute(Model.IS_ELLIPSIS));
    modelCore.setEllidedNodesAttribute(
      attrManager.getNodeAttribute(Model.ELLIDED_NODES));

    /*
		 * Only need to listen to the version tracker because any change to the
		 * source model will not be interesting because it is versioned and we are
		 * only looking at a particular version.
		 */
    tracker.addModelListener(srcModelBreakageHandler);
  }

  //===========================================================
  //== Rebuild methods
  //===========================================================

  /**
	 * Don't have to anything besides update the version in the attribute manager
	 * and propogate the event.
	 */
  @Override
  protected void rebuildModel(final List events) {
    boolean broken = false;
    synchronized (structLock) {
      final Version currentV =
        (Version) inheritManager.getProperty(FixedVersionSupport.VERSION);
      final Version newV = tracker.getVersion();
      if (!currentV.equals(newV)) {
        if (MV.isLoggable(Level.FINE)) {
          MV.fine(
            getName()
              + ": moving from version "
              + currentV.toString()
              + " to version "
              + newV.toString());
          MV.fine(getName() + " is " + this);
        }
        inheritManager.setProperty(FixedVersionSupport.VERSION, newV);
        broken = true;
      }
    }
    // Break our views
    if (broken)
      modelCore.fireModelEvent(modelEvent);
  }

  //===========================================================
  //== From Model
  //===========================================================

  @Override
  public final Iterator<IRNode> getNodes() {
    Version.saveVersion();
    try {
      LOG.info("Saving version " + Version.getVersion());
      Version.setVersion(tracker.getVersion());
      LOG.info("Jumping to version " + Version.getVersion());
      return new VersionedIterator<IRNode>(tracker.getVersion(), srcModel.getNodes());
    } finally {
      Version.restoreVersion();
      LOG.info("Restoring to version " + Version.getVersion());
    }
  }

  @Override
  public final boolean isPresent(final IRNode node) {
    Version.saveVersion();
    try {
      Version.setVersion(tracker.getVersion());
      return srcModel.isPresent(node);
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public final void addNode(final IRNode node, AVPair[] values) {
    throw new UnsupportedOperationException("Cannot modify a projection.");
  }

  @Override
  public final void removeNode(final IRNode node) {
    throw new UnsupportedOperationException("Cannot modify a projection.");
  }

  //===========================================================
  //== Model reflection methods
  //===========================================================

  /**
	 * Return a string representation of a node. This is model-specific, but it
	 * should approximate an attribute list, giving string representations of the
	 * all the attributes for which this node has a value.
	 * 
	 * @exception IllegalArgumentException
	 *              Thrown if <code>node</code> is not part of the model.
	 */
  @Override
  public final String toString(final IRNode node) {
    Version.saveVersion();
    try {
      Version.setVersion(tracker.getVersion());
      return super.toString(node);
    } finally {
      Version.restoreVersion();
    }
  }

  /**
	 * Return a string identifying the given node. This differs from
	 * {@link #toString(IRNode)}in that it is only meant to provide a name for a
	 * node, derived, for example, from an attribute value.
	 */
  @Override
  public final String idNode(final IRNode node) {
    Version.saveVersion();
    try {
      Version.setVersion(tracker.getVersion());
      return super.idNode(node);
    } finally {
      Version.restoreVersion();
    }
  }

  /**
	 * Return a string representation of the value stored in the given attribute
	 * for the given node. This implementation understands values of that store
	 * IRNodes and Sequences of IRNodes, and uses the method {@link #idNode}on
	 * their values.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the attribute is not part of the model.
	 */
  @Override
  public final String nodeValueToString(final IRNode node, final String attr)
    throws UnknownAttributeException {
    Version.saveVersion();
    try {
      Version.setVersion(tracker.getVersion());
      return super.nodeValueToString(node, attr);
    } finally {
      Version.restoreVersion();
    }
  }

  /**
	 * Return a string representaiton of the value of a given component-level
	 * attribute. This implementation understands values of that store IRNodes
	 * and Sequences of IRNodes, and uses the method {@link #idNode}on their
	 * values.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the attribute is not part of the model.
	 */
  @Override
  public final String compValueToString(final String attr)
    throws UnknownAttributeException {
    Version.saveVersion();
    try {
      Version.setVersion(tracker.getVersion());
      return super.compValueToString(attr);
    } finally {
      Version.restoreVersion();
    }
  }
}
