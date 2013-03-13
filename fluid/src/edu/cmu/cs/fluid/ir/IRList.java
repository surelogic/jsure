/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRList.java,v 1.67 2008/12/12 19:01:02 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import com.surelogic.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.util.*;

/** Variable size sequences with locations that stay valid under reshaping.
 * For efficiency, we have four different representations.  The most
 * general representation is the doubly-linked list.  This representation
 * is explained in the remainder of this description.
 * <p>
 * This implementation uses a free list.
 * This allows us to assign fixed IRLocation values
 * that persist over list changes, without having a pool of
 * nodes that grows larger and larger with each list change.
 * In order to have this free list work correctly,
 * the following invariants are needed:
 * <ol>
 * <li> The last element in the free list
 * is always the element with the highest ID.
 * This permits us to attach new nodes to the end of the free list
 * in constant time.
 * <li> The free list does not use the "prev" link.  It doesn't need to be changed.
 * <li> The (next) links between elements in the free list are always valid:
 * they are initial values.  This allows us to treat the free list as
 * infinite in length, it just hasn't all been revealed yet.
 * </ol>
 * We keep the first invariant by putting newly allocated
 * elements on the end of the list and reclaimed nodes on the
 * front of the list.  The list is never permitted to go empty.
 * The first invariant is handled by always allocated a predefined
 * slot for the "next" field of the last free element
 * just before attaching a newly allocated node to the end of the list.
 * </p>
 */
@SuppressWarnings("unchecked")
@Region("ListState")
@InRegion("initialSize, sizeSlot into ListState")
@RegionLock("ListLock is this protects ListState")
public abstract class IRList<IntS,S,ES,T> extends IRAbstractSequence<S,T> {
	
  private static final Logger LOG = SLLogger.getLogger("FLUID.ir");
  public final ES noSlot = getElemStorage().newSlot(new IRLocation(-1));
  //private final SlotFactory slotFactory;

  @UniqueInRegion("ListState")
  private Header<IntS,S,ES,T> seq = EmptyHeader.prototype();	// dl-list of cells in sequence
  private int initialSize;	// starting size (often 0)
  private IntS sizeSlot;

  public IRList() {
    this(0);
  }
  
  public IRList(int startingSize) {
    this(null,startingSize);
  }
  
  // these will be calld on "raw" receivers:
  protected abstract SlotStorage<IntS,Integer> getIntSlotStorage();
  
  @Borrowed("this")
  protected abstract SlotStorage<S,T> getSlotStorage();
  protected abstract SlotStorage<ES,IRLocation> getElemStorage();
  
  /*public SlotFactory getSlotFactory() {
    return slotFactory;
  }*/
  
  @Unique("return")
  public IRList(IRState parent, int startingSize) {
    super(parent);
    // slotFactory = sf;
    initialSize = startingSize;
    // initializeSizeSlot(startingSize); // trying to remove this
    if (startingSize > 0) initialize(startingSize,false);
  }
  @RequiresLock("ListLock")
  @Borrowed("this")
  private void initializeSizeSlot(int size) {
    sizeSlot = getIntSlotStorage().newSlot(IntegerTable.newInteger(size));
  }
  
  @Containable
  private class SlotList extends ArrayList<S> {
	@Unique("return")
	SlotList(SlotStorage<S, T> st, int capacity) {
		super(capacity);
	    for (int i=0; i < capacity; ++i) {
	        super.add(st.newSlot());
	    }
	}
	@Borrowed("this")
	private void checkSize() {
		if (super.size() < IRList.this.size()) {
			LOG.severe("BUG in IRList: elemSlots shorter than size!");
			IRList.this.describe(System.err);
		}
	}
	
	@Override 
	@Borrowed("this")
	public boolean add(S e) {
		checkSize();
		return super.add(e);
	}
	@Starts("nothing")
	@Override
	@Borrowed("this")
	public boolean remove(Object o) {
		checkSize();
		return super.remove(o);
	}
	@Borrowed("this")
	public void fillUpTo(final int i, S val) {
		while (i >= super.size()) {
	        super.add(val);
	    }
	}
  }
  
  /**
   * Called before we add the first element
   */
  @RequiresLock("ListLock")
  @Borrowed("this")
  private void initialize(int size, boolean reverse) {
    if (sizeSlot == null) initializeSizeSlot(size);
    SlotStorage<S,T> st = getSlotStorage();
    SlotList slots = new SlotList(st, size);
    /* Moved to SlotList constructor
    for (int i=0; i < size; ++i) {
      slots.add(st.newSlot());
    }
    */
    if (reverse) {
      seq = new InsertHeader(slots);
    } else {
      seq = new AppendHeader(slots);
    }
  }
  @RequiresLock("ListLock")
  private void checkInitialized(boolean reverse) {
    if (seq instanceof EmptyHeader) noLongerEmpty(reverse);
  }
  @RequiresLock("ListLock")
  private void noLongerEmpty(boolean reverse) {
    initialize(0,reverse);
  }

  /**
   * Called before wendo an operation that requires that the header be fully general
   */
  @RequiresLock("ListLock")
  private void checkGeneral(String reason) {
    checkInitialized(false);
    if (seq instanceof IRList.FullHeader) return;
    LOG.fine("Need to generalize list because of " + reason + " operation");
    generalize();
  }
  @RequiresLock("ListLock")
  private void generalize() {
    seq = seq.generalize();
  }
  
  // private final IRType locationType = new IRListLocationType(this);

  IRType<IRLocation> getLocationType() {
    return IRListLocationType.prototype;
  }
  IRType<IRLocation> getLocationType(IRInput in) {
    if (in.getRevision() < 5) {
      // System.out.println("creating list location type");
      return new IRListLocationType(this); // return locationType;
    } else {
      return IRListLocationType.prototype; 
    }
  }

  protected static final Integer NO_SIZE = -1; // for reading persistent < 1.5
  @Override
  public synchronized int size() {
    if (sizeSlot == null) return 0;
    int s = getIntSlotStorage().getSlotValue(sizeSlot);
    if (s == NO_SIZE) {
      // Hack for pre 1.5 restored lists. (always full headers)
      s = 0;
      IRLocation loc = seq.first();
      while (loc != null) {
        ++s;
        loc = seq.next(loc);
      }
    }
    // LOG.info("size = " + s);
    return s;
  }
  @RequiresLock("ListLock")
  private void setSize(int s) {
    // if the size dips below the initial size before
    // converting this list to use a full header, 
    // code could break, but since removes are only allowed
    // after we have a full header, there is no problem.
    // thus we don't need to check whether the size dips
    // below the initial size.
    
    //SlotFactory sf = getSlotFactory();
    //System.out.println("Before setting, size slot = " + sizeSlot + ": ");
    //sf.describe(sizeSlot,System.out);
    sizeSlot = getIntSlotStorage().setSlotValue(sizeSlot, IntegerTable.newInteger(s));
    //System.out.println("After setting, size slot = " + sizeSlot + ": ");
    //sf.describe(sizeSlot,System.out);
  }
  synchronized void incSize() {
    setSize(size()+1);
  }
  synchronized void decSize() {
    setSize(size()-1);
  }

  @Override
  public boolean isVariable() {
    return true;
  }

  @Override
  public Iteratable<T> elements() {
    return getSlotFactory().newIterator(new IRSequenceIterator<T>(this));
  }

  @Override
  public synchronized boolean validAt(int i) {
    return validAt(location(i));
  }  
  @Override
  public synchronized boolean validAt(IRLocation loc) {
    return seq.isValidAt(validateLocation(loc));
  }

  @Override
  public synchronized T elementAt(int i) {
    return elementAt(location(i));
  }  
  @Override
  public synchronized T elementAt(IRLocation loc) {
    return seq.getElementAt(validateLocation(loc));
  }

  @Override
  public synchronized void setElementAt(T element, int i) {
    setElementAt(element,location(i));
  }  
  @Override
  public void setElementAt(T element, IRLocation loc) {
    synchronized (this) {
      seq.setElementAt(element,validateLocation(loc));
    }
    noteChanged();
  }

