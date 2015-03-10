/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/SlotUnknownVersionException.java,v 1.3 2003/07/02 20:19:24 thallora Exp $ */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.Slot;
import edu.cmu.cs.fluid.ir.SlotUnknownException;

/** A slot has an unknown value at a particular version. */
public class SlotUnknownVersionException extends SlotUnknownException {
    private final Version version;
    public SlotUnknownVersionException(String msg, Slot s, Version v) {
	super(msg,s);
	version = v;
    }

    public Version getVersion() { return version; }
}
