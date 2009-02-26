/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.grizzly.connectioncache.spi.concurrent;

/** A class that provides a very simply unbounded queue.
 * The main requirement here is that the class support constant time (very fast)
 * deletion of arbitrary elements.  An instance of this class must be thread safe,
 * either by locking or by using a wait-free algorithm (preferred).
 * The interface is made as simple is possible to make it easier to produce
 * a wait-free implementation.
 * @param V  object type to be stored in this <code>ConcurrentQueue</code>
 */
public interface ConcurrentQueue<V> {
    /** A Handle provides the capability to delete an element of a ConcurrentQueue
     * very quickly.  Typically, the handle is stored in the element, so that an
     * element located from another data structure can be quickly deleted from 
     * a ConcurrentQueue.
     * @param V  type of <code>Handle</code>
     */
    public interface Handle<V> {
	/** Return the value that corresponds to this handle.
	 * @return  value that corresponds to this handle
         */
	V value() ;

	/** Delete the element corresponding to this handle 
	 * from the queue.  Takes constant time.  Returns
	 * true if the removal succeeded, or false if it failed.
	 * which can occur if another thread has already called
	 * poll or remove on this element.
	 * @return  true if the removal succeeded, or false if it failed
         */
	boolean remove() ;
    }

    /** Return the number of elements in the queue.
     * @return  number of elements in the queue
     */
    int size() ;

    /** Add a new element to the tail of the queue.
     * Returns a handle for the element in the queue.
     * @param arg  element to add to the queue
     * @return  a <code>Handle</code> for the element added to the queue
     */
    Handle<V> offer( V arg ) ;

    /** Return an element from the head of the queue.
     * The element is removed from the queue.
     * @return  element removed from the queue
     */
    V poll() ;
} 
