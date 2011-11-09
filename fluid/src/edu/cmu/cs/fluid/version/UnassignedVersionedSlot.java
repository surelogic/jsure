/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/UnassignedVersionedSlot.java,v 1.11 2007/07/10 22:16:33 aarong Exp $ */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRType;

/** A versioned slot with only one value, the initial value.
 * This slot may be shared by multiple nodes.
 */
/*
 * $Log: UnassignedVersionedSlot.java,v $
 * Revision 1.11  2007/07/10 22:16:33  aarong
 * Removed unused imports, unnecessary casts,
 * fixed switch-statement fall throughs
 * Changed static members to be directly accessed
 *
 * Revision 1.10  2006/05/24 02:47:20  boyland
 * fixed misdesign in bi-versioned slots:
 *    must share root version for all slots in a family
 *
 * Revision 1.9  2006/03/28 20:58:45  chance
 * Updated for Java 5 generics
 *
 * Revision 1.8  2006/03/27 21:35:50  boyland
 * Added type parameters to Slot SlotInfo and IRSequence
 * refactored out slot storage into new SLotStorage class
 *
 * Revision 1.7  2005/05/25 20:12:16  chance
 * Updated for Java 5
 *
 * Revision 1.6  2004/07/31 17:29:10  boyland
 * Added Bidirectional versioned slots.
 * Fixed VersionedHashMap to use them.
 *
 * Revision 1.5  2004/06/25 15:36:49  boyland
 * Replaced VersionedStructure with new IRState
 * Updated test cases for persistence.
 *
 * Revision 1.4  2003/07/02 20:19:25  thallora
 * Initial changes to allow fluid core analysis code to work in the edu.cmu.cs. prefix
 *
 * Revision 1.3  2003/05/22 17:16:35  chance
 * Changed to reuse UnassignedVersionedSlot for the no-arg and null cases
 *
 * Revision 1.2  2002/08/03 01:16:27  thallora
 * Log4j conversion of some files
 *
 * Revision 1.1  2001/09/18 18:39:25  boyland
 * Initial revision
 *
 * Revision 1.16  2000/07/17 21:08:27  boyland
 * Added printing and debugging.
 *
 * Revision 1.15  2000/02/21 22:31:00  boyland
 * *** empty log message ***
 *
 * Revision 1.14  2000/02/21 22:25:48  boyland
 * Added ability to detect missing deltas.
 *
 * Revision 1.13  1999/07/28 15:51:30  boyland
 * Now uses new ChangedVersionedSlot
 * Avoids redundant reads.
 *
 * Revision 1.12  1998/10/30 18:59:55  boyland
 * Removed hack for deltas.  (Now use isChanged().)
 *
 * Revision 1.11  98/10/20  15:51:43  boyland
 * Added persistence methods.
 * 
 */
public class UnassignedVersionedSlot<T> extends DependentVersionedSlot<T> {
  protected UnassignedVersionedSlot() {
    super();
  }

  protected UnassignedVersionedSlot(T value) {
    super(value);
  }

  private static final UnassignedVersionedSlot prototype = new UnassignedVersionedSlot();
  @SuppressWarnings("unchecked")
  private static final UnassignedVersionedSlot nullPrototype = new UnassignedVersionedSlot(null);

  /*
  private static int reuse = 0, num = 0;
  private static final Map cache = new IdentityHashMap();
  */
  @SuppressWarnings("unchecked")
  public static <T> UnassignedVersionedSlot<T> create() {
    // reuse++;
    // LOG.debug("Reusing UnassignedVersionedSlot prototype: "+reuse);
    return prototype;
    // return new UnassignedVersionedSlot();
  }

  @SuppressWarnings("unchecked")
  public static <T> UnassignedVersionedSlot<T> create(final Object value) {
    if (value == null) {
      // reuse++;
      return nullPrototype;
    }
    /*
    Object slot = cache.get(value);
    if (slot != null) {
      reuse++;
      return (UnassignedVersionedSlot) slot;
    }
    num++;
    */
    UnassignedVersionedSlot uvs = new UnassignedVersionedSlot(value); 
    /*
    cache.put(value, uvs);
    LOG.debug("Creating UnassignedVersionedSlot '"+value+"' "+num);
    */
    return uvs;
  }

  // VersionedSlot does most of our work for us,
  // we only need to define a few abstract methods:

  @Override
  public Version getLatestChange(Version v) {
    return Version.getInitialVersion();
  }

//  private static int assigned = 0;
//  private boolean done = false;
//  private int set = 0;

  @Override
  protected VersionedSlot<T> setValue(Version v, T newValue) {
    /*
    assigned++;
    LOG.debug("Slots now assigned = "+assigned);    
    set++;
    if (!done && set <= 2) {
      LOG.debug("Assigned twice? "+set, new Throwable());
      done = true;
    } 

    if (done) {
      LOG.debug("Assigned twice?", new Throwable());
    }
    done = true;
    */    
    return new OnceAssignedVersionedSlot<T>(initialValue, v, newValue);
  }

  @Override
  public boolean isChanged(Era e) {
    return false;
  }

  @Override
  public void writeValues(IRType<T> ty, IROutput out, Era e) {
  }
}
