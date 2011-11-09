/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRState.java,v 1.4 2007/05/25 02:12:42 boyland Exp $
 */
package edu.cmu.cs.fluid.ir;

/**
 * An indication of something that may be changed.
 * A client is expected to use <tt>instanceof</tt> checks
 * to see if the item is meaningful.
 * <p>
 * The state abstraction and the {@link ChangeRecord} abstraction have related tasks
 * but their implementation and uses are com pletely separate.  Perhaps we should look
 * into finding meanngful overlap.
 * @see SlotState
 * @see SlotInfo
 * @see IRCompound
 * @author boyland
 */
public interface IRState {
  /** If this item is nested in something larger, return it.
   * @return larger unit or null (if not nested).
   */
  public IRState getParent();
  
  public static class Operations {
    /**
     * Return the highest/largest state that includes the given state.
     * 
     * @param st
     *          state to start with
     * @return a state with null parent that includes st
     * @throws NullPointerException
     *           if st is null
     */
    public static IRState root(IRState st) {
      for (;;) {
        IRState p = st.getParent();
        if (p == null) return st;
        st = p;
      }
    }
    
    /**
     * Return the closest enclosing persistent state
     * @param st state to start from (or null)
     * @return persistent state or null
     */
    public static IRPersistent asPersistent(IRState st) {
      while (st != null && !(st instanceof IRPersistent)) {
        st = st.getParent();
      }
      return (IRPersistent)st;
    }

    /** Check whether one state includes another
     * @param s1 potential ancestor state
     * @param s2 potential descendant state
     * @return whether s1 is s2 or an ancestor of s2
     */
    public static boolean includes(IRState s1, IRState s2) {
      while (s2 != null) {
        if (s2.equals(s1)) return true;
        s2 = s2.getParent();
      }
      return false;
    }
  }
}