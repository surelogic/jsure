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

import com.sun.grizzly.Controller.Protocol;
import com.sun.grizzly.util.ByteBufferInputStream;
import com.sun.grizzly.util.OutputWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
/**
 * Non blocking TCP Connector Handler. The recommended way to use this class
 * is by creating an external Controller and share the same SelectorHandler
 * instance.
 * <p>
 * Recommended
 * -----------
 * </p><p><pre><code>
 * Controller controller = new Controller();
 * // new TCPSelectorHandler(true) means the Selector will be used only
 * // for client operation (OP_READ, OP_WRITE, OP_CONNECT).
 * TCPSelectorHandler tcpSelectorHandler = new TCPSelectorHandler(true);
 * controller.setSelectorHandler(tcpSelectorHandler);
 * TCPConnectorHandler tcpConnectorHandler = new TCPConnectorHandler();
 * tcpConnectorHandler.connect(localhost,port, new CallbackHandler(){...},
 *                             tcpSelectorHandler);
 * TCPConnectorHandler tcpConnectorHandler2 = new TCPConnectorHandler();
 * tcpConnectorHandler2.connect(localhost,port, new CallbackHandler(){...},
 *                             tcpSelectorHandler);
 * </code></pre></p><p>
 * Not recommended (but still works)
 * ---------------------------------
 * </p><p><pre><code>
 * TCPConnectorHandler tcpConnectorHandler = new TCPConnectorHandler();
 * tcpConnectorHandler.connect(localhost,port);
 * </code></pre></p><p>
 *
 * Internally, a new Controller will be created everytime connect(localhost,port)
 * is invoked, which has an impact on performance.
 *
 * @author Jeanfrancois Arcand
 */
public class TCPConnectorHandler implements ConnectorHandler<TCPSelectorHandler, CallbackHandler>{
    
