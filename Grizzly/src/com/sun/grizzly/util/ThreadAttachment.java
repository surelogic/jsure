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
import java.util.WeakHashMap;
import javax.net.ssl.SSLEngine;

/**
 * This object represent the state of a <code>WorkerThread</code>. This include
 * the ByteBuffer binded to the WorkerThread, application data etc.
 *
 * @author Jeanfrancois Arcand
 */
public class ThreadAttachment {
     
    
    private long timeout;
    
    
    private String threadId;
    
    
    private WeakHashMap<String,Object> map; 
        
    
    private ByteBuffer byteBuffer;
    
    
    /**
     * The encrypted ByteBuffer used for handshaking and reading request bytes.
     */
    private ByteBuffer inputBB;


    /**
     * The encrypted ByteBuffer used for handshaking and writing response bytes.
     */
    private ByteBuffer outputBB;


    /**
     * The <code>SSLEngine</code> used to manage the SSL over NIO request.
     */
    private SSLEngine sslEngine;

    
    public ThreadAttachment(){
        map = new WeakHashMap<String,Object>();
    }

    
    public void setAttribute(String key, Object value){
        map.put(key,value);
    }

    
    public Object getAttribute(String key){
        return map.get(key);
    }
    
    
    public Object removeAttribute(String key){
        return map.remove(key);
    }
    
    /**
     * Set the <code>ByteBuffer</code> shared this thread
     */
    public void setByteBuffer(ByteBuffer byteBuffer){
        this.byteBuffer = byteBuffer;
    }
    
    
    /**
     * Return the <code>ByteBuffer</code> shared this thread
     */
    public ByteBuffer getByteBuffer(){
        return byteBuffer;
    }
 
    
    /**
     * Return the encrypted <code>ByteBuffer</code> used to handle request.
     * @return <code>ByteBuffer</code>
     */
    public ByteBuffer getInputBB(){
        return inputBB;
    }
    
    
    /**
     * Set the encrypted <code>ByteBuffer</code> used to handle request.
     * @param inputBB <code>ByteBuffer</code>
     */    
    public void setInputBB(ByteBuffer inputBB){
        this.inputBB = inputBB;
    }
 
    
    /**
     * Return the encrypted <code>ByteBuffer</code> used to handle response.
     * @return <code>ByteBuffer</code>
     */    
    public ByteBuffer getOutputBB(){
        return outputBB;
    }
    
    
    /**
     * Set the encrypted <code>ByteBuffer</code> used to handle response.
     * @param outputBB <code>ByteBuffer</code>
     */   
    public void setOutputBB(ByteBuffer outputBB){
        this.outputBB = outputBB;
    }
    
         
    /**
     * Set the <code>SSLEngine</code>.
     * @return <code>SSLEngine</code>
     */
    public SSLEngine getSSLEngine() {
        return sslEngine;
    }

        
    /**
     * Get the <code>SSLEngine</code>.
     * @param sslEngine <code>SSLEngine</code>
     */
    public void setSSLEngine(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
    }    

    
    /**
     * Return the name of the Thread on which this instance is binded.
     */
    public String getThreadId() {
        return threadId;
    }

    
    /**
     * Set the Thread's name on which this instance is binded.
     */
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    
    /**
     * Set the timeout used by the SelectionKeyHandler to times out an idle
     * connection.
     */
    public void setTimeout(long timeout){
        this.timeout = timeout;
    }
    
    
    /**
     * Return the timeout used by the SelectionKeyHandler to times out an idle
     * connection.
     */
    public long getTimeout(){
        return timeout;
    }
    
}
