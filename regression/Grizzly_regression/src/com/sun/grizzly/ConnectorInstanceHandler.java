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

package com.sun.grizzly;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This interface is responsible of handling <code>ConnectorHandler</code>
 * instance creation. This interface can be used to implement a connection
 * pooling mechanism.
 *
 * @param E ConnectorHandler implementation this pool will manage
 * @author Jeanfrancois
 */
public interface ConnectorInstanceHandler<E extends ConnectorHandler> {
    
    
    /**
     * Acquire a <code>ConnectorHandler</code>
     * @return instance of ConnectorHandler
     */
    public E acquire();
    
    
    /**
     * Release a <code>ConnectorHandler</code>
     * @param connectorHandler release connector handler
     */
    public void release(E connectorHandler);
    
    /**
     * Concurrent Queue ConnectorInstanceHandler implementation
     * @param E ConnectorHandler implementation this pool will manage
     */
    public abstract class ConcurrentQueueConnectorInstanceHandler<E extends ConnectorHandler> implements ConnectorInstanceHandler<E> {
        /**
         * Simple queue used to pool <code>ConnectorHandler</code>
         */
        private ConcurrentLinkedQueue<E> pool;
        
        
        public ConcurrentQueueConnectorInstanceHandler(){
            pool = new ConcurrentLinkedQueue<E>();
        }
        
        /**
         * Acquire a <code>ConnectorHandler</code>
         */
        public E acquire(){
            E connectorHandler = pool.poll();
            if (connectorHandler == null){
                connectorHandler = newInstance();
            }
            return connectorHandler;
        }
        
        /**
         * Release a <code>ConnectorHandler</code>
         */
        public void release(E connectorHandler){
            pool.offer(connectorHandler);
        }
        
        public abstract E newInstance();
    }
    
    /**
     * Concurrent Queue ConnectorInstanceHandler implementation
     * @param E ConnectorHandler implementation this pool will manage
     */
    public class ConcurrentQueueDelegateCIH<E extends ConnectorHandler> 
            extends ConcurrentQueueConnectorInstanceHandler<E> {
        
        // ConnectorHandler instance creator
        private Callable<E> delegate;
        
        public ConcurrentQueueDelegateCIH(Callable<E> delegate) {
            this.delegate = delegate;
        }
        
        public E newInstance() {
            try {
                return delegate.call();
            } catch(Exception e) {
                throw new IllegalStateException("Unexpected exception", e);
            }
        }
    }
}
