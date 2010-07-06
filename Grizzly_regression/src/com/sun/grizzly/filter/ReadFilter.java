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

package com.sun.grizzly.filter;

import com.sun.grizzly.Context;
import com.sun.grizzly.Controller;
import com.sun.grizzly.ProtocolFilter;
import com.sun.grizzly.util.WorkerThread;
import java.net.SocketAddress;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

import static com.sun.grizzly.Controller.Protocol.TCP;
import static com.sun.grizzly.Controller.Protocol.UDP;
import static com.sun.grizzly.Controller.Protocol;
//import com.sun.grizzly.Controller.Protocol;
import com.sun.grizzly.SelectorHandler;

/**
 * Simple ProtocolFilter implementation which read the available bytes
 * and delegate the processing to the next ProtocolFilter in the ProtocolChain.
 * If no bytes are available, no new ProtocolHandler will be a invoked and
 * the connection (SelectionKey) will be cancelled. This filter can be used
 * for both UDP (reveive) and TCP (read).
 *
 * @author Jeanfrancois Arcand
 */
public class ReadFilter implements ProtocolFilter{
 
    public final static String UDP_SOCKETADDRESS = "socketAddress";   
    
    
    /**
     * <tt>true</tt> if a pipelined execution is required. A pipelined execution
     * occurs when a ProtocolFilter implementation set the 
     * ProtocolFilter.READ_SUCCESS as an attribute to a Context. When this 
     * attribute is present, the ProtocolChain will not release the current
     * running Thread and will re-execute all its ProtocolFilter. 
     */
    protected boolean continousExecution = false;
    
        
    public ReadFilter(){
    }
    
    /**
     * Read available bytes and delegate the processing of them to the next
     * ProtocolFilter in the ProtocolChain.
     * @return <tt>true</tt> if the next ProtocolFilter on the ProtocolChain
     *                       need to bve invoked.
     */
    public boolean execute(Context ctx) throws IOException {
        boolean invokeNextFilter = true;
        int count = -1;
        SocketAddress socketAddress = null;
        Exception exception = null;
        SelectionKey key = ctx.getSelectionKey();
        key.attach(null);
        
        ByteBuffer byteBuffer =
                ((WorkerThread)Thread.currentThread()).getByteBuffer();
        
        Protocol protocol = ctx.getProtocol();
        try {
            int loop = 0;          
            if (protocol == TCP){
                SocketChannel channel = (SocketChannel)key.channel();
                
                // As soon as bytes are ready, invoke the next ProtocolFilter.
                while (channel.isOpen() && 
                        (count = channel.read(byteBuffer)) == 0){

                    // Avoid calling the Selector.
                    if (++loop > 2){
                        ctx.setAttribute(ProtocolFilter.SUCCESSFUL_READ, 
                                         Boolean.FALSE); 
                        break;
                    }
                }
            } else if (protocol == UDP){
                DatagramChannel datagramChannel = (DatagramChannel)key.channel();
                socketAddress = datagramChannel.receive(byteBuffer);  
            }
        } catch (IOException ex) {
            exception = ex;
            log("ReadFilter.execute",ex);
        } catch (RuntimeException ex) {
            exception = ex;
            log("ReadFilter.execute",ex);
        } finally {
            if (exception != null || (count == -1 && protocol == TCP)){  
                if (exception != null){
                    ctx.setAttribute(Context.THROWABLE,exception);
                }
                ctx.setKeyRegistrationState(
                        Context.KeyRegistrationState.CANCEL);
                invokeNextFilter = false;
            } else if (socketAddress == null && protocol == UDP ){
                ctx.setKeyRegistrationState(
                        Context.KeyRegistrationState.REGISTER);    
                invokeNextFilter = false;
            } else if (protocol == UDP) {
                ctx.setAttribute(UDP_SOCKETADDRESS,socketAddress);
            }
        }
        return invokeNextFilter;
    }
    
    
    /**
     * If no bytes were available, close the connection by cancelling the
     * SelectionKey. If bytes were available, register the SelectionKey
     * for new bytes.
     *
     * @return <tt>true</tt> if the previous ProtocolFilter postExecute method
     *         needs to be invoked.
     */
    public boolean postExecute(Context ctx) throws IOException {

        final Protocol protocol = ctx.getProtocol();
        final SelectorHandler selectorHandler = 
                ctx.getController().getSelectorHandler(protocol);
        final SelectionKey key = ctx.getSelectionKey();
        final Context.KeyRegistrationState state = ctx.getKeyRegistrationState();

        try{
            // The ProtocolChain associated with this ProtocolFilter will re-invoke
            // the execute method. Do not register the SelectionKey in that case
            // to avoid thread races.        
            if (continousExecution 
                    && state == Context.KeyRegistrationState.REGISTER
                    && Boolean.FALSE != 
                        (Boolean)ctx.getAttribute(ProtocolFilter.SUCCESSFUL_READ)){
                 ctx.setAttribute(ProtocolFilter.SUCCESSFUL_READ, 
                                 Boolean.TRUE);  
            } else {
                if (state == Context.KeyRegistrationState.CANCEL){
                    selectorHandler.getSelectionKeyHandler().cancel(key);
                } else if (state == Context.KeyRegistrationState.REGISTER){
                    ctx.getController().registerKey(
                            key,SelectionKey.OP_READ,protocol);
                }
            }
            return true;
        } finally {
            ctx.removeAttribute(Context.THROWABLE);
            ctx.removeAttribute(UDP_SOCKETADDRESS);
        }        
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
        
    
    /**
     * Log a message/exception.
     * @param msg <code>String</code>
     * @param t <code>Throwable</code>
     */
    protected void log(String msg,Throwable t){
        if (Controller.logger().isLoggable(Level.FINE)){
            Controller.logger().log(Level.FINE,"ReadFilter,execute()",t);
        }
    }    
}
