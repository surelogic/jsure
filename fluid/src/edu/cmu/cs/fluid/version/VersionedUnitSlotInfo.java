/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedUnitSlotInfo.java,v
 * 1.19 2003/08/06 20:34:00 chance Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.*;
import edu.cmu.cs.fluid.ir.*;

/**
 * An attribute that contains no information, except changes. It keeps track of
 * times the attribute is given a new value, and it has a method to determine if
 * two versions differ for some node.
 * <p>
 * This class is slated for removal very soon. It has been superceded.
 * 
 * @deprecated use {@link VersionedChangedInfo} or
 *             {@link SlotFactory#newChangeRecord(String)}
 */
@Deprecated
public class VersionedUnitSlotInfo extends
		InfoStoredSlotInfo<VersionedUnitSlot, Void> implements Observer {
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger.getLogger("IR.persist");

	/**
	 * Register a new versioned unit slot descriptor.
	 * 
	 * @param name
	 *            Name under which to register this description
	 * @precondition nonNull(name);
	 */
	public VersionedUnitSlotInfo(String name)
			throws SlotAlreadyRegisteredException {
		super(name, IRUnitType.prototype, VersionedUnitSlotFactory.prototype
				.getUnitStorage(), (Void) null,
				new HashedSlots<VersionedUnitSlot, Void>());
	}

	/** Create a new anonymous versioned unit slot descriptor. */
	public VersionedUnitSlotInfo() {
		super(VersionedUnitSlotFactory.prototype.getUnitStorage(), null,
				new HashedSlots<VersionedUnitSlot, Void>()); // VersionedUnitHashEntryFactory.prototype));
	}

	public static final QuickProperties.Flag versionedUnitFlag = new QuickProperties.Flag(
			LOG, "fluid.ir.versionedUnit", "VersionedUnit", false, true);

	public static final boolean unitIsOn = unitIsOn();

	public static boolean unitIsOn() {
		if (JJNode.versioningIsOn) {
			return true;
		}
		return QuickProperties.checkFlag(versionedUnitFlag);
	}

	private Version source;

	/**
	 * Record the current state as "unchanged". After this call, all calls to
	 * {@link #isChanged(IRNode)} will return false, until explicit set as
	 * changed.
	 * <p>
	 * Warning: there is another function {@link #clearChanged()} that has a
	 * completely different function and which shouldn't be used.
	 */
	public void clearChanges() {
		source = Version.getVersion();
	}

	/**
	 * Return whether any changes have been recorded for this node.
	 * 
	 * @param node
	 * @return true if we have recorded a change for this node (since the last
	 *         clear).
	 */
	public boolean isChanged(IRNode node) {
		return changed(node, source);
	}

	/**
	 * Record that something has changed for this node.
	 */
	public boolean setChanged(IRNode node) {
		if (!unitIsOn) {
			return false;
		}
		if (VersionedSlot.getEra() != null) {
			boolean result = false;
			List<Version> eraChanges = VersionedSlot.getEraChanges();
			for (Version v : eraChanges) {
				result |= setChanged(node, v);
			}
			return result;
		} else {
			return setChanged(node, Version.getVersionLocal());
		}
	}

	private boolean setChanged(IRNode node, Version v) {
		if (v.parent() != null && changed(node, v, v.parent())) {
			return false;
		} else {
			super.setSlotValue(node, null);
			if (name() != null)
				Version.noteCurrentlyChanged(getState(node));
			return true;
		}
	}

	/**
	 * Return the versioned structure associated with this node if any is
	 * available.
	 */
	IRState getState(IRNode node) {
		IRRegion region = IRRegion.getOwnerOrNull(node);
		Bundle b = getBundle();
		if (region == null || b == null) {
			return new SlotState<Void>(this, node);
		}
		return IRChunk.get(region, b);
	}

	/**
	 * Check that the state for the slot with this node is properly loaded at
	 * this version.
	 * 
	 * @return true if it is properly loaded.
	 */
	boolean checkState(IRNode node) {
		IRRegion region = IRRegion.getOwnerOrNull(node);
		Bundle b = getBundle();
		if (region == null || b == null) {
			return true;
		}
		return Version.isCurrentlyLoaded(IRChunk.get(region, b));
	}

	// checking the VS is tricky:
	// Snapshots are useless, because they don't save change information.
	// (Perhaps this should change?)
	//
	// To be valid, we need every era along the path from
	// one version to the other, except for the lca to be
	// defined.
	// (to be continued)

	/** Ensure that the versioned structure in this era is defined. */
	void checkState(IRState st, Era e, IRNode node) {
		// System.out.println("Checking " + e);
		while (!e.isLoaded(st)) {
			new SlotUnknownEraException("intervening diffs not loaded",
					new SlotInfoSlot<Void>(this, node), e).handle();
		}
	}

	// (continued from above)
	// Starting with one of the two versions, we work backwards to
	// the lca. This is complicated by the fact that we do a
	// whole era at a time.
	//
	// We use the following facts:
	// versions except alpha with eras always have parent versions with eras.
	// If the limit is not assigned to an era, everything is local (OK).
	// Versions without eras can be ignored (local).

	/**
	 * Ensure that the versioned structure in eras for all versions between v
	 * and limit (inclusive v, exclusive of limit).
	 */
	void checkState(IRState st, Version v, Version limit, IRNode node) {
		Era elimit = limit.getEra();
		if (elimit == null)
			return;
		Era e = null;
		while ((e = v.getEra()) == null && v != limit)
			v = v.parent();
		while (e != elimit) {
			checkState(st, e, node);
			v = e.getRoot();
			e = v.getEra();
		}
		// if not back to limit, we need to check VS at this era too.
		if (v != limit)
			checkState(st, e, node);
	}

	/**
	 * Return true if a change has been noted for the node between the current
	 * version and the given version.
	 */
	public boolean changed(IRNode node, Version other) {
		return changed(node, other, Version.getVersionLocal());
	}

	/**
	 * Return true if a change has been noted for the node between two versions.
	 */
	public boolean changed(IRNode node, Version v1, Version v2) {
		if (v1.equals(v2))
			return false;
		if (getBundle() != null && IRRegion.getOwnerOrNull(node) != null) {
			IRState st = getState(node);
			if (st != null) {
				Version v0 = Version.latestCommonAncestor(v1, v2);
				checkState(st, v1, v0, node);
				checkState(st, v2, v0, node);
			}
		}
		return super.getSlot(node).changed(v1, v2);
	}

	/**
	 * Inform a change at a particular IRNode.
	 * 
	 * @param node
	 *            a tree node
	 */
	@Override
  public void update(Observable obs, Object node) {
		setChanged((IRNode) node);
	}

	public Iterable<Version> changes(IRNode node) {
		return super.getSlot(node).changes();
	}

	public static void printVUSlotCounts() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("OneVersion = " + OneVersion.count);
			LOG.fine("Versions = " + Versions.count);
		}
	}
}

