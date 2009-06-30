/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedDerivedSlot.java,v 1.5 2006/05/24 02:47:20 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.Slot;


/**
 * A versioned slot that represents derived information.
 * It relies on its client to feed it the correct information.
 * A client may use something like {@link VersionedDerivedInformation}
 * to provide a view to its clients.  The slots produced by
 * the {@link VersionedSlotFactory.bidirectional} factory obey this
 * interface (which also serves as a marker interface.)
 * @author boyland
 */
public interface VersionedDerivedSlot<T> extends Slot<T> {
  public VersionedDerivedSlot<T> setValue(T newValue, Version v);
  public Version getRootVersion();
}
