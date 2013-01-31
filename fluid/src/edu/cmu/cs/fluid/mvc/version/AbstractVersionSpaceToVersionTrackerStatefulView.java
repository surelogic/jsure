package edu.cmu.cs.fluid.mvc.version;

import java.util.Iterator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.tree.AbstractForestToModelStatefulView;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.version.Version;

/**
 * Abstract implementation for models that view VersionSpaceModels and export
 * VersionCursor models.
 */

public abstract class AbstractVersionSpaceToVersionTrackerStatefulView
  extends AbstractForestToModelStatefulView
  implements VersionTrackerModelCore.VersionVerifier {
  /**
	 * Canonical reference to the Logging Category representing versioned
	 * model&ndash;view&ndash;controller related output.
	 */
  public static final Logger LOG = SLLogger.getLogger("MV.version");

  protected final VersionSpaceModel srcModel;

  protected final VersionTrackerModelCore curModCore;
  protected final VersionSpaceToVersionTrackerStatefulViewCore vsVcCore;
  //
  //  /**
  //   * Reference to the thread that is currently executing within
  //   * our sprouting version sub-tree. <code>null</code> if no thread
  //   * is currently running. Only manipulated by
  //   * {@link #executeWithin(Runnable)}. When this is non-<code>null</code>
  //   * on this thread is allowed to invoke the method. All other callers are
  //   * forced to wait on the {@link #executiveLounge} condition variable.
  //   *
  //   * <p><em>NB. This field is not protected by the structLock, but is
  //   * protected by the {@link #executiveLounge} object.</em>
  //   */
  //  protected Thread currentExecutor = null;
  //  
  //  /**
  //   * Object used as the wait-queue for those threads that are waiting to
  //   * execute within the version tracker. Waits for the condition
  //   * {@link #currentExecutor} == <code>null</code>.
  //   */
  //  protected final Object executiveLounge = new Object();

  //===========================================================

  /**
	 * The constructor.
	 */
  public AbstractVersionSpaceToVersionTrackerStatefulView(
    final String name,
    final VersionSpaceModel src,
    final ModelCore.Factory mcf,
    final ViewCore.Factory vcf,
    final VersionTrackerModelCore.Factory vcmcf,
    final VersionSpaceToVersionTrackerStatefulViewCore.Factory vsvcvcf,
    final AttributeManager.Factory attrFactory)
    throws SlotAlreadyRegisteredException {
    super(
      name,
      mcf,
      vcf,
      attrFactory,
      NullAttributeInheritanceManagerFactory.prototype);
    final AttributeChangedCallback modelChanged = new ModelAttrCallback();
    curModCore =
      vcmcf.create(name, this, structLock, attrManager, this, modelChanged);
    vsVcCore = vsvcvcf.create(this, structLock, attrManager, modelChanged);
    srcModel = src;
  }

  //===========================================================
  //== Attribute Callback for Label Attribute
  //===========================================================

  private final class ModelAttrCallback
    extends AbstractAttributeChangedCallback {
    @Override
    protected void attributeChangedImpl(
      final String attr,
      final IRNode node,
      final Object value) {
      if ((attr == VersionSpaceToVersionTrackerStatefulView.IS_FOLLOWING)
        || (attr == VersionTrackerModel.VERSION)) {
        modelCore.fireModelEvent(
          new AttributeValuesChangedEvent(
            AbstractVersionSpaceToVersionTrackerStatefulView.this,
            attr,
            value));
      }
    }
  }

  //===========================================================
  //== From VersionTrackerModelCore.VersionVerifier
  //===========================================================

  // Called from within critical section
  @Override
  public boolean shouldChangeToVersion(final Version ver) {
    return srcModel.isPresent(ver.getShadowNode());
  }

  //===========================================================
  //== Attribute Convienence methods
  //===========================================================

  public final void setVersion(final Version ver) {
    synchronized (structLock) {
      curModCore.setVersion(ver);
    }
    modelCore.fireModelEvent(
      new AttributeValuesChangedEvent(this, VersionTrackerModel.VERSION, ver));
  }

  public final Version getVersion() {
    synchronized (structLock) {
      return curModCore.getVersion();
    }
  }

  /**
	 * Convienence method for setting the
	 * {@link VersionSpaceToVersionTrackerStatefulView#IS_FOLLOWING}attribute.
	 */
  public final void setFollowing(final boolean mode) {
    synchronized (structLock) {
      vsVcCore.setFollowing(mode);
    }
    modelCore.fireModelEvent(
      new AttributeValuesChangedEvent(
        this,
        VersionSpaceToVersionTrackerStatefulView.IS_FOLLOWING,
        (mode ? Boolean.TRUE : Boolean.FALSE)));
  }

  /**
	 * Convienence method for getting the value of the
	 * {@link VersionSpaceToVersionTrackerStatefulView#IS_FOLLOWING attribute}
	 */
  public final boolean isFollowing() {
    synchronized (structLock) {
      return vsVcCore.isFollowing();
    }
  }

  //  public void executeWithin(Runnable action) {
  //    /* Prevent more than one thread from executing in this method at once.
  //     * But we also want to prevent the thread that is executing in this
  //     * method from holding the structLock while executing the Runnable.
  //     */
  //    synchronized( executiveLounge ) {
  //      final Thread currentThread = Thread.currentThread();
  //      // Let the currently executing thread be reentrant into this method
  //      if( currentThread != currentExecutor ) {
  //	    	while( currentExecutor != null ) {
  //	    		try {
  //	    			executiveLounge.wait();
  //	    		} catch( final InterruptedException e ) {
  //	    			// @ignore BS exception, continue waiting
  //	    		}
  //	    	}
  //	    	// Done waiting, now we can execute
  //	    	currentExecutor = currentThread;
  //      } else {
  //      	// The currently executing thread re-enters without waiting
  //      }
  //    }
  //    
  //    /* Execute the Runnable. Mutual exclusion is still preserved for
  //     * the call into the core object because of the use of
  //     * currentExecutor flag. This is important because we need to NOT hold
  //     * the structLock at this point to avoid deadlocks.
  //     *
  //     * PROBLEM: WE NEED TO HOLD THE STRUCTLOCK TO PREVENT THE VERSION
  //     * FROM CHANGING!
  //     */
  //    Version next = null;
  //    synchronized( structLock ) {
  //      next = curModCore.executeWithin(action);
  //      final Version base = curModCore.getVersion();
  //      if (next.equals(base)) {
  //        return;
  //      }
  //      // Changed so update the VersionSpace
  //      LOG.debug("Updating the version space from "+base+" to "+next);
  //      srcModel.addVersionNode(base, next);
  //      LOG.debug("Updating the version cursor to "+next);
  //      curModCore.setVersion(next);
  //    }
  //    
  //    // Send an event that we changed.
  //    modelCore.fireModelEvent(
  //      new AttributeValuesChangedEvent( this, VersionTrackerModel.VERSION, next )
  // );
  //    
  //    // Allow another executor to run
  //    synchronized( executiveLounge ) {
  //    	currentExecutor = null;
  //    	// only need to wake up one of the waiters.
  //    	executiveLounge.notify();
  //    }
  //  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Node methods
  //===========================================================

  /**
	 * Model contains no nodes.
	 */
  @Override
  public final boolean isPresent(final IRNode node) {
    return false;
  }

  /**
	 * Always returns an empty iterator because the model never contains any
	 * nodes.
	 */
  @Override
  public final Iterator<IRNode> getNodes() {
    return new EmptyIterator<IRNode>();
  }

  /**
	 * Does nothing; the model can never contain any nodes.
	 */
  @Override
  public final void addNode(final IRNode node, final AVPair[] attrs) {
  }

  /**
	 * Does nothing; the model can never contain any nodes.
	 */
  @Override
  public final void removeNode(final IRNode node) {
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
