package edu.cmu.cs.fluid.version;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;
import edu.cmu.cs.fluid.util.*;
import edu.cmu.cs.fluid.derived.IDerivedInformation.UnavailableException;
import com.surelogic.Starts;

/**
 * A thread-protected version-specific Map.
 * It represents ``dependent'' state: that is,
 * it makes the closed-world assumption.  It behaves rather like
 * {@link VersionedDerivedInformation} and requires the same protected methods 
 * to be defined.  It is not persistable.
 * Rather than ``undefined'', an unbound mapping goes to null.
 * The {@link #remove()} operation is implemented by setting the value
 * for the key to null, and {@link #clear()} similarly sets
 * all keys to null.
 * <p>
 * Design:
 * <ul>
 * <li> We can't use existing hash table implementations because 
 * they are not safe for concurrent modification.  Instead we will need to
 * use our own implementation and do fine-grained concurrency access control.
 * <li> We should inherit from Abstract Map to help with default implementation;
 * The VersionedDerivedInformation can be a component class---there's no need
 * to expose the implementation to the client.
 * <li> All accesses to data must check ensureDerived, and all mutations must
 * be protected by checkMutable.
 * <li> We need to be able to set values for any version: we need to handle going
 * back in time correctly: if we find out temporally later that versionly earlier,
 * a slot does have the value we expect, we need to note the change for the
 * nest-in-version-time version.  In other words, we use 
 * ``{@link VersioniedSlotFactory#bidirectional bidrectional}'' slots
 * to hold the information.
 * </ul>
 * @see VersionedDerivedInformation
 */
@Deprecated
public class VersionedHashMap<K,V> extends AbstractMap<K,V> implements Map<K,V> {

  // a VersionedHashMap is rather like a VersionedDerivedInformation instance.
  // here we show where our delegate is and define delegating methods
  // (for both directions), using a nested class to get the delagtion to come out too).
  private VersionedDerivedInformation derivationStatus = new DerivationStatus();

  class DerivationStatus extends VersionedDerivedInformation {
    @Override
    public void ensureDerived(Version v) throws UnavailableException {
      super.ensureDerived(v);
    }

    @Override
    public void checkMutable() {
      super.checkMutable();
    }

    @Override
    protected void deriveVersion(Version v) throws UnavailableException {
      VersionedHashMap.this.deriveVersion(v);
    }

    @Override
    protected void deriveChild(Version parent, Version child)
        throws UnavailableException {
      VersionedHashMap.this.deriveChild(parent, child);
    }

    @Override
    protected void deriveParent(Version child, Version parent)
        throws UnavailableException {
      VersionedHashMap.this.deriveParent(child, parent);
    }
  }

  protected void ensureDerived(Version v) throws UnavailableException {
    derivationStatus.ensureDerived(v);
  }

  protected void ensureDerived() throws UnavailableException {
    derivationStatus.ensureDerived();
  }

  protected void checkMutable() {
    derivationStatus.checkMutable();
  }

  protected void deriveVersion(Version v) throws UnavailableException {
    throw new UnavailableException("cannot derive information for version " + v);
  }

  protected void deriveChild(Version parent, Version child) {
    deriveVersion(child);
  }

  protected void deriveParent(Version child, Version parent) {
    deriveVersion(parent);
  }

  // we also behave as a hash map: here again we define the delegate as a nested
  // class
  private Map<K, VersionedSlot<V>> map = new HashMap<K, VersionedSlot<V>>();

  private final ExplicitSlotFactory factory = VersionedSlotFactory.bidirectional(Version.getVersion());
  
  // we keep the size separately because the map contains entries
  // for everything for which there is an mapping
  private VersionedSlot<Integer> sizeSlot = (VersionedSlot<Integer>) factory
      .predefinedSlot(IntegerTable.newInteger(0));

