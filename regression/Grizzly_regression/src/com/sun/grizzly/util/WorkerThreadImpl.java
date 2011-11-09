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

import com.sun.grizzly.Controller;
import com.sun.grizzly.Pipeline;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import javax.net.ssl.SSLEngine;
import com.sun.grizzly.util.ByteBufferFactory.ByteBufferType;

/**
 * Simple worker thread used for processing HTTP requests. All threads are
 * synchronized using a <code>Pipeline</code> object
 *
 * @author Jean-Francois Arcand
 */
public class WorkerThreadImpl extends Thread implements WorkerThread{
    
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    
    /**
     * What will be run.
     */
    protected Runnable target;
    
    
    /**
     * The <code>ByteBuffer</code> used when <code>Task</code> are executed.
     */
    protected ByteBuffer byteBuffer;
    
    
    /**
     * The <code>Pipeline</code> on which this thread synchronize.
     */
    protected Pipeline<Callable> pipeline;
    
    
    /**
     * Looing variable.
     */
    protected volatile boolean execute = true;
    
    
    /**
     * The <code>ThreadGroup</code> used.
     */
    protected final static ThreadGroup threadGroup = new ThreadGroup("Grizzly");
    

    /**
     * The encrypted ByteBuffer used for handshaking and reading request bytes.
     */
    protected ByteBuffer inputBB;


    /**
     * The encrypted ByteBuffer used for handshaking and writing response bytes.
     */
    protected ByteBuffer outputBB;


    /**
     * The <code>SSLEngine</code> used to manage the SSL over NIO request.
     */
    protected SSLEngine sslEngine;
    
    
    /**
     * The state/attributes on this WorkerThread.
     */
    private ThreadAttachment threadAttachment;
    
    
    /**
     * The ByteBufferType used when creating the ByteBuffer attached to this object.
     */
    private ByteBufferType byteBufferType = ByteBufferType.HEAP_VIEW;
    
    
    /**
     * The size of the ByteBuffer attached to this object.
     */
    private int initialByteBufferSize;
    
    
    /**
     * Create a Thread that will synchronizes/block on
     * <code>Pipeline</code> instance.
     * @param threadGroup <code>ThreadGroup</code>
     * @param runnable <code>Runnable</code>
     */
    public WorkerThreadImpl(ThreadGroup threadGroup, Runnable runnable){
        this(threadGroup, runnable, DEFAULT_BYTE_BUFFER_SIZE);
    }
    
    /**
     * Create a Thread that will synchronizes/block on
     * <code>Pipeline</code> instance.
     * @param threadGroup <code>ThreadGroup</code>
     * @param runnable <code>Runnable</code>
     * @param initialByteBufferSize initial <code>ByteBuffer</code> size
     */
    public WorkerThreadImpl(ThreadGroup threadGroup, Runnable runnable, 
            int initialByteBufferSize){
        super(threadGroup, runnable);
        setDaemon(true);
        target = runnable;
        this.initialByteBufferSize = initialByteBufferSize;
    }
    
    /**
     * Create a Thread that will synchronizes/block on
     * <code>Pipeline</code> instance.
     * @param pipeline <code>Pipeline</code>
     * @param name <code>String</code>
     */
    public WorkerThreadImpl(Pipeline<Callable> pipeline, String name){
        this(pipeline, name, DEFAULT_BYTE_BUFFER_SIZE);
    }
    
    /**
     * Create a Thread that will synchronizes/block on
     * <code>Pipeline</code> instance.
     * @param pipeline <code>Pipeline</code>
     * @param name <code>String</code>
     * @param initialByteBufferSize initial <code>ByteBuffer</code> size
     */
    public WorkerThreadImpl(Pipeline<Callable> pipeline, String name, 
            int initialByteBufferSize){
        super(threadGroup, name);
        this.pipeline = pipeline;
        setDaemon(true);
        this.initialByteBufferSize = initialByteBufferSize;
    }
    
