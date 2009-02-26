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
package com.sun.grizzly;

import java.nio.channels.SelectionKey;

/**
 * An interface used as a wrapper around any kind of thread pool.
 *
 * @param E 
 * @author Jean-Francois Arcand
 */
public interface Pipeline<E> {


    /**
     * Invoked when the SelectorThread is about to expire a SelectionKey.
     * @param key <code>SelectionKey</code>
     * @return true if the SelectorThread should expire the SelectionKey, false
     *              if not.
     */
    public boolean expireKey(SelectionKey key);
    
    
    /**
     * Add an <code>E</code> to be processed by this <code>Pipeline</code>
     * @param task 
     * @throws com.sun.grizzly.PipelineFullException 
     */
    public void execute(E task) throws PipelineFullException;


    /**
     * Return a <code>E</code> object available in the pipeline.
     * @return
     */
    public E waitForIoTask() ;
    
   
   /**
     * Return the number of waiting threads.
     * @return number of waiting threads
    */
    public int getWaitingThread();
    
    
    /** 
     * Return the number of threads used by this pipeline.
     * @return max number of threads
     */
    public int getMaxThreads();
    
    
    /**
     * Return the number of active threads.
     * @return number of active threads
     */
    public int getCurrentThreadCount() ;
      
      
    /**
     * Return the curent number of threads that are currently processing 
     * a task.
     * @return number of currently processing threads
     */
    public int getCurrentThreadsBusy();
    
   
    /**
     * Init the <code>Pipeline</code> by initializing the required
     * <code>WorkerThread</code>. Default value is 10
     */
    public void initPipeline();


    /**
     * Return the name of this <code>Pipeline</code>
     * @return name of this <code>Pipeline</code>
     */
    public String getName();


    /**
     * Start the <code>Pipeline</code>
     */
    public void startPipeline();
    

    /**
     * Stop the <code>Pipeline</code> 
     */
    public void stopPipeline();

    
    /**
     * Set the <code>Thread</code> priority used when creating new threads.
     * @param priority 
     */
    public void setPriority(int priority);
    
    
    /**
     * Set the maximum thread this pipeline can handle.
     * @param maxThread 
     */
    public void setMaxThreads(int maxThread);
    
    
    /**
     * Set the minimum thread this pipeline can handle.
     * @param minThread 
     */    
    public void setMinThreads(int minThread);
    
    
    /**
     * Set the port this <code>Pipeline</code> is associated with.
     * @param port 
     */
    public void setPort(int port);
    
    
    /**
     * Set the name of this <code>Pipeline</code>
     * @param name 
     */
    public void setName(String name);
   
    
    /**
     * Set the maximum pending connection this <code>Pipeline</code>
     * can handle.
     * @param maxQueueSizeInBytesCount 
     */
    public void setQueueSizeInBytes(int maxQueueSizeInBytesCount);
    
   
    /**
     * Set the number the <code>Pipeline</code> will use when increasing the 
     * thread pool
     * @param processorThreadsIncrement 
     */
    public void setThreadsIncrement(int processorThreadsIncrement);
    
    
    /**
     * Returns the number of tasks in this <code>Pipeline</code>.
     *
     * @return Number of tasks in this <code>Pipeline</code>.
     */
    public int size();
}