  protected void changeSize(int delta) {
    Integer oldSize = sizeSlot.getValue();
    Integer newSize = IntegerTable.newInteger(oldSize.intValue() + delta);
    synchronized (this) {
      sizeSlot = sizeSlot.setValue(Version.getVersionLocal(), newSize);
    }
  }

//  VersionedEntrySet myEntrySet = new VersionedEntrySet();
//
//  @SuppressWarnings("unchecked") class VersionedEntrySet extends AbstractSet {
//
//    class VersionedIterator extends AbstractIterator {
//      CustomLinkedHashMap.LinkedEntry entry = map.entries;
//
//      Version currentVersion = Version.getVersionLocal();
//
//      CustomLinkedHashMap.LinkedEntry lastEntry = null;
//
//      @SuppressWarnings("unchecked") VersionedIterator() {
//        findNonNullEntry();
//      }
//
//      protected void findNonNullEntry() {
//        while (entry != null) {
//          synchronized (VersionedHashMap.this) {
//            VersionedSlot vs = (VersionedSlot) entry.getValue();
//            if (vs.getValue(currentVersion) != null)
//              break;
//            entry = entry.nextInTable;
//          }
//        }
//      }
//
//      /* (non-Javadoc)
//       * @see java.util.Iterator#remove()
//       */
//      @SuppressWarnings("unchecked")
//      public void remove() {
//        if (lastEntry == null)
//          throw new IllegalStateException("next not called");
//        synchronized (VersionedHashMap.this) {
//          VersionedSlot vs = (VersionedSlot) lastEntry.getValue();
//          lastEntry.setValue(vs.setValue(currentVersion, null));
//        }
//        lastEntry = null;
//      }
//
//      /* (non-Javadoc)
//       * @see java.util.Iterator#hasNext()
//       */
//      public boolean hasNext() {
//        return entry != null;
//      }
//
//      /* (non-Javadoc)
//       * @see java.util.Iterator#next()
//       */
//      public Object next() {
//        if (entry == null)
//          throw new NoSuchElementException("at end of iteration");
//        lastEntry = entry;
//        Object rv = computeNext();
//        entry = entry.nextInTable;
//        findNonNullEntry();
//        return rv;
//      }
//
//      protected Object computeNext() {
//        return new Map.Entry() {
//          final CustomLinkedHashMap.LinkedEntry entry = VersionedIterator.this.entry;
//
//          @Starts("nothing")
//		public Object getKey() {
//            return entry.getKey();
//          }
//
//          @Starts("nothing")
//		public Object getValue() {
//            return ((VersionedSlot) entry.getValue()).getValue();
//          }
//
//          @SuppressWarnings("unchecked")
//          public Object setValue(Object value) {
//            Object oldValue;
//            synchronized (VersionedHashMap.this) {
//              VersionedSlot vs = (VersionedSlot) entry.getValue();
//              oldValue = vs.getValue();
//              entry.setValue(vs.setValue(currentVersion, value));
//            }
//            return oldValue;
//          }
//        };
//      }
//    }
//
//    /* (non-Javadoc)
//     * @see java.util.AbstractCollection#size()
//     */
//    @Starts("nothing")
//	@Override
//    public int size() {
//      ensureDerived();
//      Integer size = sizeSlot.getValue();
//      return size.intValue();
//    }
//
//    /* (non-Javadoc)
//     * @see java.util.AbstractCollection#iterator()
//     */
//    @Starts("nothing")
//	@Override
//    public Iterator iterator() {
//      ensureDerived();
//      synchronized (VersionedHashMap.this) {
//      return new VersionedIterator();
//      }
//    }
//
//  }
//
//  VersionedKeySet myKeySet = new VersionedKeySet();
//
//  class VersionedKeySet extends VersionedEntrySet {
//
//    class VersionedIterator extends VersionedEntrySet.VersionedIterator {
//      @Override
//      protected Object computeNext() {
//        VersionedSlot vs;
//        synchronized (VersionedHashMap.this) {
//          vs = (VersionedSlot) entry.getValue();
//        }
//        return vs.getValue();
//      }
//    }
//
//    @Override
//    public Iterator iterator() {
//      ensureDerived();
//      synchronized (VersionedHashMap.this) {
//      return new VersionedIterator();
//      }
//    }
//  }

  @Starts("nothing")
@Override
  public Set<Map.Entry<K,V>> entrySet() {
	throw new UnsupportedOperationException();
    //return myEntrySet;
  }

  @Starts("nothing")
@Override
  public Set<K> keySet() {
	throw new UnsupportedOperationException();	  
    //return myKeySet;
  }

  /** Returns true if we have a non-null mapping for this key.
   * @see java.util.Map#containsKey(java.lang.Object)
   */
  @Starts("nothing")
@Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  @SuppressWarnings({ "cast", "unchecked" })
  @Override
  public V put(K key, V value) {
    checkMutable();
    V oldValue;
    synchronized (this) {
      VersionedSlot<V> vs = (VersionedSlot<V>) map.get(key);
      if (vs == null) {
        vs = (VersionedSlot<V>) factory
            .predefinedSlot(null);
        map.put(key, vs);
      }
      oldValue = vs.getValue();
      VersionedSlot<V> newSlot = vs.setValue(Version.getVersionLocal(), value);
      if (newSlot != vs) {
        map.put(key, newSlot);
      }
    }
    if (value == null && oldValue != null)
      changeSize(+1);
    else if (value != null && oldValue == null)
      changeSize(-1);
    return oldValue;
  }

  @Starts("nothing")
@Override
  @SuppressWarnings({ "cast" })
  public V get(Object key) {
    ensureDerived();
    VersionedSlot<V> vs;
    synchronized (this) {
      vs = (VersionedSlot<V>) map.get(key);
    }
    if (vs == null)
      return null;
    return vs.getValue();
  }

  /**
   * Rather than remove the key, simply associate it with null.
   * @see java.util.Map#remove(java.lang.Object)
   * @see java.util.Map#put
   */
  @Starts("nothing")
@Override
  @SuppressWarnings("unchecked")
  public V remove(Object key) {
    return put((K) key, null);
  }
}