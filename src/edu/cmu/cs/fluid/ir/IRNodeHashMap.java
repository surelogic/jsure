/*
 * Created on Aug 6, 2003
 *
 */
package edu.cmu.cs.fluid.ir;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.util.CustomHashMap;
import edu.cmu.cs.fluid.util.CopiedHashMap;

/**
 * A hashtable that maps IRnodes to various things.
 * Whenever it is rehash'ed, we remove any mappings for destroyed nodes.
 * 
 * @author chance
 * @author boyland
 * @deprecated This works fine, but has been superceded by IRNodeHashedMap
 */
@Deprecated
public class IRNodeHashMap extends CustomHashMap {
  private static final Logger LOG = SLLogger.getLogger("FLUID.ir");
  
  IRNodeHashMap(HashEntryFactory factory) {
    super(factory);
  }
  
  public IRNodeHashMap() {
    super();
  }
  
  @Override
  protected void resize(int size) {
    cleanup();
    super.resize(size);
  }
  
  /** Remove any destroyed nodes from the map.
   * Called on demand or automatically before we resize.
   * @see SlotInfo#gc()
   */
  public void cleanup() {
    int removed = 0;
    // This low-level loop is necessary because the iterator.remove()
    // method uses the high-level access, rather than directly removing the
    // entry.  But destroyed nodes cannot be found under their old IDs.
    for (int i=0; i < table.length; ++i) {
      CopiedHashMap.Entry e = table[i], last = null, next;
      while (e != null) {
        next = e.getNext();
        if (e.getKey().equals(IRNode.destroyedNode)) {
          if (last == null) {
            table[i] = next;
          } else {
            last.setNext(next);
          }
          ++removed;
          --size;
        } else {
          last = e;
        }
        e = next;
      }
    }
    if (removed > 0) {
      LOG.info("Removed "+removed+" entries");
    }      
  }
}
