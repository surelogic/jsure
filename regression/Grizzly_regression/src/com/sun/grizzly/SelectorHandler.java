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

import com.sun.grizzly.util.Copyable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

/**
 * A SelectorHandler handles all java.nio.channels.Selector operations. 
 * One or more instance of a Selector are handled by SelectorHandler. 
 * The logic for processing of SelectionKey interest (OP_ACCEPT,OP_READ, etc.)
 * is usually defined using an instance of SelectorHandler.
 *
 * @author Jeanfrancois Arcand
 */
public interface SelectorHandler extends Handler, Copyable {   
    
    /**
     * A token decribing the protocol supported by an implementation of this
     * interface
     * @return SelectorHandler supported protocol
     */
    public Controller.Protocol protocol();
    
    
    /**
     * Gets the underlying selector.
     * @return underlying <code>Selector</code>
     */
    public Selector getSelector();
    
    
    /**
     * Sets the underlying <code>Selector</code>
     * @param selector underlying <code>Selector</code>
     */
    public void setSelector(Selector selector);

    
    /**
     * The SelectionKey that has been registered.
     * @return <code>Set</code> of <code>SelectionKey</code>
     */
    public Set<SelectionKey> keys();
    
    
    /**
     * Is the underlying Selector open.
     * @return true / false
     */
    public boolean isOpen();
    
    
    /**
     * Shutdown this instance.
     */
    public void shutdown();
    
    
    /**
     * This method is garantee to always be called before operation 
     * Selector.select().
     * @param controllerCtx <code>Context</code>
     * @throws java.io.IOException 
     */
    public void preSelect(Context controllerCtx) throws IOException;
    
        
    /**
     * Invoke the Selector.select() method.
     * @param controllerCtx 
     * @return <code>Set</code> of <code>SelectionKey</code>
     * @throws java.io.IOException 
     */    
    public Set<SelectionKey> select(Context controllerCtx) throws IOException;
    
    
    /**
     * This method is garantee to always be called after operation 
     * Selector.select().
     * @param controllerCtx <code>Context</code>
     * @throws java.io.IOException 
     */
    public void postSelect(Context controllerCtx) throws IOException;
     
    
    /**
     * Register the SelectionKey on the Selector.
     * @param key 
     * @param ops interested operations
     */
    public void register(SelectionKey key,int ops);
    
    
    /**
     * Accepts connection, without registering it for reading or writing
     * @param key
     * @return accepted <code>SelectableChannel</code>
     * @throws java.io.IOException
     */
    public SelectableChannel acceptWithoutRegistration(SelectionKey key)
            throws IOException;

    
    /**
     * Handle OP_ACCEPT.
     * @param key <code>SelectionKey</code>
     * @param controllerCtx <code>Context</code>
     * @return true if and only if the ProtocolChain must be invoked after
     *              executing this method.
     * @throws java.io.IOException 
     */
    public boolean onAcceptInterest(SelectionKey key,Context controllerCtx)
        throws IOException;    

    /**
     * Handle OP_READ.
     * @param key <code>SelectionKey</code>
     * @param controllerCtx <code>Context</code>
     * @return true if and only if the ProtocolChain must be invoked after
     *              executing this method.
     * @throws java.io.IOException 
     */   
    public boolean onReadInterest(SelectionKey key,Context controllerCtx)
        throws IOException;    
 
    
    /**
     * Handle OP_WRITE.
     * @param key <code>SelectionKey</code>
     * @param controllerCtx <code>Context</code>
     * @return true if and only if the ProtocolChain must be invoked after
     *              executing this method.
     * @throws java.io.IOException 
     */   
    public boolean onWriteInterest(SelectionKey key,Context controllerCtx)
        throws IOException; 
    
    
    /**
     * Handle OP_CONNECT.
     * @param key <code>SelectionKey</code>
     * @param controllerCtx <code>Context</code>
     * @return true if and only if the ProtocolChain must be invoked after
     *              executing this method.
     * @throws java.io.IOException 
     */    
    public boolean onConnectInterest(SelectionKey key,Context controllerCtx)
        throws IOException; 
    
    
    /**
     * Return an instance of the <code>ConnectorHandler</code>
     * @return <code>ConnectorHandler</code>
     */
    public ConnectorHandler acquireConnectorHandler();
    
    
    /**
     * Release a ConnectorHandler.
     * @param connectorHandler <code>ConnectorHandler</code>
     */
    public void releaseConnectorHandler(ConnectorHandler connectorHandler);
    
    
    /**
     * Configure the channel operations.
     * @param channel <code>SelectableChannel</code> to configure
     * @throws java.io.IOException on possible configuration related error
     */
    public void configureChannel(SelectableChannel channel) throws IOException;
    
    
    /**
     * Return the Pipeline used to execute this SelectorHandler's
     * SelectionKey ops
     * @return The pipeline to use, or null if the Controller's Pipeline
     * should be used.
     */
    public Pipeline pipeline(); 
    
    
    /**
     * Get the SelectionKeyHandler associated with this SelectorHandler.
     */
    public SelectionKeyHandler getSelectionKeyHandler();

    
    /**
     * Set SelectionKeyHandler associated with this SelectorHandler.
     */
    public void setSelectionKeyHandler(SelectionKeyHandler selectionKeyHandler);
    
    
    /**
     * Set the <code>ProtocolChainInstanceHandler</code> to use for 
     * creating instance of <code>ProtocolChain</code>.
     */
    public void setProtocolChainInstanceHandler(
            ProtocolChainInstanceHandler protocolChainInstanceHandler);
    
    
    /**
     * Return the <code>ProtocolChainInstanceHandler</code>
     */
    public ProtocolChainInstanceHandler getProtocolChainInstanceHandler();
    
    /**
     * Closes <code>SelectableChannel</code>
     */
    public void closeChannel(SelectableChannel channel);
}
