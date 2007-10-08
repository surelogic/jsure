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

import java.nio.ByteBuffer;

/**
 * An interface that knows how to parse bytes into a protocol data unit.
 *
 * @author Charlie Hunt
 */
public interface ProtocolParser<T> {
    
    /**
     * Is this ProtocolParser expecting more data ?
     * 
     * This method is typically called after a call to <code>parseBytes()</code>
     * to determine if the <code>ByteBuffer</code> which has been parsed
     * contains a partial <code>T</code>.
     * 
     * @return - <code>true</code> if more bytes are needed to construct a
     *           <code>T</code>.  <code>false</code>, if no 
     *           additional bytes remain to be parsed into a <code>T</code>.
     */
    public boolean isExpectingMoreData();

    
    /**
     * If there are sufficient bytes in the <code>ByteBuffer</code> to compose a
     * <code>T</code>, then return a newly initialized <code>T</code>.
     * Otherwise, return null.
     * 
     * When this method is first called, it is assumed that 
     * <code>ByteBuffer.position()</code> points to the location in the 
     * <code>ByteBuffer</code> where the beginning of the first
     * <code>T</code> begins.
     * 
     * If there is no partial <code>T</code> remaining in the 
     * <code>ByteBuffer</code> when this method exits, this method will e
     * <code>this.expectingMoreData</code> to <code>false</code>.
     * Otherwise, it will be set to <code>true</code>.
     * 
     * Callees of this method may check <code>isExpectingMoreData()</code> 
     * subsequently to determine if this <code>ProtocolParser</code> is expecting 
     * more data to complete a protocol data unit.  Callees may also 
     * subsequently check <code>hasMoreBytesToParse()</code> to determine if this 
     * <code>ProtocolParser</code> has more data to parse in the given
     * <code>ByteBuffer</code>.
     * 
     * @return <code>T</code> if one is found in the <code>ByteBuffer</code>.
     *         Otherwise, returns null.
     */
    public T parseBytes(ByteBuffer byteBuffer);

    
    /**
     * Are there more bytes to be parsed in the <code>ByteBuffer</code> given
     * to this ProtocolParser's <code>parseBytes</code> ?
     * 
     * This method is typically called after a call to <code>parseBytes()</code>
     * to determine if the <code>ByteBuffer</code> has more bytes which need to
     * parsed into a <code>T</code>.
     * 
     * @return <code>true</code> if there are more bytes to be parsed.
     *         Otherwise <code>false</code>.
     */
    public boolean hasMoreBytesToParse();

    
    /**
     * Set the starting position where the next message in the
     * <code>ByteBuffer</code> given to <code>parseBytes()</code> begins.
     */
    public void setNextStartPosition(int position);

    
    /**
     * Get the starting position where the next message in the
     * <code>ByteBuffer</code> given to <code>parseBytes()</code> begins.
     */
    public int getNextStartPosition();

    
    /**
     * Set the ending position where the next message in the
     * <code>ByteBuffer</code> given to <code>parseBytes()</code> ends.
     */
    public void setNextEndPosition(int position);

    
    /**
     * Get the starting position where the next message in the
     * <code>ByteBuffer</code> given to <code>parseBytes()</code> begins.
     */
    public int getNextEndPosition();
    
    
    
    /**
     * Return the suggested number of bytes needed to hold the next message
     * to be parsed.
     */
    public int getSizeNeeded();
}
