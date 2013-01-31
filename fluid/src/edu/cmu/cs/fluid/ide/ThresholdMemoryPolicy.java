/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ide/ThresholdMemoryPolicy.java,v 1.10 2009/02/26 21:43:09 chance Exp $*/
package edu.cmu.cs.fluid.ide;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;

import com.surelogic.common.logging.SLLogger;

/**
 * A policy that checks the memory usage against a threshold
 * 
 * @author Edwin.Chan
 * @lock L is this protects Instance
 */
public final class ThresholdMemoryPolicy extends AbstractMemoryPolicy {
	private static final Logger LOG = SLLogger.getLogger("memorypolicy");
	public static final IMemoryPolicy prototype = new ThresholdMemoryPolicy(
			0.05);
	final MyListener listener = new MyListener();
	final double margin;
	final double threshold;
	boolean registered = false;
	final AtomicBoolean lowOnMemory = new AtomicBoolean(false);

	ThresholdMemoryPolicy(double margin) {
		this.margin = margin;
		this.threshold = 1.0 - margin;
	}

	class MyListener implements javax.management.NotificationListener {
		@Override
    public void handleNotification(Notification notification,
				Object handback) {
			String notifType = notification.getType();
			if (notifType
					.equals(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)
					|| notifType
							.equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
				/*
				 * Potential low memory, so notify another thread to
				 * redistribute outstanding tasks to other VMs and stop
				 * receiving new tasks.
				 */
				if (LOG.isLoggable(Level.FINE))
					LOG.fine("Got a low memory event in "
							+ Thread.currentThread());
				lowOnMemory.set(true);				
			}
		}
	}

	@Override
	public synchronized boolean addLowMemoryHandler(ILowMemoryHandler h) {
		boolean added = super.addLowMemoryHandler(h);

		if (added && !registered) {
			MemoryPoolMXBean biggest = findBiggestPool();
			if (biggest != null) {
				MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
				NotificationEmitter emitter = (NotificationEmitter) mbean;
				emitter.addNotificationListener(listener, null, biggest);
				registered = true;

				/*
				final Runtime rt = Runtime.getRuntime();
				final long size = rt.maxMemory();
				*/
				final long size = biggest.getUsage().getMax();
				if (biggest.isUsageThresholdSupported() && biggest.isCollectionUsageThresholdSupported()) {
					long limit = (long) (threshold * size);
					//System.out.println("Set limit to "+limit);
					biggest.setUsageThreshold(limit);
					biggest.setCollectionUsageThreshold(limit);
				} else {
					LOG.warning("Couldn't set (collection) usage thresholds");
				}
			}
		}
		return added;
	}

	private MemoryPoolMXBean findBiggestPool() {
		MemoryPoolMXBean biggest = null;
		for (MemoryPoolMXBean b : ManagementFactory.getMemoryPoolMXBeans()) {
			if (b.getUsage() == null
					|| !b.isCollectionUsageThresholdSupported()) {
				continue;
			}
			if (biggest == null) {
				biggest = b;
				if (LOG.isLoggable(Level.FINE)) {
					MemoryUsage use = b.getUsage();
					LOG.fine(b.getName() + " commit = "
							+ use.getCommitted());
					LOG.fine(b.getName() + " used = " + use.getUsed());
				}
			} else if (biggest.getUsage().getCommitted() < b.getUsage()
					.getCommitted()) {
				biggest = b;
			}
		}
		return biggest;
	}

	@Override
  public void checkIfLowOnMemory() {
		final boolean lowMem = lowOnMemory.getAndSet(false);
		if (lowMem /* || memoryUsed() > threshold * memoryLimit() */) {
			handleLowMemory();
		}
	}

	@Override
  public double percentToUnload() {
		double overLimit = (memoryUsed() / (double) memoryLimit()) - threshold;
		// Get back as much below the limit as we currently are over
		return (overLimit < 0.0) ? 0.0 : overLimit * 2;
	}
	
	@Override
	public synchronized void shutdown() {
		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		NotificationEmitter emitter = (NotificationEmitter) mbean;
		try {
			emitter.removeNotificationListener(listener);
		} catch (ListenerNotFoundException e) {
			// Ignored
		}
		registered = false;
	}
}
