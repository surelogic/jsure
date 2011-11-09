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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.grizzly.util.Cloner;
import com.sun.grizzly.util.Copyable;
import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.RequiresLock;

/**
 * <p>
 * Main entry point when using the Grizzly Framework. A Controller is composed
 * of Handlers, ProtocolChain and Pipeline. All of those components are
 * configurable by client using the Grizzly Framework.
 * </p>
 *
 * <p>
 * A Pipeline is a wrapper around a Thread pool.
 * </p>
 * <p>
 * A ProtocolChain implement the "Chain of Responsibility" pattern (for more info,
 * take a look at the classic "Gang of Four" design patterns book). Towards
 * that end, the Chain API models a computation as a series of "protocol filter"
 * that can be combined into a "protocol chain".
 * </p>
 * <p>
 * An Handler is a interface that can be implemented
 * by implemented by client of the Grizzly Framework to used to help handling
 * NIO operations. The Grizzly Framework define three Handlers:
 * </p>
 * <p><pre><code>
 * (1) SelectorHandler: A SelectorHandler handles all java.nio.channels.Selector
 *                     operations. One or more instance of a Selector are
 *                     handled by SelectorHandler. The logic for processing of
 *                     SelectionKey interest (OP_ACCEPT,OP_READ, etc.) is usually
 *                     defined using an instance of SelectorHandler.
 * (2) SelectionKeyHandler: A SelectionKeyHandler is used to handle the life
 *                          life cycle of a SelectionKey. Operations like cancelling,
 *                          registering or closing are handled by SelectionKeyHandler.
 * (3) ProtocolChainInstanceHandler: An ProtocolChainInstanceHandler is where one or several ProtocolChain
 *                      are created and cached. An ProtocolChainInstanceHandler decide if
 *                      a stateless or statefull ProtocolChain needs to be created.
 * </code></pre></p>
 * <p>
 * By default, the Grizzly Framework bundle default implementation for TCP
 * and UPD transport. The TCPSelectorHandler is instanciated by default. As an
 * example, supporting the HTTP protocol should only consist of adding the
 * appropriate ProtocolFilter like:
 * </p>
 * <p><pre><code>
 *       Controller sel = new Controller();
 *       sel.setProtocolChainInstanceHandler(new DefaultProtocolChainInstanceHandler(){
 *           public ProtocolChain poll() {
 *               ProtocolChain protocolChain = protocolChains.poll();
 *               if (protocolChain == null){
 *                   protocolChain = new DefaultProtocolChain();
 *                   protocolChain.addFilter(new ReadFilter());
 *                   protocolChain.addFilter(new HTTPParserFilter());
 *               }
 *               return protocolChain;
 *           }
 *       });
 *
 * </code></pre></p>
 * <p>
 * In the example above, a pool of ProtocolChain will be created, and all instance
 * of ProtocolChain will have their instance of ProtocolFilter. Hence the above
 * implementation can be called statefull. A stateless implementation would
 * instead consist of sharing the ProtocolFilter amongs ProtocolChain:
 * </p>
 * <p><pre><code>
 *       final Controller sel = new Controller();
 *       final ReadFilter readFilter = new ReadFilter();
 *       final LogFilter logFilter = new LogFilter();
 *
 *       sel.setProtocolChainInstanceHandler(new DefaultProtocolChainInstanceHandler(){
 *           public ProtocolChain poll() {
 *               ProtocolChain protocolChain = protocolChains.poll();
 *               if (protocolChain == null){
 *                   protocolChain = new DefaultProtocolChain();
 *                   protocolChain.addFilter(readFilter);
 *                   protocolChain.addFilter(logFilter);
 *               }
 *               return protocolChain;
 *           }
 *       });
 * </code></pre></p>
 * @author Jeanfrancois Arcand
 * 
 */
@PolicyLocks({@PolicyLock("SDLock is shutdownLock"),
  @PolicyLock("StateLock is stateLock") })
