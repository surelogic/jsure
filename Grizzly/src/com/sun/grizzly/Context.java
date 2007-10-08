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

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.concurrent.Callable;

/**
 * This Object is used to share information between the Grizzly Framework
 * classes and ProtocolFilter implementation.
 *
 * @author Jeanfrancois Arcand
 */
public class Context implements Callable {
    
    /**
     * A SelectionKey's registration state.
     */
    public enum KeyRegistrationState {
        /** A cancelled SelectionKey registration state. */
        CANCEL,
        /** A registered SelectionKey registration state. */
        REGISTER,
        /** A SelectionKey with no registration state. */
        NONE }
    
    
    /**
     * The list of possible SelectionKey.OP_XXXX.
     */
    public enum OpType { OP_READ,
    OP_WRITE,
    OP_CONNECT }
    
    
    /**
     * The current SelectionKey interest ops this Context is processing.
     */
    private OpType currentOpType;
    
    
    /**
     * The <code>ProtocolChain</code> used to execute this <code>Context</code>
     */
    private ProtocolChain protocolChain;
    
    
    /**
     * Constant 'throwable' String
     */
    public final static String THROWABLE ="throwable";
    
    
    /**
     * Used to share object between ProtocolFilter.
     * WARNING: Attributes which are added are never removed automatically
     * The removal operation must be done explicitly inside a ProtocolFilter.
     */
    private HashMap<String,Object> attributes = null;
    
    
    /**
     * The current connection SelectionKey.
     */
    private SelectionKey key;
    
    
    /**
     * The <code>SelectorHandler</code> associated with this Context.
     */
    private SelectorHandler selectorHandler;

