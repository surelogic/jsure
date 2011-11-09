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

import com.sun.grizzly.util.ThreadAttachment;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import static com.sun.grizzly.filter.SSLReadFilter.EXPIRE_TIME;
import com.sun.grizzly.util.WorkerThread;

/**
 * Default implementation of a SelectionKey Handler. By default, this
 * class will attach a Long to a SelectionKey in order to calculate the
 * time a SelectionKey can stay active. By default, a SelectionKey will be
 * active for 30 seconds. If during that 30 seconds the client isn't pushing
 * bytes (or closing the connection). the SelectionKey will be expired and
 * its channel closed.
 *
 * @author Jeanfrancois Arcand
 */
public class DefaultSelectionKeyHandler implements SelectionKeyHandler{
    
    
    protected Logger logger = Controller.logger();
    
    
    /**
     * Next time the exprireKeys() will delete keys.
     */
    protected long nextKeysExpiration = 0;
    
    
    /*
     * Number of seconds before idle keep-alive connections expire
     */
    protected long timeout = 30 * 1000L;
    
    /**
     * Associated <code>SelectorHandler</code>
     */
    private SelectorHandler selectorHandler;
    
    public DefaultSelectionKeyHandler() {
    }
    
    public DefaultSelectionKeyHandler(SelectorHandler selectorHandler) {
        this.selectorHandler = selectorHandler;
    }

    /**
     * Set associated <code>SelectorHandler</code>
     */
    public void setSelectorHandler(SelectorHandler selectorHandler) {
        this.selectorHandler = selectorHandler;
    } 
    
    /**
     * {@inheritDoc}
     */
    public void process(SelectionKey key) {
        Object attachment = key.attachment();
        // TODO get rid of instanceof
        if (attachment != null && attachment instanceof ThreadAttachment){
            ((WorkerThread)Thread.currentThread())
                .attach((ThreadAttachment)attachment);
            key.attach(null);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void postProcess(SelectionKey key) {
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void register(Iterator<SelectionKey> iterator, int ops) {
        long currentTime = System.currentTimeMillis();
        SelectionKey key;
        while (iterator.hasNext()) {
            key = iterator.next();
            iterator.remove();
            if (!key.isValid()){
                continue;
            }
            
            key.interestOps(key.interestOps() | ops);            
            Object attachment = key.attachment();
            // By default, attachment a null.
            if (attachment== null) {
                key.attach(currentTime);
            } else if (attachment instanceof ThreadAttachment){
                ((ThreadAttachment)attachment).setTimeout(currentTime);
            }  
        }
    }
    
    
    /**
     * Attach a times out to the SelectionKey used to cancel 
     * idle connection. Null when the feature is not required.
     *
     * @param key <code>SelectionKey</code> to register
     * @param currentTime the System.currentTimeMillis
     * @deprecated
     */
    public void register(SelectionKey key, long currentTime){
       ;
    }
    
    
    /**
     * @deprecated
     */
    public void expire(SelectionKey key, long currentTime) {
        ;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void expire(Iterator<SelectionKey> iterator) {
        if (timeout <= 0) return;
        
        long currentTime = System.currentTimeMillis();
        SelectionKey key;
        while (iterator.hasNext()) {
            key = iterator.next();
            
            if (!key.isValid()){
                continue;
            }

            if (currentTime < nextKeysExpiration) {
                return;
            }
            nextKeysExpiration = currentTime + timeout;

            // Keep-alive expired
            Object attachment = key.attachment();

            // This is extremely bad to invoke instanceof here but 
            // since the framework expose the SelectionKey, an application
            // can always attach an object on the SelectionKey and we 
            // can't predict the type of the attached object.
            if (attachment != null){
                try{
                    long expire  = 0L;
                    if (attachment instanceof Long) {
                        expire = (Long)attachment;
                    } else if (attachment instanceof SSLEngine){
                        SSLSession sslSession = ((SSLEngine)attachment)
                            .getSession();
                        if (sslSession != null
                                && sslSession.getValue(EXPIRE_TIME) != null){
                            expire = (Long)sslSession.getValue(EXPIRE_TIME);
                        }
                    } else if (attachment instanceof ThreadAttachment){
                        expire = ((ThreadAttachment)attachment).getTimeout();
                    } else {
                        return;
                    }

                    if (currentTime - expire >= timeout) {
                        cancel(key);
                    } else if (expire + timeout < nextKeysExpiration){
                        nextKeysExpiration = expire + timeout;
                    }
                } catch (ClassCastException ex){
                    if (logger.isLoggable(Level.FINEST)){
                        logger.log(Level.FINEST,
                                "Invalid SelectionKey attachment",ex);
                    }
                }
            }
        }
    }
    
    
    /**
     * Cancel a SelectionKey and close its associated Channel.
     * @param key <code>SelectionKey</code> to cancel
     */
    public void cancel(SelectionKey key) {
        if (key == null || !key.isValid()) {
            return;
        }
        
        if (selectorHandler != null) {
            selectorHandler.closeChannel(key.channel());
        } else {
            closeChannel(key.channel());
        }
        
        key.attach(null);
        key.cancel();
        key = null;
    }
    
    
    public void close(SelectionKey key) {
        cancel(key);
    }
    
    
    public Logger getLogger() {
        return logger;
    }
    
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    
    public long getTimeout() {
        return timeout;
    }
    
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    protected void closeChannel(SelectableChannel channel) {
        if (channel instanceof SocketChannel) {
            Socket socket = ((SocketChannel) channel).socket();
            
            try{
                if (!socket.isInputShutdown()) socket.shutdownInput();
            } catch (IOException ex){
                ;
            }
            
            try{
                if (!socket.isOutputShutdown()) socket.shutdownOutput();
            } catch (IOException ex){
                ;
            }
            
            try{
                socket.close();
            } catch (IOException ex){
                ;
            }
        }
        
        try{
            channel.close();
        } catch (IOException ex){
            ; // LOG ME
        }
    }    
}
