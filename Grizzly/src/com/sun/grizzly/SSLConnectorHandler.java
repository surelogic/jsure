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

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import com.sun.grizzly.util.OutputWriter;
import com.sun.grizzly.util.SSLOutputWriter;
import com.sun.grizzly.util.SSLUtils;
import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.SingleThreaded;

/**
 * <p>
 * Non blocking SSL Connector Handler. The recommended way to use this class
 * is by creating an external Controller and share the same SelectorHandler
 * instance.
 * </p><p>
 * Recommended
 * -----------
 * </p><p><pre><code>
 * Controller controller = new Controller();
 * // new SSLSelectorHandler(true) means the Selector will be used only
 * // for client operation (OP_READ, OP_WRITE, OP_CONNECT).
 * SSLSelectorHandler sslSelectorHandler = new SSLSelectorHandler(true);
 * controller.setSelectorHandler(sslSelectorHandler);
 * SSLConnectorHandler sslConnectorHandler = new SSLConnectorHandler();
 * sslConnectorHandler.connect(localhost,port, new SSLCallbackHandler(){...},
 *                             sslSelectorHandler);
 * SSLConnectorHandler sslConnectorHandler2 = new SSLConnectorHandler();
 * sslConnectorHandler2.connect(localhost,port, new SSLCallbackHandler(){...},
 *                             sslSelectorHandler);
 * </code></pre></p><p>
 * Not recommended (but still works)
 * ---------------------------------
 * </p><p><pre><code>
 * SSLConnectorHandler sslConnectorHandler = new SSLConnectorHandler();
 * sslConnectorHandler.connect(localhost,port);
 *
 * Internally, an new Controller will be created everytime connect(localhost,port)
 * is invoked, which has an impact on performance.
 *
 * As common comment: developer should be very careful if dealing directly with
 * <code>SSLConnectorHandler</code>'s underlying socket channel! In most cases
 * there is no need to do this, but use read, write methods provided
 * by <code>SSLConnectorHandler</code>
 * </code></pre></p>
 *
 * @author Alexey Stashok
 * @author Jeanfrancois Arcand
 */
@RegionLock("Lock is class protects defaultSSLContext")
public class SSLConnectorHandler implements ConnectorHandler<SSLSelectorHandler, SSLCallbackHandler>, CallbackHandler {
    
    /**
     * Default Logger.
     */
    private static Logger logger = Logger.getLogger("grizzly");
    
    /**
     * The underlying SSLSelectorHandler used to mange SelectionKeys.
     */
    private SSLSelectorHandler selectorHandler;
    
    /**
     * A <code>SSLCallbackHandler</code> handler invoked by the SSLSelectorHandler
     * when a non blocking operation is ready to be processed.
     */
    private SSLCallbackHandler callbackHandler;
    
    /*
     * An empty ByteBuffer used for handshaking
     */
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    
    /**
     * Input buffer for reading encrypted data from channel
     */
    private ByteBuffer securedInputBuffer;
    
    /**
     * Output buffer, which contains encrypted data ready for writing to channel
     */
    private ByteBuffer securedOutputBuffer;
    
    /**
     * Buffer, where application data could be written during a asynchronous handshaking.
     * It is set when user application calls: SSLConnectorHandler.handshake(appDataBuffer)
     * and references appDataBuffer.
     */
    private ByteBuffer asyncHandshakeBuffer;
    
    /**
     * The connection's SocketChannel.
     */
    private SocketChannel socketChannel;
    
    /**
     * Default <code>SSLContext</code>, created on
     * top of default <code>SSLConfiguration</code>
     */
    private static volatile SSLContext defaultSSLContext;
    
    /**
     * Is the connection established.
     */
    private volatile boolean isConnected;
    
    /**
     * Is the handshake phase completed
     */
    private volatile boolean isHandshakeDone;
    
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
     * Is async handshake in progress
     */
    private boolean isProcessingAsyncHandshake;
    
    /**
     * Result of last <code>SSLEngine</code> operation
     */
    private SSLEngineResult sslLastOperationResult;
    
    /**
     * Current handshake status
     */
    private SSLEngineResult.HandshakeStatus handshakeStatus;
    
    /**
     * Current <code>SSLEngine</code> status
     */
    private SSLEngineResult.Status sslEngineStatus = null;
    
    
    /**
     * Are we creating a controller every run.
     */
    private boolean delegateSSLTasks;
    