  @Override
  public IRLocation insertElement(T element) {
    IRLocation le;
    synchronized (this) {
      checkInitialized(true); // not necessarily general
      le = seq.insert(element);
      incSize();
      if (le == null) {
        LOG.severe("insert should not return null");
      }
    }
    noteChanged();
    return le;
  }

  @Override
  public IRLocation appendElement(T element) {
    IRLocation le = null;
    synchronized (this) {
      checkInitialized(false); // not necessarily general!
      le = seq.append(element);
      // System.out.println("Before append: size = " + size());
      incSize();
      // System.out.println("After append: size = " + size());
      if (le == null) {
        LOG.severe("append should not return null");
      }
    }
    noteChanged();
    return le;
  }

  @Override
  public IRLocation insertElementBefore(T element, IRLocation loc) {
    IRLocation le;
    synchronized (this) {
      checkGeneral("insertBefore");
      le = seq.insertBefore(validateLocation(loc), element);
      incSize();
      if (le == null) {
        LOG.severe("insertBefore should not return null");
      }
    }
    noteChanged();
    return le;
  }

  @Override
  public IRLocation insertElementAfter(T element, IRLocation loc) {
    IRLocation le;
    synchronized (this) {
      checkGeneral("insertAfter");
      le = seq.insertAfter(validateLocation(loc), element);
      incSize();
      if (le == null) {
        LOG.severe("insertAfter should not return null");
      }
    }
    noteChanged();
    return le;
  }

  @Override
  public void removeElementAt(IRLocation loc) {
    synchronized (this) {
      checkGeneral("remove");
      seq.remove(validateLocation(loc));
      decSize();
    }
    noteChanged();
  }

  @Override
  public synchronized IRLocation location(int i) {
    IRLocation le = seq.at(i);
    if (le == null) {
      throw new IndexOutOfBoundsException("no location for given index");
    }
    return le;
  }

  @Override
  public synchronized int locationIndex(IRLocation loc) {
    return seq.locate(validateLocation(loc));
  }
  @RequiresLock("ListLock")
  private IRLocation validateLocation(IRLocation loc) {
    IRLocation loc2 = seq.validateLocation(loc);
    if (loc != null && loc2 == null) {
      LOG.warning("validating " + loc + " gave null");
    }
    return loc2;
  }

  @Override
  public boolean hasElements() {
    return size() != 0;
  }

  @Override
  public synchronized IRLocation firstLocation() {
    return seq.first();
  }

  @Override
  public synchronized IRLocation lastLocation() {
    return seq.last();
  }

  @Override
  public synchronized IRLocation nextLocation(IRLocation loc) {
    return seq.next(validateLocation(loc));
  }

  @Override
  public synchronized IRLocation prevLocation(IRLocation loc) {
    return seq.prev(validateLocation(loc));
  }

  @Override
  public synchronized int compareLocations(IRLocation loc1, IRLocation loc2) {
    return seq.comparePlacements(validateLocation(loc1),
				 validateLocation(loc2));
  }

  @Override
  public void writeValue(IROutput out)
       throws IOException
  {
    out.writeSlotFactory(getSlotFactory());
    out.writeInt(initialSize);
  }
  
  public static <T> IRSequence<T> readValue(IRInput in, IRSequence<T> current)
       throws IOException
  {
    SlotFactory sf = in.readSlotFactory();
    int initialSize = in.readInt();
    if (current != null) {
      if (!current.isVariable())
	throw new IOException("re-reading list with new parameters");
      return current;
    }
    return sf.getOldFactory().<T>newSequence(~initialSize);
  }

  @Override
  public synchronized void writeContents(IRCompoundType t, IROutput out) throws IOException
  {
    /* Some of the values read/written here may be redundant,
     * since we just created the list with its initial size.
     * But to avoid this problem would be very tricky.
     * We'd have to do something like "changed" contents
     * for the head,tail,free,prev&next fields
     * but do the regular contents thing for the elements.
     * If/when we make this change, we will need to
     * "up" the revision number in IRPersistent, because
     * it is not forward compatible.
     */
    if (out.debug()) System.out.println("writeContents(...) called");
    //SlotFactory sf = getSlotFactory();
    if (sizeSlot == null) {
      initializeSizeSlot(initialSize);
    }
    getIntSlotStorage().writeSlotValue(sizeSlot,IRIntegerType.prototype,out);
    IRType<T> et = t.getType(0); // must be homogenous
    // for persistent 1.5, we write a reference to the object
    seq.writeContents(et,out);
  }

  @Override
  public synchronized void readContents(IRCompoundType t, IRInput in) throws IOException {
    if (in.debug()) System.out.println("readContents(...) called");
    IRType<T> et = t.getType(0); // must be homogenous
    int b = 'F';
    //SlotFactory sf = getSlotFactory();
    if (sizeSlot == null) initializeSizeSlot(initialSize);
    if (in.getRevision() >= 5) {
      sizeSlot = getIntSlotStorage().readSlotValue(sizeSlot,IRIntegerType.prototype,in);
      b = in.readByte();
    } else {
      LOG.warning("Pre 1.5 lists do not include size, versioned size() will be wrong.");
      sizeSlot = getIntSlotStorage().newSlot(NO_SIZE);
    }
    switch (b) {
    case 'Z':
      // do nothing
      in.debugMark("empty_header");
      break;
    case 'S':
    case 'I':
      in.debugMark("simple_header");
      checkInitialized(b =='I');
      // System.out.println("Reading simple list info");
      // full header can handle being called here
      seq.readContents(et, in);
      break;
    case 'F':
      in.debugMark("full_header");
      checkGeneral("read full");
      // System.out.println("Reading full list info");
      seq.readContents(et, in);
      break;
    default:
      throw new IOException("can't handle IRList rep = " + b + " yet.");
    }
  }

  @Override
  public synchronized boolean isChanged() {
    SlotStorage<IntS,Integer> ss = getIntSlotStorage();
    if (ss.isChanged(sizeSlot) || seq.isChanged()) return true;
    return false;
  }

  @Override
  public synchronized void writeChangedContents(IRCompoundType t, IROutput out)
       throws IOException
  {
    IRType et = t.getType(0); // must be homogenous
    // for persistent 1.5, we write a reference to the object
    out.writeCachedObject(this);
    SlotStorage<IntS,Integer> ss = getIntSlotStorage();
    if (ss.isChanged(sizeSlot)) {
      out.writeBoolean(true);
      ss.writeSlotValue(sizeSlot,IRIntegerType.prototype,out);
    } else {
      out.writeBoolean(false);
    }
    // or rather we put it in the Input stream so we can get it out when
    // we read IRListLocation types.  That is why we don't bother to write ourselves.
    seq.writeChangedContents(et,out);
  }

  @Override
  public synchronized void readChangedContents(IRCompoundType t, IRInput in)
       throws IOException 
  {
    IRType et = t.getType(0); // must be homogenous
    //SlotFactory sf = getSlotFactory();
    SlotStorage<IntS,Integer> ss = getIntSlotStorage();
    if (in.getRevision() >= 5 && in.readCachedObject() == null) {
      in.cacheReadObject(this); // doesn't need to be read.  We know who we are.
      // put this makes the reference available for IRListLocationType.
    }
	  int b = 'F';
	  if (in.getRevision() >= 5) {
	    if (in.readBoolean()) {
	      sizeSlot = ss.readSlotValue(sizeSlot,IRIntegerType.prototype,in);
	    }
	    b = in.readByte();
	  } else if (!ss.isValid(sizeSlot) || !NO_SIZE.equals(ss.getSlotValue(sizeSlot))) {
	    sizeSlot = ss.newSlot(NO_SIZE);
	    LOG.warning("Cannot correctly read pre 1.5 IRLists (no size)");
	  }
	  switch (b) {
    case 'Z':
      // do nothing
      in.debugMark("empty_header");
      break;
    case 'S':
    case 'I':
      in.debugMark("simple_header");
      // System.out.println("Reading simple list changed info");
      checkInitialized(b =='I');
      // full header can handle being called here
      seq.readChangedContents(et, in);
      break;
    case 'F':
      in.debugMark("full_header");
      checkGeneral("read changed full");
      seq.readChangedContents(et, in);
      break;
    default:
      throw new IOException("Cannot handle IRList rep = " + b);
    }    
  }