    /**
     * Execute a <code>Task</code>.
     */
    @Override
    public void run(){        
        if (byteBuffer == null){
            byteBuffer = ByteBufferFactory.allocate(byteBufferType,
                    initialByteBufferSize);
        }
        
        if (target != null){
            target.run();
            return;
        }
        
        while (execute) {
            try{
                // Wait for a Task to be added to the pipeline.
                Callable t = pipeline.waitForIoTask();
                if (t != null){
                    t.call();
                    t = null;
                }
            } catch (Throwable t) {
                // Make sure we aren't leaving any bytes after an exception.
                if (byteBuffer != null){
                    byteBuffer.clear();
                }
                if (inputBB != null){
                    inputBB.clear();
                }
                if (outputBB != null){
                    outputBB.clear();
                } 
                
                if (execute) {
                    Controller.logger().log(Level.SEVERE,
                            "WorkerThreadImpl unexpected exception: ",t);
                } else {
                    Controller.logger().log(Level.FINE,
                            "WorkerThreadImpl unexpected exception, when WorderThread supposed to be closed: ",t);
                }
            }
        }
    }
    
    
    /**
     * Stop this thread. If this Thread is performing atask, the task will be
     * completed.
     */
    public void terminate(){
        execute = false;
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
     * Detach the current set of attributes (state) associated with this instance.
     * Invoking detach(true) will re-create all the ByteBuffer associated with
     * this thread, hence this method must be called only when required. If
     * you only need to invoke the object, call detach(false) instead but make 
     * sure you aren't caching or re-using the ThreadAttachment with another
     * thread.
     * @param true to copy the attributes into the ThreadAttachment and re-create
     *             them.
     * @return a new ThreadAttachment
     */
    public ThreadAttachment detach(boolean copyState) {
        if (threadAttachment == null){
           threadAttachment = new ThreadAttachment();
        }
        
        try{
            threadAttachment.setByteBuffer(byteBuffer);
            threadAttachment.setSSLEngine(sslEngine);
            threadAttachment.setInputBB(inputBB);
            threadAttachment.setOutputBB(outputBB);
            
            return threadAttachment;
        } finally {
            // We cannot cache/re-use this object as it might be referenced
            // by more than one thread.   
            if (copyState){
                // Re-create a new ByteBuffer
                byteBuffer = ByteBufferFactory.allocate(byteBufferType,
                    initialByteBufferSize);
                ThreadAttachment newTA = new ThreadAttachment();
                // Switch to the new ThreadAttachment.
                threadAttachment = newTA;
            }
            threadAttachment.setThreadId(getName() + "-" + getId());          
        }
    }

    
    /**
     * Attach the ThreadAttachment to this instance. This will configure this
     * Thread attributes like ByteBuffer, SSLEngine, etc.
     * @param ThreadAttachment the attachment.
     */
    public void attach(ThreadAttachment threadAttachment) {
        byteBuffer = threadAttachment.getByteBuffer();
        sslEngine = threadAttachment.getSSLEngine();
        inputBB = threadAttachment.getInputBB();
        outputBB = threadAttachment.getOutputBB();
        
        this.threadAttachment = threadAttachment;   
        threadAttachment.setThreadId(getName() + "-" + getId());
    }

    
    /**
     * The <code>ByteBufferType</code> used to create the <code>ByteBuffer</code>
     * associated with this object.
     * @return The <code>ByteBufferType</code> used to create the <code>ByteBuffer</code>
     * associated with this object.
     */
    public ByteBufferType getByteBufferType() {
        return byteBufferType;
    }

    
    /**
     * Set the <code>ByteBufferType</code> to use when creating the
     * <code>ByteBuffer</code> associated with this object.
     * @param byteBufferType The ByteBuffer type.
     */
    public void setByteBufferType(ByteBufferType byteBufferType) {
        this.byteBufferType = byteBufferType;
    }
}

