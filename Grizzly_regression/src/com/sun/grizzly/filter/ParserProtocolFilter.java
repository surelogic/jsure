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
import com.sun.grizzly.ProtocolParser;
import com.sun.grizzly.util.WorkerThread;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static com.sun.grizzly.Controller.Protocol.TCP;
import static com.sun.grizzly.Controller.Protocol.UDP;
import static com.sun.grizzly.Controller.Protocol;
//import com.sun.grizzly.Controller.Protocol;
import com.sun.grizzly.util.ThreadAttachment;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple ProtocolFilter implementation which read the available bytes
 * and delegate the decision of reading more bytes or not to a ProtocolParser.
 * The ProtocolParser will decide if more bytes are required before continuing
 * the invokation of the ProtocolChain.
 *
 * @author Jeanfrancois Arcand
 */
public abstract class ParserProtocolFilter extends ReadFilter{
    
    public ParserProtocolFilter(){
    }
    
    /**
     * Read available bytes and delegate the processing of them to the next
     * ProtocolFilter in the ProtocolChain.
     * @return <tt>true</tt> if the next ProtocolFilter on the ProtocolChain
     *                       need to bve invoked.
     */
    public boolean execute(Context ctx) throws IOException {
        boolean continueExecution = super.execute(ctx);
        if (!continueExecution){
            return continueExecution;
        }
        
        return invokeProtocolParser(ctx, newProtocolParser());
    }
    
    
    /**
     * Invoke the <code>ProtocolParser</code>. If more bytes are required,
     * register the <code>SelectionKey</code> back to its associated
     * <code>SelectorHandler</code>
     * @param ctx the Context object.
     * @return <tt>true</tt> if no more bytes are needed.
     */
    public boolean invokeProtocolParser(Context ctx, ProtocolParser protocolParser){
        
        if (protocolParser == null){
            throw new IllegalStateException("ProcotolParser cannot be null");
        }
        
        boolean continueExecution = true;
        WorkerThread workerThread = (WorkerThread)Thread.currentThread();
        ByteBuffer byteBuffer = workerThread.getByteBuffer();
        SelectionKey key = ctx.getSelectionKey();
        Protocol protocol = ctx.getProtocol();
        
        protocolParser.parseBytes(byteBuffer);
        // By default, set the byteBuffer position/limit back to where
        // it was before the invokation of the ProtocolParser.
        byteBuffer.limit(protocolParser.getNextEndPosition());
        byteBuffer.position(protocolParser.getNextStartPosition());
        if (protocolParser.isExpectingMoreData()){            
            // Detach the current Thread data.
            ThreadAttachment threadAttachment = workerThread.detach(true);
            
            // Attach it to the SelectionKey so the it can be resumed latter.
            key.attach(threadAttachment);
            
            // Register to get more bytes.
            ctx.getController().registerKey(key,SelectionKey.OP_READ,protocol);
            continueExecution = false;
        } 
        return continueExecution; 
    }
    
    
    /**
     * Return a new or cached ProtocolParser instance.
     */
    public abstract ProtocolParser newProtocolParser();
    
}