    /**
     * Connector's <code>SSLEngine</code>
     */
    private SSLEngine sslEngine;
    
    /**
     * Connector's <code>SSLContext</code>
     */
    private SSLContext sslContext;
    
    @SingleThreaded
    @Borrowed("this")
    public SSLConnectorHandler() {
        this(defaultSSLContext);
    }
    
    @SingleThreaded
    @Borrowed("this")
    public SSLConnectorHandler(SSLConfig sslConfig) {
        this(sslConfig.createSSLContext());
    }
    
    @SingleThreaded
    @Borrowed("this")
    public SSLConnectorHandler(SSLContext sslContext) {
        if (sslContext == null) {
            if (defaultSSLContext == null) {
                synchronized (SSLConnectorHandler.class) {
                    if (defaultSSLContext == null) {
                        defaultSSLContext = SSLConfig.DEFAULT_CONFIG.createSSLContext();
                    }
                }
            }
            
            sslContext = defaultSSLContext;
        }
        
        this.sslContext = sslContext;
    }
    
    public boolean getDelegateSSLTasks() {
        return delegateSSLTasks;
    }
    
    public void setDelegateSSLTasks(boolean delegateSSLTasks) {
        this.delegateSSLTasks = delegateSSLTasks;
    }
    
    /**
     * Connect to hostname:port. When an aysnchronous event happens (e.g
     * OP_READ or OP_WRITE), the <code>Controller</code> will invoke
     * the CallBackHandler.
     * @param remoteAddress remote address to connect
     * @param callbackHandler the handler invoked by the Controller when
     *        an non blocking operation is ready to be handled.
     * @throws java.io.IOException
     */
    public void connect(SocketAddress remoteAddress, SSLCallbackHandler callbackHandler) throws IOException {
        connect(remoteAddress, null, callbackHandler);
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
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, SSLCallbackHandler callbackHandler) throws IOException {
        if (controller == null) {
            throw new IllegalStateException("Controller cannot be null");
        }
        
        connect(remoteAddress, localAddress, callbackHandler, (SSLSelectorHandler) controller.getSelectorHandler(protocol()));
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
    public void connect(SocketAddress remoteAddress, SSLCallbackHandler callbackHandler, SSLSelectorHandler selectorHandler) throws IOException {
        connect(remoteAddress, null, callbackHandler, selectorHandler);
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
            SSLCallbackHandler callbackHandler,
            SSLSelectorHandler selectorHandler) throws IOException {
        if (isConnected) {
            throw new AlreadyConnectedException();
        }
        
        if (controller == null) {
            throw new IllegalStateException("Controller cannot be null");
        }
        
        if (selectorHandler == null) {
            throw new IllegalStateException("Controller cannot be null");
        }
        
        this.selectorHandler = selectorHandler;
        this.callbackHandler = callbackHandler;
        
        // Wait for the onConnect to be invoked.
        isConnectedLatch = new CountDownLatch(1);
        
        selectorHandler.connect(remoteAddress, localAddress, this);
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
    public void connect(SocketAddress remoteAddress) throws IOException {
        connect(remoteAddress, (SocketAddress) null);
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
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress) throws IOException {
        if (isConnected) {
            throw new AlreadyConnectedException();
        }
        
        if (controller == null) {
            isStandalone = true;
            controller = new Controller();
            controller.setSelectorHandler(new SSLSelectorHandler(true));
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
                    if (latch.getCount() > 0) {
                        logger.log(Level.SEVERE, "Error occured on Controller startup: ", e);
                    }
                    
                    latch.countDown();
                }
            });
            
            callbackHandler = new SSLCallbackHandler<Context>() {
                
                public void onConnect(IOEvent<Context> ioEvent) {
                    SelectionKey key = ioEvent.attachment().getSelectionKey();
                    socketChannel = (SocketChannel) key.channel();
                    finishConnect(key);
                }
                
                public void onRead(IOEvent<Context> ioEvent) {
                }
                
                public void onWrite(IOEvent<Context> ioEvent) {
                }
                
                public void onHandshake(IOEvent<Context> ioEvent) {
                }
            };
            
            new Thread(controller).start();
            
            try {
                latch.await();
            } catch (InterruptedException ex) {
            }
        }
        
