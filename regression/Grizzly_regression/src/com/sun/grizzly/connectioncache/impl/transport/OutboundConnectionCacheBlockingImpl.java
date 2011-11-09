/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.grizzly.connectioncache.impl.transport;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.sun.grizzly.connectioncache.spi.concurrent.ConcurrentQueue;
import com.sun.grizzly.connectioncache.spi.transport.ConnectionFinder;
import com.sun.grizzly.connectioncache.spi.transport.ContactInfo;
import com.sun.grizzly.connectioncache.spi.transport.OutboundConnectionCache;
import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.RequiresLock;

//@Region("protected TotalRegion")
//@RegionLock("Lock is this protects TotalRegion"/*is INCONSISTENT*/)
//@Promise("@InRegion(TotalRegion) for int total*")
public final class OutboundConnectionCacheBlockingImpl<C extends Closeable>
		extends ConnectionCacheBlockingBase<C> implements
		OutboundConnectionCache<C> {

	// Configuration data
	// XXX we may want this data to be dynamically re-configurable
	private final int maxParallelConnections; // Maximum number of
	// connections we will open
	// to the same endpoint

	@InRegion("TotalRegion")
//	@Unique
//	@Aggregate("Instance into TotalRegion"/*is INCONSISTENT*/)
	private Map<ContactInfo<C>, CacheEntry<C>> entryMap;
	@InRegion("TotalRegion")
//	@Unique
//	@Aggregate("Instance into TotalRegion"/*is INCONSISTENT*/)
	private Map<C, ConnectionState<C>> connectionMap;

	public int maxParallelConnections() {
		return maxParallelConnections;
	}

  @Borrowed("this") protected String thisClassName() {
		return "OutboundConnectionCacheBlockingImpl";
	}

	// NEW: connection was just created; currently not queued
	// BUSY: connection queued on busyConnections queue
	// IDLE: connection queued on idleConnections queue
	private enum ConnectionStateValue {
		NEW, BUSY, IDLE
	}

	private static final class ConnectionState<C extends Closeable> {
		ConnectionStateValue csv; // Indicates state of
		// connection
		final ContactInfo<C> cinfo; // ContactInfo used to
		// create this
		// Connection
		final C connection; // Connection of the
		// ConnectionState
		final CacheEntry<C> entry; // This Connection's
		// CacheEntry

		int busyCount; // Number of calls to
		// get without release
		int expectedResponseCount; // Number of expected
		// responses not yet
		// received

		// At all times, a connection is either on the busy or idle queue in
		// its ConnectionEntry. If the connection is on the idle queue,
		// reclaimableHandle may also be non-null if the Connection is also on
		// the reclaimableConnections queue.
		ConcurrentQueue.Handle<C> reclaimableHandle; // non-null iff

		// connection is not
		// in use and has no
		// outstanding requests

		ConnectionState(final ContactInfo<C> cinfo, final CacheEntry<C> entry,
				final C conn) {

			this.csv = ConnectionStateValue.NEW;
			this.cinfo = cinfo;
			this.connection = conn;
			this.entry = entry;

			busyCount = 0;
			expectedResponseCount = 0;
			reclaimableHandle = null;
		}

		public String toString() {
			return "ConnectionState[" + "cinfo=" + cinfo + " connection="
					+ connection + " busyCount=" + busyCount
					+ " expectedResponseCount=" + expectedResponseCount + "]";
		}
	}

	// Represents an entry in the outbound connection cache.
	// This version handles normal shareable ContactInfo
	// (we also need to handle no share).
	private static final class CacheEntry<C extends Closeable> {
		final Queue<C> idleConnections = new LinkedBlockingQueue<C>();
		final Collection<C> idleConnectionsView = Collections
				.unmodifiableCollection(idleConnections);

		final Queue<C> busyConnections = new LinkedBlockingQueue<C>();
		final Collection<C> busyConnectionsView = Collections
				.unmodifiableCollection(busyConnections);

		public int totalConnections() {
			return idleConnections.size() + busyConnections.size();
		}
	}

	@Borrowed("this")
	public OutboundConnectionCacheBlockingImpl(final String cacheType,
			final int highWaterMark, final int numberToReclaim,
			final int maxParallelConnections, Logger logger) {

		super(cacheType, highWaterMark, numberToReclaim, logger);

		if (maxParallelConnections < 1)
			throw new IllegalArgumentException(
					"maxParallelConnections must be > 0");

		this.maxParallelConnections = maxParallelConnections;

		this.entryMap = new HashMap<ContactInfo<C>, CacheEntry<C>>();
		this.connectionMap = new HashMap<C, ConnectionState<C>>();

		if (debug()) {
			dprint(".constructor completed: " + cacheType);
		}
	}

	// XXX Added by Ethan to improve the model
	public synchronized boolean canCreateNewConnection(ContactInfo<C> cinfo) {
		CacheEntry<C> entry = entryMap.get(cinfo);
		if (entry == null)
			return true;

		return internalCanCreateNewConnection(entry);
	}

	private boolean internalCanCreateNewConnection(final CacheEntry<C> entry) {
		final int totalConnectionsInEntry = entry.totalConnections();

		final boolean createNewConnection = (totalConnectionsInEntry == 0)
				|| ((numberOfConnections() < highWaterMark()) && (totalConnectionsInEntry < maxParallelConnections));

		return createNewConnection;
	}

	private CacheEntry<C> getEntry(final ContactInfo<C> cinfo)
			throws IOException {

		if (debug()) {
			dprint("->getEntry: " + cinfo);
		}

		try {
			// This should be the only place a CacheEntry is constructed.
			// XXX added by Ethan to improve the model
			synchronized (this) {
				CacheEntry<C> result = entryMap.get(cinfo);
				if (result == null) {
					if (debug()) {
						dprint(".getEntry: " + cinfo
								+ " creating new CacheEntry");
					}

					result = new CacheEntry<C>();
					entryMap.put(cinfo, result);
				} else {
					if (debug()) {
						dprint(".getEntry: " + cinfo
								+ " re-using existing CacheEntry");
					}
				}
				return result;
			}

		} finally {
			if (debug()) {
				dprint("<-getEntry: " + cinfo);
			}
		}
	}

	// Note that tryNewConnection will ALWAYS create a new connection if
	// no connection currently exists.
	private C tryNewConnection(final CacheEntry<C> entry,
			final ContactInfo<C> cinfo) throws IOException {

		if (debug())
			dprint("->tryNewConnection: " + cinfo);

		try {
			C conn = null;

			if (internalCanCreateNewConnection(entry)) {
				// If this throws an exception just let it
				// propagate: let a higher layer handle a
				// connection creation failure.
				conn = cinfo.createConnection();

				if (debug()) {
					dprint(".tryNewConnection: " + cinfo
							+ " created connection " + conn);
				}
			}

			return conn;
		} finally {
			if (debug())
				dprint("<-tryNewConnection: " + cinfo);
		}
	}

	@RequiresLock("L")
	private void decrementTotalIdle() {
		if (debug())
			dprint("->decrementTotalIdle: totalIdle = " + totalIdle);

		try {
			if (totalIdle > 0) {
				totalIdle--;
			} else {
				if (debug()) {
					dprint(".decrementTotalIdle: "
							+ "incorrect idle count: was already 0");
				}
			}
		} finally {
			if (debug()) {
				dprint("<-decrementTotalIdle: totalIdle = " + totalIdle);
			}
		}
	}

	@RequiresLock("L")
	private void decrementTotalBusy() {
		if (debug())
			dprint("->decrementTotalBusy: totalBusy = " + totalBusy);

		try {
			if (totalBusy > 0) {
				totalBusy--;
			} else {
				if (debug()) {
					dprint(".decrementTotalBusy: "
							+ "incorrect idle count: was already 0");
				}
			}
		} finally {
			if (debug()) {
				dprint("<-decrementTotalBusy: totalBusy = " + totalBusy);
			}
		}
	}

	// Update queues and counts to make the result busy.
	@RequiresLock("L")
	private void makeResultBusy(C result, ConnectionState<C> cs,
			CacheEntry<C> entry) {

		if (debug())
			dprint("->makeResultBusy: " + result + " was previously " + cs.csv);

		try {
			switch (cs.csv) {
			case NEW:
				totalBusy++;
				break;

			case IDLE:
				totalBusy++;
				decrementTotalIdle();

				final ConcurrentQueue.Handle<C> handle = cs.reclaimableHandle;

				if (handle != null) {
					if (!handle.remove()) {
						if (debug()) {
							dprint(".makeResultBusy: " + cs.cinfo
									+ " result was not on reclaimable Q");
						}
					}
					cs.reclaimableHandle = null;
				}
				break;

			case BUSY:
				// Nothing to do here
				break;
			}

			entry.busyConnections.offer(result);
			cs.csv = ConnectionStateValue.BUSY;
			cs.busyCount++;
		} finally {
			if (debug())
				dprint("<-makeResultBusy: " + result);
		}
	}

	private C tryIdleConnections(CacheEntry<C> entry) {
		if (debug()) {
			dprint("->tryIdleConnections");
		}

		try {
			return entry.idleConnections.poll();
		} finally {
			if (debug()) {
				dprint("<-tryIdleConnections");
			}
		}
	}

	private C tryBusyConnections(CacheEntry<C> entry) {
		// Use a busy connection. Note that there MUST be a busy
		// connection available at this point, because
		// tryNewConnection did not create a new connection.
		if (debug()) {
			dprint("->tryBusyConnections");
		}

		try {
			C result = entry.busyConnections.poll();

			if (result == null) {
				throw new RuntimeException(
						"INTERNAL ERROR: no busy connection available");
			}

			return result;
		} finally {
			if (debug()) {
				dprint("<-tryBusyConnections");
			}
		}
	}

	public synchronized C get(final ContactInfo<C> cinfo) throws IOException {

		return get(cinfo, null);
	}

	public synchronized ConnectionState<C> getConnectionState(
			ContactInfo<C> cinfo, CacheEntry<C> entry, C conn) {

		if (debug())
			dprint("->getConnectionState: " + conn);

		try {
			ConnectionState<C> cs = connectionMap.get(conn);
			if (cs == null) {
				if (debug())
					dprint(".getConnectionState: " + conn
							+ " creating new ConnectionState" + cs);

				cs = new ConnectionState<C>(cinfo, entry, conn);
				connectionMap.put(conn, cs);
			} else {
				if (debug())
					dprint(".getConnectionState: " + conn
							+ " found ConnectionState" + cs);
			}

			return cs;
		} finally {
			if (debug())
				dprint("<-getConnectionState: " + conn);
		}
	}

	public synchronized C get(final ContactInfo<C> cinfo,
			final ConnectionFinder<C> finder) throws IOException {

		if (debug()) {
			dprint("->get: " + cinfo);
		}

		ConnectionState<C> cs = null;

		try {
			final CacheEntry<C> entry = getEntry(cinfo);
			C result = null;

			if (numberOfConnections() >= highWaterMark()) {
				// This reclaim probably does nothing, because
				// connections are reclaimed on release in the
				// overflow state.
				reclaim();
			}

			if (finder != null) {
				// Try the finder if present.
				if (debug()) {
					dprint(".get: " + cinfo
							+ " Calling the finder to get a connection");
				}

				result = finder.find(cinfo, entry.idleConnectionsView,
						entry.busyConnectionsView);

				if (result != null) {
					cs = getConnectionState(cinfo, entry, result);

					// Dequeue from cache entry if not NEW
					if (cs.csv == ConnectionStateValue.BUSY)
						entry.busyConnections.remove(result);
					else if (cs.csv == ConnectionStateValue.IDLE)
						entry.idleConnections.remove(result);
				}
			}

			if (result == null) {
				result = tryIdleConnections(entry);
			}

			if (result == null) {
				result = tryNewConnection(entry, cinfo);
			}

			if (result == null) {
				result = tryBusyConnections(entry);
			}

			if (cs == null)
				cs = getConnectionState(cinfo, entry, result);

			makeResultBusy(result, cs, entry);
			return result;
		} finally {
			if (debug()) {
				dprint(".get " + cinfo + " totalIdle=" + totalIdle
						+ " totalBusy=" + totalBusy);

				dprint("<-get " + cinfo + " ConnectionState=" + cs);
			}
		}
	}

	// If overflow, close conn and return true,
	// otherwise enqueue on reclaimable queue and return false.
	private boolean reclaimOrClose(ConnectionState<C> cs, final C conn) {
		if (debug())
			dprint("->reclaimOrClose: " + conn);

		try {
			final boolean isOverflow = numberOfConnections() > highWaterMark();

			if (isOverflow) {
				if (debug()) {
					dprint(".reclaimOrClose: closing overflow connection "
							+ conn);
				}

				close(conn);
			} else {
				if (debug()) {
					dprint(".reclaimOrClose: queuing reclaimable connection "
							+ conn);
				}

				cs.reclaimableHandle = reclaimableConnections.offer(conn);
			}

			return isOverflow;
		} finally {
			if (debug())
				dprint("<-reclaimOrClose: " + conn);
		}
	}

	public synchronized void release(final C conn,
			final int numResponsesExpected) {

		if (debug()) {
			dprint("->release: " + conn + " expecting " + numResponsesExpected
					+ " responses");
		}

		final ConnectionState<C> cs = connectionMap.get(conn);

		try {
			if (cs == null) {
				if (debug()) {
					dprint(".release: " + conn + " was closed");
				}

				return;
			} else {
				cs.expectedResponseCount += numResponsesExpected;
				int numResp = cs.expectedResponseCount;
				int numBusy = --cs.busyCount;
				if (numBusy < 0) {
					if (debug()) {
						dprint(".release: " + conn + " numBusy=" + numBusy
								+ " is < 0: error");
					}

					cs.busyCount = 0;
					return;
				}

				if (debug()) {
					dprint(".release: " + numResp + " responses expected");
					dprint(".release: " + numBusy + " busy count");
				}

				if (numBusy == 0) {
					final CacheEntry<C> entry = cs.entry;
					boolean wasOnBusy = entry.busyConnections.remove(conn);
					if (!wasOnBusy)
						if (debug())
							dprint(".release: " + conn
									+ " was NOT on busy queue, "
									+ "but should have been");

					boolean connectionClosed = false;
					if (numResp == 0) {
						connectionClosed = reclaimOrClose(cs, conn);
					}

					decrementTotalBusy();

					if (!connectionClosed) {
						if (debug()) {
							dprint(".release: queuing idle connection " + conn);
						}

						totalIdle++;
						entry.idleConnections.offer(conn);
						cs.csv = ConnectionStateValue.IDLE;
					}
				}
			}
		} finally {
			if (debug()) {
				dprint(".release " + conn + " cs=" + cs + " totalIdle="
						+ totalIdle + " totalBusy=" + totalBusy);

				dprint("<-release" + conn);
			}
		}
	}

	/**
	 * Decrement the number of expected responses. When a connection is idle and
	 * has no expected responses, it can be reclaimed.
	 *
	 * @param conn
	 *            a connection
	 */
	public synchronized void responseReceived(final C conn) {
		if (debug()) {
			dprint("->responseReceived: " + conn);
		}

		try {
			final ConnectionState<C> cs = connectionMap.get(conn);
			if (cs == null) {
				if (debug()) {
					dprint(".responseReceived: "
							+ "received response on closed connection " + conn);
				}

				return;
			}

			final int waitCount = --cs.expectedResponseCount;

			if (debug()) {
				dprint(".responseReceived: " + conn + " waitCount=" + waitCount);
			}

			if (waitCount < 0) {
				if (debug()) {
					dprint(".responseReceived: " + conn
							+ " incorrect call: error");
				}
				cs.expectedResponseCount = 0;
				return;
			}

			if ((waitCount == 0) && (cs.busyCount == 0)) {
				reclaimOrClose(cs, conn);
			}
		} finally {
			if (debug()) {
				dprint("<-responseReceived: " + conn);
			}
		}
	}

	/**
	 * Close a connection, regardless of whether the connection is busy or not.
	 *
	 * @param conn
	 *            a connection
	 */
	public synchronized void close(final C conn) {
		if (debug()) {
			dprint("->close: " + conn);
		}

		try {
			final ConnectionState<C> cs = connectionMap.remove(conn);
			if (cs == null) {
				if (debug()) {
					dprint(".close: " + conn + " was already closed");
				}

				return;
			}

			if (debug()) {
				dprint(".close: " + conn + "Connection state=" + cs);
			}

			final ConcurrentQueue.Handle rh = cs.reclaimableHandle;
			if (rh != null) {
				boolean result = rh.remove();
				if (debug()) {
					dprint(".close: " + conn + "reclaimableHandle .remove = "
							+ result);
				}
			}

			if (cs.entry.busyConnections.remove(conn)) {
				if (debug()) {
					dprint(".close: " + conn + " removed from busyConnections");
				}

				decrementTotalBusy();
			}

			if (cs.entry.idleConnections.remove(conn)) {
				if (debug()) {
					dprint(".close: " + conn + " removed from idleConnections");
				}

				decrementTotalIdle();
			}

			try {
				conn.close();
			} catch (IOException exc) {
				if (debug())
					dprint(".close: " + conn + ": Caught IOException on close:"
							+ exc);
			}
		} finally {
			if (debug()) {
				dprintStatistics();
				dprint("<-close: " + conn);
			}
		}
	}
}

// End of file.
