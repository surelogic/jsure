/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedSlotFactory.java,v
 * 1.14 2003/07/02 20:19:22 thallora Exp $
 */
package edu.cmu.cs.fluid.version;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.ListIteratable;

/** The family of versioned (partially mutable) slots. */
public class VersionedSlotFactory extends AbstractExplicitSlotFactory {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("IR.version");

  protected VersionedSlotFactory() {
    // Nothing to do
  }
  
  public static final VersionedSlotFactory prototype =
    new VersionedSlotFactory();
  public static final VersionedSlotFactory dependent = new DependentVersionedSlotFactory();

  static {
    IRPersistent.registerSlotFactory(prototype, 'V');
    IRPersistent.registerSlotFactory(dependent, 'v');
  }


  /**
   * A bi-directional versioned slot factory is used to create versioned slots 
   * that contain derived versioned information.  The term ``bidirectional'  means
   * that they are assigned in both directions starting from the initial root
   * version.  Unlike normal versioned slots for which an assignment of a value
   * to a version is valid for subsequent versions (unless later assigned),
   * for a bidirectional versioned slot, an assignment for a version <em>above</em>
   * the root version causes the value to be assumed for all <em>previous</em>
   * versions too (and their descendants).  For simplicity, persistence is not
   * defined for such slots.
   * @param root root version around which assignments are made.
   */
  public static ExplicitSlotFactory bidirectional(Version root) {
    return new BidirectionalVersionedSlotFactory(root); 
  }

  /**
	 * Create a versioned slot with no default that cannot be persisted.
	 * @see VersionedStructureFactory#getVS
	 */
  @Override
  public <T> Slot<T> undefinedSlot() {
    return this.<T>undefinedSlot(null);
  }

  /**
	 * Create a versioned slot with given default that cannot be persisted.
	 */
  @Override
  public <T> Slot<T> predefinedSlot(T value) {
    return predefinedSlot(value, null);
  }

  /**
	 * Create a versioned slot with no default that resides in the given
	 * versioned structure.
	 */
  public <T> Slot<T> undefinedSlot(IRState st) {
    return new IndependentVersionedSlot<T>(st);
  }

  /**
	 * Create a versioned slot with given default value that resides in the given
	 * versioned structure.
	 */
  public <T> Slot<T> predefinedSlot(T def, IRState st) {
    return new IndependentVersionedSlot<T>(def, st);
  }

  @Override
  public <T> IRSequence<T> newSequence(int size) {
    // Problem: we need to create dependent slots for the common case
    // graphs and tree, but it isn't the correct thing sometimes.
    if (this != dependent) return dependent.newSequence(size);
    if (size == 1) return new Versioned1Array<T>();
    if (size == 2) return new Versioned2Array<T>();
    return super.newSequence(size);
  }
  
  @Override
  public <T> Iteratable<T> newIterator(Iteratable<T> e) {
    if (e instanceof VersionedIterator)
      return e;
    return new VersionedIterator<T>(e);
  }

  @Override
  public <T> ListIteratable<T> newListIterator(ListIteratable<T> e) {
    if (e instanceof VersionedListIterator)
      return e;
    return new VersionedListIterator<T>(e);
  }
  
  @Override
  public <T> SlotInfo<T> newAttribute() {
    return VersionedSlotFactory.<T>makeSlotInfo();
  }
  @Override
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type)
    throws SlotAlreadyRegisteredException {
    return VersionedSlotFactory.<T>makeSlotInfo(name, type);
  }
  @Override
  public <T> SlotInfo<T> newAttribute(T defaultValue) {
    return VersionedSlotFactory.<T>makeSlotInfo(defaultValue);
  }
  @Override
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type, T defaultValue)
    throws SlotAlreadyRegisteredException {
    return VersionedSlotFactory.<T>makeSlotInfo(name, type, defaultValue);
  }

  public static <T> SlotInfo<T> makeSlotInfo() {
    return new VersionedSlotInfo<T>();
  }
  public static <T> SlotInfo<T> makeSlotInfo(String name, IRType<T> type)
    throws SlotAlreadyRegisteredException {
    return new VersionedSlotInfo<T>(name, type);
  }
  public static <T> SlotInfo<T> makeSlotInfo(T defaultValue) {
    return new VersionedSlotInfo<T>(defaultValue);
  }
  public static <T> SlotInfo<T> makeSlotInfo(
    String name,
    IRType<T> type,
    T defaultValue)
    throws SlotAlreadyRegisteredException {
    return new VersionedSlotInfo<T>(name, type, defaultValue);
  }
  
  
  @Override
  public AbstractChangeRecord newChangeRecord(String name) throws SlotAlreadyRegisteredException {
    return new VersionedChangeRecord(name);
  }

  @Override
  public void noteChange(IRState state) {
    Version.getVersionLocal().noteChanged(state);
  }
}

/**
 * A slotfactory for structures that are already protected by versioned
 * structures.
 */
class DependentVersionedSlotFactory extends VersionedSlotFactory {

  @Override
  public <T> Slot<T> undefinedSlot() {
    return UnassignedVersionedSlot.<T>create();
  }
  @Override
  public <T> Slot<T> predefinedSlot(T value) {
    return UnassignedVersionedSlot.<T>create(value);
  }

}

/**
 * Slot factory for ``bidirectional'' derived versioned slots.
 * @see VersionedSlotFactory#bidirectional
 * @author boyland
 */
class BidirectionalVersionedSlotFactory extends AbstractExplicitSlotFactory {
  final Version rootVersion;
  
  BidirectionalVersionedSlotFactory(Version v) {
    rootVersion = v;
  }
  
  @Override
  public <T> Iteratable<T> newIterator(Iteratable<T> e) {
    return VersionedSlotFactory.prototype.newIterator(e);
  }
  @Override
  public <T> Slot<T> predefinedSlot(T value) {
    return new UnassignedBiVersionedSlot<T>(rootVersion,value);
  }
  @Override
  public <T> Slot<T> undefinedSlot() {
    return new UnassignedBiVersionedSlot<T>(rootVersion);
  }
  
  @Override
  public void noteChange(IRState state) {
    // nothing: information is derived.
  }
}

class Versioned1Array<T> extends IR1Array<Slot<T>,T> {

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRAbstractArray#getSlotStorage()
   */
  @Override
  public SlotStorage<Slot<T>, T> getSlotStorage() {
    return VersionedSlotFactory.dependent.<T>getStorage();
  }
}
class Versioned2Array<T> extends IR2Array<Slot<T>,T> {

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRAbstractArray#getSlotStorage()
   */
  @Override
  public SlotStorage<Slot<T>, T> getSlotStorage() {
    return VersionedSlotFactory.dependent.<T>getStorage();
  }
  
}
