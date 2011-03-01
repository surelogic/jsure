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

import com.sun.grizzly.util.WorkerThreadImpl;

import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import com.sun.grizzly.util.ByteBufferFactory.ByteBufferType;
import com.surelogic.Aggregate;
import com.surelogic.Assume;
import com.surelogic.Assumes;
import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Promise;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Unique;

/**
 * Simple Thread Pool based on the wait/notify/synchronized mechanism.
 *
 * @author Jean-Francois Arcand
 */
@Region("protected Region")
@RegionLock("ThisLock is this protects Region"/*is INCONSISTENT*/)
@Promise("@InRegion(Region) for * *")
// Used to have stuff on this line.  Keep this comment so that the line numbers won't change in the oracle file
public class DefaultPipeline extends LinkedList<Callable>
        implements Pipeline<Callable>{


    /**
     * The number of thread waiting for a <code>Task</code>
     */
    protected int waitingThreads = 0;


    /**
     * The maximum number of Thread
     */
    protected int maxThreads = 20;


    /**
     * The minimum numbers of <code>WorkerThreadImpl</code>
     */
    protected int minThreads = 5;


    /**
     * The minimum numbers of spare <code>WorkerThreadImpl</code>
     */
    protected int minSpareThreads = 2;


    /**
     * The port used.
     */
    protected int port = 8080;


    /**
     * The number of <code>WorkerThreadImpl</code>
     */
    protected int threadCount = 0;


    /**
     * The name of this Pipeline
     */
    protected String name = "Grizzly";


    /**
     * The Thread Priority
     */
    protected int priority = Thread.NORM_PRIORITY;


    /**
     * Has the pipeline already started
     */
    protected boolean isStarted = false;


    /**
     * <code>WorkerThreadImpl</code> amanged by this pipeline.
     */
    @Unique
    @Aggregate("Instance into Region"/*is CONSISTENT*/)
    @InRegion("Region")
    protected transient WorkerThreadImpl[] workerThreads;


    /**
     * Maximum pending connection before refusing requests.
     */
    protected int maxQueueSizeInBytes = -1;


    /**
     * The increment number used when adding new thread.
     */
    protected int threadsIncrement = 1;

    /**
     * The initial ByteBuffer size for newly created WorkerThread instances
     */
    protected int initialByteBufferSize = 8192;


    /**
     * The <code>ByteBufferType</code>
     */
    private ByteBufferType byteBufferType = ByteBufferType.HEAP_VIEW;


    // ------------------------------------------------------- Constructor -----/


    @Borrowed("this"/*is CONSISTENT*/)
    public DefaultPipeline(){
        super();
    }

    @Borrowed("this"/*is CONSISTENT*/)
    public DefaultPipeline(int maxThreads, int minThreads, String name,
            int port, int priority){

        this.maxThreads = maxThreads;
        this.port = port;
        this.name = name;
        this.minThreads = minThreads;
        this.priority = priority;

        if ( minThreads < minSpareThreads )
            minSpareThreads = minThreads;

    }


    @Borrowed("this"/*is CONSISTENT*/)
    public DefaultPipeline(int maxThreads, int minThreads, String name,
            int port){
        this(maxThreads,minThreads,name,port,Thread.NORM_PRIORITY);
    }


    // ------------------------------------------------ Lifecycle ------------/


    /**
     * Init the <code>Pipeline</code> by initializing the required
     * <code>WorkerThreadImpl</code>. Default value is 10
     */
    public synchronized void initPipeline(){

        if (isStarted){
            return;
        }

        if (minThreads > maxThreads) {
            minThreads = maxThreads;
        }

        workerThreads = new WorkerThreadImpl[maxThreads];
        increaseWorkerThread(minThreads, false);
    }


    /**
     * Start the <code>Pipeline</code> and all associated
     * <code>WorkerThreadImpl</code>
     */
    public synchronized void startPipeline(){
        if (!isStarted) {
            for (int i=0; i < minThreads; i++){
                workerThreads[i].start();
            }
            isStarted = true;
        }
    }


    /**
     * Stop the <code>Pipeline</code> and all associated
     * <code>WorkerThreadImpl</code>
     */
    public synchronized void stopPipeline(){
        if (isStarted) {
            for (int i=0; i < threadCount; i++){
                workerThreads[i].terminate();
            }
            isStarted = false;
        }
        notifyAll();
    }



    /**
     * Create new <code>WorkerThreadImpl</code>. This method must be invoked
     * from a synchronized block.
     * @param increment - how many additional <code>WorkerThreadImpl</code>
     * objects to add
     * @param startThread - should newly added <code>WorkerThreadImpl</code>
     * objects be started after creation?
     */
    @RequiresLock("ThisLock"/*is CONSISTENT*/)
    protected void increaseWorkerThread(int increment, boolean startThread){
        WorkerThreadImpl workerThread;
        int currentCount = threadCount;
        int increaseCount = threadCount + increment;
        for (int i=currentCount; i < increaseCount; i++){
            workerThread = new WorkerThreadImpl(this,
                    name + "WorkerThread-"  + port + "-" + i, initialByteBufferSize);
            workerThread.setByteBufferType(byteBufferType);
            workerThread.setPriority(priority);

            if (startThread)
                workerThread.start();

            workerThreads[i] = workerThread;
            threadCount++;
        }
    }


    /**
     * Interrupt the <code>Thread</code> using it thread id
     * @param threadID - id of <code>Thread</code> to interrupt
     * @return boolean, was Thread interrupted successfully ?
     */
    public synchronized boolean interruptThread(long threadID){
        ThreadGroup threadGroup = workerThreads[0].getThreadGroup();
        Thread[] threads = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(threads);

        for (Thread thread: threads){
            if ( thread != null && thread.getId() == threadID ){
                if ( Thread.State.RUNNABLE != thread.getState()){
                    try{
                        thread.interrupt();
                        return true;
                    } catch (Throwable t){
                        ; // Swallow any exceptions.
                    }
                }
            }
        }
        return false;
    }


    // ---------------------------------------------------- Queue ------------//


    /**
     * Add an object to this pipeline
     * @param callable a <code>Callable</code> to add to this Pipeline
     * @throws com.sun.grizzly.PipelineFullException if Pipeline is full
     */
    public synchronized void execute(Callable callable) throws PipelineFullException {
        int queueSize =  size();
        if (maxQueueSizeInBytes != -1 && maxQueueSizeInBytes < queueSize){
            throw new PipelineFullException("Queue is full");
        }

        addLast(callable);
        notify();

        // Create worker threads if we know we will run out of them
        if (threadCount < maxThreads && waitingThreads < (queueSize + 1)){
            int left = maxThreads - threadCount;
            if (threadsIncrement > left){
                threadsIncrement = left;
            }
            increaseWorkerThread(threadsIncrement,true);
        }
    }


    /**
     * Return a <code>Callable</code> object available in the pipeline.
     * All Threads will synchronize on that method
     * @return <code>Callable</code>
     */
    public synchronized Callable waitForIoTask() {
        if (size() - waitingThreads <= 0) {
            try {
                waitingThreads++;
                wait();
            }  catch (InterruptedException e)  {
                Thread.currentThread().interrupt();
            }
            waitingThreads--;
        }
        return poll();
    }


    /**
     * Invoked when the SelectorThread is about to expire a SelectionKey.
     * @param key - A <code>SelectionKey</code> to expire
     * @return true if the SelectorThread should expire the SelectionKey, false
     *              if not.
     */
    public boolean expireKey(SelectionKey key){
        return true;
    }


    /**
     * Return <code>true</code> if the size of this <code>ArrayList</code>
     * minus the current waiting threads is lower than zero.
     */
    @Override
    public boolean isEmpty() {
        return  (size() - getWaitingThread() <= 0);
    }

    // --------------------------------------------------Properties ----------//

    /**
     * Return the number of waiting threads.
     * @return number of waiting threads
     */
    public synchronized int getWaitingThread(){
        return waitingThreads;
    }


    /**
     * Set the number of threads used by this pipeline.
     * @param maxThreads maximum number of threads to use
     */
    public synchronized void setMaxThreads(int maxThreads){
        this.maxThreads = maxThreads;
    }


    /**
     * Return the number of threads used by this pipeline.
     * @return maximum number of threads
     */
    public synchronized int getMaxThreads(){
        return maxThreads;
    }

    /**
     * Return current thread count
     * @return current thread count
     */
    public synchronized int getCurrentThreadCount() {
        return threadCount;
    }


    /**
     * Return the curent number of threads that are currently processing
     * a task.
     * @return current busy thread count
     */
    public synchronized int getCurrentThreadsBusy(){
        return (threadCount - waitingThreads);
    }


    /**
     * Return the maximum spare thread.
     * @return maximum spare thread count
     */
    public synchronized int getMaxSpareThreads() {
        return maxThreads;
    }


    /**
     * Return the minimum spare thread.
     * @return minimum spare thread count
     */
    public synchronized int getMinSpareThreads() {
        return minSpareThreads;
    }


    /**
     * Set the minimum spare thread this <code>Pipeline</code> can handle.
     * @param minSpareThreads minimum number of spare threads to handle
     */
    public synchronized void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
    }


    /**
     * Set the thread priority of the <code>Pipeline</code>
     * @param priority thread priority to use
     */
    public synchronized void setPriority(int priority){
        this.priority = priority;
    }


    /**
     * Set the name of this <code>Pipeline</code>
     * @param name Pipeline name to use
     */
    public synchronized void setName(String name){
        this.name = name;
    }


    /**
     * Return the name of this <code>Pipeline</code>
     * @return the name of this <code>Pipeline</code>
     */
    public synchronized String getName(){
        return name+port;
    }


    /**
     * Set the port used by this <code>Pipeline</code>
     * @param port the port used by this <code>Pipeline</code>
     */
    public synchronized void setPort(int port){
        this.port = port;
    }


    /**
     * Set the minimum thread this <code>Pipeline</code> will creates
     * when initializing.
     * @param minThreads the minimum number of threads.
     */
    public synchronized void setMinThreads(int minThreads){
        this.minThreads = minThreads;
    }


    @Override
    public String toString(){
        return "name: " + name + " maxThreads: " + maxThreads
                + " type: " + this.getClass().getName();
    }


    /**
     * Set the number the <code>Pipeline</code> will use when increasing the
     * thread pool
     * @param threadsIncrement amount to increase thread pool by
     */
    public synchronized void setThreadsIncrement(int threadsIncrement){
        this.threadsIncrement = threadsIncrement;
    }


    /**
     * The number of <code>Task</code> currently queued
     * @return number of queued connections
     */
    public synchronized int getTaskQueuedCount(){
        return size();
    }


    /**
     * Set the maximum pending connection this <code>Pipeline</code>
     * can handle.
     * @param maxQueueSizeInBytesCount maximum queue size (in bytes) this
     * Pipeline should use
     */
    public synchronized void setQueueSizeInBytes(int maxQueueSizeInBytesCount){
        this.maxQueueSizeInBytes = maxQueueSizeInBytesCount;
    }


    /**
     * Get the maximum pending connections this <code>Pipeline</code>
     * can handle.
     * @return maximum queue size (in bytes) this Pipeline is using
     */
    public synchronized int getQueueSizeInBytes(){
        return maxQueueSizeInBytes;
    }

    /**
     * Get the initial WorkerThreadImpl <code>ByteBuffer</code> size
     * @return initial WorkerThreadImpl <code>ByteBuffer</code> size
     */
    public synchronized int getInitialByteBufferSize(){
        return initialByteBufferSize;
    }

    /**
     * Set the initial WorkerThreadImpl <code>ByteBuffer</code> size
     * @param size initial WorkerThreadImpl <code>ByteBuffer</code> size
     */
    public synchronized void setInitialByteBufferSize(int size){
        initialByteBufferSize = size;
    }



    /**
     * The <code>ByteBufferType</code> used to create the <code>ByteBuffer</code>
     * associated with <code>WorkerThreadImpl</code>s created by this instance.
     * @return The <code>ByteBufferType</code> used to create the <code>ByteBuffer</code>
     * associated with <code>WorkerThreadImpl</code>s created by this instance.
     */
    public ByteBufferType getByteBufferType() {
        return byteBufferType;
    }


    /**
     * Set the <code>ByteBufferType</code> to use when creating the
     * <code>ByteBuffer</code> associated with <code>WorkerThreadImpl</code>s
     * created by this instance.
     * @param byteBufferType The ByteBuffer type.
     */
    public void setByteBufferType(ByteBufferType byteBufferType) {
        this.byteBufferType = byteBufferType;
    }
}