public class Controller implements Runnable, Lifecycle, Copyable, ConnectorHandlerPool {
    
    public enum Protocol { UDP, TCP , TLS, CUSTOM }
    
    
    /**
     * A cached list of Context. Context are by default stateless.
     */
    private ConcurrentLinkedQueue<Context> contexts;
    
    
    /**
     * The ProtocolChainInstanceHandler used by this instance. If not set, and instance
     * of the DefaultInstanceHandler will be created.
     */
    protected ProtocolChainInstanceHandler instanceHandler;
    
    
    /**
     * The SelectionKey Handler used by this instance. If not set, and instance
     * of the DefaultSelectionKeyHandler will be created.
     */
    protected SelectionKeyHandler selectionKeyHandler;
    
    
    /**
     * The SelectorHandler, which will manage connection accept,
     * if readThreadsCount > 0 and spread connection processing between
     * different read threads
     */
    protected ComplexSelectorHandler multiReadThreadSelectorHandler = null;
    
    
    /**
     * The ConnectorHandlerPool, which is responsible for creating/caching
     * ConnectorHandler instances.
     */
    protected ConnectorHandlerPool connectorHandlerPool = null;
    
    
    /**
     * The set of <code>SelectorHandler</code>s used by this instance. If not set, the instance
     * of the TCPSelectorHandler will be added by default.
     */
    protected ConcurrentLinkedQueue<SelectorHandler> selectorHandlers;
    
    
    /**
     * Controller state enum
     * STOPPED - Controller is in a stopped, not running state
     * STARTED - Controller is in a started, running state
     *
     */
    protected enum State { STOPPED, STARTED }
    
    
    /**
     * Current Controller state
     */
    protected volatile State state;
    
    
    /**
     * State lock to have consistent state value
     */
    protected final ReentrantLock stateLock;
    
    
    /**
     * The number of read threads
     */
    protected int readThreadsCount = 0;
    
    
    /**
     * The array of <code>Controller</code>s to be used for reading
     */
    protected ReadController[] readThreadControllers;
    
    
    /**
     * Default Logger.
     */
    private static Logger logger = Logger.getLogger("grizzly");
    
    
    /**
     * Default Thread Pool (called Pipeline).If not set, and instance
     * of the DefaultPipeline will be created.
     */
    private Pipeline<Callable> pipeline;
    
    
    /**
     * Shutdown lock.
     */
    protected final Object shutdownLock = new Object();
    
    
    /**
     * Collection of <code>Controller</code> state listeners, which
     * will are notified on <code>Controller</code> state change.
     */
    protected Collection<ControllerStateListener> stateListeners = 
            new LinkedList<ControllerStateListener>();
    
    
    /**
     * <tt>true</tt> if OP_ERAD and OP_WRITE can be handled concurrently.
     */
    private boolean handleReadWriteConcurrently = true;
    
    
    /**
     * Controller constructor
     */
    public Controller() {
        contexts = new ConcurrentLinkedQueue<Context>();
        stateLock = new ReentrantLock();
    }
    
    /*
     * XXX
     * Added by Ethan to allow the stateLock field to be finalized
     */
    protected Controller(Controller copyFrom){
    	contexts = copyFrom.contexts;
    	stateLock = copyFrom.stateLock;
        instanceHandler = copyFrom.instanceHandler;
        pipeline = copyFrom.pipeline;
        readThreadControllers = copyFrom.readThreadControllers;
        readThreadsCount = copyFrom.readThreadsCount;
        selectionKeyHandler = copyFrom.selectionKeyHandler;
        state = copyFrom.state;
    }
    