class VersionedUnitSlotFactory extends VersionedSlotFactory {
	// NB: This class ONLY works with unit slots!
	private VersionedUnitSlotFactory() {
		super();
	}

	public static VersionedUnitSlotFactory prototype = new VersionedUnitSlotFactory();
	static {
		IRPersistent.registerSlotFactory(prototype, 'U');
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Slot<T> undefinedSlot() {
		return (Slot<T>) NoVersions.prototype;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Slot<T> predefinedSlot(T value) {
		return (Slot<T>) NoVersions.prototype;
	}

	public SlotStorage<VersionedUnitSlot, Void> getUnitStorage() {
		return VersionedUnitStorage.prototype;
	}

	@Override
	public void noteChange(IRState state) {
		// Do nothing (derived information)
	}
}

class VersionedUnitStorage implements SlotStorage<VersionedUnitSlot, Void> {
	public static final VersionedUnitStorage prototype = new VersionedUnitStorage();

	@Override
  public boolean isThreadSafe() {
		return false;
	}
	
	@Override
  public VersionedUnitSlot newSlot() {
		return NoVersions.prototype;
	}

	@Override
  public VersionedUnitSlot newSlot(Void initialValue) {
		return NoVersions.prototype;
	}

	@Override
  public Void getSlotValue(VersionedUnitSlot slotState) {
		return null;
	}

	@Override
  public VersionedUnitSlot setSlotValue(VersionedUnitSlot slotState,
			Void newValue) {
		return slotState.setValue();
	}

	@Override
  public boolean isValid(VersionedUnitSlot slotState) {
		return true;
	}

	@Override
  public boolean isChanged(VersionedUnitSlot slotState) {
		return slotState.isChanged();
	}

	@Override
  public void writeSlotValue(VersionedUnitSlot slotState, IRType<Void> ty,
			IROutput out) throws IOException {
		slotState.writeValue(ty, out);
	}

	@Override
  public VersionedUnitSlot readSlotValue(VersionedUnitSlot slotState,
			IRType<Void> ty, IRInput in) throws IOException {
		return slotState.readValue(ty, in);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.fluid.ir.SlotStorage#describe(S, java.io.PrintStream)
	 */
	@Override
  public void describe(VersionedUnitSlot slotState, PrintStream out) {
		slotState.describe(out);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.cmu.cs.fluid.ir.SlotStorage#getSlotFactory()
	 */
	@Override
  public SlotFactory getSlotFactory() {
		return VersionedUnitSlotFactory.prototype;
	}

}

abstract class VersionedUnitSlot extends AbstractSlot<Void> {
	/**
	 * Does this slot contain changed information. By default, assume not.
	 */
	@Override
	public boolean isChanged() {
		return false;
	}

	@Override
  public Void getValue() {
		return null;
	}

	@Override
  public boolean isValid() {
		return true;
	}

	@Override
  public Slot<Void> setValue(Void newValue) {
		return setValue();
	}

	public abstract VersionedUnitSlot setValue();

	@Override
	public abstract VersionedUnitSlot readValue(IRType<Void> ty, IRInput in)
			throws IOException;

	public abstract boolean changed(Version v1, Version v2);

	public abstract Iterable<Version> changes();
}

class NoVersions extends VersionedUnitSlot {
	public static NoVersions prototype = new NoVersions();
	private static Iterable<Version> noVersions = new EmptyIterator<Version>();

	@Override
	public VersionedUnitSlot setValue() {
		return OneVersion.create(Version.getVersionLocal());
	}

	@Override
	public boolean changed(Version v1, Version v2) {
		return false;
	}

	@Override
	public VersionedUnitSlot readValue(IRType<Void> ty, IRInput in)
			throws IOException {
		Era era = VersionedSlot.getEra();
		if (era != null) {
			int count = in.readUnsignedShort();
			if (count == 1) {
				Version v = Version.read(in, era);
				if (in.debug())
					System.out.println("  " + v);
				return OneVersion.create(v);
			} else if (count > 0) {
				Vector<Version> vlog = new Vector<Version>();
				for (int i = 0; i < count; ++i) {
					Version v = Version.read(in, era);
					if (in.debug())
						System.out.println("  " + v);
					vlog.addElement(v);
				}
				return new Versions(vlog);
			} else {
				System.out
						.println("!! Warning: read empty versioned unit record!");
			}
		}
		return this;
	}

	@Override
	public Iterable<Version> changes() {
		return noVersions;
	}
}

class OneVersion extends VersionedUnitSlot {
	static int count;
	private static Version lastVersion;
	private static OneVersion lastSlot;

	private Version version;

	public static OneVersion create(final Version v) {
		count++;
		if (lastVersion == v) {
			return lastSlot;
		} else {
			lastVersion = v;
			lastSlot = new OneVersion(v);
			return lastSlot;
		}
	}

	private OneVersion(Version v) {
		version = v;
	}

	@Override
	public VersionedUnitSlot setValue() {
		Version current = Version.getVersionLocal();
		if (version == current)
			return this;

		count--;
		return new Versions(version).setValue();
	}

	@Override
	public boolean changed(Version v1, Version v2) {
		return version.isBetween(v1, v2);
	}

	@Override
	public boolean isChanged() {
		Era era = VersionedSlot.getEra();
		if (era != null)
			return era.contains(version);
		return false;
	}

	@Override
	public void writeValue(IRType<Void> ty, IROutput out) throws IOException {
		Era era = VersionedSlot.getEra();
		if (era != null) {
			if (out.debug()) {
				System.out.println("changed: " + version);
			}
			out.writeShort(1);
			version.write(out);
		}
	}

	@Override
	public VersionedUnitSlot readValue(IRType<Void> ty, IRInput in)
			throws IOException {
		Era era = VersionedSlot.getEra();
		if (era != null) {
			Versions vs = new Versions(version);
			return vs.readValue(ty, in);
		}
		return this;
	}

	@Override
	public Iterable<Version> changes() {
		return new SingletonIterator<Version>(version);
	}
}

// XXX: This class looks thread-unsafe to me (JTB 2006/6/6)
class Versions extends VersionedUnitSlot {
	static int count;
	private final Vector<Version> versionLog;

	public Versions(Version v) {
		versionLog = new Vector<Version>();
		versionLog.addElement(v);
		count++;
	}

	/**
	 * Create a set of versions all from the same era, already in sorted order.
	 */
	Versions(Vector<Version> vlog) {
		versionLog = vlog;
	}

	protected final int findVersion(Version version) {
		int min = 0;
		int max = versionLog.size();
		int index;

		// look for version in log:
		while (min < max) {
			index = (min + max) / 2;
			Version v = versionLog.elementAt(index);
			if (version.equals(v))
				return index;
			if (version.precedes(v)) {
				max = index;
			} else {
				min = index + 1;
			}
		}
		return min;
	}

	@Override
	public VersionedUnitSlot setValue() {
		Version.getVersionLocal();
		addVersion(Version.getVersionLocal());
		return this;
	}

	void addVersion(Version current) {
		// TODO: See below: perhaps we should just use a HashSet
		int index = findVersion(current);

		if (index < versionLog.size() && // I wish I didn't have to test
											// again...
				versionLog.elementAt(index).equals(current))
			return;

		versionLog.insertElementAt(current, index);
		/*
		 * System.out.print("Log: "); for (int i=0; i < versionLog.size(); ++i)
		 * System.out.print(versionLog.elementAt(i)+ " "); System.out.println();
		 */
	}

	@Override
	public boolean changed(Version v1, Version v2) {
		// TODO: If this code is slow, we can try substituting with a HashSet.
		Version lca = Version.latestCommonAncestor(v1, v2);
		Version end = Version.precedes(v1, v2) ? v2 : v1;

		int lcaindex = findVersion(lca);
		int endindex = findVersion(end);

		/*
		 * we have to special case the final case or else turn the following for
		 * loop to test i <= endindex and then first ensure endindex in range.
		 */
		if (endindex >= lcaindex && endindex < versionLog.size()
				&& !v1.equals(v2) && versionLog.elementAt(endindex).equals(end))
			return true;

		for (int i = lcaindex; i < endindex; ++i) {
			Version v = versionLog.elementAt(i);
			if (v.isBetween(v1, v2))
				return true;
		}

		return false;
	}

	@Override
	public boolean isChanged() {
		Era era = VersionedSlot.getEra();
		if (era != null) {
			// !! slow for now
			for (int i = 0; i < versionLog.size(); ++i) {
				Version v = versionLog.elementAt(i);
				if (era.contains(v))
					return true;
			}
		}
		return false;
	}

	@Override
	public void writeValue(IRType<Void> ty, IROutput out) throws IOException {
		Era era = VersionedSlot.getEra();
		if (era != null) {
			Vector<Version> inEra = new Vector<Version>();
			// !! slow for now
			for (int i = 0; i < versionLog.size(); ++i) {
				Version v = versionLog.elementAt(i);
				if (era.contains(v))
					inEra.addElement(v);
			}
			out.writeShort(inEra.size());
			for (int i = 0; i < inEra.size(); ++i) {
				Version v = inEra.elementAt(i);
				v.write(out);
			}
		}
	}

	@Override
	public VersionedUnitSlot readValue(IRType<Void> ty, IRInput in)
			throws IOException {
		Era era = VersionedSlot.getEra();
		if (era != null) {
			int count = in.readUnsignedShort();
			for (int i = 0; i < count; ++i) {
				Version v = Version.read(in, era);
				if (in.debug())
					System.out.println("  " + v);
				addVersion(v);
			}
		}
		return this;
	}

	@Override
	public Iterable<Version> changes() {
		return versionLog;
	}
}