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

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Simple interception hook used to trap events inside Grizzly core.
 *
 * @param E  type of Interceptor handler
 * @author Jeanfrancois Arcand
 */
public interface Interceptor<E>{
 
    /**
     * Continue the processing
     */
    public final static int CONTINUE = 0;

    
    /**
     * Do not continue the processing.
     */
    public final static int BREAK = 1;
     
    
    /**
     * The request line has been parsed
     */
    public final static int REQUEST_LINE_PARSED = 0;
    
    
    /**
     * The response has been proceeded.
     */
    public final static int RESPONSE_PROCEEDED = 1;  
    
    
    /**
     * The request has been buffered.
     */
    public final static int REQUEST_BUFFERED = 2;

    
    /**
     * Handle <E> and the associated handler code.
     * @param e
     * @param handlerCode 
     * @return 
     * @throws java.io.IOException 
     */
    public int handle(E e, int handlerCode) throws IOException;
    
    
    /**
     * The SocketChannel associated with this handler.
     * @param socketChannel 
     */
    public void attachChannel(SocketChannel socketChannel);
    
}
