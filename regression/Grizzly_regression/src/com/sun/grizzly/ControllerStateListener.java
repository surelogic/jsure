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
 * Controller state listener interface.
 * 
 * @author Alexey Stashok
 */
public interface ControllerStateListener {
    /**
     * Nofitication: Controller was started, pipelines were initialized
     */
    public void onStarted();
    
    /**
     * Notification: Controller is ready to process connections;
     * <code>SelectorHandler</code>s were initialized
     */
    public void onReady();
    
    /**
     * Notification: Controller was stopped, all worker threads were stopped
     */
    public void onStopped();
    
    /**
     * Notification: Exception occured during Controller thread execution
     */
    public void onException(Throwable e);
}