    /**
     * The underlying TCPSelectorHandler used to mange SelectionKeys.
     */
    private TCPSelectorHandler selectorHandler;
    
    
    /**
     * A <code>CallbackHandler</code> handler invoked by the TCPSelectorHandler
     * when a non blocking operation is ready to be processed.
     */
    private CallbackHandler callbackHandler;
    
    
    /**
     * A blocking <code>InputStream</code> that use a pool of Selector
     * to execute a blocking read operation.
     */
    private ByteBufferInputStream inputStream;
    
    
    /**
     * The connection's SocketChannel.
     */
    private SocketChannel socketChannel;
    
    
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
     * The socket tcpDelay.
     * 
     * Default value for tcpNoDelay.
     */
    protected boolean tcpNoDelay = true;
    
    
    /**
     * The socket reuseAddress
     */
    protected boolean reuseAddress = true;
    
    
    /**
     * The socket linger.
     */
    protected int linger = -1;    
    
    
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
     * @throws java.io.IOException
     */
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress,
            CallbackHandler callbackHandler) throws IOException {
        
        if (controller == null){
            throw new IllegalStateException("Controller cannot be null");
        }
        
        connect(remoteAddress,localAddress,callbackHandler,
                (TCPSelectorHandler)controller.getSelectorHandler(protocol()));
    }
    
    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke
     * the CallBackHandler.
     * @param remoteAddress remote address to connect
     * @param callbackHandler the handler invoked by the Controller when
     *        an non blocking operation is ready to be handled.
     * @param selectorHandler an instance of SelectorHandler.
     * @throws java.io.IOException
     */
    public void connect(SocketAddress remoteAddress,
            CallbackHandler callbackHandler,
            TCPSelectorHandler selectorHandler) throws IOException {
        
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
     * @throws java.io.IOException
     */
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress,
            CallbackHandler callbackHandler,
            TCPSelectorHandler selectorHandler) throws IOException {
        
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
     * @throws java.io.IOException
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
     * @throws java.io.IOException
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
            controller.setSelectorHandler(new TCPSelectorHandler(true));
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
                    socketChannel = (SocketChannel)key.channel();
                    finishConnect(key);
                    getController().registerKey(key,SelectionKey.OP_WRITE,
                            Protocol.TCP);
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
                (TCPSelectorHandler)controller.getSelectorHandler(protocol()));
    }
    
    
    /**
     * Read bytes. If blocking is set to <tt>true</tt>, a pool of temporary
     * <code>Selector</code> will be used to read bytes.
     * @param byteBuffer The byteBuffer to store bytes.
     * @param blocking <tt>true</tt> if a a pool of temporary Selector
     *        is required to handle a blocking read.
     * @return number of bytes read
     * @throws java.io.IOException
     */
    public long read(ByteBuffer byteBuffer, boolean blocking) throws IOException {
        if (!isConnected){
            throw new NotYetConnectedException();
        }
        
        SelectionKey key = socketChannel.keyFor(selectorHandler.getSelector());
        if (blocking){
            inputStream.setSelectionKey(key);
            return inputStream.read(byteBuffer);
        } else {
            if (callbackHandler == null){
                throw new IllegalStateException
                        ("Non blocking read needs a CallbackHandler");
            }
            int nRead = socketChannel.read(byteBuffer);
            
            if (nRead == 0){
                key.attach(callbackHandler);
                selectorHandler.register(key,SelectionKey.OP_READ);
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
     * @return number of bytes written
     * @throws java.io.IOException
     */
    public long write(ByteBuffer byteBuffer, boolean blocking) throws IOException {
        if (!isConnected){
            throw new NotYetConnectedException();
        }
        
        SelectionKey key = socketChannel.keyFor(selectorHandler.getSelector());
        if (blocking){
            return OutputWriter.flushChannel(socketChannel,byteBuffer);
        } else {
            if (callbackHandler == null){
                throw new IllegalStateException
                        ("Non blocking write needs a CallbackHandler");
            }
            
            int nWrite = 1;
            int totalWriteBytes = 0;
            while (nWrite > 0 && byteBuffer.hasRemaining()){
                nWrite = socketChannel.write(byteBuffer);
                totalWriteBytes += nWrite;
            }
            
            if (totalWriteBytes == 0 && byteBuffer.hasRemaining()){
                key.attach(callbackHandler);
                selectorHandler.register(key,SelectionKey.OP_WRITE);
            }
            return totalWriteBytes;
        }
    }
    
    
    /**
     * Close the underlying connection.
     */
    public void close() throws IOException{
        if (socketChannel != null){
            if (selectorHandler != null){
                SelectionKey key =
                        socketChannel.keyFor(selectorHandler.getSelector());
                
                if (key == null) return;
                
                key.cancel();
                key.attach(null);
            }
            socketChannel.close();
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
     * @param key - a <code>SelectionKey</code>
     */
    public void finishConnect(SelectionKey key){
        try{
            if (controller.logger().isLoggable(Level.FINE)) {
                controller.logger().log(Level.FINE, "Finish connect");
            }
            
            socketChannel = (SocketChannel)key.channel();
            socketChannel.finishConnect();
            isConnected = socketChannel.isConnected();
            configureChannel(socketChannel);
            
            if (controller.logger().isLoggable(Level.FINE)) {
                controller.logger().log(Level.FINE, "isConnected: " + isConnected);
            }
        } catch (IOException ex){
            controller.logger().log(Level.WARNING,"Unable to connect to:" +
                    key.channel(), ex);
        } finally {
            isConnectedLatch.countDown();
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void configureChannel(SelectableChannel channel) throws IOException{
        Socket socket = ((SocketChannel) channel).socket();

        try{
            if(linger >= 0 ) {
                socket.setSoLinger( true, linger);
            }
        } catch (SocketException ex){
            Controller.logger().log(Level.WARNING,
                    "setSoLinger exception ",ex);
        }
        
        try{
            socket.setTcpNoDelay(tcpNoDelay);
        } catch (SocketException ex){
            Controller.logger().log(Level.WARNING,
                    "setTcpNoDelay exception ",ex);
        }
        
        try{
            socket.setReuseAddress(reuseAddress);
        } catch (SocketException ex){
            Controller.logger().log(Level.WARNING,
                    "setReuseAddress exception ",ex);
        }
    }
    
    /**
     * A token decribing the protocol supported by an implementation of this
     * interface
     * @return this <code>ConnectorHandler</code>'s protocol
     */
    public Controller.Protocol protocol(){
        return Controller.Protocol.TCP;
    }
    
    
    /**
     * Is the underlying SocketChannel connected.
     * @return <tt>true</tt> if connected, otherwise <tt>false</tt>
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
        return socketChannel;
    }
    
    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }
    
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
    
    public TCPSelectorHandler getSelectorHandler() {
        return selectorHandler;
    }
    /**
     * Return the tcpNoDelay value used by the underlying accepted Sockets.
     * 
     * Also see setTcpNoDelay(boolean tcpNoDelay)
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }
    
    
    /**
     * Enable (true) or disable (false) the underlying Socket's
     * tcpNoDelay.
     * 
     * Default value for tcpNoDelay is disabled (set to false).
     * 
     * Disabled by default since enabling tcpNoDelay for most applications
     * can cause packets to appear to arrive in a fragmented fashion where it 
     * takes multiple OP_READ events (i.e. multiple calls to read small 
     * messages). The common behaviour seen when this occurs is that often times
     * a small number of bytes, as small as 1 byte at a time is read per OP_READ
     * event dispatch.  This results in a large number of system calls to
     * read(), system calls to enable and disable interest ops and potentially
     * a large number of thread context switches between a thread doing the
     * Select(ing) and a worker thread doing the read.
     * 
     * The Connector side should also set tcpNoDelay the same as it is set here 
     * whenever possible.
     */
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
    
    
    public int getLinger() {
        return linger;
    }
    
    
    public void setLinger(int linger) {
        this.linger = linger;
    }
    
    
    public boolean isReuseAddress() {
        return reuseAddress;
    }
    
    
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }   
}
