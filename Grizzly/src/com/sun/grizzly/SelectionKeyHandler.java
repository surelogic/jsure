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

import java.nio.channels.SelectionKey;
import java.util.Iterator;

/**
 * A SelectionKeyHandler is used to handle the life cycle of a SelectionKey.
 * Operations like cancelling, registering or closing are handled by
 * SelectionKeyHandler.
 *
 * @author Jeanfrancois Arcand
 */
public interface SelectionKeyHandler extends Handler{
    
    
    /**
     * <code>SelectionKey</code> process notification
     * @param key <code>SelectionKey</code> to process
     */
    public void process(SelectionKey key);
    
    
    /**
     * <code>SelectionKey</code> post process notification
     * @param key <code>SelectionKey</code> to process
     */
    public void postProcess(SelectionKey key);

    
    /**
     * Attach a times out to the SelectionKey used to cancel 
     * idle connection. Null when the feature is not required.
     *
     * @param key <code>SelectionKey</code> to register
     * @param currentTime the System.currentTimeMillis
     * @deprecated
     */
    public void register(SelectionKey key, long currentTime);
    
    
    /**
     * Register a set of <code>SelectionKey</code>s.
     * Note: After processing each <code>SelectionKey</code> it should be
     * removed from <code>Iterator</code>
     *
     * @param selectionKeySet <code>Iterator</code> of <code>SelectionKey</code>s 
     * @param selectionKeyOps The interest set to apply when registering.
     * to register
     */
    public void register(Iterator<SelectionKey> keyIterator, int selectionKeyOps);

    
    /**
     * Expire a <code>SelectionKey</code>. If a <code>SelectionKey</code> is 
     * inactive for certain time (timeout),  the <code>SelectionKey</code> 
     * will be cancelled and its associated Channel closed.
     * @param key <code>SelectionKey</code> to expire
     * @param currentTime the System.currentTimeMillis
     * @deprecated
     */
    public void expire(SelectionKey key, long currentTime);
    
    
    /**
     * Expire a <code>SelectionKey</code> set. Method checks 
     * each <code>SelectionKey</code> from the <code>Set</code>. And if 
     * a <code>SelectionKey</code> is inactive for certain time (timeout),
     * the <code>SelectionKey</code> will be cancelled and its associated Channel closed.
     * @param keyIterator <code>Iterator</code> of <code>SelectionKey</code>s 
     * to expire
     */
    public void expire(Iterator<SelectionKey> keyIterator);

    
    /**
     * Cancel a SelectionKey and close its Channel.
     * @param key <code>SelectionKey</code> to cancel
     */
    public void cancel(SelectionKey key);
    
        
    /**
     * Close the SelectionKey's channel input or output, but keep alive
     * the SelectionKey.
     * @param key <code>SelectionKey</code> to close
     */
    public void close(SelectionKey key);
    
}
