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
import java.util.logging.Logger;

import com.sun.grizzly.connectioncache.spi.concurrent.ConcurrentQueueFactory;
import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("protected TotalRegion")
@RegionLock("L is this protects TotalRegion"/*is CONSISTENT*/)
abstract class ConnectionCacheBlockingBase<C extends Closeable>
        extends ConnectionCacheBase<C> {
    
	@InRegion("TotalRegion")
    protected int totalBusy ;	// Number of busy connections
	@InRegion("TotalRegion")
    protected int totalIdle ;	// Number of idle connections
    
    @Borrowed("this"/*is CONSISTENT*/)
    ConnectionCacheBlockingBase( String cacheType, int highWaterMark,
            int numberToReclaim, Logger logger ) {
        
        super( cacheType, highWaterMark, numberToReclaim, logger ) ;
        
        this.totalBusy = 0 ;
        this.totalIdle = 0 ;
        
        this.reclaimableConnections =
                ConcurrentQueueFactory.<C>makeConcurrentQueue() ;
    }
    
    public synchronized long numberOfConnections() {
        return totalIdle + totalBusy ;
    }
    
    public synchronized long numberOfIdleConnections() {
        return totalIdle ;
    }
    
    public synchronized long numberOfBusyConnections() {
        return totalBusy ;
    }
    
    public synchronized long numberOfReclaimableConnections() {
        return reclaimableConnections.size() ;
    }
}

