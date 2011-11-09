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
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.grizzly;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Default implementation of an ProtocolChainInstanceHandler. 
 * <code>ProtocolChain</code> are cached using a ConcurrentLinkedQueue. When
 * the queue becomes empty, a new instance of <code>ProtocolChain</code>
 * is created.
 *
 * @author Jeanfrancois Arcand
 */
public class DefaultProtocolChainInstanceHandler 
            implements ProtocolChainInstanceHandler{
    

    /**
     * List used to cache instance of ProtocolChain.
     */
    protected ConcurrentLinkedQueue<ProtocolChain> protocolChains;
        
    
    public DefaultProtocolChainInstanceHandler() {
        protocolChains = new ConcurrentLinkedQueue<ProtocolChain>();      
    }

    
    /**
     * Return a pooled instance of ProtocolChain. If the pool is empty,
     * a new instance of ProtocolChain will be returned.
     * @return <tt>ProtocolChain</tt>
     */
    public ProtocolChain poll() {
        ProtocolChain protocolChain = protocolChains.poll();
        if (protocolChain == null){
            protocolChain = new DefaultProtocolChain();
        }
        return protocolChain;       
    }

    
    /**
     * Offer (add) an instance of ProtocolChain to this instance pool.
     * @param protocolChain - <tt>ProtocolChain</tt> to offer / add to the pool
     * @return boolean, if <tt>ProtocolChain</tt> was successfully added 
     *          to the pool
     */
    public boolean offer(ProtocolChain protocolChain) {
        return protocolChains.offer(protocolChain);
    }
    
}
