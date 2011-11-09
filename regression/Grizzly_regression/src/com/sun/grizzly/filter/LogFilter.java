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
import com.sun.grizzly.util.ByteBufferInputStream;
import com.sun.grizzly.util.WorkerThread;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Simple filter that log that output the bytes read. This filter uses 
 * a pool of temporary Selector to read as much as possible bytes. This filter
 * should be used for debugging purpose only.
 *
 * @author Jeanfrancois Arcand
 */
public class LogFilter implements ProtocolFilter{
        
    public final static String BYTEBUFFER_INPUTSTREAM = "bbInputStream";
        
    public LogFilter() {
    }

    public boolean execute(Context ctx) throws IOException {                
        ByteBufferInputStream inputStream = (ByteBufferInputStream)
                ctx.getAttribute(BYTEBUFFER_INPUTSTREAM);
        if (inputStream == null){
            inputStream = new ByteBufferInputStream();
            if (ctx.getProtocol() == Controller.Protocol.TLS){
                inputStream.setSecure(true);
            }
            ctx.setAttribute(BYTEBUFFER_INPUTSTREAM,inputStream);
        }
        final WorkerThread workerThread = ((WorkerThread)Thread.currentThread());
        
        ByteBuffer dd = workerThread.getByteBuffer().duplicate();
        dd.flip();
        inputStream.setByteBuffer(dd);
        inputStream.setSelectionKey(ctx.getSelectionKey());

        byte[] requestBytes = new byte[8192];
        inputStream.read(requestBytes);
        
        System.out.println("Request: " + new String(requestBytes));   
        workerThread.getByteBuffer().clear();
        workerThread.getByteBuffer().put(requestBytes);
        return true;
    }

    public boolean postExecute(Context ctx) throws IOException {
        return true;
    }
    
}
