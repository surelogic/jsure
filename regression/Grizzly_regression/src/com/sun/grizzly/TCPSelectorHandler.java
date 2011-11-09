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
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A SelectorHandler handles all java.nio.channels.Selector operations.
 * One or more instance of a Selector are handled by SelectorHandler.
 * The logic for processing of SelectionKey interest (OP_ACCEPT,OP_READ, etc.)
 * is usually defined using an instance of SelectorHandler.
 *
 * This class represents a TCP implementation of a SelectorHandler.
 * This class first bind a ServerSocketChannel to a TCP port and then start
 * waiting for NIO events.
 *
 * @author Jeanfrancois Arcand
 */
public class TCPSelectorHandler implements SelectorHandler {
    
    
    /**
     * The ConnectorInstanceHandler used to return a new or pooled
     * ConnectorHandler
     */
    protected ConnectorInstanceHandler connectorInstanceHandler;
    
    
    /**
     * The list of SelectionKey to register next time the Selector.select is
     * invoked.
     */
    protected ConcurrentLinkedQueue<SelectionKey> opReadToRegister;
    
    
    /**
     * The list of SelectionKey to register next time the Selector.select is
     * invoked.
     */
    protected ConcurrentHashMap<SocketAddress[],CallbackHandler>
            opConnectToRegister;
    
    
    /**
     * The list of SelectionKey to register next time the Selector.select is
     * invoked.
     */
    protected ConcurrentLinkedQueue<SelectionKey> opWriteToRegister;
    
    
    /**
     * The socket tcpDelay.
     * 
     * Default value for tcpNoDelay is disabled (set to true).
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
     * The socket time out
     */
    protected int socketTimeout = -1;
    
    
    protected Logger logger;
    
    
    /**
     * The server socket time out
     */
    protected int serverTimeout = 0;
    
    
    /**
     * The inet address to use when binding.
     */
    protected InetAddress inet;
    
    
    /**
     * The default TCP port.
     */
    protected int port = 18888;
    
    
    /**
     * The ServerSocket instance.
     */
    protected ServerSocket serverSocket;
    
    
    /**
     * The ServerSocketChannel.
     */
    protected ServerSocketChannel serverSocketChannel;
    
    
    /**
     * The single Selector.
     */
    protected Selector selector;
    
    
    /**
     * The Selector time out.
     */
    protected long selectTimeout = 1000L;
    
    
    /**
     * Server socket backlog.
     */
    protected int ssBackLog = 4096;
    
    
    /**
     * Is this used for client only or client/server operation.
     */
    protected boolean isClient = false;
    
    
    /**
     * The SelectionKeyHandler associated with this SelectorHandler.
     */
    protected SelectionKeyHandler selectionKeyHandler;
    
    
    /**
     * The ProtocolChainInstanceHandler used by this instance. If not set, and instance
     * of the DefaultInstanceHandler will be created.
     */
    protected ProtocolChainInstanceHandler instanceHandler;
    
    
    public TCPSelectorHandler(){
    }
    
    
    public TCPSelectorHandler(boolean isClient) {
        this.isClient = isClient;
    }
    
    
    public void copyTo(Copyable copy) {
        TCPSelectorHandler copyHandler = (TCPSelectorHandler) copy;
        copyHandler.selector = selector;
        copyHandler.selectionKeyHandler = selectionKeyHandler;
        copyHandler.selectTimeout = selectTimeout;
        copyHandler.serverTimeout = serverTimeout;
        copyHandler.inet = inet;
        copyHandler.port = port;
        copyHandler.ssBackLog = ssBackLog;
        copyHandler.tcpNoDelay = tcpNoDelay;
        copyHandler.linger = linger;
        copyHandler.socketTimeout = socketTimeout;
        copyHandler.logger = logger;
        copyHandler.reuseAddress = reuseAddress;
        copyHandler.connectorInstanceHandler = connectorInstanceHandler;
    }
    
    
    /**
     * Return the set of SelectionKey registered on this Selector.
     */
    public Set<SelectionKey> keys(){
        if (selector != null){
            return selector.keys();
        } else {
            throw new IllegalStateException("Selector is not created!");
        }
    }
    
    
    /**
     * Is the Selector open.
     */
    public boolean isOpen(){
        if (selector != null){
            return selector.isOpen();
        } else {
            return false;
        }
    }
    
    
    /**
     * Before invoking Selector.select(), make sure the ServerScoketChannel
     * has been created. If true, then register all SelectionKey to the Selector.
     * @param ctx <code>Context</code>
     */
    public void preSelect(Context ctx) throws IOException {
        initOpRegistriesIfRequired();
                
        if (selector == null){
            try{
                connectorInstanceHandler = new ConnectorInstanceHandler.
                        ConcurrentQueueDelegateCIH(
                        getConnectorInstanceHandlerDelegate());
                
                // Create the socket listener
                selector = Selector.open();
                
                if (!isClient){
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocket = serverSocketChannel.socket();
                    serverSocket.setReuseAddress(reuseAddress);
                    if ( inet == null)
                        serverSocket.bind(new InetSocketAddress(port),ssBackLog);
                    else
                        serverSocket.bind(new InetSocketAddress(inet,port),ssBackLog);
                    
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                }
            } catch (SocketException ex){
                throw new BindException(ex.getMessage() + ": " + port + "=" + this);
            }
            
            if (!isClient){
                serverSocket.setSoTimeout(serverTimeout);
            }
        } else {
            onReadOps();
            onWriteOps();
            onConnectOps(ctx);
        }
    }
    
    
    /**
     * Handle new OP_READ ops.
     */
    protected void onReadOps(){
        if (!opReadToRegister.isEmpty()){
            selectionKeyHandler.register(opReadToRegister.iterator(),
                    SelectionKey.OP_READ);
        }
    }
    
    
    /**
     * Handle new OP_WRITE ops.
     */
    protected void onWriteOps(){
        if (!opWriteToRegister.isEmpty()){
            selectionKeyHandler.register(opWriteToRegister.iterator(),
                    SelectionKey.OP_WRITE);
        }
    }
    
    
    /**
     * Handle new OP_CONNECT ops.
     */
    protected void onConnectOps(Context ctx) throws IOException{
        if (!opConnectToRegister.isEmpty()){
            CallbackHandler<SocketChannel> callbackHandler;
            SocketChannel socketChannel;
            Iterator<SocketAddress[]> iterator =
                    opConnectToRegister.keySet().iterator();
            SocketAddress[] remoteLocal;
            SelectionKey key;
            SocketChannel channel;
            while(iterator.hasNext()){
                remoteLocal = iterator.next();
                socketChannel= SocketChannel.open();
                socketChannel.socket().setReuseAddress(true);
                if (remoteLocal[1] != null){
                    socketChannel.socket().bind(remoteLocal[1]);
                }
                socketChannel.configureBlocking(false);
                boolean isConnected = socketChannel.connect(remoteLocal[0]);
                key = socketChannel.register(selector,
                        SelectionKey.OP_CONNECT,opConnectToRegister.remove(remoteLocal));
                
                // if channel was connected immediately
                if (isConnected) {
                    onConnectInterest(key, ctx);
                }
            }
        }
    }
    
    
    /**
     * Execute the Selector.select(...) operations.
     * @param ctx <code>Context</code>
     * @return <code>Set</code> of <code>Context</code>
     */
    public Set<SelectionKey> select(Context ctx) throws IOException{        
        selector.select(selectTimeout);
        return selector.selectedKeys();
    }
    
    
    /**
     * Invoked after Selector.select().
     * @param ctx <code>Context</code>
     */
    public void postSelect(Context ctx) {
        Set<SelectionKey> readyKeys = keys();
        if (readyKeys.isEmpty()){
            return;
        }

        if (isOpen()) {
            selectionKeyHandler.expire(readyKeys.iterator());
        }            
    }
    
    
    /**
     * Register a SelectionKey to this Selector.
     */
    public void register(SelectionKey key, int ops) {
        if (ops == SelectionKey.OP_READ){
            opReadToRegister.offer(key);
        } else if (ops == SelectionKey.OP_WRITE){
            opWriteToRegister.offer(key);
        } else if (ops == (SelectionKey.OP_WRITE|SelectionKey.OP_READ)){
            opReadToRegister.offer(key);
            opWriteToRegister.offer(key);
        }
        selector.wakeup();
    }
    
    
    /**
     * Register a CallBackHandler to this Selector.
     *
     * @param remoteAddress remote address to connect
     * @param localAddress local address to bin
     * @param callBackHandler <code>CallbackHandler</code>
     * @throws java.io.IOException
     */
    protected void connect(SocketAddress remoteAddress, SocketAddress localAddress,
            CallbackHandler callBackHandler) throws IOException{
        opConnectToRegister.put(new SocketAddress[]{remoteAddress,localAddress},
                callBackHandler);
        selector.wakeup();
    }
    
    
    /**
     * Shuntdown this instance by closing its Selector and associated channels.
     */
    public void shutdown(){       
        if (selector != null){
            for (SelectionKey selectionKey : selector.keys()) {                
                selectionKeyHandler.close(selectionKey);
            } 
        }
        
        try{
            if (serverSocket != null)
                serverSocket.close();
        } catch (Throwable ex){
            Controller.logger().log(Level.SEVERE,
                    "serverSocket.close",ex);
        }
        
        try{
            if (serverSocketChannel != null)
                serverSocketChannel.close();
        } catch (Throwable ex){
            Controller.logger().log(Level.SEVERE,
                    "serverSocketChannel.close",ex);
        }
        
        try{
            if (selector != null)
                selector.close();
        } catch (Throwable ex){
            Controller.logger().log(Level.SEVERE,
                    "selector.close",ex);
        }
        
    }
    
