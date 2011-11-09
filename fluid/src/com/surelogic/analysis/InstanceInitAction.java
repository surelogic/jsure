package com.surelogic.analysis;

/**
 * Encapsulates activities that should occur immediately before/after the 
 * recursive visitation is performed.
 * 
 * This class is always used according to the following pattern:
 * 
 * <pre>
 * action.tryBefore();
 * try {
 *   // do something
 * } finally {
 *   action.finallyAfter();
 * }
 * action.afterVisit();
 * </pre>
 * 
 * @see {@link #InstanceInitializationVisitor}
 */
public interface InstanceInitAction {
  public static final InstanceInitAction NULL_ACTION = new InstanceInitAction() {
    public void tryBefore() { /* do nothing */ }
    public void finallyAfter() { /* do nothing */ }
    public void afterVisit() { /* do nothing */ }
  };

  /**
   * Called immediately before the activity of interest is performed, before the
   * start of a <code>try</code> block.
   */
  public void tryBefore();
  
  /**
   * Called immediately after the activity of interest is performed, at the
   * start of a finally block.  Should clean up anything that 
   * {@link #tryBefore()} initialized. 
   */
  public void finallyAfter();
  
  /**
   * Called after {@link #finallyAfter()}, but only if the activity and 
   * <code>finallyAfter</code> completed normally (without exceptions). 
   */
  public void afterVisit();
}
