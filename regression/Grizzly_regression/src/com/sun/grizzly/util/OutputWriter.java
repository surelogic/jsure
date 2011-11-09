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

package com.sun.grizzly.util;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * NIO utility to flush <code>ByteBuffer</code>
 *
 * @author Scott Oaks
 */
public class OutputWriter {
    
    /**
     * The default rime out before closing the connection
     */
    private static int defaultWriteTimeout = 30000;
    
    
    /**
     * Flush the buffer by looping until the <code>ByteBuffer</code> is empty
     * @param socketChannel <code>SocketChannel</code>
     * @param bb the ByteBuffer to write.
     * @return 
     * @throws java.io.IOException 
     */   
    public static long flushChannel(SocketChannel socketChannel, ByteBuffer bb)
            throws IOException{
        return flushChannel(socketChannel,bb,defaultWriteTimeout);
    }
       
    
    /**
     * Flush the buffer by looping until the <code>ByteBuffer</code> is empty
     * @param socketChannel <code>SocketChannel</code>
     * @param bb the ByteBuffer to write.
     * @param writeTimeout 
     * @return 
     * @throws java.io.IOException 
     */   
    public static long flushChannel(SocketChannel socketChannel, 
            ByteBuffer bb, long writeTimeout) throws IOException{    
        
        if (bb == null){
            throw new IllegalStateException("Invalid Response State. ByteBuffer" 
                    + " cannot be null.");
        }
        
        if (socketChannel == null){
            throw new IllegalStateException("Invalid Response State. " +
                    "SocketChannel cannot be null.");
        }       
        
        SelectionKey key = null;
        Selector writeSelector = null;
        int attempts = 0;
        int bytesProduced = 0;
        try {
            while ( bb.hasRemaining() ) {
                int len = socketChannel.write(bb);
                attempts++;
                if (len < 0){
                    throw new EOFException();
                } 
            
                bytesProduced += len;
                
                if (len == 0) {
                    if ( writeSelector == null ){
                        writeSelector = SelectorFactory.getSelector();
                        if ( writeSelector == null){
                            // Continue using the main one.
                            continue;
                        }
                    }
                    
                    key = socketChannel.register(writeSelector, 
                                                 SelectionKey.OP_WRITE);
                    
                    if (writeSelector.select(writeTimeout) == 0) {
                        if (attempts > 2)
                            throw new IOException("Client disconnected");
                    } else {
                        attempts--;
                    }
                } else {
                    attempts = 0;
                }
            }   
        } finally {
            if (key != null) {
                key.cancel();
                key = null;
            }
            
            if ( writeSelector != null ) {
                // Cancel the key.
                writeSelector.selectNow();
                SelectorFactory.returnSelector(writeSelector);
            }
        }
        return bytesProduced;
    }  
    
    
    /**
     * Flush the buffer by looping until the <code>ByteBuffer</code> is empty
     * @param socketChannel <code>SocketChannel</code>
     * @param bb the ByteBuffer to write.
     * @return 
     * @throws java.io.IOException 
     */   
    public static long flushChannel(SocketChannel socketChannel, ByteBuffer[] bb)
            throws IOException{
        return flushChannel(socketChannel,bb,defaultWriteTimeout);
    }    
     
    
    /**
     * Flush the buffer by looping until the <code>ByteBuffer</code> is empty
     * @param socketChannel <code>SocketChannel</code>
     * @param bb the ByteBuffer to write.
     * @param writeTimeout 
     * @return 
     * @throws java.io.IOException 
     */   
    public static long flushChannel(SocketChannel socketChannel,
            ByteBuffer[] bb, long writeTimeout) throws IOException{
      
        if (bb == null){
            throw new IllegalStateException("Invalid Response State. ByteBuffer" 
                    + " cannot be null.");
        }
        
        if (socketChannel == null){
            throw new IllegalStateException("Invalid Response State. " +
                    "SocketChannel cannot be null.");
        }   
        
        SelectionKey key = null;
        Selector writeSelector = null;
        int attempts = 0;
        long totalBytes = 0;
        for (ByteBuffer aBb : bb) {
            totalBytes += aBb.remaining();
        }
        
        long byteProduced = 0;
        try {
            while (byteProduced < totalBytes ) {
                long len = socketChannel.write(bb);
                attempts++;
                byteProduced += len;
                if (len < 0){
                    throw new EOFException();
                } 
            
                if (len == 0) {
                    if ( writeSelector == null ){
                        writeSelector = SelectorFactory.getSelector();
                        if ( writeSelector == null){
                            // Continue using the main one.
                            continue;
                        }
                    }
                    
                    key = socketChannel.register(writeSelector,  
                                                 SelectionKey.OP_WRITE);
                    
                    if (writeSelector.select(writeTimeout) == 0) {
                        if (attempts > 2)
                            throw new IOException("Client disconnected");
                    } else {
                        attempts--;
                    }
                } else {
                    attempts = 0;
                }
            }   
        } finally {
            if (key != null) {
                key.cancel();
                key = null;
            }
            
            if ( writeSelector != null ) {
                // Cancel the key.
                writeSelector.selectNow();
                SelectorFactory.returnSelector(writeSelector);
            }
        }
        return byteProduced;
    }  

    
    /**
     * Flush the buffer by looping until the <code>ByteBuffer</code> is empty
     * @param datagramChannel 
     * @param socketAddress 
     * @param bb the ByteBuffer to write.
     * @return 
     * @throws java.io.IOException 
     */   
    public static long flushChannel(DatagramChannel datagramChannel,
            SocketAddress socketAddress, ByteBuffer bb) 
                throws IOException{   
        return flushChannel(datagramChannel,socketAddress,bb,defaultWriteTimeout);
    }
    
    
    /**
     * Flush the buffer by looping until the <code>ByteBuffer</code> is empty
     * @param datagramChannel 
     * @param socketAddress 
     * @param bb the ByteBuffer to write.
     * @param writeTimeout 
     * @return 
     * @throws java.io.IOException 
     */   
    public static long flushChannel(DatagramChannel datagramChannel,
            SocketAddress socketAddress, ByteBuffer bb, long writeTimeout) 
                throws IOException{    
        
        if (bb == null){
            throw new IllegalStateException("Invalid Response State. ByteBuffer" 
                    + " cannot be null.");
        }
        
        if (datagramChannel == null){
            throw new IllegalStateException("Invalid Response State. " +
                    "DatagramChannel cannot be null.");
        }       
        
        if (socketAddress == null){
            throw new IllegalStateException("Invalid Response State. " +
                    "SocketAddress cannot be null.");
        }
        
        SelectionKey key = null;
        Selector writeSelector = null;
        int attempts = 0;
        int bytesProduced = 0;
        try {
            while ( bb.hasRemaining() ) {
                int len = datagramChannel.send(bb,socketAddress);
                attempts++;
                if (len < 0){
                    throw new EOFException();
                } 
            
                bytesProduced += len;
                
                if (len == 0) {
                    if ( writeSelector == null ){
                        writeSelector = SelectorFactory.getSelector();
                        if ( writeSelector == null){
                            // Continue using the main one.
                            continue;
                        }
                    }
                    
                    key = datagramChannel.register(writeSelector, 
                                                   SelectionKey.OP_WRITE);
                    
                    if (writeSelector.select(writeTimeout) == 0) {
                        if (attempts > 2)
                            throw new IOException("Client disconnected");
                    } else {
                        attempts--;
                    }
                } else {
                    attempts = 0;
                }
            }   
        } finally {
            if (key != null) {
                key.cancel();
                key = null;
            }
            
            if ( writeSelector != null ) {
                // Cancel the key.
                writeSelector.selectNow();
                SelectorFactory.returnSelector(writeSelector);
            }
        }
        return bytesProduced;
    }  
    
    
    public static int getDefaultWriteTimeout() {
        return defaultWriteTimeout;
    }

    
    public static void setDefaultWriteTimeout(int aDefaultWriteTimeout) {
        defaultWriteTimeout = aDefaultWriteTimeout;
    }
}
