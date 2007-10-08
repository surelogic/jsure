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
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * A SelectorHandler handles all java.nio.channels.Selector operations. 
 * One or more instance of a Selector are handled by SelectorHandler. 
 * The logic for processing of SelectionKey interest (OP_ACCEPT,OP_READ, etc.)
 * is usually defined using an instance of SelectorHandler.
 *
 * This class represent a UPD implementation of a SelectorHandler. 
 * This class first bind a datagramSocketChannel to a UDP port and then start 
 * waiting for NIO events. 
 *
 * @author Jeanfrancois Arcand
 */
public class UDPSelectorHandler extends TCPSelectorHandler {
 
    private final static String NOT_SUPPORTED = 
            "Not supported by this SelectorHandler";
   
    /**
     * The datagramSocket instance.
     */
    protected DatagramSocket datagramSocket;
    
    
    /**
     * The DatagramChannel.
     */
    protected DatagramChannel datagramChannel;    
 
 
    public UDPSelectorHandler() {
    }
    
    
    public UDPSelectorHandler(boolean isClient) {
        super(isClient);
    }

    
    @Override
    public void copyTo(Copyable copy) {
        super.copyTo(copy);
        UDPSelectorHandler copyHandler = (UDPSelectorHandler) copy;
        copyHandler.datagramSocket = datagramSocket;
        copyHandler.datagramChannel= datagramChannel;
    }

    /**
     * Before invoking Selector.select(), make sure the ServerScoketChannel
     * has been created. If true, then register all SelectionKey to the Selector.
     */
    @Override
    public void preSelect(Context ctx) throws IOException {
        initOpRegistriesIfRequired();
        
        if (selector == null){
            try{
                connectorInstanceHandler = new ConnectorInstanceHandler.
                        ConcurrentQueueDelegateCIH(
                        getConnectorInstanceHandlerDelegate());
                
                
                datagramChannel = DatagramChannel.open();
                datagramChannel.configureBlocking(false);
                selector = Selector.open();                  

                datagramSocket = datagramChannel.socket();
                datagramSocket.setReuseAddress(reuseAddress);
                if ( inet == null)
                    datagramSocket.bind(new InetSocketAddress(port));
                else
                    datagramSocket.bind(new InetSocketAddress(inet,port));

                datagramChannel.configureBlocking(false);
                datagramChannel.register( selector, SelectionKey.OP_READ );
            } catch (SocketException ex){
                throw new BindException(ex.getMessage() + ": " + port);
            }
        
            datagramSocket.setSoTimeout(serverTimeout);     
        } else {
            onReadOps();
            onWriteOps();
            onConnectOps(ctx);
        }
    }

    
    /**
     * Handle new OP_CONNECT ops.
     */
    protected void onConnectOps(Context ctx) throws IOException{   
        if (!opConnectToRegister.isEmpty()){           
            Iterator<SocketAddress[]> iterator = 
                    opConnectToRegister.keySet().iterator();
            SocketAddress[] remoteLocal;
            SelectionKey key;
            while(iterator.hasNext()){                
                remoteLocal = iterator.next();
                final DatagramChannel datagramChannel = DatagramChannel.open();
                datagramChannel.socket().setReuseAddress(true);
                if (remoteLocal[1] != null){
                    datagramChannel.socket().bind(remoteLocal[1]);
                }
                datagramChannel.configureBlocking(false);
                datagramChannel.connect(remoteLocal[0]);
                key = datagramChannel.register(selector,
                        SelectionKey.OP_READ|SelectionKey.OP_WRITE,
                        opConnectToRegister.remove(remoteLocal));
                onConnectInterest(key, ctx);
            } 
        }
    }
  
    
    /**
     * Shuntdown this instance by closing its Selector and associated channels.
     */
    @Override
    public void shutdown(){
        try{
            if ( datagramSocket != null )
                datagramSocket.close();
        } catch (Throwable ex){
            Controller.logger().log(Level.SEVERE,
                    "closeSocketException",ex);
        }

        try{
            if ( datagramChannel != null)
                datagramChannel.close();
        } catch (Throwable ex){
            Controller.logger().log(Level.SEVERE,
                    "closeSocketException",ex);
        }

        try{
            if ( selector != null)
                selector.close();
        } catch (Throwable ex){
            Controller.logger().log(Level.SEVERE,
                    "closeSocketException",ex);
        }       
    }
    
    
    /**
     * Handle OP_ACCEPT. Not used for UPD.
     */
    @Override
    public boolean onAcceptInterest(SelectionKey key, Context ctx) throws IOException{
        return false;
    }
    
    
    /**
     * A token decribing the protocol supported by an implementation of this
     * interface
     */
    public Controller.Protocol protocol(){
        return Controller.Protocol.UDP;
    }   
    
    @Override
    public int getPortLowLevel() {
        if (datagramSocket != null) {
            return datagramSocket.getLocalPort();
        }
        
        return -1;
    }
    
    public int getSsBackLog() {
        throw new IllegalStateException(NOT_SUPPORTED);
    }

    
    public void setSsBackLog(int ssBackLog) {
        throw new IllegalStateException(NOT_SUPPORTED);
    }
    
    
    public boolean isTcpNoDelay() {
        throw new IllegalStateException(NOT_SUPPORTED);
    }

    
    public void setTcpNoDelay(boolean tcpNoDelay) {
        throw new IllegalStateException(NOT_SUPPORTED);
    }
    
    
    public int getLinger() {
       throw new IllegalStateException(NOT_SUPPORTED);
    }

    
    public void setLinger(int linger) {
        throw new IllegalStateException(NOT_SUPPORTED);
    }    
    
    
    public int getSocketTimeout() {
        throw new IllegalStateException(NOT_SUPPORTED);
    }

    
    public void setSocketTimeout(int socketTimeout) {
        throw new IllegalStateException(NOT_SUPPORTED);
    }    

    public void closeChannel(SelectableChannel channel) {
        try{
            channel.close();
        } catch (IOException ex){
            ; // LOG ME
        }
    }
    
    //--------------- ConnectorInstanceHandler -----------------------------
    @Override
    protected Callable<ConnectorHandler> getConnectorInstanceHandlerDelegate() {
        return new Callable<ConnectorHandler>() {
            public ConnectorHandler call() throws Exception {
                return new UDPConnectorHandler();
            }
        };
    }
}
