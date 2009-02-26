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

package com.sun.grizzly;

/**
 * Callback handler for non blocking client operations. This class should be
 *
 * @param E  object containing information about the current 
 *        non blocking connection
 * @author Jeanfrancois Arcand
 */
public interface CallbackHandler<E> extends Handler{

    /**
     * This method is called when an non blocking OP_CONNECT is ready
     * to get processed.
     * @param ioEvent an object containing information about the current 
     *        non blocking connection. 
     */
    public void onConnect(IOEvent<E> ioEvent);
    
    
    /**
     * This method is called when an non blocking OP_READ is ready
     * to get processed.
     * @param ioEvent an object containing information about the current 
     *        non blocking connection. 
     */
    public void onRead(IOEvent<E> ioEvent);
    
    
    /**
     * This method is called when an non blocking OP_WRITE is ready
     * to get processed.
     * @param ioEvent an object containing information about the current 
     *        non blocking connection. 
     */
    public void onWrite(IOEvent<E> ioEvent);
    
}
