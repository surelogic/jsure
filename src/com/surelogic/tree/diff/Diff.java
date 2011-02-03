package com.surelogic.tree.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.tree.diff.IDiffNode.Status;

public class Diff {
  public static final Logger LOG = SLLogger.getLogger("tree.diff");
  
  public static <T extends IDiffNode<T>> T diff(T root, T root2, boolean keepSameNodes) {
    if (root.identity().equals(root2.identity())) {
      T copy = root.shallowCopy();
      copy.setChildren(diff(root.getChildrenAsCollection(), root2.getChildrenAsCollection(), keepSameNodes));
      return copy;
    }
    throw new IllegalArgumentException("Messages for the two roots don't match");
  }
  
  /**
   * Preserve the existing contents of parameters
   * Preserve the relative order of oldNodes + newNodes
   * Keep one copy of nodes that are the same (not counting children)
   * Move conflicting nodes next to each other
   * 
   * @return
   */
  public static <T extends IDiffNode<T>> Collection<T> diff(Collection<T> oldNodes, Collection<T> newNodes, boolean keepSameNodes) {
    if (oldNodes == null) {
      return newNodes;
    }
    if (newNodes == null) {
      return oldNodes;
    }
    try {
      DiffInfo<T> info = new DiffInfo<T>(keepSameNodes, oldNodes);
      return info.computeDiff(newNodes);
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
    return Collections.emptyList();
  }

  /**
   * A collection of info needed for any given node to compute the diff
   */
  private static class DiffInfo<T extends IDiffNode<T>> {
    final boolean keepSameNodes;
    
    final Collection<T> oldNodes;
    final List<T> results;   
    
    //  Keyed on the message
    final Map<Object,List<T>> processed = new HashMap<Object,List<T>>(); 
    @SuppressWarnings("unchecked")
    final Comparator comp; 
    
    DiffInfo(boolean keepSameNodes, Collection<T> oldNodes) {     
      this.keepSameNodes = keepSameNodes;
      this.oldNodes      = oldNodes;
      this.comp          = oldNodes.isEmpty() ? null : oldNodes.iterator().next().getComparator();
      this.results       = new ArrayList<T>(oldNodes);
      
      // Prematch the old nodes
      for (T n : results) {
        final Object key = n.identity();
        List<T> list = processed.get(key);
        if (list == null) {
          list = new ArrayList<T>(1);
          processed.put(key, list);
        } 
        list.add(n);

        n.setStatus(Status.DELETED);
      }
    }

    Collection<T> computeDiff(Collection<T> newNodes) {
      // For each new node, check for conflicts (based on the message)
      for (final T n : newNodes) {
        final Object id       = n.identity();
        final List<T> matches = processed.get(id);
        if (matches != null) {
          final int size = matches.size();      
          if (size == 1) {
            final T match = matches.get(0); 
            handlePotentialConflict(id, n, match);
          } else {
            siftPossibleMatches(id, n, matches);
          }
        } else {
          // A truly NEW node!
          results.add(n);     
          n.setStatus(Status.ADDED);
        }
      }
      if (!keepSameNodes) {
        // copy to avoid problems with ConcurrentModExceptions
        for (T n : new ArrayList<T>(results)) {
          if (treeMarkedAsSame(n)) {
            results.remove(n);        
          }
        }
      }
      return results; 
    }
    
    @SuppressWarnings("unchecked")
    private void siftPossibleMatches(Object id, final T n, final List<T> matches) {
      int diffs       = Integer.MAX_VALUE;
      List<T> options = new ArrayList<T>();

      boolean moreToProcess = false;
      // copy to avoid problems with ConcurrentModExceptions
      for (final T option : new ArrayList<T>(matches)) {
        // Only compare to those that aren't processed yet
        if (option.getStatus().equals(Status.DELETED)) {
          try {
            int val = comp.compare(id, option.identity());
            if (val < diffs) {
              diffs = val;
              options.clear();
              options.add(option);
            }
            else if (val == diffs) {
              options.add(option);
            }
            moreToProcess = true;
          }
          catch (Throwable t) {
            t.printStackTrace();
          }
        }
      }
      T match = null;

      if (moreToProcess) {
        final int size = options.size();

        diffs   = Integer.MAX_VALUE;
        if (size == 1) {
          match = options.get(0);
        } 
        else if (size == 0) {
          LOG.severe("Nothing to match against: "+n.getMessage());
        } 
        else { 
          // More than one option, so look at subtrees
          for (T option : options) {
            int val = compareSubtrees(n, option);
            if (val < diffs) {
              diffs = val;
              match = option;
            }
          }
        }
      } else {
        LOG.info("All already matched up");
      }
      if (match == null) {      
        results.add(n);
        n.setStatus(Status.ADDED_NO_MATCH);
      } else {
        handlePotentialConflict(id, n, match);
      }
    }

    private void handlePotentialConflict(Object id, final T n, final T match) {
      // A potential conflict
      if (match.isShallowMatch(n)) {
        // Same so far, but still need to check children
        match.setChildren(diff(match.getChildrenAsCollection(), n.getChildrenAsCollection(), keepSameNodes));
        if (match.numChildren() > 0) {
          match.setStatus(Status.ORIGINAL);
        } else {
          match.setStatus(Status.SAME);
        }
      } else {
        // A conflict here, so insert before the match          
        int i = replaceMatch(id, match);
        if (i >= 0) {
          n.setStatus(Status.CHANGED);
          n.setChildren(diff(match.getChildrenAsCollection(), n.getChildrenAsCollection(), false));          
          results.add(i, n);
        } else {
          // FIX how do we get here?
          LOG.severe("Couldn't find node in list: "+match.getMessage());
        }
      }
    }  
    
    /**     
     * @return the index of the match
     */
    int replaceMatch(Object id, T match) {
      int i = results.indexOf(match);
      if (i >= 0) {
        T copy = match.deepCopy();
        copy.setStatus(Status.ORIGINAL);     
        results.set(i, copy);
        
        List<T> matches = processed.get(id);
        matches.remove(match);
      }
      return i;
    }
  }
  
  /**
   * Assuming that we've already matched the message
   * @return An integer representing the difference between the two trees
   */
  private static <T extends IDiffNode<T>> int compareSubtrees(T n, T option) {
    if (n.isShallowMatch(n)) {
      // Compare children
      T t1   = n.deepCopy();
      T t2   = option.deepCopy();
      T diff = diff(t1, t2, false);
      return countNodes(diff);
    }
    return Integer.MAX_VALUE;
  }

  /**
   * @return The number of nodes in the tree
   */
  private static <T extends IDiffNode<T>> int countNodes(T n) {
    if (n == null) {
      return 0;
    }
    int val = 1;
    for (T child : n.getChildrenAsCollection()) {
      val += countNodes(child);
    }
    return val;
  }  
  
  /**
   * @return true if the subtree is all considered the same
   */
  private static <T extends IDiffNode<T>> boolean treeMarkedAsSame(T subtree) {
    if (subtree.getStatus() == Status.SAME) {
      for (T child : subtree.getChildrenAsCollection()) {
        if (!treeMarkedAsSame(child)) {
          return false;
        }
      }
      return true; 
    }
    return false;
  }
}