    /**
     * The Controller associated with this Context.
     */
    private Controller controller;
    
    
    /**
     * The state's of the key registration.
     */
    private KeyRegistrationState keyRegistrationState
            = KeyRegistrationState.REGISTER;
    
    
    /**
     * The current Pipeline that execute this object.
     */
    private Pipeline pipeline;
    
    
    /**
     * An optional <code>IOEvent</code> that can be invoked
     * before the <code>ProtocolChain</code> is invoked.
     */
    private IOEvent<Context> ioEvent;
    
    
    /**
     * Constructor
     */
    public Context() {
    }
    
    
    /**
     * Remove a key/value object.
     * @param key - name of an attribute
     * @return  attribute which has been removed
     */
    public Object removeAttribute(String key){
        if (attributes == null){
            return null;
        }
        return attributes.remove(key);
    }
    
    
    /**
     * Set a key/value object.
     * @param key - name of an attribute
     * @param value - value of named attribute
     */
    public void setAttribute(String key,Object value){
        if (attributes == null){
            attributes = new HashMap<String,Object>();
        }
        attributes.put(key,value);
    }
    
    
    /**
     * Return an object based on a key.
     * @param key - name of an attribute
     * @return - attribute value for the <tt>key</tt>, null if <tt>key</tt>
     *           does not exist in <tt>attributes</tt>
     */
    public Object getAttribute(String key){
        if (attributes == null){
            return null;
        }
        return attributes.get(key);
    }
    
    
    /**
     * Return the current SelectionKey.
     * @return - this Context's SelectionKey
     */
    public SelectionKey getSelectionKey() {
        return key;
    }
    
    
    /**
     * Set the connection SelectionKey.
     * @param key - set this Context's SelectionKey
     */
    public void setSelectionKey(SelectionKey key) {
        this.key = key;
    }
    
    
    /**
     * Return the current Controller.
     * @return - this Context's current Controller
     */
    public Controller getController() {
        return controller;
    }
    
    
    /**
     * Set the current Controller.
     * @param controller
     */
    public void setController(Controller controller) {
        this.controller = controller;
    }
    
    
    /**
     * Recycle this instance. Note that attributes aren't removed.
     */
    public void recycle(){
        key = null;
        keyRegistrationState = KeyRegistrationState.REGISTER;
        protocolChain = null;
        ioEvent = null;
    }
    
    
    /**
     * Return SelectionKey's next registration state.
     * @return this Context's SelectionKey registration state
     */
    public KeyRegistrationState getKeyRegistrationState() {
        return keyRegistrationState;
    }
    
    
    /**
     * Set the SelectionKey's next registration state
     * @param keyRegistrationState - set this Context's SelectionKey
     *        registration state
     */
    public void setKeyRegistrationState(KeyRegistrationState keyRegistrationState) {
        this.keyRegistrationState = keyRegistrationState;
    }
    
    
    /**
     * Execute the <code>ProtocolChain</code>.
     * @throws java.lang.Exception Exception thrown by protocol chain
     */
    public Object call() throws Exception {
        // If a IOEvent has been defined, invoke it first and
        // let its associated CallbackHandler decide if the ProtocolChain
        // be invoked or not.
        Object attachment = key.attachment();
        if (ioEvent != null && (attachment instanceof CallbackHandler)){
            try{
                CallbackHandler callBackHandler = ((CallbackHandler)attachment);
                if (currentOpType == OpType.OP_READ){
                    callBackHandler.onRead(ioEvent);
                } else if (currentOpType == OpType.OP_WRITE){
                    callBackHandler.onWrite(ioEvent);
                } else if (currentOpType == OpType.OP_CONNECT){
                    callBackHandler.onConnect(ioEvent);
                }
            } finally {
                if (ioEvent != null){
                    // Prevent the CallbackHandler to re-use the context.
                    // TODO: This is still dangerous as the Context might have been
                    // cached by the CallbackHandler.
                    ioEvent.attach(null);
                    ioEvent = null;
                }
            }
        } else {
            SelectionKey currentKey = key;
            selectorHandler.getSelectionKeyHandler().process(currentKey);
            try {
                protocolChain.execute(this);
            } finally {
                selectorHandler.getSelectionKeyHandler().postProcess(currentKey);
            }
        }
        return null;
    }
    
    
    /**
     * Return <code>ProtocolChain</code> executed by this instance.
     * @return <code>ProtocolChain</code> instance
     */
    public ProtocolChain getProtocolChain() {
        return protocolChain;
    }
    
    
    /**
     * Set the <code>ProtocolChain</code> used by this <code>Context</code>.
     * @param protocolChain instance of <code>ProtocolChain</code> to be used by the Context
     */
    public void setProtocolChain(ProtocolChain protocolChain) {
        this.protocolChain = protocolChain;
    }
    
    
    /**
     * Get the current SelectionKey interest ops this instance is executing.
     * @return OpType the currentOpType.
     */
    public OpType getCurrentOpType() {
        return currentOpType;
    }
    
    
    /**
     * Set the current OpType value.
     * @param currentOpType sets current operation type
     */
    public void setCurrentOpType(OpType currentOpType) {
        this.currentOpType = currentOpType;
    }
    
    
    /**
     * Execute this Context using the Controller's Pipeline
     * @throws com.sun.grizzly.PipelineFullException
     */
    public void execute() throws PipelineFullException{
        if (pipeline == null && controller != null){
            pipeline = controller.getPipeline();
        }
        pipeline.execute(this);
    }
    
    
    /**
     * Return the <code>Pipeline</code> executing this instance.
     * @return  <code>Pipeline</code>
     */
    public Pipeline getPipeline() {
        if (pipeline == null && controller != null){
            pipeline = controller.getPipeline();
        }
        return pipeline;
    }
    
    
    /**
     * Set the <code>Pipeline</code> that will execute this instance.
     * @param pipeline  the <code>Pipeline</code> to set
     */
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }
    
    
    /**
     * Set an optional CallbackHandler.
     * @param ioEvent  the <code>IOEvent</code> to set
     */
    protected void setIOEvent(IOEvent<Context> ioEvent){
        this.ioEvent = ioEvent;
    }
    
    /**
     * Return the current <code>IOEvent</code> associated with this
     * instance.
     * @return IOEvent the current <code>IOEvent</code> associated with this
     * instance.
     */
    protected IOEvent getIOEvent(){
        return ioEvent;
    }
    
    
    /**
     * Return the current Controller.Protocol this instance is executing.
     * @return the current Controller.Protocol this instance is executing.
     */
    public Controller.Protocol getProtocol() {
        return selectorHandler.protocol();
    }
    
    
    /**
     * @Deprecated
     *
     * Set the current Controller.Protocol this instance is executing.
     * @param protocol The current protocol.
     */
    public void setProtocol(Controller.Protocol protocol) {
    }
    
    
    /**
     * Return the current <code>SelectorHandler</code> this instance is executing.
     * @return the current <code>SelectorHandler</code> this instance is executing.
     */
    public SelectorHandler getSelectorHandler() {
        return selectorHandler;
    }
    
    /**
     * Set the current <code>SelectorHandler</code> this instance is executing.
     * @param selectorHandler <code>SelectorHandler</code>
     */
    public void setSelectorHandler(SelectorHandler selectorHandler) {
        this.selectorHandler = selectorHandler;
    }
}
