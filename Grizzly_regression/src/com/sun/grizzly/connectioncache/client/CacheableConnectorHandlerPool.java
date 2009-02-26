/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.grizzly.connectioncache.client;


import com.sun.grizzly.ConnectorHandler;
import com.sun.grizzly.ConnectorHandlerPool;
import com.sun.grizzly.ConnectorInstanceHandler;
import com.sun.grizzly.Controller;
import com.sun.grizzly.Controller.Protocol;
import com.sun.grizzly.DefaultConnectorHandlerPool;
import com.sun.grizzly.connectioncache.spi.transport.ConnectionCacheFactory;
import com.sun.grizzly.connectioncache.spi.transport.ConnectionFinder;
import com.sun.grizzly.connectioncache.spi.transport.OutboundConnectionCache;

/**
 * <code>ConnectorInstanceHandler</code> which use a
 * <code>ConcurrentQueue</code> to pool <code>CacheableConnectorHandler</code>
 *
 * @author Alexey Stashok
 */
public class CacheableConnectorHandlerPool implements 
        ConnectorHandlerPool<CacheableConnectorHandler> {
    
    private Controller controller;
    private ConnectorHandlerPool protocolConnectorHandlerPool;
    private ConnectorInstanceHandler<CacheableConnectorHandler> connectorInstanceHandler;
    private OutboundConnectionCache<ConnectorHandler> outboundConnectionCache;
    private ConnectionFinder<ConnectorHandler> connectionFinder;
    
    public CacheableConnectorHandlerPool(Controller controller, int highWaterMark,
            int numberToReclaim, int maxParallel) {
            this(controller, highWaterMark, numberToReclaim, maxParallel, null);
    }
    
    public CacheableConnectorHandlerPool(Controller controller, int highWaterMark,
            int numberToReclaim, int maxParallel, ConnectionFinder<ConnectorHandler> connectionFinder) {
        this.controller = controller;
        this.outboundConnectionCache = 
                ConnectionCacheFactory.<ConnectorHandler>makeBlockingOutboundConnectionCache(
                "Grizzly outbound connection cache", highWaterMark, 
                numberToReclaim, maxParallel, controller.logger());
        this.connectionFinder = connectionFinder;
        protocolConnectorHandlerPool = new DefaultConnectorHandlerPool(controller);
        connectorInstanceHandler = new CacheableConnectorInstanceHandler();
    }

    public CacheableConnectorHandler acquireConnectorHandler(Protocol protocol) {
        CacheableConnectorHandler connectorHandler = connectorInstanceHandler.acquire();
        connectorHandler.setProtocol(protocol);
        return connectorHandler;
    }
    
    public void releaseConnectorHandler(CacheableConnectorHandler connectorHandler) {
        connectorInstanceHandler.release(connectorHandler);
    }
    
    OutboundConnectionCache<ConnectorHandler> getOutboundConnectionCache() {
        return outboundConnectionCache;
    }

    ConnectionFinder getConnectionFinder() {
        return connectionFinder;
    }

    Controller getController() {
        return controller;
    }
    
    ConnectorHandlerPool getProtocolConnectorHandlerPool() {
        return protocolConnectorHandlerPool;
    }
            
    /**
     * Default <code>ConnectorInstanceHandler</code> which use a
     * <code>ConcurrentQueue</code> to pool <code>ConnectorHandler</code>
     */
    private class CacheableConnectorInstanceHandler extends
            ConnectorInstanceHandler.ConcurrentQueueConnectorInstanceHandler<CacheableConnectorHandler> {
        
        public CacheableConnectorHandler newInstance() {
            return new CacheableConnectorHandler(CacheableConnectorHandlerPool.this);
        }
    }
}
