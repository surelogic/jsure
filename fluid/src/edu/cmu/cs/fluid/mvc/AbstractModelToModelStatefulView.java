package edu.cmu.cs.fluid.mvc;

import java.util.*;
import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Default implementation of a StatefulView for use when attributes from
 * multiple source models do not need to be merged when presented to views of
 * the stateful view. Implements the methods to delegate to the provided core
 * implementations.
 * 
 * <p>
 * <em>It is the subclasser's responsibility to initialize
 * the model-level attribute {@link View#SRC_MODELS}.</em>
 * 
 * <p>
 * This class implements the infrastructure for rebuilding a stateful view. The
 * infrastructure is similar to that used by the AWT: a rebuild may be
 * scheduled to occur, and when it does occur, it executes on a different
 * thread. Every stateful view has it's own rebuild thread, of class
 * {@link AbstractModelToModelStatefulView.RebuildWorkerThread}. The thread
 * waits for a rebuild request that is triggered by a call to
 * {@link #signalBreak}. The method <code>signalBreak</code> appends the
 * provided ModelEvent to the end of a list (referred to by {@link #events})
 * and schedules the rebuild thread to wake up.
 * 
 * <p>
 * The call to <code>signalBreak</code> can originate from a subclass (e.g.,
 * the stateful view has been altered directly) or as a result of receiving an
 * event from a source model. In the later case, the call is placed by instance
 * of {@link AbstractModelToModelStatefulView.BreakageListener}referred to by
 * the instance file {@link #srcModelBreakageHandler}. This listener should be
 * registered with every independent source model of the stateful view. The
 * breakage listener handles {@link RebuildEvent}s specially. They are not
 * passed to <code>signalBreak</code>}. They are instead used to toggle the
 * state of a {@link AbstractModelToModelStatefulView.SrcBuildState}object,
 * which keeps track of which source models are in the process of rebuildling
 * themselves. The <code>SrcBuildState</code> instance is shared among the
 * stateful view, the rebuild thread, and the breakage listener, and his
 * referenced by the fields {@link #srcState},
 * {@link AbstractModelToModelStatefulView.RebuildWorkerThread#srcState}, and
 * {@link AbstractModelToModelStatefulView.BreakageListener#srcState}
 * respectively.
 * 
 * <p>
 * As mentioned already, the rebuild thread waits for notification that an
 * event has been received. A {@link RebuildEvent}is fired indicating that a
 * rebuild has begun. At this point it then waits for its source models to be
 * quiescent; that is, none of them to be updating their state. It uses
 * {@link AbstractModelToModelStatefulView.SrcBuildState#waitForQuiescence}for
 * this purpose. Once this is true, it appends the received events to a local
 * list (initially empty) and passes that list to the abstract method
 * {@link #rebuildModel(List)}. Subclasses implement this to rebuild
 * themselves. If the state of the stateful view is actually changed, the
 * implementation must result in the firing of at least one {@link ModelEvent}
 * so that views of this model are informed of its breakage. Upon successful
 * completion of <code>rebuildModel</code>, a <code>RebuildEvent</code> is
 * fired indicating that the stateful view has completed a rebuild.
 * 
 * <p>
 * There is one further subtlety. If a non- <code>RebuildEvent</code> arrives
 * while a local rebuild is in process, then the rebuild thread is interrupted.
 * Implemenations of {@link #rebuildModel(List)}should check the interrupted
 * status of the current thread (it always runs in the rebuild thread) and
 * throw an {@link java.lang.InterruptedException}if the thread has been
 * interrupted. This is caught within the rebuild thread's body and triggers
 * the local rebuild process to start over. Even if {@link #rebuildModel(List)}
 * does not check for interruptions, the rebuild will be done over if an event
 * was received during the rebuild. Checking for interruptions prevents wasted
 * work, however.
 * 
 * @author Aaron Greenhouse
 */
public abstract class AbstractModelToModelStatefulView
  extends AbstractModel
  implements ModelToModelStatefulView {
  //===========================================================
  //== Static Fields
  //===========================================================

  /**
	 * A "canned" InterruptedException to use when a rebuild is interrupted. It
	 * will never&mdash;must never&mdash;be propogated to the user so the bogus
	 * creation point is okay.
	 */
  protected static final InterruptedException cannedInterrupt =
    new InterruptedException();

  /**
	 * A static empty list so that we don't have to create a new one every time
	 * the parameterless {@link #rebuildModel()}is called.
	 */
  private static final List<ModelEvent> emptyList = new ArrayList<ModelEvent>(0);

  //===========================================================
  //== Instance Fields
  //===========================================================

  /** The attribute inheritance manager to use. */
  protected final AttributeInheritanceManager inheritManager;

  /** The ViewCore delegate object. */
  protected final ViewCore viewCore;

  /** The factory for managing {@link RebuildEvent}s. */
  private final RebuildEventFactory rebuildEventFactory;

  /** The list of received events; used by the worker thread */
  private final List<ModelEvent> events;

  /**
   * This flag is set <code>true</code> if the rebuild thread should stop.
   * Protected by the {@link #events event queue}.
   */
  private boolean shouldStop = false;

  /**
   * The worker thread. Can be used to check if the rebuild thread has been
   * interrupted in the implementatino of {@link #rebuildModel(List)}.
   */
  protected final Thread rebuildWorker;

  /**
   * Shared reference to the object that contains the current rebuild state of
   * the source models.
   */
  private final SrcBuildState srcState;

  /**
   * Is the model currently rebuilding? [I should say why it is volatile...]
   */
  private volatile boolean rebuilding = false;

  /**
   * Should the model redo the rebuild? [I should say why it is volatile...]
   */
  private volatile boolean doOver = false;

  /**
   * The unique listener to attach to source models to trigger a rebuild when
   * they break. This specific listener instance must be attached to
   * <em>all</em> the immediate, independent source models of the stateful
   * view. Two source models are independent if neither is an ancester of the
   * other.
   */
  protected final ModelListener srcModelBreakageHandler;

  /**
   * The list of Rebuilders affiliated with the stateful view. These will be
   * invoked after {@link #rebuildModel}but before the rebuild event is sent
   * that indicates the rebuild is finished.
   */
  private Rebuilder[] rebuilders = new Rebuilder[0];

  /**
   * We run the rebuild as action because we need to lock the entire
   * model&ndash;view chain during a rebuild because we don't want the
   * source models to change while we are viewing them.
   */
  private final RebuildAction rebuildAction = new RebuildAction();
  private Model.AtomizedModelAction lockedRebuildAction;
  
  //===========================================================
  //== Constructor
  //===========================================================

  /**
	 * Initialize the "generic" model-to-model portion of the model, excepting
	 * the {@link #IS_ELLIPSIS}and {@link #ELLIDED_NODES}attributes. <em>It is
	 *     * the responsibility of the subclass to both create the attributes and to
	 *     * invoke the methods {@link ModelCore#setIsEllipsisAttribute} and {@link
   * ModelCore#setEllidedNodesAttribute} to set the
   * {@link ModelCore#isEllipsis} and {@link ModelCore#ellidedNodes} fields.</em>
	 */
  public AbstractModelToModelStatefulView(
    final String name,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory)
    throws SlotAlreadyRegisteredException {
    super(name, mf, attrFactory);

    /*
     * The two constructors should be exactly the same after the call to the
     * super constructor.
     */
    viewCore = vf.create(name, this, structLock, attrManager);
    inheritManager = inheritFactory.create(this, structLock, attrManager);

    // initialize event handling
    srcState = new SrcBuildState();
    srcModelBreakageHandler =
      new BreakageListener(
        "Model \"" + getName() + "\"",
        new BreakForwarder(),
        srcState);
    rebuildEventFactory = new RebuildEventFactory(this);
    events = new ArrayList<ModelEvent>(1);
    rebuildWorker = new RebuildWorkerThread(name, srcState);
    rebuildWorker.setDaemon(true);
    rebuildWorker.start();
  }

  public AbstractModelToModelStatefulView(
    final String name,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final SlotFactory slotf,
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory)
    throws SlotAlreadyRegisteredException {
    super(name, mf, attrFactory, slotf);

    /*
		 * The two constructors should be exactly the same after the call to the
		 * super constructor.
		 */
    viewCore = vf.create(name, this, structLock, attrManager);
    inheritManager = inheritFactory.create(this, structLock, attrManager);

    // initialize event handling
    srcState = new SrcBuildState();
    srcModelBreakageHandler =
      new BreakageListener(
        "Model \"" + getName() + "\"",
        new BreakForwarder(),
        srcState);
    rebuildEventFactory = new RebuildEventFactory(this);
    events = new ArrayList<ModelEvent>(1);
    rebuildWorker = new RebuildWorkerThread(name, srcState);
    rebuildWorker.setDaemon(true);
    rebuildWorker.start();
  }

  /**
	 * This method must be invoked at the end of initialization of the model.
	 * Checks that all model attributes are given a value, and initializes the
	 * rebuild architecture.
	 */
  @Override
  public void finalizeInitialization() {
    // first check model attributes
    super.finalizeInitialization();

    // check for cycle
    if (downChainFrom(this)) {
      throw new RuntimeException(
        "Model \"" + getName() + "\" descends from itself!");
    }
  }
  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Handle Changes to User-Defined attributes
  //===========================================================

  /**
   * Extend the shutdown functionality by also making sure the 
   * rebuild thread is stopped.
   */
  @Override
  public void shutdown() {
    super.shutdown();
    synchronized (events) {
      shouldStop = true;
      events.notify();
    }
  }
  
  //===========================================================
  //== Handle Changes to User-Defined attributes
  //===========================================================

  /**
	 * Invoked by the Models user-defined attribute changed callback. This
	 * version delegates to {@link #userDefinedAttributeChangedImpl}, and then
	 * (possibly) causes the model to break.
	 * 
	 * @param attr
	 *          The name of the attribute; interned String.
	 * @param node
	 *          If the attribute is a node-level attribute, then this is the node
	 *          whose attribute value changed. If the attribute is a model-level
	 *          attribute then this is <code>null</code>.
	 * @param value
	 *          The new value of the attribute.
	 */
  @Override
  protected final void userDefinedAttributeChanged(
    final String attr,
    final IRNode node,
    final Object value) {
    if (userDefinedAttributeChangedImpl(attr, node, value)) {
      signalBreak(
        new AttributeValuesChangedEvent(
          AbstractModelToModelStatefulView.this,
          node,
          attr,
          value));
    }
  }

  /**
	 * Delegate for {@link #userDefinedAttributeChanged}. Implementors should
	 * override this method instead. The return value of the method indicates if
	 * the model should break or not. This default implementation does nothing
	 * except to return <code>true</code>, causing model breakage.
	 * 
	 * @param attr
	 *          The name of the attribute; interned String.
	 * @param node
	 *          If the attribute is a node-level attribute, then this is the node
	 *          whose attribute value changed. If the attribute is a model-level
	 *          attribute then this is <code>null</code>.
	 * @param value
	 *          The new value of the attribute.
	 * @return <code>true</code> iff the model should break, <code>false</code>
	 *         if the change to the attribute value should not cause the model to
	 *         break.
	 */
  protected boolean userDefinedAttributeChangedImpl(
    final String attr,
    final IRNode node,
    final Object value) {
    return true;
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin StatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  private final class BreakForwarder extends ModelAdapter {
    /*
		 * (non-Javadoc)
		 * 
		 * @see edu.cmu.cs.fluid.mvc.ModelListener#breakView(edu.cmu.cs.fluid.mvc.ModelEvent)
		 */
    @Override
    public void breakView(ModelEvent e) {
      if (e instanceof AttributeAddedEvent) {
        /*
				 * Handle attribute addition specially too. Farm out to a special
				 * method.
				 */
        final AttributeAddedEvent addedEvent = (AttributeAddedEvent) e;
        attributeAddedToSource(
          addedEvent.getSourceAsModel(),
          addedEvent.getAttributeName(),
          addedEvent.isNodeAttribute());
      } else {
        // pass along normal events
        signalBreak(e);
      }
    }
  }

  //===========================================================
  //== Indicate a rebuild is needed
  //===========================================================

  /**
	 * Indicate that a source model has broken, and that the stateful view needs
	 * to be rebuilt. The worker thread will be notified to do a rebuild in the
	 * near future.
	 * 
	 * @param e
	 *          The source model event that we received
	 */
  protected final void signalBreak(final ModelEvent e) {
    if (e instanceof RebuildEvent) {
      MV.info("breaking...");
    }

    synchronized (events) {
      events.add(e);
      if (rebuilding) {
        /*
         * interrupt the rebuild thread if it is rebuilding and the source
         * models are quiescent. If they are not and we interrupt then we
         * interrupt the rebuild thread while it is waiting for quiescence which
         * is a waste time and energy.
         */
        if (srcState.isQuiescent())
          rebuildWorker.interrupt();

        // set flag in case the rebuild method doesn't check for interrupts
        doOver = true;
      } else {
        events.notify();
      }
    }
  }

  //===========================================================
  //== Action for Rebuilding
  //===========================================================
  
  private class RebuildAction implements Model.AtomizedModelAction {
    private List<ModelEvent> events;
    private boolean interrupted;
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.mvc.Model.AtomizedModelAction#execute()
     */
    @Override
    public List<ModelEvent> execute() {
      interrupted = false;
      try {
        rebuildModel(events);
      } catch (InterruptedException e) {
        // reset Interrupted bit
        Thread.interrupted();
        
        if (Thread.interrupted()) {
          System.err.println("SEVERE: thread STILL interrupted");
        }
        interrupted = true;
      }
      return Collections.emptyList();
    }
    
    public void setEvents(List<ModelEvent> events) {
      this.events = events;
    }
    public boolean wasInterrupted() {
      return interrupted;
    }
  }
  
  //===========================================================
  //== Worker Thread for Rebuilding
  //===========================================================
  
  /**
   * Thread used to rebuild the stateful view. Loops waiting for notification
   * via {@link #signalBreak}. When it awakes, {@link #rebuildModel( List )} is
   * called with the list of events from the source model(s).
   * 
   * <P>
   * The purpose of this is to disconnect the flow of control along a chain of
   * stateful views.
   */
  private class RebuildWorkerThread extends Thread {
    /**
     * Shared reference to the object that contains the current rebuild state of
     * the source models.
     */
    private final SrcBuildState srcState;

    /**
     * Local copy of the events that have arrived since the last
     * <em>successful</em> rebuild.
     */
    private final List<ModelEvent> copyOfEvents = new ArrayList<ModelEvent>();
    
    public RebuildWorkerThread(
      final String name,
      final SrcBuildState srcState) {
      super(name + "'s Rebuild Worker");
      this.srcState = srcState;
      setDaemon(true);
    }

    @Override
    public void run() {
      boolean localShouldStop = false;
      while (!localShouldStop) {
        // Wait for something to do.
        synchronized (events) {
          try {
            while (!shouldStop && events.isEmpty()) {
              events.wait();
            }
            localShouldStop = shouldStop;
          } catch (final InterruptedException e) {
            /*
             * Interrupted during wait. Need to figure out why this happens, but
             * it means we should stop waiting and rebuild
             */
          }
        }
        
        // If we aren't supposed to stop...
        if (!localShouldStop) {
          // Any funky races between waking up and here?

          // --- Rebuild proceses begins here ---
          // send rebuild begun event...
          modelCore.fireModelEvent(rebuildEventFactory.getBegunEvent());

          /*
           * use try--finally block to insure we always send a rebuild completed
           * event
           */
          try {
            rebuilding = true;

            rebuild: while (rebuilding) {
              try {
                /*
                 * Wait for the source models to be quiescent. We try not to
                 * interrupt the rebuild worker while it is waiting at this point,
                 * but it could happen if a source model breaks between the time
                 * the breakage listener checks for quiescence and the time it
                 * interrupts the rebuild worker. If this happens its not a big
                 * deal, we will just collect the new events and start waiting for
                 * quiescence again.
                 */
                srcState.waitForQuiescence(); // throws InterruptedException!

                /*
                 * If we just started the rebuild, then get the events that have
                 * arrived since the last successful rebuild. Otherwise we still
                 * need to get any events that may have arrived while waiting for
                 * quiescence or during our interrupted rebuild.
                 */
                synchronized (events) {
                  int size = events.size();
                  if (size < 4) {
                    for (int i = 0; i < size; i++) {
                      copyOfEvents.add(events.get(i));
                    }
                  } else {
                    copyOfEvents.addAll(events);
                  }
                  events.clear();
                }

                // long start = System.currentTimeMillis();
                // System.out.println(
                // AbstractModelToModelStatefulView.this.getName()
                // + " Rebuilding" );
                doOver = false;

                boolean interrupted = rebuildModel_locked(copyOfEvents);
                if (interrupted) {
                  continue rebuild;
                }
                // System.out.println(
                // AbstractModelToModelStatefulView.this.getName()
                // + " : "+(System.currentTimeMillis()-start)+" ms");

                /*
                 * Invoke the rebuild plugins.
                 */
                invokeRebuilders(copyOfEvents);
              } catch (InterruptedException e) {
                // System.out.println( "Rebuild interrupted, restarting" );
                // reset Interrupted bit
                Thread.interrupted();
                // restart the rebuild
                continue rebuild;
              } catch (Exception e) {
                MV.log(Level.SEVERE, "Caught exception during rebuild!", e);
                // abort the current rebuild
                rebuilding = false;
              }
              /*
               * Handle case where rebuildModel doesn't look for interrupts.
               * signalBreak will have set doOver to true if an event arrived
               * while we were rebuilding. We rebuild all over again if this
               * happened.
               */
              if (!doOver)
                rebuilding = false;
            }
          } finally {
            // make sure we send the rebuild completed event
            modelCore.fireModelEvent(rebuildEventFactory.getCompletedEvent());
            // clear our copy of events
            copyOfEvents.clear();
          }
        }
      }
    }
  }

  //===========================================================
  //== Rebuild Broken Views
  //===========================================================

  /**
   * This method is called when a new attribute has been added to one of the
   * source models. If the model wishes to respond to this with a rebuild then
   * {@link #signalBreak}should be invoked.
   * 
   * @param src
   *          The model to which the attribute was added.
   * @param attr
   *          The name of the new attribute.
   * @param isNodeLevel
   *          <code>true</code> iff the attribute is a node-level attribute.
   */
  protected void attributeAddedToSource(
    final Model src,
    final String attr,
    final boolean isNodeLevel) {
    /*
		 * XXX Should really try to inherit the new attribute here. XXX The event
		 * handling/rebuild stuff needs XXX to be globally redone for this to be
		 * dealt with properly. It can XXX be done with the current system, but it
		 * is not easy to do.
		 */
  }

  /**
   * Rebuild the model for <em>the first time</em>; this method must only be
   * invoked from the constructor/initialization code of a subclass
   * <em>before</em> the stateful view attaches its
   * {@link #srcModelBreakageHandler}to its source models. To trigger a rebuild
   * from any other scenario, {@link #signalBreak}must be used instead.
   * 
   * <p>
   * Must send out <code>ModelEvent</code> s as appropriate to propagate
   * breakage down the model&ndash;view chain.
   */
  protected final void rebuildModel() {
    if (rebuildModel_locked(emptyList)) {
      /*
       * Shouldn't happen because this should only be invoked by the constructor
       * before any model listeners are attached to any source models we might
       * have.
       */
      System.err.println("SEVERE: rebuildModel() shouldn't be interrupted.");
    }
  }
  
  /**
   * Should be the only caller of rebuildModel() below
   */
  private boolean rebuildModel_locked(List<ModelEvent> events) 
  {
    synchronized (structLock) {
      rebuildAction.setEvents(events);
      if (lockedRebuildAction == null) {
        lockedRebuildAction = ModelUtils.wrapAction(this, rebuildAction);
      }
      lockedRebuildAction.execute();
      return rebuildAction.wasInterrupted();
    }
  }
  
  /**
   * Rebuild the model, given the <code>ModelEvents</code> from the source
   * model(s) as hints; this method must <em>never</em> be invoked by a
   * subclass. Invocation of this method is managed by the stateful view's
   * {@link AbstractModelToModelStatefulView.RebuildWorkerThread}. Subclasses
   * must invoke {@link #signalBreak} to cause a rebuild.
   * 
   * <p>
   * The stateful view <em>must</em> be able to rebuild itself even when
   * <code>events</code> is empty.
   * 
   * <P>
   * Must send out <code>ModelEvent</code> s as appropriate to propagate
   * breakage down the model&ndash;view chain.
   * 
   * <p>
   * Implementations should periodically check the interrupted status of the
   * thread, throwing an InterruptedException if the thread has been
   * interrupted. This will ccause the rebuild to begin a new, and will have
   * been trigered by the receipt of an event from one of the stateful view's
   * source models.
   * 
   * @exception InterruptedException
   *              See above discussion.
   */
  protected abstract void rebuildModel(List<ModelEvent> events)
    throws InterruptedException;

  //===========================================================
  //== Management of rebuilder plugins
  //===========================================================

  /**
	 * Add a rebuilder to the stateful view to be invoked after
	 * <code>rebuildModel()</code> has completed.
	 */
  public final void addRebuilder(final Rebuilder r) {
    synchronized (this) {
      final int len = rebuilders.length;
      final Rebuilder[] new_list = new Rebuilder[len + 1];
      System.arraycopy(rebuilders, 0, new_list, 0, len);
      new_list[len] = r;
      // Atomic value assignment!!!
      rebuilders = new_list;
    }
  }

  /**
	 * Remove a rebuilder from the stateful view.
	 */
  public final void removeModelListener(final Rebuilder l) {
    synchronized (this) {
      final int len = rebuilders.length;
      final Rebuilder[] new_list = new Rebuilder[len - 1];
      int where = -1;
      for (int i = 0;(where == -1) && (i < len); i++) {
        if (l == rebuilders[i])
          where = i;
      }

      if (where != -1) {
        System.arraycopy(rebuilders, 0, new_list, 0, where);
        System.arraycopy(
          rebuilders,
          where + 1,
          new_list,
          where,
          len - where - 1);
        // Atomic value assignment!!!
        rebuilders = new_list;
      }
    }
  }

  /**
	 * Invoke the registered Rebuilders.
	 * 
	 * @param l
	 *          A List of {@link ModelEvent}s that have arrived since the last
	 *          time the rebuilders were invoked.
	 */
  private void invokeRebuilders(final List l) throws InterruptedException {
    // Atomic value assignment!!!
    final Rebuilder[] copy = rebuilders;
    for (int i = 0; i < copy.length; i++) {
      copy[i].rebuild(this, l);
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End StatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin View Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Source Models convienence methods
  //===========================================================

  @Override
  public Iterator<Model> getSourceModels() {
    return viewCore.getSourceModels();
  }

  //===========================================================
  //== Query about the relationship between models
  //===========================================================

  @Override
  public boolean downChainFrom(final Model m) {
    return viewCore.downChainFrom(m);
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End View Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}
