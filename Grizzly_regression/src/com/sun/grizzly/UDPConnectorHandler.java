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

import com.sun.grizzly.util.ByteBufferInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Client side interface used to implement non blocking client operation.
 * Implementation of this class must make sure the following methods are
 * invoked in that order:
 * <p><pre><code>
 * (1) connect()
 * (2) read() or write().
 * </code></pre></p>
 *
 * @author Jeanfrancois Arcand
 */
public class UDPConnectorHandler implements ConnectorHandler<UDPSelectorHandler, CallbackHandler>{
    
    /**
     * The underlying UDPSelectorHandler used to mange SelectionKeys.
     */
    private UDPSelectorHandler selectorHandler;
    
    
    /**
     * A <code>CallbackHandler</code> handler invoked by the UDPSelectorHandler
     * when a non blocking operation is ready to be processed.
     */
    private CallbackHandler callbackHandler;
    
    
    /**
     * The connection's DatagramChannel.
     */
    private DatagramChannel datagramChannel;
    
    
    /**
     * Is the connection established.
     */
    private volatile boolean isConnected;
    
    
    /**
     * The internal Controller used (in case not specified).
     */
    private Controller controller;
    
    
    /**
     * IsConnected Latch related
     */
    private CountDownLatch isConnectedLatch;
    
    
    /**
     * Are we creating a controller every run.
     */
    private boolean isStandalone = false;
    
    
    /**
     * A blocking <code>InputStream</code> that use a pool of Selector
     * to execute a blocking read operation.
     */
    private ByteBufferInputStream inputStream;
    
    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke
     * the CallBackHandler.
     * @param remoteAddress remote address to connect
     * @param callbackHandler the handler invoked by the Controller when
     *        an non blocking operation is ready to be handled.
     */
    public void connect(SocketAddress remoteAddress,
            CallbackHandler callbackHandler) throws IOException {
        
        connect(remoteAddress,null,callbackHandler);
    }
    
    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke
     * the CallBackHandler.
     * @param remoteAddress remote address to connect
     * @param localAddress local address to bind
     * @param callbackHandler the handler invoked by the Controller when
     *        an non blocking operation is ready to be handled.
     */
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress,
            CallbackHandler callbackHandler) throws IOException {
        
        if (controller == null){
            throw new IllegalStateException("Controller cannot be null");
        }
        
        connect(remoteAddress,localAddress,callbackHandler,
                (UDPSelectorHandler)controller.getSelectorHandler(protocol()));
    }
    
    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke
     * the CallBackHandler.
     * @param remoteAddress remote address to connect
     * @param callbackHandler the handler invoked by the Controller when
     *        an non blocking operation is ready to be handled.
     * @param selectorHandler an instance of SelectorHandler.
     */
    public void connect(SocketAddress remoteAddress,
            CallbackHandler callbackHandler,
            UDPSelectorHandler selectorHandler) throws IOException {
        
        connect(remoteAddress,null,callbackHandler,selectorHandler);
    }
    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke
     * the CallBackHandler.
     * @param remoteAddress remote address to connect
     * @param localAddress local address to bin
     * @param callbackHandler the handler invoked by the Controller when
     *        an non blocking operation is ready to be handled.
     * @param selectorHandler an instance of SelectorHandler.
     */
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress,
            CallbackHandler callbackHandler,
            UDPSelectorHandler selectorHandler) throws IOException {
        
        if (isConnected){
            throw new AlreadyConnectedException();
        }
        
        if (controller == null){
            throw new IllegalStateException("Controller cannot be null");
        }
        
        if (selectorHandler == null){
            throw new IllegalStateException("Controller cannot be null");
        }
        
        this.selectorHandler = selectorHandler;
        this.callbackHandler = callbackHandler;
        
        // Wait for the onConnect to be invoked.
        isConnectedLatch = new CountDownLatch(1);
        
        selectorHandler.connect(remoteAddress,localAddress,callbackHandler);
        inputStream = new ByteBufferInputStream();
        
        try {
            isConnectedLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new IOException(ex.getMessage());
        }
    }
    
    
    /**
     * Connect to hostname:port. Internally an instance of Controller and
     * its default SelectorHandler will be created everytime this method is
     * called. This method should be used only and only if no external
     * Controller has been initialized.
     * @param remoteAddress remote address to connect
     */
    public void connect(SocketAddress remoteAddress)
    throws IOException {
        connect(remoteAddress,(SocketAddress)null);
    }
    
    
    /**
     * Connect to hostname:port. Internally an instance of Controller and
     * its default SelectorHandler will be created everytime this method is
     * called. This method should be used only and only if no external
     * Controller has been initialized.
     * @param remoteAddress remote address to connect
     * @param localAddress local address to bin
     */
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress)
    throws IOException {
        
        if (isConnected){
            throw new AlreadyConnectedException();
        }
        
        if (controller == null){
            isStandalone = true;
            controller = new Controller();
            controller.setSelectorHandler(new UDPSelectorHandler(true));
            DefaultPipeline pipeline = new DefaultPipeline();
            pipeline.initPipeline();
            pipeline.startPipeline();
            controller.setPipeline(pipeline);
            
            final CountDownLatch latch = new CountDownLatch(1);
            controller.addStateListener(new ControllerStateListenerAdapter() {
                @Override
                public void onReady() {
                    latch.countDown();
                }
                
                @Override
                public void onException(Throwable e) {
                    latch.countDown();
                }
            });
            
            callbackHandler = new CallbackHandler<Context>(){
                public void onConnect(IOEvent<Context> ioEvent) {
                    SelectionKey key = ioEvent.attachment().getSelectionKey();
                    finishConnect(key);
                }
                public void onRead(IOEvent<Context> ioEvent) {
                }
                public void onWrite(IOEvent<Context> ioEvent) {
                }
            };
            
            new Thread(controller).start();
            
            try {
                latch.await();
            } catch (InterruptedException ex) {
            }
        }
        
        connect(remoteAddress,localAddress,callbackHandler,
                (UDPSelectorHandler)controller.getSelectorHandler(protocol()));
    }
    
    
    /**
     * Read bytes. If blocking is set to <tt>true</tt>, a pool of temporary
     * <code>Selector</code> will be used to read bytes.
     * @param byteBuffer The byteBuffer to store bytes.
     * @param blocking <tt>true</tt> if a a pool of temporary Selector
     *        is required to handle a blocking read.
     */
    public long read(ByteBuffer byteBuffer, boolean blocking) throws IOException {
        if (!isConnected){
            throw new NotYetConnectedException();
        }
        
        SelectionKey key = datagramChannel.keyFor(selectorHandler.getSelector());
        if (blocking){
            inputStream.setSelectionKey(key);
            inputStream.setChannelType(
                    ByteBufferInputStream.ChannelType.DatagramChannel);
            int nRead = inputStream.read(byteBuffer);
            return nRead;
        } else {
            if (callbackHandler == null){
                throw new IllegalStateException
                        ("Non blocking read needs a CallbackHandler");
            }
            int nRead = datagramChannel.read(byteBuffer);
            
            if (nRead == 0){
                key.attach(callbackHandler);
                selectorHandler.register(key, SelectionKey.OP_READ);
            }
            return nRead;
        }
    }
    
    
    /**
     * Writes bytes. If blocking is set to <tt>true</tt>, a pool of temporary
     * <code>Selector</code> will be used to writes bytes.
     * @param byteBuffer The byteBuffer to write.
     * @param blocking <tt>true</tt> if a a pool of temporary Selector
     *        is required to handle a blocking write.
     */
    public long write(ByteBuffer byteBuffer, boolean blocking) throws IOException {
        if (!isConnected){
            throw new NotYetConnectedException();
        }
        
        SelectionKey key = datagramChannel.keyFor(selectorHandler.getSelector());
        if (blocking){
            throw new IllegalStateException("Blocking mode not supported");
        } else {
            if (callbackHandler == null){
                throw new IllegalStateException
                        ("Non blocking write needs a CallbackHandler");
            }
            int nWrite = datagramChannel.write(byteBuffer);
            
            if (nWrite == 0){
                key.attach(callbackHandler);
                selectorHandler.register(key, SelectionKey.OP_WRITE);
            }
            return nWrite;
        }
    }
    
    
    /**
     * Receive bytes.
     * @param byteBuffer The byteBuffer to store bytes.
     * @param socketAddress
     * @return number bytes sent
     * @throws java.io.IOException
     */
    public long send(ByteBuffer byteBuffer, SocketAddress socketAddress)
    throws IOException {
        if (!isConnected){
            throw new NotYetConnectedException();
        }
        
        if (callbackHandler == null){
            throw new IllegalStateException
                    ("Non blocking read needs a CallbackHandler");
        }
        
        return datagramChannel.send(byteBuffer,socketAddress);
    }
    
    
    /**
     * Receive bytes.
     * @param byteBuffer The byteBuffer to store bytes.
     * @return <code>SocketAddress</code>
     * @throws java.io.IOException
     */
    public SocketAddress receive(ByteBuffer byteBuffer) throws IOException {
        if (!isConnected){
            throw new NotYetConnectedException();
        }
        
        SelectionKey key = datagramChannel.keyFor(selectorHandler.getSelector());
        
        if (callbackHandler == null){
            throw new IllegalStateException
                    ("Non blocking read needs a CallbackHandler");
        }
        
        SocketAddress socketAddress = datagramChannel.receive(byteBuffer);
        return socketAddress;
    }
    
    
    /**
     * Close the underlying connection.
     */
    public void close() throws IOException{
        if (datagramChannel != null){
            if (selectorHandler != null){
                SelectionKey key =
                        datagramChannel.keyFor(selectorHandler.getSelector());
                
                if (key == null) return;
                
                key.cancel();
                key.attach(null);
            }
            datagramChannel.close();
        }
        
        if (controller != null && isStandalone){
            controller.stop();
            controller = null;
        }
        
        isStandalone = false;
        isConnected = false;
    }
    
    
    /**
     * Finish handling the OP_CONNECT interest ops.
     */
    public void finishConnect(SelectionKey key){
        if (controller.logger().isLoggable(Level.FINE)) {
            controller.logger().log(Level.FINE, "Finish connect");
        }
        
        datagramChannel = (DatagramChannel)key.channel();
        isConnected = datagramChannel.isConnected();
        isConnectedLatch.countDown();
    }
    
    
    /**
     * A token decribing the protocol supported by an implementation of this
     * interface
     */
    public Controller.Protocol protocol(){
        return Controller.Protocol.UDP;
    }
    
    
    /**
     * Is the underlying DatagramChannel connected.
     * @return true if connected, othewise false
     */
    public boolean isConnected(){
        return isConnected;
    }
    
    
    public Controller getController() {
        return controller;
    }
    
    
    public void setController(Controller controller) {
        this.controller = controller;
    }
    
    public SelectableChannel getUnderlyingChannel() {
        return datagramChannel;
    }
    
    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }
    
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
    
    public UDPSelectorHandler getSelectorHandler() {
        return selectorHandler;
    }
    
}