  @Override
  public synchronized void describe(PrintStream out) {
    out.println("IRList with size: ");
    getIntSlotStorage().describe(sizeSlot,out);
    seq.describe(out);
  }
  
  //===========================================================
  // Implementation of List
  //===========================================================
  
  private interface Locator {
    IRLocation first(IRSequence seq);
    IRLocation next(IRSequence seq, IRLocation loc);
  }
  
  private static Locator fromFront = new Locator() {
    @Override
    @SuppressWarnings("unchecked")
    public IRLocation first(IRSequence seq) {
      return seq.firstLocation();
    }
    @Override
    public IRLocation next(IRSequence seq, IRLocation loc) {
      return seq.nextLocation(loc);
    }
  };
  
  private static Locator fromBack = new Locator() {
    @Override
    @SuppressWarnings("unchecked")
    public IRLocation first(IRSequence seq) {
      return seq.lastLocation();
    }
    @Override
    public IRLocation next(IRSequence seq, IRLocation loc) {
      return seq.prevLocation(loc);
    }
  };
  
  private IRLocation locationOf(Locator l, Object o) {
    IRLocation loc = l.first(this);
    if (o != null) {    
      while (loc != null) {
        if (o.equals(elementAt(loc))) {
          return loc;
        }
        loc = l.next(this, loc);
      }
    } else {
      while (loc != null) {
        if (elementAt(loc) == null) {
          return loc;
        }
        loc = l.next(this, loc);
      }
    }
    return null;
  }
  
  @Override
  @Starts("nothing")
public boolean contains(Object o) {
    return locationOf(fromFront, o) != null;
  }
  
