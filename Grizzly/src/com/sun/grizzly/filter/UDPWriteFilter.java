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
import com.sun.grizzly.util.OutputWriter;
import com.sun.grizzly.util.WorkerThread;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.logging.Level;

/**
 * Simple ProtocolFilter implementation which write the available bytes
 * and delegate the processing to the next ProtocolFilter in the ProtocolChain.
 * If no bytes are available for write, no new ProtocolHandler will be a invoked and 
 * the connection (SelectionKey) will be cancelled. 
 *
 * @author Jeanfrancois Arcand
 */
public class UDPWriteFilter implements ProtocolFilter{    
    
    public final static String UDP_SOCKETADDRESS = "socketAddress";
    
    public UDPWriteFilter(){
    }
     
    
    /**
     * Write available bytes and delegate the processing of them to the next
     * ProtocolFilter in the ProtocolChain.
     * @return <tt>true</tt> if the next ProtocolFilter on the ProtocolChain
     *                       need to bve invoked.
     */
    public boolean execute(Context ctx) throws IOException {
        boolean result = true;
        SocketAddress socketAddress = null;
        DatagramChannel datagramChannel = null;
        Exception exception = null;
        SelectionKey key = ctx.getSelectionKey();
        key.attach(null);
        
        ByteBuffer byteBuffer = 
                ((WorkerThread)Thread.currentThread()).getByteBuffer();
        try {
            socketAddress = (SocketAddress)ctx.getAttribute(UDP_SOCKETADDRESS);
            
            if (socketAddress == null){
                throw new IllegalStateException("socketAddress cannot be null");
            }
            
            datagramChannel = (DatagramChannel)key.channel();
            OutputWriter.flushChannel(datagramChannel,socketAddress,byteBuffer);   
        } catch (IOException ex) {
            exception = ex;
            log("UDPWriteFilter.execute",ex);
        } catch (RuntimeException ex) {
            exception = ex;    
            log("UDPWriteFilter.execute",ex);
        } finally {                               
            if (exception != null){
                ctx.setAttribute(Context.THROWABLE,exception);
                ctx.setKeyRegistrationState(
                        Context.KeyRegistrationState.CANCEL);
                result = false;
            } else {
                ctx.setKeyRegistrationState(
                        Context.KeyRegistrationState.REGISTER);    
            } 
        }       
        return result;
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
        if (ctx.getKeyRegistrationState()
                == Context.KeyRegistrationState.CANCEL){
            ctx.getController().getSelectorHandler(ctx.getProtocol()).
                getSelectionKeyHandler().cancel(ctx.getSelectionKey());
        } else if (ctx.getKeyRegistrationState()
                == Context.KeyRegistrationState.REGISTER){
            ctx.getController().registerKey(ctx.getSelectionKey(),
                    SelectionKey.OP_WRITE,ctx.getProtocol());
        }
        return true;
    }
    
    
    /**
     * Log a message/exception.
     * @param msg <code>String</code>
     * @param t <code>Throwable</code>
     */
    protected void log(String msg,Throwable t){
        if (Controller.logger().isLoggable(Level.FINE)){
            Controller.logger().log(Level.FINE,"Write,execute()",t);
        }
    } 
}
