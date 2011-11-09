package com.surelogic.tree.diff;

import java.util.*;

@SuppressWarnings("unchecked")
public interface IDiffNode<T extends IDiffNode> {
  public enum Status { SAME, BACKEDGE, ORIGINAL, CHANGED, ADDED, DELETED, ADDED_NO_MATCH }
  
  Status setStatus(Status s);
  Status getStatus();
  String getMessage();
  Collection<T> getChildrenAsCollection();
  Collection<T> setChildren(Collection<T> c);
  int numChildren();
  
  Object identity();
  
  /**
   * @return 0 if exact match, more similar if closer to zero
   */
  Comparator getComparator();
  
  /**
   * Assumes that the identities are equal
   * 
   * @return true if the other attributes match
   */
  boolean isShallowMatch(T n);
  
  T shallowCopy();
  T deepCopy();
}
