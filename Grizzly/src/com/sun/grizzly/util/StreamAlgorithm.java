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

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
/**
 * Generic parsing interface that can be used to implement protocol
 * specific logic parsing.
 *
 * @deprecated Use the ProtocolParser instead.
 * 
 * @author Jean-Francois Arcand
 */
public interface StreamAlgorithm{
    
    
    /**
     * Return the stream content-length. If the content-length wasn't parsed,
     * return -1.
     * @return  content length or -1 if content length was not parsed
     */
    public int contentLength();
    
    
    /**
     * Return the stream header length. The header length is the length between
     * the start of the stream and the first occurance of character '\r\n' .
     * @return  header length
     */
    public int headerLength();
    
    
    /**
     * Allocate a <code>ByteBuffer</code>
     * @param useDirect allocate a direct <code>ByteBuffer</code>.
     * @param useView allocate a view <code>ByteBuffer</code>.
     * @param size the size of the newly created <code>ByteBuffer</code>.
     * @return a new <code>ByteBuffer</code>
     */
    public ByteBuffer allocate(boolean useDirect, boolean useView, int size);
    
    
    /**
     * Before parsing the bytes, initialize and prepare the algorithm.
     * @param byteBuffer the <code>ByteBuffer</code> used by this algorithm
     * @return <code>ByteBuffer</code> used by this algorithm
     */
    public ByteBuffer preParse(ByteBuffer byteBuffer);
    
    
    /**
     * Parse the <code>ByteBuffer</code> and try to determine if the bytes
     * stream has been fully read from the <code>SocketChannel</code>.
     * @param byteBuffer the bytes read.
     * @return true if the algorithm determines the end of the stream.
     */
    public boolean parse(ByteBuffer byteBuffer);
    
    
    /**
     * After parsing the bytes, post process the <code>ByteBuffer</code> 
     * @param byteBuffer the <code>ByteBuffer</code> used by this algorithm
     * @return <code>ByteBuffer</code> used by this algorithm
     */
    public ByteBuffer postParse(ByteBuffer byteBuffer);  
    
    
    /**
     * Recycle the algorithm.
     */
    public void recycle();
    
    
    /**
     * Rollback the <code>ByteBuffer</code> to its previous state in case
     * an error as occured.
     * @param byteBuffer
     * @return  <code>ByteBuffer</code>
     */
    public ByteBuffer rollbackParseState(ByteBuffer byteBuffer);  
    
    
    /**
     * The <code>Interceptor</code> associated with this algorithm.
     * @return <code>Interceptor</code>
     */
    public Interceptor getHandler();

    
    /**
     * Set the <code>SocketChannel</code> used by this algorithm
     * @param socketChannel set <code>SocketChannel</code>
     */
    public void setSocketChannel(SocketChannel socketChannel);
    
    
    /**
     * Set the <code>port</code> this algorithm is used.
     * @param port  port number
     */
    public void setPort(int port);
    
    
    /**
     * Return the port
     * @return  port number being used
     */
    public int getPort();

}

