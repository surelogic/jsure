
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
package com.sun.grizzly.util;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;

/**
 * This class implement IO stream operations on top of a <code>ByteBuffer</code>. 
 * Under the hood, this class use a temporary Selector pool for reading
 * bytes when the client ask for more and the current Selector is not yet ready.
 * 
 * @author Jeanfrancois Arcand
 */
public class ByteBufferInputStream extends InputStream {

    /**
     * The <code>Channel</code> type is used to avoid invoking the instanceof
     * operation when registering the Socket|Datagram Channel to the Selector.
     */ 
    public enum ChannelType { SocketChannel, DatagramChannel }
    
    
    /**
     * By default this class will cast the Channel to a SocketChannel.
     */
    private ChannelType defaultChannelType = ChannelType.SocketChannel;
    
        
    private static int defaultReadTimeout = 15000;
    
    /**
     * The wrapped <code>ByteBuffer</code<
     */
    protected ByteBuffer byteBuffer;

    
    /**
     * The <code>SelectionKey</code> used by this stream.
     */
    protected SelectionKey key = null;
    
    
    /**
     * The time to wait before timing out when reading bytes
     */
    protected int readTimeout = defaultReadTimeout;
    
    
    /**
     * Number of times to retry before return EOF
     */
    protected int readTry = 2;
    
    
    /**
     * Is the stream secure.
     */
    private boolean secure = false;
    
    
    // ------------------------------------------------- Constructor -------//
    
    
    public ByteBufferInputStream () {
    }

    
    public ByteBufferInputStream (final ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    // ---------------------------------------------------------------------//
    
    
    /**
     * Set the wrapped <code>ByteBuffer</code>
     * @param byteBuffer The wrapped byteBuffer
     */
    public void setByteBuffer(final ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }
    
    
    /**
     * Get the wrapped <code>ByteBuffer</code>
     * @return <code>ByteBuffer</code>
     */
    public ByteBuffer getByteBuffer() {
        return  byteBuffer;
    }
    
    
    /**
     * Return the available bytes 
     * @return the wrapped byteBuffer.remaining()
     */
    @Override
    public int available () {
        return (byteBuffer.remaining());
    }

    
    /**
     * Close this stream. 
     */
    @Override
    public void close () {
    }

    
    /**
     * Return true if mark is supported.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    
    /**
     * Read the first byte from the wrapped <code>ByteBuffer</code>.
     */
    @Override
    public int read() throws IOException {
        if (!byteBuffer.hasRemaining()){
            int eof = 0;
            for (int i=0; i < readTry; i++) {
                eof = doRead();
                if ( eof != 0 ){
                    break;
                } 
            }
        }
        return (byteBuffer.hasRemaining() ? (byteBuffer.get () & 0xff): -1);
     }

    
    /**
     * Read the bytes from the wrapped <code>ByteBuffer</code>.
     */
    @Override
    public int read(byte[] b) throws IOException {
        return (read (b, 0, b.length));
    }

    
    /**
     * Read the first byte of the wrapped <code>ByteBuffer</code>.
     * @param offset 
     * @param length 
     */
    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (!byteBuffer.hasRemaining()) {
            int eof = 0;
            for (int i=0; i < readTry; i++) {
                eof = doRead();
                
                if ( eof != 0 ){
                    break;
                } 
            }
            
            if (eof <= 0){
                return -1;
            }
        }
 
        if (length > byteBuffer.remaining()) {
            length = byteBuffer.remaining();
        }
        byteBuffer.get(b, offset, length);
         
        return (length);
    }
    
    
    /**
     * Read the bytes of the wrapped <code>ByteBuffer</code>.
     * @param bb <code>ByteBuffer</code>
     * @return - number of bytes read
     * @throws java.io.IOException 
     */
    public int read(ByteBuffer bb) throws IOException {
        byteBuffer = bb;
        int eof = 0;
        for (int i=0; i < readTry; i++) {
            eof = doRead();

            if ( eof != 0 ){
                break;
            } 
        }

        if (eof <= 0){
            return -1;
        }
        return byteBuffer.limit();
    }
    
    
    /**
     * Recycle this object.
     */ 
    public void recycle(){
        byteBuffer = null;  
        key = null;
    }
    
    
    /**
     * Set the <code>SelectionKey</code> used to reads bytes.
     * @param key <code>SelectionKey</code>
     */
    public void setSelectionKey(SelectionKey key){
        this.key = key;
    }
    
    
    /**
     * Read bytes using the read <code>ReadSelector</code>
     * @return - number of bytes read
     * @throws java.io.IOException 
     */
    protected int doRead() throws IOException{        
        if ( key == null ) return -1;
        
        if (secure){
            return doSecureRead();
        } else {
            return doClearRead();
        }
    }
        
    
    /**
     * Read and decrypt bytes from the underlying SSL connections. All
     * the SSLEngine operations are delegated to class <code>SSLUtils</code>.
     * @return  number of bytes read
     * @throws java.io.IOException 
     */    
    protected  int doSecureRead() throws IOException{ 
        final WorkerThreadImpl workerThread = 
                (WorkerThreadImpl)Thread.currentThread();

        if (byteBuffer.position() > 0) {
            byteBuffer.compact();
        }
        
        ByteBuffer byteBuffer = workerThread.getByteBuffer();
        int bytesRead = SSLUtils.doSecureRead((SocketChannel) key.channel(), 
                workerThread.getSSLEngine(), byteBuffer, 
                workerThread.getInputBB());
        byteBuffer.flip();
        
        return bytesRead;
    }   
        
        
    protected int doClearRead() throws IOException{
        byteBuffer.clear();
        int count = 1;
        int byteRead = 0;
        Selector readSelector = null;
        SelectionKey tmpKey = null;

        try{
            ReadableByteChannel readableChannel = (ReadableByteChannel)key.channel();
            while (count > 0){
                count = readableChannel.read(byteBuffer);
                if ( count > -1 )
                    byteRead += count;
                else
                    byteRead = count;
            }            
            
            if ( byteRead == 0 ){
                readSelector = SelectorFactory.getSelector();

                if ( readSelector == null ){
                    return 0;
                }
                count = 1;
                
                tmpKey = null;
                if (defaultChannelType == ChannelType.SocketChannel){
                    tmpKey = ((SocketChannel)readableChannel)
                            .register(readSelector,SelectionKey.OP_READ);  
                } else {
                    tmpKey = ((DatagramChannel)readableChannel)
                            .register(readSelector,SelectionKey.OP_READ);                    
                }
                tmpKey.interestOps(tmpKey.interestOps() | SelectionKey.OP_READ);
                int code = readSelector.select(readTimeout);
                tmpKey.interestOps(
                    tmpKey.interestOps() & (~SelectionKey.OP_READ));

                if ( code == 0 ){
                    return 0; // Return on the main Selector and try again.
                }

                while (count > 0){
                    count = readableChannel.read(byteBuffer);
                    if ( count > -1 )
                        byteRead += count;
                    else
                        byteRead = count;                    
                }
            }
        } finally {
            if (tmpKey != null)
                tmpKey.cancel();

            if ( readSelector != null){
                // Bug 6403933
                try{
                    readSelector.selectNow();
                } catch (IOException ex){
                    ;
                }
                SelectorFactory.returnSelector(readSelector);
            }
            
            byteBuffer.flip();
        }
        return byteRead;
    } 

    
    /**
     * Return the timeout between two consecutives Selector.select() when a 
     * temporary Selector is used.
     * @return read timeout being used
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    
    /**
     * Set the timeout between two consecutives Selector.select() when a 
     * temporary Selector is used.
     * @param rt - read timeout
     */    
    public void setReadTimeout(int rt) {
        readTimeout = rt;
    }

    
    /**
     * Return the Selector.select() default time out.
     * @return  default time out
     */
    public static int getDefaultReadTimeout() {
        return defaultReadTimeout;
    }

    
    /**
     * Set the default Selector.select() time out.
     * @param aDefaultReadTimeout  time out value
     */
    public static void setDefaultReadTimeout(int aDefaultReadTimeout) {
        defaultReadTimeout = aDefaultReadTimeout;
    }

    
    /**
     * Return the <code>Channel</code> type. The return value is SocketChannel
     * or DatagramChannel.
     * @return  <code>Channel</code> being used
     */
    public ChannelType getChannelType() {
        return defaultChannelType;
    }

    
    /**
     * Set the <code>Channel</code> type, which is ocketChannel
     * or DatagramChannel.
     * @param channelType  <code>Channel</code> to use
     */
    public void setChannelType(ChannelType channelType) {
        this.defaultChannelType = channelType;
    }

    
    /**
     * Is this Stream secure.
     * @return  true is stream is secure, otherwise false
     */
    public boolean isSecure() {
        return secure;
    }

    
    /**
     * Set this stream secure.
     * @param secure  true to set stream secure, otherwise false
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }
}