        connect(remoteAddress, localAddress, callbackHandler, (SSLSelectorHandler) controller.getSelectorHandler(protocol()));
    }
    
    /**
     * Initiate SSL handshake phase.
     * Handshake is required to be done after connection established.
     *
     * @param byteBuffer Application <code>ByteBuffer</code>, where application data
     * will be stored
     * @param blocking true, if handshake should be done in blocking mode, for non-blocking false
     * @return If blocking parameter is true - method should always return true if handshake is done,
     * or throw IOException otherwise. For non-blocking mode method returns true if handshake is done, or false
     * if handshake will be completed in non-blocking manner.
     * If False returned - <code>SSLConnectorHandler</code> will call callbackHandler.onHandshake() to notify
     * about finishing handshake phase.
     * @throws java.io.IOException if some error occurs during processing I/O operations/
     */
    public boolean handshake(ByteBuffer byteBuffer, boolean blocking) throws IOException {
        sslEngine.beginHandshake();
        handshakeStatus = sslEngine.getHandshakeStatus();
        
        if (blocking) {
            SSLUtils.doHandshake(socketChannel, byteBuffer, securedInputBuffer, securedOutputBuffer, sslEngine, handshakeStatus);
            finishHandshake();
            
            // Sync should be always done
            return true;
        } else {
            doAsyncHandshake(byteBuffer);
            
            // is async handshake completed
            return isHandshakeDone();
        }
    }
    
    /**
     * Read bytes. If blocking is set to <tt>true</tt>, a pool of temporary
     * <code>Selector</code> will be used to read bytes.
     * @param byteBuffer The byteBuffer to store bytes.
     * @param blocking <tt>true</tt> if a a pool of temporary Selector
     *        is required to handle a blocking read.
     * @return number of bytes read from a channel.
     * Be careful, because return value represents the length of encrypted data,
     * which was read from a channel. Don't use return value to determine the
     * availability of a decrypted data to process, but use byteBuffer.remaining().
     * @throws java.io.IOException
     */
    public long read(ByteBuffer byteBuffer, boolean blocking) throws IOException {
        if (!isConnected) {
            throw new NotYetConnectedException();
        }
        
        if (blocking) {
            return SSLUtils.doSecureRead(socketChannel, sslEngine,
                    byteBuffer, securedInputBuffer);
        } else {
            int nRead = doReadAsync(byteBuffer);
            
            if (nRead == 0) {
                registerSelectionKeyFor(SelectionKey.OP_READ);
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
     * @return number of bytes written on a channel.
     * Be careful, as non-crypted data is passed, but crypted data is written
     * on channel. Don't use return value to determine the
     * number of bytes from original buffer, which were written.
     * @throws java.io.IOException
     */
    public long write(ByteBuffer byteBuffer, boolean blocking) throws IOException {
        if (!isConnected) {
            throw new NotYetConnectedException();
        }
        
        if (blocking) {
            long nWrite = SSLOutputWriter.flushChannel(socketChannel,
                    byteBuffer, securedOutputBuffer, sslEngine);
            // Mark securedOutputBuffer as empty
            securedOutputBuffer.position(securedOutputBuffer.limit());
            return nWrite;
        } else {
            if (callbackHandler == null) {
                throw new IllegalStateException("Non blocking write needs a CallbackHandler");
            }
            
            int nWrite = 1;
            int totalWrite = 0;
            
            while (nWrite > 0 &&
                    (byteBuffer.hasRemaining() || securedOutputBuffer.hasRemaining())) {
                nWrite = doWriteAsync(byteBuffer);
                totalWrite += nWrite;
            }
            
            if (byteBuffer.hasRemaining() || securedOutputBuffer.hasRemaining()) {
                registerSelectionKeyFor(SelectionKey.OP_WRITE);
            }
            
            return totalWrite;
        }
    }
    
    
    /**
     * Close the underlying connection.
     */
    public void close() throws IOException {
        if (socketChannel != null) {
            if (isConnected) {
                try {
                    if (securedOutputBuffer.hasRemaining()) {
                        // if there is something is securedOutputBuffer - flush it
                        OutputWriter.flushChannel(socketChannel, securedOutputBuffer);
                    }
                    
                    // Close secure outbound channel and flush data
                    sslEngine.closeOutbound();
                    SSLUtils.wrap(EMPTY_BUFFER, securedOutputBuffer, sslEngine);
                    OutputWriter.flushChannel(socketChannel, securedOutputBuffer);
                } catch (IOException e) {
                    logger.log(Level.FINE, "IOException during closing the connector.", e);
                }
            }
            
            if (selectorHandler != null) {
                SelectionKey key = socketChannel.keyFor(selectorHandler.getSelector());
                
                if (key == null) {
                    return;
                }
                key.cancel();
                key.attach(null);
            }
            
            socketChannel.close();
        }
        
        if (controller != null && isStandalone) {
            controller.stop();
            controller = null;
        }
        
        sslEngine = null;
        asyncHandshakeBuffer = null;
        isStandalone = false;
        isConnected = false;
        isHandshakeDone = false;
    }
    
    
    /**
     * Finish handling the OP_CONNECT interest ops.
     * @param key - a <code>SelectionKey</code>
     */
    public void finishConnect(SelectionKey key) {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Finish connect");
            }

            socketChannel = (SocketChannel) key.channel();
            socketChannel.finishConnect();
            isConnected = socketChannel.isConnected();
            if (isConnected) {
                initSSLEngineIfRequired();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception occured on the connection phase.", e);
        } finally {
            isConnectedLatch.countDown();
        }
    }
    
    /**
     * Changes SSLConnectorHandler state, after handshake operation is done.
     * Normally should not be called by outside Grizzly, just in case when custom
     * handshake code was used.
     */
    public void finishHandshake() {
        isProcessingAsyncHandshake = false;
        isHandshakeDone = true;
    }
    
    /**
     * A token decribing the protocol supported by an implementation of this
     * interface
     * @return this <code>ConnectorHandler</code>'s protocol
     */
    public Controller.Protocol protocol() {
        return Controller.Protocol.TLS;
    }
    
    
    /**
     * Is the underlying SocketChannel connected.
     * @return <tt>true</tt> if connected, otherwise <tt>false</tt>
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Is the underlying SocketChannel connected.
     * @return <tt>true</tt> if connected, otherwise <tt>false</tt>
     */
    public boolean isHandshakeDone() {
        return isHandshakeDone && !isProcessingAsyncHandshake;
    }
    
    public Controller getController() {
        return controller;
    }
    
    public void setController(Controller controller) {
        this.controller = controller;
    }
    
    /**
     * Get SSLConnector's <code>SSLContext</code>
     */
    public SSLContext getSSLContext() {
        return sslContext;
    }
    
    /**
     * Set <code>SSLContext</code>.
     * Use this method to change SSLConnectorHandler configuration.
     * New configuration will become active only after SSLConnector
     * will be closed and connected again.
     */
    public void setSSLContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }
    
    /**
     * Configure SSLConnectorHandler's SSL settings.
     *
     * Use this method to change SSLConnectorHandler configuration.
     * New configuration will become active only after SSLConnector
     * will be closed and connected again.
     */
    public void configure(SSLConfig sslConfig) {
        this.sslContext = sslConfig.createSSLContext();
    }
    
    /**
     * Returns SSLConnector's <code>SSLEngine</code>
     * @return <code>SSLEngine</code>
     */
    public SSLEngine getSSLEngine() {
        return sslEngine;
    }
    
    /**
     * Set <code>SSLEngine</code>
     * @param sslEngine <code>SSLEngine</code>
     */
    public void setSSLEngine(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
    }
    
    /**
     * Returns <code>SSLConnectorHandler</code>'s secured input buffer, it
     * uses for reading data from a socket channel.
     * @return secured input <code>ByteBuffer</code>
     */
    public ByteBuffer getSecuredInputBuffer() {
        return securedInputBuffer;
    }
    
    /**
     * Returns <code>SSLConnectorHandler</code>'s secured output buffer, it
     * uses for writing data to a socket channel.
     * @return secured output <code>ByteBuffer</code>
     */
    public ByteBuffer getSecuredOutputBuffer() {
        return securedOutputBuffer;
    }
    
    public SelectableChannel getUnderlyingChannel() {
        return socketChannel;
    }
    
    public SSLCallbackHandler getCallbackHandler() {
        return callbackHandler;
    }
    
    public void setCallbackHandler(SSLCallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
    
    public SSLSelectorHandler getSelectorHandler() {
        return selectorHandler;
    }
    
    /**
     * Gets the size of the largest application buffer that may occur when
     * using this session.
     * SSLEngine application data buffers must be large enough to hold the
     * application data from any inbound network application data packet
     * received. Typically, outbound application data buffers can be of any size.
     *
     * (javadoc is taken from SSLSession.getApplicationBufferSize())
     * @return largets application buffer size, which may occur
     */
    public int getApplicationBufferSize() {
        initSSLEngineIfRequired();
        return sslEngine.getSession().getApplicationBufferSize();
    }
    
    public void onConnect(IOEvent ioEvent) {
        callbackHandler.onConnect(ioEvent);
    }
    
    public void onRead(IOEvent ioEvent) {
        try {
            // if processing handshake - pass the data to handshake related code
            if (isProcessingAsyncHandshake) {
                doAsyncHandshake(asyncHandshakeBuffer);
                if (isHandshakeDone()) {
                    callbackHandler.onHandshake(ioEvent);
                }
                
                return;
            }
            
            callbackHandler.onRead(ioEvent);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception occured when reading from SSL channel.", e);
        }
    }
    
    public void onWrite(IOEvent ioEvent) {
        try {
            // check if all the secured data was written, if not -
            // flush as much as possible.
            if (!securedOutputBuffer.hasRemaining() || flushSecuredOutputBuffer()) {
                // if no encrypted data left in buffer - continue processing
                if (isProcessingAsyncHandshake) {
                    doAsyncHandshake(asyncHandshakeBuffer);
                    if (isHandshakeDone()) {
                        callbackHandler.onHandshake(ioEvent);
                    }
                    
                    return;
                }
                
                callbackHandler.onWrite(ioEvent);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception occured when writing to SSL channel.", e);
        }
    }
    
    /**
     * Read a data from channel in async mode and decrypt
     * @param byteBuffer buffer for decrypted data
     * @return number of bytes read from a channel
     * @throws java.io.IOException
     */
    private int doReadAsync(ByteBuffer byteBuffer) throws IOException {
        // Clear or compact secured input buffer
        clearOrCompactBuffer(securedInputBuffer);
        
        // Read data to secured buffer
        int bytesRead = socketChannel.read(securedInputBuffer);
        
        if (bytesRead == -1) {
            try {
                sslEngine.closeInbound();
                // check if there is some secured data still available
                if (securedInputBuffer.position() == 0 ||
                        sslEngineStatus == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                    return -1;
                }
            } catch (SSLException e) {
                return -1;
            }
        }
        
        securedInputBuffer.flip();
        
        if (bytesRead == 0 && !securedInputBuffer.hasRemaining()) {
            return 0;
        }
        
        SSLEngineResult result = null;
        do {
            result = sslEngine.unwrap(securedInputBuffer, byteBuffer);
            // During handshake phase several unwrap actions could be executed on read data
        } while (result.getStatus() == SSLEngineResult.Status.OK && 
                result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP && 
                result.bytesProduced() == 0);
        
        updateSSLEngineStatus(result);
        
        if (sslEngineStatus == SSLEngineResult.Status.CLOSED) {
            return -1;
        } else if (sslEngineStatus == SSLEngineResult.Status.BUFFER_OVERFLOW) {
            throw new IllegalStateException("Application buffer is overflowed");
        }
        
        return bytesRead;
    }
    
    /**
     * Write secured data to channel in async mode
     *
     * @param byteBuffer non-crypted data buffer
     * @return number of bytes written on a channel.
     * Be careful, as non-crypted data is passed, but crypted data is written
     * on channel. Don't use return value to determine,
     * number of bytes from original buffer, which were written.
     * @throws java.io.IOException
     */
    private int doWriteAsync(ByteBuffer byteBuffer) throws IOException {
        if (securedOutputBuffer.hasRemaining() && !flushSecuredOutputBuffer()) {
            return 0;
        }
        
        securedOutputBuffer.clear();
        SSLEngineResult result = SSLUtils.wrap(byteBuffer, securedOutputBuffer, sslEngine);
        
        updateSSLEngineStatus(result);
        
        return socketChannel.write(securedOutputBuffer);
        
    }
    
    /**
     * Perform an SSL handshake in async mode.
     * @param byteBuffer The application <code>ByteBuffer</code>
     *
     * @throws IOException if the handshake fail.
     */
    private void doAsyncHandshake(ByteBuffer byteBuffer) throws IOException {
        SSLEngineResult result;
        isProcessingAsyncHandshake = true;
        asyncHandshakeBuffer = byteBuffer;
        while (handshakeStatus != HandshakeStatus.FINISHED) {
            switch (handshakeStatus) {
                case NEED_WRAP:
                    result = SSLUtils.wrap(EMPTY_BUFFER, securedOutputBuffer, sslEngine);
                    updateSSLEngineStatus(result);
                    switch (result.getStatus()) {
                        case OK:
                            if (!flushSecuredOutputBuffer()) {
                                return;
                            }
                            break;
                        default:
                            throw new IOException("Handshaking error: " + result.getStatus());
                    }
                    
                    if (handshakeStatus != HandshakeStatus.NEED_UNWRAP) {
                        break;
                    }
                case NEED_UNWRAP:
                    int bytesRead = doReadAsync(byteBuffer);
                    if (bytesRead == -1) {
                        try {
                            sslEngine.closeInbound();
                        } catch (IOException e) {
                            logger.log(Level.FINE, "Exception occured when closing sslEngine inbound.", e);
                        }
                        
                        throw new EOFException("Connection closed");
                    } else if (bytesRead == 0 && sslLastOperationResult.bytesConsumed() == 0) {
                        registerSelectionKeyFor(SelectionKey.OP_READ);
                        return;
                    }
                    
                    if (handshakeStatus != HandshakeStatus.NEED_TASK) {
                        break;
                    }
                case NEED_TASK:
                    handshakeStatus = executeDelegatedTask();
                    break;
                default:
                    throw new RuntimeException("Invalid Handshaking State" + handshakeStatus);
            }
        }
        
        if (isProcessingAsyncHandshake) {
            finishHandshake();
        }
        
        asyncHandshakeBuffer = null;
    }
    
    /**
     * Complete hanshakes operations.
     * @return SSLEngineResult.HandshakeStatus
     */
    private SSLEngineResult.HandshakeStatus executeDelegatedTask() {
        Runnable runnable;
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            runnable.run();
        }
        
        return sslEngine.getHandshakeStatus();
    }
    
    /**
     * Update <code>SSLConnectorHandler</code> internal status with
     * last <code>SSLEngine</code> operation result.
     *
     * @param result last <code>SSLEngine</code> operation result
     */
    private void updateSSLEngineStatus(SSLEngineResult result) {
        sslLastOperationResult = result;
        sslEngineStatus = result.getStatus();
        handshakeStatus = result.getHandshakeStatus();
    }
    
    /**
     * Clears buffer if there is no info available, or compact buffer otherwise.
     * @param buffer byte byffer
     */
    private static void clearOrCompactBuffer(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            buffer.clear();
        } else if (buffer.remaining() < buffer.capacity()) {
            buffer.compact();
        }
    }
    
    /**
     * Gets <code>SSLConnectorHandler</code> <code>SelectionKey</code>
     * @return <code>SelectionKey</code>
     */
    private SelectionKey getSelectionKey() {
        return socketChannel.keyFor(selectorHandler.getSelector());
    }
    
    /**
     * Registers <code>SSLConnectorHandler<code>'s <code>SelectionKey</code>
     * to listen channel operations.
     * @param ops interested channel operations
     */
    private void registerSelectionKeyFor(int ops) {
        SelectionKey key = getSelectionKey();
        key.attach(this);
        selectorHandler.register(key, ops);
    }
    
    /**
     * Flushes as much as possible bytes from the secured output buffer
     * @return true if secured buffer was completely flushed, false otherwise
     */
    private boolean flushSecuredOutputBuffer() throws IOException {
        int nWrite = 1;
        
        while (nWrite > 0 && securedOutputBuffer.hasRemaining()) {
            nWrite = socketChannel.write(securedOutputBuffer);
        }
        
        if (securedOutputBuffer.hasRemaining()) {
            SelectionKey key = socketChannel.keyFor(selectorHandler.getSelector());
            key.attach(callbackHandler);
            selectorHandler.register(key, SelectionKey.OP_WRITE);
            
            return false;
        }
        
        return true;
    }
    
    /**
     * Initiate <code>SSLEngine</code> and related secure buffers
     */
    private void initSSLEngineIfRequired() {
        if (sslEngine == null) {
            sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(true);
        }
            
        int bbSize = sslEngine.getSession().getPacketBufferSize();
        securedInputBuffer = ByteBuffer.allocate(bbSize * 2);
        securedOutputBuffer = ByteBuffer.allocate(bbSize * 2);
        securedOutputBuffer.limit(0);
    }
}
