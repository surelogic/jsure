/*
 * Created on Jul 17, 2003
 *  
 */
package edu.cmu.cs.fluid.mvc;

/**
 * Model listener that causes the model portion of the stateful view to be
 * rebuilt whenever an event is received. This listener does not need to be
 * wrapped by <code>ThreadedModelAdapter</code> because
 * {@link AbstractModelToModelStatefulView}handles threading and
 * synchronization issues itself.
 * 
 * <p>
 * This listener manages a flag based on {@link RebuildEvent}s that indicats
 * whether any of the stateful view's source models is in the process of
 * rebuilding itself.
 */
public final class BreakageListener extends ModelAdapter {
  /**
	 * Shared reference to the object that contains the current rebuild state of
	 * the source models.
	 */
  private final SrcBuildState srcState;

  /**
	 * The ModelListener to pass things onto
	 */
  private final ModelListener ml;

  private final String name;

  /**
	 * Create a new breakage listener.
	 * 
	 * @param srcState
	 *          Reference to the shared object that contains the current rebuild
	 *          state of the source models.
	 */
  public BreakageListener(
    final String name,
    final ModelListener m,
    final SrcBuildState srcState) {
    this.srcState = srcState;
    this.ml = m;
    this.name = name;
  }

  /**
	 * @see edu.cmu.cs.fluid.mvc.ModelListener#breakView(edu.cmu.cs.fluid.mvc.ModelEvent)
	 */
  @Override
  public void breakView(final ModelEvent e) {
    Model.MV.fine(name + " received event: " + e.toString());

    // handle RebuildEvents specially
    if (e instanceof RebuildEvent) {
      final RebuildEvent re = (RebuildEvent) e;
      srcState.toggleRebuildState(re.getSourceAsModel(), re.getRebuildState());
    } else {
      // Don't pass on non-breaking events
      if (e.shouldCauseRebuild()) {
        ml.breakView(e);
      }
    }
  }
}