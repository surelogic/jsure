/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotInfoSlot.java,v 1.5 2007/07/05 18:15:15 aarong Exp $ */
package edu.cmu.cs.fluid.ir;

/** An adapter class that permits us to refer to a slot by attribute
 * and node.
 */
public class SlotInfoSlot<T> extends AbstractSlot<T> {
    public final SlotInfo<T> attr;
    public final IRNode node;

    public SlotInfoSlot(SlotInfo<T> si, IRNode n) {
	attr = si;
	node = n;
    }

    public SlotInfo<T> getSlotInfo() { return attr; }
    public SlotInfo<T> getAttribute() { return attr; }

    public IRNode getNode() { return node; }

    @Override
    public T getValue() throws SlotUndefinedException
    {
	return node.getSlotValue(attr);
    }

    @Override
    public Slot<T> setValue(T newValue) throws SlotImmutableException
    {
	node.setSlotValue(attr,newValue);
	return this;
    }

    @Override
    public boolean isValid()
    {
	return node.valueExists(attr);
    }

    @Override
    public boolean isChanged()
    {
	if (attr instanceof PersistentSlotInfo) {
	    return ((PersistentSlotInfo)attr).valueChanged(node);
	}
	return false;
    }
}
