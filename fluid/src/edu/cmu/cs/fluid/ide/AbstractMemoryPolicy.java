/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ide/AbstractMemoryPolicy.java,v 1.4 2008/06/26 19:48:42 chance Exp $*/
package edu.cmu.cs.fluid.ide;

import java.lang.management.*;
import java.util.*;

public abstract class AbstractMemoryPolicy implements IMemoryPolicy {
	private final List<ILowMemoryHandler> loMemHandlers = new ArrayList<ILowMemoryHandler>();

	public synchronized boolean addLowMemoryHandler(ILowMemoryHandler h) {
		if (loMemHandlers.contains(h)) {
			return false;
		}
		loMemHandlers.add(h);
		return true;
	}

	protected final void handleLowMemory() {
		List<ILowMemoryHandler> handlers;
		synchronized (this) {
			handlers = new ArrayList<ILowMemoryHandler>(loMemHandlers);
		}
		for (ILowMemoryHandler h : handlers) {
			h.handleLowMemory(this);
		}
	}

	public long memoryUsed() {
		MemoryMXBean b = ManagementFactory.getMemoryMXBean();
		return b.getHeapMemoryUsage().getUsed();
	}

	public long memoryLimit() {
		MemoryMXBean b = ManagementFactory.getMemoryMXBean();
		return b.getHeapMemoryUsage().getCommitted();
	}
	
	public void shutdown() {
		// Nothing to do
	}
}