  @Override
  @Starts("nothing")
public Object[] toArray() {
    int size   = size();
    Object[] a = new Object[size];
    int i = 0;
    for(Object o : this) {
      a[i] = o;
      i++;
    }
    return a;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E[] toArray(E[] a) {
    int size = size();    
    if (a.length < size) {
      a = (E[])java.lang.reflect.Array.
      newInstance(a.getClass().getComponentType(), size);
    }
    int i = 0;
    for(Object o : this) {
      a[i] = (E) o;
      i++;
    }
    if (a.length > size)
      a[size] = null;
    return a;    
  }

  @Override
  public boolean add(T val) {    
    appendElement(val);
    return true;
  }

  @Override
  @Starts("nothing")
public boolean remove(Object o) {
    IRLocation loc = locationOf(fromFront, o);
    if (loc != null) {
      removeElementAt(loc);
      return true;
    }
    return false;
  }

  @Override
  @Starts("nothing")
public int indexOf(Object o) {
    IRLocation loc = locationOf(fromFront, o);
    if (loc != null) {
      return locationIndex(loc);
    }
    return -1;
  }

  @Override
  @Starts("nothing")
public int lastIndexOf(Object o) {
    IRLocation loc = locationOf(fromBack, o);
    if (loc != null) {
      return locationIndex(loc);
    }
    return -1;
  }
  
  @Override
  protected ListIteratable<T> createListIterator(int start) {
    return new IRSequence_ListIterator<T>(this, start);
  }
  
  //===========================================================
  // END Implementation of List
  //===========================================================
  
  /**
   * The structure for the list nodes.
   * All these methods assume that the list is properly
   * synchronizing all accesses.
   * @author boyland
   */
  @Promise("@Borrowed(this) for *(**)")
  static interface Header<IntS,S,ES,T> {

    public IRLocation validateLocation(IRLocation loc);

    public boolean isValidAt(IRLocation loc);
    public T getElementAt(IRLocation loc);
    public void setElementAt(T element, IRLocation loc);
    
    /**
     * @param i
     * @return
     */
    IRLocation at(int i);

    /**
     * @param loc
     * @return
     */
    int locate(IRLocation loc);

    /**
     * @return
     */
    IRLocation first();

    /**
     * @return
     */
    IRLocation last();

    public IRLocation next(IRLocation loc);
    public IRLocation prev(IRLocation loc);
    
    /**
     * @param element
     * @param element2
     * @return
     */
    int comparePlacements(IRLocation element, IRLocation element2);


    /**
     * @param elem
     */
    IRLocation insert(T elem);

    /**
     * @param elem
     */
    IRLocation append(T elem);

    /**
     * @param loc
     * @param elem
     */
    IRLocation insertBefore(IRLocation loc, T elem);

    /**
     * @param loc
     * @param elem
     */
    IRLocation insertAfter(IRLocation loc, T elem);

    /**
     * @param loc
     */
    void remove(IRLocation loc);
    

    /**
     * @param et
     * @param in
     */
    void readContents(IRType<T> et, IRInput in) throws IOException;

    /**
     * @param et
     * @param out
     */
    void writeContents(IRType<T> et, IROutput out) throws IOException;

    /**
     * @return
     */
    boolean isChanged();

    /**
     * @param et
     * @param in
     */
    void readChangedContents(IRType<T> et, IRInput in) throws IOException;

    /**
     * @param et
     * @param out
     */
    void writeChangedContents(IRType<T> et, IROutput out) throws IOException;

    /**
     * Generalize this header to one that handles everything.
     * @return a full header that has the same information
     */
    Header<IntS,S,ES,T> generalize();
    
    void describe(PrintStream out);
  }
  
  /**
   * The header for a list that has no nodes and never has.
   * @author boyland
   */
  @Promise("@Borrowed(this) for public *(**)")
  static class EmptyHeader<IntS,S,ES,T> implements Header<IntS,S,ES,T> {
    static final EmptyHeader prototype = new EmptyHeader();
    
    @SuppressWarnings("unchecked")
    static final <IntS,S,ES,T> EmptyHeader<IntS,S,ES,T> prototype() { return prototype; }
    
    @Override
    public T getElementAt(IRLocation loc) {
      throw new IndexOutOfBoundsException("no elements in empty list");
    }
    @Override
    public boolean isValidAt(IRLocation loc) {
      return false;
    }
    @Override
    public void setElementAt(Object element, IRLocation loc) {
      throw new IndexOutOfBoundsException("no elements in empty list");
    }
    @Override
    public IRLocation validateLocation(IRLocation loc) {
      throw new IndexOutOfBoundsException("no elements in empty list");
    }
    @Override
    public void readChangedContents(IRType et, IRInput in) throws IOException {
    }
    @Override
    public void writeChangedContents(IRType et, IROutput out) throws IOException {
      out.debugMark("empty_header");
      out.writeByte('Z');
    }
    @Override
    public boolean isChanged() {
      return false;
    }
    @Override
    public void readContents(IRType et, IRInput in) throws IOException {
    }
    @Override
    public void writeContents(IRType et, IROutput out) throws IOException {
      out.debugMark("empty_header");
      out.writeByte('Z');
    }

    @Override
    public int comparePlacements(IRLocation loc1, IRLocation loc2) {
      throw new IndexOutOfBoundsException("no element in empty list");
    }
    @Override
    public IRLocation last() {
      return null;
    }
    @Override
    public IRLocation first() {
      return null;
    }
    @Override
    public IRLocation next(IRLocation loc) {
      throw new IndexOutOfBoundsException("no location in empty list");
    }
    @Override
    public IRLocation prev(IRLocation loc) {
      throw new IndexOutOfBoundsException("no location in empty list");
    }

    @Override
    public int locate(IRLocation loc) {
      throw new IndexOutOfBoundsException("no element in empty list");
    }
    @Override
    public IRLocation at(int i) {
      throw new IndexOutOfBoundsException("no element in empty list");
    }

    @Override
    public void remove(IRLocation loc) {
      throw new IndexOutOfBoundsException("no element in empty list");
    }

    // these should be protected against in IRList
    @Override
    public IRLocation insertAfter(IRLocation loc, Object x) {
      throw new FluidError("should not be called");
    }
    @Override
    public IRLocation insertBefore(IRLocation loc, Object x) {
      throw new FluidError("should not be called");
    }
    @Override
    public IRLocation append(Object x) {
      throw new FluidError("should not be called");
    }
    @Override
    public IRLocation insert(Object x) {
      throw new FluidError("should not be called");      
    }
    @Override
    public Header<IntS,S,ES,T> generalize() {
      // TODO Auto-generated method stub
      return null;
    }
    @Override
    public void describe(PrintStream out) {
      out.println("IRList[0]");
    }

  }
  
  /**
   * A list in which we simple have a list of slots, one for each location.
   * This is permitted if nothing interesting happens to next/prev links.
   * @author boyland
   */
  @Promise("@Borrowed(this) for public *(**)")
  abstract class SimpleHeader extends CountInstances implements Header<IntS,S,ES,T> {
    protected final SlotList elemSlots;
    
    protected SimpleHeader(SlotList es) {
      elemSlots = es;
    }

    protected S getElemSlot(int i) {
      S emptySlot = getSlotStorage().newSlot();
      /*
      while (i >= elemSlots.size()) {
        elemSlots.add(emptySlot);
      }
      */
      elemSlots.fillUpTo(i, emptySlot);
      return elemSlots.get(i);
    }

    @Override
    public IRLocation validateLocation(IRLocation loc) {
      /* Moved to SlotList.checkSize
      if (elemSlots.size() < size()) {
        LOG.severe("BUG in IRList: elemSlots shorter than size!");
        IRList.this.describe(System.err);
      }
      */
      return loc;
    }

    @Override
    public boolean isValidAt(IRLocation loc) {
      int i = loc.getID();
      if (i < 0 || i >= size() || i >= elemSlots.size()) return false;
      return getSlotStorage().isValid(elemSlots.get(i));
    }

    @Override
    public T getElementAt(IRLocation loc) {
      return getSlotStorage().getSlotValue(elemSlots.get(loc.getID()));
    }

    @Override
    public void setElementAt(T element, IRLocation loc) {
      SlotStorage<S,T> sf = getSlotStorage();
      int i = loc.getID();
      S slotState = elemSlots.get(i);
      slotState = sf.setSlotValue(slotState,element);
      elemSlots.set(i,slotState);
    }

    @Override
    public IRLocation insertAfter(IRLocation loc, T x) {
      throw new FluidError("should not be called");
    }

    @Override
    public IRLocation insertBefore(IRLocation loc, T x) {
      throw new FluidError("should not be called");
    }

    @Override
    public void remove(IRLocation loc) {
      throw new FluidError("should not be called");
    }

    @Override
    public void writeContents(IRType et, IROutput out) throws IOException {
      out.debugMark("simple_header");
      out.writeByte(getHeaderByte());
      SlotStorage<S,T> sf = getSlotStorage();
      out.writeByte(0); // no head, tail or free
      int n = elemSlots.size();
      if (out.debug()) System.out.println("List has " + n + " elements");
      out.writeInt(n);
      for (int i=0; i < n; ++i) {
        if (out.debug()) System.out.println("  " + i);
        S data = elemSlots.get(i);
        if (sf.isValid(data)) {
          out.writeByte(Element.ELEM_FLAG);
          sf.writeSlotValue(data,et,out);
        } else {
          out.writeByte(0);
        }        
      }
      if (out.debug()) System.out.println();
    }

    @Override
    public void readContents(IRType et, IRInput in) throws IOException {
      in.debugMark("simple_header");
      SlotStorage<S,T> sf = getSlotStorage();
      if (in.readByte() != 0) {
        throw new IOException("simple header has no head/tail/free");
      }
      int n = in.readInt();
      if (in.debug()) 
        System.out.println("List has " + n + " elements");
      for (int i=0; i < n; ++i) {
        if (in.debug()) System.out.println("  "+i);
        S data = getElemSlot(i);
        int flags = in.readByte();
        if (flags == 0) continue;
        if (flags != Element.ELEM_FLAG) {
          throw new IOException("Too complex a flag " + flags);
        }
        elemSlots.set(i,sf.readSlotValue(data,et,in));
      }
    }

    @Override
    public boolean isChanged() {
      SlotStorage<S,T> sf = getSlotStorage();
      for (Iterator<S> it = elemSlots.iterator(); it.hasNext();) {
        if (sf.isChanged(it.next())) return true;
      }
      return false;
    }

    @Override
    public void writeChangedContents(IRType et, IROutput out) throws IOException {
      out.writeByte(getHeaderByte());
      SlotStorage<S,T> sf = getSlotStorage();
      int n = elemSlots.size();
      out.writeByte(0); // no head/tail/free
      boolean allChanged = true;
      for (Iterator<S> it = elemSlots.iterator(); it.hasNext();) {
        if (!sf.isChanged(it.next())) allChanged = false;
      }
      out.writeBoolean(allChanged);
      if (allChanged) out.writeInt(n);
      for (int i = 0; i < n; ++i) {
        S data = elemSlots.get(i);
        if (allChanged) {
          out.writeByte(Element.ELEM_FLAG);
          sf.writeSlotValue(data,et,out);
        } else {
          if (sf.isChanged(data)) {
            out.writeInt(i);
            out.writeByte(Element.ELEM_FLAG);
            sf.writeSlotValue(data,et,out);            
          }
        }
      }
      if (!allChanged) out.writeInt(-1);      
    }

    public synchronized void readChangedContents(IRType<T> et, IRInput in) throws IOException {
      SlotStorage<S,T> sf = getSlotStorage();
      if (in.readByte() != 0) {
        throw new IOException("Unexpected change flags for simple header");
      }
      boolean allChanged = in.readBoolean();
      if (allChanged) {
        int n = in.readInt();
        for (int i=0; i < n; ++i) {
          S data = getElemSlot(i);
          if (in.readByte() != Element.ELEM_FLAG) {
            throw new IOException("Expected 4");
          }
          data = sf.readSlotValue(data,et,in);
          elemSlots.set(i,data);
        }
      } else {
        int i;
        while ((i = in.readInt()) != -1) {
          S data = getElemSlot(i);
          if (in.readByte() != Element.ELEM_FLAG) {
            throw new IOException("Expected 4");
          }
          data = sf.readSlotValue(data,et,in);
          elemSlots.set(i,data);          
        }
      }
    }
    protected abstract byte getHeaderByte();

    /**
     * Add a new element to the sequence, with the given initialization.
     * @param elem
     */
    protected void addElem(T elem) {
      SlotStorage<S,T> sf = getSlotStorage();
      S s = sf.newSlot();
      /*
       * We have to create the slot and then initialize in two steps
       * to ensure that the persistence system realizes that this slot is "changed".
       * BUG 892.
       */
      s = sf.setSlotValue(s, elem);
      elemSlots.add(s);
    }
  }
  
  /**
   * Used when the list only has things appended to it.
   * @author boyland
   */
  @Promise("@Borrowed(this) for public *(**)")
  class AppendHeader extends SimpleHeader {
    public AppendHeader(SlotList es) {
      super(es);
    }
    
    public IRLocation at(int i) {
      if (i < 0 || i >= size()) return null;
      return IRLocation.get(i);
    }
    public int locate(IRLocation loc) {
      if (loc == null) return 0;
      return loc.getID();
    }
    public IRLocation first() {
      if (size() == 0) return null;
      return IRLocation.zeroPrototype;
    }
    public IRLocation last() {
      if (size() == 0) return null;
      return IRLocation.get(size()-1);
    }
    public IRLocation prev(IRLocation loc) {
      int i = loc.getID();
      if (i == 0) return null;
      return IRLocation.get(i-1);
    }
    public IRLocation next(IRLocation loc) {
      int i = loc.getID();
      if (++i >= size()) return null; // may have gone back in time.
      return IRLocation.get(i);
    }
    
    public int comparePlacements(IRLocation loc1, IRLocation loc2) {
      return loc1.getID() - loc2.getID();
    }
    @RequiresLock("IRList.this:ListLock")
    public IRLocation append(T elem) {
      if (elemSlots.size() != size()) {
        checkGeneral("append (bad)");
        return seq.append(elem);
      }
      addElem(elem);
      return IRLocation.get(elemSlots.size()-1);
    }
    @RequiresLock("IRList.this:ListLock")
    public IRLocation insert(T x) {
      checkGeneral("insert");
      return seq.insert(x);
    }
    
    @Override
    protected byte getHeaderByte() {
      return 'S';
    }
    @RequiresLock("IRList.this:ListLock")
    public Header<IntS,S,ES,T> generalize() {
      return new FullHeader(elemSlots,false);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.ir.IRList.Header#describe(java.io.PrintStream)
     */
    public void describe(PrintStream out) {
      int n = elemSlots.size();
      SlotStorage<S,T> sf = getSlotStorage();
      out.println("IRList[" + n + "]");
      for (int i=0; i < n; ++i) {
        S data = elemSlots.get(i);
        out.print(" [" + i + "] => ");
        sf.describe(data,out);
      }     
    }
    
  }

  @Promise("@Borrowed(this) for public *(**)")
  class InsertHeader extends SimpleHeader {
    public InsertHeader(SlotList es) {
      super(es);
    }
    
    public IRLocation at(int i) {
      if (i < 0 || i >= size()) return null;
      return IRLocation.get(size()-1-i);
    }
    public int locate(IRLocation loc) {
      return size()-1-loc.getID();
    }
    public IRLocation first() {
      if (size() == 0) return null;
      return IRLocation.get(size()-1);
    }
    public IRLocation last() {
      if (size() == 0) return null;
      return IRLocation.get(0);
    }
    public IRLocation next(IRLocation loc) {
      int i = loc.getID();
      if (i == 0) return null;
      return IRLocation.get(i-1);
    }
    public IRLocation prev(IRLocation loc) {
      int i = loc.getID();
      if (++i >= size()) return null; // may have gone back in time.
      return IRLocation.get(i);
    }
    
    public int comparePlacements(IRLocation loc1, IRLocation loc2) {
      return loc2.getID() - loc1.getID();
    }

    @RequiresLock("IRList.this:ListLock")
    public IRLocation append(T elem) {
      checkGeneral("append");
      return seq.append(elem);
    }
    @RequiresLock("IRList.this:ListLock")
    public IRLocation insert(T elem) {
      if (elemSlots.size() != size()) {
        // can't handle this
        checkGeneral("insert (bad)");
        return seq.insert(elem);
      }
      addElem(elem);
      return IRLocation.get(elemSlots.size()-1);
    }
    
    @Override
    protected byte getHeaderByte() {
      return 'I';
    }
    @RequiresLock("IRList.this:ListLock")
    public FullHeader generalize() {
      return new FullHeader(elemSlots,true);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.ir.IRList.Header#describe(java.io.PrintStream)
     */
    public void describe(PrintStream out) {
      int n = elemSlots.size();
      SlotStorage<S,T> sf = getSlotStorage();
      out.println("IRList[" + n + "]");
      for (int i=0; i < n; ++i) {
        S data = elemSlots.get(i);
        out.print(" [" + (n-1-i) + "] => ");
        sf.describe(data,out);
      }     
    }
    
  }

  /**
   * The header used in the full general case.
   * It takes over from the simple case in which things are added only
   * at the end.
   * <p>
   * We assume that when this header is created, it is possible that the
   * list went through several lengthening extensions in which new locations were
   * added to the end.  We don't have the (versioned) slot information to handle
   * this and so we make use of the sentinel IRLocation to mark this point in
   * time.  The sentinel will only be used with versioned slots (or some
   * hypothetical slot which remembers history).  When asked for the "next" slot or
   * the last slot, we check against the sentinel first.
   * <p>
   * This class is also used as a fall-back for "insert headers" where the list elements 
   * are each inserted in reverse order.  In this case "head" and "prev" links require
   * the use of the sentinel.  Happily, we never need to use the sentinel in the same
   * place in different ways.
   * <p>
   * Modifications make this information incorrect, because the nodes will no longer
   * be in order, and the current size doesn't tell us what comes next.  Thus
   * whenever we have a modification when the current state has a sentinel (we check
   * "head" and "tail"), we convert the information to the full information (without sentinels)
   * for the new versions branching off here.  This involves changing a versioned slot,
   * but since we're doing a modification already, the delta is folded into the
   * modification itself.
   * <p>
   * If we have something like a versioned this, this converting of sentinel information
   * into real information can happen over and over, an unbounded number of times.
   * (One may continually return to the last version that originally used the simple
   * header.)
   * However, normally, it happens only once.
   * <p>
   * NB: we handle the initialSize special to avoid having sentinels everywhere.
   * We assume that the number of elements never goes below the initial size.
   * @author boyland
   */
  @Promise("@Borrowed(this) for public *(**)")
  class FullHeader extends CountInstances implements Header<IntS,S,ES,T> {
    protected ES head, tail; // valid only if size() > 0
    private ES free;			// "infinite" list of new cells.
    // private int nextid = 0;		// next cell id in free list
    protected final List<Element> elements; // index from cell-id to cell

    @RequiresLock("IRList.this:ListLock")
    public FullHeader(List<S> elemSlots, boolean reverse) {
      int maxSize = elemSlots.size();
      elements = new Vector<Element>(maxSize);
      for (int i=0; i<maxSize; ++i) {
        elements.add(new Element(i,elemSlots.get(i)));
      }
      allocFirst();   
      if (reverse) initBackward(maxSize);
      else initForward(maxSize);
    }

    /** Allocate a new sequence cell and put at end of free list. */
    @Borrowed("this")
    void alloc() {
      int nextid = elements.size();
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element le =
        new Element(nextid++,getSlotStorage().newSlot());
      elements.add(le);
      /*
       * We initialize the element slot so that the persistence mechanism 
       * will see that it is changed when this node is added to the list:
       * otherwise the initial value assigned will not show up as a change
       * and be missed as in Bug 892.
       */
      le.elem = getSlotStorage().newSlot();
      le.prev = noSlot;
      le.next = noSlot;
      if (free == null) {
        // le.prev = sf.newSlot(null);
        free = ls.newSlot(le);
      } else {
        Element last = elements.get(nextid-2);
        // le.prev = sf.newSlot(last);
        // NB: last.next needs to be made a predefined value:
        last.next = ls.newSlot(le);
      }
      /* le.next will need to be converted
       * into a predefined slot before being used.
       */
    }
      
    /** Allocate the first element in the infinite sequence.
     * Its prev field is preset to null.
     * Its next field is <em>not</em> set until later,
     * until we know what its <em>initial</em> value will be,
     * @see #newElement()
     */
    @Borrowed("this")
    void allocFirst() {
      alloc(); // does all the work for us.
    }

    /**
     * Initialize a full header to handle the fact that it was previously an
     * AppendHeader.  We know that the list started with a specific number of
     * nodes and only added to the end, never removing anything.  The client
     * could have used versioning to "remove" things, but this only reverts to a
     * previous state.  In particular, the size() has <em>never</em> been less than
     * the initial size and never greater than the number of slots (maxSize).
     * @param maxSize the number of slots in the append header previously
     */
    @RequiresLock("IRList.this:ListLock")
    @Borrowed("this")
    protected void initForward(int maxSize) {
      //int size = size();
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      ES sentinelSlot = ls.newSlot(IRLocation.getSentinel());
      if (maxSize == 0) {
        head = ls.newSlot(null);
        tail = ls.newSlot(null);
      } else {
        Element p = elements.get(0);
        head = ls.newSlot(p); // doesn't need to be right if size() == 0
        p.prev = ls.newSlot(null);
        for (int i = 1; i < maxSize; ++i) {
          Element n = elements.get(i);
          if (initialSize > i) { // next has never been anything other than to next node
            p.next = ls.newSlot(n);
          } else {
            p.next = sentinelSlot; 
          }
          n.prev = ls.newSlot(p); // safe to be unversioned
          p = n;
        }
        p.next = ls.newSlot(null);
        if (initialSize == maxSize) { // size has never changed
          tail = ls.newSlot(p);
        } else {
          tail = sentinelSlot;
        }
      }
    }

    /**
     * Initialize the full header to handle the case that the
     * slots were originally in an {@link InsertHeader}.
     * @param maxSize size of slot list in insert header
     */
    @RequiresLock("IRList.this:ListLock")
    @Borrowed("this")
    protected void initBackward(int maxSize) {
      //int size = size();
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      ES sentinelSlot = ls.newSlot(IRLocation.getSentinel());
      if (maxSize == 0) {
        head = ls.newSlot(null);
        tail = ls.newSlot(null);
      } else {
        Element n = elements.get(0);
        tail = ls.newSlot(n); // doesn't need to be right if size() == 0
        n.next = ls.newSlot(null);
        for (int i = 1; i < maxSize; ++i) {
          Element p = elements.get(i);
          if (initialSize > i) { // this node was brought into life with this "prev"
            n.prev = ls.newSlot(p);
          } else {
            n.prev = sentinelSlot; 
          }
          p.next = ls.newSlot(n); // safe to be unversioned
          n = p;
        }
        n.prev = ls.newSlot(null);
        if (initialSize == maxSize) { // list has never changed size
          head = ls.newSlot(n);
        } else {
          head = sentinelSlot;
        }
      }
    }

    /**
     * make sure we are ready to mutate slots.  This can only
     * we done when no slots have sentinel state. Since if any
     * have sentintel state, then the head or tail will, we use this to check.
     */
    protected void noSentinelsBeforeMutation() {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      IRLocation sentinel = IRLocation.getSentinel();
      int size = size();
      if (ls.getSlotValue(tail) == sentinel || ls.getSlotValue(head) == sentinel) {
        IRLocation latest = size == 0 ? null : (IRLocation)elements.get(size()-1);
        if (ls.getSlotValue(head) == sentinel) {
          head = ls.setSlotValue(head,latest);
        }
        if (ls.getSlotValue(tail) == sentinel) {
          tail = ls.setSlotValue(tail,latest);
        }
        // don't fiddle with the free cell at the end of the free list
        for (int i=0; i < elements.size()-1; ++i) {
          Element node = elements.get(i);
          if (ls.getSlotValue(node.next) == sentinel) {
            node.next = ls.setSlotValue(node.next, size == (i+1) ? null : elements.get(i+1));
          }
          if (ls.getSlotValue(node.prev) == sentinel) {
            node.prev = ls.setSlotValue(node.prev, size == (i+1) ? null : elements.get(i+1));
          }
        }
      }
    }
    
    Element lookup(int id) {
      while (id >= elements.size()) {
        alloc();
      }
      return elements.get(id);
    }

    public Element validateLocation(IRLocation loc) {
      if (loc instanceof IRList.Element && ((Element) loc).getHeader() == IRList.this) {
        return (Element) loc;
      } else if (loc == null) {
        return null;
      } else {
        return lookup(loc.getID());
      }
    }
    
    public T getElementAt(IRLocation loc) {
      return ((Element)loc).getElement();
    }
    public boolean isValidAt(IRLocation loc) {
      return ((Element)loc).isValid();
    }
    public void setElementAt(T element, IRLocation loc) {
      ((Element)loc).setElement(element);
    }
    
    /**
     * Return a sequence cell from the free list. We allocate from our
     * "infinite" list, adding new ones on the "end" as necessary. The way slots
     * are set is carefully managed to allow versioning to work correctly.
     */
    Element newElement() {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element first = (Element)ls.getSlotValue(free);
      if (first.getID() == (elements.size()-1)) {
        // we need to unroll our "infinite" list a little further
        // It doesn't hurt, if this happens too early.
        alloc();
      }
      Element next = (Element)ls.getSlotValue(first.next);
      /* Now unhook first from free list, and make next the first one */
      // was synchronized but moved to the whole list
      free = ls.setSlotValue(free,next);
      first.next = ls.setSlotValue(first.next,null);
      if (first.prev == noSlot) {
        first.prev = ls.newSlot(null);
      }
      // next.prev = ls.setSlotValue(next.prev,null);
      return first;
    }

    /** Allocate a new sequence cell and overwrite the value in it
     * to the parameter value.
     */
    Element newElement(T value) {
      Element le = newElement();
      le.setElement(value);
      return le;
    }

    /** Return a cell to the free list. */
    void freeElement(Element le) {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element first = (Element)ls.getSlotValue(free);
      le.next = ls.setSlotValue(le.next,first);
      // le.prev = ls.setSlotValue(le.prev,null);
      // first.prev = ls.setSlotValue(first.prev,le);
      // make sure free is not subject to race conditions.
      // (was synchronized, but moved to the whole method for simplicity)
      { free = ls.setSlotValue(free,le); }
    }
    @RequiresLock("IRList.this:ListLock")
    @Vouch("e.getNext() will use the same ListLock at runtime")
    public int locate(IRLocation loc) {
      if (!(loc instanceof IRList.Element)) {
        LOG.warning("expected branch");
        loc = elements.get(loc.getID());
      }
      Element elem = (Element)loc;
      int i=0;
      Element e = first();
      while (e != null && e != elem) {
        e = e.getNext();
        ++i;
      }
      if (e == null) return -1;
      else return i;
    }
    @RequiresLock("IRList.this:ListLock")
    public IRLocation at(int i) {
      if (i == -1) return null;
      IRLocation loc = first();
      while (i > 0 && loc != null) {
        loc = next(loc);
        --i;
      }
      return loc;
    }

    public Element first() {
      int size = size();
      if (size == 0) return null;
      IRLocation l = getElemStorage().getSlotValue(head);
      if (l == IRLocation.getSentinel()) {
        return elements.get(size-1);
      }
      return (Element) l;
    }

    public Element last() {
      int size = size();
      if (size == 0) return null;
      IRLocation l = getElemStorage().getSlotValue(tail);
      if (l == IRLocation.getSentinel()) {
        return elements.get(size-1);
      }
      return (Element)l;
    }
    @RequiresLock("IRList.this:ListLock")
    @Vouch("e.getNext() will use the same ListLock at runtime")
    public Element next(IRLocation loc) {
      Element e;
      if (loc instanceof IRList.Element) {
        e = (Element)loc;
      } else {
        LOG.warning("unexpected branch");
        e = elements.get(loc.getID());
        assert (loc.getID() == e.getID());
      }
      return e.getNext();
    }
    @RequiresLock("IRList.this:ListLock")
    @Vouch("e.getPrev() will use the same ListLock at runtime")
    public Element prev(IRLocation loc) {
      Element e;
      if (loc instanceof IRList.Element) {
        e = (Element)loc;
      } else {
        LOG.warning("unexpected branch");
        e = elements.get(loc.getID());
      }
      return e.getPrev();
    }
    
    public void index(Vector<Object> v) {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element e = (Element)ls.getSlotValue(head);
      while (e != null) {
        v.setElementAt(e,e.getID());
        e = (Element)ls.getSlotValue(e.next);
      }
    }
    @RequiresLock("IRList.this:ListLock")
    public int comparePlacements(IRLocation e1, IRLocation e2) {
      // could do a little better, but always worst case O(n)
      return locate(e1) - locate(e2);
    }

    private void addFirst(Element elem) {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      head = ls.setSlotValue(head,elem);
      tail = ls.setSlotValue(tail,elem);
      elem.prev = ls.setSlotValue(elem.prev,null);
      elem.next = ls.setSlotValue(elem.next,null);
    }

    public IRLocation insert(T x) {
      noSentinelsBeforeMutation();
      Element elem = newElement(x);
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element h = (Element)ls.getSlotValue(head);
      if (h == null) {
        addFirst(elem);
      } else {
        head = ls.setSlotValue(head,elem);
        h.prev = ls.setSlotValue(h.prev,elem);
        elem.prev = ls.setSlotValue(elem.prev,null);
        elem.next = ls.setSlotValue(elem.next,h);
      }
      return elem;
    }

    public IRLocation append(T x) {
      noSentinelsBeforeMutation();
      Element elem = newElement(x);
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element t = (Element)ls.getSlotValue(tail);
      if (t == null) {
        addFirst(elem);
      } else {
        t.next = ls.setSlotValue(t.next,elem);
        tail = ls.setSlotValue(tail,elem);
        elem.prev = ls.setSlotValue(elem.prev,t);
        elem.next = ls.setSlotValue(elem.next,null);
      }
      return elem;
    }

    public IRLocation insertBefore(IRLocation loc, T x) {
      noSentinelsBeforeMutation();
      Element n = (Element)loc;
      Element elem = newElement(x);
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element p = (Element)ls.getSlotValue(n.prev);
      n.prev = ls.setSlotValue(n.prev,elem);
      elem.prev = ls.setSlotValue(elem.prev,p);
      elem.next = ls.setSlotValue(elem.next,n);
      if (p == null) {
        head = ls.setSlotValue(head,elem);
      } else {
        p.next = ls.setSlotValue(p.next,elem);
      }
      return elem;
    }

    public IRLocation insertAfter(IRLocation loc, T x) {
      noSentinelsBeforeMutation();
      Element p = (Element)loc;
      Element elem = newElement(x);
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element n = (Element)ls.getSlotValue(p.next);
      p.next = ls.setSlotValue(p.next,elem);
      elem.prev = ls.setSlotValue(elem.prev,p);
      elem.next = ls.setSlotValue(elem.next,n);
      if (n == null) {
        tail = ls.setSlotValue(tail,elem);
      } else {
        n.prev = ls.setSlotValue(n.prev,elem);
      }
      return elem;
    }
    @RequiresLock("IRList.this:ListLock")
    @Vouch("e.getNext/Prev() will use the same ListLock at runtime")
    public void remove(IRLocation loc) {
      noSentinelsBeforeMutation();
      Element le = (Element)loc;
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      Element p = le.getPrev();
      Element n = le.getNext();
      if (p == null) {
        if (ls.getSlotValue(head) != le)
            throw new IRSequenceException("removing from wrong list");
        head = ls.setSlotValue(head, n);
        if (n == null) {
          tail = ls.setSlotValue(tail, p);
        } else {
          n.prev = ls.setSlotValue(n.prev, p);
        }
      } else {
        if (n == null) {
          if (ls.getSlotValue(tail) != le)
              throw new IRSequenceException("removing from wrong list");
          tail = ls.setSlotValue(tail, p);
        } else {
          n.prev = ls.setSlotValue(n.prev, p);
        }
        p.next = ls.setSlotValue(p.next, n);
      }
      freeElement(le);
    }

    public void writeContents(IRType<T> et, IROutput out) throws IOException {
      out.debugMark("full_header");
      out.writeByte('F');
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      IRType<IRLocation> locType = getLocationType();
      out.writeByte(7);
      ls.writeSlotValue(head,locType,out);
      ls.writeSlotValue(tail,locType,out);
      ls.writeSlotValue(free,locType,out);
      int nextid = elements == null ? 0 : elements.size();
      if (out.debug()) System.out.println("List has " + nextid + " elements");
      out.writeInt(nextid);
      for (int i=0; i < nextid; ++i) {
        if (out.debug()) System.out.println("  " + i);
        Element le = elements.get(i);
        le.writeContents(et,out);
      }
      if (out.debug()) System.out.println();
    }

    public synchronized void readContents(IRType<T> et, IRInput in) throws IOException {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      IRType<IRLocation> locType = getLocationType(in);
      int flags = (in.getRevision() < 5) ? 7 : in.readByte();
      //System.out.println("flags = " + flags);
      if ((flags & 1) != 0)
        head = ls.readSlotValue(head, locType, in);
      if ((flags & 2) != 0)
        tail = ls.readSlotValue(tail, locType, in);
      if ((flags & 4) != 0)
        free = ls.readSlotValue(free, locType, in);
      int n = in.readInt();
      if (in.debug()) 
        System.out.println("Full List has " + n + " elements");
      for (int i = 0; i < n; ++i) {
        lookup(i).readContents(et, in);
      }
    }

    public boolean isChanged() {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      if (ls.isChanged(head) || ls.isChanged(tail)) return true;
      if (free != noSlot && ls.isChanged(free)) return true;
      int nextid = elements == null ? 0 : elements.size();
      for (int i=0; i < nextid; ++i) {
        Element le = elements.get(i);
        if (le.isChanged()) return true;
      }
      return false;
    }

    public void writeChangedContents(IRType<T> et, IROutput out)
        throws IOException {
      out.debugMark("full_header");
      out.writeByte('F');
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      int flags = 0;
      if (ls.isChanged(head)) flags |= 1;
      if (ls.isChanged(tail)) flags |= 2;
      if (ls.isChanged(free)) flags |= 4;
      out.writeByte(flags);
      IRType<IRLocation> locType = getLocationType();
      if ((flags & 1) != 0) {
        ls.writeSlotValue(head, locType, out);
      }
      if ((flags & 2) != 0) {
        ls.writeSlotValue(tail, locType, out);
      }
      if ((flags & 4) != 0) {
        ls.writeSlotValue(free, locType, out);
      }
      boolean allChanged = true;
      int nextid = elements == null ? 0 : elements.size();
      for (int i = 0; i < nextid; ++i) {
        Element le = elements.get(i);
        if (!le.isChanged()) {
          allChanged = false;
          break;
        }
      }
      out.writeBoolean(allChanged);
      if (allChanged) out.writeInt(nextid);
      for (int i = 0; i < nextid; ++i) {
        Element le = elements.get(i);
        if (allChanged) {
          le.writeContents(et, out);
        } else {
          if (le.isChanged()) {
            out.writeInt(i);
            le.writeChangedContents(et, out);
          }
        }
      }
      if (!allChanged) out.writeInt(-1);
    }

    public void readChangedContents(IRType<T> et, IRInput in) throws IOException {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      int flags = in.readByte();
      if (in.debug()) {
        System.out.println("    head="
            + ((flags & 1) != 0 ? "changed" : "unchanged") + " tail="
            + ((flags & 2) != 0 ? "changed" : "unchanged") + " free="
            + ((flags & 4) != 0 ? "changed" : "unchanged"));
      }
      IRType<IRLocation> locType = getLocationType(in);
      if ((flags & 1) != 0) {
        head = ls.readSlotValue(head, locType, in);
      }
      if ((flags & 2) != 0) {
        tail = ls.readSlotValue(tail, locType, in);
      }
      if ((in.getRevision() < 5 && in.readBoolean()) || (flags & 4) != 0) {
        if (in.debug()) System.out.println("    free=changed");
        free = ls.readSlotValue(free, locType, in);
      } else {
        if (in.debug()) System.out.println("    free=unchanged");
      }
      boolean allChanged = in.readBoolean();
      if (in.debug())
          System.out.println("    (" + (allChanged ? "all" : "not all")
              + " changed)");
      if (allChanged) {
        int n = in.readInt();
        for (int i = 0; i < n; ++i) {
          if (in.debug()) System.out.println("    for loc #" + i);
          lookup(i).readContents(et, in);
        }
      } else {
        int i;
        while ((i = in.readInt()) != -1) {
          if (in.debug()) System.out.println("    for loc #" + i);
          lookup(i).readChangedContents(et, in);
        }
      }
    }
    
    public FullHeader generalize() {
      return this;
    }
    
    public void describe(PrintStream out) {
      int n = elements.size();
      out.println("IRList[" + n + "]");
      for (int i=0; i < n; ++i) {
        Element le = elements.get(i);
        out.print(" " + i + " => ");
        le.describe(out);
      }
    }
  }

  /**
   * Generic Doubly-Linked list class to be used for IRList. 
   * The next slot may be the sentinel location which means that
   * the next location is the next location ID unless we are
   * past the end of the size().  This spcial case permits us to start with
   * the much more efficient structure SimpleHeader.
   * <p>
   * <bold>This class has race
   * conditions in it concerning slots. Whenever we have <blockquote>
   * <tt>slot = slot.setValue(<i>e</i>)
   * </blockquote>, this needs to be protected somehow.
   *</bold>
   */
  class Element extends IRLocation {
    static final int ELEM_FLAG = 4;
    
    ES prev, next;
    S elem; // next may evaluate to IRLocation.sentinel


    Element(int id, S aSlotState) {
      super(id);
      prev = next = null;
      elem = aSlotState;
    }

    @RequiresLock("IRList.this:ListLock")
    Element getPrev() {
      IRLocation loc = (getElemStorage().getSlotValue(prev));
      // handle backward header extras
      if (loc == IRLocation.getSentinel()) {
        int i = getID();
        // we increment because the locations were in reverse order!
        if (++i >= size()) return null;
        else return (Element)seq.validateLocation(IRLocation.get(i));
      }
      return (Element) loc;
    }

    @RequiresLock("IRList.this:ListLock")
    Element getNext() {
      IRLocation loc = (getElemStorage().getSlotValue(next));
      // handle forward header extras
      if (loc == IRLocation.getSentinel()) {
        int i = getID();
        if (++i >= size()) return null;
        else return (Element)seq.validateLocation(IRLocation.get(i));
      }
      return (Element)loc;
    }

    IRList getHeader() {
      return IRList.this;
    }

    boolean isValid() {
      return getSlotStorage().isValid(elem);
    }

    T getElement() {
      return getSlotStorage().getSlotValue(elem);
    }

    void setElement(T value) {
      elem = getSlotStorage().setSlotValue(elem, value);
    }

    public void writeContents(IRType<T> et, IROutput out) throws IOException {
      int flags = 0;
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      SlotStorage<S,T> st = getSlotStorage();
      if (prev != noSlot && ls.isValid(prev)) flags |= 1;
      if (next != noSlot && ls.isValid(next)) flags |= 2;
      if (st.isValid(elem)) flags |= ELEM_FLAG;
      out.writeByte(flags);
      if ((flags & 1) != 0) {
        ls.writeSlotValue(prev, getLocationType(), out);
      }
      if ((flags & 2) != 0) {
        ls.writeSlotValue(next, getLocationType(), out);
      }
      if ((flags & ELEM_FLAG) != 0) {
        st.writeSlotValue(elem, et, out);
      }
    }

    public void readContents(IRType<T> et, IRInput in) throws IOException {
      int flags = in.readByte();
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      SlotStorage<S,T> st = getSlotStorage();
      if ((flags & 1) != 0) {
        if (prev == noSlot) {
          prev = ls.newSlot();
        }
        prev = ls.readSlotValue(prev, getLocationType(in), in);
      }
      if ((flags & 2) != 0) {
        if (next == noSlot) {
          next = ls.newSlot();
        }
        next = ls.readSlotValue(next, getLocationType(in), in);
      }
      if ((flags & ELEM_FLAG) != 0) {
        elem = st.readSlotValue(elem, et, in);
        if (in.debug())
            System.out.println("    read elem " + st.getSlotValue(elem));
      }
    }

    public boolean isChanged() {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      return (prev != noSlot && ls.isChanged(prev))
          || (next != noSlot && ls.isChanged(next))
          || getSlotStorage().isChanged(elem);
    }

    public void writeChangedContents(IRType<T> et, IROutput out)
        throws IOException {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      SlotStorage<S,T> st = getSlotStorage();
      int flags = 0;
      if (prev != noSlot && ls.isChanged(prev)) flags |= 1;
      if (next != noSlot && ls.isChanged(next)) flags |= 2;
      if (st.isChanged(elem)) flags |= ELEM_FLAG;
      out.writeByte(flags);
      if ((flags & 1) != 0) {
        ls.writeSlotValue(prev, getLocationType(), out);
      }
      if ((flags & 2) != 0) {
        ls.writeSlotValue(next, getLocationType(), out);
      }
      if ((flags & ELEM_FLAG) != 0) {
        st.writeSlotValue(elem, et, out);
      }
    }

    public void readChangedContents(IRType et, IRInput in) throws IOException {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      SlotStorage<S,T> st = getSlotStorage();
      int flags = in.readByte();
      if (in.debug()) {
        System.out.println("        prev="
            + ((flags & 1) != 0 ? "changed" : "unchanged") + " next="
            + ((flags & 2) != 0 ? "changed" : "unchanged") + " elem="
            + ((flags & ELEM_FLAG) != 0 ? "changed" : "unchanged"));
      }
      if ((flags & 1) != 0) {
        if (prev == noSlot) prev = ls.newSlot();
        prev = ls.readSlotValue(prev, getLocationType(in), in);
      }
      if ((flags & 2) != 0) {
        if (next == noSlot) next = ls.newSlot();
        next = ls.readSlotValue(next, getLocationType(in), in);
      }
      if ((flags & ELEM_FLAG) != 0) {
        elem = st.readSlotValue(elem, et, in);
      }
    }

    public void describe(PrintStream out) {
      SlotStorage<ES,IRLocation> ls = getElemStorage();
      SlotStorage<S,T> st = getSlotStorage();
      st.describe(elem, out);
      out.print("  prev => ");
      if (prev == noSlot) {
        out.println(" (no slot) ");
      } else {
        ls.describe(prev, out);
      }
      out.print("  next => ");
      if (next == noSlot) {
        out.println("  (no slot) ");
      } else {
        ls.describe(next, out);
      }
    }
  }

  static class Exception extends IRSequenceException {

    public Exception() {
      super();
    }

    public Exception(String s) {
      super(s);
    }
  }

  static class IRListLocationType extends IRLocationType {

    static final IRType<IRLocation> prototype = new IRListLocationType(null);

    private final IRList list;

    public IRListLocationType(IRList l) {
      super();
      list = l;
    }

    @Override
    public IRLocation readValue(IRInput in) throws IOException {
      IRLocation loc = super.readValue(in);
      if (in.debug()) {
        System.out.println("    location "
            + (loc == null ? "null" : Integer.toString(loc.getID())));
      }
      if (loc == null || loc == IRLocation.getSentinel()) return loc;
      IRList l;
      if (in.getRevision() < 5) {
        l = list;
      } else {
        l = (IRList) in.readCachedObject();
        if (l == null) {
          return loc;
        }
      }
      assert l != null;
      // System.out.println("l = " + l + ", loc = " + loc);
      return l.validateLocation(loc);
    }

    @Override
    public void writeValue(IRLocation l, IROutput out) throws IOException {
      if (out.debug()) {
        System.out
            .println("    location "
                + (l == null ? "null" : Integer.toString(l.getID())));
      }
      super.writeValue(l, out);
      if (l == null || l == IRLocation.getSentinel()) return;
      // new for persistence 1.5:
      if (l instanceof IRList.Element) {
        IRList owner = ((IRList.Element)l).getHeader();
        if (!out.writeCachedObject(owner)) { 
          throw new IOException("owning list must already exist!"); 
        }
      }
      else {
        if (true) throw new IOException("Internal error: full list has an unvalidated location");
        // workaround to not caching null
        if (!out.writeCachedObject(new Object())) {
          // do nothing (don't bother writing anything)
        }
      }
      
      
    }
  }
}

