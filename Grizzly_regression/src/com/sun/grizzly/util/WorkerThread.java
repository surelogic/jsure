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

/**
 * Simple interface to allow the addition of <code>Thread</code> attributes.
 *
 * @author Jean-Francois Arcand
 */
public interface WorkerThread{
    
    
    /**
     * Set an instance of <code>ByteBuffer</code>
     * @param byteBuffer <code>ByteBuffer</code>
     */
    public void setByteBuffer(ByteBuffer byteBuffer);

    
    /**
     * Get the <code>ByteBuffer</code>
     * @return <code>ByteBuffer</code>
     */
    public ByteBuffer getByteBuffer();
    
    
    /**
     * Detach the current set of attributes (state) associated with this instance.
     * Invoking detach(true) will re-create all the ByteBuffer associated with
     * this thread, hence this method must be called only when required. If
     * you only need to invoke the object, call detach(false) instead but make 
     * sure you aren't caching or re-using the ThreadAttachment with another
     * thread.
     * @param true to copy the attributes into the ThreadAttachment and re-create
     *             them
     * @return a new ThreadAttachment
     */
    public ThreadAttachment detach(boolean copyState);
    
    
    /**
     * Attach the ThreadAttachment to this instance. This will configure this
     * Thread attributes like ByteBuffer, SSLEngine, etc.
     * @param ThreadAttachment the attachment.
     */
    public void attach(ThreadAttachment threadAttachment);
}
