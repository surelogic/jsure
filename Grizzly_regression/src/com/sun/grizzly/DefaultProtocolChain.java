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

import java.io.IOException;
import java.util.ArrayList;

/**
 * Default ProtocolChain implementation.
 *
 * @author Jeanfrancois Arcand
 */
public class DefaultProtocolChain implements ProtocolChain {
    
    /**
     * The list of ProtocolFilter this chain will invoke.
     */
    protected ArrayList<ProtocolFilter> protocolFilters;
    
    
    /**
     * <tt>true</tt> if a pipelined execution is required. A pipelined execution
     * occurs when a ProtocolFilter implementation set the 
     * ProtocolFilter.READ_SUCCESS as an attribute to a Context. When this 
     * attribute is present, the ProtocolChain will not release the current
     * running Thread and will re-execute all its ProtocolFilter. 
     */
    protected boolean continousExecution = false;
    
    
    public DefaultProtocolChain() {
        protocolFilters = new ArrayList<ProtocolFilter>();
    }
    
    
    /**
     * Execute this ProtocolChain.
     * @param ctx <code>Context</code>
     * @throws java.lang.Exception 
     */
    public void execute(Context ctx) throws Exception {
        Controller controller = ctx.getController();
        if (protocolFilters.size() != 0){
            boolean reinvokeChain = true;
            while (reinvokeChain){
                int currentPosition = executeProtocolFilter(ctx);
                reinvokeChain = postExecuteProtocolFilter(currentPosition, ctx);
            }
            ctx.recycle();
        }
        controller.returnContext(ctx);
    }
    
    
    /**
     * Execute the ProtocolFilter.execute method. If a ProtocolFilter.execute
     * return false, avoid invoking the next ProtocolFilter.
     * @param ctx <code>Context</code>
     * @return position of next <code>ProtocolFilter</code> to exexute
     */
    protected int executeProtocolFilter(Context ctx) {
        boolean invokeNext = true;
        int size = protocolFilters.size();
        int currentPosition = 0;
        
        for (int i=0; i < size; i++){
            try{
                invokeNext = protocolFilters.get(i).execute(ctx);
            } catch (IOException ex){
                // TODO: Need to handle this exception
                ex.printStackTrace(System.err);
            }
            
            currentPosition = i;
            if ( !invokeNext ) break;
        }
        return currentPosition;
    }
    
    
    /**
     * Execute the ProtocolFilter.postExcute.
     * @param currentPosition position in list of <code>ProtocolFilter</code>s
     * @param ctx <code>Context</code>
     * @return false, always false
     */
    protected boolean postExecuteProtocolFilter(int currentPosition,Context ctx) {
        boolean invokeNext = true;
        ProtocolFilter tmpHandler;
        boolean reinvokeChain = false;
        for (int i = currentPosition; i > -1; i--){
            try{
                tmpHandler = protocolFilters.get(i);
                invokeNext = tmpHandler.postExecute(ctx);                 
            } catch (IOException ex){
                // TODO: Need to handle this exception
                ex.printStackTrace(System.err);
            }
            if ( !invokeNext ) {
                reinvokeChain = false;
                break;
            }
        }
        
        if (continousExecution  
            && (Boolean)ctx.getAttribute(ProtocolFilter.SUCCESSFUL_READ) 
                == Boolean.TRUE 
            && ctx.getKeyRegistrationState() 
                == Context.KeyRegistrationState.REGISTER){
            reinvokeChain = true;    
        } 

        return reinvokeChain;
    }
    
    
    /**
     * Remove a ProtocolFilter.
     * @param theFilter the ProtocolFilter to remove
     * @return removed ProtocolFilter
     */
    public boolean removeFilter(ProtocolFilter theFilter) {
        return protocolFilters.remove(theFilter);
    }
    
    
    /**
     * Return the <code>List</code> of available <code>ProtocolFilter</code>
     * @param protocolFilter 
     * @return 
     */
    public boolean addFilter(ProtocolFilter protocolFilter) {
        return protocolFilters.add(protocolFilter);
    }
    
    
    /**
     * Insert a ProtocolFilter at position pos.
     * @param pos 
     * @param protocolFilter 
     */
    public void addFilter(int pos, ProtocolFilter protocolFilter){
        protocolFilters.add(pos,protocolFilter);
    }
    
    
    /**
     *Insert a ProtocolFilter at position pos.
     * @param pos - position in this ProtocolChain
     * @param protocolFilter - <code>ProtocolFilter</code> to insert
     * @return <code>ProtocolFilter</code> that was set
     */
    public ProtocolFilter setProtocolFilter(int pos,
            ProtocolFilter protocolFilter) {
        return protocolFilters.set(pos,protocolFilter);
    }
    
    
    /**
     * Set to <tt>true</tt> if the current <code>Pipeline</code> can 
     * re-execute its ProtocolFilter(s) after a successful execution. Enabling
     * this property is useful for protocol that needs to support pipelined
     * message requests as the ProtocolFilter are automatically re-executed, 
     * avoiding the overhead of releasing the current Thread, registering 
     * back the SelectionKey to the SelectorHandler and waiting for a new
     * NIO event. 
     * 
     * Some protocols (like http) can get the http headers in one
     * SocketChannel.read, parse the message and then get the next http message 
     * on the second SocketChannel.read(). Not having to release the Thread
     * and re-execute the ProtocolFilter greatly improve performance.
     * @param continousExecution true to enable continuous execution.
     *        (default is false).
     */
    public void setContinuousExecution(boolean continousExecution){
        this.continousExecution = continousExecution;
    }
    
    
    /**
     * Return <tt>true</tt> if the current <code>Pipeline</code> can 
     * re-execute its ProtocolFilter after a successful execution. 
     */    
    public boolean isContinuousExecution(){
        return continousExecution;
    }
    
}
