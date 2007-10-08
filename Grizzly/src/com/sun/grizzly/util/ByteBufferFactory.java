
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

import java.nio.ByteBuffer;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;


/**
 * Factory class used to create <code>ByteBuffer</code>. 
 *
 * The ByteBuffer can by direct (ByteBufferType.DIRECT) or heap (ByteBufferType.HEAP)
 * a view (ByteBufferType.DIRECT_VIEW) or ByteBufferType.HEAP_VIEW)
 * or backed by an array (ByteBufferType.HEAP_ARRAY).
 *
 * @author Jean-Francois Arcand
 */
@Region("private static BufferRegion")
@RegionLock("Lock is class protects BufferRegion")
public class ByteBufferFactory{

    /**
     * An enumeration of all type of ByteBuffer this object can create.
     */
    public enum ByteBufferType { DIRECT, HEAP, DIRECT_VIEW, HEAP_VIEW, HEAP_ARRAY }
    
    
    /**
     * The default capacity of the default view of a <code>ByteBuffer</code>
     */ 
    public static int defaultCapacity = 8192;
    
    
    /**
     * The default capacity of the <code>ByteBuffer</code> from which views
     * will be created.
     */
    public static int capacity = 4000000; 
    
    
    /**
     * The <code>ByteBuffer</code> used to create direct byteBuffer view.
     */
    @InRegion("BufferRegion")
    private static ByteBuffer directByteBuffer;
        
    
    /**
     * The <code>ByteBuffer</code> used to create direct byteBuffer view.
     */
    @InRegion("BufferRegion")
    private static ByteBuffer heapByteBuffer;
    
    
    /**
     * Private constructor.
     */
    private ByteBufferFactory(){
    }
    
    
    /**
     * Return a direct <code>ByteBuffer</code> view
     * @param size the Size of the <code>ByteBuffer</code>
     * @param direct - direct or non-direct buffer?
     * @return <code>ByteBuffer</code>
     */ 
    public synchronized static ByteBuffer allocateView(int size, boolean direct){
        if (direct && (directByteBuffer == null || 
               (directByteBuffer.capacity() - directByteBuffer.limit() < size))){
            directByteBuffer = ByteBuffer.allocateDirect(capacity);                 
        } else if (heapByteBuffer == null || 
               (heapByteBuffer.capacity() - heapByteBuffer.limit() < size)){
            heapByteBuffer = ByteBuffer.allocate(capacity);            
        }
        ByteBuffer byteBuffer = (direct ? directByteBuffer : heapByteBuffer);

        byteBuffer.limit(byteBuffer.position() + size);
        ByteBuffer view = byteBuffer.slice();
        byteBuffer.position(byteBuffer.limit());  
        
        return view;
    }

    
    /**
     * Return a direct <code>ByteBuffer</code> view using the default size.
     * @param direct - direct or non-direct buffer
     * @return <code>ByteBuffer</code>
     */ 
    public static ByteBuffer allocateView(boolean direct){
        return allocateView(defaultCapacity, direct);
    }
     
    
    /**
     * Return a new ByteBuffer based on the requested <code>ByteBufferType</code>
     * @param type the requested <code>ByteBufferType</code>
     * @param size the <code>ByteBuffer</code> size.
     * @return a new ByteBuffer based on the requested <code>ByteBufferType</code>
     */
    public static ByteBuffer allocate(ByteBufferType type,int size){       
        if (type == ByteBufferType.HEAP){
            return ByteBuffer.allocate(size);
        } else if (type == ByteBufferType.HEAP_VIEW) {
            return allocateView(size,false);
        } else if (type == ByteBufferType.HEAP_ARRAY) {
            return ByteBuffer.wrap(new byte[size]);
        } else if (type == ByteBufferType.DIRECT){
           return ByteBuffer.allocateDirect(size); 
        } else if (type == ByteBufferType.DIRECT_VIEW){
            return allocateView(size,true);
        } else {
            throw new IllegalStateException("Invalid ByteBuffer Type");
        }
    }
}