    /**
     * {@inheritDoc}
     */
    public SelectableChannel acceptWithoutRegistration(SelectionKey key)
    throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel channel = server.accept();
        return channel;
    }
    
    /**
     * Handle OP_ACCEPT.
     * @param ctx <code>Context</code>
     * @return always returns false
     */
    public boolean onAcceptInterest(SelectionKey key,
            Context ctx) throws IOException{
        SelectableChannel channel = acceptWithoutRegistration(key);
        
        if (channel != null) {
            configureChannel(channel);
            SelectionKey readKey =
                    channel.register(selector, SelectionKey.OP_READ);
            readKey.attach(System.currentTimeMillis());
        }
        return false;
    }
    
    /**
     * Handle OP_READ.
     * @param ctx <code>Context</code>
     * @param key <code>SelectionKey</code>
     * @return false if handled by a <code>CallbackHandler</code>, otherwise true
     */
    public boolean onReadInterest(final SelectionKey key,final Context ctx)
    throws IOException{
        // disable OP_READ on key before doing anything else
        key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
        if (key.attachment() instanceof CallbackHandler){
            final Context context = ctx.getController().pollContext(key);
            context.setCurrentOpType(Context.OpType.OP_READ);
            invokeCallbackHandler(context);
            return false;
        } else {
            return true;
        }
    }
    
    
    /**
     * Handle OP_WRITE.
     *
     * @param key <code>SelectionKey</code>
     * @param ctx <code>Context</code>
     */
    public boolean onWriteInterest(final SelectionKey key,final Context ctx)
    throws IOException{
        // disable OP_WRITE on key before doing anything else
        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
        if (key.attachment() instanceof CallbackHandler){
            final Context context = ctx.getController().pollContext(key);
            context.setSelectionKey(key);
            context.setCurrentOpType(Context.OpType.OP_WRITE);
            invokeCallbackHandler(context);
            return false;
        } else {
            return true;
        }
    }
    
    
    /**
     * Handle OP_CONNECT.
     * @param key <code>SelectionKey</code>
     * @param ctx <code>Context</code>
     */
    public boolean onConnectInterest(final SelectionKey key, Context ctx)
    throws IOException{
        // disable OP_CONNECT on key before doing anything else
        key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));
        
        // No OP_READ nor OP_WRITE allowed yet.
        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
        key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
        
        if (key.attachment() instanceof CallbackHandler){
            Context context = ctx.getController().pollContext(key);
            context.setSelectionKey(key);
            context.setCurrentOpType(Context.OpType.OP_CONNECT);
            invokeCallbackHandler(context);
        }
        return false;
    }
    
    
    /**
     * Invoke a CallbackHandler via a Context instance.
     * @param context <code>Context</code>
     * @throws java.io.IOException
     */
    protected void invokeCallbackHandler(Context context) throws IOException{
        context.setSelectorHandler(this);
        
        IOEvent<Context>ioEvent = new IOEvent.DefaultIOEvent<Context>(context);
        context.setIOEvent(ioEvent);
        try {
            context.execute();
        } catch (PipelineFullException ex){
            throw new IOException(ex.getMessage());
        }
    }
    
    
    /**
     * Return an instance of the default <code>ConnectorHandler</code>,
     * which is the <code>TCPConnectorHandler</code>
     * @return <code>ConnectorHandler</code>
     */
    public ConnectorHandler acquireConnectorHandler(){
        if (selector == null || !selector.isOpen()){
            throw new IllegalStateException("SelectorHandler not yet started");
        }
        
        ConnectorHandler connectorHandler = connectorInstanceHandler.acquire();
        return connectorHandler;
    }
    
    
    /**
     * Release a ConnectorHandler.
     */
    public void releaseConnectorHandler(ConnectorHandler connectorHandler){
        connectorInstanceHandler.release(connectorHandler);
    }
    
    
    /**
     * A token decribing the protocol supported by an implementation of this
     * interface
     */
    public Controller.Protocol protocol(){
        return Controller.Protocol.TCP;
    }
    // ------------------------------------------------------ Utils ----------//
    
    
    /**
     * Initializes <code>SelectionKey</code> operation registries
     */
    protected void initOpRegistriesIfRequired() {
        if (opReadToRegister == null){
            opReadToRegister = new ConcurrentLinkedQueue<SelectionKey>();
        }
        
        if (opWriteToRegister== null){
            opWriteToRegister = new ConcurrentLinkedQueue<SelectionKey>();
        }
        
        if (opConnectToRegister== null){
            opConnectToRegister = 
                    new ConcurrentHashMap<SocketAddress[],CallbackHandler>();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void configureChannel(SelectableChannel channel) throws IOException{
        Socket socket = ((SocketChannel) channel).socket();
        
        channel.configureBlocking(false);
        
        try{
            if(linger >= 0 ) {
                socket.setSoLinger( true, linger);
            }
        } catch (SocketException ex){
            logger.log(Level.WARNING,
                    "setSoLinger exception ",ex);
        }
        
        try{
            socket.setTcpNoDelay(tcpNoDelay);
        } catch (SocketException ex){
            logger.log(Level.WARNING,
                    "setTcpNoDelay exception ",ex);
        }
        
        try{
            socket.setReuseAddress(reuseAddress);
        } catch (SocketException ex){
            logger.log(Level.WARNING,
                    "setReuseAddress exception ",ex);
        }
    }
    
    
    // ------------------------------------------------------ Properties -----//
    
    public final Selector getSelector() {
        return selector;
    }
    
    public final void setSelector(Selector selector) {
        this.selector = selector;
    }
    
    public long getSelectTimeout() {
        return selectTimeout;
    }
    
    public void setSelectTimeout(long selectTimeout) {
        this.selectTimeout = selectTimeout;
    }
    
    public int getServerTimeout() {
        return serverTimeout;
    }
    
    public void setServerTimeout(int serverTimeout) {
        this.serverTimeout = serverTimeout;
    }
    
    public InetAddress getInet() {
        return inet;
    }
    
    public void setInet(InetAddress inet) {
        this.inet = inet;
    }
    
    /**
     * Returns port number <code>SelectorHandler</code> is listening on
     * Similar to <code>getPort()</code>, but getting port number directly from
     * connection (<code>ServerSocket</code>, <code>DatagramSocket</code>).
     * So if default port number 0 was set during initialization, then <code>getPort()</code>
     * will return 0, but getPortLowLevel() will
     * return port number assigned by OS.
     *
     * @return port number or -1 if <code>SelectorHandler</code> was not initialized for accepting connections.
     */
    public int getPortLowLevel() {
        if (serverSocket != null) {
            return serverSocket.getLocalPort();
        }
        
        return -1;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public int getSsBackLog() {
        return ssBackLog;
    }
    
    public void setSsBackLog(int ssBackLog) {
        this.ssBackLog = ssBackLog;
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
    
    public int getSocketTimeout() {
        return socketTimeout;
    }
    
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public boolean isReuseAddress() {
        return reuseAddress;
    }
    
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }
    
    /**
     * Return the Pipeline used to execute this SelectorHandler's
     * SelectionKey ops
     * @return The pipeline to use, or null if the Controller's Pipeline
     * should be used.
     */
    public Pipeline pipeline(){
        return null;
    }
    
    
    /**
     * Get the SelectionKeyHandler associated with this SelectorHandler.
     */
    public SelectionKeyHandler getSelectionKeyHandler() {
        return selectionKeyHandler;
    }
    
    
    /**
     * Set SelectionKeyHandler associated with this SelectorHandler.
     */
    public void setSelectionKeyHandler(SelectionKeyHandler selectionKeyHandler) {
        this.selectionKeyHandler = selectionKeyHandler;
    }
    
    
    /**
     * Set the <code>ProtocolChainInstanceHandler</code> to use for
     * creating instance of <code>ProtocolChain</code>.
     */
    public void setProtocolChainInstanceHandler(ProtocolChainInstanceHandler
            instanceHandler){
        this.instanceHandler = instanceHandler;
    }
    
    
    /**
     * Return the <code>ProtocolChainInstanceHandler</code>
     */
    public ProtocolChainInstanceHandler getProtocolChainInstanceHandler(){
        return instanceHandler;
    }
    
    /**
     * {@inheritDoc}
     */
    public void closeChannel(SelectableChannel channel) {
        // channel could be either SocketChannel or ServerSocketChannel
        if (channel instanceof SocketChannel) {
            Socket socket = ((SocketChannel) channel).socket();
            
            try {
                if (!socket.isInputShutdown()) socket.shutdownInput();
            } catch (IOException ex){
                ;
            }
            
            try {
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

    //--------------- ConnectorInstanceHandler -----------------------------
    /**
     * Return <Callable>factory<Callable> object, which knows how
     * to create <code>ConnectorInstanceHandler<code> corresponding to the protocol
     * @return <Callable>factory</code>
     */
    protected Callable<ConnectorHandler> getConnectorInstanceHandlerDelegate() {
        return new Callable<ConnectorHandler>() {
            public ConnectorHandler call() throws Exception {
                return new TCPConnectorHandler();
            }
        };
    }    
}
