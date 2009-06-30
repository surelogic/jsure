/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/SlotUnknownEraException.java,v 1.3 2003/07/02 20:19:24 thallora Exp $ */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.Slot;
import edu.cmu.cs.fluid.ir.SlotUnknownException;

/** A slot has an unknown value at a particular era. */
public class SlotUnknownEraException extends SlotUnknownException {
    private final Era era;
    public SlotUnknownEraException(String msg, Slot s, Era e) {
	super(msg,s);
	era = e;
    }

    public Era getEra() { return era; }
}
