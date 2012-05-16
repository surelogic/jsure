package edu.cmu.cs.fluid.ir;

import java.util.concurrent.atomic.AtomicLong;

import edu.cmu.cs.fluid.FluidError;

/**
 * A default implementation of the intermediate representation node interface.
 * Not persistable as is.
 * 
 * @author Edwin
 */
public abstract class AbstractIRNode implements IRNode {
	private static final AtomicLong destroyedNodes = new AtomicLong();
	private static final AtomicLong nodesCreated = new AtomicLong();

	public static long getTotalNodesCreated() {
		return nodesCreated.get();
	}

	public static boolean checkIfNumDestroyed(long num) {
		long current = destroyedNodes.get();
		if (current > num) {
			return destroyedNodes.compareAndSet(current, 0);
		}
		return false;
	}

	public AbstractIRNode() {
		nodesCreated.intValue();
	}

	public Object identity() {
		if (destroyed())
			return destroyedNode;
		return this;
	}

	private volatile int hash = super.hashCode();

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (destroyed()) {
			return other == destroyedNode
					|| (other != null && other.equals(destroyedNode));
		}
		// not destroyed
		if (other instanceof IRNode) {
			return this == ((IRNode) other).identity();
		} else {
			return false;
		}
	}

	/**
	 * If the node is not in a region, mark it as destroyed. Otherwise, the
	 * whole region needs to be destroyed.
	 */
	public void destroy() {
		synchronized (this) {
			hash = DESTROYED_HASH;
			destroyedNodes.incrementAndGet();
		}
	}

	/** True for a node that has been destroyed. */
	final boolean destroyed() {
		return hash == DESTROYED_HASH;
	}

	/**
	 * True if the node has been destroyed, works for any {@link IRNode}.
	 * 
	 * @param n
	 *            node to test
	 * @return true if this node has been destoryed.
	 */
	public static boolean isDestroyed(IRNode n) {
		return n.identity() == IRNode.destroyedNode;
	}

	/** If in a region, then self-identify */
	@Override
	public String toString() {
		if (destroyed()) {
			return "Destroyed" + super.toString();
		} else {
			return toString_internal();
		}
	}

	protected String toString_internal() {
		return super.toString();
	}

	/**
	 * Get the slot's value for a particular node.
	 * 
	 * @typeparam Value
	 * @param si
	 *            Description of slot to be accessed.
	 *            <dl purpose=fluid>
	 *            <dt>type
	 *            <dd>SlotInfo[Value]
	 *            </dl>
	 * @precondition nonNull(si)
	 * @return the value of the slot associated with this node.
	 *         <dl purpose=fluid>
	 *         <dt>type
	 *         <dd>Value
	 *         </dl>
	 * @exception SlotUndefinedException
	 *                If the slot is not initialized with a value.
	 */
	public <T> T getSlotValue(SlotInfo<T> si) throws SlotUndefinedException {
		try {
			return si.getSlotValue(this);
		} catch (SlotUndefinedException e) {
			if (this.identity() == destroyedNode) {
				throw new FluidError("Trying to access a destroyed node");
			}
			throw e;
		}
	}

	/**
	 * Change the value stored in the slot.
	 * 
	 * @typeparam Value
	 * @param si
	 *            Description of slot to be accessed.
	 *            <dl purpose=fluid>
	 *            <dt>type
	 *            <dd>SlotInfo[Value]
	 *            </dl>
	 * @param newValue
	 *            value to store in the slot.
	 *            <dl purpose=fluid>
	 *            <dt>type
	 *            <dd>Value
	 *            <dt>capabilities
	 *            <dd>store
	 *            </dl>
	 */
	public <T> void setSlotValue(SlotInfo<T> si, T newValue)
			throws SlotImmutableException {
		if (si == null) {
			throw new NullPointerException();
		}
		si.setSlotValue(this, newValue);
	}

	// for convenience:
	public int getIntSlotValue(SlotInfo<Integer> si) {
		return (this.<Integer> getSlotValue(si)).intValue();
	}

	// for convenience
	public void setSlotValue(SlotInfo<Integer> si, int newValue) {
		setSlotValue(si, (Integer) (newValue));
	}

	/**
	 * Check if a value is defined for a particular node.
	 * 
	 * @typeparam Value
	 * @param si
	 *            Description of slot to be accessed.
	 *            <dl purpose=fluid>
	 *            <dt>type
	 *            <dd>SlotInfo[Value]
	 *            </dl>
	 * @precondition nonNull(si)
	 * @return true if getSlotValue would return a value, false otherwise
	 */
	public <T> boolean valueExists(SlotInfo<T> si) {
		return si.valueExists(this);
	}
}