    /**
     * This method handle the processing of all Selector's interest op
     * (OP_ACCEPT,OP_READ,OP_WRITE,OP_CONNECT) by delegating to its Handler.
     * By default, all java.nio.channels.Selector operations are implemented
     * using SelectorHandler. All SelectionKey operations are implemented by
     * SelectionKeyHandler. Finally, ProtocolChain creation/re-use are implemented
     * by InstanceHandler.
     * @param selectorHandler - the <code>SelectorHandler</code>
     */
//    @RequiresLock("SDLock")
    protected void doSelect(SelectorHandler selectorHandler){
        SelectionKey key = null;
        Set<SelectionKey> readyKeys;
        Iterator<SelectionKey> iterator;
        int selectorState;
        boolean delegateToWorkerThread = false;
        ProtocolChainInstanceHandler pciHandler = null;
        ProtocolChain protocolChain = null;
        Context ctx = null;
        Context serverCtx = contexts.poll();
        if (serverCtx == null){
            serverCtx = new Context();
            serverCtx.setController(this);
        }
        
        try{
            selectorState = 0;
            
            // Set the SelectionKeyHandler only if the SelectorHandler doesn't
            // define one.
            if (selectorHandler.getSelectionKeyHandler() == null){
                selectorHandler.setSelectionKeyHandler(selectionKeyHandler);
            }
            
            selectorHandler.preSelect(serverCtx);
            
            readyKeys = selectorHandler.select(serverCtx);
            selectorState = readyKeys.size();
            
            if (state == State.STARTED && selectorState != 0) {
                iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    boolean skipOpWrite = false;
                    if (key.isValid()) {
                        if ((key.readyOps() & SelectionKey.OP_ACCEPT)
                                == SelectionKey.OP_ACCEPT){
                            if (readThreadsCount > 0 &&
                                    multiReadThreadSelectorHandler.supportsProtocol(selectorHandler.protocol())) {
                                delegateToWorkerThread = multiReadThreadSelectorHandler.
                                        onAcceptInterest(key, serverCtx, selectorHandler);
                            } else {
                                delegateToWorkerThread = selectorHandler.
                                        onAcceptInterest(key,serverCtx);
                            }
                            continue;
                        } 
                                                
                        if ((key.readyOps() & SelectionKey.OP_CONNECT)
                                == SelectionKey.OP_CONNECT) {
                            delegateToWorkerThread = selectorHandler.
                                    onConnectInterest(key,serverCtx);
                            continue;
                        }
                        
                        // OP_READ will always be processed first, then 
                        // based on the handleReadWriteConcurrently, the OP_WRITE
                        // might be processed just after or during the next
                        // Selector.select() invokation.
                        if ((key.readyOps() & SelectionKey.OP_READ)
                                == SelectionKey.OP_READ) {
                            delegateToWorkerThread = selectorHandler.
                                    onReadInterest(key,serverCtx);
                                                                                
                            if (!handleReadWriteConcurrently){
                                skipOpWrite = true;
                            }                        
                        } 

                        // The OP_READ processing might have closed the 
                        // Selection, hence we must make sure the
                        // SelectionKey is still valid.
                        if (!skipOpWrite && key.isValid() 
                                && (key.readyOps() & SelectionKey.OP_WRITE)
                                == SelectionKey.OP_WRITE) {
                            delegateToWorkerThread = selectorHandler.
                                    onWriteInterest(key,serverCtx);
                        } 

                        if (delegateToWorkerThread){
                            pciHandler = selectorHandler
                                    .getProtocolChainInstanceHandler();
                            protocolChain = (pciHandler != null ? 
                                pciHandler.poll(): instanceHandler.poll());
                            Context context = pollContext(key,protocolChain);
                            context.setSelectorHandler(selectorHandler);
                            context.setPipeline(selectorHandler.pipeline());
                            context.execute();                            
                        }
                    } else {
                        selectorHandler.getSelectionKeyHandler().cancel(key);
                    }
                }
            }
            
            delegateToWorkerThread = false;
            selectorHandler.postSelect(serverCtx);
            contexts.offer(serverCtx);
        } catch (ClosedSelectorException e) {
            // TODO: This could indicate that the Controller is
            //       shutting down. Hence, we need to handle this Exception
            //       appropriately. Perhaps check the state before logging
            //       what's happening ?
            stateLock.lock();
            try {
                if (state != State.STOPPED) {
                    logger.log(Level.SEVERE, "Selector was unexpectedly closed.");
                    notifyException(e);
                } else {
                    logger.log(Level.FINE, "doSelect Selector closed");
                }
            } finally {
                stateLock.unlock();
            }
        } catch (ClosedChannelException e) {
            // Don't use stateLock. This case is not strict
            if (state != State.STOPPED) {
                logger.log(Level.WARNING, "Channel was unexpectedly closed");
                if (key != null){
                    selectorHandler.getSelectionKeyHandler().cancel(key);
                }
                
                notifyException(e);
            }
        } catch (Throwable t) {
            if (key != null){
                selectorHandler.getSelectionKeyHandler().cancel(key);
            }
            
            notifyException(t);
            logger.log(Level.SEVERE,"doSelect exception",t);
        }
    }

    
    /**
     * Register a SelectionKey.
     * @param key <tt>SelectionKey</tt> to register
     */
    public void registerKey(SelectionKey key){
        registerKey(key,SelectionKey.OP_READ);
    }
    
    
    /**
     * Register a SelectionKey on the first SelectorHandler that was added
     * using the addSelectorHandler().
     * @param key <tt>SelectionKey</tt> to register
     * @param ops - the interest op to register
     */
    public void registerKey(SelectionKey key, int ops){
        registerKey(key, ops, selectorHandlers.peek().protocol());
    }
    
    
    /**
     * Register a SelectionKey.
     * @param key <tt>SelectionKey</tt> to register
     * @param ops - the interest op to register
     * @param protocol specified protocol SelectorHandler key should be registered on
     */
    public void registerKey(SelectionKey key, int ops, Protocol protocol){
        if (state == State.STOPPED) {
            return;
        }
        
        getSelectorHandler(protocol).register(key,ops);
    }
    
    
    /**
     * Cancel a SelectionKey
     * @param key <tt>SelectionKey</tt> to cancel
     * @deprecated
     */
    public void cancelKey(SelectionKey key){
        if (state == State.STOPPED) {
            return;
        }
        
        SelectorHandler selectorHandler = getSelectorHandler(key.selector());
        if (selectorHandler != null) {
            selectorHandler.getSelectionKeyHandler().cancel(key);
        } else {
            throw new IllegalStateException("SelectionKey is not associated " +
                    "with known SelectorHandler");
        }
    }
    
    
    /**
     * Get an instance of a <code>Context</code>
     * @param key <code>SelectionKey</code>
     * @return <code>Context</code>
     */
    public Context pollContext(SelectionKey key){
        return pollContext(key,instanceHandler.poll());
    }
    
    /**
     * Get an instance of a <code>Context</code>
     * @param key <code>SelectionKey</code>
     * @param protocolChain The ProtocolChain used to execute the 
     *                      returned Context.
     * @return <code>Context</code>
     */    
    protected Context pollContext(SelectionKey key, 
            ProtocolChain protocolChain){
        Context ctx = contexts.poll();
        if (ctx == null){
            ctx = new Context();
        }
        ctx.setController(this);
        ctx.setSelectionKey(key);
        ctx.setProtocolChain(protocolChain);
        return ctx;
    }
    
    /**
     * Return a Context to the pool
     * @param ctx - the <code>Context</code>
     */
    public void returnContext(Context ctx){
        contexts.offer(ctx);
    }
    
    
    /**
     * Return the current <code>Logger</code> used by this Controller.
     */
    public static Logger logger() {
        return logger;
    }
    
    
    /**
     * Set the Logger single instance to use.
     */
    public static void setLogger(Logger l){
        logger = l;
    }
    
    // ------------------------------------------------------ Handlers ------//
    
    
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
     * @deprecated
     * Set the <code>SelectionKeyHandler</code> to use for managing the life
     * cycle of SelectionKey.
     * Method is deprecated. Use SelectorHandler.setSelectionKeyHandler() instead
     */
    public void setSelectionKeyHandler(SelectionKeyHandler selectionKeyHandler){
        this.selectionKeyHandler = selectionKeyHandler;
    }
    
    
    /**
     * @deprecated
     * Return the <code>SelectionKeyHandler</code>
     * Method is deprecated. Use SelectorHandler.getSelectionKeyHandler() instead
     */
    public SelectionKeyHandler getSelectionKeyHandler(){
        return selectionKeyHandler;
    }
    
    
    /**
     * Add a <code>SelectorHandler</code>
     * @param selectorHandler - the <code>SelectorHandler</code>
     */
    public void addSelectorHandler(SelectorHandler selectorHandler){
        if (selectorHandlers == null){
            selectorHandlers = new ConcurrentLinkedQueue<SelectorHandler>();
        }
        selectorHandlers.add(selectorHandler);
    }
    
    
    /**
     * Set the first <code>SelectorHandler</code>
     * @param selectorHandler - the <code>SelectorHandler</code>
     */
    public void setSelectorHandler(SelectorHandler selectorHandler){
        addSelectorHandler(selectorHandler);
    }
    
    
    /**
     * Return the <code>SelectorHandler</code> associated with the protocol.
     * @param protocol - the <code>Protocol</code>
     * @return <code>SelectorHandler</code>
     */
    public SelectorHandler getSelectorHandler(Protocol protocol){
        for (SelectorHandler selectorHandler: selectorHandlers){
            if (selectorHandler.protocol() == protocol){
                return selectorHandler;
            }
        }
        return null;
    }
    
    /**
     * Return the <code>SelectorHandler</code> associated 
     * with the <code>Selector</code>.
     * @param selector - the <code>Selector</code>
     * @return <code>SelectorHandler</code>
     */
    public SelectorHandler getSelectorHandler(Selector selector){
        for (SelectorHandler selectorHandler: selectorHandlers){
            if (selectorHandler.getSelector() == selector){
                return selectorHandler;
            }
        }
        
        return null;
    }
    
    /**
     * Return the first <code>SelectorHandler</code>
     * @return <code>ConcurrentLinkedQueue</code>
     */
    public ConcurrentLinkedQueue getSelectorHandlers(){
        return selectorHandlers;
    }
    
    
    /**
     * Return the <code>Pipeline</code> (Thread Pool) used by this Controller.
     */
    public Pipeline getPipeline() {
        return pipeline;
    }
    
    
    /**
     * Set the <code>Pipeline</code> (Thread Pool).
     */
    public void setPipeline(Pipeline<Callable> pipeline) {
        this.pipeline = pipeline;
    }
    
    
    /**
     * Return the number of Reader threads count.
     */
    public int getReadThreadsCount() {
        return readThreadsCount;
    }
    
    
    /**
     * Set the number of Reader threads count.
     */
    public void setReadThreadsCount(int readThreadsCount) {
        this.readThreadsCount = readThreadsCount;
    }
    
    
    /**
     * Return the <code>ConnectorHandlerPool</code> used.
     */
    public ConnectorHandlerPool getConnectorHandlerPool() {
        return connectorHandlerPool;
    }
     
    
    /**
     * Set the <code>ConnectorHandlerPool</code> used.
     */
    public void setConnectorHandlerPool(ConnectorHandlerPool connectorHandlerPool) {
        this.connectorHandlerPool = connectorHandlerPool;
    }
    
    // ------------------------------------------------------ Runnable -------//
    
    
    /**
     * Execute this Controller.
     */
    public void run() {
        try{
            start();
        } catch(IOException e){
            notifyException(e);
            throw new RuntimeException(e.getCause());
        }
    }
    
    // -------------------------------------------------------- Copyable ----//
    
    
    /**
     * Copy this Controller state to another instance of a Controller.
     */
    public void copyTo(Copyable copy) {
    	// XXX Commented out by Ethan since the stateLock field is now final
    	/*
        Controller copyController = (Controller) copy;
        copyController.contexts = contexts;
        copyController.instanceHandler = instanceHandler;
        copyController.pipeline = pipeline;
        copyController.readThreadControllers = readThreadControllers;
        copyController.readThreadsCount = readThreadsCount;
        copyController.selectionKeyHandler = selectionKeyHandler;
        copyController.stateLock = stateLock;
        copyController.state = state;
        */
    }
    
    // -------------------------------------------------------- Lifecycle ----//
    
    /**
     * Add controller state listener
     */
    public void addStateListener(ControllerStateListener stateListener) {
        stateListeners.add(stateListener);
    }
    
    /**
     * Remove controller state listener
     */
    public void removeStateListener(ControllerStateListener stateListener) {
        stateListeners.remove(stateListener);
    }
    
    /**
     * Notify controller started
     */
    private void notifyStarted() {
        for(ControllerStateListener stateListener : stateListeners) {
            stateListener.onStarted();
        }
    }
    
    
    /**
     * Notify controller is ready
     */
    private void notifyReady() {
        for(ControllerStateListener stateListener : stateListeners) {
            stateListener.onReady();
        }
    }

    
    /**
     * Notify controller stopped
     */
    private void notifyStopped() {
        for(ControllerStateListener stateListener : stateListeners) {
            stateListener.onStopped();
        }
    }
    
    
    /**
     * Notify exception occured
     */
    private void notifyException(Throwable e) {
        for(ControllerStateListener stateListener : stateListeners) {
            stateListener.onException(e);
        }
    }
    
    
    /**
     * Start the Controller. If the Pipeline and/or Handler has not been
     * defined, the default will be used.
     */
    public void start() throws IOException {
        if (pipeline == null){
            pipeline = new DefaultPipeline();
        }
        
        if (instanceHandler == null){
            instanceHandler = new DefaultProtocolChainInstanceHandler();
        }
        
        // if selectorHandlers were not set by user explicitly - add TCPSelectorHandler by default
        if (selectorHandlers == null){
            selectorHandlers = new ConcurrentLinkedQueue<SelectorHandler>();
            selectorHandlers.add(new TCPSelectorHandler());
        }
        
        if (selectionKeyHandler == null){
            selectionKeyHandler = new DefaultSelectionKeyHandler();
        }
        
        if (connectorHandlerPool == null) {
            connectorHandlerPool = 
                    new DefaultConnectorHandlerPool(this);
        }
        
        if (readThreadsCount > 0) {
            initReadThreads();
            multiReadThreadSelectorHandler =
                    new RoundRobinSelectorHandler(readThreadControllers);
        }
        
        pipeline.initPipeline();
        pipeline.startPipeline();
        
        stateLock.lock();
        try {
            state = State.STARTED;
        } finally {
            stateLock.unlock();
        }
        notifyStarted();
        
        boolean firstTimeSelect = true;
        try {
            synchronized(shutdownLock){
                while(state == State.STARTED){
                    for(SelectorHandler selectorHandler: selectorHandlers) {
                        // State changed inside the loop
                        if (state != State.STARTED){
                            break;
                        }
                        doSelect(selectorHandler);
                    }
                    
                    if (firstTimeSelect) {
                        firstTimeSelect = false;
                        notifyReady();
                    }
                }
            
                for(SelectorHandler selectorHandler: selectorHandlers){
                    SelectionKeyHandler selectionKeyHandler = 
                            selectorHandler.getSelectionKeyHandler();

                    for (SelectionKey selectionKey : selectorHandler.keys()) {
                        selectionKeyHandler.close(selectionKey);
                    }
                    selectorHandler.shutdown();
                }
                selectorHandlers.clear();
                pipeline.stopPipeline();
            }
        } finally {
            notifyStopped();
        }
    }
    
    
    /**
     * Stop the Controller by cancelling all the registered keys.
     */
    public void stop() throws IOException {
        stateLock.lock();
        try {
            if (state != State.STOPPED) {
                state = State.STOPPED;
                // TODO: Consider moving the for Controller loop below to
                //       the end of the start() method and using a
                //       wait() / notify() construct to shutdown this
                //       Controller.
                synchronized(shutdownLock){
                    if (readThreadsCount > 0) {
                        for(Controller readController : readThreadControllers) {
                            try {
                                readController.stop();
                            } catch (IOException e) {
                                logger.log(Level.WARNING,
                                        "Exception occured when stopping read Controller!", e);
                            }
                        }
                        
                        multiReadThreadSelectorHandler.shutdown();
                        multiReadThreadSelectorHandler = null;
                        readThreadControllers = null;
                    }
                }
            } else {
                logger.log(Level.FINE, "Controller is already in stopped state");
            }
        } finally {
            stateLock.unlock();
        }
        
    }
    
    
    /**
     * Not implemented.
     */
    public void pause() throws IOException {
        ; // Not yet implemented
    }
    
    
    /**
     * Not implemented.
     */    
    public void resume() throws IOException {
        ; // Not yet implemented
    }
    
    
    /**
     * Initialize the number of ReadThreadController.
     */
    private void initReadThreads() throws IOException {
        readThreadControllers = new ReadController[readThreadsCount];
        for(int i=0; i<readThreadsCount; i++) {
            ReadController controller = new ReadController(this);
            for (SelectorHandler selectorHandler: selectorHandlers){
                SelectorHandler copySelectorHandler = Cloner.clone(selectorHandler);
                copySelectorHandler.setSelector(Selector.open());
                controller.addSelectorHandler(copySelectorHandler);
            }
            controller.setReadThreadsCount(0);
            readThreadControllers[i] = controller;
            // TODO Get a Thread from a Pool instead.
            new Thread(controller).start();
        }
    }
    
    
    /**
     * Is this Controller started?
     * @return <code>boolean</code> true / false
     */
    public boolean isStarted() {
        boolean result = false;
        if (stateLock != null){
            stateLock.lock();
            try {
                result = (state == State.STARTED);
            } finally {
                stateLock.unlock();
            }
        }
        return result;
    }
    
    
    // ----------- ConnectorHandlerPool interface implementation ----------- //  
    
    
    /**
     * Return an instance of a <code>ConnectorHandler</code> based on the
     * Protocol requeted. 
     */
    public ConnectorHandler acquireConnectorHandler(Protocol protocol){
        return connectorHandlerPool.acquireConnectorHandler(protocol);
    }
    
    
    /**
     * Return a <code>ConnectorHandler</code> to the pool of ConnectorHandler.
     * Any reference to the returned must not be re-used as that instance
     * can always be acquired again, causing unexpected results.
     */
    public void releaseConnectorHandler(ConnectorHandler connectorHandler){
        connectorHandlerPool.releaseConnectorHandler(connectorHandler);
    }
        

    /**
     * <tt>true</tt> if OP_ERAD and OP_WRITE can be handled concurrently. 
     * If <tt>false</tt>, the Controller will first invoke the OP_READ handler and
     * then invoke the OP_WRITE during the next Selector.select() invokation.
     */
    public boolean isHandleReadWriteConcurrently() {
        return handleReadWriteConcurrently;
    }

    
    /**
     * <tt>true</tt> if OP_ERAD and OP_WRITE can be handled concurrently. 
     * If <tt>false</tt>, the Controller will first invoke the OP_READ handler and
     * then invoke the OP_WRITE during the next Selector.select() invokation.
     */
    public void setHandleReadWriteConcurrently(boolean handleReadWriteConcurrently) {
        this.handleReadWriteConcurrently = handleReadWriteConcurrently;
    }
}
