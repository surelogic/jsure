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
import com.sun.grizzly.util.WorkerThread;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

/**
 * Simple ProtocolFilter implementation which read the available bytes
 * and delegate the processing to the next ProtocolFilter in the ProtocolChain.
 * If no bytes are available, no new ProtocolHandler will be a invoked and 
 * the connection (SelectionKey) will be cancelled.
 *
 * @deprecated The ReadFilter can be used for both TCP and UDP.  
 * @author Jeanfrancois Arcand
 */
public class UDPReadFilter extends ReadFilter{

    
    public final static String UDP_SOCKETADDRESS = "socketAddress";
    
    
    public UDPReadFilter(){
    }
     
    /**
     * Read available bytes and delegate the processing of them to the next
     * ProtocolFilter in the ProtocolChain.
     * @return <tt>true</tt> if the next ProtocolFilter on the ProtocolChain
     *                       need to bve invoked.
     */
    @Override
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
            datagramChannel = (DatagramChannel)key.channel();
            socketAddress = datagramChannel.receive(byteBuffer);   
        } catch (IOException ex) {
            exception = ex;
            log("UDPReadFilter.execute",ex);
        } catch (RuntimeException ex) {
            exception = ex;    
            log("UDPReadFilter.execute",ex);
        } finally {                               
            if (exception != null){
                ctx.setAttribute(Context.THROWABLE,exception);
                ctx.setKeyRegistrationState(
                        Context.KeyRegistrationState.CANCEL);
                result = false;
            } else if (socketAddress == null){
                ctx.setKeyRegistrationState(
                        Context.KeyRegistrationState.REGISTER);    
                result = false;
            } else {
                ctx.setAttribute(UDP_SOCKETADDRESS,socketAddress);
            }
        }       
        return result;
    }
}
