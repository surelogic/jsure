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

package com.sun.grizzly.connectioncache.spi.transport;

import java.io.Closeable;
import java.util.Collection ;
import java.io.IOException ;

/** An instance of a ConnectionFinder may be supplied to the
 * OutboundConnectionCache.get method.
 * @param C  a connection
 */
public interface ConnectionFinder<C extends Closeable> {
    /** Method that searches idleConnections and busyConnections for 
     * the best connection.  May return null if no best connections
     * exists.  May create a new connection and return it.
     * @param cinfo  a <code>ContactInfo</code>
     * @param idleConnections  a <code>Collection</code> of idle connections
     * @param busyConnections  a <code>Collection</code> of busy connections
     * @return  a "best" connection, may be null if no "best" connection exists
     * @throws java.io.IOException 
     */
    C find( ContactInfo<C> cinfo, Collection<C> idleConnections, 
	Collection<C> busyConnections ) throws IOException ;
}

