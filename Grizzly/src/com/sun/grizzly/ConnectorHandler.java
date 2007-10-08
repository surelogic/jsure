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

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * Client side interface used to implement non blocking client operation. 
 * Implementation of this class must make sure the following methods are 
 * invoked in that order:
 * 
 * (1) connect()
 * (2) read() or write().
 * 
 *
 * @param E a <code>SelectorHandler</code>
 * @param P a <code>CallbackHandler</code>
 * @author Jeanfrancois Arcand
 */
public interface ConnectorHandler<E extends SelectorHandler, P extends CallbackHandler> extends Handler, Closeable {
    
    
     /**
     * A token decribing the protocol supported by an implementation of this
     * interface
     * @return <code>Controller.Protocol</code>
      */
    public Controller.Protocol protocol();  
    
    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g 
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke 
     * the CallBackHandler.
     * @param remoteAddress remote address to connect
     * @param callbackHandler the handler invoked by the Controller when 
     *        an non blocking operation is ready to be handled.
     * @param e <code>SelectorHandler</code>
     * @throws java.io.IOException 
     */
    public void connect(SocketAddress remoteAddress, 
                        P callbackHandler,
                        E e) throws IOException;

    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g 
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke 
     * the CallBackHandler.
     * @param remoteAddress remote address to connect 
     * @param callbackHandler the handler invoked by the Controller when 
     *        an non blocking operation is ready to be handled.
     * @throws java.io.IOException
     */    
    public void connect(SocketAddress remoteAddress, 
                        P callbackHandler) throws IOException;
    
    
    /**
     * Connect to hostname:port. Internally an instance of Controller and
     * its default SelectorHandler will be created everytime this method is 
     * called. This method should be used only and only if no external 
     * Controller has been initialized.
     * @param remoteAddress remote address to connect
     * @throws java.io.IOException 
     */
    public void connect(SocketAddress remoteAddress)
        throws IOException;         
    
    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g 
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke 
     * the CallBackHandler.
     * @param remoteAddress remote address to connect 
     * @param localAddress local address to bind
     * @param callbackHandler the handler invoked by the Controller when 
     *        an non blocking operation is ready to be handled. 
     * @param e <code>SelectorHandler</code>
     * @throws java.io.IOException
     */
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, 
                        P callbackHandler,
                        E e) throws IOException;

    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g 
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke 
     * the CallBackHandler.
     * @param remoteAddress remote address to connect
     * @param localAddress local address to bind
     * @param callbackHandler the handler invoked by the Controller when 
     *        an non blocking operation is ready to be handled.
     * @throws java.io.IOException 
     */    
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, 
                        P callbackHandler) throws IOException;
    
    
    /**
     * Connect to hostname:port. Internally an instance of Controller and
     * its default SelectorHandler will be created everytime this method is 
     * called. This method should be used only and only if no external 
     * Controller has been initialized.
     * @param remoteAddress remote address to connect
     * @param localAddress local address to bind
     * @throws java.io.IOException 
     */
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress)
        throws IOException;
    
    
    /**
     * Read bytes. If blocking is set to <tt>true</tt>, a pool of temporary
     * <code>Selector</code> will be used to read bytes.
     * @param byteBuffer The byteBuffer to store bytes.
     * @param blocking <tt>true</tt> if a a pool of temporary Selector
     *        is required to handle a blocking read.
     * @return number of bytes read
     * @throws java.io.IOException 
     */
    public long read(ByteBuffer byteBuffer, boolean blocking) throws IOException;

    
    /**
     * Writes bytes. If blocking is set to <tt>true</tt>, a pool of temporary
     * <code>Selector</code> will be used to writes bytes.
     * @param byteBuffer The byteBuffer to write.
     * @param blocking <tt>true</tt> if a a pool of temporary Selector
     *        is required to handle a blocking write.
     * @return number of bytes written
     * @throws java.io.IOException 
     */    
    public long write(ByteBuffer byteBuffer, boolean blocking) throws IOException;  
    
    
    /**
     * Close the underlying connection.
     * @throws java.io.IOException 
     */
    public void close() throws IOException;
    
    
    /**
     * Decide how the OP_CONNECT final steps are handled.
     * @param key <code>SelectionKey</code>
     */
    public void finishConnect(SelectionKey key);
    
    
    /**
     * Set the <code>Controller</code> associated with this instance.
     * @param controller <code>Controller</code>
     */
    public void setController(Controller controller);
    
    
    /**
     * Return the <code>Controller</code>
     * @return 
     */
    public Controller getController();
    
    /**
     * Method returns <code>SelectorHandler</code>, which manages this 
     * <code>ConnectorHandler</code>
     * @return <code>SelectorHandler</code>
     */
    public E getSelectorHandler();

    /**
     * Method returns <code>ConnectorHandler</code>'s underlying channel
     * @return channel
     */
    public SelectableChannel getUnderlyingChannel();
    
    /**
     * Returns <code>ConnectorHandler</code>'s callback handler instance,
     * which is used to process occuring events
     * 
     * @return callback handler
     */
    public P getCallbackHandler();
    
    /**
     * Sets <code>ConnectorHandler</code>'s callback handler instance,
     * which is used to process occuring events
     * 
     * @param callbackHandler handler
     */
    public void setCallbackHandler(P callbackHandler);
}
